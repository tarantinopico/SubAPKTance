package com.antigravity.substance.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antigravity.substance.data.db.entities.IntakeCustomFieldValueEntity
import com.antigravity.substance.data.db.entities.IntakeLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeLogDao {
    @Query("SELECT * FROM intake_logs ORDER BY timestampMillis DESC")
    fun observeAllLogs(): Flow<List<IntakeLogEntity>>

    @Query("SELECT * FROM intake_logs WHERE timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC")
    fun observeLogsSince(sinceMillis: Long): Flow<List<IntakeLogEntity>>

    @Query("SELECT * FROM intake_logs WHERE substanceId = :substanceId ORDER BY timestampMillis DESC")
    fun observeLogsForSubstance(substanceId: Long): Flow<List<IntakeLogEntity>>

    @Query("SELECT * FROM intake_logs ORDER BY timestampMillis DESC")
    suspend fun getAllLogsOnce(): List<IntakeLogEntity>

    @Query("SELECT * FROM intake_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): IntakeLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: IntakeLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<IntakeLogEntity>): List<Long>

    @Update
    suspend fun updateLog(log: IntakeLogEntity)

    @Delete
    suspend fun deleteLog(log: IntakeLogEntity)

    @Query("DELETE FROM intake_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM intake_logs WHERE substanceId = :substanceId")
    suspend fun deleteLogsForSubstance(substanceId: Long)

    @Query("DELETE FROM intake_logs")
    suspend fun deleteAllLogs()

    // EAV Custom Field Values
    @Query("SELECT * FROM intake_custom_field_values WHERE logId = :logId")
    suspend fun getCustomFieldValuesForLog(logId: Long): List<IntakeCustomFieldValueEntity>

    @Query("SELECT * FROM intake_custom_field_values")
    fun observeAllCustomFieldValues(): Flow<List<IntakeCustomFieldValueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFieldValues(values: List<IntakeCustomFieldValueEntity>)

    @Query("DELETE FROM intake_custom_field_values WHERE logId = :logId")
    suspend fun deleteCustomFieldValuesForLog(logId: Long)

    @Query("DELETE FROM intake_custom_field_values")
    suspend fun deleteAllCustomFieldValues()
}
