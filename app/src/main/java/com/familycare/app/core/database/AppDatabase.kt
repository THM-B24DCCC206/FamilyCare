
package com.familycare.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HealthMetricEntity::class,
        MedicineEntity::class,
        AlertEntity::class,
        AppointmentEntity::class,
        MedicationLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract val healthMetricDao: HealthMetricDao
    abstract val medicineDao: MedicineDao
    abstract val alertDao: AlertDao
    abstract val appointmentDao: AppointmentDao
    abstract val medicationLogDao: MedicationLogDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS appointments (
                        id TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        hospitalName TEXT NOT NULL,
                        department TEXT NOT NULL,
                        doctorName TEXT NOT NULL,
                        appointmentTime INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS medication_logs (
                        id TEXT NOT NULL,
                        medicineId TEXT NOT NULL,
                        medicineName TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        scheduledDate TEXT NOT NULL,
                        scheduledTime TEXT NOT NULL,
                        actualTakenTime INTEGER,
                        isTaken INTEGER NOT NULL,
                        isSkipped INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_medication_logs_medicineId_scheduledDate_scheduledTime
                    ON medication_logs (
                        medicineId,
                        scheduledDate,
                        scheduledTime
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_medication_logs_userId_scheduledDate
                    ON medication_logs (userId, scheduledDate)
                    """.trimIndent()
                )
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE medication_logs
                    ADD COLUMN overdueNotified INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "familycare_db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
