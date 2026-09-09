package com.jahc.auto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val target: String = "whatsapp", // whatsapp | whatsapp_business | whatsapp_dual
    val contactName: String = "",
    val phone: String = "",
    val message: String = "",
    val hour: Int = 9,
    val minute: Int = 0,
    val repeatDaily: Boolean = true,
    val daysOfWeek: String = "1111111", // Mon..Sun 1=enabled
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSentAt: Long? = null
) {
    fun targetPackage(): String = when (target) {
        "whatsapp_business" -> "com.whatsapp.w4b"
        else -> "com.whatsapp"
    }
    fun isDual(): Boolean = target == "whatsapp_dual"
    fun timeLabel(): String = String.format("%02d:%02d", hour, minute)
}
