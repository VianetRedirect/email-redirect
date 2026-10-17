package com.vianetredirect.firetvremote

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A paired Fire TV: where it is on the network and the token it gave us when we entered its PIN. */
class Device(val host: String, var name: String, var token: String?) {

    fun toJson(): JSONObject = JSONObject()
        .put("host", host)
        .put("name", name)
        .put("token", token ?: "")

    companion object {
        fun fromJson(o: JSONObject): Device {
            val token = o.optString("token", "")
            return Device(o.getString("host"), o.optString("name", "Fire TV"), if (token.isEmpty()) null else token)
        }
    }
}

/** Keeps the list of paired Fire TVs in SharedPreferences. Tiny, synchronous, no database. */
class DeviceStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("devices", Context.MODE_PRIVATE)

    fun all(): MutableList<Device> {
        val raw = prefs.getString("list", null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<Device>(arr.length())
            for (i in 0 until arr.length()) out.add(Device.fromJson(arr.getJSONObject(i)))
            out
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun current(): Device? {
        val list = all()
        if (list.isEmpty()) return null
        val host = prefs.getString("current", null)
        return list.firstOrNull { it.host == host } ?: list[0]
    }

    /** Adds or replaces the entry for this host and makes it the current one. */
    fun save(device: Device) {
        val list = all()
        list.removeAll { it.host == device.host }
        list.add(0, device)
        write(list)
        prefs.edit().putString("current", device.host).apply()
    }

    fun select(host: String) {
        prefs.edit().putString("current", host).apply()
    }

    fun remove(host: String) {
        val list = all()
        list.removeAll { it.host == host }
        write(list)
        if (prefs.getString("current", null) == host) prefs.edit().remove("current").apply()
    }

    private fun write(list: List<Device>) {
        val arr = JSONArray()
        for (d in list) arr.put(d.toJson())
        prefs.edit().putString("list", arr.toString()).apply()
    }
}
