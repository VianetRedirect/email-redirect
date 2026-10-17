package com.vianetredirect.firetvremote

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class Response(val code: Int, val body: String) {
    val ok: Boolean get() = code in 200..299
    val unauthorized: Boolean get() = code == 401 || code == 403
    /** The Fire TV sometimes answers a perfectly good key press with a 500, so that counts as accepted too. */
    val accepted: Boolean get() = ok || code == 500
}

/**
 * Talks to the Fire TV over the same local HTTPS service (port 8080) the official Amazon remote app uses.
 * Pair once by typing the PIN the TV shows; after that every request carries the token it handed back.
 */
class FireTvClient(val host: String, @Volatile var token: String?) {

    companion object {
        const val PORT = 8080
        const val WAKE_PORT = 8009
        private const val API_KEY = "0987654321"
        private const val USER_AGENT = "okhttp/4.10.0"

        // The Fire TV presents a self-signed certificate on port 8080, so certificate checks are skipped.
        // Requests only ever go to a private LAN address the user paired with.
        private val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        private val sslFactory: SSLSocketFactory by lazy {
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
            ctx.socketFactory
        }

        private val anyHost = HostnameVerifier { _, _ -> true }
    }

    @Throws(IOException::class)
    fun request(
        method: String,
        path: String,
        body: String? = null,
        readTimeoutMs: Int = 4000,
        connectTimeoutMs: Int = 1500,
        closeConnection: Boolean = false,
        withToken: Boolean = true
    ): Response {
        val url = URL("https://$host:$PORT$path")
        val c = url.openConnection() as HttpsURLConnection
        c.sslSocketFactory = sslFactory
        c.hostnameVerifier = anyHost
        c.connectTimeout = connectTimeoutMs
        c.readTimeout = readTimeoutMs
        c.requestMethod = method
        c.useCaches = false
        c.setRequestProperty("X-Api-Key", API_KEY)
        c.setRequestProperty("User-Agent", USER_AGENT)
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        c.setRequestProperty("Accept", "application/json")
        if (withToken) token?.let { c.setRequestProperty("X-Client-Token", it) }
        if (closeConnection) c.setRequestProperty("Connection", "close")

        if (method == "POST") {
            val bytes = (body ?: "").toByteArray(Charsets.UTF_8)
            c.doOutput = true
            c.setFixedLengthStreamingMode(bytes.size)
            c.outputStream.use { it.write(bytes) }
        }
        val code = c.responseCode
        val stream = if (code >= 400) c.errorStream else c.inputStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        return Response(code, text)
    }

    // ---- Pairing -------------------------------------------------------------------------

    /** Asks the TV to put a PIN on screen. */
    @Throws(IOException::class)
    fun displayPin(friendlyName: String): Response = request(
        "POST", "/v1/FireTV/pin/display",
        JSONObject().put("friendlyName", friendlyName).toString(),
        readTimeoutMs = 6000, connectTimeoutMs = 3000, closeConnection = true, withToken = false
    )

    /** Sends the PIN back. Returns the client token, or null if the TV rejected the PIN. */
    @Throws(IOException::class)
    fun verifyPin(pin: String): String? {
        val r = request(
            "POST", "/v1/FireTV/pin/verify",
            JSONObject().put("pin", pin).toString(),
            readTimeoutMs = 8000, connectTimeoutMs = 3000, closeConnection = true, withToken = false
        )
        if (!r.ok) return null
        val desc = try {
            JSONObject(r.body).optString("description", "")
        } catch (e: Exception) {
            ""
        }
        // A token looks like "Wj9PP8I"; an error reads like a sentence.
        return if (desc.isBlank() || desc.contains(' ')) null else desc
    }

    // ---- Remote keys ---------------------------------------------------------------------

    /**
     * Navigation keys: dpad_up, dpad_down, dpad_left, dpad_right, select, back, home, menu.
     * keyActionType is keyDownUp for a tap, or keyDown / keyUp for press-and-hold.
     */
    @Throws(IOException::class)
    fun key(action: String, keyActionType: String = "keyDownUp"): Response {
        val r = request(
            "POST", "/v1/FireTV?action=$action",
            JSONObject().put("keyActionType", keyActionType).toString()
        )
        // Some firmware only knows the body-less form for a plain press.
        if (keyActionType == "keyDownUp" && r.code in 400..499 && !r.unauthorized) {
            return request("POST", "/v1/FireTV?action=$action")
        }
        return r
    }

    @Throws(IOException::class)
    fun playPause(): Response = request("POST", "/v1/media?action=play")

    /** Fast forward (forward = true) or rewind. */
    @Throws(IOException::class)
    fun scan(forward: Boolean, keyActionType: String = "keyDownUp"): Response {
        val body = JSONObject()
            .put("direction", if (forward) "forward" else "back")
            .put("keyAction", JSONObject().put("keyActionType", keyActionType))
        return request("POST", "/v1/media?action=scan", body.toString())
    }

    @Throws(IOException::class)
    fun sendText(text: String): Response = request(
        "POST", "/v1/FireTV/text", JSONObject().put("text", text).toString(), readTimeoutMs = 6000
    )

    @Throws(IOException::class)
    fun launchApp(appId: String): Response = request("POST", "/v1/FireTV/app/$appId", readTimeoutMs = 8000)

    @Throws(IOException::class)
    fun apps(): Response = request("GET", "/v1/FireTV/appsV2", readTimeoutMs = 10000)

    @Throws(IOException::class)
    fun status(): Response = request("GET", "/v1/FireTV/status", readTimeoutMs = 3000, connectTimeoutMs = 2500)

    /** Plain-HTTP nudge on port 8009 that the official app uses to wake a sleeping Fire TV. */
    @Throws(IOException::class)
    fun wake() {
        val c = URL("http://$host:$WAKE_PORT/apps/FireTVRemote").openConnection() as HttpURLConnection
        c.connectTimeout = 2500
        c.readTimeout = 5000
        c.requestMethod = "POST"
        c.doOutput = true
        c.setRequestProperty("User-Agent", USER_AGENT)
        c.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
        c.setFixedLengthStreamingMode(0)
        c.outputStream.close()
        val code = c.responseCode
        val stream = if (code >= 400) c.errorStream else c.inputStream
        stream?.close()
    }
}
