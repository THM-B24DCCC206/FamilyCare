
package com.familycare.app.data.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.core.database.MedicationLogDao
import com.familycare.app.core.database.MedicationLogEntity
import com.familycare.app.core.database.MedicineDao
import com.familycare.app.core.database.MedicineEntity
import com.familycare.app.domain.model.MedicationLog
import com.familycare.app.domain.model.Medicine
import com.familycare.app.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicineRepositoryImpl(
    private val medicineDao: MedicineDao,
    private val medicationLogDao: MedicationLogDao
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
        val today = getDateString(System.currentTimeMillis())

        return medicationLogDao.getLogsForDate(userId, today)
            .map { logs ->
                logs.map { log ->
                    MedicationLog(
                        id = log.id,
                        medicineId = log.medicineId,
                        medicineName = log.medicineName,
                        userId = log.userId,
                        scheduledTime =
                            "${log.scheduledDate} ${log.scheduledTime}",
                        actualTakenTime = log.actualTakenTime,
                        isTaken = log.isTaken,
                        isSkipped = log.isSkipped
                    )
                }
            }
    }

    /**
     * Tạo nhật ký cho các giờ uống thuốc trong ngày.
     * Nhật ký đã tồn tại sẽ không bị ghi đè.
     */
    suspend fun ensureTodayLogs(
        medicines: List<Medicine>,
        userId: String
    ): Resource<Unit> {
        return try {
            val today = getDateString(System.currentTimeMillis())

            medicines.forEach { medicine ->
                medicine.reminderTimes.distinct().forEach { time ->
                    val logId = "${medicine.id}_${today}_$time"

                    medicationLogDao.insertIfAbsent(
                        MedicationLogEntity(
                            id = logId,
                            medicineId = medicine.id,
                            medicineName = medicine.name,
                            userId = userId,
                            scheduledDate = today,
                            scheduledTime = time
                        )
                    )
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(
                e.message ?: "Không thể tạo nhật ký uống thuốc"
            )
        }
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
            Resource.Error(
                e.message ?: "Không thể lưu thuốc"
            )
        }
    }

    override suspend fun markLogAsTaken(
        logId: String
    ): Resource<Unit> {
        return try {
            val log = medicationLogDao.getLogById(logId)
                ?: return Resource.Error(
                    "Không tìm thấy nhật ký uống thuốc"
                )

            if (!log.isTaken) {
                medicationLogDao.markAsTaken(
                    logId = logId,
                    takenAt = System.currentTimeMillis()
                )
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(
                e.message ?: "Không thể ghi nhận đã uống thuốc"
            )
        }
    }

    override suspend fun getMonthlyComplianceRate(
        userId: String
    ): Double {
        // Sẽ bổ sung phép tính khi hoàn thiện truy vấn nhật ký theo tháng.
        return 0.0
    }

    private fun getDateString(timestamp: Long): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.ROOT
        ).format(Date(timestamp))
    }
}
