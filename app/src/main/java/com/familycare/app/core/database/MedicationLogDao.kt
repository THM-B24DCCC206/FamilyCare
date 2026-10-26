
package com.familycare.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationLogDao {

    @Query(
        """
        SELECT * FROM medication_logs
        WHERE userId = :userId
          AND scheduledDate = :date
        ORDER BY scheduledTime ASC
        """
    )
    fun getLogsForDate(
        userId: String,
        date: String
    ): Flow<List<MedicationLogEntity>>

    @Query(
        """
        SELECT * FROM medication_logs
        WHERE medicineId = :medicineId
          AND scheduledDate = :date
          AND scheduledTime = :time
        LIMIT 1
        """
    )
    suspend fun getLog(
        medicineId: String,
        date: String,
        time: String
    ): MedicationLogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(log: MedicationLogEntity): Long

    @Query(
        """
        UPDATE medication_logs
        SET isTaken = 1,
            isSkipped = 0,
            actualTakenTime = :takenAt
        WHERE id = :logId
        """
    )
    suspend fun markAsTaken(
        logId: String,
        takenAt: Long
    )

    @Query(
        """
        SELECT * FROM medication_logs
        WHERE id = :logId
        LIMIT 1
        """
    )
    suspend fun getLogById(logId: String): MedicationLogEntity?
}
