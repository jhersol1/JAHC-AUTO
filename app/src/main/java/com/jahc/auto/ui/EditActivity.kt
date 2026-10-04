package com.jahc.auto.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jahc.auto.data.AppDatabase
import com.jahc.auto.data.Schedule
import com.jahc.auto.databinding.ActivityEditBinding
import com.jahc.auto.worker.ScheduleWorker
import kotlinx.coroutines.launch

class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    private val db by lazy { AppDatabase.get(this) }
    private var editingId: Long = -1
    private var hour = 9
    private var minute = 0
    private var year = 0
    private var month = 0
    private var day = 0

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchContactPicker() else Toast.makeText(this, "Permiso de contactos denegado", Toast.LENGTH_SHORT).show()
    }

    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            val cursor = contentResolver.query(uri, null, null, null, null) ?: return@registerForActivityResult
            cursor.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "" else ""
                    if (name.isNotBlank()) binding.editContact.setText(name)
                    val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
                    val hasPhoneIdx = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    if (idIdx >= 0 && hasPhoneIdx >= 0 && it.getInt(hasPhoneIdx) > 0) {
                        val contactId = it.getString(idIdx)
                        val phones = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?", arrayOf(contactId), null
                        )
                        phones?.use { pc ->
                            if (pc.moveToFirst()) {
                                val phoneIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                val rawPhone = if (phoneIdx >= 0) pc.getString(phoneIdx) ?: "" else ""
                                val phone = formatPhone(rawPhone)
                                if (phone.isNotBlank()) binding.editPhone.setText(phone)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error leyendo contacto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchContactPicker() {
        pickContactLauncher.launch(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val targets = arrayOf("WhatsApp", "WhatsApp Business", "WhatsApp Dual")
        binding.spinnerTarget.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, targets)

        // Repite como Skedit
        val repeats = arrayOf("No Repetir", "Diario")
        binding.spinnerRepeat.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeats)
        // Fecha inicial hoy
        val cal = java.util.Calendar.getInstance()
        year = cal.get(java.util.Calendar.YEAR); month = cal.get(java.util.Calendar.MONTH); day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        updateDateButton()
        updateTimeButton()

        editingId = intent.getLongExtra("id", -1)
        if (editingId != -1L) {
            lifecycleScope.launch {
                val s = db.scheduleDao().getById(editingId) ?: return@launch
                binding.editTitle.setText(s.title)
                binding.editContact.setText(s.contactName)
                binding.editPhone.setText(formatPhone(s.phone))
                binding.editMessage.setText(s.message)
                hour = s.hour; minute = s.minute
                updateTimeButton()
                binding.spinnerTarget.setSelection(when(s.target) {
                    "whatsapp_business" -> 1; "whatsapp_dual" -> 2; else -> 0
                })
                binding.spinnerRepeat.setSelection(if (s.repeatDaily) 1 else 0)
                binding.switchDaily.isChecked = s.repeatDaily
            }
        }

        binding.btnDate.setOnClickListener {
            android.app.DatePickerDialog(this, { _, y, m, d -> year = y; month = m; day = d; updateDateButton() }, year, month, day).show()
        }
        binding.btnTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> hour = h; minute = m; updateTimeButton() }, hour, minute, true).show()
        }

        binding.btnPickContact.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                launchContactPicker()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }

        binding.btnSave.setOnClickListener {
            val msg = binding.editMessage.text.toString().trim()
            if (msg.isEmpty()) { Toast.makeText(this, "Mensaje vacío", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val target = when(binding.spinnerTarget.selectedItemPosition) {
                1 -> "whatsapp_business"; 2 -> "whatsapp_dual"; else -> "whatsapp"
            }
            val repeatDaily = binding.spinnerRepeat.selectedItemPosition == 1
            val normalizedPhone = normalizePhone(binding.editPhone.text.toString().trim())
            val sch = Schedule(
                id = if (editingId == -1L) 0 else editingId,
                title = binding.editTitle.text.toString().trim(),
                target = target,
                contactName = binding.editContact.text.toString().trim(),
                phone = normalizedPhone,
                message = msg,
                hour = hour, minute = minute,
                repeatDaily = repeatDaily,
                enabled = true
            )
            lifecycleScope.launch {
                val isEdit = editingId != -1L
                if (isEdit) {
                    android.util.Log.d("EditActivity", "[SCHEDULE] programación editada id=$editingId")
                    ScheduleWorker.cancel(this@EditActivity, editingId)
                    ScheduleWorker.cancelAlarm(this@EditActivity, editingId)
                }
                val newId = db.scheduleDao().upsert(sch)
                val saved = sch.copy(id = if (sch.id == 0L) newId else sch.id)
                ScheduleWorker.scheduleNext(this@EditActivity, saved, if (isEdit) "reprogramada" else "creada")
                Toast.makeText(this@EditActivity, "Guardado permanente", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun updateDateButton() {
        val months = arrayOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sep.", "oct.", "nov.", "dic.")
        binding.btnDate.text = "${months[month]} $day, $year"
    }
    private fun updateTimeButton() {
        val ampm = if (hour < 12) "a. m." else "p. m."
        val h12 = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        binding.btnTime.text = String.format("%02d:%02d %s", h12, minute, ampm)
    }

    private fun formatPhone(raw: String): String {
        var digits = raw.filter { it.isDigit() }
        if (digits.length == 9) digits = "51$digits"
        return if (digits.isNotBlank()) "+$digits" else raw
    }
    private fun normalizePhone(raw: String): String {
        var digits = raw.filter { it.isDigit() }
        if (digits.length == 9) digits = "51$digits"
        return digits
    }
}
