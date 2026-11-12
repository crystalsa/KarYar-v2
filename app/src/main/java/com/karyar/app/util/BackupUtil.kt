package com.karyar.app.util

import android.content.Context
import android.net.Uri
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class FullBackupPayload(
    val format: String = BACKUP_FORMAT_ID,
    val backupVersion: Int = CURRENT_BACKUP_VERSION,
    val dbVersion: Int = AppDatabase.CURRENT_DB_VERSION,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val createdAtJalali: String = JalaliCalendar.todayString(),
    val folders: List<WorkplaceFolderEntity>,
    val dateFolders: List<DateFolderEntity>,
    val workers: List<WorkerEntity>,
    val attendances: List<AttendanceEntity>,
    val expenses: List<ExpenseEntity>
) {
    companion object {
        const val BACKUP_FORMAT_ID = "KARYAR_FULL_BACKUP"
        const val CURRENT_BACKUP_VERSION = 1
    }
}

data class LocalBackupFileInfo(
    val file: File,
    val fileName: String,
    val createdAtJalali: String,
    val createdAtTimestamp: Long,
    val foldersCount: Int,
    val dateFoldersCount: Int,
    val workersCount: Int,
    val attendancesCount: Int,
    val expensesCount: Int
)

object BackupUtil {

    const val LOCAL_BACKUPS_DIR = "backups"

    fun defaultBackupFileName(jalaliDate: String = JalaliCalendar.todayString()): String {
        val safeDate = jalaliDate.replace("/", "-")
        val timeStamp = System.currentTimeMillis() % 100000
        return "KarYar_Backup_${safeDate}_$timeStamp.json"
    }

