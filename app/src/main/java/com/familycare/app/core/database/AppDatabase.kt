
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
        AppointmentEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val healthMetricDao: HealthMetricDao
    abstract val medicineDao: MedicineDao
    abstract val alertDao: AlertDao
    abstract val appointmentDao: AppointmentDao

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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "familycare_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
