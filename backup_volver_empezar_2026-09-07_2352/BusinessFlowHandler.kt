package com.jahc.auto.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.jahc.auto.data.Schedule

/**
 * WhatsApp Business flow - completely separate from Dual.
 * Handles: picker -> search contact -> tap -> type message -> send
 */
object BusinessFlowHandler {

    private var lastBusinessPickerAction = 0L
    private var businessPickerSearched = false
    private var searchIconTried = false

    fun reset() {
        lastBusinessPickerAction = 0L
        businessPickerSearched = false
        searchIconTried = false
    }

    private fun findSearchIcon(service: AutoAccessibilityService, root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        service.collectAllClickable(root, candidates)
        val topArea = candidates.filter { n ->
            val r = android.graphics.Rect(); n.getBoundsInScreen(r)
            r.centerY() < 300 && r.centerX() > 100
        }
        val searchIcon = topArea.firstOrNull { n ->
            val cd = n.contentDescription?.toString()?.lowercase() ?: ""
            val cls = n.className?.toString()?.lowercase() ?: ""
            cd.contains("search") || cd.contains("buscar") || cls.contains("imagebutton") || cls.contains("imageview")
        }
        if (searchIcon != null) {
            val r = android.graphics.Rect(); searchIcon.getBoundsInScreen(r)
            android.util.Log.d("BusinessFlow", "Search icon found at $r desc=${searchIcon.contentDescription}")
        }
        return searchIcon
    }

    fun handlePickerEvent(
        service: AutoAccessibilityService,
        root: AccessibilityNodeInfo,
        sch: Schedule
    ): Boolean {
        if (!service.containsText(root, "Enviar a")) return false
        if (System.currentTimeMillis() - lastBusinessPickerAction < 2800) return true
        lastBusinessPickerAction = System.currentTimeMillis()

        val targetContact = sch.contactName.ifBlank { "Andres" }
        val searchText = if (sch.phone.isNotBlank()) sch.phone.filter { it.isDigit() }.let { if (it.length == 9) it else it.takeLast(9) } else targetContact

        android.util.Log.d("BusinessFlow", "Picker detected, contact=$targetContact phone=${sch.phone} searchText=$searchText")

        var searchEdit = service.findEditText(root)
        if (searchEdit == null) {
            val buscarBtn = service.findButtonByText(root, "Buscar") ?: service.findNodeWithText(root, "Buscar")
            if (buscarBtn != null) {
                android.util.Log.d("BusinessFlow", "Click Buscar")
                buscarBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                android.os.Handler(service.mainLooper).postDelayed({
                    val freshRoot = service.rootInActiveWindow ?: return@postDelayed
                    val freshSearchEdit = service.findEditText(freshRoot)
                    if (freshSearchEdit != null) {
                        android.util.Log.d("BusinessFlow", "Set search text $searchText")
                        val b = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, searchText) }
                        freshSearchEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
                        freshSearchEdit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        android.os.Handler(service.mainLooper).postDelayed({
                            val r2 = service.rootInActiveWindow ?: return@postDelayed
                            tapBusinessContact(service, r2, targetContact, searchText)
                        }, 5000)
                    }
                }, 2500)
                return true
            }

            if (!searchIconTried) {
                val searchIcon = findSearchIcon(service, root)
                if (searchIcon != null) {
                    searchIconTried = true
                    android.util.Log.d("BusinessFlow", "Tap search icon")
                    searchIcon.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                android.util.Log.d("BusinessFlow", "No edit text, no Buscar, no search icon found")
            }
            return false
        }