    fun serializeBackupToJson(payload: FullBackupPayload): String {
        val root = JSONObject()
        root.put("format", payload.format)
        root.put("backupVersion", payload.backupVersion)
        root.put("dbVersion", payload.dbVersion)
        root.put("createdAtTimestamp", payload.createdAtTimestamp)
        root.put("createdAtJalali", payload.createdAtJalali)

        val foldersArray = JSONArray()
        for (f in payload.folders) {
            foldersArray.put(
                JSONObject().apply {
                    put("id", f.id)
                    put("name", f.name)
                    put("foremanName", f.foremanName)
                    put("employerName", f.employerName)
                    put("colorTag", f.colorTag)
                    put("createdAt", f.createdAt)
                    put("notes", f.notes)
                }
            )
        }
        root.put("workplace_folders", foldersArray)

        val dateFoldersArray = JSONArray()
        for (df in payload.dateFolders) {
            dateFoldersArray.put(
                JSONObject().apply {
                    put("id", df.id)
                    put("folderId", df.folderId)
                    put("date", df.date)
                    put("dayOfWeek", df.dayOfWeek)
                    put("title", df.title)
                    put("notes", df.notes)
                    put("createdAt", df.createdAt)
                    put("epochDay", df.epochDay)
                }
            )
        }
        root.put("date_folders", dateFoldersArray)

        val workersArray = JSONArray()
        for (w in payload.workers) {
            workersArray.put(
                JSONObject().apply {
                    put("id", w.id)
                    put("folderId", w.folderId)
                    put("name", w.name)
                    put("role", w.role)
                    put("phone", w.phone)
                    put("nationalId", w.nationalId)
                    put("baseDailyWage", w.baseDailyWage)
                    put("baseHourlyWage", w.baseHourlyWage)
                    put("isHourlyEnabled", w.isHourlyEnabled)
                    put("hourlyWageRate", w.hourlyWageRate)
                    put("hourlyHours", w.hourlyHours)
                    put("isOvertimeEnabled", w.isOvertimeEnabled)
                    put("overtimeRate", w.overtimeRate)
                    put("overtimeHours", w.overtimeHours)
                    put("isActive", w.isActive)
                    put("notes", w.notes)
                    put("colorTag", w.colorTag)
                    put("transitAllowance", w.transitAllowance)
                    put("transitImpact", w.transitImpact)
                    put("foodAllowance", w.foodAllowance)
                    put("foodImpact", w.foodImpact)
                    put("accommodationAllowance", w.accommodationAllowance)
                    put("accommodationImpact", w.accommodationImpact)
                    put("medicalAllowance", w.medicalAllowance)
                    put("medicalImpact", w.medicalImpact)
                    put("createdAt", w.createdAt)
                }
            )
        }
        root.put("workers", workersArray)

        val attendancesArray = JSONArray()
        for (att in payload.attendances) {
            attendancesArray.put(
                JSONObject().apply {
                    put("id", att.id)
                    put("folderId", att.folderId)
                    put("workerId", att.workerId)
                    put("dateFolderId", att.dateFolderId)
                    put("date", att.date)
                    put("epochDay", att.epochDay)
                    put("status", att.status.name)
                    put("timestamp", att.timestamp)
                    put("entryTime", att.entryTime)
                    put("exitTime", att.exitTime)
                    put("regularHours", att.regularHours)
                    put("overtimeHours", att.overtimeHours)
                    put("overtimeRate", att.overtimeRate)
                    put("hourlyWageRate", att.hourlyWageRate)
                    put("hourlyHours", att.hourlyHours)
                    put("dailyWage", att.dailyWage)
                    put("hourlyWage", att.hourlyWage)
                    put("workplaceName", att.workplaceName)
                    put("employerName", att.employerName)
                    put("foremanName", att.foremanName)
                    put("notes", att.notes)
                    put("transitAllowance", att.transitAllowance)
                    put("transitImpact", att.transitImpact)
                    put("foodAllowance", att.foodAllowance)
                    put("foodImpact", att.foodImpact)
                    put("accommodationAllowance", att.accommodationAllowance)
                    put("accommodationImpact", att.accommodationImpact)
                    put("medicalAllowance", att.medicalAllowance)
                    put("medicalImpact", att.medicalImpact)
                    put("hasCustomAllowances", att.hasCustomAllowances)
                }
            )
        }
        root.put("attendance", attendancesArray)

        val expensesArray = JSONArray()
        for (exp in payload.expenses) {
            expensesArray.put(
                JSONObject().apply {
                    put("id", exp.id)
                    put("folderId", exp.folderId)
                    put("title", exp.title)
                    put("category", exp.category)
                    put("scope", exp.scope)
                    put("impactType", exp.impactType)
                    if (exp.workerId != null) {
                        put("workerId", exp.workerId)
                    } else {
                        put("workerId", JSONObject.NULL)
                    }
                    if (exp.workerName != null) {
                        put("workerName", exp.workerName)
                    } else {
                        put("workerName", JSONObject.NULL)
                    }
                    put("amount", exp.amount)
                    put("accommodationDays", exp.accommodationDays)
                    put("date", exp.date)
                    put("epochDay", exp.epochDay)
                    put("timestamp", exp.timestamp)
                    put("workplaceName", exp.workplaceName)
                    put("employerName", exp.employerName)
                    put("foremanName", exp.foremanName)
                    put("notes", exp.notes)
                }
            )
        }
        root.put("expenses", expensesArray)

        return root.toString(2)
    }

    private fun optFiniteNonNegativeDouble(obj: JSONObject, key: String, default: Double): Double {
        val raw = obj.optDouble(key, default)
        return if (raw.isFinite() && raw >= 0.0) raw else default
    }

    private fun optNonNegativeLong(obj: JSONObject, key: String, default: Long): Long {
        val raw = obj.optLong(key, default)
        return if (raw >= 0L) raw else default
    }

    private fun normalizeImpact(raw: String, default: String = "ALLOWANCE"): String {
        val upper = raw.trim().uppercase()
        return if (upper == "ALLOWANCE" || upper == "DEDUCTION") upper else default
    }

    private fun normalizeScope(raw: String): String {
        val upper = raw.trim().uppercase()
        return if (upper == "INDIVIDUAL" || upper == "GROUP") upper else "GROUP"
    }

