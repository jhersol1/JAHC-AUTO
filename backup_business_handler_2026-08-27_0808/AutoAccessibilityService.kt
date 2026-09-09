package com.jahc.auto.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Path
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.jahc.auto.data.Schedule

class AutoAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var pendingSchedule: Schedule? = null
        @Volatile var enabled = false
    }

    private var lastLockAction = 0L
    private var lastPinSuccess = 0L
    private var lastPickerAction = 0L
    private var lastDirectAction = 0L
    private var lastDualAction = 0L
    @Volatile var waitingForDualPicker = false
    @Volatile private var dualPickerSearched = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        enabled = true
    }

    private fun findLockScreenRoot(): AccessibilityNodeInfo? {
        try {
            val winList = try { windows } catch (_: Exception) { null }
            if (winList != null) {
                for (w in winList) {
                    try {
                        val r = w.root ?: continue
                        if (hasPinPad(r) || containsDigitGrid(r)) return r
                    } catch (_: Exception) { continue }
                }
            }
        } catch (_: Exception) {}
        return try { rootInActiveWindow } catch (_: Exception) { null }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (pendingSchedule == null) {
            try {
                val prefs = getSharedPreferences("jahc_auto", MODE_PRIVATE)
                val t = prefs.getString("pending_target", null)
                if (t != null) {
                    pendingSchedule = com.jahc.auto.data.Schedule(
                        id = prefs.getLong("pending_id", -1),
                        contactName = prefs.getString("pending_contact", "Jhersol2.0") ?: "Jhersol2.0",
                        phone = prefs.getString("pending_phone", "904869774") ?: "904869774",
                        target = t,
                        message = prefs.getString("pending_msg", "hola") ?: "hola",
                        hour = 0, minute = 0
                    )
                    android.util.Log.d("AutoAccessibility", "Loaded pending from prefs: $t")
                }
            } catch (_: Exception) {}
            if (pendingSchedule == null) return
        }
        val sch = pendingSchedule ?: return
        if (event == null) return

        val rootEarly = findLockScreenRoot()
        val evPkg = event.packageName?.toString() ?: ""
        val isWhatsAppEvent = evPkg in listOf("com.whatsapp", "com.whatsapp.w4b")
        if (rootEarly != null) {
            if (isWhatsAppEvent) {
                // En WhatsApp: solo manejar Recibe ayuda rapida (Meta AI interstitial), NUNCA lock screen
                if (containsText(rootEarly, "Recibe ayuda rapida") || containsText(rootEarly, "IA de Meta")) {
                    val btn = findButtonByText(rootEarly, "Aceptar") ?: findButtonByText(rootEarly, "Accept")
                    if (btn != null) {
                        btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return
                    }
                }
            } else {
                // Fuera de WhatsApp: manejar lock screen + chooser
                val activeRoot = try { rootInActiveWindow } catch (_: Exception) { null }
                val inPicker = activeRoot != null && containsText(activeRoot, "Enviar a")
                val inConversation = activeRoot != null && (containsText(activeRoot, "Escribe un mensaje") || containsText(activeRoot, "Mi respuesta") || findEditText(activeRoot) != null && !inPicker)
                val pkgActive = try { activeRoot?.packageName?.toString() } catch (_: Exception) { null }
                val isGoogleSearch = pkgActive == "com.google.android.googlequicksearchbox"
                val hasFocusedEdit = activeRoot != null && findEditText(activeRoot) != null
                val isLocked = try { (getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager).isKeyguardLocked } catch (_: Exception) { true }
                if (inPicker || inConversation || isGoogleSearch) {
                    // no hacer nada de lock - evita escribir "bu bu" en buscador
                } else if (hasFocusedEdit && pkgActive !in listOf("com.android.systemui", "android")) {
                    // Hay un EditText enfocado en una app -> no es lock screen, no hacer swipe/PIN
                } else if (!isLocked) {
                    // Teléfono desbloqueado -> no tocar lock screen
                } else {
                    if (handleLockScreen(rootEarly)) return
                }
                if (containsText(rootEarly, "Recibe ayuda rapida") || containsText(rootEarly, "IA de Meta")) {
                    val btn = findButtonByText(rootEarly, "Aceptar") ?: findButtonByText(rootEarly, "Accept")
                    if (btn != null) {
                        btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return
                    }
                }
                // Selector dual WhatsApp
                if (containsText(rootEarly, "Seleccionar aplicacion") || containsText(rootEarly, "Seleccionar aplicación")) {
                    if (System.currentTimeMillis() - lastDualAction < 3000) return
                    lastDualAction = System.currentTimeMillis()
                    android.util.Log.d("AutoAccessibility", "Dual chooser detected")
                    handleDualChooser(rootEarly)
                    return
                }
            }
        }

        val pkg = event.packageName?.toString() ?: return
        // Permitir WhatsApp + chooser del sistema (MIUI dual selector)
        val isWhatsApp = pkg in listOf("com.whatsapp", "com.whatsapp.w4b")
        val isChooser = pkg in listOf("android", "com.miui.packageinstaller", "com.android.packageinstaller")
        if (!isWhatsApp && !isChooser) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val root = rootInActiveWindow ?: return
        if (pendingSchedule == null) return
        val isLockedBiz = try { (getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager).isKeyguardLocked } catch (_: Exception) { false }
        if (isLockedBiz) return

        // === BUSINESS: delegar todo a BusinessFlowHandler (separado de Dual) ===
        if (sch?.target == "whatsapp_business" && pkg == "com.whatsapp.w4b") {
            if (BusinessFlowHandler.handlePickerEvent(this, root, sch)) return
            if (System.currentTimeMillis() - lastDirectAction < 5000) return
            lastDirectAction = System.currentTimeMillis()
            android.os.Handler(mainLooper).postDelayed({
                if (BusinessFlowHandler.handleDirectMessage(this, sch)) return@postDelayed
            }, 3000)
            android.os.Handler(mainLooper).postDelayed({
                if (pendingSchedule != null && pendingSchedule?.id == sch.id) {
                    showResultNotification(false, sch)
                    pendingSchedule = null
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }, 30000)
            return
        }

        // Picker "Enviar a..." -> Business: Buscar -> filtrar -> click (sin duplicar envios)
        if (containsText(root, "Enviar a") && !waitingForDualPicker) {
            if (System.currentTimeMillis() - lastPickerAction < 2800) return
            lastPickerAction = System.currentTimeMillis()
            val targetContact = sch.contactName.ifBlank { "Andres" }
            android.util.Log.d("AutoAccessibility", "Picker detected, contact=$targetContact")
            // Para dual 904869774: busca por phone si contactName es Jhersol Dual, para encontrar Jhersol (Tú)
            val searchText = when {
                sch.isDual() && sch.phone.isNotBlank() -> sch.phone.filter { it.isDigit() }.takeLast(9) // 904869774
                else -> targetContact
            }
            var searchEdit = findEditText(root)
            if (searchEdit == null) {
                val buscarBtn = findButtonByText(root, "Buscar") ?: findNodeWithText(root, "Buscar")
                if (buscarBtn != null) {
                    android.util.Log.d("AutoAccessibility", "Click Buscar")
                    buscarBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    android.os.Handler(mainLooper).postDelayed({
                        if (waitingForDualPicker) return@postDelayed
                        val freshRoot = rootInActiveWindow ?: return@postDelayed
                        val freshSearchEdit = findEditText(freshRoot)
                        if (freshSearchEdit != null) {
                            android.util.Log.d("AutoAccessibility", "Set search text $searchText (dual phone)")
                            val b = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, searchText) }
                            freshSearchEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
                            freshSearchEdit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            android.os.Handler(mainLooper).postDelayed({
                                if (waitingForDualPicker) return@postDelayed
                                val r2 = rootInActiveWindow ?: return@postDelayed
                                // Para dual: tap primer contacto por posición fija después de 5s
                                if (sch.isDual()) {
                                    android.util.Log.d("AutoAccessibility", "Tap first contact dual by position")
                                    val dm = resources.displayMetrics
                                    val tapX = dm.widthPixels / 2f
                                    val tapY = dm.heightPixels * 0.16f
                                    val path = android.graphics.Path().apply { moveTo(tapX, tapY); lineTo(tapX + 1f, tapY + 1f) }
                                    val gesture = GestureDescription.Builder()
                                        .addStroke(GestureDescription.StrokeDescription(path, 0, 120))
                                        .build()
                                    dispatchGesture(gesture, null, null)
                                } else {
                                    val cand1 = findButtonByText(r2, targetContact) ?: findNodeWithText(r2, targetContact)
                                    val contactNode2 = cand1 ?: run {
                                        if (sch.phone.isNotBlank()) {
                                            findButtonByText(r2, searchText) ?: findNodeWithText(r2, searchText) ?: findNodeContainingText(r2, searchText) ?: findNodeContainingText(r2, targetContact)
                                        } else null
                                    }
                                    if (contactNode2 != null) {
                                        android.util.Log.d("AutoAccessibility", "Click contact $targetContact (after search phone=$searchText)")
                                        contactNode2.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    } else {
                                        val firstContact = findFirstContact(r2)
                                        if (firstContact != null) {
                                            android.util.Log.d("AutoAccessibility", "Click first contact fallback")
                                            firstContact.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                        }
                                    }
                                }
                            }, 5000)
                        }
                    }, 2500)
                    return
                }
            }
            searchEdit = findEditText(root)
            if (searchEdit != null) {
                val cand = findButtonByText(root, targetContact) ?: findNodeWithText(root, targetContact)
                val contactNode = cand ?: run {
                    if (sch.phone.isNotBlank()) findButtonByText(root, searchText) ?: findNodeWithText(root, searchText) ?: findNodeContainingText(root, searchText) ?: findNodeContainingText(root, targetContact) else null
                }
                if (contactNode != null) {
                    android.util.Log.d("AutoAccessibility", "Click contact $targetContact (phone $searchText)")
                    contactNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
                android.util.Log.d("AutoAccessibility", "Set search text $searchText")
                val b = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, searchText) }
                searchEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
                searchEdit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                android.os.Handler(mainLooper).postDelayed({
                    if (waitingForDualPicker) return@postDelayed
                    val r2 = rootInActiveWindow ?: return@postDelayed
                    if (sch.isDual()) {
                        android.util.Log.d("AutoAccessibility", "Tap first contact dual2 by position")
                        val dm = resources.displayMetrics
                        val tapX = dm.widthPixels / 2f
                        val tapY = dm.heightPixels * 0.16f
                        val path = android.graphics.Path().apply { moveTo(tapX, tapY); lineTo(tapX + 1f, tapY + 1f) }
                        val gesture = GestureDescription.Builder()
                            .addStroke(GestureDescription.StrokeDescription(path, 0, 120))
                            .build()
                        dispatchGesture(gesture, null, null)
                    } else {
                        val c2 = findButtonByText(r2, targetContact) ?: findNodeWithText(r2, targetContact)
                        val contactNode2 = c2 ?: run {
                            if (sch.phone.isNotBlank()) findButtonByText(r2, searchText) ?: findNodeWithText(r2, searchText) ?: findNodeContainingText(r2, searchText) ?: findNodeContainingText(r2, targetContact) else null
                        }
                        if (contactNode2 != null) {
                            android.util.Log.d("AutoAccessibility", "Click contact $targetContact (after search2 phone $searchText)")
                            contactNode2.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        } else {
                            val firstContact = findFirstContact(r2)
                            if (firstContact != null) {
                                android.util.Log.d("AutoAccessibility", "Click first contact fallback2")
                                firstContact.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            }
                        }
                    }
                }, 5000)
                return
            }
            return
        }

        // Dual share picker: after clicking right WhatsApp in chooser, ExternalShareAlias opens
        // Approach: type search → wait 5s → tap first contact by fixed screen position
        if (waitingForDualPicker && isWhatsApp) {
            if (dualPickerSearched) {
                android.util.Log.d("AutoAccessibility", "Dual picker: already searched, skip")
                return
            }
            if (System.currentTimeMillis() - lastDualAction < 500) return
            android.util.Log.d("AutoAccessibility", "Dual share picker detected, handling...")

            val targetContact = sch.contactName.ifBlank { "Andres" }
            val searchText = when {
                sch.isDual() && sch.phone.isNotBlank() -> sch.phone.filter { it.isDigit() }.takeLast(9)
                else -> targetContact
            }

            val sendBtn = findSendButtonById(root) ?: findSendButton(root)
            if (sendBtn != null) {
                android.util.Log.d("AutoAccessibility", "Dual picker: send button visible, in conversation")
                waitingForDualPicker = false
                dualPickerSearched = false
                lastDualAction = 0
                return
            }

            val buscarBtn = findButtonByText(root, "Buscar") ?: findNodeWithText(root, "Buscar")
            if (buscarBtn != null) {
                android.util.Log.d("AutoAccessibility", "Dual picker: click Buscar")
                buscarBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }

            val searchEdit = findEditText(root)
            if (searchEdit != null) {
                android.util.Log.d("AutoAccessibility", "Dual picker: type $searchText")
                val b = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, searchText) }
                searchEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
                searchEdit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                dualPickerSearched = true

                android.os.Handler(mainLooper).postDelayed({
                    android.util.Log.d("AutoAccessibility", "Dual picker: tap by position after 5s")
                    val dm = resources.displayMetrics
                    val tapX = dm.widthPixels / 2f
                    val tapY = dm.heightPixels * 0.16f
                    val path = android.graphics.Path().apply {
                        moveTo(tapX, tapY)
                        lineTo(tapX + 1f, tapY + 1f)
                    }
                    val gesture = GestureDescription.Builder()
                        .addStroke(GestureDescription.StrokeDescription(path, 0, 120))
                        .build()
                    dispatchGesture(gesture, null, null)
                    android.util.Log.d("AutoAccessibility", "Dual picker: tapped at ($tapX, $tapY)")

                    android.os.Handler(mainLooper).postDelayed({
                        waitingForDualPicker = false
                        dualPickerSearched = false
                        lastDualAction = 0
                        android.util.Log.d("AutoAccessibility", "Dual picker: clicking send")

                        android.os.Handler(mainLooper).postDelayed({
                            val sendX = dm.widthPixels * 0.92f
                            val sendY = dm.heightPixels * 0.68f
                            val sendPath = android.graphics.Path().apply {
                                moveTo(sendX, sendY)
                                lineTo(sendX + 1f, sendY + 1f)
                            }
                            val sendGesture = GestureDescription.Builder()
                                .addStroke(GestureDescription.StrokeDescription(sendPath, 0, 120))
                                .build()
                            dispatchGesture(sendGesture, null, null)
                            android.util.Log.d("AutoAccessibility", "Dual picker: send tap at ($sendX, $sendY)")

                            showResultNotification(true, sch)
                            pendingSchedule = null
                            android.os.Handler(mainLooper).postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 800)
                            android.os.Handler(mainLooper).postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 1500)
                        }, 1500)
                    }, 3000)
                }, 5000)
                return
            }

            android.util.Log.d("AutoAccessibility", "Dual picker: no search field yet, waiting...")
            return
        }

        if (System.currentTimeMillis() - lastDirectAction < 5000) return
        if (System.currentTimeMillis() - lastDualAction < 8000) return
        if (waitingForDualPicker) return
        lastDirectAction = System.currentTimeMillis()
        // Espera 3s a que el chat cargue antes de escribir (evita clicks fantasma)
        android.os.Handler(mainLooper).postDelayed({
            val freshRoot = rootInActiveWindow ?: return@postDelayed
            if (waitingForDualPicker) {
                android.util.Log.d("AutoAccessibility", "Direct blocked: waitingForDualPicker")
                return@postDelayed
            }
            if (System.currentTimeMillis() - lastPickerAction < 4000) {
                android.util.Log.d("AutoAccessibility", "Direct blocked: picker reciente")
                return@postDelayed
            }
            if (containsText(freshRoot, "Enviar a") || containsText(freshRoot, "Buscar")) return@postDelayed
            // Si sigue en picker de contactos, no escribir mensaje aún
            if (freshRoot.packageName?.toString() == "com.whatsapp" && containsText(freshRoot, "ContactPicker")) return@postDelayed
            if (freshRoot.packageName?.toString() == "com.whatsapp.w4b" && containsText(freshRoot, "Seleccionar")) return@postDelayed
            // Si hay muchos ContactPicker en stack (dual picker), no escribir
            if (containsText(freshRoot, "Seleccionar contacto") || containsText(freshRoot, "Nuevo chat")) return@postDelayed
            val edit = findEditText(freshRoot) ?: return@postDelayed
            android.util.Log.d("AutoAccessibility", "Set message ${sch.message}")
            val bundle = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sch.message) }
            edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                android.os.Handler(mainLooper).postDelayed({
                    val sendRoot = rootInActiveWindow ?: return@postDelayed
                    var sendBtn = findSendButtonById(sendRoot) ?: findSendButton(sendRoot)
                    if (sendBtn == null) {
                        sendBtn = findButtonByText(sendRoot, "Enviar") ?: findNodeWithText(sendRoot, "Enviar")
                    }
                    // Fallback: green send button in bottom-right area (WhatsApp Dual)
                    if (sendBtn == null) {
                        val candidates = mutableListOf<AccessibilityNodeInfo>()
                        collectAllClickable(sendRoot, candidates)
                        val dm = resources.displayMetrics
                        val screenW = dm.widthPixels
                        val screenH = dm.heightPixels
                        sendBtn = candidates.filter { n ->
                            val r = android.graphics.Rect(); n.getBoundsInScreen(r)
                            r.centerX() > screenW * 0.6f && r.centerY() in (screenH * 0.3f).toInt()..(screenH * 0.6f).toInt()
                        }.maxByOrNull {
                            val r = android.graphics.Rect(); it.getBoundsInScreen(r)
                            r.centerX().toFloat()
                        }
                        if (sendBtn != null) {
                            val r = android.graphics.Rect(); sendBtn.getBoundsInScreen(r)
                            android.util.Log.d("AutoAccessibility", "Send btn fallback pos at $r")
                        }
                    }
                val rect = android.graphics.Rect(); sendBtn?.getBoundsInScreen(rect)
                android.util.Log.d("AutoAccessibility", "Send btn found=${sendBtn != null} rect=$rect desc=${sendBtn?.contentDescription}")
                val sent = sendBtn?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
                android.util.Log.d("AutoAccessibility", "Click send sent=$sent")
                showResultNotification(sent, sch)
                pendingSchedule = null
                android.os.Handler(mainLooper).postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 800)
                android.os.Handler(mainLooper).postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 1500)
                android.os.Handler(mainLooper).postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 2200)
            }, 2000)
        }, 3000)
        // Timeout 30s: si no se envió, notificar fallido y cerrar
        android.os.Handler(mainLooper).postDelayed({
            if (pendingSchedule != null && pendingSchedule?.id == sch.id) {
                showResultNotification(false, sch)
                pendingSchedule = null
                waitingForDualPicker = false
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }, 30000)
    }

    internal fun clearPendingPrefs() {
        try { getSharedPreferences("jahc_auto", MODE_PRIVATE).edit().clear().apply() } catch (_: Exception) {}
    }

    internal fun showResultNotification(success: Boolean, sch: Schedule) {
        try {
            clearPendingPrefs()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val chId = "jahc_auto_result"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(chId, "JAHC Auto", NotificationManager.IMPORTANCE_HIGH)
                ch.enableVibration(true)
                nm.createNotificationChannel(ch)
            }
            val title = if (success) "Envío correcto" else "Envío fallido"
            val text = if (success) "Mensaje a ${sch.contactName} enviado" else "No se pudo enviar a ${sch.contactName} - demora/altercado"
            val n = NotificationCompat.Builder(this, chId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()
            nm.notify(sch.id.toInt() + 1000, n)
            android.util.Log.d("AutoAccessibility", "Notificacion $title: $text")
        } catch (e: Exception) { android.util.Log.e("AutoAccessibility", "notif fail", e) }
    }

    internal fun containsText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text?.toString()?.contains(text, true) == true) return true
        if (node.contentDescription?.toString()?.contains(text, true) == true) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsText(child, text)) return true
        }
        return false
    }

    private fun handleDualChooser(root: AccessibilityNodeInfo) {
        // Paso 1: Busca pestaña TRABAJO y haz click si existe
        val trabajoTab = findButtonByText(root, "TRABAJO") ?: findNodeWithText(root, "TRABAJO")
        if (trabajoTab != null) {
            android.util.Log.d("AutoAccessibility", "Dual chooser: clicking TRABAJO tab")
            trabajoTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            // Espera a que cargue la pestaña TRABAJO
            android.os.Handler(mainLooper).postDelayed({
                val freshRoot = rootInActiveWindow ?: return@postDelayed
                clickRightWhatsApp(freshRoot)
            }, 1000)
            return
        }

        // Paso 2: Si no hay pestañas, busca WhatsApp nodes y haz click en el derecho
        clickRightWhatsApp(root)
    }

    private fun clickRightWhatsApp(root: AccessibilityNodeInfo) {
        // MIUI dual chooser: app1=normal, app2=dual. Use resource-id directly
        val app2 = findNodeByResId(root, "android.miui:id/app2")
        if (app2 != null) {
            val rect = android.graphics.Rect(); app2.getBoundsInScreen(rect)
            android.util.Log.d("AutoAccessibility", "Dual click app2 (dual) at $rect")
            app2.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            dualPickerSearched = false
            waitingForDualPicker = true
            android.util.Log.d("AutoAccessibility", "waitingForDualPicker = true (app2)")
            return
        }

        // Fallback: find clickable node whose child has content-desc containing "dual"
        val target = findDualByChildDesc(root)
        if (target != null) {
            val rect = android.graphics.Rect(); target.getBoundsInScreen(rect)
            android.util.Log.d("AutoAccessibility", "Dual click via child desc at $rect")
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            dualPickerSearched = false
            waitingForDualPicker = true
            android.util.Log.d("AutoAccessibility", "waitingForDualPicker = true")
            return
        }

        // Fallback: rightmost clickable node in chooser area
        val allClickable = mutableListOf<AccessibilityNodeInfo>()
        collectAllClickable(root, allClickable)
        val chooserArea = allClickable.filter { n ->
            val rect = android.graphics.Rect(); n.getBoundsInScreen(rect)
            rect.centerY() in 1700..2000
        }
        val rightmost = chooserArea.maxByOrNull { n ->
            val rect = android.graphics.Rect(); n.getBoundsInScreen(rect); rect.centerX()
        }
        if (rightmost != null) {
            val rect = android.graphics.Rect(); rightmost.getBoundsInScreen(rect)
            android.util.Log.d("AutoAccessibility", "Dual click rightmost at $rect")
            rightmost.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            android.util.Log.d("AutoAccessibility", "Dual: no clickable nodes found in chooser")
        }
    }

    private fun findNodeByResId(node: AccessibilityNodeInfo, resId: String): AccessibilityNodeInfo? {
        if (node.viewIdResourceName == resId) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findNodeByResId(child, resId)
            if (res != null) return res
        }
        return null
    }

    private fun findDualByChildDesc(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Check if any child/descendant has content-desc containing "dual"
        val cd = node.contentDescription?.toString() ?: ""
        if (cd.contains("dual", true) || cd.contains("dual", true)) {
            // Return the clickable parent
            var n: AccessibilityNodeInfo? = node
            while (n != null) { if (n.isClickable) return n; n = n.parent }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findDualByChildDesc(child)
            if (res != null) return res
        }
        return null
    }

    internal fun collectAllClickable(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable) out.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectAllClickable(child, out)
        }
    }

    private fun AccessibilityNodeInfo.boundsInScreenRect(): Int {
        val rect = android.graphics.Rect()
        getBoundsInScreen(rect)
        return rect.centerX()
    }

    internal fun findEditText(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className?.toString()?.contains("EditText") == true) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findEditText(child)
            if (res != null) return res
        }
        return null
    }

    internal fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val cls = node.className?.toString() ?: ""
        if (cls.contains("ImageButton") || cls.contains("ImageView")) {
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            if (desc.contains("enviar") || desc.contains("send")) {
                val rect = android.graphics.Rect(); node.getBoundsInScreen(rect)
                if (rect.centerY() > 800) return node
                return node
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findSendButton(child)
            if (res != null) return res
        }
        return null
    }

    internal fun findSendButtonById(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val id = node.viewIdResourceName ?: ""
        if (id.endsWith("/send") || id.contains("send")) {
            var n: AccessibilityNodeInfo? = node
            while (n != null) { if (n.isClickable) return n; n = n.parent }
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findSendButtonById(child)
            if (res != null) return res
        }
        return null
    }

    internal fun findButtonByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val t = node.text?.toString() ?: ""
        val cd = node.contentDescription?.toString() ?: ""
        if ((t.equals(text, true) || cd.equals(text, true)) && node.isClickable) return node
        if (t.contains(text, true) && node.className?.toString()?.contains("Button") == true) {
            var n: AccessibilityNodeInfo? = node
            while (n != null) { if (n.isClickable) return n; n = n.parent }
        }
        if (t == text || cd == text) {
            var n: AccessibilityNodeInfo? = node
            while (n != null) { if (n.isClickable) return n; n = n.parent }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findButtonByText(child, text)
            if (res != null) return res
        }
        return null
    }

    internal fun findNodeWithText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val t = node.text?.toString() ?: ""
        val cd = node.contentDescription?.toString() ?: ""
        if (t == text || cd == text) {
            var n: AccessibilityNodeInfo? = node
            while (n != null) { if (n.isClickable) return n; n = n.parent }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findNodeWithText(child, text)
            if (res != null) return res
        }
        return null
    }

    internal fun findNodeContainingText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val t = node.text?.toString() ?: ""
        val cd = node.contentDescription?.toString() ?: ""
        if (t.contains(text, true) || cd.contains(text, true)) {
            var n: AccessibilityNodeInfo? = node
            while (n != null) { if (n.isClickable) return n; n = n.parent }
            if (node.isClickable) return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findNodeContainingText(child, text)
            if (res != null) return res
        }
        return null
    }

    internal fun findFirstContact(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Busca primer contacto en lista: clickeable, no EditText, con texto visible, en zona media de pantalla
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectContactCandidates(node, candidates)
        if (candidates.isEmpty()) return null
        // Filtra por posición Y (contactos están entre 250-900px), elige el más arriba
        return candidates.minByOrNull { it.boundsInScreenRectY() }
    }

    private fun collectContactCandidates(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable && node.className?.toString()?.contains("EditText") != true) {
            val t = node.text?.toString() ?: ""
            val cd = node.contentDescription?.toString() ?: ""
            val hasText = t.length > 3 || cd.length > 3
            // Evita el EditText de búsqueda y el botón Buscar
            if (hasText && !t.contains("Buscar", true) && !cd.contains("Buscar", true)) {
                val rect = android.graphics.Rect(); node.getBoundsInScreen(rect)
                if (rect.centerY() in 250..900 && rect.centerX() > 100) {
                    out.add(node)
                }
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectContactCandidates(child, out)
        }
    }

    private fun AccessibilityNodeInfo.boundsInScreenRectY(): Int {
        val rect = android.graphics.Rect()
        getBoundsInScreen(rect)
        return rect.centerY()
    }

    private fun hasPinPad(node: AccessibilityNodeInfo): Boolean {
        var count = 0
        fun countDigits(n: AccessibilityNodeInfo) {
            val t = n.text?.toString()?.trim() ?: ""
            val cd = n.contentDescription?.toString()?.trim() ?: ""
            val first = t.firstOrNull()?.toString() ?: cd.firstOrNull()?.toString() ?: ""
            if (first.matches(Regex("[0-9]")) && n.isClickable) count++
            else if ((t.contains(Regex("[0-9]")) || cd.contains(Regex("[0-9]"))) && n.isClickable && t.length <= 6) {
                // Para botones tipo "2\nABC"
                if (t.trim().firstOrNull()?.isDigit() == true) count++
            }
            for (i in 0 until n.childCount) {
                val c = n.getChild(i) ?: continue
                countDigits(c)
            }
        }
        countDigits(node)
        return count >= 8
    }

    private fun handleLockScreen(root: AccessibilityNodeInfo): Boolean {
        val prefs = getSharedPreferences("jahc_auto", MODE_PRIVATE)
        val code = "010798" // PIN fijo para pruebas
        if (code.isEmpty() || !code.all { it.isDigit() }) return false

        // Si es shade de notificaciones -> swipe arriba (debounce 3s solo para swipe)
        if (!hasPinPad(root) && !containsDigitGrid(root)) {
            if (System.currentTimeMillis() - lastLockAction < 3000) return true
            lastLockAction = System.currentTimeMillis()
            android.util.Log.d("AutoAccessibility", "Swipe up to reveal PIN")
            return trySwipeUp()
        }

        // Ya está el pad -> entra PIN async con cooldown de 15s después de PIN exitoso
        if (System.currentTimeMillis() - lastPinSuccess < 15000) return true
        if (isPinRunning) return true
        if (System.currentTimeMillis() - lastPinTap < 2500) return true
        lastPinTap = System.currentTimeMillis()
        isPinRunning = true
        tapDigitsAsync(code)
        return true
    }

    private fun containsDigitGrid(node: AccessibilityNodeInfo): Boolean {
        var count = 0
        fun count(n: AccessibilityNodeInfo) {
            val t = n.text?.toString()?.trim() ?: ""
            val cd = n.contentDescription?.toString()?.trim() ?: ""
            val firstT = t.firstOrNull()?.toString() ?: ""
            val firstCd = cd.firstOrNull()?.toString() ?: ""
            if (firstT.matches(Regex("[0-9]")) || firstCd.matches(Regex("[0-9]"))) count++
            for (i in 0 until n.childCount) {
                val c = n.getChild(i) ?: continue
                count(c)
            }
        }
        count(node)
        return count >= 5
    }

    private fun findDigitBounds(root: AccessibilityNodeInfo, digit: String): PointF? {
        val t = root.text?.toString()?.trim() ?: ""
        val cd = root.contentDescription?.toString()?.trim() ?: ""
        val matches = t == digit || cd == digit || t.startsWith(digit + "\n") || t.startsWith(digit + " ") || t.split("\n").firstOrNull()?.trim() == digit
        if (matches) {
            var n: AccessibilityNodeInfo? = root
            var target: AccessibilityNodeInfo? = null
            while (n != null) {
                if (n.isClickable) { target = n; break }
                n = n.parent
            }
            val nodeForBounds = target ?: root
            val rect = android.graphics.Rect()
            nodeForBounds.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                return PointF(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val res = findDigitBounds(child, digit)
            if (res != null) return res
        }
        return null
    }

    private var lastPinTap = 0L
    @Volatile private var isPinRunning = false
    private fun tapDigitsAsync(code: String) {
        var idx = 0
        var retries = 0
        val handler = android.os.Handler(mainLooper)
        fun next() {
            if (idx >= code.length) {
                android.util.Log.d("AutoAccessibility", "PIN entered ok=true code=$code")
                lastPinSuccess = System.currentTimeMillis()
                isPinRunning = false
                return
            }
            val freshRoot = try { rootInActiveWindow ?: findLockScreenRoot() } catch (_: Exception) { null }
            if (freshRoot == null) {
                handler.postDelayed({ next() }, 500)
                return
            }
            val c = code[idx]
            val p = findDigitBounds(freshRoot, c.toString())
            if (p != null) {
                val path = Path().apply { moveTo(p.x, p.y); lineTo(p.x + 1f, p.y + 1f) }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 120))
                    .build()
                dispatchGesture(gesture, null, null)
                android.util.Log.d("AutoAccessibility", "Tap digit=$c at node (${p.x.toInt()},${p.y.toInt()})")
                idx++
                retries = 0
                handler.postDelayed({ next() }, 400)
            } else {
                android.util.Log.d("AutoAccessibility", "Digit $c not found, swipe")
                trySwipeUp()
                retries++
                if (retries > 3) {
                    val dm = resources.displayMetrics
                    val sw = dm.widthPixels.toFloat()
                    val sh = dm.heightPixels.toFloat()
                    val fallback = when (c) {
                        '0' -> PointF(sw * 0.50f, sh * 0.87f)
                        else -> {
                            val colX = floatArrayOf(sw * 0.22f, sw * 0.50f, sw * 0.78f)
                            val rowY = floatArrayOf(sh * 0.63f, sh * 0.74f, sh * 0.85f)
                            val n = (c - '1')
                            PointF(colX[n % 3], rowY[n / 3])
                        }
                    }
                    val path2 = Path().apply { moveTo(fallback.x, fallback.y); lineTo(fallback.x + 1f, fallback.y + 1f) }
                    val g2 = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path2, 0, 120)).build()
                    dispatchGesture(g2, null, null)
                    android.util.Log.d("AutoAccessibility", "Tap digit=$c fallback (${fallback.x.toInt()},${fallback.y.toInt()})")
                    idx++
                    retries = 0
                    handler.postDelayed({ next() }, 400)
                } else {
                    handler.postDelayed({ next() }, 900)
                }
            }
        }
        handler.post { next() }
    }

    private fun tapDigitsByNode(root: AccessibilityNodeInfo, code: String): Boolean {
        tapDigitsAsync(code)
        return true
    }

    private fun trySwipeUp(): Boolean {
        // No hacer swipe si estamos en conversación o buscador — evita escribir "bu bu"
        try {
            val ar = rootInActiveWindow
            if (ar != null) {
                if (containsText(ar, "Mi respuesta") || containsText(ar, "Escribe un mensaje")) return false
                val pkg = ar.packageName?.toString()
                if (pkg == "com.google.android.googlequicksearchbox") return false
                if (findEditText(ar) != null && pkg !in listOf("com.android.systemui", "android")) return false
            }
        } catch (_: Exception) {}
        return try {
            val dm = resources.displayMetrics
            val startX = (dm.widthPixels / 2).toFloat()
            val startY = (dm.heightPixels * 0.96).toFloat()
            val endY = (dm.heightPixels * 0.30).toFloat()
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
                .build()
            dispatchGesture(gesture, null, null)
            true
        } catch (e: Exception) {
            android.util.Log.e("AutoAccessibility", "Swipe failed", e)
            false
        }
    }

    override fun onInterrupt() {}
    override fun onDestroy() { enabled = false; super.onDestroy() }
}
