package com.destro.otgcontroller

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("otg_profiles", Context.MODE_PRIVATE)

    fun load(): MutableList<GameProfile> {
        val raw = prefs.getString("profiles", null)
        if (raw == null) return mutableListOf(GameProfile("Default", 50, listOf(
            ControlItem("Move", .18f, .72f), ControlItem("Aim", .78f, .42f), ControlItem("Fire", .88f, .72f)
        )))
        return try {
            val a = JSONArray(raw)
            MutableList(a.length()) { i ->
                val o = a.getJSONObject(i)
                val c = o.optJSONArray("controls") ?: JSONArray()
                GameProfile(o.optString("name", "Profile"), o.optInt("sensitivity", 50),
                    MutableList(c.length()) { j ->
                        val v = c.getJSONObject(j)
                        ControlItem(v.optString("label", "Marker"), v.optDouble("x", .5).toFloat(), v.optDouble("y", .5).toFloat())
                    })
            }
        } catch (_: Exception) { mutableListOf(GameProfile("Default")) }
    }

    fun save(profiles: List<GameProfile>) {
        val a = JSONArray()
        profiles.forEach { p ->
            val c = JSONArray()
            p.controls.forEach { c.put(JSONObject().put("label", it.label).put("x", it.x.toDouble()).put("y", it.y.toDouble())) }
            a.put(JSONObject().put("name", p.name).put("sensitivity", p.sensitivity).put("controls", c))
        }
        prefs.edit().putString("profiles", a.toString()).apply()
    }
}