    private fun normalizeCategory(raw: String): String {
        val upper = raw.trim().uppercase()
        return when (upper) {
            "TRANSIT", "ACCOMMODATION", "FOOD", "MEDICAL", "OTHER" -> upper
            else -> "OTHER"
        }
    }

    fun parseBackupFromJson(jsonString: String): FullBackupPayload {
        val trimmed = jsonString.trim().removePrefix("\uFEFF")
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("فایل پشتیبان خالی است")
        }
        val root = try {
            JSONObject(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("فرمت فایل پشتیبان معتبر نیست (JSON نامعتبر)", e)
        }

        val format = root.optString("format", "")
        if (format != FullBackupPayload.BACKUP_FORMAT_ID && !root.has("workplace_folders")) {
            throw IllegalArgumentException("این فایل یک نسخه پشتیبان معتبر برنامه کاریار نیست")
        }

        val backupVersion = root.optInt("backupVersion", FullBackupPayload.CURRENT_BACKUP_VERSION)
        val dbVersion = root.optInt("dbVersion", AppDatabase.CURRENT_DB_VERSION)
        val createdAtTimestamp = root.optLong("createdAtTimestamp", System.currentTimeMillis())
        val createdAtJalali = root.optString("createdAtJalali", JalaliCalendar.todayString())

        val foldersArray = root.optJSONArray("workplace_folders") ?: JSONArray()
        val folders = mutableListOf<WorkplaceFolderEntity>()
        for (i in 0 until foldersArray.length()) {
            val obj = foldersArray.optJSONObject(i) ?: continue
            val id = obj.optLong("id", 0L)
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) continue
            folders.add(
                WorkplaceFolderEntity(
                    id = id,
                    name = name,
                    foremanName = obj.optString("foremanName", ""),
                    employerName = obj.optString("employerName", ""),
                    colorTag = obj.optLong("colorTag", 0xFFD97706L),
                    createdAt = obj.optString("createdAt", ""),
                    notes = obj.optString("notes", "")
                )
            )
        }

        val dateFoldersArray = root.optJSONArray("date_folders") ?: JSONArray()
        val dateFolders = mutableListOf<DateFolderEntity>()
        for (i in 0 until dateFoldersArray.length()) {
            val obj = dateFoldersArray.optJSONObject(i) ?: continue
            val rawDate = obj.optString("date", "")
            val fixedDate = JalaliCalendar.fixLegacyInvalidDate(rawDate) ?: rawDate
            val epochDay = JalaliCalendar.safeEpochDay(fixedDate)
            val rawDayOfWeek = obj.optString("dayOfWeek", "")
            val dayOfWeek = if (rawDayOfWeek.isNotBlank()) rawDayOfWeek else JalaliCalendar.getDayOfWeek(fixedDate)
            dateFolders.add(
                DateFolderEntity(
                    id = obj.optLong("id", 0L),
                    folderId = obj.optLong("folderId", 0L),
                    date = fixedDate,
                    dayOfWeek = dayOfWeek,
                    title = obj.optString("title", ""),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    epochDay = epochDay
                )
            )
        }

