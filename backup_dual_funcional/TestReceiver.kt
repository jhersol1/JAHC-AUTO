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
                android.util.Log.d("TestReceiver", "=== TEST DUAL START ===")
                val testSchedule = com.jahc.auto.data.Schedule(
                    id = -1,
                    contactName = "Jhersol2.0",
                    phone = "904869774",
                    target = "whatsapp_dual",
                    message = "hola",
                    hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    minute = Calendar.getInstance().get(Calendar.MINUTE)
                )
                com.jahc.auto.service.AutoAccessibilityService.pendingSchedule = testSchedule
                android.util.Log.d("TestReceiver", "Set pendingSchedule: dual 904869774 'hola'")
                val wakeIntent = Intent(context, com.jahc.auto.ui.WakeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(wakeIntent)
                android.util.Log.d("TestReceiver", "WakeActivity launched")
            } catch (e: Exception) {
                android.util.Log.e("TestReceiver", "err", e)
            } finally { pending.finish() }
        }.start()
    }
}
