package com.ivac.otpautofill

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Relay {
    /** Pulls the first 6-digit code out of an SMS body (or accepts a bare 4–8 digit code). */
    fun extractCode(text: String): String? {
        val t = text.trim()
        if (Regex("^\\d{4,8}$").matches(t)) return t
        return Regex("(?<!\\d)(\\d{6})(?!\\d)").find(t)?.groupValues?.get(1)
    }

    /** Blocking — call from a background thread. Returns null on success, else an error text. */
    fun push(c: Context, code: String): String? {
        if (!Store.paired(c)) return "not paired"
        return try {
            val con = URL(Store.relay(c) + "/push").openConnection() as HttpURLConnection
            con.requestMethod = "POST"; con.connectTimeout = 8000; con.readTimeout = 8000; con.doOutput = true
            con.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().put("key", Store.key(c)).put("code", code).toString()
            con.outputStream.use { it.write(body.toByteArray()) }
            val rc = con.responseCode
            con.disconnect()
            if (rc in 200..299) null else "relay answered $rc"
        } catch (e: Exception) { e.message ?: "network error" }
    }
}
