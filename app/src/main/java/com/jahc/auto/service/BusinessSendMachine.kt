package com.jahc.auto.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityNodeInfo
import com.jahc.auto.data.AppDatabase
import com.jahc.auto.data.Schedule

enum class SendState {
    IDLE,
    UNLOCKING,
    WAITING_FOR_DEVICE_READY,
    OPENING_WHATSAPP,
    WAITING_FOR_WHATSAPP,
    VERIFYING_WHATSAPP,
    OPENING_SEARCH,
    WAITING_FOR_SEARCH,
    SEARCHING_CONTACT,
    VERIFYING_CONTACT,
    OPENING_CHAT,
    VERIFYING_CHAT,
    TYPING_MESSAGE,
    VERIFYING_MESSAGE,
    SENDING,
    VERIFYING_SENT,
    REVERIFY_SENT,
    CLEANUP,
    READY_FOR_NEXT_MESSAGE,
    FAILED
}

private enum class Screen { LOCK, PICKER, SEARCH, CHAT, OTHER_WA, BACKGROUND, UNKNOWN }

/**
 * Máquina de estados para UN mensaje de WhatsApp Business.
 * Avanza solo por verificación real de UI (poll 400ms + eventos),
 * nunca por delays fijos. Reintentos 3x con recovery, sin duplicar envíos.
 */
object BusinessSendMachine {
    private const val TAG = "BusinessSendMachine"
    private const val POLL_MS = 400L
    // Presupuesto GLOBAL de recovery por ejecución: pertenece a toda la corrida,
    // no se reinicia al cambiar de estado.
    private const val MAX_RECOVERY = 9
    private var recoveryCount = 0

    @Volatile var active = false
    private var sch: Schedule? = null
    private var state = SendState.IDLE
    private var entered = false
    private var stateSince = 0L
    private var openedWhatsApp = false
    private var sentClicked = false
    private var sendClicks = 0
    private var searchTyped = false
    private var contactClicked = false
    private var stableCount = 0
    private var lastTick = 0L
    private var lastEventAt = 0L
    private var lastEvType = -1
    private var lastEvPkg: String? = null
    private var lastEvCls: String? = null
    private var awaitingPostTap = false
    private var droughtLogged = false
    // Pendiente de confirmación diferida (un solo slot).
    private var pendId: Long = -1
    private var pendPhone = ""
    private var pendContact = ""
    private var pendMsg = ""
    private var pendTs = 0L
    private var postTapEvLogged = false
    private var failReason: String? = null
    private var jobStart = 0L
    private var sendArmed = false
    private var sendArmedAt = 0L
    private var armedRect: android.graphics.Rect? = null
    private var armStable = 0
    private const val OVERALL_TIMEOUT_MS = 150000L
    private var bubblesBeforeSend = -1
    private var aceptarHandled = false
    private var tapAt = 0L
    private var reverifyDone = false
    private var reverifyPhase = 0
    private var reverifyAt = 0L
    private var emptyStreak = 0
    private var verifyStuckSince = 0L
    private var verifyStirred = false
    private var staleStreak = 0
    private var postReopenAt = 0L
    private var rowClickAt = 0L
    private var pendingLogTick = 0
    private var verifyPollCount = 0
    private var entrySeenFull = false
    private var lastWinSig: String? = null
    private var winDumpedVisit = false
    // CASO 1 (arrancó bloqueado) vs CASO 2 (usuario en otra app).
    private var startedLocked = false
    private var prevPkg: String? = null
    // WakeLock estrictamente por ejecución: se adquiere en start(), se libera
    // en stop() (todos los finales pasan por ahí). Nunca permanente.
    @Volatile private var wakeLock: PowerManager.WakeLock? = null
    private var wakeStart = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null

    fun isProcessing(id: Long): Boolean = active && sch?.id == id

    fun start(svc: AutoAccessibilityService, s: Schedule) {
        if (active) return
        active = true
        sch = s
        try {
            svc.getSharedPreferences("jahc_auto", android.content.Context.MODE_PRIVATE).edit()
                .putString("trace", "START@" + ((System.currentTimeMillis() / 1000) % 100000) + ";").apply()
        } catch (_: Exception) {}
        openedWhatsApp = false
        sentClicked = false
        sendClicks = 0
        searchTyped = false
        contactClicked = false
        sendArmed = false
        armedRect = null
        armStable = 0
        stableCount = 0
        failReason = null
        jobStart = System.currentTimeMillis()
        bubblesBeforeSend = -1
        aceptarHandled = false
        tapAt = 0L
        reverifyDone = false
        reverifyPhase = 0
        reverifyAt = 0L
        emptyStreak = 0
        verifyStuckSince = 0L
        verifyStirred = false
        staleStreak = 0
        postReopenAt = 0L
        pendingLogTick = 0
        verifyPollCount = 0
        entrySeenFull = false
        postTapEvLogged = false
        awaitingPostTap = false
        droughtLogged = false
        loadPending(svc)
        winDumpedVisit = false
        awaitingPostTap = false
        lastWinSig = null
        lastEventAt = 0L
        recoveryCount = 0
        acquireWakeLock(svc)
        // Foto inicial: ¿bloqueado o usuario en otra app? Define el cleanup.
        startedLocked = isLocked(svc)
        prevPkg = if (startedLocked) null else AutoAccessibilityService.lastUserPkg
        log("[CHECK] startedLocked=$startedLocked prevPkg=$prevPkg")
        log("[QUEUE] processing id=${s.id} to=${s.contactName}")
        // Todo el drive en hilo main (árbol y gestos válidos).
        handler.post {
            if (!active || sch?.id != s.id) return@post
            advance(svc, SendState.OPENING_WHATSAPP)
            val r = Runnable {
                try { if (active) { tick(svc); handler.postDelayed(tickRunnable!!, POLL_MS) } } catch (_: Exception) {}
            }
            tickRunnable = r
            handler.postDelayed(r, POLL_MS)
        }
    }

    fun onEvent(svc: AutoAccessibilityService) {
        if (!active) return
        lastEventAt = System.currentTimeMillis()
        val now = lastEventAt
        if (now - lastTick < 250) return
        tick(svc)
    }

    fun onRawEvent(event: android.view.accessibility.AccessibilityEvent?) {
        if (!active) return
        lastEventAt = System.currentTimeMillis()
        lastEvType = event?.eventType ?: -1
        lastEvPkg = event?.packageName?.toString()
        try { lastEvCls = event?.className?.toString() } catch (_: Exception) {}
    }

    fun stop() {
        active = false
        releaseWakeLock()
        try { tickRunnable?.let { handler.removeCallbacks(it) } } catch (_: Exception) {}
        tickRunnable = null
        state = SendState.IDLE
    }

    private fun acquireWakeLock(svc: AutoAccessibilityService) {
        try {
            releaseWakeLock()
            val pm = svc.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "JAHC:SendMachine"
            )
            wl.acquire(170000L)
            wakeLock = wl
            wakeStart = System.currentTimeMillis()
            log("[WAKE] acquire (FULL, cap 170s)")
        } catch (e: Exception) {
            log("[WAKE] acquire err ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            val wl = wakeLock
            wakeLock = null
            if (wl != null && wl.isHeld) {
                wl.release()
                log("[WAKE] released held=${System.currentTimeMillis() - wakeStart}ms")
            }
        } catch (e: Exception) {
            log("[WAKE] release err ${e.message}")
        }
    }

    // ---------- core ----------

    private val traceLock = Any()

    private fun mark(svc: AutoAccessibilityService, s: String) {
        try {
            synchronized(traceLock) {
                val p = svc.getSharedPreferences("jahc_auto", android.content.Context.MODE_PRIVATE)
                val prev = p.getString("trace", "") ?: ""
                val sec = (System.currentTimeMillis() / 1000) % 100000
                var t = prev + s + "@" + sec + ";"
                if (t.length > 4000) t = t.takeLast(4000)
                p.edit().putString("trace", t).apply()
            }
        } catch (_: Exception) {}
    }

    private fun advance(svc: AutoAccessibilityService, s: SendState) {
        state = s
        entered = false
        stateSince = System.currentTimeMillis()
        // recoveryCount NO se resetea: presupuesto global de la ejecución.
        log("[STATE] $s")
        mark(svc, s.name)
        tick(svc)
    }

    private fun retry(svc: AutoAccessibilityService, why: String) {
        recoveryCount++
        if (recoveryCount > MAX_RECOVERY) {
            log("[RECOVERY] limit reached ($MAX_RECOVERY)")
            fail(svc, "RECOVERY_LIMIT_REACHED at $state ($why)")
            return
        }
        log("[ERROR] $state timeout ($why)")
        log("[RECOVERY] attempt $recoveryCount/$MAX_RECOVERY")
        entered = false
        stateSince = System.currentTimeMillis()
        recover(svc)
    }

