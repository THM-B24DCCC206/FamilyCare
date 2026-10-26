
package com.familycare.app.data.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.core.database.MedicineDao
import com.familycare.app.core.database.MedicineEntity
import com.familycare.app.domain.model.MedicationLog
import com.familycare.app.domain.model.Medicine
import com.familycare.app.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class MedicineRepositoryImpl(
    private val medicineDao: MedicineDao
) : MedicineRepository {

    override fun getMedicinesByMember(
        userId: String
    ): Flow<List<Medicine>> {
        return medicineDao.getMedicinesByMember(userId)
            .map { medicines ->
                medicines.map { it.toDomain() }
            }
    }

    override fun getTodayLogs(
        userId: String
    ): Flow<List<MedicationLog>> {
        // Chưa có bảng lưu nhật ký uống thuốc.
        return flowOf(emptyList())
    }

    override suspend fun addMedicine(
        medicine: Medicine
    ): Resource<Unit> {
        return try {
            medicineDao.insertMedicine(
                MedicineEntity.fromDomain(medicine)
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể lưu thuốc")
        }
    }

    override suspend fun markLogAsTaken(
        logId: String
    ): Resource<Unit> {
        return Resource.Error(
            "Chức năng ghi nhận uống thuốc chưa được triển khai"
        )
    }

    override suspend fun getMonthlyComplianceRate(
        userId: String
    ): Double {
        // Chưa có dữ liệu nhật ký để tính tỷ lệ.
        return 0.0
    }
}
