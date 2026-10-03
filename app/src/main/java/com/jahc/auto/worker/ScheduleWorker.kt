package com.jahc.auto.worker

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.jahc.auto.data.AppDatabase
import com.jahc.auto.data.Schedule
import com.jahc.auto.service.AutoAccessibilityService
import java.util.concurrent.TimeUnit

class ScheduleWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong("scheduleId", -1)
        if (id == -1L) return Result.failure()
        val db = AppDatabase.get(applicationContext)
        val sch = db.scheduleDao().getById(id) ?: return Result.failure()
        if (!sch.enabled) return Result.success()

        // Despierta el cel si está bloqueado
        try {
            val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val wl = pm.newWakeLock(android.os.PowerManager.FULL_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or android.os.PowerManager.ON_AFTER_RELEASE, "JAHC:AutoWake")
            wl.acquire(10000)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ try { wl.release() } catch (_: Exception) {} }, 10000)
        } catch (_: Exception) {}

        // Encolar para envío (un mensaje a la vez, con reintentos y recovery)
        com.jahc.auto.service.SendQueue.enqueue(sch)
        try {
            com.jahc.auto.service.AutoAccessibilityService.instance?.let {
                com.jahc.auto.service.SendQueue.pumpIfIdle(it)
            }
        } catch (_: Exception) {}

        // Reprograma para mañana (el markSent real lo hace la cola al confirmar SENT)
        scheduleNext(applicationContext, sch)
        return Result.success()
    }

    companion object {
        fun scheduleNext(ctx: Context, sch: Schedule) {
            if (!sch.repeatDaily) return
            val now = java.util.Calendar.getInstance()
            val next = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, sch.hour)
                set(java.util.Calendar.MINUTE, sch.minute)
                set(java.util.Calendar.SECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            val delay = next.timeInMillis - now.timeInMillis
            val data = workDataOf("scheduleId" to sch.id)
            val req = OneTimeWorkRequestBuilder<ScheduleWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("jahc_${sch.id}")
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("jahc_${sch.id}", ExistingWorkPolicy.REPLACE, req)
            // Backup con AlarmManager exacto para cuando está bloqueado/Doze
            try {
                val am = ctx.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val intent = android.content.Intent(ctx, AlarmReceiver::class.java).apply { putExtra("scheduleId", sch.id) }
                val pi = android.app.PendingIntent.getBroadcast(ctx, sch.id.toInt(), intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                // setAlarmClock es más confiable en Xiaomi/AOD que setExactAndAllowWhileIdle
                val info = android.app.AlarmManager.AlarmClockInfo(next.timeInMillis, pi)
                try { am.setAlarmClock(info, pi) } catch (_: Exception) { am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, next.timeInMillis, pi) }
            } catch (_: Exception) {}
        }

        fun cancel(ctx: Context, id: Long) {
            WorkManager.getInstance(ctx).cancelUniqueWork("jahc_$id")
        }
    }
}