    private fun fail(svc: AutoAccessibilityService, reason: String) {
        // Con tap válido no hay FAILED: hay UNCERTAIN (verificación pendiente).
        if (sentClicked) {
            uncertain(svc, reason)
            return
        }
        // Distinguir: fallo de envío vs falta de evidencia de verificación.
        val kind = when {
            state == SendState.UNLOCKING || state == SendState.WAITING_FOR_DEVICE_READY -> reason
            else -> "SEND_FAILED: $reason"
        }
        failReason = kind
        log("[ERROR] FAILED id=${sch?.id} reason=$kind")
        log("[FAILED] $kind")
        log("[VERIFY] FAILED")
        state = SendState.FAILED
        navigateBack(svc)
        try { BusinessFlowHandler.reset() } catch (_: Exception) {}
        finish(svc, false, kind)
    }

    /** Tap válido sin evidencia: UNCERTAIN (libera ya, confirma después). */
    private fun uncertain(svc: AutoAccessibilityService, reason: String) {
        val s = sch
        log("[VERIFY] UNCERTAIN id=${s?.id} reason=$reason")
        if (s != null) savePending(svc, s)
        state = SendState.FAILED
        mark(svc, "UNCERTAIN")
        var traceSnap = ""
        try {
            synchronized(traceLock) {
                traceSnap = svc.getSharedPreferences("jahc_auto", android.content.Context.MODE_PRIVATE)
                    .getString("trace", "") ?: ""
            }
        } catch (_: Exception) {}
        navigateBack(svc)
        try { BusinessFlowHandler.reset() } catch (_: Exception) {}
        stop()
        if (s != null) SendQueue.finishUncertain(svc, s, reason)
        try {
            svc.getSharedPreferences("jahc_auto", android.content.Context.MODE_PRIVATE).edit()
                .putString("last_run", "UNCERTAIN id=${s?.id} reason=$reason locked=$startedLocked prev=$prevPkg ts=${System.currentTimeMillis()}")
                .putString("last_trace", traceSnap.takeLast(900))
                .apply()
        } catch (_: Exception) {}
    }

    private fun navMark(svc: AutoAccessibilityService, s: String) {
        try {
            svc.getSharedPreferences("jahc_auto", android.content.Context.MODE_PRIVATE).edit()
                .putString("last_nav", "$s ts=${System.currentTimeMillis()}").apply()
        } catch (_: Exception) {}
    }

