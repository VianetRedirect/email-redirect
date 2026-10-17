package com.vianetredirect.firetvremote

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors
import java.util.regex.Pattern

/** Finds the Fire TV, asks it to show a PIN, and stores the token the TV hands back. */
class SetupActivity : Activity() {

    companion object {
        const val EXTRA_HOST = "host"
        private val IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$")
    }

    private lateinit var store: DeviceStore
    private lateinit var list: ListView
    private lateinit var emptyText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var ipInput: EditText

    private val found = ArrayList<FoundDevice>()
    private val adapter = DeviceAdapter()
    private var discovery: Discovery? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var busy: AlertDialog? = null
    private var pinDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        store = DeviceStore(this)

        list = findViewById(R.id.deviceList)
        emptyText = findViewById(R.id.emptyText)
        progress = findViewById(R.id.progress)
        ipInput = findViewById(R.id.ipInput)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            val d = found[position]
            beginPairing(d.host, d.name)
        }
        findViewById<ImageButton>(R.id.btnRescan).setOnClickListener { restartDiscovery() }
        findViewById<Button>(R.id.btnConnect).setOnClickListener { connectManual() }
        ipInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                connectManual()
                true
            } else {
                false
            }
        }

        // Launched with a host when the user chose "Pair again": go straight to the PIN.
        val host = intent.getStringExtra(EXTRA_HOST)
        if (host != null) {
            val known = store.all().firstOrNull { it.host == host }
            beginPairing(host, known?.name ?: Discovery.GENERIC_NAME)
        }
    }

    override fun onStart() {
        super.onStart()
        startDiscovery()
    }

    override fun onStop() {
        super.onStop()
        stopDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideBusy()
        pinDialog?.dismiss()
        executor.shutdown()
    }

    // ---- Discovery ------------------------------------------------------------------------

    private fun startDiscovery() {
        if (discovery != null) return
        progress.visibility = View.VISIBLE
        if (found.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            emptyText.text = getString(R.string.setup_searching)
        }
        val d = Discovery(this, { onFound(it) }, { onDiscoveryFinished() })
        discovery = d
        d.start()
    }

    private fun stopDiscovery() {
        discovery?.stop()
        discovery = null
    }

    private fun restartDiscovery() {
        stopDiscovery()
        found.clear()
        adapter.notifyDataSetChanged()
        startDiscovery()
    }

    private fun onFound(d: FoundDevice) {
        val existing = found.indexOfFirst { it.host == d.host }
        if (existing >= 0) {
            // The subnet sweep only knows the address; mDNS knows the name. Prefer the name.
            if (found[existing].name == Discovery.GENERIC_NAME && d.name != Discovery.GENERIC_NAME) {
                found[existing] = d
                adapter.notifyDataSetChanged()
            }
            return
        }
        found.add(d)
        adapter.notifyDataSetChanged()
        emptyText.visibility = View.GONE
    }

    private fun onDiscoveryFinished() {
        progress.visibility = View.INVISIBLE
        if (found.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            emptyText.text = getString(R.string.setup_none_found)
        }
    }

    private fun connectManual() {
        val ip = ipInput.text.toString().trim()
        if (!IPV4.matcher(ip).matches()) {
            Toast.makeText(this, R.string.setup_invalid_ip, Toast.LENGTH_SHORT).show()
            return
        }
        beginPairing(ip, Discovery.GENERIC_NAME)
    }

    // ---- Pairing --------------------------------------------------------------------------

    private fun beginPairing(host: String, name: String) {
        val client = FireTvClient(host, null)
        showBusy(getString(R.string.setup_contacting))
        executor.execute {
            val friendly = "Fire Remote (" + Build.MODEL + ")"
            var ok = false
            try {
                ok = client.displayPin(friendly).ok
            } catch (e: Exception) {
            }
            if (!ok) {
                // Maybe it is asleep: nudge it the way the official app does, then try once more.
                try { client.wake() } catch (e: Exception) { }
                try { Thread.sleep(1500) } catch (e: InterruptedException) { }
                try {
                    ok = client.displayPin(friendly).ok
                } catch (e: Exception) {
                }
            }
            val success = ok
            main.post {
                if (isFinishing) return@post
                hideBusy()
                if (success) askForPin(client, name) else showError(getString(R.string.setup_unreachable, host))
            }
        }
    }

    private fun askForPin(client: FireTvClient, name: String) {
        val view = layoutInflater.inflate(R.layout.dialog_pin, null)
        val input = view.findViewById<EditText>(R.id.pinInput)
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.setup_pin_title)
            .setView(view)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = input.text.toString().trim()
                if (pin.length >= 4) {
                    dialog.dismiss()
                    verify(client, pin, name)
                }
            }
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                true
            } else {
                false
            }
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        pinDialog = dialog
        dialog.show()
        input.requestFocus()
    }

    private fun verify(client: FireTvClient, pin: String, name: String) {
        showBusy(getString(R.string.setup_pairing))
        executor.execute {
            val token = try {
                client.verifyPin(pin)
            } catch (e: Exception) {
                null
            }
            main.post {
                if (isFinishing) return@post
                hideBusy()
                if (token == null) {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.setup_wrong_pin)
                        .setPositiveButton(R.string.menu_repair) { _, _ -> beginPairing(client.host, name) }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                } else {
                    store.save(Device(client.host, name, token))
                    Toast.makeText(this, getString(R.string.setup_paired, name), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun showBusy(message: String) {
        hideBusy()
        busy = AlertDialog.Builder(this).setMessage(message).setCancelable(false).show()
    }

    private fun hideBusy() {
        busy?.dismiss()
        busy = null
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this).setMessage(message).setPositiveButton(R.string.ok, null).show()
    }

    private inner class DeviceAdapter : BaseAdapter() {
        override fun getCount(): Int = found.size
        override fun getItem(position: Int): Any = found[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?: layoutInflater.inflate(R.layout.item_device, parent, false)
            val d = found[position]
            v.findViewById<TextView>(R.id.title).text = d.name
            v.findViewById<TextView>(R.id.subtitle).text = d.host
            return v
        }
    }
}
