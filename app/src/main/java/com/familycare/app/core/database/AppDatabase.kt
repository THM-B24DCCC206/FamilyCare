package com.familycare.app.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                scannedViaOcr INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS alerts (
                id TEXT PRIMARY KEY NOT NULL,
                familyId TEXT NOT NULL,
                targetUserId TEXT NOT NULL,
                targetUserName TEXT NOT NULL,
                type TEXT NOT NULL,
                level TEXT NOT NULL,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                isRead INTEGER NOT NULL,
                latitude REAL,
                longitude REAL
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

                    while (c.moveToNext()) {
                        val timesType = object : TypeToken<List<String>>() {}.type
                        val times: List<String> = runCatching {
                            gson.fromJson<List<String>>(c.getString(remCol), timesType)
                        }.getOrDefault(emptyList())

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
                                scannedViaOcr = c.getInt(ocrCol) == 1
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

    override suspend fun getRecentAlertsByType(
        userId: String,
        type: String,
        sinceTimestamp: Long
    ): List<AlertEntity> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "alerts",
            null,
            "targetUserId = ? AND type = ? AND timestamp >= ?",
            arrayOf(userId, type, sinceTimestamp.toString()),
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
    }

    override suspend fun getAlertById(alertId: String): AlertEntity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("alerts", null, "id = ?", arrayOf(alertId), null, null, null, "1")
        cursor.use { c ->
            if (c.moveToNext()) cursorToAlert(c) else null
        }
    }

    fun getUnreadAlerts(familyId: String): Flow<List<AlertEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase
                val cursor = db.query("alerts", null, "familyId = ? AND isRead = 0", arrayOf(familyId), null, null, "timestamp DESC")
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

    override suspend fun insertAlert(alert: AlertEntity) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", alert.id)
            put("familyId", alert.familyId)
            put("targetUserId", alert.targetUserId)
            put("targetUserName", alert.targetUserName)
            put("type", alert.type)
            put("level", alert.level)
            put("title", alert.title)
            put("message", alert.message)
            put("timestamp", alert.timestamp)
            put("isRead", if (alert.isRead) 1 else 0)
            alert.latitude?.let { put("latitude", it) }
            alert.longitude?.let { put("longitude", it) }
        }
        db.insertWithOnConflict("alerts", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _signal.value = System.currentTimeMillis()
    }

    override suspend fun markAsRead(alertId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("isRead", 1)
        }
        db.update("alerts", values, "id = ?", arrayOf(alertId))
        _signal.value = System.currentTimeMillis()
    }

    suspend fun markAllAsRead(familyId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("isRead", 1)
        }
        db.update("alerts", values, "familyId = ?", arrayOf(familyId))
        _signal.value = System.currentTimeMillis()
    }

    override suspend fun deleteAlert(alertId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("alerts", "id = ?", arrayOf(alertId))
        _signal.value = System.currentTimeMillis()
    }

    suspend fun getRecentAlertsDirect(familyId: String, limit: Int): List<AlertEntity> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("alerts", null, "familyId = ?", arrayOf(familyId), null, null, "timestamp DESC", limit.toString())
        val list = mutableListOf<AlertEntity>()
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToAlert(c))
            }
        }
        list
    }

    private fun cursorToAlert(c: Cursor): AlertEntity {
        val idCol = c.getColumnIndexOrThrow("id")
        val famCol = c.getColumnIndexOrThrow("familyId")
        val userCol = c.getColumnIndexOrThrow("targetUserId")
        val nameCol = c.getColumnIndexOrThrow("targetUserName")
        val typeCol = c.getColumnIndexOrThrow("type")
        val lvlCol = c.getColumnIndexOrThrow("level")
        val titleCol = c.getColumnIndexOrThrow("title")
        val msgCol = c.getColumnIndexOrThrow("message")
        val timeCol = c.getColumnIndexOrThrow("timestamp")
        val readCol = c.getColumnIndexOrThrow("isRead")
        val latCol = c.getColumnIndexOrThrow("latitude")
        val lngCol = c.getColumnIndexOrThrow("longitude")

        return AlertEntity(
            id = c.getString(idCol),
            familyId = c.getString(famCol),
            targetUserId = c.getString(userCol),
            targetUserName = c.getString(nameCol),
            type = c.getString(typeCol),
            level = c.getString(lvlCol),
            title = c.getString(titleCol),
            message = c.getString(msgCol),
            timestamp = c.getLong(timeCol),
            isRead = c.getInt(readCol) == 1,
            latitude = if (!c.isNull(latCol)) c.getDouble(latCol) else null,
            longitude = if (!c.isNull(lngCol)) c.getDouble(lngCol) else null
        )
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
