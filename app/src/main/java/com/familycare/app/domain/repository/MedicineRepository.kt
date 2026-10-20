package com.familycare.app.domain.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.domain.model.MedicationLog
import com.familycare.app.domain.model.Medicine
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun getMedicinesByMember(userId: String): Flow<List<Medicine>>
    fun getTodayLogs(userId: String): Flow<List<MedicationLog>>
    suspend fun addMedicine(medicine: Medicine): Resource<Unit>
    suspend fun markLogAsTaken(logId: String): Resource<Unit>
    suspend fun getMonthlyComplianceRate(userId: String): Double
}
