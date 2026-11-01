package com.karyar.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.ui.WorkerViewModel
import com.karyar.app.util.BackupUtil
import com.karyar.app.util.WageCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRestoreTest {

    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var viewModel: WorkerViewModel

    @Before
    fun setUp() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        db = AppDatabase.getDatabase(app)
        db.attendanceDao().clearAll()
        db.expenseDao().clearAll()
        db.workerDao().clearAll()
        db.dateFolderDao().clearAll()
        db.folderDao().clearAll()
        BackupUtil.getLocalBackupsDir(app).deleteRecursively()
        viewModel = WorkerViewModel(app, kotlinx.coroutines.flow.flowOf(emptyList()))
    }

    @org.junit.After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.stopCollectorsAndReceivers()
        }
    }

    @Test
    fun fullBackupAndRestore_preservesAllTablesAndFinancialCalculationsExact() = runBlocking {
        // ۱. ایجاد دو پروژه با روزهای کاری، کارگران، ترددها و هزینه‌ها
        val f1Id = db.folderDao().insertFolder(
            WorkplaceFolderEntity(
                name = "پروژه برج میلاد نو",
                foremanName = "استاد رحیمی",
                employerName = "شرکت عمران سازه",
                colorTag = 0xFFD97706L,
                createdAt = "1404/02/01",
                notes = "فاز اول اسکلت بتنی"
            )
        )
        val f2Id = db.folderDao().insertFolder(
            WorkplaceFolderEntity(
                name = "پروژه مجتمع تجاری پارس",
                foremanName = "مهندس کاظمی",
                employerName = "گروه سرمایه‌گذاری پارس",
                colorTag = 0xFF10B981L,
                createdAt = "1404/02/05",
                notes = "فاز نازک‌کاری"
            )
        )

        val df1Id = db.dateFolderDao().insertDateFolder(
            DateFolderEntity(
                folderId = f1Id,
                date = "1404/02/10",
                dayOfWeek = "چهارشنبه",
                title = "روز اول بتن‌ریزی",
                notes = "هوا آفتابی"
            )
        )
        val df2Id = db.dateFolderDao().insertDateFolder(
            DateFolderEntity(
                folderId = f2Id,
                date = "1404/02/11",
                dayOfWeek = "پنج‌شنبه",
                title = "گچ‌کاری طبقه دوم",
                notes = ""
            )
        )

        val w1Id = db.workerDao().insertWorker(
            WorkerEntity(
                folderId = f1Id,
                name = "کریم منصوری",
                role = "آرماتوربند",
                phone = "09121234567",
                nationalId = "0013542419",
                baseDailyWage = 1_500_000L,
                isOvertimeEnabled = true,
                overtimeRate = 200_000L,
                overtimeHours = 2.0,
                transitAllowance = 50_000L,
                transitImpact = "ALLOWANCE",
                foodAllowance = 80_000L,
                foodImpact = "ALLOWANCE",
                accommodationAllowance = 40_000L,
                accommodationImpact = "DEDUCTION"
            )
        )
        val w2Id = db.workerDao().insertWorker(
            WorkerEntity(
                folderId = f2Id,
                name = "جواد حیدری",
                role = "گچ‌کار",
                phone = "09351112233",
                baseHourlyWage = 180_000L,
                isHourlyEnabled = true,
                hourlyWageRate = 180_000L,
                hourlyHours = 6.5
            )
        )

        db.attendanceDao().insertAttendance(
            AttendanceEntity(
                folderId = f1Id,
                workerId = w1Id,
                dateFolderId = df1Id,
                date = "1404/02/10",
                status = AttendanceStatus.FULL_DAY,
                entryTime = "07:30",
                exitTime = "18:30",
                regularHours = 8.0,
                overtimeHours = 2.5,
                overtimeRate = 200_000L,
                dailyWage = 1_500_000L,
                transitAllowance = 50_000L,
                transitImpact = "ALLOWANCE",
                foodAllowance = 80_000L,
                foodImpact = "ALLOWANCE",
                accommodationAllowance = 40_000L,
                accommodationImpact = "DEDUCTION",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج میلاد نو",
                foremanName = "استاد رحیمی",
                employerName = "شرکت عمران سازه",
                notes = "کارکرد کامل با اضافه‌کاری"
            )
        )

        db.attendanceDao().insertAttendance(
            AttendanceEntity(
                folderId = f2Id,
                workerId = w2Id,
                dateFolderId = df2Id,
                date = "1404/02/11",
                status = AttendanceStatus.HOURLY,
                regularHours = 0.0,
                hourlyHours = 6.5,
                hourlyWageRate = 180_000L,
                hourlyWage = 180_000L,
                hasCustomAllowances = true,
                workplaceName = "پروژه مجتمع تجاری پارس"
            )
        )

        db.expenseDao().insertExpense(
            ExpenseEntity(
                folderId = f1Id,
                title = "ناهار کارگاهی",
                category = "FOOD",
                scope = "GROUP",
                impactType = "DEDUCTION",
                amount = 250_000L,
                date = "1404/02/10",
                notes = "تهیه غذا"
            )
        )
        db.expenseDao().insertExpense(
            ExpenseEntity(
                folderId = f1Id,
                title = "مساعده نقدی",
                category = "OTHER",
                scope = "INDIVIDUAL",
                impactType = "DEDUCTION",
                workerId = w1Id,
                workerName = "کریم منصوری",
                amount = 300_000L,
                date = "1404/02/10"
            )
        )

        val summaryBefore = WageCalculator.calculateFinancialSummary(
            workers = db.workerDao().getWorkersByFolderSync(f1Id),
            attendances = db.attendanceDao().getAttendanceByFolderSync(f1Id),
            expenses = db.expenseDao().getExpensesByFolderSync(f1Id),
            todayStr = "1404/02/10"
        )

        // ۲. تهیه نسخه پشتیبان کامل
        val (backupJson, savedFile) = viewModel.createFullBackupJsonSync(saveLocalCopy = true)
        assertNotNull(savedFile)
        assertTrue(savedFile!!.exists())
        assertEquals(1, BackupUtil.listLocalBackups(app).size)

        // ۳. پاک کردن کل اطلاعات دیتابیس
        db.attendanceDao().clearAll()
        db.expenseDao().clearAll()
        db.workerDao().clearAll()
        db.dateFolderDao().clearAll()
        db.folderDao().clearAll()
        assertEquals(0, db.folderDao().getAllFoldersSync().size)
        assertEquals(0, db.workerDao().getAllWorkersSync().size)
        assertEquals(0, db.attendanceDao().getAllAttendanceSync().size)

        // ۴. بازیابی نسخه پشتیبان
        val restoreResult = viewModel.restoreFullBackupJsonSync(backupJson)
        assertTrue(restoreResult.isSuccess)

        // ۵. بررسی صحت کامل تمام جداول و محاسبات مالی پس از بازیابی
        val restoredFolders = db.folderDao().getAllFoldersSync()
        val restoredDateFolders = db.dateFolderDao().getAllDateFoldersSync()
        val restoredWorkers = db.workerDao().getAllWorkersSync()
        val restoredAttendances = db.attendanceDao().getAllAttendanceSync()
        val restoredExpenses = db.expenseDao().getAllExpensesSync()

        assertEquals(2, restoredFolders.size)
        assertEquals(2, restoredDateFolders.size)
        assertEquals(2, restoredWorkers.size)
        assertEquals(2, restoredAttendances.size)
        assertEquals(2, restoredExpenses.size)

        val summaryAfter = WageCalculator.calculateFinancialSummary(
            workers = db.workerDao().getWorkersByFolderSync(f1Id),
            attendances = db.attendanceDao().getAttendanceByFolderSync(f1Id),
            expenses = db.expenseDao().getExpensesByFolderSync(f1Id),
            todayStr = "1404/02/10"
        )
        assertEquals(summaryBefore.grandTotalProjectCost, summaryAfter.grandTotalProjectCost)
        assertEquals(summaryBefore.totalBaseWagesPaid, summaryAfter.totalBaseWagesPaid)
        assertEquals(summaryBefore.totalOvertimePaid, summaryAfter.totalOvertimePaid)
        assertEquals(summaryBefore.totalAllowances, summaryAfter.totalAllowances)
        assertEquals(summaryBefore.totalDeductions, summaryAfter.totalDeductions)
    }

    @Test
    fun restoreCorruptedJson_doesNotClearExistingDatabaseData() = runBlocking {
        val folderId = db.folderDao().insertFolder(
            WorkplaceFolderEntity(name = "پروژه حیاتی فعلی")
        )
        db.workerDao().insertWorker(
            WorkerEntity(folderId = folderId, name = "کارگر فعلی", role = "بنا")
        )

        val result = viewModel.restoreFullBackupJsonSync("{ invalid_json_content ")
        assertTrue(result.isFailure)

        // اطلاعات قبلی نباید پاک شده باشد
        assertEquals(1, db.folderDao().getAllFoldersSync().size)
        assertEquals("پروژه حیاتی فعلی", db.folderDao().getAllFoldersSync().first().name)
        assertEquals(1, db.workerDao().getAllWorkersSync().size)
    }
}
