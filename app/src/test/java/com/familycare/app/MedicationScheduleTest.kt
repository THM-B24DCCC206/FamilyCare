package com.familycare.app

import com.familycare.app.presentation.home.DoseStatus
import com.familycare.app.presentation.home.MedicationSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationScheduleTest {

    @Test
    fun testDoseStatus_upcomingWhenMoreThan30MinutesBefore() {
        val schedule = MedicationSchedule(
            id = "test_1",
            medicineId = "med_1",
            medicineName = "Amlodipine",
            dosage = "5mg",
            instruction = "Sau ăn sáng",
            scheduledTime = "08:00" // 480 minutes
        )

        // At 07:00 (420 minutes, -60 mins diff) -> UPCOMING
        val status = schedule.computeStatus(currentMinutesOfDay = 420)
        assertEquals(DoseStatus.UPCOMING, status)
    }

    @Test
    fun testDoseStatus_dueWithin30MinutesBeforeAnd60MinutesAfter() {
        val schedule = MedicationSchedule(
            id = "test_2",
            medicineId = "med_2",
            medicineName = "Panadol",
            dosage = "500mg",
            instruction = "Sau ăn trưa",
            scheduledTime = "12:00" // 720 minutes
        )

        // At 11:35 (695 mins, -25 mins diff) -> DUE
        assertEquals(DoseStatus.DUE, schedule.computeStatus(currentMinutesOfDay = 695))

        // At 12:00 (720 mins, 0 min diff) -> DUE
        assertEquals(DoseStatus.DUE, schedule.computeStatus(currentMinutesOfDay = 720))

        // At 12:59 (779 mins, +59 mins diff) -> DUE
        assertEquals(DoseStatus.DUE, schedule.computeStatus(currentMinutesOfDay = 779))
    }

    @Test
    fun testDoseStatus_overdueWhenMoreThan60MinutesAfterAndNotTaken() {
        val schedule = MedicationSchedule(
            id = "test_3",
            medicineId = "med_3",
            medicineName = "Metformin",
            dosage = "850mg",
            instruction = "Sau ăn tối",
            scheduledTime = "19:00", // 1140 minutes
            isTaken = false,
            isSkipped = false
        )

        // At 20:30 (1230 mins, +90 mins diff) -> OVERDUE
        val status = schedule.computeStatus(currentMinutesOfDay = 1230)
        assertEquals(DoseStatus.OVERDUE, status)
    }

    @Test
    fun testDoseStatus_takenOverridesTimeDifferences() {
        val schedule = MedicationSchedule(
            id = "test_4",
            medicineId = "med_4",
            medicineName = "Omega 3",
            dosage = "1 viên",
            instruction = "Uống với nước",
            scheduledTime = "08:00",
            isTaken = true,
            actualTakenTime = System.currentTimeMillis()
        )

        // Even if checked at 22:00 (far overdue), status is TAKEN
        val status = schedule.computeStatus(currentMinutesOfDay = 1320)
        assertEquals(DoseStatus.TAKEN, status)
    }

    @Test
    fun testDoseStatus_skippedOverridesTimeDifferences() {
        val schedule = MedicationSchedule(
            id = "test_5",
            medicineId = "med_5",
            medicineName = "Vitamin C",
            dosage = "1000mg",
            instruction = "Sau ăn",
            scheduledTime = "09:00",
            isTaken = false,
            isSkipped = true
        )

        // Even if checked at 15:00, status is SKIPPED
        val status = schedule.computeStatus(currentMinutesOfDay = 900)
        assertEquals(DoseStatus.SKIPPED, status)
    }

    @Test
    fun testChronologicalSorting() {
        val doses = listOf(
            MedicationSchedule("1", "m1", "Thuốc tối", "1 viên", "", "20:30"),
            MedicationSchedule("2", "m2", "Thuốc sáng", "1 viên", "", "07:00"),
            MedicationSchedule("3", "m3", "Thuốc trưa", "1 viên", "", "12:15"),
            MedicationSchedule("4", "m4", "Thuốc chiều", "1 viên", "", "16:45")
        )

        val sorted = doses.sortedBy { it.scheduledTime }
        assertEquals(listOf("07:00", "12:15", "16:45", "20:30"), sorted.map { it.scheduledTime })
    }

    @Test
    fun testPendingDosesCalculationAndAllDoneState() {
        val doses = listOf(
            MedicationSchedule("1", "m1", "Thuốc 1", "1 viên", "", "08:00", isTaken = true),
            MedicationSchedule("2", "m2", "Thuốc 2", "1 viên", "", "12:00", isTaken = false, isSkipped = false),
            MedicationSchedule("3", "m3", "Thuốc 3", "1 viên", "", "20:00", isTaken = false, isSkipped = true)
        )

        val pendingCount = doses.count { !it.isTaken && !it.isSkipped }
        assertEquals(1, pendingCount)

        val allDoneInitially = doses.isNotEmpty() && pendingCount == 0
        assertFalse(allDoneInitially)

        // After completing dose 2
        val updatedDoses = doses.map { if (it.id == "2") it.copy(isTaken = true) else it }
        val updatedPendingCount = updatedDoses.count { !it.isTaken && !it.isSkipped }
        assertEquals(0, updatedPendingCount)

        val allDoneFinal = updatedDoses.isNotEmpty() && updatedPendingCount == 0
        assertTrue(allDoneFinal)
    }

    @Test
    fun testTakenTimeFormatted() {
        val timestamp = 1700000000000L // arbitrary fixed timestamp
        val schedule = MedicationSchedule(
            id = "1",
            medicineId = "m1",
            medicineName = "Thuốc A",
            dosage = "1 viên",
            instruction = "",
            scheduledTime = "08:00",
            isTaken = true,
            actualTakenTime = timestamp
        )

        val expected = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        assertEquals(expected, schedule.takenTimeFormatted)
    }
}
