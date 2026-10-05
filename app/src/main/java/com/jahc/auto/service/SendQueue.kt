package com.jahc.auto.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jahc.auto.data.AppDatabase
import com.jahc.auto.data.Schedule
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * FIFO + mutex: solo un mensaje controla WhatsApp a la vez.
 * Los receivers encolan; la máquina consume de a uno.
 * Dedup por id: evita doble envío entre AlarmReceiver y WorkManager.
 */
object SendQueue {
    private const val TAG = "SendQueue"
    private val queue = ConcurrentLinkedQueue<Schedule>()
    private val processing = AtomicBoolean(false)
    @Volatile var current: Schedule? = null
    private val finishedAt = ConcurrentHashMap<Long, Long>()
    // Ventana corta: solo cubre la carrera AlarmManager+WorkManager del MISMO
    // disparo (segundos). Una reprogramación legítima llega minutos después.
    private const val FINISH_MEMORY_MS = 45_000L

    fun enqueue(sch: Schedule): Boolean {
        val now = System.currentTimeMillis()
        finishedAt.entries.removeIf { now - it.value > FINISH_MEMORY_MS }
        val cur = current
        if (cur != null && cur.id == sch.id) {
            android.util.Log.d(TAG, "[QUEUE] duplicado descartado id=${sch.id} (ya en proceso)")
            return false
        }
        if (queue.any { it.id == sch.id }) {
            android.util.Log.d(TAG, "[QUEUE] duplicado descartado id=${sch.id} (ya en cola)")
            return false
        }
        if (finishedAt.containsKey(sch.id)) {
            android.util.Log.d(TAG, "[QUEUE] duplicado descartado id=${sch.id} (disparo repetido <45s)")
            return false
        }
        queue.add(sch)
        android.util.Log.d(TAG, "[QUEUE] ejecución aceptada id=${sch.id} to=${sch.contactName} phone=${sch.phone} size=${queue.size}")
        return true
    }

    fun pendingCount(): Int = queue.size + (if (processing.get()) 1 else 0)

    fun pumpIfIdle(svc: AutoAccessibilityService) {
        if (BusinessSendMachine.active) return
        if (AutoAccessibilityService.pendingSchedule == null) {
            // Revalidar al sacar: por si entró un duplicado entre threads.
            var next: Schedule? = null
            var guard = 0
            while (next == null && guard++ < 8) {
                val cand = queue.poll() ?: break
                if (cand.id == current?.id || finishedAt.containsKey(cand.id)) {
                    android.util.Log.d(TAG, "[QUEUE] duplicado descartado id=${cand.id} al sacar")
                    continue
                }
                next = cand
            }
            if (next == null) return
            AutoAccessibilityService.pendingSchedule = next
            android.util.Log.d(TAG, "[QUEUE] dequeue id=${next.id} remaining=${queue.size}")
        }
        val sch = AutoAccessibilityService.pendingSchedule ?: return
        if (processing.compareAndSet(false, true)) {
            current = sch
            val total = pendingCount()
            android.util.Log.d(TAG, "[QUEUE] next execution id=${sch.id} (${total - 1} waiting)")
            android.util.Log.d(TAG, "[QUEUE] processing id=${sch.id} (${total - 1} waiting)")
            BusinessSendMachine.start(svc, sch)
        }
    }

    fun finish(svc: AutoAccessibilityService, sch: Schedule, success: Boolean, reason: String?) {
        try {
            svc.showResultNotification(success, sch, reason)
            if (success && sch.id > 0) {
                Thread {
                    try {
                        val db = AppDatabase.get(svc.applicationContext)
                        runBlocking { db.scheduleDao().markSent(sch.id, System.currentTimeMillis()) }
                    } catch (_: Exception) {}
                }.start()
            }
        } catch (_: Exception) {}
        try { svc.clearPendingPrefs() } catch (_: Exception) {}
        if (AutoAccessibilityService.pendingSchedule?.id == sch.id) AutoAccessibilityService.pendingSchedule = null
        finishedAt[sch.id] = System.currentTimeMillis()
        current = null
        processing.set(false)
        android.util.Log.d(TAG, "[QUEUE] finish id=${sch.id} ${if (success) "SENT" else "FAILED($reason)"} remaining=${queue.size}")
        pumpIfIdle(svc)
    }

    /** Fin UNCERTAIN: libera ya, sin marcar enviado ni bloquear reintentos. */
    fun finishUncertain(svc: AutoAccessibilityService, sch: Schedule, reason: String?) {
        try {
            svc.showResultNotification(false, sch, reason, uncertain = true)
        } catch (_: Exception) {}
        try { svc.clearPendingPrefs() } catch (_: Exception) {}
        if (AutoAccessibilityService.pendingSchedule?.id == sch.id) AutoAccessibilityService.pendingSchedule = null
        current = null
        processing.set(false)
        android.util.Log.d(TAG, "[QUEUE] finish id=${sch.id} UNCERTAIN($reason) remaining=${queue.size}")
        pumpIfIdle(svc)
    }

    /** Lleva WhatsApp al frente. Bloqueado: setAlarmClock (whitelisted BAL). Desbloqueado: directo. */
    fun openWhatsApp(svc: AutoAccessibilityService) {
        try {
            val locked = try {
                (svc.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager).isKeyguardLocked
            } catch (_: Exception) { false }
            val wakeIntent = Intent(svc, com.jahc.auto.ui.WakeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (locked) {
                val sch = current
                val am = svc.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val reqCode = 9000 + ((sch?.id ?: 0) % 1000).toInt()
                val pi = PendingIntent.getActivity(
                    svc, reqCode, wakeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 600, pi), pi)
                android.util.Log.d(TAG, "[QUEUE] WakeActivity via setAlarmClock (locked)")
            } else {
                svc.startActivity(wakeIntent)
                android.util.Log.d(TAG, "[QUEUE] WakeActivity direct (unlocked)")
            }
        } catch (e: Exception) {
            android.util.Log.d(TAG, "[QUEUE] openWhatsApp err ${e.message}")
        }
    }
}
