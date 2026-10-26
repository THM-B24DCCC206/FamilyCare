package com.familycare.app.core.database

import android.content.ContentValues
import android.content.Context
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
    val medicineDao: MedicineDao = MedicineDaoImpl(dbHelper)
    val alertDao: AlertDao = AlertDaoImpl(dbHelper)
    val userDao: UserDao = UserDaoImpl(dbHelper)

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

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "familycare_db.db", null, 1) {
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
                password TEXT NOT NULL,
                fullName TEXT NOT NULL,
                role TEXT NOT NULL,
                familyId TEXT,
                familyName TEXT,
                inviteCode TEXT,
                createdAt INTEGER NOT NULL
            )
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
                password TEXT NOT NULL,
                fullName TEXT NOT NULL,
                role TEXT NOT NULL,
                familyId TEXT,
                familyName TEXT,
                inviteCode TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS health_metrics")
        db.execSQL("DROP TABLE IF EXISTS medicines")
        db.execSQL("DROP TABLE IF EXISTS alerts")
        onCreate(db)
    }
}

class HealthMetricDaoImpl(private val dbHelper: DatabaseHelper) : HealthMetricDao {
    private val _signal = MutableStateFlow(System.currentTimeMillis())

    override fun getMetricsByMember(userId: String): Flow<List<HealthMetricEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                queryMetrics(userId, null)
            }
        }
    }

    override fun getMetricsByType(userId: String, type: String): Flow<List<HealthMetricEntity>> {
        return _signal.map {
            withContext(Dispatchers.IO) {
                queryMetrics(userId, type)
            }
        }
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

    private fun queryMetrics(userId: String, type: String?): List<HealthMetricEntity> {
        val db = dbHelper.readableDatabase
        val selection = if (type == null) "userId = ?" else "userId = ? AND type = ?"
        val selectionArgs = if (type == null) arrayOf(userId) else arrayOf(userId, type)

        val cursor = db.query(
            "health_metrics",
            null,
            selection,
            selectionArgs,
            null,
            null,
            "timestamp DESC"
        )
        val list = mutableListOf<HealthMetricEntity>()
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow("id")
            val userCol = c.getColumnIndexOrThrow("userId")
            val typeCol = c.getColumnIndexOrThrow("type")
            val v1Col = c.getColumnIndexOrThrow("valuePrimary")
            val v2Col = c.getColumnIndexOrThrow("valueSecondary")
            val unitCol = c.getColumnIndexOrThrow("unit")
            val timeCol = c.getColumnIndexOrThrow("timestamp")
            val notesCol = c.getColumnIndexOrThrow("notes")
            val ppgCol = c.getColumnIndexOrThrow("isFromCameraPpg")

            while (c.moveToNext()) {
                list.add(
                    HealthMetricEntity(
                        id = c.getString(idCol),
                        userId = c.getString(userCol),
                        type = c.getString(typeCol),
                        valuePrimary = c.getDouble(v1Col),
                        valueSecondary = if (!c.isNull(v2Col)) c.getDouble(v2Col) else null,
                        unit = c.getString(unitCol),
                        timestamp = c.getLong(timeCol),
                        notes = c.getString(notesCol),
                        isFromCameraPpg = c.getInt(ppgCol) == 1
                    )
                )
            }
        }
        return list
    }
}

class MedicineDaoImpl(private val dbHelper: DatabaseHelper) : MedicineDao {
    private val _signal = MutableStateFlow(System.currentTimeMillis())
    private val gson = Gson()

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
                    val dosageCol = c.getColumnIndexOrThrow("dosage")
                    val instrCol = c.getColumnIndexOrThrow("instruction")
                    val timesCol = c.getColumnIndexOrThrow("reminderTimes")
                    val startCol = c.getColumnIndexOrThrow("startDate")
                    val endCol = c.getColumnIndexOrThrow("endDate")
                    val ocrCol = c.getColumnIndexOrThrow("scannedViaOcr")

