package com.jahc.auto.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY hour, minute")
    fun observeAll(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules")
    suspend fun getAllOnce(): List<Schedule>

    @Query("SELECT * FROM schedules WHERE enabled = 1")
    suspend fun getEnabled(): List<Schedule>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): Schedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: Schedule): Long

    @Delete
    suspend fun delete(schedule: Schedule)

    @Query("UPDATE schedules SET lastSentAt = :ts WHERE id = :id")
    suspend fun markSent(id: Long, ts: Long)
}
