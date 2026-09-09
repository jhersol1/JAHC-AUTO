package com.jahc.auto.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jahc.auto.data.AppDatabase
import com.jahc.auto.databinding.ActivityMainBinding
import com.jahc.auto.worker.ScheduleWorker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val db by lazy { AppDatabase.get(this) }
    private lateinit var adapter: ScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ScheduleAdapter(
            onToggle = { sch, enabled ->
                lifecycleScope.launch {
                    db.scheduleDao().upsert(sch.copy(enabled = enabled))
                    if (enabled) ScheduleWorker.scheduleNext(this@MainActivity, sch.copy(enabled = true))
                    else ScheduleWorker.cancel(this@MainActivity, sch.id)
                }
            },
            onEdit = { sch -> startActivity(Intent(this, EditActivity::class.java).putExtra("id", sch.id)) },
            onDelete = { sch ->
                lifecycleScope.launch {
                    db.scheduleDao().delete(sch)
                    ScheduleWorker.cancel(this@MainActivity, sch.id)
                }
            }
        )
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.fab.setOnClickListener { startActivity(Intent(this, EditActivity::class.java)) }
        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Activa JAHC Auto", Toast.LENGTH_LONG).show()
        }
        binding.btnPin.setOnClickListener { showPinDialog() }
        binding.btnTestLock.setOnClickListener { startActivity(Intent(this, TestLockActivity::class.java)) }
        // Muestra PIN guardado si existe
        updatePinButton()

        lifecycleScope.launch {
            db.scheduleDao().observeAll().collectLatest { list ->
                adapter.submitList(list)
                binding.emptyView.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun showPinDialog() {
        val prefs = getSharedPreferences("jahc_auto", MODE_PRIVATE)
        val current = prefs.getString("pin_code", "") ?: ""
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN 6 dígitos o Patrón (ej. 1235789)"
            setText(current)
            filters = arrayOf(android.text.InputFilter.LengthFilter(9))
            textSize = 18f
            setPadding(40, 40, 40, 40)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Desbloqueo automático")
            .setMessage("PIN 6 dígitos (ej. 123456) o Patrón del 1-9 (ej. 1235789 donde 1=arriba-izq, 9=abajo-der).")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val pin = input.text.toString().trim()
                if (pin.isEmpty() || pin.length < 4 || !pin.all { it.isDigit() }) {
                    Toast.makeText(this, "Debe ser 4-9 dígitos", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit().putString("pin_code", pin).apply()
                    updatePinButton()
                    Toast.makeText(this, "Guardado: $pin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Borrar") { _, _ ->
                prefs.edit().remove("pin_code").apply()
                updatePinButton()
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun updatePinButton() {
        val pin = getSharedPreferences("jahc_auto", MODE_PRIVATE).getString("pin_code", "")
        binding.btnPin.text = if (pin.isNullOrEmpty()) "🔒 PIN 6 dígitos" else "🔒 PIN: ••••••"
    }
}
