package com.ivac.otpautofill

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Settings + a short activity log, kept in SharedPreferences. */
object Store {
    private fun p(c: Context) = c.getSharedPreferences("ivac", Context.MODE_PRIVATE)

    fun phone(c: Context) = p(c).getString("phone", "") ?: ""
    fun key(c: Context) = p(c).getString("key", "") ?: ""
    fun relay(c: Context) = p(c).getString("relay", "") ?: ""
    fun paired(c: Context) = key(c).isNotBlank() && relay(c).startsWith("https://")

    fun save(c: Context, phone: String, key: String, relay: String) {
        p(c).edit().putString("phone", phone.trim()).putString("key", key.trim().uppercase())
            .putString("relay", relay.trim().trimEnd('/')).apply()
    }

    fun log(c: Context, line: String) {
        val t = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val old = (p(c).getString("log", "") ?: "").split("\n").filter { it.isNotBlank() }
        p(c).edit().putString("log", (listOf("$t  $line") + old).take(20).joinToString("\n")).apply()
    }
    fun logText(c: Context) = p(c).getString("log", "") ?: ""
}
