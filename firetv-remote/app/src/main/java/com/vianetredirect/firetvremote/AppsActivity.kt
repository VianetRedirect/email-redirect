package com.vianetredirect.firetvremote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.Executors

/** Lists what is installed on the Fire TV and opens an app with one tap. */
class AppsActivity : Activity() {

    class AppEntry(val id: String, val name: String)

    companion object {
        const val EXTRA_PICK = "pick"
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_APP_NAME = "app_name"
        private val ID_KEYS = arrayOf("appId", "packageName", "package", "pkg", "id")
        private val NAME_KEYS = arrayOf("name", "displayName", "title", "appName", "label")

        // Shown only if the Fire TV will not hand over its app list.
        private val FALLBACK = listOf(
            AppEntry("com.netflix.ninja", "Netflix"),
            AppEntry("com.amazon.avod", "Prime Video"),
            AppEntry("com.amazon.firetv.youtube", "YouTube"),
            AppEntry("com.disney.disneyplus", "Disney+"),
            AppEntry("uk.co.bbc.iplayer", "BBC iPlayer"),
            AppEntry("air.ITVMobilePlayer", "ITVX"),
            AppEntry("com.channel4.ondemand", "Channel 4"),
            AppEntry("com.bskyb.nowtv.beta", "NOW"),
            AppEntry("com.apple.atve.amazon.appletv", "Apple TV"),
            AppEntry("com.spotify.tv.android", "Spotify"),
            AppEntry("com.plexapp.android", "Plex")
        )
    }

    private val all = ArrayList<AppEntry>()
    private val shown = ArrayList<AppEntry>()
    private val adapter = AppAdapter()
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var client: FireTvClient? = null
    private var pickMode = false

    private lateinit var statusText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var filter: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apps)
        val device = DeviceStore(this).current()
        if (device == null) {
            finish()
            return
        }
        client = FireTvClient(device.host, device.token)
        pickMode = intent.getBooleanExtra(EXTRA_PICK, false)
        if (pickMode) findViewById<TextView>(R.id.appsTitle).setText(R.string.pick_title)

        statusText = findViewById(R.id.statusText)
        progress = findViewById(R.id.progress)
        filter = findViewById(R.id.filter)
        val list = findViewById<ListView>(R.id.appList)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ -> choose(shown[position]) }
        findViewById<ImageButton>(R.id.btnClose).setOnClickListener { finish() }
        filter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter() }
        })
        load()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    private fun load() {
        val c = client ?: return
        executor.execute {
            var apps: List<AppEntry> = emptyList()
            try {
                val r = c.apps()
                if (r.ok) apps = parseApps(r.body)
            } catch (e: Exception) {
            }
            val result = apps.sortedBy { it.name.lowercase() }
            main.post {
                if (isFinishing) return@post
                progress.visibility = View.INVISIBLE
                all.clear()
                if (result.isEmpty()) {
                    all.addAll(FALLBACK)
                    statusText.text = getString(R.string.apps_fallback)
                    statusText.visibility = View.VISIBLE
                } else {
                    all.addAll(result)
                    statusText.visibility = View.GONE
                }
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val q = filter.text.toString().trim().lowercase()
        shown.clear()
        for (a in all) {
            if (q.isEmpty() || a.name.lowercase().contains(q) || a.id.lowercase().contains(q)) shown.add(a)
        }
        adapter.notifyDataSetChanged()
    }

    private fun choose(app: AppEntry) {
        if (pickMode) {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_APP_ID, app.id).putExtra(EXTRA_APP_NAME, app.name))
            finish()
        } else {
            launch(app)
        }
    }

    private fun launch(app: AppEntry) {
        val c = client ?: return
        Toast.makeText(this, getString(R.string.apps_launching, app.name), Toast.LENGTH_SHORT).show()
        executor.execute {
            val ok = try {
                c.launchApp(app.id).accepted
            } catch (e: Exception) {
                false
            }
            main.post {
                if (isFinishing) return@post
                if (ok) finish() else Toast.makeText(this, R.string.apps_launch_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // The exact shape of the appsV2 reply is not documented, so walk whatever comes back and
    // pick out every object that carries a package-style id and a display name.
    private fun parseApps(body: String): List<AppEntry> {
        val out = LinkedHashMap<String, AppEntry>()
        val root = try {
            JSONTokener(body).nextValue()
        } catch (e: Exception) {
            null
        } ?: return emptyList()
        walk(root, out)
        return ArrayList(out.values)
    }

    private fun walk(node: Any, out: MutableMap<String, AppEntry>) {
        if (node is JSONObject) {
            val id = firstString(node, ID_KEYS)
            val name = firstString(node, NAME_KEYS)
            if (id != null && id.contains('.') && !id.contains(' ') && !id.contains('/') && !out.containsKey(id)) {
                out[id] = AppEntry(id, name ?: id)
            }
            val keys = node.keys()
            while (keys.hasNext()) {
                val v = node.opt(keys.next())
                if (v is JSONObject || v is JSONArray) walk(v, out)
            }
        } else if (node is JSONArray) {
            for (i in 0 until node.length()) {
                val v = node.opt(i)
                if (v is JSONObject || v is JSONArray) walk(v, out)
            }
        }
    }

    private fun firstString(o: JSONObject, keys: Array<String>): String? {
        for (k in keys) {
            val v = o.opt(k)
            if (v is String && v.isNotBlank()) return v
        }
        return null
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount(): Int = shown.size
        override fun getItem(position: Int): Any = shown[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?: layoutInflater.inflate(R.layout.item_device, parent, false).also {
                it.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_apps)
            }
            val a = shown[position]
            v.findViewById<TextView>(R.id.title).text = a.name
            v.findViewById<TextView>(R.id.subtitle).text = a.id
            return v
        }
    }
}
