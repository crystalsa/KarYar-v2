package com.karyar.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
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

        db.close()
    }
}
