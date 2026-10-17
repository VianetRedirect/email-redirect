package com.vianetredirect.firetvremote

import android.content.Context
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.util.ArrayDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class FoundDevice(val host: String, val name: String)

/**
 * Finds Fire TV devices on the local network two ways at once:
 *  - mDNS, which is how the official app finds them, and gives us the device name;
 *  - a quick sweep of the phone's /24 subnet for the remote service on port 8080, as a fallback
 *    for networks where multicast is blocked.
 * Both report on the main thread through [onFound]; [onFinished] fires when the sweep is done.
 */
class Discovery(
    context: Context,
    private val onFound: (FoundDevice) -> Unit,
    private val onFinished: () -> Unit
) {
    companion object {
        const val GENERIC_NAME = "Fire TV"
        private const val SERVICE_TYPE = "_amzn-wplay._tcp."
    }

    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val main = Handler(Looper.getMainLooper())
    private val stopped = AtomicBoolean(true)
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var scanPool: ExecutorService? = null

    fun start() {
        if (!stopped.getAndSet(false)) return
        startMdns()
        startSubnetScan()
    }

    fun stop() {
        if (stopped.getAndSet(true)) return
        discoveryListener?.let {
            try { nsd.stopServiceDiscovery(it) } catch (e: Exception) { }
        }
        discoveryListener = null
        scanPool?.shutdownNow()
        scanPool = null
        synchronized(this) { resolveQueue.clear() }
    }

    private fun report(host: String, name: String) {
        main.post {
            if (!stopped.get()) onFound(FoundDevice(host, name))
        }
    }

    // ---- mDNS ----------------------------------------------------------------------------

    private fun startMdns() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) { discoveryListener = null }
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String?) {}
            override fun onDiscoveryStopped(serviceType: String?) {}
            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo != null) enqueueResolve(serviceInfo)
            }
        }
        discoveryListener = listener
        try {
            nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            discoveryListener = null
        }
    }

    // NsdManager only resolves one service at a time, so resolves are queued.
    @Synchronized
    private fun enqueueResolve(info: NsdServiceInfo) {
        resolveQueue.add(info)
        resolveNext()
    }

    @Synchronized
    private fun resolveNext() {
        if (resolving || stopped.get()) return
        val info = resolveQueue.poll() ?: return
        resolving = true
        try {
            @Suppress("DEPRECATION")
            nsd.resolveService(info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) { finishResolve() }
                override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                    val addr = serviceInfo?.host
                    if (serviceInfo != null && addr is Inet4Address) {
                        val raw = serviceInfo.attributes?.get("n")
                        val name = if (raw != null) String(raw, Charsets.UTF_8).trim() else ""
                        val host = addr.hostAddress
                        if (host != null) report(host, if (name.isEmpty()) GENERIC_NAME else name)
                    }
                    finishResolve()
                }
            })
        } catch (e: Exception) {
            finishResolve()
        }
    }

    @Synchronized
    private fun finishResolve() {
        resolving = false
        resolveNext()
    }

    // ---- Subnet sweep --------------------------------------------------------------------

    private fun startSubnetScan() {
        val local = localIpv4()
        val parts = local?.split(".") ?: emptyList()
        if (parts.size != 4) {
            main.postDelayed({ if (!stopped.get()) onFinished() }, 4000)
            return
        }
        val prefix = parts[0] + "." + parts[1] + "." + parts[2] + "."
        val pool = Executors.newFixedThreadPool(48)
        scanPool = pool
        for (i in 1..254) {
            val ip = prefix + i
            if (ip == local) continue
            pool.execute {
                if (!stopped.get() && portOpen(ip, FireTvClient.PORT, 400) && looksLikeFireTv(ip)) {
                    report(ip, GENERIC_NAME)
                }
            }
        }
        pool.shutdown()
        Thread {
            try { pool.awaitTermination(25, TimeUnit.SECONDS) } catch (e: InterruptedException) { }
            main.post { if (!stopped.get()) onFinished() }
        }.start()
    }

    private fun localIpv4(): String? {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return null
        val props = cm.getLinkProperties(network) ?: return null
        for (la in props.linkAddresses) {
            val a = la.address
            if (a is Inet4Address && !a.isLoopbackAddress) return a.hostAddress
        }
        return null
    }

    private fun portOpen(ip: String, port: Int, timeoutMs: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(ip, port), timeoutMs); true }
    } catch (e: Exception) {
        false
    }

    /** Anything can listen on 8080; a Fire TV answers the status call with an "unauthorized" JSON reply. */
    private fun looksLikeFireTv(ip: String): Boolean = try {
        val r = FireTvClient(ip, null).request("GET", "/v1/FireTV/status", readTimeoutMs = 2500, connectTimeoutMs = 1500, withToken = false)
        r.unauthorized || r.ok || r.body.contains("description")
    } catch (e: Exception) {
        false
    }
}
