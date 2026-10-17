package com.vianetredirect.firetvremote

import android.content.Context

class Shortcut(val id: String, val name: String)

/** The four app buttons at the bottom of the remote. Long-press one to point it at another app. */
object Shortcuts {

    const val COUNT = 4

    private val DEFAULTS = arrayOf(
        Shortcut("com.amazon.avod", "Prime Video"),
        Shortcut("com.netflix.ninja", "Netflix"),
        Shortcut("com.disney.disneyplus", "Disney+"),
        Shortcut("com.amazon.bueller.music", "Amazon Music")
    )

    private fun prefs(context: Context) = context.getSharedPreferences("shortcuts", Context.MODE_PRIVATE)

    fun get(context: Context, slot: Int): Shortcut {
        val p = prefs(context)
        val id = p.getString("id$slot", null) ?: return DEFAULTS[slot]
        return Shortcut(id, p.getString("name$slot", id) ?: id)
    }

    fun set(context: Context, slot: Int, s: Shortcut) {
        prefs(context).edit().putString("id$slot", s.id).putString("name$slot", s.name).apply()
    }

    /** Label and tint that echo the printed buttons on the real remote. */
    fun label(context: Context, s: Shortcut): Pair<String, Int> = when (s.id) {
        "com.amazon.avod" -> Pair("prime video", context.getColor(R.color.brand_prime))
        "com.netflix.ninja" -> Pair("NETFLIX", context.getColor(R.color.brand_netflix))
        "com.disney.disneyplus" -> Pair("Disney+", context.getColor(R.color.brand_disney))
        "com.amazon.bueller.music" -> Pair("amazon music", context.getColor(R.color.brand_music))
        "com.amazon.firetv.youtube" -> Pair("YouTube", context.getColor(R.color.brand_youtube))
        "com.hulu.plus" -> Pair("hulu", context.getColor(R.color.brand_hulu))
        "uk.co.bbc.iplayer" -> Pair("iPlayer", context.getColor(R.color.brand_iplayer))
        "com.spotify.tv.android" -> Pair("Spotify", context.getColor(R.color.brand_spotify))
        else -> Pair(s.name, context.getColor(R.color.remote_icon))
    }
}
