package com.familycare.app.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    val healthMetricDao: HealthMetricDao = HealthMetricDaoImpl(dbHelper)
    fun healthMetricDao(): HealthMetricDao = healthMetricDao

    val medicineDao: MedicineDao = MedicineDaoImpl(dbHelper)
    fun medicineDao(): MedicineDao = medicineDao

    val alertDao: AlertDao = AlertDaoImpl(dbHelper)
    fun alertDao(): AlertDao = alertDao

    val userDao: UserDao = UserDaoImpl(dbHelper)
    fun userDao(): UserDao = userDao

    val appointmentDao: AppointmentDao = AppointmentDaoImpl(dbHelper)
    fun appointmentDao(): AppointmentDao = appointmentDao

    val medicationLogDao: MedicationLogDao = MedicationLogDaoImpl(dbHelper)
    fun medicationLogDao(): MedicationLogDao = medicationLogDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "familycare_db.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS health_metrics (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                type TEXT NOT NULL,
                valuePrimary REAL NOT NULL,
                valueSecondary REAL,
                unit TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                notes TEXT NOT NULL,
                isFromCameraPpg INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS medicines (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                name TEXT NOT NULL,
                dosage TEXT NOT NULL,
                instruction TEXT NOT NULL,
                reminderTimes TEXT NOT NULL,
                startDate INTEGER NOT NULL,
                endDate INTEGER,
                scannedViaOcr INTEGER NOT NULL,
                purpose TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                memberId TEXT NOT NULL DEFAULT '',
                memberName TEXT NOT NULL DEFAULT '',
                targetUserId TEXT NOT NULL DEFAULT '',
                targetUserName TEXT NOT NULL DEFAULT '',
                alertType TEXT NOT NULL DEFAULT 'THRESHOLD_EXCEEDED',
                alertLevel TEXT NOT NULL DEFAULT 'WARNING',
                type TEXT NOT NULL DEFAULT 'THRESHOLD_EXCEEDED',
                level TEXT NOT NULL DEFAULT 'WARNING',
                title TEXT NOT NULL DEFAULT '',
                message TEXT NOT NULL DEFAULT '',
                address TEXT,
                latitude REAL,
                longitude REAL,
                timestamp INTEGER NOT NULL DEFAULT 0,
                isRead INTEGER NOT NULL DEFAULT 0,
                isResolved INTEGER NOT NULL DEFAULT 0,
                familyId TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL DEFAULT '',
                fullName TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'MEMBER',
                familyId TEXT NOT NULL DEFAULT '',
                familyName TEXT NOT NULL DEFAULT '',
                inviteCode TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL DEFAULT 0,
                avatarUrl TEXT NOT NULL DEFAULT '',
                birthDate TEXT NOT NULL DEFAULT '',
                bloodType TEXT NOT NULL DEFAULT '',
                chronicDiseasesJson TEXT NOT NULL DEFAULT '[]',
                drugAllergiesJson TEXT NOT NULL DEFAULT '[]'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS appointments (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                hospitalName TEXT NOT NULL,
                department TEXT NOT NULL,
                doctorName TEXT NOT NULL,
                appointmentTime INTEGER NOT NULL,
                notes TEXT NOT NULL,
                isCompleted INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS medication_logs (
                id TEXT PRIMARY KEY NOT NULL,
                medicineId TEXT NOT NULL,
                medicineName TEXT NOT NULL,
                userId TEXT NOT NULL,
                scheduledDate TEXT NOT NULL,
                scheduledTime TEXT NOT NULL,
                actualTakenTime INTEGER,
                isTaken INTEGER NOT NULL,
                isSkipped INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Đảm bảo cột purpose luôn tồn tại trong medicines
        try {
            db.execSQL("ALTER TABLE medicines ADD COLUMN purpose TEXT NOT NULL DEFAULT ''")
        } catch (_: Exception) {
            // Cột đã tồn tại
        }

        // Đảm bảo các cột mới luôn tồn tại trong alerts
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN memberId TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN memberName TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN alertType TEXT NOT NULL DEFAULT 'THRESHOLD_EXCEEDED'") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN alertLevel TEXT NOT NULL DEFAULT 'WARNING'") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN address TEXT") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN isResolved INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}

        // Dọn dẹp sạch sẽ toàn bộ các bản ghi dữ liệu mẫu giả lập cũ
        try {
            db.execSQL("DELETE FROM alerts WHERE memberName LIKE '%Nguyễn Văn Nam%' OR targetUserName LIKE '%Nguyễn Văn Nam%' OR memberName LIKE '%Nguyễn Hoàng%' OR targetUserName LIKE '%Nguyễn Hoàng%' OR message LIKE '%Nguyễn Hoàng%' OR memberName LIKE '%Trần Thị Lan%' OR targetUserName LIKE '%Trần Thị Lan%' OR message LIKE '%Trần Thị Lan%' OR memberId = 'user_demo_01' OR targetUserId = 'user_demo_01' OR familyId = 'fam_123456' OR message LIKE '%10.7769%'")
            db.execSQL("UPDATE alerts SET message = 'Đã phát cảnh báo khẩn cấp tới toàn bộ người thân.', address = NULL WHERE message LIKE '%Vui lòng bật Định vị%' OR message LIKE '%Chưa nhận diện được vị trí%'")
            db.execSQL("UPDATE alerts SET alertLevel = 'CRITICAL', level = 'CRITICAL' WHERE alertType = 'SOS_EMERGENCY' OR type = 'SOS_EMERGENCY' OR title LIKE '%SOS%' OR title LIKE '%CẤP CỨU%'")
        } catch (_: Exception) {}
        try {
            db.execSQL("DELETE FROM users WHERE fullName LIKE '%Nguyễn Văn Nam%' OR id = 'member_bac_nam' OR id = 'user_demo_01' OR id = 'user_01' OR fullName = 'Bác Trần Thị Lan' OR fullName = 'Con Nguyễn Hoàng'")
        } catch (_: Exception) {}

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_medication_logs_medicineId_scheduledDate_scheduledTime
            ON medication_logs (medicineId, scheduledDate, scheduledTime)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_medication_logs_userId_scheduledDate
            ON medication_logs (userId, scheduledDate)
            """.trimIndent()
        )
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL DEFAULT '',
                fullName TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'MEMBER',
                familyId TEXT NOT NULL DEFAULT '',
                familyName TEXT NOT NULL DEFAULT '',
                inviteCode TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL DEFAULT 0,
                avatarUrl TEXT NOT NULL DEFAULT '',
                birthDate TEXT NOT NULL DEFAULT '',
                bloodType TEXT NOT NULL DEFAULT '',
                chronicDiseasesJson TEXT NOT NULL DEFAULT '[]',
                drugAllergiesJson TEXT NOT NULL DEFAULT '[]'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS appointments (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                hospitalName TEXT NOT NULL,
                department TEXT NOT NULL,
                doctorName TEXT NOT NULL,
                appointmentTime INTEGER NOT NULL,
                notes TEXT NOT NULL,
                isCompleted INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS medication_logs (
                id TEXT PRIMARY KEY NOT NULL,
                medicineId TEXT NOT NULL,
                medicineName TEXT NOT NULL,
                userId TEXT NOT NULL,
                scheduledDate TEXT NOT NULL,
                scheduledTime TEXT NOT NULL,
                actualTakenTime INTEGER,
                isTaken INTEGER NOT NULL,
                isSkipped INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Đảm bảo cột purpose luôn tồn tại trong medicines
        try {
            db.execSQL("ALTER TABLE medicines ADD COLUMN purpose TEXT NOT NULL DEFAULT ''")
        } catch (_: Exception) {
            // Cột đã tồn tại
        }

        // Đảm bảo các cột mới luôn tồn tại trong alerts
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN memberId TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN memberName TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN alertType TEXT NOT NULL DEFAULT 'THRESHOLD_EXCEEDED'") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN alertLevel TEXT NOT NULL DEFAULT 'WARNING'") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN address TEXT") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE alerts ADD COLUMN isResolved INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}

        // Dọn dẹp sạch sẽ toàn bộ các bản ghi dữ liệu mẫu giả lập cũ
        try {
            db.execSQL("DELETE FROM alerts WHERE memberName LIKE '%Nguyễn Văn Nam%' OR targetUserName LIKE '%Nguyễn Văn Nam%' OR memberName LIKE '%Nguyễn Hoàng%' OR targetUserName LIKE '%Nguyễn Hoàng%' OR message LIKE '%Nguyễn Hoàng%' OR memberName LIKE '%Trần Thị Lan%' OR targetUserName LIKE '%Trần Thị Lan%' OR message LIKE '%Trần Thị Lan%' OR memberId = 'user_demo_01' OR targetUserId = 'user_demo_01' OR familyId = 'fam_123456' OR message LIKE '%10.7769%'")
            db.execSQL("UPDATE alerts SET message = 'Đã phát cảnh báo khẩn cấp tới toàn bộ người thân.', address = NULL WHERE message LIKE '%Vui lòng bật Định vị%' OR message LIKE '%Chưa nhận diện được vị trí%'")
            db.execSQL("UPDATE alerts SET alertLevel = 'CRITICAL', level = 'CRITICAL' WHERE alertType = 'SOS_EMERGENCY' OR type = 'SOS_EMERGENCY' OR title LIKE '%SOS%' OR title LIKE '%CẤP CỨU%'")
        } catch (_: Exception) {}
        try {
            db.execSQL("DELETE FROM users WHERE fullName LIKE '%Nguyễn Văn Nam%' OR id = 'member_bac_nam' OR id = 'user_demo_01' OR id = 'user_01' OR fullName = 'Bác Trần Thị Lan' OR fullName = 'Con Nguyễn Hoàng'")
        } catch (_: Exception) {}
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS health_metrics")
        db.execSQL("DROP TABLE IF EXISTS medicines")
        db.execSQL("DROP TABLE IF EXISTS alerts")
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS appointments")
        db.execSQL("DROP TABLE IF EXISTS medication_logs")
        onCreate(db)
    }
}

class HealthMetricDaoImpl(private val dbHelper: DatabaseHelper) : HealthMetricDao {
    private val _signal = MutableStateFlow(System.currentTimeMillis())

    override fun getMetricsByMember(userId: String): Flow<List<HealthMetricEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                queryMetrics(userId, null, null)
            }
        }
    }

    override fun getMetricsByType(userId: String, type: String): Flow<List<HealthMetricEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                queryMetrics(userId, type, null)
            }
        }
    }

    override fun getRecentMetrics(userId: String, limit: Int): Flow<List<HealthMetricEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                queryMetrics(userId, null, limit)
            }
        }
    }

    override suspend fun getLatestMetricByType(userId: String, type: String): HealthMetricEntity? = withContext(Dispatchers.IO) {
        val list = queryMetrics(userId, type, 1)
        list.firstOrNull()
    }

    override suspend fun insertMetrics(metrics: List<HealthMetricEntity>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (metric in metrics) {
                val values = ContentValues().apply {
                    put("id", metric.id)
                    put("userId", metric.userId)
                    put("type", metric.type)
                    put("valuePrimary", metric.valuePrimary)
                    metric.valueSecondary?.let { put("valueSecondary", it) }
                    put("unit", metric.unit)
                    put("timestamp", metric.timestamp)
                    put("notes", metric.notes)
                    put("isFromCameraPpg", if (metric.isFromCameraPpg) 1 else 0)
                }
                db.insertWithOnConflict("health_metrics", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _signal.value = System.currentTimeMillis()
    }

    override suspend fun insertMetric(metric: HealthMetricEntity) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", metric.id)
            put("userId", metric.userId)
            put("type", metric.type)
            put("valuePrimary", metric.valuePrimary)
            metric.valueSecondary?.let { put("valueSecondary", it) }
            put("unit", metric.unit)
            put("timestamp", metric.timestamp)
            put("notes", metric.notes)
            put("isFromCameraPpg", if (metric.isFromCameraPpg) 1 else 0)
        }
        db.insertWithOnConflict("health_metrics", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _signal.value = System.currentTimeMillis()
    }

    override suspend fun deleteMetric(metricId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("health_metrics", "id = ?", arrayOf(metricId))
        _signal.value = System.currentTimeMillis()
    }

    suspend fun getRecentMetricsDirect(userId: String, limit: Int): List<HealthMetricEntity> = withContext(Dispatchers.IO) {
        queryMetrics(userId, null, limit)
    }

    suspend fun getMetricsByTypeAndDateRange(
        userId: String,
        type: String,
        startTime: Long,
        endTime: Long
    ): List<HealthMetricEntity> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "health_metrics",
            null,
            "userId = ? AND type = ? AND timestamp >= ? AND timestamp <= ?",
            arrayOf(userId, type, startTime.toString(), endTime.toString()),
            null,
            null,
            "timestamp DESC"
        )
        val list = mutableListOf<HealthMetricEntity>()
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow("id")
            val userCol = c.getColumnIndexOrThrow("userId")
            val typeCol = c.getColumnIndexOrThrow("type")
            val val1Col = c.getColumnIndexOrThrow("valuePrimary")
            val val2Col = c.getColumnIndexOrThrow("valueSecondary")
            val unitCol = c.getColumnIndexOrThrow("unit")
            val timeCol = c.getColumnIndexOrThrow("timestamp")
            val noteCol = c.getColumnIndexOrThrow("notes")
            val ppgCol = c.getColumnIndexOrThrow("isFromCameraPpg")

            while (c.moveToNext()) {
                list.add(
                    HealthMetricEntity(
                        id = c.getString(idCol),
                        userId = c.getString(userCol),
                        type = c.getString(typeCol),
                        valuePrimary = c.getDouble(val1Col),
                        valueSecondary = if (!c.isNull(val2Col)) c.getDouble(val2Col) else null,
                        unit = c.getString(unitCol),
                        timestamp = c.getLong(timeCol),
                        notes = c.getString(noteCol),
                        isFromCameraPpg = c.getInt(ppgCol) == 1
                    )
                )
            }
        }
        list
    }

    private fun queryMetrics(userId: String, type: String?, limit: Int?): List<HealthMetricEntity> {
        val db = dbHelper.readableDatabase
        val selection = if (type == null) "userId = ?" else "userId = ? AND type = ?"
        val selectionArgs = if (type == null) arrayOf(userId) else arrayOf(userId, type)
        val limitStr = limit?.toString()

        val cursor = db.query("health_metrics", null, selection, selectionArgs, null, null, "timestamp DESC", limitStr)
        val list = mutableListOf<HealthMetricEntity>()
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow("id")
            val userCol = c.getColumnIndexOrThrow("userId")
            val typeCol = c.getColumnIndexOrThrow("type")
            val val1Col = c.getColumnIndexOrThrow("valuePrimary")
            val val2Col = c.getColumnIndexOrThrow("valueSecondary")
            val unitCol = c.getColumnIndexOrThrow("unit")
            val timeCol = c.getColumnIndexOrThrow("timestamp")
            val noteCol = c.getColumnIndexOrThrow("notes")
            val ppgCol = c.getColumnIndexOrThrow("isFromCameraPpg")

            while (c.moveToNext()) {
                list.add(
                    HealthMetricEntity(
                        id = c.getString(idCol),
                        userId = c.getString(userCol),
                        type = c.getString(typeCol),
                        valuePrimary = c.getDouble(val1Col),
                        valueSecondary = if (!c.isNull(val2Col)) c.getDouble(val2Col) else null,
                        unit = c.getString(unitCol),
                        timestamp = c.getLong(timeCol),
                        notes = c.getString(noteCol),
                        isFromCameraPpg = c.getInt(ppgCol) == 1
                    )
                )
            }
        }
        return list
    }
}

class MedicineDaoImpl(private val dbHelper: DatabaseHelper) : MedicineDao {
    private val gson = Gson()
    private val _signal = MutableStateFlow(System.currentTimeMillis())

    override fun getMedicinesByMember(userId: String): Flow<List<MedicineEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase
                val cursor = db.query("medicines", null, "userId = ?", arrayOf(userId), null, null, "startDate DESC")
                val list = mutableListOf<MedicineEntity>()
                cursor.use { c ->
                    val idCol = c.getColumnIndexOrThrow("id")
                    val userCol = c.getColumnIndexOrThrow("userId")
                    val nameCol = c.getColumnIndexOrThrow("name")
                    val dosCol = c.getColumnIndexOrThrow("dosage")
                    val insCol = c.getColumnIndexOrThrow("instruction")
                    val remCol = c.getColumnIndexOrThrow("reminderTimes")
                    val sCol = c.getColumnIndexOrThrow("startDate")
                    val eCol = c.getColumnIndexOrThrow("endDate")
                    val ocrCol = c.getColumnIndexOrThrow("scannedViaOcr")
                    val purCol = c.getColumnIndex("purpose")

                    while (c.moveToNext()) {
                        val timesType = object : TypeToken<List<String>>() {}.type
                        val times: List<String> = runCatching {
                            gson.fromJson<List<String>>(c.getString(remCol), timesType)
                        }.getOrDefault(emptyList())

                        val purpose = if (purCol != -1 && !c.isNull(purCol)) c.getString(purCol) else ""

                        list.add(
                            MedicineEntity(
                                id = c.getString(idCol),
                                userId = c.getString(userCol),
                                name = c.getString(nameCol),
                                dosage = c.getString(dosCol),
                                instruction = c.getString(insCol),
                                reminderTimes = times,
                                startDate = c.getLong(sCol),
                                endDate = if (!c.isNull(eCol)) c.getLong(eCol) else null,
                                scannedViaOcr = c.getInt(ocrCol) == 1,
                                purpose = purpose
                            )
                        )
                    }
                }
                list
            }
        }
    }

    override suspend fun insertMedicine(medicine: MedicineEntity) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", medicine.id)
            put("userId", medicine.userId)
            put("name", medicine.name)
            put("dosage", medicine.dosage)
            put("instruction", medicine.instruction)
            put("reminderTimes", gson.toJson(medicine.reminderTimes))
            put("startDate", medicine.startDate)
            medicine.endDate?.let { put("endDate", it) }
            put("scannedViaOcr", if (medicine.scannedViaOcr) 1 else 0)
            put("purpose", medicine.purpose)
        }
        db.insertWithOnConflict("medicines", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _signal.value = System.currentTimeMillis()
    }

    override suspend fun updateMedicine(medicine: MedicineEntity) = insertMedicine(medicine)

    override suspend fun deleteMedicine(medicineId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("medicines", "id = ?", arrayOf(medicineId))
        _signal.value = System.currentTimeMillis()
    }
}

class AlertDaoImpl(private val dbHelper: DatabaseHelper) : AlertDao {
    private val _signal = MutableStateFlow(System.currentTimeMillis())

    override fun getAlertsByFamily(familyId: String): Flow<List<AlertEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase
                val cursor = db.query("alerts", null, "familyId = ?", arrayOf(familyId), null, null, "timestamp DESC")
                val list = mutableListOf<AlertEntity>()
                cursor.use { c ->
                    while (c.moveToNext()) {
                        list.add(cursorToAlert(c))
                    }
                }
                list
            }
        }
    }

    override fun getAllAlerts(): Flow<List<AlertEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                try {
                    val db = dbHelper.readableDatabase
                    val cursor = db.query("alerts", null, null, null, null, null, "timestamp DESC")
                    val list = mutableListOf<AlertEntity>()
                    cursor.use { c ->
                        while (c.moveToNext()) {
                            list.add(cursorToAlert(c))
                        }
                    }
                    list
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Lỗi truy vấn getAllAlerts", e)
                    emptyList()
                }
            }
        }
    }

    override suspend fun getRecentAlertsByType(
        userId: String,
        type: String,
        sinceTimestamp: Long
    ): List<AlertEntity> = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                "alerts",
                null,
                "(memberId = ? OR targetUserId = ?) AND (alertType = ? OR type = ?) AND timestamp >= ?",
                arrayOf(userId, userId, type, type, sinceTimestamp.toString()),
                null,
                null,
                "timestamp DESC"
            )
            val list = mutableListOf<AlertEntity>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(cursorToAlert(c))
                }
            }
            list
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi truy vấn getRecentAlertsByType", e)
            emptyList()
        }
    }

    override suspend fun getLatestAlertForMember(
        memberId: String,
        alertType: com.familycare.app.domain.model.AlertType,
        alertTypeStr: String
    ): AlertEntity? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                "alerts",
                null,
                "(memberId = ? OR targetUserId = ?) AND (alertType = ? OR type = ?)",
                arrayOf(memberId, memberId, alertType.name, alertType.name),
                null,
                null,
                "timestamp DESC",
                "1"
            )
            cursor.use { c ->
                if (c.moveToNext()) cursorToAlert(c) else null
            }
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi truy vấn getLatestAlertForMember", e)
            null
        }
    }

    override suspend fun getAlertById(alertId: Long): AlertEntity? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.query("alerts", null, "id = ?", arrayOf(alertId.toString()), null, null, null, "1")
            cursor.use { c ->
                if (c.moveToNext()) cursorToAlert(c) else null
            }
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi truy vấn getAlertById", e)
            null
        }
    }

    fun getUnreadAlerts(familyId: String): Flow<List<AlertEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                try {
                    val db = dbHelper.readableDatabase
                    val cursor = db.query("alerts", null, "familyId = ? AND isRead = 0", arrayOf(familyId), null, null, "timestamp DESC")
                    val list = mutableListOf<AlertEntity>()
                    cursor.use { c ->
                        while (c.moveToNext()) {
                            list.add(cursorToAlert(c))
                        }
                    }
                    list
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Lỗi truy vấn getUnreadAlerts", e)
                    emptyList()
                }
            }
        }
    }

    override suspend fun insertAlert(alert: AlertEntity): Long = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                if (alert.id > 0) {
                    put("id", alert.id)
                }
                put("memberId", alert.memberId)
                put("memberName", alert.memberName)
                put("targetUserId", alert.memberId)
                put("targetUserName", alert.memberName)
                put("alertType", alert.alertType.name)
                put("type", alert.alertType.name)
                put("alertLevel", alert.alertLevel.name)
                put("level", alert.alertLevel.name)
                put("title", alert.title)
                put("message", alert.message)
                put("address", alert.address)
                put("timestamp", alert.timestamp)
                put("isRead", if (alert.isRead) 1 else 0)
                put("isResolved", if (alert.isResolved) 1 else 0)
                put("familyId", alert.familyId)
                alert.latitude?.let { put("latitude", it) }
                alert.longitude?.let { put("longitude", it) }
            }
            val insertedId = db.insertWithOnConflict("alerts", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            _signal.value = System.currentTimeMillis()
            insertedId
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi insertAlert", e)
            -1L
        }
    }

    override suspend fun updateReadStatus(alertId: Long, isRead: Boolean): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("isRead", if (isRead) 1 else 0)
            }
            db.update("alerts", values, "id = ?", arrayOf(alertId.toString()))
            _signal.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi updateReadStatus", e)
        }
        Unit
    }

    override suspend fun updateResolvedStatus(alertId: Long, isResolved: Boolean): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("isResolved", if (isResolved) 1 else 0)
            }
            db.update("alerts", values, "id = ?", arrayOf(alertId.toString()))
            _signal.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi updateResolvedStatus", e)
        }
        Unit
    }

    override suspend fun deleteAlert(alertId: Long): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.delete("alerts", "id = ?", arrayOf(alertId.toString()))
            _signal.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi deleteAlert", e)
        }
        Unit
    }

    override suspend fun deleteDummyAlerts(): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.delete(
                "alerts",
                "memberName LIKE '%Nguyễn Văn Nam%' OR targetUserName LIKE '%Nguyễn Văn Nam%' OR memberName LIKE '%Nguyễn Hoàng%' OR targetUserName LIKE '%Nguyễn Hoàng%' OR message LIKE '%Nguyễn Hoàng%' OR memberName LIKE '%Trần Thị Lan%' OR targetUserName LIKE '%Trần Thị Lan%' OR message LIKE '%Trần Thị Lan%' OR memberId = 'user_demo_01' OR targetUserId = 'user_demo_01' OR familyId = 'fam_123456' OR message LIKE '%10.7769%'",
                null
            )
            val cv = ContentValues().apply {
                put("message", "Đã phát cảnh báo khẩn cấp tới toàn bộ người thân.")
                putNull("address")
            }
            db.update("alerts", cv, "message LIKE '%Vui lòng bật Định vị%' OR message LIKE '%Chưa nhận diện được vị trí%'", null)
            val cvCritical = ContentValues().apply {
                put("alertLevel", "CRITICAL")
                put("level", "CRITICAL")
            }
            db.update("alerts", cvCritical, "alertType = 'SOS_EMERGENCY' OR type = 'SOS_EMERGENCY' OR title LIKE '%SOS%' OR title LIKE '%CẤP CỨU%'", null)
            _signal.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi deleteDummyAlerts", e)
        }
        Unit
    }

    override suspend fun clearAllAlerts(): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.delete("alerts", null, null)
            _signal.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi clearAllAlerts", e)
        }
        Unit
    }

    override suspend fun cleanGpsReminderFromAlerts(): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("message", "Đã phát cảnh báo khẩn cấp tới toàn bộ người thân.")
                putNull("address")
            }
            db.update("alerts", cv, "message LIKE '%Vui lòng bật Định vị%' OR message LIKE '%Chưa nhận diện được vị trí%'", null)
            val cvCritical = ContentValues().apply {
                put("alertLevel", "CRITICAL")
                put("level", "CRITICAL")
            }
            db.update("alerts", cvCritical, "alertType = 'SOS_EMERGENCY' OR type = 'SOS_EMERGENCY' OR title LIKE '%SOS%' OR title LIKE '%CẤP CỨU%'", null)
            _signal.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi cleanGpsReminderFromAlerts", e)
        }
        Unit
    }

    suspend fun markAllAsRead(familyId: String) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("isRead", 1)
            }
            db.update("alerts", values, "familyId = ?", arrayOf(familyId))
            _signal.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi markAllAsRead", e)
        }
    }

    suspend fun getRecentAlertsDirect(familyId: String, limit: Int): List<AlertEntity> = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.query("alerts", null, "familyId = ?", arrayOf(familyId), null, null, "timestamp DESC", limit.toString())
            val list = mutableListOf<AlertEntity>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(cursorToAlert(c))
                }
            }
            list
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi getRecentAlertsDirect", e)
            emptyList()
        }
    }

    private fun cursorToAlert(c: Cursor): AlertEntity {
        return try {
            val idCol = c.getColumnIndex("id")
            val famCol = c.getColumnIndex("familyId")
            val userCol = c.getColumnIndex("memberId").takeIf { it != -1 } ?: c.getColumnIndex("targetUserId")
            val nameCol = c.getColumnIndex("memberName").takeIf { it != -1 } ?: c.getColumnIndex("targetUserName")
            val typeCol = c.getColumnIndex("alertType").takeIf { it != -1 } ?: c.getColumnIndex("type")
            val lvlCol = c.getColumnIndex("alertLevel").takeIf { it != -1 } ?: c.getColumnIndex("level")
            val titleCol = c.getColumnIndex("title")
            val msgCol = c.getColumnIndex("message")
            val addressCol = c.getColumnIndex("address")
            val timeCol = c.getColumnIndex("timestamp")
            val readCol = c.getColumnIndex("isRead")
            val resolvedCol = c.getColumnIndex("isResolved")
            val latCol = c.getColumnIndex("latitude")
            val lngCol = c.getColumnIndex("longitude")

            val rawIdStr = if (idCol != -1 && !c.isNull(idCol)) {
                try { c.getString(idCol) } catch (_: Exception) { "" }
            } else ""
            val idVal = rawIdStr.toLongOrNull() ?: kotlin.math.abs(rawIdStr.hashCode().toLong()).coerceAtLeast(1L)

            val memberIdVal = if (userCol != -1 && !c.isNull(userCol)) c.getString(userCol) else ""
            val memberNameVal = if (nameCol != -1 && !c.isNull(nameCol)) c.getString(nameCol) else ""
            val typeStr = if (typeCol != -1 && !c.isNull(typeCol)) c.getString(typeCol) else ""
            val lvlStr = if (lvlCol != -1 && !c.isNull(lvlCol)) c.getString(lvlCol) else ""
            val titleVal = if (titleCol != -1 && !c.isNull(titleCol)) c.getString(titleCol) else ""
            val msgVal = if (msgCol != -1 && !c.isNull(msgCol)) c.getString(msgCol) else ""
            val addressVal = if (addressCol != -1 && !c.isNull(addressCol)) c.getString(addressCol) else null
            val timeVal = if (timeCol != -1 && !c.isNull(timeCol)) c.getLong(timeCol) else System.currentTimeMillis()
            val isReadVal = if (readCol != -1 && !c.isNull(readCol)) c.getInt(readCol) == 1 else false
            val isResolvedVal = if (resolvedCol != -1 && !c.isNull(resolvedCol)) c.getInt(resolvedCol) == 1 else false
            val latVal = if (latCol != -1 && !c.isNull(latCol)) c.getDouble(latCol) else null
            val lngVal = if (lngCol != -1 && !c.isNull(lngCol)) c.getDouble(lngCol) else null
            val famVal = if (famCol != -1 && !c.isNull(famCol)) c.getString(famCol) else ""

            val alertTypeEnum = runCatching { com.familycare.app.domain.model.AlertType.valueOf(typeStr) }
                .getOrDefault(
                    if (typeStr.contains("SOS", ignoreCase = true) || titleVal.contains("SOS", ignoreCase = true) || titleVal.contains("CẤP CỨU", ignoreCase = true)) {
                        com.familycare.app.domain.model.AlertType.SOS_EMERGENCY
                    } else {
                        com.familycare.app.domain.model.AlertType.THRESHOLD_EXCEEDED
                    }
                )

            // Khóa cứng: Bất kỳ cảnh báo SOS hoặc có còi cứu hộ khẩn cấp đều 100% là CRITICAL (Khẩn cấp)
            val alertLevelEnum = when {
                alertTypeEnum == com.familycare.app.domain.model.AlertType.SOS_EMERGENCY -> com.familycare.app.domain.model.AlertLevel.CRITICAL
                typeStr.contains("SOS", ignoreCase = true) || titleVal.contains("SOS", ignoreCase = true) || titleVal.contains("CẤP CỨU", ignoreCase = true) -> com.familycare.app.domain.model.AlertLevel.CRITICAL
                titleVal.contains("CẢNH BÁO ĐỎ", ignoreCase = true) -> com.familycare.app.domain.model.AlertLevel.CRITICAL
                lvlStr.equals("CRITICAL", ignoreCase = true) -> com.familycare.app.domain.model.AlertLevel.CRITICAL
                lvlStr.equals("INFO", ignoreCase = true) -> com.familycare.app.domain.model.AlertLevel.INFO
                else -> com.familycare.app.domain.model.AlertLevel.WARNING
            }

            AlertEntity(
                id = idVal,
                memberId = memberIdVal,
                memberName = memberNameVal,
                alertType = alertTypeEnum,
                alertLevel = alertLevelEnum,
                title = titleVal,
                message = msgVal,
                address = addressVal,
                latitude = latVal,
                longitude = lngVal,
                timestamp = timeVal,
                isRead = isReadVal,
                isResolved = isResolvedVal,
                familyId = famVal
            )
        } catch (e: Exception) {
            Log.e("AppDatabase", "Lỗi phân giải bản ghi AlertEntity từ Cursor", e)
            AlertEntity(
                id = System.currentTimeMillis(),
                memberId = "unknown",
                memberName = "Người thân",
                alertType = com.familycare.app.domain.model.AlertType.THRESHOLD_EXCEEDED,
                alertLevel = com.familycare.app.domain.model.AlertLevel.WARNING,
                title = "Thông báo sức khỏe",
                message = "Cập nhật dữ liệu gia đình",
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

class UserDaoImpl(private val dbHelper: DatabaseHelper) : UserDao {
    private val _signal = MutableStateFlow(System.currentTimeMillis())

    override suspend fun getUserById(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("users", null, "id = ?", arrayOf(userId), null, null, null, "1")
        cursor.use { c ->
            if (c.moveToNext()) cursorToUserEntity(c) else null
        }
    }

    override suspend fun getCurrentUser(): UserEntity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("users", null, null, null, null, null, null, "1")
        cursor.use { c ->
            if (c.moveToNext()) cursorToUserEntity(c) else null
        }
    }

    override suspend fun getAllUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("users", null, null, null, null, null, "createdAt ASC")
        val list = mutableListOf<UserEntity>()
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToUserEntity(c))
            }
        }
        list
    }

    override fun observeCurrentUser(): Flow<UserEntity?> {
        return _signal.map {
            getCurrentUser()
        }
    }

    override suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", user.id)
            put("fullName", user.fullName)
            put("email", user.email)
            put("avatarUrl", user.avatarUrl)
            put("familyId", user.familyId)
            put("role", user.role)
            put("birthDate", user.birthDate)
            put("bloodType", user.bloodType)
            put("chronicDiseasesJson", user.chronicDiseasesJson)
            put("drugAllergiesJson", user.drugAllergiesJson)
        }
        db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _signal.value = System.currentTimeMillis()
    }

    override suspend fun deleteUserById(userId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("users", "id = ?", arrayOf(userId))
        _signal.value = System.currentTimeMillis()
    }

    override suspend fun register(user: UserAccountEntity): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val existing = queryUserByEmail(user.email)
        if (existing != null) return@withContext false

        val values = ContentValues().apply {
            put("id", user.id)
            put("email", user.email.trim().lowercase())
            put("password", user.password)
            put("fullName", user.fullName.trim())
            put("role", user.role)
            user.familyId?.let { put("familyId", it) }
            user.familyName?.let { put("familyName", it) }
            user.inviteCode?.let { put("inviteCode", it) }
            put("createdAt", user.createdAt)
        }
        val res = db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        _signal.value = System.currentTimeMillis()
        res != -1L
    }

    override suspend fun getUserByEmail(email: String): UserAccountEntity? = withContext(Dispatchers.IO) {
        queryUserByEmail(email)
    }

    override suspend fun getUserAccountById(userId: String): UserAccountEntity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("users", null, "id = ?", arrayOf(userId), null, null, null)
        cursor.use { c ->
            if (c.moveToFirst()) parseUserAccount(c) else null
        }
    }

    override suspend fun updateFamily(userId: String, familyId: String, familyName: String, inviteCode: String, role: String?) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("familyId", familyId)
            put("familyName", familyName)
            put("inviteCode", inviteCode)
            role?.let { put("role", it) }
        }
        db.update("users", values, "id = ?", arrayOf(userId))
        _signal.value = System.currentTimeMillis()
        Unit
    }

    private fun queryUserByEmail(email: String): UserAccountEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "users",
            null,
            "LOWER(email) = ?",
            arrayOf(email.trim().lowercase()),
            null,
            null,
            null
        )
        return cursor.use { c ->
            if (c.moveToFirst()) parseUserAccount(c) else null
        }
    }

    private fun parseUserAccount(c: Cursor): UserAccountEntity {
        val idCol = c.getColumnIndexOrThrow("id")
        val emailCol = c.getColumnIndexOrThrow("email")
        val passCol = c.getColumnIndexOrThrow("password")
        val nameCol = c.getColumnIndexOrThrow("fullName")
        val roleCol = c.getColumnIndexOrThrow("role")
        val famIdCol = c.getColumnIndexOrThrow("familyId")
        val famNameCol = c.getColumnIndexOrThrow("familyName")
        val inviteCol = c.getColumnIndexOrThrow("inviteCode")
        val createdCol = c.getColumnIndexOrThrow("createdAt")

        return UserAccountEntity(
            id = c.getString(idCol),
            email = c.getString(emailCol),
            password = if (!c.isNull(passCol)) c.getString(passCol) else "",
            fullName = c.getString(nameCol),
            role = c.getString(roleCol),
            familyId = if (!c.isNull(famIdCol)) c.getString(famIdCol) else null,
            familyName = if (!c.isNull(famNameCol)) c.getString(famNameCol) else null,
            inviteCode = if (!c.isNull(inviteCol)) c.getString(inviteCol) else null,
            createdAt = if (!c.isNull(createdCol)) c.getLong(createdCol) else 0L
        )
    }

    private fun cursorToUserEntity(c: Cursor): UserEntity {
        val idCol = c.getColumnIndexOrThrow("id")
        val nameCol = c.getColumnIndexOrThrow("fullName")
        val emailCol = c.getColumnIndexOrThrow("email")
        val avatarCol = c.getColumnIndexOrThrow("avatarUrl")
        val famCol = c.getColumnIndexOrThrow("familyId")
        val roleCol = c.getColumnIndexOrThrow("role")
        val dobCol = c.getColumnIndexOrThrow("birthDate")
        val bloodCol = c.getColumnIndexOrThrow("bloodType")
        val disCol = c.getColumnIndexOrThrow("chronicDiseasesJson")
        val allCol = c.getColumnIndexOrThrow("drugAllergiesJson")

        return UserEntity(
            id = c.getString(idCol),
            fullName = c.getString(nameCol),
            email = c.getString(emailCol),
            avatarUrl = if (!c.isNull(avatarCol)) c.getString(avatarCol) else "",
            familyId = if (!c.isNull(famCol)) c.getString(famCol) else "",
            role = if (!c.isNull(roleCol)) c.getString(roleCol) else "MEMBER",
            birthDate = if (!c.isNull(dobCol)) c.getString(dobCol) else "",
            bloodType = if (!c.isNull(bloodCol)) c.getString(bloodCol) else "",
            chronicDiseasesJson = if (!c.isNull(disCol)) c.getString(disCol) else "[]",
            drugAllergiesJson = if (!c.isNull(allCol)) c.getString(allCol) else "[]"
        )
    }
}


class AppointmentDaoImpl(private val dbHelper: DatabaseHelper) : AppointmentDao {
    private val _signal = MutableStateFlow(System.currentTimeMillis())

    override fun getAppointmentsByMember(userId: String): Flow<List<AppointmentEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                val list = mutableListOf<AppointmentEntity>()
                val db = dbHelper.readableDatabase
                val cursor = db.rawQuery(
                    "SELECT * FROM appointments WHERE userId = ? ORDER BY appointmentTime ASC",
                    arrayOf(userId)
                )
                cursor.use { c ->
                    val idCol = c.getColumnIndexOrThrow("id")
                    val userCol = c.getColumnIndexOrThrow("userId")
                    val hospCol = c.getColumnIndexOrThrow("hospitalName")
                    val deptCol = c.getColumnIndexOrThrow("department")
                    val docCol = c.getColumnIndexOrThrow("doctorName")
                    val timeCol = c.getColumnIndexOrThrow("appointmentTime")
                    val notesCol = c.getColumnIndexOrThrow("notes")
                    val compCol = c.getColumnIndexOrThrow("isCompleted")

                    while (c.moveToNext()) {
                        list.add(
                            AppointmentEntity(
                                id = c.getString(idCol),
                                userId = c.getString(userCol),
                                hospitalName = c.getString(hospCol),
                                department = c.getString(deptCol),
                                doctorName = c.getString(docCol),
                                appointmentTime = c.getLong(timeCol),
                                notes = c.getString(notesCol),
                                isCompleted = c.getInt(compCol) == 1
                            )
                        )
                    }
                }
                list
            }
        }
    }

    override suspend fun insertAppointment(appointment: AppointmentEntity) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", appointment.id)
            put("userId", appointment.userId)
            put("hospitalName", appointment.hospitalName)
            put("department", appointment.department)
            put("doctorName", appointment.doctorName)
            put("appointmentTime", appointment.appointmentTime)
            put("notes", appointment.notes)
            put("isCompleted", if (appointment.isCompleted) 1 else 0)
        }
        db.insertWithOnConflict("appointments", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _signal.value = System.currentTimeMillis()
    }

    override suspend fun deleteAppointment(appointmentId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("appointments", "id = ?", arrayOf(appointmentId))
        _signal.value = System.currentTimeMillis()
    }
}


class MedicationLogDaoImpl(private val dbHelper: DatabaseHelper) : MedicationLogDao {
    private val _signal = MutableStateFlow(System.currentTimeMillis())

    override fun getLogsForDate(userId: String, date: String): Flow<List<MedicationLogEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    "medication_logs",
                    null,
                    "userId = ? AND scheduledDate = ?",
                    arrayOf(userId, date),
                    null,
                    null,
                    "scheduledTime ASC"
                )
                val list = mutableListOf<MedicationLogEntity>()
                cursor.use { c ->
                    val idCol = c.getColumnIndexOrThrow("id")
                    val medIdCol = c.getColumnIndexOrThrow("medicineId")
                    val nameCol = c.getColumnIndexOrThrow("medicineName")
                    val userCol = c.getColumnIndexOrThrow("userId")
                    val dateCol = c.getColumnIndexOrThrow("scheduledDate")
                    val timeCol = c.getColumnIndexOrThrow("scheduledTime")
                    val actualCol = c.getColumnIndexOrThrow("actualTakenTime")
                    val takenCol = c.getColumnIndexOrThrow("isTaken")
                    val skipCol = c.getColumnIndexOrThrow("isSkipped")

                    while (c.moveToNext()) {
                        list.add(
                            MedicationLogEntity(
                                id = c.getString(idCol),
                                medicineId = c.getString(medIdCol),
                                medicineName = c.getString(nameCol),
                                userId = c.getString(userCol),
                                scheduledDate = c.getString(dateCol),
                                scheduledTime = c.getString(timeCol),
                                actualTakenTime = if (!c.isNull(actualCol)) c.getLong(actualCol) else null,
                                isTaken = c.getInt(takenCol) == 1,
                                isSkipped = c.getInt(skipCol) == 1
                            )
                        )
                    }
                }
                list
            }
        }
    }

    override suspend fun getLog(medicineId: String, date: String, time: String): MedicationLogEntity? {
        return withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                "medication_logs",
                null,
                "medicineId = ? AND scheduledDate = ? AND scheduledTime = ?",
                arrayOf(medicineId, date, time),
                null,
                null,
                null,
                "1"
            )
            cursor.use { c ->
                if (c.moveToFirst()) {
                    val idCol = c.getColumnIndexOrThrow("id")
                    val medIdCol = c.getColumnIndexOrThrow("medicineId")
                    val nameCol = c.getColumnIndexOrThrow("medicineName")
                    val userCol = c.getColumnIndexOrThrow("userId")
                    val dateCol = c.getColumnIndexOrThrow("scheduledDate")
                    val timeCol = c.getColumnIndexOrThrow("scheduledTime")
                    val actualCol = c.getColumnIndexOrThrow("actualTakenTime")
                    val takenCol = c.getColumnIndexOrThrow("isTaken")
                    val skipCol = c.getColumnIndexOrThrow("isSkipped")

                    MedicationLogEntity(
                        id = c.getString(idCol),
                        medicineId = c.getString(medIdCol),
                        medicineName = c.getString(nameCol),
                        userId = c.getString(userCol),
                        scheduledDate = c.getString(dateCol),
                        scheduledTime = c.getString(timeCol),
                        actualTakenTime = if (!c.isNull(actualCol)) c.getLong(actualCol) else null,
                        isTaken = c.getInt(takenCol) == 1,
                        isSkipped = c.getInt(skipCol) == 1
                    )
                } else null
            }
        }
    }

    override suspend fun insertIfAbsent(log: MedicationLogEntity): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", log.id)
            put("medicineId", log.medicineId)
            put("medicineName", log.medicineName)
            put("userId", log.userId)
            put("scheduledDate", log.scheduledDate)
            put("scheduledTime", log.scheduledTime)
            log.actualTakenTime?.let { put("actualTakenTime", it) }
            put("isTaken", if (log.isTaken) 1 else 0)
            put("isSkipped", if (log.isSkipped) 1 else 0)
        }
        val result = db.insertWithOnConflict("medication_logs", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        if (result != -1L) {
            _signal.value = System.currentTimeMillis()
        }
        result
    }

    override suspend fun markAsTaken(logId: String, takenAt: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("isTaken", 1)
            put("isSkipped", 0)
            put("actualTakenTime", takenAt)
        }
        db.update("medication_logs", values, "id = ?", arrayOf(logId))
        _signal.value = System.currentTimeMillis()
    }

    override suspend fun getLogById(logId: String): MedicationLogEntity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "medication_logs",
            null,
            "id = ?",
            arrayOf(logId),
            null,
            null,
            null,
            "1"
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                val idCol = c.getColumnIndexOrThrow("id")
                val medIdCol = c.getColumnIndexOrThrow("medicineId")
                val nameCol = c.getColumnIndexOrThrow("medicineName")
                val userCol = c.getColumnIndexOrThrow("userId")
                val dateCol = c.getColumnIndexOrThrow("scheduledDate")
                val timeCol = c.getColumnIndexOrThrow("scheduledTime")
                val actualCol = c.getColumnIndexOrThrow("actualTakenTime")
                val takenCol = c.getColumnIndexOrThrow("isTaken")
                val skipCol = c.getColumnIndexOrThrow("isSkipped")

                MedicationLogEntity(
                    id = c.getString(idCol),
                    medicineId = c.getString(medIdCol),
                    medicineName = c.getString(nameCol),
                    userId = c.getString(userCol),
                    scheduledDate = c.getString(dateCol),
                    scheduledTime = c.getString(timeCol),
                    actualTakenTime = if (!c.isNull(actualCol)) c.getLong(actualCol) else null,
                    isTaken = c.getInt(takenCol) == 1,
                    isSkipped = c.getInt(skipCol) == 1
                )
            } else null
        }
    }
}
