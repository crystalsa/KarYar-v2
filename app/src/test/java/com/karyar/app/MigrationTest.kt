package com.karyar.app

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.karyar.app.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private fun insertWorkerV5(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        id: Long,
        folderId: Long,
        name: String,
        role: String = "کارگر",
        dateFolderId: Long = 10,
        workDate: String = "1403/01/01",
        dayOfWeek: String = "شنبه",
        nationalId: String = ""
    ) {
        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `dateFolderId`, `workDate`, `dayOfWeek`, `name`, `role`, `phone`, `nationalId`,
                `baseDailyWage`, `baseHourlyWage`, `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`,
                `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`, `isActive`, `notes`, `colorTag`,
                `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES (
                $id, $folderId, $dateFolderId, '$workDate', '$dayOfWeek', '$name', '$role', '', '$nationalId',
                1000000, 0, 0, 0, 0.0, 0, 0, 0.0, 1, '', 16086790,
                0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1000
            )
        """.trimIndent())
    }

    private fun insertAttendanceV5(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        id: Long,
        folderId: Long,
        workerId: Long,
        date: String,
        timestamp: Long = 1000L,
        notes: String = "",
        regularHours: Double = 8.0,
        hourlyHours: Double = 0.0,
        hourlyWageRate: Long = 0L,
        dailyWage: Long = 1000000L,
        hourlyWage: Long = 0L,
        overtimeHours: Double = 0.0,
        overtimeRate: Long = 0L,
        entryTime: String = "08:00",
        exitTime: String = "17:00",
        workplaceName: String = "کارگاه ۱",
        employerName: String = "",
        foremanName: String = ""
    ) {
        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `date`, `timestamp`, `entryTime`, `exitTime`,
                `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`, `hourlyHours`,
                `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES (
                $id, $folderId, $workerId, '$date', $timestamp, '$entryTime', '$exitTime',
                $regularHours, $overtimeHours, $overtimeRate, $hourlyWageRate, $hourlyHours,
                $dailyWage, $hourlyWage, '$workplaceName', '$employerName', '$foremanName', '$notes'
            )
        """.trimIndent())
    }

    private fun insertExpenseV5(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        id: Long,
        folderId: Long,
        title: String,
        category: String = "FOOD",
        scope: String = "GROUP",
        amount: Long,
        date: String = "1403/01/01",
        timestamp: Long = 1000L,
        impactType: String = "EXPENSE",
        accommodationDays: Int = 0,
        workplaceName: String = "",
        employerName: String = "",
        foremanName: String = "",
        notes: String = ""
    ) {
        db.execSQL("""
            INSERT INTO `expenses` (
                `id`, `folderId`, `title`, `category`, `scope`, `impactType`,
                `workerId`, `workerName`, `amount`, `accommodationDays`,
                `date`, `timestamp`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES (
                $id, $folderId, '$title', '$category', '$scope', '$impactType',
                NULL, NULL, $amount, $accommodationDays,
                '$date', $timestamp, '$workplaceName', '$employerName', '$foremanName', '$notes'
            )
        """.trimIndent())
    }

    private fun readWorkers(db: androidx.sqlite.db.SupportSQLiteDatabase): List<com.karyar.app.data.local.entity.WorkerEntity> {
        val cursor = db.query("""
            SELECT id, folderId, name, role, phone, nationalId, baseDailyWage, baseHourlyWage,
                   isHourlyEnabled, hourlyWageRate, hourlyHours, isOvertimeEnabled, overtimeRate, overtimeHours,
                   isActive, notes, colorTag, transitAllowance, transitImpact, foodAllowance, foodImpact,
                   accommodationAllowance, accommodationImpact, medicalAllowance, medicalImpact, createdAt
            FROM workers ORDER BY id ASC
        """.trimIndent())
        val list = mutableListOf<com.karyar.app.data.local.entity.WorkerEntity>()
        while (cursor.moveToNext()) {
            list.add(
                com.karyar.app.data.local.entity.WorkerEntity(
                    id = cursor.getLong(0),
                    folderId = cursor.getLong(1),
                    name = cursor.getString(2),
                    role = cursor.getString(3),
                    phone = cursor.getString(4),
                    nationalId = cursor.getString(5),
                    baseDailyWage = cursor.getLong(6),
                    baseHourlyWage = cursor.getLong(7),
                    isHourlyEnabled = cursor.getInt(8) != 0,
                    hourlyWageRate = cursor.getLong(9),
                    hourlyHours = cursor.getDouble(10),
                    isOvertimeEnabled = cursor.getInt(11) != 0,
                    overtimeRate = cursor.getLong(12),
                    overtimeHours = cursor.getDouble(13),
                    isActive = cursor.getInt(14) != 0,
                    notes = cursor.getString(15),
                    colorTag = cursor.getLong(16),
                    transitAllowance = cursor.getLong(17),
                    transitImpact = cursor.getString(18),
                    foodAllowance = cursor.getLong(19),
                    foodImpact = cursor.getString(20),
                    accommodationAllowance = cursor.getLong(21),
                    accommodationImpact = cursor.getString(22),
                    medicalAllowance = cursor.getLong(23),
                    medicalImpact = cursor.getString(24),
                    createdAt = cursor.getLong(25)
                )
            )
        }
        cursor.close()
        return list
    }

    private fun readAttendancesV6(db: androidx.sqlite.db.SupportSQLiteDatabase): List<com.karyar.app.data.local.entity.AttendanceEntity> {
        val cursor = db.query("""
            SELECT id, folderId, workerId, dateFolderId, date, epochDay, status, timestamp,
                   entryTime, exitTime, regularHours, overtimeHours, overtimeRate, hourlyWageRate,
                   hourlyHours, dailyWage, hourlyWage, workplaceName, employerName, foremanName, notes
            FROM attendance ORDER BY id ASC
        """.trimIndent())
        val list = mutableListOf<com.karyar.app.data.local.entity.AttendanceEntity>()
        while (cursor.moveToNext()) {
            list.add(
                com.karyar.app.data.local.entity.AttendanceEntity(
                    id = cursor.getLong(0),
                    folderId = cursor.getLong(1),
                    workerId = cursor.getLong(2),
                    dateFolderId = cursor.getLong(3),
                    date = cursor.getString(4),
                    epochDay = cursor.getLong(5),
                    status = com.karyar.app.data.local.entity.AttendanceStatus.valueOf(cursor.getString(6)),
                    timestamp = cursor.getLong(7),
                    entryTime = cursor.getString(8),
                    exitTime = cursor.getString(9),
                    regularHours = cursor.getDouble(10),
                    overtimeHours = cursor.getDouble(11),
                    overtimeRate = cursor.getLong(12),
                    hourlyWageRate = cursor.getLong(13),
                    hourlyHours = cursor.getDouble(14),
                    dailyWage = cursor.getLong(15),
                    hourlyWage = cursor.getLong(16),
                    workplaceName = cursor.getString(17),
                    employerName = cursor.getString(18),
                    foremanName = cursor.getString(19),
                    notes = cursor.getString(20),
                    hasCustomAllowances = false
                )
            )
        }
        cursor.close()
        return list
    }

    private fun readAttendancesV7(db: androidx.sqlite.db.SupportSQLiteDatabase): List<com.karyar.app.data.local.entity.AttendanceEntity> {
        val cursor = db.query("""
            SELECT id, folderId, workerId, dateFolderId, date, epochDay, status, timestamp,
                   entryTime, exitTime, regularHours, overtimeHours, overtimeRate, hourlyWageRate,
                   hourlyHours, dailyWage, hourlyWage, workplaceName, employerName, foremanName, notes,
                   transitAllowance, transitImpact, foodAllowance, foodImpact,
                   accommodationAllowance, accommodationImpact, medicalAllowance, medicalImpact,
                   hasCustomAllowances
            FROM attendance ORDER BY id ASC
        """.trimIndent())
        val list = mutableListOf<com.karyar.app.data.local.entity.AttendanceEntity>()
        while (cursor.moveToNext()) {
            list.add(
                com.karyar.app.data.local.entity.AttendanceEntity(
                    id = cursor.getLong(0),
                    folderId = cursor.getLong(1),
                    workerId = cursor.getLong(2),
                    dateFolderId = cursor.getLong(3),
                    date = cursor.getString(4),
                    epochDay = cursor.getLong(5),
                    status = com.karyar.app.data.local.entity.AttendanceStatus.valueOf(cursor.getString(6)),
                    timestamp = cursor.getLong(7),
                    entryTime = cursor.getString(8),
                    exitTime = cursor.getString(9),
                    regularHours = cursor.getDouble(10),
                    overtimeHours = cursor.getDouble(11),
                    overtimeRate = cursor.getLong(12),
                    hourlyWageRate = cursor.getLong(13),
                    hourlyHours = cursor.getDouble(14),
                    dailyWage = cursor.getLong(15),
                    hourlyWage = cursor.getLong(16),
                    workplaceName = cursor.getString(17),
                    employerName = cursor.getString(18),
                    foremanName = cursor.getString(19),
                    notes = cursor.getString(20),
                    transitAllowance = cursor.getLong(21),
                    transitImpact = cursor.getString(22),
                    foodAllowance = cursor.getLong(23),
                    foodImpact = cursor.getString(24),
                    accommodationAllowance = cursor.getLong(25),
                    accommodationImpact = cursor.getString(26),
                    medicalAllowance = cursor.getLong(27),
                    medicalImpact = cursor.getString(28),
                    hasCustomAllowances = cursor.getInt(29) != 0
                )
            )
        }
        cursor.close()
        return list
    }

    private fun readExpenses(db: androidx.sqlite.db.SupportSQLiteDatabase): List<com.karyar.app.data.local.entity.ExpenseEntity> {
        val cursor = db.query("""
            SELECT id, folderId, title, category, scope, impactType, workerId, workerName,
                   amount, accommodationDays, date, epochDay, timestamp, workplaceName, employerName, foremanName, notes
            FROM expenses ORDER BY id ASC
        """.trimIndent())
        val list = mutableListOf<com.karyar.app.data.local.entity.ExpenseEntity>()
        while (cursor.moveToNext()) {
            list.add(
                com.karyar.app.data.local.entity.ExpenseEntity(
                    id = cursor.getLong(0),
                    folderId = cursor.getLong(1),
                    title = cursor.getString(2),
                    category = cursor.getString(3),
                    scope = cursor.getString(4),
                    impactType = cursor.getString(5),
                    workerId = if (cursor.isNull(6)) null else cursor.getLong(6),
                    workerName = if (cursor.isNull(7)) null else cursor.getString(7),
                    amount = cursor.getLong(8),
                    accommodationDays = cursor.getInt(9),
                    date = cursor.getString(10),
                    epochDay = cursor.getLong(11),
                    timestamp = cursor.getLong(12),
                    workplaceName = cursor.getString(13),
                    employerName = cursor.getString(14),
                    foremanName = cursor.getString(15),
                    notes = cursor.getString(16)
                )
            )
        }
        cursor.close()
        return list
    }

    @Test
    fun migrate5To6_preservesDistinctWorkersWithSameName() {
        // 1. Create database with schema version 5
        var db = helper.createDatabase(TEST_DB, 5)

        // Insert a workplace folder
        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (1, 'پروژه تست', 'سرکارگر ۱', 'کارفرما ۱', 16086790, '1403/01/01', '')
        """)

        // Insert a date folder
        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`)
            VALUES (10, 1, '1403/01/01', 'شنبه', 'روز اول', '', 1000)
        """)

        // Mandatory scenario: Two distinct workers with the exact SAME name in the same workplace on the same day
        // Worker 1 (id = 101, name = "علی رضایی", role = "بنا")
        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `dateFolderId`, `workDate`, `dayOfWeek`, `name`, `role`, `phone`, `nationalId`,
                `baseDailyWage`, `baseHourlyWage`, `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`,
                `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`, `isActive`, `notes`, `colorTag`,
                `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES (
                101, 1, 10, '1403/01/01', 'شنبه', 'علی رضایی', 'بنا', '09120000001', '0011111111',
                1000000, 0, 0, 0, 0.0, 0, 0, 0.0, 1, '', 16086790,
                0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1000
            )
        """)

        // Worker 2 (id = 102, name = "علی رضایی" - same name!, role = "نقاش", different nationalId)
        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `dateFolderId`, `workDate`, `dayOfWeek`, `name`, `role`, `phone`, `nationalId`,
                `baseDailyWage`, `baseHourlyWage`, `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`,
                `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`, `isActive`, `notes`, `colorTag`,
                `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES (
                102, 1, 10, '1403/01/01', 'شنبه', 'علی رضایی', 'نقاش', '09120000002', '0022222222',
                1200000, 0, 0, 0, 0.0, 0, 0, 0.0, 1, '', 16086790,
                0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1000
            )
        """)

        // Attendance for Worker 1
        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `date`, `timestamp`, `entryTime`, `exitTime`,
                `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`, `hourlyHours`,
                `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES (
                1, 1, 101, '1403/01/01', 2000, '08:00', '17:00',
                8.0, 0.0, 0, 0, 0.0, 1000000, 0, 'پروژه تست', 'کارفرما ۱', 'سرکارگر ۱', 'تمام روز'
            )
        """)

        // Attendance for Worker 2
        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `date`, `timestamp`, `entryTime`, `exitTime`,
                `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`, `hourlyHours`,
                `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES (
                2, 1, 102, '1403/01/01', 2000, '08:00', '17:00',
                8.0, 0.0, 0, 0, 0.0, 1200000, 0, 'پروژه تست', 'کارفرما ۱', 'سرکارگر ۱', 'تمام روز'
            )
        """)

        // Duplicate attendance row with older timestamp for worker 101 to verify safe deduplication
        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `date`, `timestamp`, `entryTime`, `exitTime`,
                `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`, `hourlyHours`,
                `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES (
                999, 1, 101, '1403/01/01', 1000, '08:00', '17:00',
                8.0, 0.0, 0, 0, 0.0, 1000000, 0, 'پروژه تست', 'کارفرما ۱', 'سرکارگر ۱', 'قدیمی'
            )
        """)

        // Close db v5
        db.close()

        // 2. Run migration to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, AppDatabase.MIGRATION_5_6)

        // 3. Verify that BOTH workers still exist with their original IDs (NO MERGING)
        val workersCursor = db.query("SELECT id, name, role FROM workers ORDER BY id ASC")
        val workersList = mutableListOf<Triple<Long, String, String>>()
        while (workersCursor.moveToNext()) {
            workersList.add(Triple(workersCursor.getLong(0), workersCursor.getString(1), workersCursor.getString(2)))
        }
        workersCursor.close()

        assertEquals("Both workers must be preserved without merging", 2, workersList.size)
        assertEquals(101L, workersList[0].first)
        assertEquals("علی رضایی", workersList[0].second)
        assertEquals("بنا", workersList[0].third)

        assertEquals(102L, workersList[1].first)
        assertEquals("علی رضایی", workersList[1].second)
        assertEquals("نقاش", workersList[1].third)

        // 4. Verify that attendances still point to their respective original workerIds and duplicate was removed
        val attCursor = db.query("SELECT id, workerId, status, timestamp FROM attendance ORDER BY workerId ASC")
        val attList = mutableListOf<Triple<Long, Long, String>>()
        while (attCursor.moveToNext()) {
            attList.add(Triple(attCursor.getLong(0), attCursor.getLong(1), attCursor.getString(2)))
        }
        attCursor.close()

        assertEquals("Exactly two attendance records must remain (duplicate deduplicated)", 2, attList.size)
        assertEquals(101L, attList[0].second)
        assertEquals(1L, attList[0].first) // kept newer timestamp row 1 (ts=2000), removed row 999 (ts=1000)
        assertEquals("FULL_DAY", attList[0].third)

        assertEquals(102L, attList[1].second)
        assertEquals(2L, attList[1].first)
        assertEquals("FULL_DAY", attList[1].third)

        // 5. Verify migration_report in SharedPreferences and deleted row backup file in db_backups
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val report = AppDatabase.getPendingMigrationReport(context)
        org.junit.Assert.assertNotNull("Migration report must be present when duplicates were deleted", report)
        assertEquals(1, report!!.deletedDuplicatesCount)
        assertEquals(0, report.deletedOrphansCount)
        assertEquals("در به‌روزرسانی داده‌ها ۱ رکورد حضور تکراری و ۰ رکورد ناسازگار حذف شد", report.formatDialogMessage())
        org.junit.Assert.assertFalse("Backup file path must not be blank", report.backupFilePath.isNullOrBlank())
        val backupFile = java.io.File(report.backupFilePath!!)
        org.junit.Assert.assertTrue("Deleted rows backup file must exist in db_backups", backupFile.exists())
        val backupJson = org.json.JSONObject(backupFile.readText(Charsets.UTF_8))
        val deletedRows = backupJson.getJSONArray("deletedRows")
        assertEquals(1, deletedRows.length())
        val deletedRow = deletedRows.getJSONObject(0)
        assertEquals(999L, deletedRow.getLong("id"))
        assertEquals(101L, deletedRow.getLong("workerId"))
        assertEquals("1403/01/01", deletedRow.getString("date"))
        assertEquals(1000000L, deletedRow.getLong("dailyWage"))
        assertEquals("DUPLICATE_ATTENDANCE", deletedRow.getString("reason"))

        AppDatabase.clearMigrationReport(context)
        backupFile.delete()
        db.close()
    }

    @Test
    fun migrate5To6_convertsLegacyNotesToCorrectStatusAndClearsNotes() {
        var db = helper.createDatabase(TEST_DB + "-notes", 5)

        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (1, 'کارگاه ۱', '', '', 0, '1403/01/01', '')
        """)
        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`)
            VALUES (10, 1, '1403/01/01', 'شنبه', '', '', 1000)
        """)

        // Insert 4 workers
        for (i in 1..4) {
            insertWorkerV5(db, i.toLong(), 1L, "کارگر $i", "نقش")
        }

        // Attendance with legacy notes
        // 1. غیبت
        insertAttendanceV5(db, 1L, 1L, 1L, "1403/01/01", timestamp = 1000L, regularHours = 8.0, hourlyHours = 0.0, notes = "غیبت")
        // 2. ساعتی
        insertAttendanceV5(db, 2L, 1L, 2L, "1403/01/01", timestamp = 1000L, regularHours = 0.0, hourlyHours = 5.0, notes = "ساعتی")
        // 3. نصف روز
        insertAttendanceV5(db, 3L, 1L, 3L, "1403/01/01", timestamp = 1000L, regularHours = 4.0, hourlyHours = 0.0, notes = "نصف روز")
        // 4. تمام روز
        insertAttendanceV5(db, 4L, 1L, 4L, "1403/01/01", timestamp = 1000L, regularHours = 8.0, hourlyHours = 0.0, notes = "تمام روز")

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB + "-notes", 6, true, AppDatabase.MIGRATION_5_6)

        val cursor = db.query("SELECT id, status, notes FROM attendance ORDER BY id ASC")
        val results = mutableListOf<Triple<Long, String, String>>()
        while (cursor.moveToNext()) {
            results.add(Triple(cursor.getLong(0), cursor.getString(1), cursor.getString(2)))
        }
        cursor.close()

        assertEquals(4, results.size)
        // 1: ABSENT, note cleared
        assertEquals("ABSENT", results[0].second)
        assertEquals("", results[0].third)

        // 2: HOURLY, note cleared
        assertEquals("HOURLY", results[1].second)
        assertEquals("", results[1].third)

        // 3: HALF_DAY, note cleared
        assertEquals("HALF_DAY", results[2].second)
        assertEquals("", results[2].third)

        // 4: FULL_DAY, note cleared
        assertEquals("FULL_DAY", results[3].second)
        assertEquals("", results[3].third)

        db.close()
    }

    @Test
    fun migrate5To6_preservesSameNationalId_andSameNameAcrossDays_andEmptyFolder() {
        var db = helper.createDatabase(TEST_DB + "-national-id", 5)

        // 1. Folder with workers
        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (1, 'پروژه الف', '', '', 0, '1403/01/01', '')
        """)
        // 2. Empty Folder (no workers)
        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (2, 'پروژه خالی بدون کارگر', '', '', 0, '1403/01/01', '')
        """)

        // Date folder
        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`)
            VALUES (10, 1, '1403/01/01', 'شنبه', '', '', 1000)
        """)

        // Two workers with identical National ID
        insertWorkerV5(db, 10L, 1L, "کارگر اول", nationalId = "1234567890")
        insertWorkerV5(db, 20L, 1L, "کارگر دوم با همان کد ملی", nationalId = "1234567890")

        // Same worker name in three different days (representing 3 distinct worker rows from v5 daily clones)
        insertWorkerV5(db, 31L, 1L, "محمد حسینی", workDate = "1403/01/01")
        insertWorkerV5(db, 32L, 1L, "محمد حسینی", workDate = "1403/01/02")
        insertWorkerV5(db, 33L, 1L, "محمد حسینی", workDate = "1403/01/03")

        // Attendance for an orphaned date (date not in date_folders) to verify auto-recovery of date_folder
        insertAttendanceV5(db, 101L, 1L, 10L, "1403/01/05", timestamp = 1000L, regularHours = 8.0, notes = "")

        // Valid expense
        insertExpenseV5(db, 1L, 1L, "ناهار", "FOOD", "GROUP", 50000L, "1403/01/01", 1000L)

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB + "-national-id", 6, true, AppDatabase.MIGRATION_5_6)

        // 1. Verify all 5 workers are preserved with exact IDs
        val wCursor = db.query("SELECT id FROM workers ORDER BY id ASC")
        val workerIds = mutableListOf<Long>()
        while (wCursor.moveToNext()) {
            workerIds.add(wCursor.getLong(0))
        }
        wCursor.close()

        assertEquals("All 5 workers must be preserved without merging", 5, workerIds.size)
        assertEquals(listOf(10L, 20L, 31L, 32L, 33L), workerIds)

        // 2. Verify empty folder exists
        val fCursor = db.query("SELECT id FROM workplace_folders ORDER BY id ASC")
        val folderIds = mutableListOf<Long>()
        while (fCursor.moveToNext()) {
            folderIds.add(fCursor.getLong(0))
        }
        fCursor.close()
        assertEquals(2, folderIds.size)

        // 3. Verify orphaned attendance date '1403/01/05' caused auto-creation of a date_folder
        val dfCursor = db.query("SELECT id, date FROM date_folders WHERE date = '1403/01/05'")
        var recoveredDfId = 0L
        if (dfCursor.moveToNext()) {
            recoveredDfId = dfCursor.getLong(0)
        }
        dfCursor.close()
        org.junit.Assert.assertTrue("Date folder should be automatically created for orphan date", recoveredDfId > 0L)

        // Verify attendance dateFolderId points to recoveredDfId
        val attCheck = db.query("SELECT dateFolderId FROM attendance WHERE id = 101")
        var linkedDfId = 0L
        if (attCheck.moveToNext()) {
            linkedDfId = attCheck.getLong(0)
        }
        attCheck.close()
        assertEquals(recoveredDfId, linkedDfId)

        // 4. Verify total count of expenses matches before migration
        val expCursor = db.query("SELECT COUNT(*) FROM expenses")
        expCursor.moveToNext()
        val expCount = expCursor.getInt(0)
        expCursor.close()
        assertEquals(1, expCount)

        db.close()
    }

    @Test
    fun migrate5To6_verifiesTotalCountsEqualBeforeAndAfterExceptRealDuplicates() {
        var db = helper.createDatabase(TEST_DB + "-counts", 5)

        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (1, 'پروژه جامع', 'سرکارگر', 'کارفرما', 0, '1403/01/01', '')
        """)
        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`)
            VALUES (10, 1, '1403/01/01', 'شنبه', '', '', 1000),
                   (20, 1, '1403/01/02', 'یکشنبه', '', '', 2000)
        """)

        // Insert 3 workers
        for (i in 1..3) {
            insertWorkerV5(db, i.toLong(), 1L, "کارگر $i", "نقش $i")
        }

        // Attendance: 3 on day 1, 2 on day 2, PLUS 1 duplicate on day 1 for worker 1 (total 6 rows in v5)
        insertAttendanceV5(db, 1L, 1L, 1L, "1403/01/01", timestamp = 2000L, regularHours = 8.0, notes = "")
        insertAttendanceV5(db, 2L, 1L, 2L, "1403/01/01", timestamp = 2000L, regularHours = 8.0, notes = "")
        insertAttendanceV5(db, 3L, 1L, 3L, "1403/01/01", timestamp = 2000L, regularHours = 8.0, notes = "")
        insertAttendanceV5(db, 4L, 1L, 1L, "1403/01/02", timestamp = 3000L, regularHours = 8.0, notes = "")
        insertAttendanceV5(db, 5L, 1L, 2L, "1403/01/02", timestamp = 3000L, regularHours = 8.0, notes = "")
        // Duplicate for worker 1 on 1403/01/01 with older timestamp
        insertAttendanceV5(db, 6L, 1L, 1L, "1403/01/01", timestamp = 1000L, regularHours = 8.0, notes = "")

        // 3 Expenses
        insertExpenseV5(db, 1L, 1L, "هزینه ۱", "FOOD", "GROUP", 10000L, "1403/01/01", 1000L)
        insertExpenseV5(db, 2L, 1L, "هزینه ۲", "TRANSIT", "GROUP", 20000L, "1403/01/01", 1000L)
        insertExpenseV5(db, 3L, 1L, "هزینه ۳", "OTHER", "GROUP", 30000L, "1403/01/02", 2000L)

        db.close()

        // Run migration
        db = helper.runMigrationsAndValidate(TEST_DB + "-counts", 6, true, AppDatabase.MIGRATION_5_6)

        // Count workers: must be exactly 3
        val wCountCursor = db.query("SELECT COUNT(*) FROM workers")
        wCountCursor.moveToNext()
        val workerCount = wCountCursor.getInt(0)
        wCountCursor.close()
        assertEquals("Worker count after migration must equal before migration", 3, workerCount)

        // Count attendance: 6 rows originally, exactly 1 duplicate removed -> 5 rows remaining
        val aCountCursor = db.query("SELECT COUNT(*) FROM attendance")
        aCountCursor.moveToNext()
        val attCount = aCountCursor.getInt(0)
        aCountCursor.close()
        assertEquals("Attendance count must equal before migration minus real duplicates", 5, attCount)

        // Count expenses: must be exactly 3
        val eCountCursor = db.query("SELECT COUNT(*) FROM expenses")
        eCountCursor.moveToNext()
        val expCountPost = eCountCursor.getInt(0)
        eCountCursor.close()
        assertEquals("Expenses count after migration must equal before migration", 3, expCountPost)

        // Verify migration report and backup for the 1 deleted duplicate (id = 6)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val report = AppDatabase.getPendingMigrationReport(context)
        org.junit.Assert.assertNotNull(report)
        assertEquals(1, report!!.deletedDuplicatesCount)
        assertEquals(0, report.deletedOrphansCount)
        val backupFile = java.io.File(report.backupFilePath!!)
        org.junit.Assert.assertTrue(backupFile.exists())
        val json = org.json.JSONObject(backupFile.readText(Charsets.UTF_8))
        val deletedRows = json.getJSONArray("deletedRows")
        assertEquals(1, deletedRows.length())
        assertEquals(6L, deletedRows.getJSONObject(0).getLong("id"))
        assertEquals(1L, deletedRows.getJSONObject(0).getLong("workerId"))
        assertEquals("1403/01/01", deletedRows.getJSONObject(0).getString("date"))

        AppDatabase.clearMigrationReport(context)
        backupFile.delete()
        db.close()
    }

    @Test
    fun migrate6To7_addsCustomAllowanceColumnsAndPreservesExistingData() {
        var db = helper.createDatabase(TEST_DB + "-6to7", 6)

        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (1, 'کارگاه نسخه ۶', 'سرکارگر', 'کارفرما', 16086790, '1403/02/01', '')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`, `epochDay`)
            VALUES (10, 1, '1403/02/01', 'شنبه', 'روز اول', '', 1000, 100)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `name`, `role`, `phone`, `nationalId`, `baseDailyWage`, `baseHourlyWage`,
                `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`, `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`,
                `isActive`, `notes`, `colorTag`, `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES (
                101, 1, 'رضا محمدی', 'آرماتوربند', '09121112233', '0012345678', 1500000, 0,
                0, 0, 0.0, 1, 200000, 2.0, 1, 'یادداشت کارگر', 16086790,
                50000, 'ALLOWANCE', 100000, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1000
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `dateFolderId`, `date`, `epochDay`, `status`, `timestamp`,
                `entryTime`, `exitTime`, `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`,
                `hourlyHours`, `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES (
                501, 1, 101, 10, '1403/02/01', 100, 'HALF_DAY', 5000,
                '08:00', '12:00', 4.0, 2.0, 200000, 0,
                0.0, 1500000, 0, 'کارگاه نسخه ۶', 'کارفرما', 'سرکارگر', 'توضیح حضور'
            )
        """.trimIndent())

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB + "-6to7", 7, true, AppDatabase.MIGRATION_6_7)

        val cursor = db.query("""
            SELECT id, workerId, status, dailyWage, overtimeHours, notes,
                   transitAllowance, transitImpact, foodAllowance, foodImpact,
                   accommodationAllowance, accommodationImpact, medicalAllowance, medicalImpact,
                   hasCustomAllowances
            FROM attendance WHERE id = 501
        """.trimIndent())

        org.junit.Assert.assertTrue("Attendance row must exist after 6->7 migration", cursor.moveToFirst())
        assertEquals(501L, cursor.getLong(0))
        assertEquals(101L, cursor.getLong(1))
        assertEquals("HALF_DAY", cursor.getString(2))
        assertEquals(1500000L, cursor.getLong(3))
        assertEquals(2.0, cursor.getDouble(4), 0.001)
        assertEquals("توضیح حضور", cursor.getString(5))
        assertEquals(50000L, cursor.getLong(6))
        assertEquals("ALLOWANCE", cursor.getString(7))
        assertEquals(100000L, cursor.getLong(8))
        assertEquals("ALLOWANCE", cursor.getString(9))
        assertEquals(0L, cursor.getLong(10))
        assertEquals("ALLOWANCE", cursor.getString(11))
        assertEquals(0L, cursor.getLong(12))
        assertEquals("ALLOWANCE", cursor.getString(13))
        assertEquals(1, cursor.getInt(14))
        cursor.close()
        db.close()
    }

    @Test
    fun migrate6To7_preservesExactFinancialTotalsBeforeAndAfterMigrationForMultipleWorkersWithAbsentDays() {
        var db = helper.createDatabase(TEST_DB + "-6to7-financial", 6)

        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (1, 'پروژه تست مالی مایگریشن', 'سرکارگر', 'کارفرما', 16086790, '1403/04/01', '')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`, `epochDay`)
            VALUES (10, 1, '1403/04/01', 'جمعه', 'روز اول', '', 1000, 200),
                   (20, 1, '1403/04/02', 'شنبه', 'روز دوم', '', 2000, 201),
                   (30, 1, '1403/04/03', 'یکشنبه', 'روز سوم', '', 3000, 202)
        """.trimIndent())

        // Worker 1: روزمزد با مزایا و کسورات ترکیبی (ایاب‌وذهاب افزایشی، خوراک افزایشی، اسکان کاهشی، درمان کاهشی)
        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `name`, `role`, `phone`, `nationalId`, `baseDailyWage`, `baseHourlyWage`,
                `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`, `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`,
                `isActive`, `notes`, `colorTag`, `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES (
                101, 1, 'کارگر اول', 'بنا', '09121111111', '0011111111', 1200000, 0,
                0, 0, 0.0, 1, 150000, 2.0, 1, '', 16086790,
                50000, 'ALLOWANCE', 80000, 'ALLOWANCE', 100000, 'DEDUCTION', 30000, 'DEDUCTION', 1000
            )
        """.trimIndent())

        // Worker 2: ساعتی با کسورات سنگین و روز غیبت (بدهکار در روز غیبت)
        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `name`, `role`, `phone`, `nationalId`, `baseDailyWage`, `baseHourlyWage`,
                `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`, `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`,
                `isActive`, `notes`, `colorTag`, `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES (
                102, 1, 'کارگر دوم', 'جوشکار', '09122222222', '0022222222', 0, 180000,
                1, 180000, 6.0, 1, 220000, 1.5, 1, '', 16086790,
                40000, 'DEDUCTION', 60000, 'ALLOWANCE', 90000, 'DEDUCTION', 20000, 'ALLOWANCE', 1000
            )
        """.trimIndent())

        // Worker 3: روزمزد با تمام روز، نصف روز و غیبت
        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `name`, `role`, `phone`, `nationalId`, `baseDailyWage`, `baseHourlyWage`,
                `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`, `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`,
                `isActive`, `notes`, `colorTag`, `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES (
                103, 1, 'کارگر سوم', 'کارگر ساده', '09123333333', '0033333333', 900000, 0,
                0, 0, 0.0, 0, 0, 0.0, 1, '', 16086790,
                35000, 'ALLOWANCE', 45000, 'DEDUCTION', 0, 'ALLOWANCE', 15000, 'ALLOWANCE', 1000
            )
        """.trimIndent())

        // Attendance rows in v6 (no allowance columns in attendance table yet):
        // Day 1: Worker 1 FULL_DAY, Worker 2 HOURLY, Worker 3 HALF_DAY
        // Day 2: Worker 1 ABSENT, Worker 2 ABSENT, Worker 3 FULL_DAY
        // Day 3: Worker 1 HALF_DAY, Worker 2 HOURLY, Worker 3 ABSENT
        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `dateFolderId`, `date`, `epochDay`, `status`, `timestamp`,
                `entryTime`, `exitTime`, `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`,
                `hourlyHours`, `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES
                (1, 1, 101, 10, '1403/04/01', 200, 'FULL_DAY', 1000, '08:00', '17:00', 8.0, 2.0, 150000, 0, 0.0, 1200000, 0, 'پروژه', '', '', ''),
                (2, 1, 102, 10, '1403/04/01', 200, 'HOURLY', 1000, '08:00', '14:00', 0.0, 1.5, 220000, 180000, 6.0, 0, 180000, 'پروژه', '', '', ''),
                (3, 1, 103, 10, '1403/04/01', 200, 'HALF_DAY', 1000, '08:00', '12:00', 4.0, 0.0, 0, 0, 0.0, 450000, 0, 'پروژه', '', '', ''),
                (4, 1, 101, 20, '1403/04/02', 201, 'ABSENT', 2000, '08:00', '17:00', 0.0, 0.0, 0, 0, 0.0, 0, 0, 'پروژه', '', '', ''),
                (5, 1, 102, 20, '1403/04/02', 201, 'ABSENT', 2000, '08:00', '17:00', 0.0, 0.0, 0, 0, 0.0, 0, 0, 'پروژه', '', '', ''),
                (6, 1, 103, 20, '1403/04/02', 201, 'FULL_DAY', 2000, '08:00', '17:00', 8.0, 0.0, 0, 0, 0.0, 900000, 0, 'پروژه', '', '', ''),
                (7, 1, 101, 30, '1403/04/03', 202, 'HALF_DAY', 3000, '08:00', '12:00', 4.0, 1.0, 150000, 0, 0.0, 600000, 0, 'پروژه', '', '', ''),
                (8, 1, 102, 30, '1403/04/03', 202, 'HOURLY', 3000, '08:00', '13:00', 0.0, 0.0, 220000, 180000, 5.0, 0, 180000, 'پروژه', '', '', ''),
                (9, 1, 103, 30, '1403/04/03', 202, 'ABSENT', 3000, '08:00', '17:00', 0.0, 0.0, 0, 0, 0.0, 0, 0, 'پروژه', '', '', '')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `expenses` (
                `id`, `folderId`, `title`, `category`, `scope`, `impactType`, `workerId`, `workerName`,
                `amount`, `accommodationDays`, `date`, `epochDay`, `timestamp`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES
                (1, 1, 'نهار کارگاه', 'FOOD', 'GROUP', 'DEDUCTION', NULL, NULL, 180000, 0, '1403/04/01', 200, 1000, 'پروژه', '', '', ''),
                (2, 1, 'پاداش کارگر اول', 'OTHER', 'INDIVIDUAL', 'ALLOWANCE', 101, 'کارگر اول', 75000, 0, '1403/04/01', 200, 1000, 'پروژه', '', '', '')
        """.trimIndent())

        val workersBefore = readWorkers(db)
        val attendancesBefore = readAttendancesV6(db)
        val expensesBefore = readExpenses(db)

        val summaryBefore = com.karyar.app.util.WageCalculator.calculateFinancialSummary(
            workers = workersBefore,
            attendances = attendancesBefore,
            expenses = expensesBefore
        )

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB + "-6to7-financial", 7, true, AppDatabase.MIGRATION_6_7)

        val workersAfter = readWorkers(db)
        val attendancesAfter = readAttendancesV7(db)
        val expensesAfter = readExpenses(db)
        db.close()

        // Verify all 9 attendance rows now have hasCustomAllowances = true and copied worker allowances
        assertEquals(9, attendancesAfter.size)
        for (att in attendancesAfter) {
            org.junit.Assert.assertTrue("Row ${att.id} must have hasCustomAllowances=true after MIGRATION_6_7", att.hasCustomAllowances)
            val w = workersAfter.first { it.id == att.workerId }
            assertEquals(w.transitAllowance, att.transitAllowance)
            assertEquals(w.transitImpact, att.transitImpact)
            assertEquals(w.foodAllowance, att.foodAllowance)
            assertEquals(w.foodImpact, att.foodImpact)
            assertEquals(w.accommodationAllowance, att.accommodationAllowance)
            assertEquals(w.accommodationImpact, att.accommodationImpact)
            assertEquals(w.medicalAllowance, att.medicalAllowance)
            assertEquals(w.medicalImpact, att.medicalImpact)
        }

        val summaryAfter = com.karyar.app.util.WageCalculator.calculateFinancialSummary(
            workers = workersAfter,
            attendances = attendancesAfter,
            expenses = expensesAfter
        )

        // Verify total project financials before and after migration are 100% identical
        assertEquals(summaryBefore.grandTotalProjectCost, summaryAfter.grandTotalProjectCost)
        assertEquals(summaryBefore.totalWorkerDebt, summaryAfter.totalWorkerDebt)
        assertEquals(summaryBefore.totalBaseWagesPaid, summaryAfter.totalBaseWagesPaid)
        assertEquals(summaryBefore.totalHourlyPaid, summaryAfter.totalHourlyPaid)
        assertEquals(summaryBefore.totalOvertimePaid, summaryAfter.totalOvertimePaid)
        assertEquals(summaryBefore.grandTotalExpenses, summaryAfter.grandTotalExpenses)

        // Verify each worker's financial breakdown before and after migration is 100% identical
        for (perfBefore in summaryBefore.workerPerformances) {
            val perfAfter = summaryAfter.workerPerformances.first { it.worker.id == perfBefore.worker.id }
            assertEquals(perfBefore.baseWageTotal, perfAfter.baseWageTotal)
            assertEquals(perfBefore.hourlyPayTotal, perfAfter.hourlyPayTotal)
            assertEquals(perfBefore.overtimePayTotal, perfAfter.overtimePayTotal)
            assertEquals(perfBefore.totalAllowances, perfAfter.totalAllowances)
            assertEquals(perfBefore.totalDeductions, perfAfter.totalDeductions)
            assertEquals(perfBefore.transitAllowanceTotal, perfAfter.transitAllowanceTotal)
            assertEquals(perfBefore.transitDeductionTotal, perfAfter.transitDeductionTotal)
            assertEquals(perfBefore.foodAllowanceTotal, perfAfter.foodAllowanceTotal)
            assertEquals(perfBefore.foodDeductionTotal, perfAfter.foodDeductionTotal)
            assertEquals(perfBefore.accommodationAllowanceTotal, perfAfter.accommodationAllowanceTotal)
            assertEquals(perfBefore.accommodationDeductionTotal, perfAfter.accommodationDeductionTotal)
            assertEquals(perfBefore.medicalAllowanceTotal, perfAfter.medicalAllowanceTotal)
            assertEquals(perfBefore.medicalDeductionTotal, perfAfter.medicalDeductionTotal)
            assertEquals(perfBefore.groupExpenseShare, perfAfter.groupExpenseShare)
            assertEquals(perfBefore.netPayout, perfAfter.netPayout)
            assertEquals(perfBefore.debtAmount, perfAfter.debtAmount)
        }
    }

    @Test
    fun migrate5To7_chainedMigrationPreservesDataAndAppliesV7Schema() {
        var db = helper.createDatabase(TEST_DB + "-5to7", 5)

        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (1, 'پروژه زنجیره‌ای ۵ به ۷', 'سرکارگر', 'کارفرما', 16086790, '1403/03/01', '')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`)
            VALUES (10, 1, '1403/03/01', 'سه‌شنبه', 'روز اول', '', 1000)
        """.trimIndent())

        insertWorkerV5(db, 201L, 1L, "حسن کریمی", role = "جوشکار")
        insertWorkerV5(db, 202L, 1L, "حسن کریمی", role = "برقکار")

        insertAttendanceV5(db, 1L, 1L, 201L, "1403/03/01", timestamp = 1000L, notes = "تمام روز", dailyWage = 1200000L)
        insertAttendanceV5(db, 2L, 1L, 201L, "1403/03/01", timestamp = 2000L, notes = "غیبت", dailyWage = 1200000L)
        insertAttendanceV5(db, 3L, 1L, 202L, "1403/03/01", timestamp = 1500L, notes = "نصف روز", regularHours = 4.0, dailyWage = 1400000L)

        insertExpenseV5(db, 10L, 1L, "صبحانه کارگاه", "FOOD", "GROUP", 150000L, "1403/03/01", 1000L)

        db.close()

        db = helper.runMigrationsAndValidate(
            TEST_DB + "-5to7",
            7,
            true,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        )

        val wCursor = db.query("SELECT id, name, role FROM workers ORDER BY id ASC")
        val workers = mutableListOf<Triple<Long, String, String>>()
        while (wCursor.moveToNext()) {
            workers.add(Triple(wCursor.getLong(0), wCursor.getString(1), wCursor.getString(2)))
        }
        wCursor.close()
        assertEquals(2, workers.size)
        assertEquals(201L, workers[0].first)
        assertEquals(202L, workers[1].first)

        val attCursor = db.query("""
            SELECT id, workerId, status, notes, hasCustomAllowances, transitImpact
            FROM attendance ORDER BY workerId ASC
        """.trimIndent())
        val attendances = mutableListOf< Pair<Long, String> >()
        while (attCursor.moveToNext()) {
            attendances.add(attCursor.getLong(1) to attCursor.getString(2))
            assertEquals("", attCursor.getString(3))
            assertEquals(1, attCursor.getInt(4))
            assertEquals("ALLOWANCE", attCursor.getString(5))
        }
        attCursor.close()

        assertEquals(listOf(201L to "ABSENT", 202L to "HALF_DAY"), attendances)
        db.close()
    }

    @Test
    fun migrate5To6_logsExpectedDuplicateAndOrphanDeletedCounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppDatabase.clearMigrationReport(context)
        org.robolectric.shadows.ShadowLog.clear()
        var db = helper.createDatabase(TEST_DB + "-log-counts", 5)

        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (1, 'پروژه لاگ', '', '', 0, '1403/01/01', '')
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`)
            VALUES (10, 1, '1403/01/01', 'شنبه', '', '', 1000)
        """.trimIndent())

        insertWorkerV5(db, 1L, 1L, "کارگر معتبر")

        // 1 valid attendance + 2 duplicates for worker 1 on 1403/01/01 (total 2 duplicates deleted)
        insertAttendanceV5(db, 1L, 1L, 1L, "1403/01/01", timestamp = 1000L, dailyWage = 1_100_000L, hourlyWage = 150_000L, overtimeRate = 200_000L)
        insertAttendanceV5(db, 2L, 1L, 1L, "1403/01/01", timestamp = 2000L, dailyWage = 1_200_000L, hourlyWage = 160_000L, overtimeRate = 210_000L)
        insertAttendanceV5(db, 3L, 1L, 1L, "1403/01/01", timestamp = 3000L, dailyWage = 1_300_000L, hourlyWage = 170_000L, overtimeRate = 220_000L)

        // 2 orphan attendance rows: workerId 999 does not exist, folderId 888 does not exist
        insertAttendanceV5(db, 4L, 1L, 999L, "1403/01/01", timestamp = 1000L, dailyWage = 900_000L)
        insertAttendanceV5(db, 5L, 888L, 1L, "1403/01/02", timestamp = 1000L, dailyWage = 950_000L)

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB + "-log-counts", 6, true, AppDatabase.MIGRATION_5_6)

        val remainingCursor = db.query("SELECT id FROM attendance")
        val remainingIds = mutableListOf<Long>()
        while (remainingCursor.moveToNext()) {
            remainingIds.add(remainingCursor.getLong(0))
        }
        remainingCursor.close()
        assertEquals("Only the latest valid attendance row (id=3) should remain", listOf(3L), remainingIds)
        db.close()

        val logs = org.robolectric.shadows.ShadowLog.getLogsForTag("AppDatabase").map { it.msg }
        org.junit.Assert.assertTrue(
            "Expected duplicate deletion count (2) to be logged in AppDatabase logs, actual: $logs",
            logs.any { it.contains("MIGRATION_5_6: Removed 2 duplicate attendance row(s).") }
        )
        org.junit.Assert.assertTrue(
            "Expected orphan deletion count (2) to be logged in AppDatabase logs, actual: $logs",
            logs.any { it.contains("MIGRATION_5_6: Removed 2 orphan row(s) violating foreign keys (attendance: 2).") }
        )

        // Verify SharedPreferences("migration_report") has counts, breakdown, and Persian dialog message
        val prefs = context.getSharedPreferences(AppDatabase.MIGRATION_PREFS_NAME, android.content.Context.MODE_PRIVATE)
        org.junit.Assert.assertTrue(prefs.getBoolean(AppDatabase.KEY_HAS_PENDING_REPORT, false))
        assertEquals(2, prefs.getInt(AppDatabase.KEY_DELETED_DUPLICATES_COUNT, -1))
        assertEquals(2, prefs.getInt(AppDatabase.KEY_DELETED_ORPHANS_COUNT, -1))
        assertEquals("attendance: 2", prefs.getString(AppDatabase.KEY_ORPHAN_BREAKDOWN, ""))
        assertEquals(
            "در به‌روزرسانی داده‌ها ۲ رکورد حضور تکراری و ۲ رکورد ناسازگار حذف شد",
            prefs.getString(AppDatabase.KEY_REPORT_MESSAGE, "")
        )

        val pendingReport = AppDatabase.getPendingMigrationReport(context)
        org.junit.Assert.assertNotNull(pendingReport)
        assertEquals(2, pendingReport!!.deletedDuplicatesCount)
        assertEquals(2, pendingReport.deletedOrphansCount)
        assertEquals(
            "در به‌روزرسانی داده‌ها ۲ رکورد حضور تکراری و ۲ رکورد ناسازگار حذف شد",
            pendingReport.formatDialogMessage()
        )

        // Verify pre-deletion backup file in db_backups contains id, workerId, date, and wage amounts
        val backupPath = pendingReport.backupFilePath
        org.junit.Assert.assertFalse("Backup path should be recorded in migration report", backupPath.isNullOrBlank())
        val backupFile = java.io.File(backupPath!!)
        org.junit.Assert.assertTrue("Backup file must exist inside db_backups", backupFile.exists())
        assertEquals("db_backups", backupFile.parentFile?.name)

        val jsonRoot = org.json.JSONObject(backupFile.readText(Charsets.UTF_8))
        assertEquals(2, jsonRoot.getInt("deletedDuplicatesCount"))
        assertEquals(2, jsonRoot.getInt("deletedOrphansCount"))
        val rowsArray = jsonRoot.getJSONArray("deletedRows")
        assertEquals(4, rowsArray.length())

        val rowById = (0 until rowsArray.length())
            .map { rowsArray.getJSONObject(it) }
            .associateBy { it.getLong("id") }

        assertEquals(setOf(1L, 2L, 4L, 5L), rowById.keys)
        // Check duplicate row 1
        assertEquals("DUPLICATE_ATTENDANCE", rowById[1L]!!.getString("reason"))
        assertEquals(1L, rowById[1L]!!.getLong("workerId"))
        assertEquals("1403/01/01", rowById[1L]!!.getString("date"))
        assertEquals(1_100_000L, rowById[1L]!!.getLong("dailyWage"))
        assertEquals(150_000L, rowById[1L]!!.getLong("hourlyWage"))
        assertEquals(200_000L, rowById[1L]!!.getLong("overtimeRate"))

        // Check duplicate row 2
        assertEquals("DUPLICATE_ATTENDANCE", rowById[2L]!!.getString("reason"))
        assertEquals(1L, rowById[2L]!!.getLong("workerId"))
        assertEquals("1403/01/01", rowById[2L]!!.getString("date"))
        assertEquals(1_200_000L, rowById[2L]!!.getLong("dailyWage"))

        // Check orphan rows 4 and 5
        assertEquals("ORPHAN_FOREIGN_KEY", rowById[4L]!!.getString("reason"))
        assertEquals(999L, rowById[4L]!!.getLong("workerId"))
        assertEquals("1403/01/01", rowById[4L]!!.getString("date"))
        assertEquals(900_000L, rowById[4L]!!.getLong("dailyWage"))

        assertEquals("ORPHAN_FOREIGN_KEY", rowById[5L]!!.getString("reason"))
        assertEquals(1L, rowById[5L]!!.getLong("workerId"))
        assertEquals("1403/01/02", rowById[5L]!!.getString("date"))
        assertEquals(950_000L, rowById[5L]!!.getLong("dailyWage"))

        // Confirming dialog clears migration_report from SharedPreferences
        AppDatabase.clearMigrationReport(context)
        org.junit.Assert.assertNull(AppDatabase.getPendingMigrationReport(context))
        org.junit.Assert.assertFalse(prefs.getBoolean(AppDatabase.KEY_HAS_PENDING_REPORT, false))

        backupFile.delete()
    }

    private fun resetDatabaseInstance() {
        val instanceField = AppDatabase::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        (instanceField.get(null) as? AppDatabase)?.close()
        instanceField.set(null, null)
    }

    @Test
    fun getDatabase_createsBackupWhenUserVersionIsLowerAndKeepsOnlyLast3Backups() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backupDir = java.io.File(context.filesDir, "db_backups")
        backupDir.deleteRecursively()

        val dbFile = context.getDatabasePath("worker_management_db")
        dbFile.parentFile?.mkdirs()

        try {
            // Simulate 4 upgrades from older versions (v3, v4, v5, v6) before calling getDatabase
            for (oldVersion in listOf(3, 4, 5, 6)) {
                resetDatabaseInstance()
                if (dbFile.exists()) dbFile.delete()
                java.io.File(dbFile.path + "-wal").delete()
                java.io.File(dbFile.path + "-shm").delete()

                val sqliteDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
                sqliteDb.execSQL("PRAGMA user_version = $oldVersion")
                sqliteDb.close()

                // Trigger pre-migration backup via getDatabase
                val roomDb = AppDatabase.getDatabase(context)
                roomDb.close()
                resetDatabaseInstance()
            }

            val backups = backupDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()
            assertEquals("Only the last 3 backups must be kept", 3, backups.size)
            org.junit.Assert.assertTrue("Oldest backup (v3) should be pruned", backups.none { it.name.contains("_v3_") })
            org.junit.Assert.assertTrue("Backup for v4 should exist", backups.any { it.name.contains("_v4_") })
            org.junit.Assert.assertTrue("Backup for v5 should exist", backups.any { it.name.contains("_v5_") })
            org.junit.Assert.assertTrue("Backup for v6 should exist", backups.any { it.name.contains("_v6_") })

            // Verify that when user_version == CURRENT_DB_VERSION (7), no additional backup is created
            if (dbFile.exists()) dbFile.delete()
            val currentSqliteDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            currentSqliteDb.execSQL("PRAGMA user_version = ${AppDatabase.CURRENT_DB_VERSION}")
            currentSqliteDb.close()

            val beforeNames = backupDir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
            val roomDbCurrent = AppDatabase.getDatabase(context)
            roomDbCurrent.close()
            resetDatabaseInstance()
            val afterNames = backupDir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
            assertEquals("No new backup should be created when DB is already at current version", beforeNames, afterNames)

            // Verify that if backup fails (e.g. db_backups is a file blocking directory creation), getDatabase does not crash
            backupDir.deleteRecursively()
            backupDir.writeText("not-a-directory")
            if (dbFile.exists()) dbFile.delete()
            val v6SqliteDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            v6SqliteDb.execSQL("PRAGMA user_version = 6")
            v6SqliteDb.close()

            val safeDb = AppDatabase.getDatabase(context)
            org.junit.Assert.assertNotNull("getDatabase must succeed even if backup fails", safeDb)
            safeDb.close()
        } finally {
            resetDatabaseInstance()
            backupDir.deleteRecursively()
            if (dbFile.exists()) dbFile.delete()
            java.io.File(dbFile.path + "-wal").delete()
            java.io.File(dbFile.path + "-shm").delete()
        }
    }

    @Test
    fun migrate7To8_recalculatesEpochDayForDateFoldersAttendanceAndExpenses() {
        var db = helper.createDatabase(TEST_DB + "-v7-to-v8", 7)

        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (1, 'پروژه تست مایگریشن ۸', 'سرکارگر', 'کارفرما', 16086790, '1403/12/30', '')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `name`, `role`, `phone`, `nationalId`, `baseDailyWage`, `baseHourlyWage`,
                `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`, `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`,
                `isActive`, `notes`, `colorTag`, `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES (
                10, 1, 'کارگر اول', 'بنا', '', '', 1000000, 0,
                0, 0, 0.0, 0, 0, 0.0,
                1, '', 16086790, 0, 'ALLOWANCE', 0, 'ALLOWANCE',
                0, 'ALLOWANCE', 0, 'ALLOWANCE', 1000
            )
        """.trimIndent())

        // Insert rows with intentionally wrong legacy epochDay values (e.g. 0 or off-by-one from old 33-year rule)
        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`, `epochDay`)
            VALUES
                (101, 1, '1403/12/30', 'پنج‌شنبه', 'آخر سال ۱۴۰۳', '', 1000, 12345),
                (102, 1, '1404/01/01', 'جمعه', 'نوروز ۱۴۰۴', '', 2000, 12346),
                (103, 1, '1405/01/01', 'شنبه', 'نوروز ۱۴۰۵', '', 3000, 99999)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `dateFolderId`, `date`, `epochDay`, `status`, `timestamp`,
                `entryTime`, `exitTime`, `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`,
                `hourlyHours`, `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`,
                `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`, `accommodationAllowance`,
                `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `hasCustomAllowances`
            ) VALUES
                (201, 1, 10, 101, '1403/12/30', 0, 'FULL_DAY', 1000, '08:00', '17:00', 8.0, 0.0, 0, 0, 0.0, 1000000, 0, '', '', '', '', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1),
                (202, 1, 10, 102, '1404/01/01', 111, 'FULL_DAY', 2000, '08:00', '17:00', 8.0, 0.0, 0, 0, 0.0, 1000000, 0, '', '', '', '', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1),
                (203, 1, 10, 103, '1405/01/01', 222, 'FULL_DAY', 3000, '08:00', '17:00', 8.0, 0.0, 0, 0, 0.0, 1000000, 0, '', '', '', '', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `expenses` (
                `id`, `folderId`, `title`, `category`, `scope`, `impactType`, `workerId`, `workerName`,
                `amount`, `accommodationDays`, `date`, `epochDay`, `timestamp`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES
                (301, 1, 'هزینه ناهار', 'FOOD', 'GROUP', 'DEDUCTION', NULL, NULL, 250000, 0, '1403/12/30', -1, 1000, '', '', '', ''),
                (302, 1, 'هزینه ایاب و ذهاب', 'TRANSIT', 'INDIVIDUAL', 'ALLOWANCE', 10, 'کارگر اول', 50000, 0, '1405/01/01', 0, 2000, '', '', '', '')
        """.trimIndent())

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB + "-v7-to-v8", 8, true, AppDatabase.MIGRATION_7_8)

        val expected14031230 = com.karyar.app.util.JalaliCalendar.toEpochDay("1403/12/30")
        val expected14040101 = com.karyar.app.util.JalaliCalendar.toEpochDay("1404/01/01")
        val expected14050101 = com.karyar.app.util.JalaliCalendar.toEpochDay("1405/01/01")

        // Verify date_folders epochDays
        db.query("SELECT `id`, `epochDay` FROM `date_folders` ORDER BY `id` ASC").use { c ->
            val actual = mutableMapOf<Long, Long>()
            while (c.moveToNext()) {
                actual[c.getLong(0)] = c.getLong(1)
            }
            assertEquals(expected14031230, actual[101L])
            assertEquals(expected14040101, actual[102L])
            assertEquals(expected14050101, actual[103L])
        }

        // Verify attendance epochDays
        db.query("SELECT `id`, `epochDay` FROM `attendance` ORDER BY `id` ASC").use { c ->
            val actual = mutableMapOf<Long, Long>()
            while (c.moveToNext()) {
                actual[c.getLong(0)] = c.getLong(1)
            }
            assertEquals(expected14031230, actual[201L])
            assertEquals(expected14040101, actual[202L])
            assertEquals(expected14050101, actual[203L])
        }

        // Verify expenses epochDays
        db.query("SELECT `id`, `epochDay` FROM `expenses` ORDER BY `id` ASC").use { c ->
            val actual = mutableMapOf<Long, Long>()
            while (c.moveToNext()) {
                actual[c.getLong(0)] = c.getLong(1)
            }
            assertEquals(expected14031230, actual[301L])
            assertEquals(expected14050101, actual[302L])
        }

        db.close()
    }

    @Test
    fun migrate7To9_repairsLegacyInvalidDate1404_12_30_handlesCollisionsAndSortingDoesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val backupDir = java.io.File(context.filesDir, AppDatabase.BACKUP_DIR_NAME)
        backupDir.deleteRecursively()
        AppDatabase.saveMigrationReport(0, 0, "", null, context)

        var db = helper.createDatabase(TEST_DB + "-v7-to-v9", 7)

        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES
                (1, 'پروژه تست برخورد تاریخ ۱۴۰۴/۱۲/۳۰', 'سرکارگر', 'کارفرما', 16086790, '1404/12/01', ''),
                (2, 'پروژه تست بدون برخورد ۱۴۰۴/۱۲/۳۰', 'سرکارگر ۲', 'کارفرما ۲', 16086790, '1404/12/01', '')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `name`, `role`, `phone`, `nationalId`, `baseDailyWage`, `baseHourlyWage`,
                `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`, `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`,
                `isActive`, `notes`, `colorTag`, `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES
                (10, 1, 'کارگر بدون تداخل', 'بنا', '', '', 1000000, 0, 0, 0, 0.0, 0, 0, 0.0, 1, '', 16086790, 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1000),
                (20, 1, 'کارگر دارای تداخل', 'جوشکار', '', '', 1200000, 0, 0, 0, 0.0, 0, 0, 0.0, 1, '', 16086790, 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1000),
                (30, 2, 'کارگر پروژه دوم', 'آرماتوربند', '', '', 900000, 0, 0, 0, 0.0, 0, 0, 0.0, 1, '', 16086790, 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1000)
        """.trimIndent())

        // Folder 1 has both 1404/12/30 (invalid) and 1405/01/01 (valid) -> collision in date_folders
        // Folder 2 has 1404/12/30 (invalid, no 1405/01/01) and 1404/07/31 (invalid 31st day in 30-day month)
        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`, `epochDay`)
            VALUES
                (101, 1, '1404/12/29', 'پنج‌شنبه', 'آخر اسفند', '', 1000, 0),
                (102, 1, '1404/12/30', 'جمعه', 'روز نامعتبر قدیم', 'یادداشت ادغام‌شده', 2000, 0),
                (103, 1, '1405/01/01', 'جمعه', 'اول فروردین ۱۴۰۵', '', 3000, 0),
                (201, 2, '1404/12/30', 'جمعه', 'انتقال به ۱۴۰۵/۰۱/۰۱', '', 4000, 0),
                (202, 2, '1404/07/31', 'پنج‌شنبه', 'مهر ۳۱ روزه نامعتبر', '', 5000, 0)
        """.trimIndent())

        // Worker 10 has 1404/12/30 only (no collision -> moves to 1405/01/01)
        // Worker 20 has both 1404/12/30 and 1405/01/01 (collision -> 1405/01/01 kept, 1404/12/30 backed up & deleted)
        // Worker 30 has 1404/12/30 and 1404/07/31 (moves to 1405/01/01 and 1404/07/30)
        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `dateFolderId`, `date`, `epochDay`, `status`, `timestamp`,
                `entryTime`, `exitTime`, `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`,
                `hourlyHours`, `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`,
                `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`, `accommodationAllowance`,
                `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `hasCustomAllowances`
            ) VALUES
                (501, 1, 10, 102, '1404/12/30', 0, 'FULL_DAY', 1000, '08:00', '17:00', 8.0, 0.0, 0, 0, 0.0, 1000000, 0, '', '', '', 'انتقال مستقیم', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1),
                (502, 1, 20, 102, '1404/12/30', 0, 'HALF_DAY', 2000, '08:00', '12:00', 4.0, 0.0, 0, 0, 0.0, 600000, 0, '', '', '', 'رکورد برخوردی قدیمی', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1),
                (503, 1, 20, 103, '1405/01/01', 0, 'FULL_DAY', 3000, '08:00', '17:00', 8.0, 0.0, 0, 0, 0.0, 1200000, 0, '', '', '', 'رکورد مقصد اصلی', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1),
                (504, 2, 30, 201, '1404/12/30', 0, 'FULL_DAY', 4000, '08:00', '17:00', 8.0, 0.0, 0, 0, 0.0, 900000, 0, '', '', '', '', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1),
                (505, 2, 30, 202, '1404/07/31', 0, 'FULL_DAY', 5000, '08:00', '17:00', 8.0, 0.0, 0, 0, 0.0, 900000, 0, '', '', '', '', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `expenses` (
                `id`, `folderId`, `title`, `category`, `scope`, `impactType`, `workerId`, `workerName`,
                `amount`, `accommodationDays`, `date`, `epochDay`, `timestamp`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES
                (601, 1, 'هزینه اسفند نامعتبر', 'FOOD', 'GROUP', 'DEDUCTION', NULL, NULL, 300000, 0, '1404/12/30', 0, 1000, '', '', '', '')
        """.trimIndent())

        db.close()

        db = helper.runMigrationsAndValidate(
            TEST_DB + "-v7-to-v9",
            9,
            true,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9
        )

        val epoch14050101 = com.karyar.app.util.JalaliCalendar.toEpochDay("1405/01/01")
        val epoch14040730 = com.karyar.app.util.JalaliCalendar.toEpochDay("1404/07/30")

        // 1. Verify all date_folders have valid dates & accurate epochDays, and sorting does not crash
        val migratedDateFolders = mutableListOf<com.karyar.app.data.local.entity.DateFolderEntity>()
        db.query("SELECT `id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`, `epochDay` FROM `date_folders` ORDER BY `id` ASC").use { c ->
            while (c.moveToNext()) {
                val dateStr = c.getString(2)
                org.junit.Assert.assertNotNull(
                    "date_folders date '$dateStr' must be valid after MIGRATION_8_9",
                    com.karyar.app.util.JalaliCalendar.parseDateOrNull(dateStr)
                )
                migratedDateFolders.add(
                    com.karyar.app.data.local.entity.DateFolderEntity(
                        id = c.getLong(0),
                        folderId = c.getLong(1),
                        date = dateStr,
                        dayOfWeek = c.getString(3),
                        title = c.getString(4),
                        notes = c.getString(5),
                        createdAt = c.getLong(6),
                        epochDay = c.getLong(7)
                    )
                )
            }
        }
        // 102 collided with 103 and was merged into 103; 201 -> 1405/01/01; 202 -> 1404/07/30
        assertEquals(4, migratedDateFolders.size)
        val df103 = migratedDateFolders.first { it.id == 103L }
        assertEquals("1405/01/01", df103.date)
        assertEquals(epoch14050101, df103.epochDay)
        assertEquals("یادداشت ادغام‌شده", df103.notes)

        val df201 = migratedDateFolders.first { it.id == 201L }
        assertEquals("1405/01/01", df201.date)
        assertEquals(epoch14050101, df201.epochDay)

        val df202 = migratedDateFolders.first { it.id == 202L }
        assertEquals("1404/07/30", df202.date)
        assertEquals(epoch14040730, df202.epochDay)

        // Verify AttendanceScreen / WorkersScreen sorting by epochDay works without crashing
        val sortedFolders = migratedDateFolders.sortedWith(compareBy({ it.epochDay }, { it.id }))
        assertEquals(4, sortedFolders.size)

        // 2. Verify attendance rows: 501 moved to 1405/01/01; 502 collided with 503 and was backed up; 503 preserved; 504 -> 1405/01/01; 505 -> 1404/07/30
        val migratedAttendances = readAttendancesV7(db)
        assertEquals(4, migratedAttendances.size)
        for (att in migratedAttendances) {
            org.junit.Assert.assertNotNull(
                "attendance date '${att.date}' must be valid after MIGRATION_8_9",
                com.karyar.app.util.JalaliCalendar.parseDateOrNull(att.date)
            )
            assertEquals(com.karyar.app.util.JalaliCalendar.toEpochDay(att.date), att.epochDay)
        }
        val att501 = migratedAttendances.first { it.id == 501L }
        assertEquals("1405/01/01", att501.date)
        assertEquals(103L, att501.dateFolderId)

        val att503 = migratedAttendances.first { it.id == 503L }
        assertEquals("1405/01/01", att503.date)
        assertEquals(1200000L, att503.dailyWage)

        val att504 = migratedAttendances.first { it.id == 504L }
        assertEquals("1405/01/01", att504.date)

        val att505 = migratedAttendances.first { it.id == 505L }
        assertEquals("1404/07/30", att505.date)

        // 3. Verify expenses: 601 moved from 1404/12/30 to 1405/01/01
        val migratedExpenses = readExpenses(db)
        assertEquals(1, migratedExpenses.size)
        assertEquals("1405/01/01", migratedExpenses.first().date)
        assertEquals(epoch14050101, migratedExpenses.first().epochDay)

        // 4. Verify colliding rows (date_folder 102 and attendance 502) were backed up to JSON and reported
        val report = AppDatabase.migrationReportFlow.value
        org.junit.Assert.assertNotNull("MigrationReport should be emitted when colliding rows are merged", report)
        assertEquals(2, report!!.deletedDuplicatesCount)
        org.junit.Assert.assertNotNull(report.backupFilePath)
        val backupJsonFile = java.io.File(report.backupFilePath!!)
        org.junit.Assert.assertTrue("Backup JSON file must exist", backupJsonFile.exists())
        val jsonRoot = org.json.JSONObject(backupJsonFile.readText(Charsets.UTF_8))
        assertEquals(2, jsonRoot.getInt("deletedDuplicatesCount"))
        val rowsArray = jsonRoot.getJSONArray("deletedRows")
        assertEquals(2, rowsArray.length())
        val reasons = (0 until rowsArray.length()).map { rowsArray.getJSONObject(it).getString("reason") }.toSet()
        val dates = (0 until rowsArray.length()).map { rowsArray.getJSONObject(it).getString("date") }.toSet()
        org.junit.Assert.assertTrue(reasons.contains("DUPLICATE_ATTENDANCE_ON_INVALID_DATE_FIX"))
        org.junit.Assert.assertTrue(reasons.contains("DUPLICATE_DATE_FOLDER_ON_INVALID_DATE_FIX"))
        org.junit.Assert.assertTrue(dates.contains("1404/12/30"))

        db.close()
        backupDir.deleteRecursively()
        AppDatabase.saveMigrationReport(0, 0, "", null, context)
    }
}
