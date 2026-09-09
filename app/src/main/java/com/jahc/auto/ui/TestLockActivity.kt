package com.jahc.auto.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jahc.auto.databinding.ActivityTestLockBinding

class TestLockActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestLockBinding
    private val entered = StringBuilder()
    private var correctPin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        correctPin = getSharedPreferences("jahc_auto", MODE_PRIVATE).getString("pin_code", "") ?: ""
        binding.txtStatus.text = "Simulación PIN 6 - Ingresa: $correctPin (test)"
        setupButtons()
        binding.btnClear.setOnClickListener { entered.clear(); updateDots() }
        binding.btnTestAuto.setOnClickListener {
            entered.clear(); updateDots()
            // Dispara la accesibilidad como si fuera systemui: pone pendingSchedule fake y llama al servicio vía broadcast
            com.jahc.auto.service.AutoAccessibilityService.pendingSchedule = com.jahc.auto.data.Schedule(id = -1, contactName = "TEST", phone = "0", message = "test")
            Toast.makeText(this, "Lanza JAHC Auto ahora (accesibilidad probará PIN)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtons() {
        val buttons = listOf(binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4, binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9)
        for (b in buttons) {
            b.setOnClickListener {
                val d = b.text.toString().trim().firstOrNull() ?: return@setOnClickListener
                entered.append(d)
                updateDots()
                if (entered.length == 6) checkPin()
            }
        }
    }

    private fun updateDots() {
        val dots = "● ".repeat(entered.length) + "○ ".repeat(6 - entered.length)
        binding.txtDots.text = dots.trim()
        binding.txtEntered.text = entered.toString()
    }

    private fun checkPin() {
        if (entered.toString() == correctPin) {
            binding.txtStatus.text = "✅ PIN Correcto! Simulación desbloqueada"
            Toast.makeText(this, "Desbloqueo simulado OK", Toast.LENGTH_SHORT).show()
        } else {
            binding.txtStatus.text = "❌ PIN Incorrecto: ${entered} != $correctPin"
            Toast.makeText(this, "PIN falló - no bloquea sistema", Toast.LENGTH_SHORT).show()
        }
        entered.clear()
        updateDots()
    }
}
