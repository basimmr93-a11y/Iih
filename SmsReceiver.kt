package com.ivac.otpautofill

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlin.concurrent.thread

/** Forwards only SMS that look like IVAC login codes. Everything else is ignored and never stored. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION || !Store.paired(context)) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val sender = msgs.firstOrNull()?.originatingAddress ?: ""
        val body = msgs.joinToString("") { it.messageBody ?: "" }
        if (!sender.contains("ivac", true) && !body.contains("ivac", true)) return
        val code = Relay.extractCode(body) ?: return
        val pending = goAsync()
        thread {
            val err = Relay.push(context, code)
            Store.log(context, if (err == null) "Forwarded $code" else "Failed ($err) — code $code")
            pending.finish()
        }
    }
}
