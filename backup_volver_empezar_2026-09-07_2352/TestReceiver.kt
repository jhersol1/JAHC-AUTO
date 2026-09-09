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
                val testSchedule = com.jahc.auto.data.Schedule(
                    id = -1,
                    contactName = "Jhersol2.0",
                    phone = "904869774",
                    target = target,
                    message = "hola",
                    hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    minute = Calendar.getInstance().get(Calendar.MINUTE)
                )
                com.jahc.auto.service.AutoAccessibilityService.pendingSchedule = testSchedule
                try {
                    val prefs = context.getSharedPreferences("jahc_auto", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("pending_target", target).putString("pending_phone", "904869774").putString("pending_contact", "Jhersol2.0").putString("pending_msg", "hola").putLong("pending_id", -1).apply()
                } catch (e: Exception) { android.util.Log.e("TestReceiver", "prefs err", e) }
                android.util.Log.d("TestReceiver", "Set pendingSchedule: $target 904869774 'hola'")
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
            } catch (e: Exception) {
                android.util.Log.e("TestReceiver", "err", e)
            } finally { pending.finish() }
        }.start()
    }
}
