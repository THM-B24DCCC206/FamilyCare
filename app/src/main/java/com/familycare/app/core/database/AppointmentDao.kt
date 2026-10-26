
package com.familycare.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

    @Query(
        "SELECT * FROM appointments " +
        "WHERE userId = :userId " +
        "ORDER BY appointmentTime ASC"
    )
    fun getAppointmentsByMember(
        userId: String
    ): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(
        appointment: AppointmentEntity
    )

    @Query("DELETE FROM appointments WHERE id = :appointmentId")
    suspend fun deleteAppointment(appointmentId: String)
}