                    while (c.moveToNext()) {
                        val timesJson = c.getString(timesCol)
                        val timesList: List<String> = runCatching {
                            val type = object : TypeToken<List<String>>() {}.type
                            gson.fromJson<List<String>>(timesJson, type)
                        }.getOrDefault(emptyList())

                        list.add(
                            MedicineEntity(
                                id = c.getString(idCol),
                                userId = c.getString(userCol),
                                name = c.getString(nameCol),
                                dosage = c.getString(dosageCol),
                                instruction = c.getString(instrCol),
                                reminderTimes = timesList,
                                startDate = c.getLong(startCol),
                                endDate = if (!c.isNull(endCol)) c.getLong(endCol) else null,
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
                    val idCol = c.getColumnIndexOrThrow("id")
                    val famCol = c.getColumnIndexOrThrow("familyId")
                    val userCol = c.getColumnIndexOrThrow("targetUserId")
                    val nameCol = c.getColumnIndexOrThrow("targetUserName")
                    val typeCol = c.getColumnIndexOrThrow("type")
                    val levelCol = c.getColumnIndexOrThrow("level")
                    val titleCol = c.getColumnIndexOrThrow("title")
                    val msgCol = c.getColumnIndexOrThrow("message")
                    val timeCol = c.getColumnIndexOrThrow("timestamp")
                    val readCol = c.getColumnIndexOrThrow("isRead")
                    val latCol = c.getColumnIndexOrThrow("latitude")
                    val lngCol = c.getColumnIndexOrThrow("longitude")

                    while (c.moveToNext()) {
                        list.add(
                            AlertEntity(
                                id = c.getString(idCol),
                                familyId = c.getString(famCol),
                                targetUserId = c.getString(userCol),
                                targetUserName = c.getString(nameCol),
                                type = c.getString(typeCol),
                                level = c.getString(levelCol),
                                title = c.getString(titleCol),
                                message = c.getString(msgCol),
                                timestamp = c.getLong(timeCol),
                                isRead = c.getInt(readCol) == 1,
                                latitude = if (!c.isNull(latCol)) c.getDouble(latCol) else null,
                                longitude = if (!c.isNull(lngCol)) c.getDouble(lngCol) else null
                            )
                        )
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
        val values = ContentValues().apply { put("isRead", 1) }
        db.update("alerts", values, "id = ?", arrayOf(alertId))
        _signal.value = System.currentTimeMillis()
    }
}


data class UserAccountEntity(
    val id: String,
    val email: String,
    val password: String,
    val fullName: String,
    val role: String,
    val familyId: String? = null,
    val familyName: String? = null,
    val inviteCode: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

interface UserDao {
    suspend fun register(user: UserAccountEntity): Boolean
    suspend fun getUserByEmail(email: String): UserAccountEntity?
    suspend fun getUserById(userId: String): UserAccountEntity?
    suspend fun updateFamily(userId: String, familyId: String, familyName: String, inviteCode: String, role: String? = null)
}

class UserDaoImpl(private val dbHelper: DatabaseHelper) : UserDao {
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
        res != -1L
    }

    override suspend fun getUserByEmail(email: String): UserAccountEntity? = withContext(Dispatchers.IO) {
        queryUserByEmail(email)
    }

    override suspend fun getUserById(userId: String): UserAccountEntity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("users", null, "id = ?", arrayOf(userId), null, null, null)
        cursor.use { c ->
            if (c.moveToFirst()) parseUser(c) else null
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
            if (c.moveToFirst()) parseUser(c) else null
        }
    }

    private fun parseUser(c: android.database.Cursor): UserAccountEntity {
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
            password = c.getString(passCol),
            fullName = c.getString(nameCol),
            role = c.getString(roleCol),
            familyId = if (!c.isNull(famIdCol)) c.getString(famIdCol) else null,
            familyName = if (!c.isNull(famNameCol)) c.getString(famNameCol) else null,
            inviteCode = if (!c.isNull(inviteCol)) c.getString(inviteCol) else null,
            createdAt = c.getLong(createdCol)
        )
    }
}
