package com.vianetredirect.firetvremote

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : Activity(), DpadView.Listener {

    companion object {
        /** Always points at the newest APK published by the GitHub Actions workflow. */
        const val DOWNLOAD_URL = "https://github.com/VianetRedirect/email-redirect/releases/latest/download/FireRemote.apk"
        private const val REQUEST_PICK_APP = 100
        private const val MENU_SHARE = 1
        private const val MENU_LINK = 2
        private const val MENU_ADD = 3
        private const val MENU_REPAIR = 4
        private const val MENU_FORGET = 5
        private val APP_BUTTONS = intArrayOf(R.id.btnApp1, R.id.btnApp2, R.id.btnApp3, R.id.btnApp4)
    }

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

        // Top row: power, and the blue Alexa button (which types instead of listens)
        val power = findViewById<ImageButton>(R.id.btnPower)
        power.setOnClickListener { haptic(it); wake() }
        power.setOnLongClickListener { haptic(it); sleep(); true }
        findViewById<ImageButton>(R.id.btnMic).setOnClickListener { haptic(it); showKeyboardDialog() }

        bindKey(R.id.btnBack, "back")
        bindKey(R.id.btnHome, "home")
        bindKey(R.id.btnMenu, "menu")
        bindScan(R.id.btnRewind, false)
        bindScan(R.id.btnForward, true)
        findViewById<ImageButton>(R.id.btnPlayPause).setOnClickListener { haptic(it); send { c -> c.playPause() } }

        // Mute | volume rocker | Guide
        findViewById<ImageButton>(R.id.btnMute).setOnClickListener { haptic(it); sendVolume("mute", "keyDownUp") }
        bindVolume(R.id.btnVolUp, "volume_up")
        bindVolume(R.id.btnVolDown, "volume_down")
        findViewById<ImageButton>(R.id.btnGuide).setOnClickListener {
            haptic(it)
            if (ensureDevice()) startActivity(Intent(this, AppsActivity::class.java))
        }

        // Four app buttons: tap opens the app, long-press picks a different one
        for (slot in APP_BUTTONS.indices) {
            val b = findViewById<TextView>(APP_BUTTONS[slot])
            b.setOnClickListener { haptic(it); launchShortcut(slot) }
            b.setOnLongClickListener { haptic(it); pickShortcut(slot); true }
        }
        refreshShortcuts()

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

    private fun haptic(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
        findViewById<ImageButton>(id).setOnTouchListener(HoldTouchListener { type -> send { c -> c.key(action, type) } })
    }

    private fun bindScan(id: Int, forward: Boolean) {
        findViewById<ImageButton>(id).setOnTouchListener(HoldTouchListener { type -> send { c -> c.scan(forward, type) } })
    }

    private fun bindVolume(id: Int, action: String) {
        findViewById<ImageButton>(id).setOnTouchListener(HoldTouchListener { type -> sendVolume(action, type) })
    }

    /** Volume over the network only works on some Fire TV models, so a refusal gets its own message. */
    private fun sendVolume(action: String, type: String) {
        val c = client
        if (c == null) {
            ensureDevice()
            return
        }
        executor.execute {
            val ok = try {
                c.key(action, type).ok
            } catch (e: Exception) {
                false
            }
            if (!ok && type != "keyUp") main.post { toastThrottled(getString(R.string.err_no_volume)) }
        }
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

    private fun sleep() {
        if (!ensureDevice()) return
        Toast.makeText(this, R.string.sleep_sent, Toast.LENGTH_SHORT).show()
        send { c -> c.key("sleep") }
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

    // ---- App shortcut buttons -------------------------------------------------------------

    private fun refreshShortcuts() {
        for (slot in APP_BUTTONS.indices) {
            val b = findViewById<TextView>(APP_BUTTONS[slot])
            val (label, color) = Shortcuts.label(this, Shortcuts.get(this, slot))
            b.text = label
            b.setTextColor(color)
        }
    }

    private fun launchShortcut(slot: Int) {
        val c = client
        if (c == null) {
            ensureDevice()
            return
        }
        val s = Shortcuts.get(this, slot)
        Toast.makeText(this, getString(R.string.apps_launching, s.name), Toast.LENGTH_SHORT).show()
        executor.execute {
            val ok = try {
                c.launchApp(s.id).accepted
            } catch (e: Exception) {
                false
            }
            if (!ok) main.post { toastThrottled(getString(R.string.apps_launch_failed)) }
        }
    }

    private fun pickShortcut(slot: Int) {
        if (!ensureDevice()) return
        val pick = Intent(this, AppsActivity::class.java).putExtra(AppsActivity.EXTRA_PICK, true)
        startActivityForResult(pick, REQUEST_PICK_APP + slot)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val slot = requestCode - REQUEST_PICK_APP
        if (slot !in APP_BUTTONS.indices || resultCode != RESULT_OK || data == null) return
        val id = data.getStringExtra(AppsActivity.EXTRA_APP_ID) ?: return
        val name = data.getStringExtra(AppsActivity.EXTRA_APP_NAME) ?: id
        Shortcuts.set(this, slot, Shortcut(id, name))
        refreshShortcuts()
        Toast.makeText(this, getString(R.string.shortcut_changed, name), Toast.LENGTH_SHORT).show()
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

    // ---- Sharing the app ------------------------------------------------------------------

    private fun shareApp() {
        val uri = Uri.parse("content://" + ApkProvider.AUTHORITY + "/" + ApkProvider.FILE_NAME)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/vnd.android.package-archive")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, DOWNLOAD_URL))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(ApkProvider.FILE_NAME, uri)
        startActivity(Intent.createChooser(send, getString(R.string.menu_share)))
    }

    private fun copyLink() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), DOWNLOAD_URL))
        Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show()
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
        menu.menu.add(0, MENU_SHARE, 0, R.string.menu_share)
        menu.menu.add(0, MENU_LINK, 1, R.string.menu_link)
        menu.menu.add(0, MENU_ADD, 2, R.string.menu_add)
        menu.menu.add(0, MENU_REPAIR, 3, R.string.menu_repair)
        menu.menu.add(0, MENU_FORGET, 4, R.string.menu_forget)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_SHARE -> shareApp()
                MENU_LINK -> copyLink()
                MENU_ADD -> startActivity(Intent(this, SetupActivity::class.java))
                MENU_REPAIR -> device?.let { d ->
                    startActivity(Intent(this, SetupActivity::class.java).putExtra(SetupActivity.EXTRA_HOST, d.host))
                }
                MENU_FORGET -> forgetDevice()
            }
            true
        }
        menu.show()
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
