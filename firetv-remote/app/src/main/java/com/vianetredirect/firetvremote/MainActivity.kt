package com.vianetredirect.firetvremote

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : Activity(), DpadView.Listener {

    private lateinit var store: DeviceStore
    private var device: Device? = null
    private var client: FireTvClient? = null

    // One worker thread keeps key presses in order (keyDown before keyUp) without blocking the UI.
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var lastErrorToast = 0L
    @Volatile private var lastWakeAttempt = 0L
    private var setupOffered = false
    private var unauthorizedDialog: AlertDialog? = null

    private lateinit var deviceName: TextView
    private lateinit var statusDot: View
    private lateinit var dpad: DpadView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = DeviceStore(this)

        deviceName = findViewById(R.id.deviceName)
        statusDot = findViewById(R.id.statusDot)
        dpad = findViewById(R.id.dpad)
        dpad.listener = this

        bindKey(R.id.btnBack, "back")
        bindKey(R.id.btnHome, "home")
        bindKey(R.id.btnMenu, "menu")
        bindScan(R.id.btnRewind, false)
        bindScan(R.id.btnForward, true)

        val playPause = findViewById<ImageButton>(R.id.btnPlayPause)
        playPause.isSoundEffectsEnabled = false
        playPause.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            send { c -> c.playPause() }
        }

        findViewById<Button>(R.id.btnKeyboard).setOnClickListener { showKeyboardDialog() }
        findViewById<Button>(R.id.btnApps).setOnClickListener {
            if (ensureDevice()) startActivity(Intent(this, AppsActivity::class.java))
        }
        findViewById<View>(R.id.deviceChip).setOnClickListener { showDeviceMenu(it) }
        findViewById<ImageButton>(R.id.btnMore).setOnClickListener { showMoreMenu(it) }
    }

    override fun onResume() {
        super.onResume()
        loadDevice()
        if (device == null) {
            if (!setupOffered) {
                setupOffered = true
                startActivity(Intent(this, SetupActivity::class.java))
            }
        } else {
            checkConnection()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unauthorizedDialog?.dismiss()
        executor.shutdown()
    }

    // ---- Device state ---------------------------------------------------------------------

    private fun loadDevice() {
        val d = store.current()
        device = d
        client = if (d == null) null else FireTvClient(d.host, d.token)
        deviceName.text = d?.name ?: getString(R.string.no_device)
        setStatus(null)
    }

    private fun setStatus(online: Boolean?) {
        val color = when (online) {
            true -> R.color.online
            false -> R.color.offline
            null -> R.color.unknown
        }
        statusDot.backgroundTintList = ColorStateList.valueOf(getColor(color))
    }

    private fun ensureDevice(): Boolean {
        if (client != null) return true
        startActivity(Intent(this, SetupActivity::class.java))
        return false
    }

    /** Opens the connection early so the first key press does not pay for the TLS handshake. */
    private fun checkConnection() {
        val c = client ?: return
        executor.execute {
            var online = true
            var unauthorized = false
            try {
                unauthorized = c.status().unauthorized
            } catch (e: Exception) {
                online = false
            }
            main.post {
                if (client !== c) return@post
                setStatus(online)
                if (unauthorized) onUnauthorized()
            }
        }
    }

    // ---- Sending keys ---------------------------------------------------------------------

    private fun send(call: (FireTvClient) -> Response) {
        val c = client
        if (c == null) {
            ensureDevice()
            return
        }
        executor.execute {
            try {
                val r = call(c)
                main.post {
                    if (client !== c) return@post
                    when {
                        r.unauthorized -> onUnauthorized()
                        r.accepted -> setStatus(true)
                        else -> {
                            setStatus(true)
                            toastThrottled(getString(R.string.err_not_supported))
                        }
                    }
                }
            } catch (e: Exception) {
                // Port 8080 is closed while the Fire TV sleeps. Nudge it awake once, then retry.
                val now = SystemClock.elapsedRealtime()
                if (now - lastWakeAttempt > 10000) {
                    lastWakeAttempt = now
                    try {
                        c.wake()
                        Thread.sleep(1500)
                        val r = call(c)
                        main.post {
                            if (client !== c) return@post
                            if (r.unauthorized) onUnauthorized() else setStatus(true)
                        }
                        return@execute
                    } catch (e2: Exception) {
                    }
                }
                main.post {
                    if (client !== c) return@post
                    setStatus(false)
                    toastThrottled(getString(R.string.err_unreachable))
                }
            }
        }
    }

    override fun onKey(action: String, keyActionType: String) {
        send { c -> c.key(action, keyActionType) }
    }

    private fun bindKey(id: Int, action: String) {
        val b = findViewById<ImageButton>(id)
        b.isSoundEffectsEnabled = false
        b.setOnTouchListener(HoldTouchListener { type -> send { c -> c.key(action, type) } })
    }

    private fun bindScan(id: Int, forward: Boolean) {
        val b = findViewById<ImageButton>(id)
        b.isSoundEffectsEnabled = false
        b.setOnTouchListener(HoldTouchListener { type -> send { c -> c.scan(forward, type) } })
    }

    private fun toastThrottled(message: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastErrorToast < 4000) return
        lastErrorToast = now
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun onUnauthorized() {
        if (unauthorizedDialog?.isShowing == true) return
        val d = device ?: return
        unauthorizedDialog = AlertDialog.Builder(this)
            .setMessage(R.string.err_unauthorized)
            .setPositiveButton(R.string.menu_repair) { _, _ ->
                startActivity(Intent(this, SetupActivity::class.java).putExtra(SetupActivity.EXTRA_HOST, d.host))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---- Keyboard -------------------------------------------------------------------------

    private fun showKeyboardDialog() {
        if (!ensureDevice()) return
        val view = layoutInflater.inflate(R.layout.dialog_text, null)
        val input = view.findViewById<EditText>(R.id.textInput)
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.keyboard_title)
            .setView(view)
            .setPositiveButton(R.string.keyboard_send) { _, _ -> sendText(input.text.toString()) }
            .setNegativeButton(R.string.cancel, null)
            .create()
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendText(input.text.toString())
                dialog.dismiss()
                true
            } else {
                false
            }
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        input.requestFocus()
    }

    private fun sendText(text: String) {
        if (text.isEmpty()) return
        send { c -> c.sendText(text) }
    }

    // ---- Menus ----------------------------------------------------------------------------

    private fun showDeviceMenu(anchor: View) {
        val devices = store.all()
        if (devices.isEmpty()) {
            startActivity(Intent(this, SetupActivity::class.java))
            return
        }
        val addId = devices.size
        val menu = PopupMenu(this, anchor)
        for (i in devices.indices) {
            val d = devices[i]
            menu.menu.add(0, i, i, d.name + "  ·  " + d.host)
        }
        menu.menu.add(0, addId, addId, R.string.menu_add)
        menu.setOnMenuItemClickListener { item ->
            if (item.itemId == addId) {
                startActivity(Intent(this, SetupActivity::class.java))
            } else {
                store.select(devices[item.itemId].host)
                loadDevice()
                checkConnection()
            }
            true
        }
        menu.show()
    }

    private fun showMoreMenu(anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.menu.add(0, 1, 0, R.string.menu_wake)
        menu.menu.add(0, 2, 1, R.string.menu_sleep)
        menu.menu.add(0, 3, 2, R.string.menu_add)
        menu.menu.add(0, 4, 3, R.string.menu_repair)
        menu.menu.add(0, 5, 4, R.string.menu_forget)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> wake()
                2 -> send { c -> c.key("sleep") }
                3 -> startActivity(Intent(this, SetupActivity::class.java))
                4 -> device?.let { d ->
                    startActivity(Intent(this, SetupActivity::class.java).putExtra(SetupActivity.EXTRA_HOST, d.host))
                }
                5 -> forgetDevice()
            }
            true
        }
        menu.show()
    }

    private fun wake() {
        val c = client
        if (c == null) {
            ensureDevice()
            return
        }
        executor.execute {
            try {
                c.wake()
                main.post { Toast.makeText(this, R.string.wake_sent, Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                main.post { toastThrottled(getString(R.string.err_unreachable)) }
            }
            main.postDelayed({ checkConnection() }, 2500)
        }
    }

    private fun forgetDevice() {
        val d = device ?: return
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.menu_forget) + "?\n" + d.name + " (" + d.host + ")")
            .setPositiveButton(R.string.menu_forget) { _, _ ->
                store.remove(d.host)
                loadDevice()
                if (device == null) startActivity(Intent(this, SetupActivity::class.java)) else checkConnection()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
