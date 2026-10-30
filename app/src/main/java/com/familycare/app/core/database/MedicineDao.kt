package com.familycare.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines WHERE userId = :userId ORDER BY startDate DESC")
    fun getMedicinesByMember(userId: String): Flow<List<MedicineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity)

    @Query("DELETE FROM medicines WHERE id = :medicineId")
    suspend fun deleteMedicine(medicineId: String)
    @Query("SELECT * FROM medicines WHERE id = :medicineId LIMIT 1")
    suspend fun getMedicineById(medicineId: String): MedicineEntity?

    @androidx.room.Update
    suspend fun updateMedicine(medicine: MedicineEntity)
}