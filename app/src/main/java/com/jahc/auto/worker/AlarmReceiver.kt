package com.jahc.auto.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("scheduleId", -1)
        if (id == -1L) return
        // Intento directo sin WorkManager para evitar throttling en pruebas rápidas
        try {
            val pending = goAsync()
            Thread {
                try {
                    val db = com.jahc.auto.data.AppDatabase.get(context)
                    val sch = kotlinx.coroutines.runBlocking { db.scheduleDao().getById(id) } ?: return@Thread
                    if (!sch.enabled) return@Thread
                    // Encolar: la cola garantiza un mensaje a la vez (nunca sobrescribir)
                    com.jahc.auto.service.SendQueue.enqueue(sch)
                    val svc = com.jahc.auto.service.AutoAccessibilityService.instance
                    if (svc != null) {
                        com.jahc.auto.service.SendQueue.pumpIfIdle(svc)
                    } else {
                        val wakeIntent = Intent(context, com.jahc.auto.ui.WakeActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        try { context.startActivity(wakeIntent) } catch (_: Exception) {}
                    }
                    // También encola WorkManager como respaldo
                    val data = workDataOf("scheduleId" to id)
                    val req = OneTimeWorkRequestBuilder<ScheduleWorker>().setInputData(data).addTag("jahc_$id").build()
                    try { WorkManager.getInstance(context).enqueue(req) } catch (_: Exception) {}
                } catch (_: Exception) {} finally { pending.finish() }
            }.start()
        } catch (_: Exception) {
            val data = workDataOf("scheduleId" to id)
            val req = OneTimeWorkRequestBuilder<ScheduleWorker>().setInputData(data).addTag("jahc_$id").build()
            try { WorkManager.getInstance(context).enqueue(req) } catch (_: Exception) {}
        }
    }
}
