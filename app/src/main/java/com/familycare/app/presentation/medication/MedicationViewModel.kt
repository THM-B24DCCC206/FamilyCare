
package com.familycare.app.presentation.medication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.data.repository.MedicineRepositoryImpl
import com.familycare.app.domain.model.Medicine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MedicationViewModel(application: Application) :
    AndroidViewModel(application) {

    private val medicineDao = AppDatabase.getInstance(application).medicineDao
    private val repository = MedicineRepositoryImpl(medicineDao)
    private val alarmScheduler = MedicationAlarmScheduler(application)
    private val userId = "demo-user"

    val medicines = repository.getMedicinesByMember(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addMedicine(
        name: String,
        dosage: String,
        instruction: String,
        reminderTimes: List<String>
    ) {
        if (name.isBlank() || dosage.isBlank()) return

        viewModelScope.launch {
            val medicine = Medicine(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name.trim(),
                dosage = dosage.trim(),
                instruction = instruction.trim(),
                reminderTimes = reminderTimes.distinct()
            )

            repository.addMedicine(medicine)
            alarmScheduler.schedule(
                medicine.id, medicine.name, medicine.dosage,
                medicine.reminderTimes
            )
        }
    }

    fun addOcrMedicines(candidates: List<String>) {
        viewModelScope.launch {
            candidates.map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { candidate ->
                    val lines = candidate.lines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    if (lines.isNotEmpty()) {
                        repository.addMedicine(
                            Medicine(
                                id = UUID.randomUUID().toString(),
                                userId = userId,
                                name = lines.first(),
                                dosage = "Cần xác nhận theo đơn thuốc",
                                instruction = lines.drop(1).joinToString("\n"),
                                reminderTimes = emptyList(),
                                scannedViaOcr = true
                            )
                        )
                    }
                }
        }
    }

    fun updateMedicine(
        medicine: Medicine,
        name: String,
        dosage: String,
        instruction: String,
        reminderTimes: List<String>
    ) {
        if (name.isBlank() || dosage.isBlank()) return

        viewModelScope.launch {
            val updated = medicine.copy(
                name = name.trim(),
                dosage = dosage.trim(),
                instruction = instruction.trim(),
                reminderTimes = reminderTimes.distinct()
            )

            alarmScheduler.cancel(medicine.id, medicine.reminderTimes)
            repository.addMedicine(updated)
            alarmScheduler.schedule(
                updated.id, updated.name, updated.dosage,
                updated.reminderTimes
            )
        }
    }

    fun deleteMedicine(medicineId: String) {
        viewModelScope.launch {
            val medicine = medicines.value.firstOrNull { it.id == medicineId }
            if (medicine != null) {
                alarmScheduler.cancel(medicine.id, medicine.reminderTimes)
            }
            medicineDao.deleteMedicine(medicineId)
        }
    }
}