        val cand = service.findButtonByText(root, targetContact) ?: service.findNodeWithText(root, targetContact)
        val contactNode = cand ?: run {
            if (sch.phone.isNotBlank()) {
                service.findButtonByText(root, searchText) ?: service.findNodeWithText(root, searchText)
                    ?: service.findNodeContainingText(root, searchText) ?: service.findNodeContainingText(root, targetContact)
            } else null
        }
        if (contactNode != null) {
            android.util.Log.d("BusinessFlow", "Click contact $targetContact")
            contactNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        android.util.Log.d("BusinessFlow", "Set search text $searchText")
        val b = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, searchText) }
        searchEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
        searchEdit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        android.os.Handler(service.mainLooper).postDelayed({
            val r2 = service.rootInActiveWindow ?: return@postDelayed
            tapBusinessContact(service, r2, targetContact, searchText)
        }, 5000)
        return true
    }

    private fun tapBusinessContact(
        service: AutoAccessibilityService,
        root: AccessibilityNodeInfo,
        targetContact: String,
        searchText: String
    ) {
        val c2 = service.findButtonByText(root, targetContact) ?: service.findNodeWithText(root, targetContact)
        val contactNode2 = c2 ?: run {
            if (searchText.isNotBlank()) {
                service.findButtonByText(root, searchText) ?: service.findNodeWithText(root, searchText)
                    ?: service.findNodeContainingText(root, searchText) ?: service.findNodeContainingText(root, targetContact)
            } else null
        }
        if (contactNode2 != null) {
            android.util.Log.d("BusinessFlow", "Click contact $targetContact (after search)")
            contactNode2.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            val firstContact = service.findFirstContact(root)
            if (firstContact != null) {
                android.util.Log.d("BusinessFlow", "Click first contact fallback")
                firstContact.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    fun handleDirectMessage(
        service: AutoAccessibilityService,
        sch: Schedule
    ): Boolean {
        if (System.currentTimeMillis() - lastBusinessPickerAction < 4000) return true

        val freshRoot = service.rootInActiveWindow ?: return true
        if (freshRoot.packageName?.toString() == "com.whatsapp.w4b" && service.containsText(freshRoot, "Seleccionar")) return true

        if (service.containsText(freshRoot, "Enviar a")) {
            android.util.Log.d("BusinessFlow", "DirectMsg: still in picker, skip")
            return true
        }

        val edit = service.findEditText(freshRoot) ?: return true
        android.util.Log.d("BusinessFlow", "Set message ${sch.message}")
        val bundle = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sch.message) }
        edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)

        android.os.Handler(service.mainLooper).postDelayed({
            val sendRoot = service.rootInActiveWindow
            var sendBtn = sendRoot?.let { service.findSendButtonById(it) ?: service.findSendButton(it) }
            if (sendBtn == null) {
                sendBtn = sendRoot?.let { service.findButtonByText(it, "Enviar") ?: service.findNodeWithText(it, "Enviar") }
            }
            if (sendBtn == null && sendRoot != null) {
                val candidates = mutableListOf<AccessibilityNodeInfo>()
                service.collectAllClickable(sendRoot, candidates)
                val dm = service.resources.displayMetrics
                val screenW = dm.widthPixels
                val screenH = dm.heightPixels
                sendBtn = candidates.filter { n ->
                    if (!n.isVisibleToUser) return@filter false
                    val r = android.graphics.Rect(); n.getBoundsInScreen(r)
                    if (r.width() <= 0 || r.height() <= 0) return@filter false
                    if (r.centerX() < 0 || r.centerX() > screenW || r.centerY() < 0 || r.centerY() > screenH) return@filter false
                    r.centerX() > screenW * 0.6f && r.centerY() > screenH * 0.65f && r.width() < screenW * 0.3f && r.height() < screenH * 0.2f
                }.maxByOrNull {
                    val r = android.graphics.Rect(); it.getBoundsInScreen(r); r.centerX().toFloat()
                }
                if (sendBtn != null) android.util.Log.d("BusinessFlow", "Send btn found via bottom area visible")
            }
            val rect = android.graphics.Rect(); sendBtn?.getBoundsInScreen(rect)
            android.util.Log.d("BusinessFlow", "Send btn found=${sendBtn != null} rect=$rect")
            var sent = sendBtn?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            android.util.Log.d("BusinessFlow", "Click send sent=$sent")

            android.os.Handler(service.mainLooper).postDelayed({
                var sendX = 959f
                var sendY = 1459f
                if (sendBtn != null && rect.width() > 0 && rect.height() > 0) {
                    sendX = rect.centerX().toFloat()
                    sendY = rect.centerY().toFloat()
                } else if (rect.width() == 0) {
                    // Fallback: sin rect, usa Y 1459 (conversa sin teclado) no 2135
                    sendY = 1459f
                }
                val path = android.graphics.Path().apply { moveTo(sendX, sendY); lineTo(sendX + 1f, sendY + 1f) }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 150))
                    .build()
                service.dispatchGesture(gesture, null, null)
                android.util.Log.d("BusinessFlow", "Send gesture at ($sendX,$sendY) rect=$rect")
            }, 300)

            android.os.Handler(service.mainLooper).postDelayed({
                val checkRoot = service.rootInActiveWindow
                val hasMetaInterstitial = checkRoot != null && service.containsText(checkRoot, "Aceptar")
                if (hasMetaInterstitial) {
                    android.util.Log.d("BusinessFlow", "Meta AI interstitial detected, clicking Aceptar")
                    var aceptarBtn = checkRoot?.let { service.findButtonByText(it, "Aceptar") ?: service.findNodeWithText(it, "Aceptar") }
                    if (aceptarBtn == null) aceptarBtn = checkRoot?.let { service.findNodeContainingText(it, "Aceptar") }
                    if (aceptarBtn == null) aceptarBtn = checkRoot?.let { service.findNodeContainingText(it, "Accept") }
                    // Buscar en todas las ventanas (dialog puede estar en otra ventana)
                    if (aceptarBtn == null) {
                        try {
                            val wins = service.windows
                            for (w in wins) {
                                val wr = w.root ?: continue
                                var cand = service.findButtonByText(wr, "Aceptar") ?: service.findNodeWithText(wr, "Aceptar")
                                if (cand == null) cand = service.findNodeContainingText(wr, "Aceptar")
                                if (cand != null) { aceptarBtn = cand; break }
                            }
                        } catch (_: Exception) {}
                    }
                    if (aceptarBtn != null) {
                        val r = android.graphics.Rect(); aceptarBtn.getBoundsInScreen(r)
                        android.util.Log.d("BusinessFlow", "Aceptar btn found rect=$r")
                        aceptarBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        android.os.Handler(service.mainLooper).postDelayed({
                            val rx = r.centerX().toFloat()
                            val ry = r.centerY().toFloat()
                            if (rx > 0 && ry > 0) {
                                val pg = Path().apply { moveTo(rx, ry); lineTo(rx+1f, ry+1f) }
                                val gg = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(pg, 0, 120)).build()
                                service.dispatchGesture(gg, null, null)
                                android.util.Log.d("BusinessFlow", "Aceptar gesture at rect ($rx,$ry)")
                            }
                        }, 300)
                    } else {
                        // Dump para debug: listar todos los clickeables con texto
                        try {
                            val dbg = mutableListOf<AccessibilityNodeInfo>()
                            checkRoot?.let { service.collectAllClickable(it, dbg) }
                            for (n in dbg.take(15)) {
                                val r2 = android.graphics.Rect(); n.getBoundsInScreen(r2)
                                android.util.Log.d("BusinessFlow", "DBG clickable text='${n.text}' desc='${n.contentDescription}' class=${n.className} rect=$r2")
                            }
                            val wins2 = service.windows
                            for (w in wins2) {
                                val wr = w.root ?: continue
                                if (service.containsText(wr, "Aceptar")) android.util.Log.d("BusinessFlow", "DBG window contains Aceptar pkg=${w.root?.packageName} title=${w.title}")
                            }
                        } catch (e: Exception) { android.util.Log.e("BusinessFlow", "dbg err", e) }
                        val dm = service.resources.displayMetrics
                        val ax = dm.widthPixels * 0.5f
                        val ay = 2045f
                        val p = Path().apply { moveTo(ax, ay); lineTo(ax+1f, ay+1f) }
                        val g = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 120)).build()
                        service.dispatchGesture(g, null, null)
                        android.util.Log.d("BusinessFlow", "Aceptar fallback tap at ($ax,$ay) - no node found, Y=2045")
                        android.os.Handler(service.mainLooper).postDelayed({
                            val ay2 = 2002f
                            val p2 = Path().apply { moveTo(ax, ay2); lineTo(ax+1f, ay2+1f) }
                            val g2 = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p2, 0, 120)).build()
                            service.dispatchGesture(g2, null, null)
                            android.util.Log.d("BusinessFlow", "Aceptar second fallback at ($ax,$ay2)")
                        }, 400)
                    }
                    android.os.Handler(service.mainLooper).postDelayed({
                        val secondRoot = service.rootInActiveWindow
                        var secondBtn = secondRoot?.let { service.findSendButtonById(it) ?: service.findSendButton(it) }
                        if (secondBtn == null && secondRoot != null) {
                            val cands = mutableListOf<AccessibilityNodeInfo>()
                            service.collectAllClickable(secondRoot, cands)
                            val dm2 = service.resources.displayMetrics
                            val sw2 = dm2.widthPixels
                            val sh2 = dm2.heightPixels
                            secondBtn = cands.filter { n ->
                                if (!n.isVisibleToUser) return@filter false
                                val rr = android.graphics.Rect(); n.getBoundsInScreen(rr)
                                if (rr.width() <= 0 || rr.height() <= 0) return@filter false
                                if (rr.centerX() < 0 || rr.centerX() > sw2 || rr.centerY() < 0 || rr.centerY() > sh2) return@filter false
                                rr.centerX() > sw2 * 0.6f && rr.centerY() > sh2 * 0.65f && rr.width() < sw2 * 0.3f && rr.height() < sh2 * 0.2f
                            }.maxByOrNull { val rr = android.graphics.Rect(); it.getBoundsInScreen(rr); rr.centerX().toFloat() }
                            if (secondBtn != null) android.util.Log.d("BusinessFlow", "Second send btn via bottom area visible")
                        }
                        val secondRect = android.graphics.Rect(); secondBtn?.getBoundsInScreen(secondRect)
                        var sx2 = 959f
                        var sy2 = 2160f
                        if (secondBtn != null && secondRect.width() > 0) {
                            sx2 = secondRect.centerX().toFloat()
                            sy2 = secondRect.centerY().toFloat()
                        }
                        val pp2 = Path().apply { moveTo(sx2, sy2); lineTo(sx2+1f, sy2+1f) }
                        val gg2 = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(pp2, 0, 150)).build()
                        service.dispatchGesture(gg2, null, null)
                        android.util.Log.d("BusinessFlow", "Second send gesture at ($sx2,$sy2) rect=$secondRect")

                        android.os.Handler(service.mainLooper).postDelayed({
                            val verifyRoot = service.rootInActiveWindow
                            val stillHasAccept = verifyRoot != null && service.containsText(verifyRoot, "Aceptar")
                            if (stillHasAccept) {
                                android.util.Log.d("BusinessFlow", "Still has Aceptar after second send, retry accept")
                                val retryBtn = verifyRoot?.let { service.findButtonByText(it, "Aceptar") ?: service.findNodeWithText(it, "Aceptar") }
                                if (retryBtn != null) retryBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                android.os.Handler(service.mainLooper).postDelayed({
                                    val pp3 = Path().apply { moveTo(sx2, sy2); lineTo(sx2+1f, sy2+1f) }
                                    val gg3 = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(pp3, 0, 150)).build()
                                    service.dispatchGesture(gg3, null, null)
                                    android.util.Log.d("BusinessFlow", "Third send gesture after retry")
                                    service.showResultNotification(true, sch)
                                    AutoAccessibilityService.pendingSchedule = null
                                    service.clearPendingPrefs()
                                    reset()
                                    android.os.Handler(service.mainLooper).postDelayed({ service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) }, 1000)
                                }, 3000)
                            } else {
                                android.util.Log.d("BusinessFlow", "Second send done, no more Aceptar")
                                service.showResultNotification(true, sch)
                                AutoAccessibilityService.pendingSchedule = null
                                service.clearPendingPrefs()
                                reset()
                                android.os.Handler(service.mainLooper).postDelayed({ service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) }, 1000)
                            }
                        }, 3000)
                    }, 1500)
                } else {
                    android.util.Log.d("BusinessFlow", "No Meta interstitial, normal finish")
                    service.showResultNotification(sent, sch)
                    AutoAccessibilityService.pendingSchedule = null
                    service.clearPendingPrefs()
                    reset()
                    android.os.Handler(service.mainLooper).postDelayed({ service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) }, 800)
                    android.os.Handler(service.mainLooper).postDelayed({ service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) }, 1500)
                    android.os.Handler(service.mainLooper).postDelayed({ service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) }, 2200)
                }
            }, 6000)
        }, 2000)

        android.os.Handler(service.mainLooper).postDelayed({
            if (AutoAccessibilityService.pendingSchedule != null && AutoAccessibilityService.pendingSchedule?.id == sch.id) {
                service.showResultNotification(false, sch)
                AutoAccessibilityService.pendingSchedule = null
                service.clearPendingPrefs()
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }
        }, 30000)

        return true
    }
}
