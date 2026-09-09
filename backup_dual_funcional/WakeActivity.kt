package com.jahc.auto.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jahc.auto.service.AutoAccessibilityService

class WakeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        // Intenta desbloquear
        try {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, null)
            }
        } catch (_: Exception) {}

        val schedule = AutoAccessibilityService.pendingSchedule
        // Lanza WhatsApp después de 600ms para que el sistema despierte
        android.os.Handler(mainLooper).postDelayed({
            if (schedule != null) {
                val pkg = schedule.targetPackage()
                val uri = if (schedule.phone.isNotBlank()) {
                    var phone = schedule.phone.filter { it.isDigit() }
                    if (phone.length == 9) phone = "51$phone"
                    "https://wa.me/$phone?text=${java.net.URLEncoder.encode(schedule.message, "UTF-8")}"
                } else {
                    "https://wa.me/?text=${java.net.URLEncoder.encode(schedule.message, "UTF-8")}"
                }
                // Para dual: ACTION_SEND como Facebook/SKEdit - muestra selector MIUI 2 WhatsApp
                if (schedule.isDual()) {
                    android.util.Log.d("WakeActivity", "Dual: ACTION_SEND to trigger MIUI dual chooser")
                    try {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, schedule.message)
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    } catch (_: Exception) {}
                } else {
                    val baseIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(uri)
                        `package` = pkg
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { startActivity(baseIntent) } catch (_: Exception) {
                        val fallback = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        try { startActivity(fallback) } catch (_: Exception) {}
                    }
                }
            }
            finish()
        }, 700)
    }
}