    /**
     * CASO 1 (empezó bloqueado): BACK/HOME + matar WhatsApp; la pantalla se
     * apaga sola por timeout (sin wakelock). Nunca se bloquea por código.
     * CASO 2 (usuario en otra app): volver a esa app, nunca HOME a ciegas.
     */
    private fun navigateBack(svc: AutoAccessibilityService) {
        try {
            svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            handler.postDelayed({ try { svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) } catch (_: Exception) {} }, 800)
            val homePkgs = setOf(
                "com.miui.home", "com.google.android.apps.nexuslauncher",
                "android", "com.android.systemui"
            )
            val target = prevPkg
            if (!startedLocked && target != null && target !in homePkgs &&
                target != "com.whatsapp.w4b" && target != "com.whatsapp" && target != "com.jahc.auto"
            ) {
                // CASO 2: salir con BACKs hacia la app anterior (el launch intent
                // no resuelve chrome en este equipo). Verificar y solo si no
                // volvió, intentar relanzar; HOME solo como último recurso.
                log("[CHECK] cleanup caso2, back to $target")
                handler.postDelayed({
                    try {
                        val fg = try { activeRoot(svc)?.packageName?.toString() } catch (_: Exception) { null }
                        if (fg == target) {
                            navMark(svc, "ok-back $target")
                            log("[CHECK] back to prev=$target via BACK")
                        } else {
                            val li = try { svc.packageManager.getLaunchIntentForPackage(target) } catch (_: Exception) { null }
                            if (li != null) {
                                li.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                svc.startActivity(li)
                                navMark(svc, "ok-relaunch $target")
                                log("[CHECK] back to prev=$target via relaunch")
                            } else {
                                navMark(svc, "stuck fg=$fg, home fallback")
                                svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                            }
                        }
                    } catch (e: Exception) {
                        navMark(svc, "err $target ${e.message}")
                        try { svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) } catch (_: Exception) {}
                    }
                }, 2500)
            } else {
                handler.postDelayed({ try { svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) } catch (_: Exception) {} }, 1500)
                if (startedLocked) log("[CHECK] caso1: home + kill, screen sleeps alone")
            }
            handler.postDelayed({
                try {
                    val am = svc.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    am.killBackgroundProcesses("com.whatsapp.w4b")
                    am.killBackgroundProcesses("com.whatsapp")
                } catch (_: Exception) {}
            }, 4000)
        } catch (_: Exception) {}
    }

    private fun savePending(svc: AutoAccessibilityService, s: Schedule) {
        pendId = s.id
        pendPhone = s.phone.filter { it.isDigit() }
        pendContact = s.contactName
        pendMsg = s.message.trim()
        pendTs = System.currentTimeMillis()
        try {
            svc.getSharedPreferences("jahc_verify", android.content.Context.MODE_PRIVATE).edit()
                .putLong("id", pendId).putString("phone", pendPhone)
                .putString("contact", pendContact).putString("msg", pendMsg)
                .putLong("ts", pendTs).apply()
        } catch (_: Exception) {}
        log("[VERIFY] UNCERTAIN id=${s.id} msg='$pendMsg' saved for next run")
    }

    private fun loadPending(svc: AutoAccessibilityService) {
        pendId = -1; pendPhone = ""; pendContact = ""; pendMsg = ""; pendTs = 0L
        try {
            val p = svc.getSharedPreferences("jahc_verify", android.content.Context.MODE_PRIVATE)
            val ts = p.getLong("ts", 0)
            if (ts > 0 && System.currentTimeMillis() - ts < 24 * 3600 * 1000L) {
                pendId = p.getLong("id", -1)
                pendPhone = p.getString("phone", "") ?: ""
                pendContact = p.getString("contact", "") ?: ""
                pendMsg = p.getString("msg", "") ?: ""
                pendTs = ts
                if (pendId != -1L && pendMsg.isNotEmpty()) {
                    log("[VERIFY] pending loaded id=$pendId msg='$pendMsg'")
                    return
                }
            }
        } catch (_: Exception) {}
        pendId = -1; pendPhone = ""; pendContact = ""; pendMsg = ""; pendTs = 0L
    }

    private fun clearPending(svc: AutoAccessibilityService) {
        pendId = -1; pendPhone = ""; pendContact = ""; pendMsg = ""; pendTs = 0L
        try { svc.getSharedPreferences("jahc_verify", android.content.Context.MODE_PRIVATE).edit().clear().apply() } catch (_: Exception) {}
    }

    /** Intento de confirmación diferida: solo mismo número, solo lectura. */
    private fun tryConfirmPending(svc: AutoAccessibilityService) {
        if (pendId == -1L || pendMsg.isEmpty()) return
        val s = sch ?: return
        if (s.phone.filter { it.isDigit() } != pendPhone) return
        val root = try { svc.rootInActiveWindow } catch (_: Exception) { null } ?: return
        var found = false
        try {
            val stack = ArrayDeque<AccessibilityNodeInfo>()
            stack.add(root)
            var guard = 0
            while (stack.isNotEmpty() && guard++ < 2500 && !found) {
                val n = stack.removeLast()
                try {
                    val cls = n.className?.toString() ?: ""
                    if (!cls.contains("EditText") && n.text?.toString() == pendMsg) found = true
                    for (i in 0 until n.childCount) n.getChild(i)?.let { stack.add(it) }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        if (!found) return
        log("[VERIFY] CONFIRMED id=$pendId msg='$pendMsg'")
        mark(svc, "CONFIRMED")
        if (pendId > 0) {
            val markId = pendId
            Thread {
                try {
                    val db = AppDatabase.get(svc.applicationContext)
                    kotlinx.coroutines.runBlocking { db.scheduleDao().markSent(markId, System.currentTimeMillis()) }
                } catch (_: Exception) {}
            }.start()
        }
        try {
            svc.showResultNotification(true, s.copy(contactName = pendContact.ifBlank { s.contactName }), "confirmado diferido")
        } catch (_: Exception) {}
        // showResultNotification hace clear() y borraría la traza (incluido
        // este CONFIRMED): re-marcar después para que persista en durable.
        mark(svc, "CONFIRMED")
        clearPending(svc)
    }

    private fun finish(svc: AutoAccessibilityService, success: Boolean, reason: String?) {
        val s = sch
        val locked = startedLocked
        val prev = prevPkg
        var traceSnap = ""
        try {
            synchronized(traceLock) {
                traceSnap = svc.getSharedPreferences("jahc_auto", android.content.Context.MODE_PRIVATE)
                    .getString("trace", "") ?: ""
            }
        } catch (_: Exception) {}
        stop()
        if (s != null) SendQueue.finish(svc, s, success, reason)
        // Resumen durable (logcat del equipo es diminuto y rota).
        try {
            svc.getSharedPreferences("jahc_auto", android.content.Context.MODE_PRIVATE).edit()
                .putString("last_run", "${if (success) "SENT" else "FAILED"} id=${s?.id} reason=${reason ?: "-"} locked=$locked prev=$prev ts=${System.currentTimeMillis()}")
                .putString("last_trace", traceSnap.takeLast(900))
                .apply()
        } catch (_: Exception) {}
    }

    /** Recovery real: unlock -> foreground -> detectar pantalla -> retomar. Nunca sleep+ciego. */
    private fun recover(svc: AutoAccessibilityService) {
        if (isLocked(svc)) {
            log("[RECOVERY] device locked, waiting unlock")
            state = SendState.UNLOCKING
            entered = false
            stateSince = System.currentTimeMillis()
            return
        }
        val scr = detectScreen(svc)
        log("[RECOVERY] current screen=$scr")
        when (scr) {
            Screen.CHAT -> { state = SendState.VERIFYING_CHAT }
            Screen.PICKER, Screen.SEARCH -> { state = SendState.SEARCHING_CONTACT; searchTyped = false; contactClicked = false }
            else -> {
                log("[RECOVERY] bringing WhatsApp to foreground")
                openedWhatsApp = true
                SendQueue.openWhatsApp(svc)
                state = SendState.WAITING_FOR_WHATSAPP
            }
        }
        entered = false
        stateSince = System.currentTimeMillis()
    }

    private fun tick(svc: AutoAccessibilityService) {
        if (!active) return
        if (state == SendState.IDLE) return // start() aún no corrió en main
        if (state != SendState.VERIFYING_SENT && (emptyStreak != 0 || verifyStuckSince != 0L || verifyStirred || staleStreak != 0 || verifyPollCount != 0 || winDumpedVisit)) {
            emptyStreak = 0; verifyStuckSince = 0L; verifyStirred = false; staleStreak = 0; verifyPollCount = 0; winDumpedVisit = false
        }
        val job = sch ?: run { stop(); return }
        lastTick = System.currentTimeMillis()
        // DIAG temporal: primer evento posterior al tap.
        if (active && sentClicked && tapAt > 0 && !postTapEvLogged && lastEventAt > tapAt) {
            postTapEvLogged = true
            log("[DIAG] first post-tap event type=$lastEvType pkg=$lastEvPkg dt=${lastEventAt - tapAt}ms")
        }
        // Deadline global: ningún loop entre estados puede ser infinito.
        if (lastTick - jobStart > OVERALL_TIMEOUT_MS) {
            fail(svc, "overall timeout in $state")
            return
        }
        // Si otro schedule tomó el slot, abortar sin tocar nada (la cola manda).
        val cur = AutoAccessibilityService.pendingSchedule
        if (cur != null && cur.id != job.id) {
            log("[STATE] slot taken by id=${cur.id}, aborting id=${job.id}")
            stop()
            return
        }
        if (!entered) {
            entered = true
            enterState(svc, state)
            if (!active) return
        }
        if (lastTick - stateSince > timeoutFor(state)) {
            if (state == SendState.UNLOCKING) {                // Desbloqueo lo hace el service con eventos; solo extender ventana.
                recoveryCount++
                if (recoveryCount > MAX_RECOVERY) {
                    log("[RECOVERY] limit reached ($MAX_RECOVERY)")
                    fail(svc, "RECOVERY_LIMIT_REACHED at UNLOCKING")
                } else {
                    log("[ERROR] UNLOCKING timeout, still waiting ($recoveryCount/$MAX_RECOVERY)")
                    stateSince = System.currentTimeMillis()
                }
                return
            }
            // Tap válido sin evidencia: UNCERTAIN rápido (sin agotar recoveries,
            // salvo flujo Aceptar que necesita más tiempo).
            if (state == SendState.VERIFYING_SENT && sentClicked && !aceptarHandled) {
                log("[VERIFY] UNCERTAIN id=${sch?.id} (tap válido sin evidencia)")
                uncertain(svc, "tap válido sin evidencia")
                return
            }
            retry(svc, "no condition")
            return
        }
        checkState(svc, state)
    }

    // ---------- actions on entry (idempotentes) ----------

    private fun enterState(svc: AutoAccessibilityService, s: SendState) {
        when (s) {
            SendState.REVERIFY_SENT -> {
                // Solo verificación del tap original. Nada de escribir ni pulsar.
                reverifyPhase = 0
                reverifyAt = 0L
            }
            SendState.OPENING_WHATSAPP -> {
                // Captura tardía en hilo main: el árbol en start() puede venir
                // vacío si pump corrió desde otro thread.
                if (prevPkg == null && !startedLocked) {
                    try {
                        val p = activeRoot(svc)?.packageName?.toString()
                        if (p != null && p != "com.whatsapp.w4b" && p != "com.whatsapp" &&
                            p != "com.jahc.auto" && p != "android" && p != "com.android.systemui"
                        ) {
                            prevPkg = p
                            log("[CHECK] prevPkg=$p")
                        }
                    } catch (_: Exception) {}
                }
                if (!openedWhatsApp) {
                    openedWhatsApp = true
                    SendQueue.openWhatsApp(svc)
                }
                advance(svc, SendState.WAITING_FOR_WHATSAPP)
            }
            SendState.OPENING_SEARCH -> {
                val root = activeRoot(svc)
                if (root != null && (svc.findButtonByText(root, "Buscar") ?: svc.findNodeWithText(root, "Buscar")) != null) {
                    log("[CHECK] search entry available=true")
                }
                advance(svc, SendState.WAITING_FOR_SEARCH)
            }
            SendState.SEARCHING_CONTACT -> {
                if (!searchTyped) {
                    searchTyped = true
                    typeSearch(svc)
                }
            }
            SendState.TYPING_MESSAGE -> {
                // Jamás reescribir después de haber tocado Send: sería duplicado.
                if (sentClicked) {
                    log("[CHECK] already tapped once, verify only (no retype)")
                    advance(svc, SendState.VERIFYING_SENT)
                    return
                }
                // Siempre escribir con SET_TEXT aunque haya prefill de wa.me:
                // el commit real por IME es lo que activa el envío.
                // Si el campo ya trae el texto, igual se registra la evidencia.
                try {
                    val want0 = sch?.message?.trim() ?: ""
                    val cur0 = entryText(svc)
                    if (cur0 == want0 && want0.isNotEmpty() && !entrySeenFull) {
                        entrySeenFull = true
                        log("[VERIFY] entrySeenFull=true")
                    }
                } catch (_: Exception) {}
                typeMessage(svc)
                // La verificación la hace VERIFYING_MESSAGE por poll; no esperar al timeout.
                advance(svc, SendState.VERIFYING_MESSAGE)
            }
            SendState.SENDING -> {
                // Solo armar; el tap real lo hace checkState con árbol fresco.
                sendArmed = false
            }
            SendState.CLEANUP -> doCleanup(svc)
            else -> {}
        }
    }

    // ---------- polling checks ----------

    private fun checkState(svc: AutoAccessibilityService, s: SendState) {
        when (s) {
            SendState.UNLOCKING -> {
                if (isUnlocked(svc)) { log("[CHECK] device unlocked=true"); advance(svc, SendState.WAITING_FOR_DEVICE_READY) }
            }
            SendState.WAITING_FOR_DEVICE_READY -> {
                if (isUnlocked(svc)) { log("[CHECK] device ready=true"); advance(svc, SendState.OPENING_WHATSAPP) }
            }
            SendState.WAITING_FOR_WHATSAPP -> {
                if (isLocked(svc)) {
                    log("[CHECK] locked during wait, unlocking first")
                    advance(svc, SendState.UNLOCKING)
                } else if (isWhatsAppForeground(svc)) { log("[CHECK] whatsapp foreground=true"); stableCount = 0; advance(svc, SendState.VERIFYING_WHATSAPP) }
            }
            SendState.VERIFYING_WHATSAPP -> {
                if (isLocked(svc)) { stableCount = 0; advance(svc, SendState.UNLOCKING); return }
                if (!isWhatsAppForeground(svc)) { stableCount = 0; return }
                stableCount++
                if (stableCount >= 2) {
                    log("[CHECK] whatsapp stable=true")
                    routeFromWhatsApp(svc)
                }
            }
            SendState.WAITING_FOR_SEARCH -> {
                val root = activeRoot(svc) ?: return
                if (svc.findEditText(root) != null) { log("[CHECK] search field found=true"); advance(svc, SendState.SEARCHING_CONTACT) }
            }
            SendState.SEARCHING_CONTACT -> {
                if (isCorrectChat(svc)) { log("[CHECK] contact found=true"); advance(svc, SendState.VERIFYING_CONTACT); return }
                if (detectScreen(svc) == Screen.CHAT) { advance(svc, SendState.VERIFYING_CHAT); return }
                // Resultados async: click al contacto una sola vez cuando aparezca.
                if (!contactClicked) {
                    val s = sch
                    val root = activeRoot(svc)
                    if (s != null && root != null) {
                        var digits = s.phone.filter { it.isDigit() }
                        if (digits.length == 9) digits = "51$digits"
                        val key = if (digits.length > 9) digits.takeLast(9) else digits.ifBlank { s.contactName }
                        val node = if (key.isNotBlank()) {
                            svc.findNodeContainingText(root, key)
                                ?: svc.findNodeContainingText(root, s.contactName.ifBlank { key })
                        } else null
                        if (node != null) {
                            contactClicked = true
                            log("[CHECK] contact visible, click once")
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            advance(svc, SendState.OPENING_CHAT)
                        }
                    }
                }
            }
            SendState.VERIFYING_CONTACT, SendState.OPENING_CHAT -> {
                if (isCorrectChat(svc)) { log("[CHECK] correct chat=true"); advance(svc, SendState.VERIFYING_CHAT) }
            }
            SendState.VERIFYING_CHAT -> {
                val scr = detectScreen(svc)
                if (scr == Screen.CHAT && isCorrectChat(svc)) {
                    log("[CHECK] in correct chat=true")
                    tryConfirmPending(svc)
                    advance(svc, SendState.TYPING_MESSAGE)
                } else if (scr == Screen.PICKER || scr == Screen.SEARCH) {
                    log("[CHECK] fell back to picker, searching")
                    searchTyped = false; contactClicked = false
                    advance(svc, SendState.SEARCHING_CONTACT)
                } else if (isCorrectChat(svc)) {
                    // Lista/otra pantalla con el contacto visible: abrirlo con
                    // UN clic espaciado (no ráfaga), subiendo al padre clicable.
                    val nowC = System.currentTimeMillis()
                    if (!contactClicked || nowC - rowClickAt > 5000) {
                        val s = sch
                        val root = activeRoot(svc)
                        var node: AccessibilityNodeInfo? = if (s != null && root != null) {
                            val key = s.contactName.ifBlank { "" }
                            (if (key.isNotBlank()) svc.findNodeContainingText(root, key) else null)
                                ?: run {
                                    val digits = s.phone.filter { it.isDigit() }
                                    if (digits.length >= 9) svc.findNodeContainingText(root, digits.takeLast(9)) else null
                                }
                        } else null
                        var target = node
                        try {
                            while (target != null && !target.isClickable) target = target.parent
                        } catch (_: Exception) {}
                        if (target != null) {
                            contactClicked = true
                            rowClickAt = nowC
                            log("[CHECK] contact row visible, open chat once")
                            try { target.performAction(AccessibilityNodeInfo.ACTION_CLICK) } catch (_: Exception) {}
                            advance(svc, SendState.OPENING_CHAT)
                        } else {
                            searchTyped = false; contactClicked = false
                            advance(svc, SendState.SEARCHING_CONTACT)
                        }
                    }
                }
            }
            SendState.VERIFYING_MESSAGE -> {
                val cur = entryText(svc)
                val want = sch?.message?.trim() ?: ""
                if (cur == want && want.isNotEmpty()) {
                    if (!entrySeenFull) {
                        entrySeenFull = true
                        log("[VERIFY] entrySeenFull=true")
                    }
                    log("[CHECK] message entered=true"); advance(svc, SendState.SENDING)
                }
            }
            SendState.SENDING -> {
                // PROHIBIDO re-tap: un solo tap por ejecución. Si ya se tocó,
                // solo verificar (nunca duplicar).
                if (sentClicked) {
                    log("[CHECK] already tapped once, verify only (no retap)")
                    advance(svc, SendState.VERIFYING_SENT)
                    return
                }
                // Two-phase con estabilidad: el botón se mueve con el teclado
                // animado; solo tocar cuando sus bounds no cambian en 2 polls.
                val rNow = activeRoot(svc)
                val bNow = rNow?.let { svc.findSendButtonById(it) ?: svc.findSendButton(it) }
                val rr = android.graphics.Rect()
                bNow?.getBoundsInScreen(rr)
                val prev = armedRect
                if (bNow == null || rr.width() <= 0) {
                    armedRect = null; armStable = 0
                    if (!sendArmed) {
                        sendArmed = true
                        sendArmedAt = System.currentTimeMillis()
                        log("[CHECK] send armed, waiting fresh layout")
                    }
                    return
                }
                if (prev != null && kotlin.math.abs(prev.centerX() - rr.centerX()) <= 15 &&
                    kotlin.math.abs(prev.centerY() - rr.centerY()) <= 15
                ) {
                    armStable++
                } else {
                    armStable = 0
                }
                armedRect = android.graphics.Rect(rr)
                // DIAG temporal: estado por poll previo al tap.
                try {
                    val nowD = System.currentTimeMillis()
                    val winId = try { rNow?.windowId ?: -1 } catch (_: Exception) { -1 }
                    val ime = try { svc.windows?.any { it.type == 2 } } catch (_: Exception) { null }
                    log("[DIAG] send poll rect=$rr stable=$armStable ev=$lastEvType/$lastEvPkg/${nowD - lastEventAt}ms win=$winId ime=$ime")
                } catch (_: Exception) {}
                if (!sendArmed) {
                    sendArmed = true
                    sendArmedAt = System.currentTimeMillis()
                    log("[CHECK] send armed, waiting fresh layout")
                    return
                }
                if (armStable < 2) return
                sendArmed = false
                // Baseline perezoso: solo vale si hay filas cargadas. Sin filas,
                // baseline UNKNOWN (-1) y esa vía de evidencia queda vedada.
                val wantTap = sch?.message?.trim() ?: ""
                val bRoot = freshRoot(svc)
                val nRows = if (bRoot != null) countRows(bRoot) else 0
                if (attemptSendClick(svc)) {
                    sentClicked = true
                    sendClicks++
                    tapAt = System.currentTimeMillis()
                    awaitingPostTap = true
                    mark(svc, "TAP")
                    log("[VERIFY] TAP_VALID id=${sch?.id}")
                    if (!entrySeenFull) {
                        entrySeenFull = true
                        log("[VERIFY] entrySeenFull=true")
                    }
                    if (bRoot != null && nRows > 0) {
                        bubblesBeforeSend = countBubblesIn(bRoot, wantTap)
                        log("[VERIFY] rows=$nRows bubbles=$bubblesBeforeSend baseline=$bubblesBeforeSend")
                    } else {
                        bubblesBeforeSend = -1
                        log("[VERIFY] baseline=UNKNOWN")
                    }
                    log("[CHECK] bubbles before=$bubblesBeforeSend tapAt=$tapAt")
                    advance(svc, SendState.VERIFYING_SENT)
                }
            }
            SendState.VERIFYING_SENT -> {
                // DIAGNÓSTICO ventanas (solo lectura): UNA vez por visita.
                // (Cada dump son varios IPC Binder; por poll saturaba el hilo.)
                if (!winDumpedVisit) {
                    winDumpedVisit = true
                    try {
                        val wins = try { svc.windows } catch (_: Exception) { null }
                        val arNow = try { svc.rootInActiveWindow } catch (_: Exception) { null }
                        val arId = try { arNow?.windowId ?: -1 } catch (_: Exception) { -1 }
                        val arPkg = try { arNow?.packageName?.toString() } catch (_: Exception) { null }
                        val arCls = try { arNow?.className?.toString() } catch (_: Exception) { null }
                        val sig = "$arId|$arPkg|$arCls|${wins?.size ?: -1}"
                        if (sig != lastWinSig) {
                            lastWinSig = sig
                            log("[WIN] activeRoot id=$arId pkg=$arPkg class=$arCls")
                            wins?.forEach { w ->
                                try {
                                    val wr = try { w.root } catch (_: Exception) { null }
                                    val title = if (android.os.Build.VERSION.SDK_INT >= 33) {
                                        try { w.title?.toString() } catch (_: Exception) { null }
                                    } else null
                                    log("[WIN] id=${w.id} type=${w.type} pkg=${wr?.packageName} active=${w.isActive} focused=${w.isFocused} title=$title")
                                } catch (_: Exception) {}
                            }
                        }
                    } catch (_: Exception) {}
                }
                // Cadencia: 1 poll útil cada ~3 ticks (~1.2s). Escanear el árbol
                // en cada tick saturaba el hilo main y mataba la entrega de
                // eventos (lecturas cada vez más viejas). Menos es más.
                verifyPollCount++
                if (verifyPollCount % 3 != 0) return
                // Fusible post-reapertura: si tras reabrir no hay evidencia en
                // 15s, fallar acotado (sin más ciclos, sin re-tap).
                if (postReopenAt > 0 && System.currentTimeMillis() - postReopenAt > 15000) {
                    log("[VERIFY_SENT] no evidence after reopen, fail bounded")
                    fail(svc, "NO_EVIDENCE_AFTER_REOPEN")
                    return
                }
                // Guardia: solo evaluar dentro de conversación real. Si el BACK
                // nos dejó en lista/otra pantalla, esperar (timeout/recovery
                // acotan), no leer el buscador como si fuera el mensaje.
                if (detectScreen(svc) != Screen.CHAT) {
                    log("[VERIFY_SENT] not in chat, wait")
                    return
                }
                // ETAPA 1: evaluación anclada al primer evento posterior al tap
                // (>=1s para dar tiempo a la burbuja). Una sola vez por ejecución.
                if (awaitingPostTap && sentClicked && tapAt > 0 &&
                    System.currentTimeMillis() - tapAt >= 1000 &&
                    lastEventAt > tapAt
                ) {
                    awaitingPostTap = false
                    val wantEv = sch?.message?.trim() ?: ""
                    val rEv = freshRoot(svc)
                    val entryEv = try { rEv?.let { svc.findEditText(it)?.text?.toString()?.trim() } } catch (_: Exception) { null }
                    val bubEv = if (rEv != null && wantEv.isNotEmpty() && bubblesBeforeSend >= 0) countBubblesIn(rEv, wantEv) else -1
                    log("[VERIFY_SENT] anchored eval evType=$lastEvType pkg=$lastEvPkg entry='$entryEv' bubbles=$bubEv baseline=$bubblesBeforeSend")
                    mark(svc, "EV" + (if ((entryEv != null && entryEv.isEmpty()) || (bubEv >= 0 && bubEv > bubblesBeforeSend)) "1" else "0"))
                    if (entrySeenFull && ((entryEv != null && entryEv.isEmpty()) ||
                            (bubEv >= 0 && bubEv > bubblesBeforeSend))
                    ) {
                        log("[VERIFY_SENT] evidence found (anchored event, no delivery confirmation required)")
                        advance(svc, SendState.CLEANUP)
                        return
                    } else {
                        log("[VERIFY_SENT] anchored read: no evidence yet")
                    }
                }
                if (awaitingPostTap && tapAt > 0 && System.currentTimeMillis() - tapAt > 8000) {
                    awaitingPostTap = false
                    mark(svc, "NOEV")
                    log("[VERIFY_SENT] NO_POST_SEND_EVENT")
                }
                log("[VERIFY_SENT] check")
                // UNA sola lectura por poll: mismo root para todo.
                val root = freshRoot(svc)
                if (root == null) {
                    // Árbol muerto y sin eventos que lo renueven: no decidir,
                    // pero si persiste, forzar re-verificación (BACK+reabrir).
                    staleStreak++
                    log("[VERIFY_SENT] stale tree, wait fresh ($staleStreak)")
                    if (staleStreak >= 10 && !reverifyDone && sentClicked) {
                        reverifyDone = true
                        log("[VERIFY_SENT] stale too long, reverify via BACK+wa.me (once, no retap)")
                        advance(svc, SendState.REVERIFY_SENT)
                    }
                    return
                }
                staleStreak = 0
                // Puerta de eventos: sin evento reciente NO se evalúa evidencia
                // (ni SENT ni FAILED por lecturas). Los polls son solo guardia.
                val evFresh = System.currentTimeMillis() - lastEventAt < 2500
                if (!evFresh) {
                    if (!droughtLogged) {
                        droughtLogged = true
                        log("[VERIFY] NO_RECENT_EVENTS")
                    }
                    return
                }
                droughtLogged = false
                if (svc.containsText(root, "Aceptar") || svc.containsText(root, "Accept")) {
                    if (!aceptarHandled) {
                        aceptarHandled = true
                        handleAceptar(svc, root)
                    }
                    return
                }
                val editNode = try { svc.findEditText(root) } catch (_: Exception) { null }
                val cur = try { editNode?.text?.toString()?.trim() } catch (_: Exception) { null }
                if (cur == null) {
                    staleStreak++
                    log("[VERIFY_SENT] stale tree, wait fresh ($staleStreak)")
                    if (staleStreak >= 10 && !reverifyDone && sentClicked) {
                        reverifyDone = true
                        log("[VERIFY_SENT] stale too long, reverify via BACK+wa.me (once, no retap)")
                        advance(svc, SendState.REVERIFY_SENT)
                    }
                    return
                }
                staleStreak = 0
                if (cur.isEmpty()) {
                    // Estabilidad: 2 polls seguidos vacíos con árbol fresco, y
                    // solo si antes se vio el campo lleno (evita falsos SENT).
                    emptyStreak++
                    if (emptyStreak >= 2 && entrySeenFull) {
                        log("[VERIFY_SENT] evidence found (entry empty x2, no delivery confirmation required)")
                        log("[VERIFY] SENT reason=ENTRY_EMPTY")
                        advance(svc, SendState.CLEANUP)
                    }
                    return
                }
                emptyStreak = 0
                // Aunque el campo siga lleno, una burbuja nueva prueba la
                // colocación local. Los checks de entrega (✓✓) NO son requisito.
                // Requiere baseline válido + haber visto el campo lleno.
                val want = sch?.message?.trim() ?: ""
                if (want.isNotEmpty() && entrySeenFull && bubblesBeforeSend >= 0) {
                    val rowsNow = countRows(root)
                    val now = countBubblesIn(root, want)
                    log("[VERIFY] rows=$rowsNow bubbles=$now baseline=$bubblesBeforeSend")
                    if (now > bubblesBeforeSend) {
                        log("[VERIFY_SENT] evidence found (bubbles $bubblesBeforeSend->$now, no delivery confirmation required)")
                        log("[VERIFY] SENT reason=BUBBLES_INCREASE")
                        advance(svc, SendState.CLEANUP)
                        return
                    }
                }
                // Sin evidencia y sin eventos frescos el árbol puede estar
                // viejo: un toque benigno al CAMPO (nunca a Enviar) genera
                // eventos y refresca la lectura. Una sola vez por ciclo.
                if (verifyStuckSince == 0L) verifyStuckSince = System.currentTimeMillis()
                if (!verifyStirred && System.currentTimeMillis() - verifyStuckSince > 6000) {
                    verifyStirred = true
                    try {
                        val r2 = freshRoot(svc)
                        val edit = r2?.let { svc.findEditText(it) }
                        if (edit != null) {
                            val er = android.graphics.Rect(); edit.getBoundsInScreen(er)
                            if (er.width() > 0 && er.height() > 0) {
                                edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                                val path = android.graphics.Path().apply {
                                    moveTo(er.centerX().toFloat(), er.centerY().toFloat())
                                    lineTo(er.centerX() + 1f, er.centerY() + 1f)
                                }
                                val tap = GestureDescription.Builder()
                                    .addStroke(GestureDescription.StrokeDescription(path, 0, 80)).build()
                                svc.dispatchGesture(tap, null, null)
                                log("[VERIFY_SENT] stir entry tap (refresh tree, not send)")
                            }
                        }
                    } catch (_: Exception) {}
                } else {
                    pendingLogTick++
                    if (pendingLogTick % 5 == 1) log("[VERIFY_SENT] pending/no delivery confirmation")
                }
                // Sin evidencia en 8s: re-verificación determinista UNA vez por
                // ejecución (BACK + reabrir wa.me). Jamás re-tap de Enviar.
                // Además: si tras el tap no llegan eventos (árbol congelado),
                // re-verificar ya a los 3s sin eventos, sin esperar los 8s.
                val noEvents = tapAt > 0 && lastEventAt > 0 &&
                    System.currentTimeMillis() - tapAt > 3000 &&
                    System.currentTimeMillis() - lastEventAt > 3000
                if (!reverifyDone && sentClicked &&
                    (System.currentTimeMillis() - stateSince > 8000 || noEvents)
                ) {
                    reverifyDone = true
                    log("[VERIFY_SENT] no evidence, reverify via BACK+wa.me (once, no retap)")
                    advance(svc, SendState.REVERIFY_SENT)
                    return
                }
            }
            SendState.REVERIFY_SENT -> {
                // Fase de solo-verificación del tap original (tapAt). Prohibido:
                // escribir, pulsar Enviar, re-tipificar, reiniciar el envío.
                if (reverifyPhase == 0) {
                    reverifyPhase = 1
                    reverifyAt = System.currentTimeMillis()
                    log("[REVERIFY] back out of chat (tapAt=$tapAt id=${sch?.id})")
                    try { svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) } catch (_: Exception) {}
                    return
                }
                if (reverifyPhase == 1 && System.currentTimeMillis() - reverifyAt > 1200) {
                    reverifyPhase = 2
                    reverifyAt = System.currentTimeMillis()
                    reopenChat(svc)
                    return
                }
                if (reverifyPhase == 2) {
                    // Lectura anclada a evento real de ventana (árbol nuevo
                    // garantizado), no a polls ciegos. Fallback por tiempo.
                    val freshWin = lastEvType == android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                        lastEventAt > reverifyAt &&
                        (lastEvPkg == "com.whatsapp.w4b" || lastEvPkg == "com.whatsapp")
                    if (freshWin) {
                        log("[REVERIFY] fresh window event, read now")
                        mark(svc, "RE_FRESH")
                        verifyStuckSince = 0L
                        postReopenAt = System.currentTimeMillis()
                        advance(svc, SendState.VERIFYING_SENT)
                    } else if (detectScreen(svc) == Screen.CHAT ||
                        System.currentTimeMillis() - reverifyAt > 8000
                    ) {
                        log("[REVERIFY] chat reopened, fresh read")
                        verifyStuckSince = 0L
                        postReopenAt = System.currentTimeMillis()
                        advance(svc, SendState.VERIFYING_SENT)
                    }
                }
            }
            else -> {}
        }
    }

    /** Reabre la conversación del MISMO número solo para leer (nunca enviar).
     *  Vía setAlarmClock (whitelisted BAL): el startActivity directo desde
     *  segundo plano lo bloquea Android 14+ en silencio. */
    private fun reopenChat(svc: AutoAccessibilityService) {
        val s = sch ?: return
        try {
            var phone = s.phone.filter { it.isDigit() }
            if (phone.length == 9) phone = "51$phone"
            val uri = "https://wa.me/$phone"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(uri)
                `package` = targetPkg()
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            val am = svc.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val pi = android.app.PendingIntent.getActivity(
                svc, 9100 + ((s.id % 1000).toInt()),
                intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(android.app.AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 600, pi), pi)
            log("[REVERIFY] reopen wa.me $phone via setAlarmClock (read-only)")
        } catch (e: Exception) {
            log("[REVERIFY] reopen err ${e.message}")
        }
    }

    private fun routeFromWhatsApp(svc: AutoAccessibilityService) {
        when (detectScreen(svc)) {
            Screen.CHAT -> {
                if (isCorrectChat(svc)) { log("[CHECK] already in correct chat, skip search"); advance(svc, SendState.VERIFYING_CHAT) }
                else { log("[CHECK] wrong chat, searching"); searchTyped = false; contactClicked = false; advance(svc, SendState.SEARCHING_CONTACT) }
            }
            Screen.PICKER, Screen.SEARCH -> { searchTyped = false; contactClicked = false; advance(svc, SendState.SEARCHING_CONTACT) }
            else -> { searchTyped = false; contactClicked = false; advance(svc, SendState.OPENING_SEARCH) }
        }
    }

    // ---------- primitives ----------

    // ---------- selección de campos por CONTEXTO+BOUNDS (sin IDs: null en runtime) ----------

    private fun allEditTexts(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        try {
            val cls = node.className?.toString() ?: ""
            if (cls.contains("EditText")) out.add(node)
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { allEditTexts(it, out) }
            }
        } catch (_: Exception) {}
    }

    private fun editSig(n: AccessibilityNodeInfo): String {
        val cls = try { n.className?.toString() } catch (_: Exception) { null }
        val tx = try { n.text?.toString() } catch (_: Exception) { null }
        val cd = try { n.contentDescription?.toString() } catch (_: Exception) { null }
        val id = try { n.viewIdResourceName } catch (_: Exception) { null }
        val r = android.graphics.Rect()
        try { n.getBoundsInScreen(r) } catch (_: Exception) {}
        val st = try { "v=${n.isVisibleToUser} e=${n.isEnabled} c=${n.isClickable} f=${n.isFocused}" } catch (_: Exception) { "?" }
        return "type=$cls text=$tx desc=$cd viewId=$id rect=$r $st"
    }

    /** Campo de BÚSQUEDA: EditText superior (zona alta). Nunca el inferior. */
    private fun findSearchBox(svc: AutoAccessibilityService, root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val all = mutableListOf<AccessibilityNodeInfo>()
        allEditTexts(root, all)
        if (all.isEmpty()) {
            log("[EDITTEXT] SEARCH none found")
            return null
        }
        val sh = try { svc.resources.displayMetrics.heightPixels } catch (_: Exception) { 2400 }
        val top = all.minByOrNull {
            val r = android.graphics.Rect()
            try { it.getBoundsInScreen(r) } catch (_: Exception) {}
            r.centerY()
        } ?: return null
        val r = android.graphics.Rect()
        try { top.getBoundsInScreen(r) } catch (_: Exception) {}
        if (r.centerY() > sh * 0.5f) {
            log("[EDITTEXT] SEARCH rejected (not upper zone) ${editSig(top)}")
            return null
        }
        log("[EDITTEXT] SEARCH upper ok ${editSig(top)}")
        return top
    }

    /** Campo de MENSAJE: EditText inferior (zona baja). Nunca el superior. */
    private fun findMessageBox(svc: AutoAccessibilityService, root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val all = mutableListOf<AccessibilityNodeInfo>()
        allEditTexts(root, all)
        if (all.isEmpty()) {
            log("[EDITTEXT] MESSAGE none found")
            return null
        }
        val sh = try { svc.resources.displayMetrics.heightPixels } catch (_: Exception) { 2400 }
        val bottom = all.maxByOrNull {
            val r = android.graphics.Rect()
            try { it.getBoundsInScreen(r) } catch (_: Exception) {}
            r.centerY()
        } ?: return null
        val r = android.graphics.Rect()
        try { bottom.getBoundsInScreen(r) } catch (_: Exception) {}
        if (r.centerY() < sh * 0.5f) {
            log("[EDITTEXT] MESSAGE rejected (not lower zone) ${editSig(bottom)}")
            return null
        }
        log("[EDITTEXT] MESSAGE lower ok ${editSig(bottom)}")
        return bottom
    }

    private fun typeSearch(svc: AutoAccessibilityService) {
        val s = sch ?: return
        // Nunca tipear búsqueda dentro de una conversación (caería en el campo de mensaje).
        if (detectScreen(svc) == Screen.CHAT) {
            log("[CHECK] already in conversation, skip search typing")
            return
        }
        val root = activeRoot(svc) ?: return
        var digits = s.phone.filter { it.isDigit() }
        if (digits.length == 9) digits = "51$digits"
        val searchDigits = if (digits.length > 9) digits.takeLast(9) else digits.ifBlank { s.contactName }
        if (searchDigits.isBlank()) { log("[CHECK] search text empty, skip"); return }
        val buscar = svc.findButtonByText(root, "Buscar") ?: svc.findNodeWithText(root, "Buscar")
        if (buscar != null) {
            log("[STATE] click Buscar")
            buscar.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        val box = findSearchBox(svc, activeRoot(svc) ?: return)
        if (box != null) {
            val b = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, searchDigits) }
            box.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
            box.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            log("[TYPE] search geometry ok (upper) typed=$searchDigits")
            log("[CHECK] search typed=$searchDigits")
        }
        // Un solo tap al contacto cuando aparezca lo hace SEARCHING_CONTACT poll; click aquí si ya visible:
        val r2 = activeRoot(svc)
        val contact = r2?.let {
            svc.findNodeContainingText(it, searchDigits)
                ?: svc.findNodeContainingText(it, s.contactName.ifBlank { searchDigits })
        }
        if (contact != null) {
            log("[CHECK] contact visible, click")
            contact.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun typeMessage(svc: AutoAccessibilityService) {
        val s = sch ?: return
        val want = s.message.trim()
        log("[TYPE] message_length=${want.length}")
        val root = activeRoot(svc) ?: return
        val edit = findMessageBox(svc, root) ?: return
        log("[TYPE] message geometry ok (lower)")
        val before = try { edit.text?.toString()?.trim() ?: "" } catch (_: Exception) { "" }
        log("[TYPE] prefill_detected=${before == want && want.isNotEmpty()}")
        val b = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, s.message) }
        val setOk = try { edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b) } catch (_: Exception) { false }
        log("[TYPE] SET_TEXT executed=$setOk")
        try { edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS) } catch (_: Exception) {}
        try {
            val er = android.graphics.Rect(); edit.getBoundsInScreen(er)
            if (er.width() > 0 && er.height() > 0) {
                val path = Path().apply { moveTo(er.centerX().toFloat(), er.centerY().toFloat()); lineTo(er.centerX() + 1f, er.centerY() + 1f) }
                svc.dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 80)).build(), null, null)
            }
        } catch (_: Exception) {}
        val after = try { edit.text?.toString()?.trim() ?: "" } catch (_: Exception) { "" }
        log("[TYPE] field_verified=${after == want && want.isNotEmpty()}")
        log("[CHECK] message typed, focus+tap entry")
    }

    /** Recolecta nodos Send por ID de vista o descripción (sin filtrar). */
    private fun collectSendNodes(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        try {
            val id = try { node.viewIdResourceName } catch (_: Exception) { null }
            val cd = try { node.contentDescription?.toString() } catch (_: Exception) { null }
            if (id == "com.whatsapp.w4b:id/send" || (cd?.contains("Enviar", true) == true)) out.add(node)
            for (i in 0 until node.childCount) {
                try { node.getChild(i)?.let { collectSendNodes(it, out) } } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    /** Un clic real, nunca al mic con campo vacío. Devuelve true si se pulsó. */
    private fun attemptSendClick(svc: AutoAccessibilityService): Boolean {
        val root = activeRoot(svc) ?: return false
        val dm = svc.resources.displayMetrics
        val sw = dm.widthPixels; val sh = dm.heightPixels
        fun boundsOf(n: AccessibilityNodeInfo): android.graphics.Rect {
            val r = android.graphics.Rect()
            try { n.getBoundsInScreen(r) } catch (_: Exception) {}
            return r
        }
        fun flagsOf(n: AccessibilityNodeInfo): Triple<Boolean, Boolean, Boolean> {
            return try { Triple(n.isVisibleToUser, n.isEnabled, n.isClickable) } catch (_: Exception) { Triple(false, false, false) }
        }
        fun inBottom(r: android.graphics.Rect): Boolean {
            return r.width() > 0 && r.height() > 0 && r.centerX() >= 0 && r.centerX() <= sw &&
                r.centerY() > sh * 0.65f && r.centerY() <= sh
        }
        var btn: AccessibilityNodeInfo? = null
        var dropped = 0
        // Prioridad 1: helpers estrictos (viewId de Send).
        val strict = svc.findSendButtonById(root) ?: svc.findSendButton(root)
        if (strict != null) {
            val r = boundsOf(strict)
            val (vis, en, clk) = flagsOf(strict)
            val id = try { strict.viewIdResourceName } catch (_: Exception) { null }
            val cd = try { strict.contentDescription?.toString() } catch (_: Exception) { null }
            val cls = try { strict.className?.toString() } catch (_: Exception) { null }
            log("[SEND_NODE] source=id-strict type=$cls text=null desc=$cd viewId=$id visible=$vis enabled=$en clickable=$clk rect=$r")
            if (vis && en && clk && inBottom(r)) {
                btn = strict
                log("[SEND_NODE] selected=true rect=$r")
            } else {
                dropped++
                log("[SEND_NODE] selected=false")
            }
        }
        // Prioridad 2: ImageButton real en zona inferior.
        if (btn == null) {
            val pool = mutableListOf<AccessibilityNodeInfo>()
            collectSendNodes(root, pool)
            for (n in pool) {
                val cls = try { n.className?.toString() ?: "" } catch (_: Exception) { "" }
                if (!cls.contains("ImageButton") && !cls.contains("ImageView")) continue
                val r = boundsOf(n)
                val (vis, en, clk) = flagsOf(n)
                val id = try { n.viewIdResourceName } catch (_: Exception) { null }
                val cd = try { n.contentDescription?.toString() } catch (_: Exception) { null }
                log("[SEND_NODE] source=imagebutton type=$cls text=null desc=$cd viewId=$id visible=$vis enabled=$en clickable=$clk rect=$r")
                if (vis && en && clk && inBottom(r)) {
                    btn = n
                    log("[SEND_NODE] selected=true rect=$r")
                    break
                } else {
                    dropped++
                    log("[SEND_NODE] selected=false")
                }
            }
        }
        // Prioridad 3: cualquier mención Enviar, PERO solo en zona inferior.
        // Jamás un nodo de arriba/header/toolbar.
        if (btn == null) {
            val pool = mutableListOf<AccessibilityNodeInfo>()
            collectSendNodes(root, pool)
            for (n in pool) {
                val r = boundsOf(n)
                val (vis, en, clk) = flagsOf(n)
                val id = try { n.viewIdResourceName } catch (_: Exception) { null }
                val cd = try { n.contentDescription?.toString() } catch (_: Exception) { null }
                val tx = try { n.text?.toString() } catch (_: Exception) { null }
                val cls = try { n.className?.toString() } catch (_: Exception) { null }
                log("[SEND_NODE] source=text type=$cls text=$tx desc=$cd viewId=$id visible=$vis enabled=$en clickable=$clk rect=$r")
                if (vis && en && clk && inBottom(r)) {
                    btn = n
                    log("[SEND_NODE] selected=true rect=$r")
                    break
                } else {
                    dropped++
                    log("[SEND_NODE] selected=false")
                }
            }
        }
        if (btn == null && dropped > 0) log("[SEND_NODE] dropped=$dropped")
        if (btn == null) {
            val cands = mutableListOf<AccessibilityNodeInfo>()
            svc.collectAllClickable(root, cands)
            val dm = svc.resources.displayMetrics
            val sw = dm.widthPixels; val sh = dm.heightPixels
            btn = cands.filter { n ->
                if (!n.isVisibleToUser) return@filter false
                val r = android.graphics.Rect(); n.getBoundsInScreen(r)
                if (r.width() <= 0 || r.height() <= 0) return@filter false
                if (r.centerX() < 0 || r.centerX() > sw || r.centerY() < 0 || r.centerY() > sh) return@filter false
                r.centerX() > sw * 0.6f && r.centerY() > sh * 0.65f && r.width() < sw * 0.3f && r.height() < sh * 0.2f
            }.maxByOrNull { val r = android.graphics.Rect(); it.getBoundsInScreen(r); r.centerX().toFloat() }
        }
        val rect = android.graphics.Rect(); btn?.getBoundsInScreen(rect)
        val isSend = btn?.let {
            it.viewIdResourceName == "com.whatsapp.w4b:id/send" ||
                it.contentDescription?.toString()?.contains("Enviar", true) == true
        } ?: false
        val btnEnabled = btn?.isEnabled == true
        val btnClickable = btn?.isClickable == true
        val entry = entryText(svc)
        if (!isSend && entry.isEmpty()) {
            log("[CHECK] send skipped (mic, entry empty) rect=$rect")
            return false
        }
        if (!btnEnabled || !btnClickable) {
            // Botón visible pero no accionable (chat cargando): esperar, no tocar.
            log("[CHECK] send not actionable yet enabled=$btnEnabled clickable=$btnClickable rect=$rect")
            return false
        }
        // El performAction CLICK en w4b devuelve true pero no envía; el tap por
        // gesto es lo que realmente pulsa Send. Un solo tap (el mic-guard evita el mic).
        var ok = false
        var via = "none"
        if (rect.width() > 0 && rect.height() > 0) {
            try {
                val path = android.graphics.Path().apply {
                    moveTo(rect.centerX().toFloat(), rect.centerY().toFloat())
                    lineTo(rect.centerX() + 1f, rect.centerY() + 1f)
                }
                val tap = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                    .build()
                log("[CHECK] gesture tap at (${rect.centerX()},${rect.centerY()}) dur=50ms")
                ok = svc.dispatchGesture(tap, object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        android.util.Log.d(TAG, "[CHECK] gesture completed")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        android.util.Log.d(TAG, "[CHECK] gesture CANCELLED")
                    }
                }, null)
                via = "gesture=$ok"
                if (!ok) {
                    ok = btn?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
                    via = "gesture=false click=$ok"
                }
            } catch (_: Exception) {
                ok = btn?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
                via = "gesture=ex click=$ok"
            }
        } else {
            ok = btn?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            via = "click=$ok"
        }
        log("[CHECK] send tapped=$ok via=$via rect=$rect isSend=$isSend")
        return ok
    }

    private fun handleAceptar(svc: AutoAccessibilityService, root: AccessibilityNodeInfo) {
        val btn = svc.findButtonByText(root, "Aceptar")
            ?: svc.findNodeWithText(root, "Aceptar")
            ?: svc.findNodeContainingText(root, "Aceptar")
            ?: svc.findNodeContainingText(root, "Accept")
        if (btn == null) {
            // Fallback por coordenadas relativas (solo Aceptar, no el envío)
            try {
                val dm = svc.resources.displayMetrics
                val ax = dm.widthPixels * 0.5f
                val path = Path().apply { moveTo(ax, 2045f); lineTo(ax + 1f, 2046f) }
                svc.dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 120)).build(), null, null)
                log("[CHECK] aceptar fallback tap")
            } catch (_: Exception) {}
            return
        }
        btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        log("[CHECK] aceptar clicked")
        handler.postDelayed({
            if (!active) return@postDelayed
            if (attemptSendClick(svc)) { sentClicked = true; sendClicks++ }
        }, 2000)
    }

    private fun doCleanup(svc: AutoAccessibilityService) {
        val s = sch ?: run { stop(); return }
        log("[STATE] CLEANUP id=${s.id}")
        navigateBack(svc)
        try { BusinessFlowHandler.reset() } catch (_: Exception) {}
        state = SendState.READY_FOR_NEXT_MESSAGE
        log("[QUEUE] id=${s.id} SENT")
        finish(svc, true, null)
    }

    // ---------- screen detection (IDs/texto, no solo coordenadas) ----------

    private fun activeRoot(svc: AutoAccessibilityService): AccessibilityNodeInfo? {
        return try { svc.rootInActiveWindow } catch (_: Exception) { null }
    }

    /**
     * Árbol fresco: intenta refrescar el nodo raíz. Si el sistema devuelve
     * false, el árbol está muerto (vista vieja) y NO debe usarse para verificar.
     */
    private fun freshRoot(svc: AutoAccessibilityService): AccessibilityNodeInfo? {
        val r = activeRoot(svc) ?: return null
        return try {
            if (r.refresh()) r else null
        } catch (_: Exception) { null }
    }

    /** Texto del campo con árbol fresco. null = sin datos (no decidir nada). */
    private fun entryTextFresh(svc: AutoAccessibilityService): String? {
        val r = freshRoot(svc) ?: return null
        return try { svc.findEditText(r)?.text?.toString()?.trim() } catch (_: Exception) { null }
    }

    private fun isLocked(svc: AutoAccessibilityService): Boolean {
        return try {
            (svc.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager).isKeyguardLocked
        } catch (_: Exception) { true }
    }

    private fun isUnlocked(svc: AutoAccessibilityService): Boolean {
        val kg = try {
            (svc.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager).isKeyguardLocked
        } catch (_: Exception) { true }
        val inter = try {
            (svc.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager).isInteractive
        } catch (_: Exception) { false }
        return !kg && inter
    }

    private fun targetPkg(): String {
        val t = sch?.target ?: "whatsapp_business"
        return if (t == "whatsapp_business") "com.whatsapp.w4b" else "com.whatsapp"
    }

    private fun isWhatsAppForeground(svc: AutoAccessibilityService): Boolean {
        val root = activeRoot(svc) ?: return false
        return try { root.packageName?.toString() == targetPkg() } catch (_: Exception) { false }
    }

    /** ¿Hay marcador de conversación real (no lista)? Evita confundir la
     *  lista de chats (tiene search box = EditText) con una conversación. */
    /** ¿Hay marcadores de conversación real (botón Send o mic por descripción
     *  y clase)? Evita confundir la lista de chats con una conversación.
     *  Sin viewIds: llegan null en runtime. */
    private fun hasConversationMarkers(root: AccessibilityNodeInfo): Boolean {
        val ids = ArrayDeque<AccessibilityNodeInfo>()
        ids.add(root)
        var guard = 0
        while (ids.isNotEmpty() && guard++ < 1500) {
            val n = ids.removeLast()
            try {
                val cls = n.className?.toString() ?: ""
                val cd = n.contentDescription?.toString() ?: ""
                if ((cls.contains("ImageButton") || cls.contains("ImageView")) &&
                    (cd.contains("Enviar", true) || cd.contains("voz", true) || cd.contains("voice", true))
                ) return true
                for (i in 0 until n.childCount) n.getChild(i)?.let { ids.add(it) }
            } catch (_: Exception) {}
        }
        return false
    }

    private fun detectScreen(svc: AutoAccessibilityService): Screen {
        if (isLocked(svc)) return Screen.LOCK
        val root = activeRoot(svc) ?: return Screen.UNKNOWN
        val pkg = try { root.packageName?.toString() } catch (_: Exception) { null }
        if (pkg != targetPkg()) return Screen.BACKGROUND
        if (svc.containsText(root, "Enviar a")) return Screen.PICKER
        if (svc.findEditText(root) != null && hasConversationMarkers(root)) return Screen.CHAT
        if (svc.containsText(root, "Buscar")) return Screen.SEARCH
        return Screen.OTHER_WA
    }

    private fun isCorrectChat(svc: AutoAccessibilityService): Boolean {
        val s = sch ?: return false
        val root = activeRoot(svc) ?: return false
        if (detectScreen(svc) == Screen.PICKER) return false
        if (findMessageBox(svc, root) == null) return false
        if (s.contactName.isNotBlank() &&
            (svc.findNodeWithText(root, s.contactName) != null ||
                svc.findNodeContainingText(root, s.contactName) != null)) return true
        // El encabezado puede mostrar el número (contacto no agendado con ese nombre)
        val digits = s.phone.filter { it.isDigit() }
        if (digits.length >= 9 && svc.findNodeContainingText(root, digits.takeLast(9)) != null) return true
        // Último recurso: este chat lo abrió nuestro propio wa.me en esta corrida.
        // WhatsApp ya resolvió el número; rechazarlo por el título sería un falso negativo.
        if (openedWhatsApp && detectScreen(svc) == Screen.CHAT) {
            log("[CHECK] correct chat=true (via wa.me, title differs)")
            return true
        }
        return false
    }

    private fun entryText(svc: AutoAccessibilityService): String {
        return try { activeRoot(svc)?.let { findMessageBox(svc, it)?.text?.toString()?.trim() } ?: "" } catch (_: Exception) { "" }
    }

    /** Cuenta burbujas con el texto exacto SOLO dentro de las filas de la
     *  conversación (IDs estables). Exige árbol fresco; si no hay, -1. */
    private fun countBubbles(svc: AutoAccessibilityService, msg: String): Int {
        if (msg.isEmpty()) return -1
        val root = freshRoot(svc) ?: return -1
        return countBubblesIn(root, msg)
    }

    /** Cuenta filas de conversación (lista cargada si >0). */
    private fun countRows(root: AccessibilityNodeInfo): Int {
        var n = 0
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        var guard = 0
        while (stack.isNotEmpty() && guard++ < 1500) {
            val node = stack.removeLast()
            try {
                if (node.viewIdResourceName == "com.whatsapp.w4b:id/conversation_text_row") {
                    n++
                    continue
                }
                for (i in 0 until node.childCount) node.getChild(i)?.let { stack.add(it) }
            } catch (_: Exception) {}
        }
        return n
    }

    /** Variante que reutiliza un root ya leído (un solo escaneo por poll). */
    private fun countBubblesIn(root: AccessibilityNodeInfo, msg: String): Int {        if (msg.isEmpty()) return -1
        val rows = ArrayDeque<AccessibilityNodeInfo>()
        rows.add(root)
        var n = 0
        var guard = 0
        // 1) juntar subárboles conversation_text_row
        val convRows = mutableListOf<AccessibilityNodeInfo>()
        while (rows.isNotEmpty() && guard++ < 1500) {
            val node = rows.removeLast()
            try {
                if (node.viewIdResourceName == "com.whatsapp.w4b:id/conversation_text_row") {
                    convRows.add(node)
                    continue
                }
                for (i in 0 until node.childCount) node.getChild(i)?.let { rows.add(it) }
            } catch (_: Exception) {}
        }
        // 2) contar message_text exactos dentro de esas filas (sin EditText)
        guard = 0
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addAll(convRows)
        // Fallback: si no se halló ninguna fila (layout distinto), árbol completo.
        if (stack.isEmpty()) {
            try { stack.add(root) } catch (_: Exception) { return -1 }
        }
        while (stack.isNotEmpty() && guard++ < 4000) {
            val node = stack.removeLast()
            try {
                val cls = node.className?.toString() ?: ""
                if (!cls.contains("EditText") &&
                    node.viewIdResourceName == "com.whatsapp.w4b:id/message_text" &&
                    node.text?.toString() == msg
                ) n++
                for (i in 0 until node.childCount) node.getChild(i)?.let { stack.add(it) }
            } catch (_: Exception) {}
        }
        return n
    }

    private fun timeoutFor(s: SendState): Long = when (s) {
        SendState.UNLOCKING -> 25000L
        SendState.WAITING_FOR_DEVICE_READY -> 10000L
        SendState.OPENING_WHATSAPP -> 8000L
        SendState.WAITING_FOR_WHATSAPP -> 20000L
        SendState.VERIFYING_WHATSAPP -> 8000L
        SendState.OPENING_SEARCH -> 5000L
        SendState.WAITING_FOR_SEARCH -> 8000L
        SendState.SEARCHING_CONTACT -> 15000L
        SendState.VERIFYING_CONTACT -> 12000L
        SendState.OPENING_CHAT -> 8000L
        SendState.VERIFYING_CHAT -> 12000L
        SendState.TYPING_MESSAGE -> 8000L
        SendState.VERIFYING_MESSAGE -> 10000L
        SendState.SENDING -> 10000L
        SendState.VERIFYING_SENT -> 12000L
        SendState.REVERIFY_SENT -> 12000L
        SendState.CLEANUP -> 8000L
        else -> 5000L
    }

    private fun log(m: String) = android.util.Log.d(TAG, m)
}
