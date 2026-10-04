package com.jahc.auto.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jahc.auto.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class TestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                android.util.Log.d("TestReceiver", "=== TEST START ===")
                val target = intent.getStringExtra("target") ?: "whatsapp_dual"
                val msg = intent.getStringExtra("message") ?: "hola"
                // sid opcional: mismo sid 2 veces seguidas prueba el dedup (una sola ejecución).
                val sid = intent.getLongExtra("sid", -System.currentTimeMillis())
                val testSchedule = com.jahc.auto.data.Schedule(
                    id = sid,
                    contactName = "Jhersol2.0",
                    phone = "991004829",
                    target = target,
                    message = msg,
                    hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    minute = Calendar.getInstance().get(Calendar.MINUTE)
                )
                try {
                    val prefs = context.getSharedPreferences("jahc_auto", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("pending_target", target).putString("pending_phone", "991004829").putString("pending_contact", "Jhersol2.0").putString("pending_msg", msg).putLong("pending_id", testSchedule.id).putLong("pending_ts", System.currentTimeMillis()).apply()
                } catch (e: Exception) { android.util.Log.e("TestReceiver", "prefs err", e) }
                android.util.Log.d("TestReceiver", "Enqueue test: $target 991004829 '$msg' id=${testSchedule.id}")
                com.jahc.auto.service.SendQueue.enqueue(testSchedule)
                val svc = com.jahc.auto.service.AutoAccessibilityService.instance
                if (svc != null) {
                    com.jahc.auto.service.SendQueue.pumpIfIdle(svc)
                } else {
                    // Servicio aún no conectado: despertar igual, pumpIfIdle corre con el primer evento
                    val wakeIntent = Intent(context, com.jahc.auto.ui.WakeActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                try {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    val pi = android.app.PendingIntent.getActivity(context, 9999, wakeIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                    val info = android.app.AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 500, pi)
                    am.setAlarmClock(info, pi)
                    android.util.Log.d("TestReceiver", "WakeActivity via setAlarmClock whitelisted")
                } catch (_: Exception) {
                    context.startActivity(wakeIntent)
                    android.util.Log.d("TestReceiver", "WakeActivity launched fallback")
                }
                }
            } catch (e: Exception) {
                android.util.Log.e("TestReceiver", "err", e)
            } finally { pending.finish() }
        }.start()
    }
}
