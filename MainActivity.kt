package com.ivac.otpautofill

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var phone: EditText
    private lateinit var key: EditText
    private lateinit var relay: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        phone = findViewById(R.id.phone); key = findViewById(R.id.key); relay = findViewById(R.id.relay)
        phone.setText(Store.phone(this)); key.setText(Store.key(this)); relay.setText(Store.relay(this))

        findViewById<Button>(R.id.save).setOnClickListener {
            Store.save(this, phone.text.toString(), key.text.toString(), relay.text.toString())
            if (!Store.paired(this)) toast("Enter the client key and an https:// relay URL") else ensureSms()
            refresh()
        }
        findViewById<Button>(R.id.forward).setOnClickListener {
            val code = Relay.extractCode(findViewById<EditText>(R.id.manual).text.toString())
            if (code == null) toast("No 4–8 digit code found") else send(code)
        }
        findViewById<Button>(R.id.test).setOnClickListener { send("000000") }
        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun send(code: String) {
        thread {
            val err = Relay.push(this, code)
            Store.log(this, if (err == null) "Sent $code" else "Failed ($err)")
            runOnUiThread { toast(if (err == null) "Sent" else "Failed: $err"); refresh() }
        }
    }

    private fun ensureSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 1)
    }
    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) { super.onRequestPermissionsResult(r, p, g); refresh() }

    private fun refresh() {
        val sms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val s = findViewById<TextView>(R.id.status); val sub = findViewById<TextView>(R.id.statusSub)
        when {
            !Store.paired(this) -> { s.text = "Not paired yet"; sub.text = "Save the pairing below." }
            !sms -> { s.text = "SMS access needed"; sub.text = "Tap Save pairing and allow SMS permission." }
            else -> { s.text = "Ready — listening for OTP"; sub.text = "New IVAC codes are forwarded automatically." }
        }
        findViewById<TextView>(R.id.log).text = Store.logText(this).ifBlank { "Nothing yet — forwarded codes will appear here." }
    }
    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()
}