        val workersArray = root.optJSONArray("workers") ?: JSONArray()
        val workers = mutableListOf<WorkerEntity>()
        for (i in 0 until workersArray.length()) {
            val obj = workersArray.optJSONObject(i) ?: continue
            workers.add(
                WorkerEntity(
                    id = obj.optLong("id", 0L),
                    folderId = obj.optLong("folderId", 0L),
                    name = obj.optString("name", ""),
                    role = obj.optString("role", ""),
                    phone = obj.optString("phone", ""),
                    nationalId = obj.optString("nationalId", ""),
                    baseDailyWage = optNonNegativeLong(obj, "baseDailyWage", 0L),
                    baseHourlyWage = optNonNegativeLong(obj, "baseHourlyWage", 0L),
                    isHourlyEnabled = obj.optBoolean("isHourlyEnabled", false),
                    hourlyWageRate = optNonNegativeLong(obj, "hourlyWageRate", 0L),
                    hourlyHours = optFiniteNonNegativeDouble(obj, "hourlyHours", 0.0),
                    isOvertimeEnabled = obj.optBoolean("isOvertimeEnabled", false),
                    overtimeRate = optNonNegativeLong(obj, "overtimeRate", 0L),
                    overtimeHours = optFiniteNonNegativeDouble(obj, "overtimeHours", 0.0),
                    isActive = obj.optBoolean("isActive", true),
                    notes = obj.optString("notes", ""),
                    colorTag = obj.optLong("colorTag", 0xFFD97706L),
                    transitAllowance = optNonNegativeLong(obj, "transitAllowance", 0L),
                    transitImpact = normalizeImpact(obj.optString("transitImpact", "ALLOWANCE")),
                    foodAllowance = optNonNegativeLong(obj, "foodAllowance", 0L),
                    foodImpact = normalizeImpact(obj.optString("foodImpact", "ALLOWANCE")),
                    accommodationAllowance = optNonNegativeLong(obj, "accommodationAllowance", 0L),
                    accommodationImpact = normalizeImpact(obj.optString("accommodationImpact", "ALLOWANCE")),
                    medicalAllowance = optNonNegativeLong(obj, "medicalAllowance", 0L),
                    medicalImpact = normalizeImpact(obj.optString("medicalImpact", "ALLOWANCE")),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val attendancesArray = root.optJSONArray("attendance") ?: JSONArray()
        val attendances = mutableListOf<AttendanceEntity>()
        for (i in 0 until attendancesArray.length()) {
            val obj = attendancesArray.optJSONObject(i) ?: continue
            val rawDate = obj.optString("date", "")
            val fixedDate = JalaliCalendar.fixLegacyInvalidDate(rawDate) ?: rawDate
            val epochDay = JalaliCalendar.safeEpochDay(fixedDate)
            val statusStr = obj.optString("status", AttendanceStatus.FULL_DAY.name)
            val status = try {
                AttendanceStatus.valueOf(statusStr)
            } catch (_: Exception) {
                WageCalculator.parseStatus(statusStr)
            }
            attendances.add(
                AttendanceEntity(
                    id = obj.optLong("id", 0L),
                    folderId = obj.optLong("folderId", 0L),
                    workerId = obj.optLong("workerId", 0L),
                    dateFolderId = obj.optLong("dateFolderId", 0L),
                    date = fixedDate,
                    epochDay = epochDay,
                    status = status,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    entryTime = obj.optString("entryTime", "08:00"),
                    exitTime = obj.optString("exitTime", "17:00"),
                    regularHours = optFiniteNonNegativeDouble(obj, "regularHours", 8.0),
                    overtimeHours = optFiniteNonNegativeDouble(obj, "overtimeHours", 0.0),
                    overtimeRate = optNonNegativeLong(obj, "overtimeRate", 0L),
                    hourlyWageRate = optNonNegativeLong(obj, "hourlyWageRate", 0L),
                    hourlyHours = optFiniteNonNegativeDouble(obj, "hourlyHours", 0.0),
                    dailyWage = optNonNegativeLong(obj, "dailyWage", 0L),
                    hourlyWage = optNonNegativeLong(obj, "hourlyWage", 0L),
                    workplaceName = obj.optString("workplaceName", ""),
                    employerName = obj.optString("employerName", ""),
                    foremanName = obj.optString("foremanName", ""),
                    notes = obj.optString("notes", ""),
                    transitAllowance = optNonNegativeLong(obj, "transitAllowance", 0L),
                    transitImpact = normalizeImpact(obj.optString("transitImpact", "ALLOWANCE")),
                    foodAllowance = optNonNegativeLong(obj, "foodAllowance", 0L),
                    foodImpact = normalizeImpact(obj.optString("foodImpact", "ALLOWANCE")),
                    accommodationAllowance = optNonNegativeLong(obj, "accommodationAllowance", 0L),
                    accommodationImpact = normalizeImpact(obj.optString("accommodationImpact", "ALLOWANCE")),
                    medicalAllowance = optNonNegativeLong(obj, "medicalAllowance", 0L),
                    medicalImpact = normalizeImpact(obj.optString("medicalImpact", "ALLOWANCE")),
                    hasCustomAllowances = obj.optBoolean("hasCustomAllowances", false)
                )
            )
        }

        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
        val expenses = mutableListOf<ExpenseEntity>()
        for (i in 0 until expensesArray.length()) {
            val obj = expensesArray.optJSONObject(i) ?: continue
            val rawDate = obj.optString("date", "")
            val fixedDate = JalaliCalendar.fixLegacyInvalidDate(rawDate) ?: rawDate
            val epochDay = JalaliCalendar.safeEpochDay(fixedDate)
            val workerId = if (obj.isNull("workerId")) null else obj.optLong("workerId", 0L).takeIf { it > 0L }
            val workerName = if (obj.isNull("workerName")) null else obj.optString("workerName", "").takeIf { it.isNotBlank() }
            val normalizedScope = normalizeScope(obj.optString("scope", "GROUP"))
            expenses.add(
                ExpenseEntity(
                    id = obj.optLong("id", 0L),
                    folderId = obj.optLong("folderId", 0L),
                    title = obj.optString("title", ""),
                    category = normalizeCategory(obj.optString("category", "OTHER")),
                    scope = normalizedScope,
                    impactType = normalizeImpact(
                        obj.optString("impactType", "DEDUCTION"),
                        default = "DEDUCTION"
                    ),
                    workerId = workerId,
                    workerName = workerName,
                    amount = optNonNegativeLong(obj, "amount", 0L),
                    accommodationDays = obj.optInt("accommodationDays", 0).coerceAtLeast(0),
                    date = fixedDate,
                    epochDay = epochDay,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    workplaceName = obj.optString("workplaceName", ""),
                    employerName = obj.optString("employerName", ""),
                    foremanName = obj.optString("foremanName", ""),
                    notes = obj.optString("notes", "")
                )
            )
        }

        return FullBackupPayload(
            format = FullBackupPayload.BACKUP_FORMAT_ID,
            backupVersion = backupVersion,
            dbVersion = dbVersion,
            createdAtTimestamp = createdAtTimestamp,
            createdAtJalali = createdAtJalali,
            folders = folders,
            dateFolders = dateFolders,
            workers = workers,
            attendances = attendances,
            expenses = expenses
        )
    }

    fun getLocalBackupsDir(context: Context): File {
        val dir = File(context.filesDir, LOCAL_BACKUPS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveLocalBackupFile(
        context: Context,
        jsonContent: String,
        fileName: String = defaultBackupFileName()
    ): File {
        val dir = getLocalBackupsDir(context)
        val file = File(dir, fileName)
        file.writeText(jsonContent, Charsets.UTF_8)
        return file
    }

    fun writeBackupToUri(context: Context, uri: Uri, jsonContent: String) {
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("امکان باز کردن فایل مقصد وجود ندارد")
        outputStream.use { stream ->
            stream.write(jsonContent.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("امکان خواندن فایل انتخاب‌شده وجود ندارد")
        return inputStream.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        }
    }

    fun listLocalBackups(context: Context): List<LocalBackupFileInfo> {
        val dir = getLocalBackupsDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json", ignoreCase = true) }
            ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                val payload = parseBackupFromJson(file.readText(Charsets.UTF_8))
                LocalBackupFileInfo(
                    file = file,
                    fileName = file.name,
                    createdAtJalali = payload.createdAtJalali,
                    createdAtTimestamp = payload.createdAtTimestamp,
                    foldersCount = payload.folders.size,
                    dateFoldersCount = payload.dateFolders.size,
                    workersCount = payload.workers.size,
                    attendancesCount = payload.attendances.size,
                    expensesCount = payload.expenses.size
                )
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.createdAtTimestamp }
    }
}
