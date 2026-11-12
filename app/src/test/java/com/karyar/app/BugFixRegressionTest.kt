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
import com.karyar.app.data.repository.WorkerRepository
import com.karyar.app.domain.model.DashboardAnalytics
import com.karyar.app.domain.model.WorkerPerformance
import com.karyar.app.ui.WorkerViewModel
import com.karyar.app.util.BackupUtil
import com.karyar.app.util.ExcelExportUtil
import com.karyar.app.util.FullBackupPayload
import com.karyar.app.util.PdfExportUtil
import com.karyar.app.util.WageCalculator
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [ShadowPdfDocument::class])
class BugFixRegressionTest {

    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var repository: WorkerRepository

    @Before
    fun setUp() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        db = AppDatabase.getDatabase(app)
        db.attendanceDao().clearAll()
        db.expenseDao().clearAll()
        db.workerDao().clearAll()
        db.dateFolderDao().clearAll()
        db.folderDao().clearAll()
        repository = WorkerRepository(
            database = db,
            folderDao = db.folderDao(),
            dateFolderDao = db.dateFolderDao(),
            workerDao = db.workerDao(),
            attendanceDao = db.attendanceDao(),
            expenseDao = db.expenseDao()
        )
    }

    @After
    fun tearDown() = runBlocking {
        db.attendanceDao().clearAll()
        db.expenseDao().clearAll()
        db.workerDao().clearAll()
        db.dateFolderDao().clearAll()
        db.folderDao().clearAll()
    }

    @Test
    fun parseBackupFromJson_handlesCorruptNumbersNonObjectsAndInvalidEnumsSafely() {
        val malformedJson = """
            {
              "format": "${FullBackupPayload.BACKUP_FORMAT_ID}",
              "workplace_folders": [
                null,
                "invalid_primitive",
                {
                  "id": 1,
                  "name": "کارگاه تست",
                  "colorTag": 4292441862,
                  "createdAt": "1405/01/10"
                }
              ],
              "date_folders": [
                null,
                {
                  "id": 10,
                  "folderId": 1,
                  "date": "1405/01/10",
                  "dayOfWeek": "دوشنبه",
                  "title": "روز اول"
                }
              ],
              "workers": [
                123,
                {
                  "id": 20,
                  "folderId": 1,
                  "name": "کارگر تستی",
                  "role": "بنا",
                  "baseDailyWage": -5000,
                  "hourlyHours": "NaN",
                  "overtimeHours": "Infinity",
                  "transitImpact": "UNKNOWN_IMPACT"
                }
              ],
              "attendance": [
                null,
                {
                  "id": 30,
                  "folderId": 1,
                  "workerId": 20,
                  "dateFolderId": 10,
                  "date": "1405/01/10",
                  "status": "BOGUS_STATUS",
                  "regularHours": "NaN",
                  "hourlyHours": -3.0,
                  "dailyWage": -100
                }
              ],
              "expenses": [
                "bad_item",
                {
                  "id": 40,
                  "folderId": 1,
                  "title": "ناهار",
                  "category": "INVALID_CAT",
                  "scope": "INVALID_SCOPE",
                  "impactType": "INVALID_IMPACT",
                  "amount": -25000,
                  "date": "1405/01/10"
                }
              ]
            }
        """.trimIndent()

        val payload = BackupUtil.parseBackupFromJson(malformedJson)
        assertEquals(1, payload.folders.size)
        assertEquals(1, payload.dateFolders.size)
        assertEquals(1, payload.workers.size)
        assertEquals(1, payload.attendances.size)
        assertEquals(1, payload.expenses.size)

        val worker = payload.workers.first()
        assertEquals(0L, worker.baseDailyWage)
        assertTrue(worker.hourlyHours.isFinite() && worker.hourlyHours >= 0.0)
        assertTrue(worker.overtimeHours.isFinite() && worker.overtimeHours >= 0.0)

        val att = payload.attendances.first()
        assertEquals(AttendanceStatus.ABSENT, att.status)
        assertTrue(att.regularHours.isFinite() && att.regularHours >= 0.0)
        assertTrue(att.hourlyHours.isFinite() && att.hourlyHours >= 0.0)
        assertEquals(0L, att.dailyWage)

        val exp = payload.expenses.first()
        assertEquals("OTHER", exp.category)
        assertEquals("GROUP", exp.scope)
        assertEquals(0L, exp.amount)
    }

    @Test
    fun restoreFullBackupPayload_deduplicatesNormalizedCollidingDateFolders() = runBlocking {
        val folder = WorkplaceFolderEntity(id = 1L, name = "پروژه هم‌پوشان", createdAt = "1405/01/01")
        // 1404/12/30 in non-leap 1404 normalizes to 1405/01/01 — colliding with df2
        val df1 = DateFolderEntity(id = 10L, folderId = 1L, date = "1404/12/30", dayOfWeek = "شنبه")
        val df2 = DateFolderEntity(id = 11L, folderId = 1L, date = "1405/01/01", dayOfWeek = "شنبه")
        val worker = WorkerEntity(id = 20L, folderId = 1L, name = "احمد", role = "بنا", baseDailyWage = 1_000_000L)
        val att = AttendanceEntity(
            id = 30L,
            folderId = 1L,
            workerId = 20L,
            dateFolderId = 11L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            dailyWage = 1_000_000L
        )

        val payload = FullBackupPayload(
            folders = listOf(folder),
            dateFolders = listOf(df1, df2),
            workers = listOf(worker),
            attendances = listOf(att),
            expenses = emptyList()
        )

        repository.restoreFullBackupPayload(payload)

        val restoredDfs = db.dateFolderDao().getDateFoldersByFolderSync(1L)
        assertEquals("Duplicate normalized date folders must be deduplicated", 1, restoredDfs.size)
        assertEquals("1405/01/01", restoredDfs.first().date)

        val restoredAtts = db.attendanceDao().getAttendanceByFolderSync(1L)
        assertEquals(1, restoredAtts.size)
        assertEquals(restoredDfs.first().id, restoredAtts.first().dateFolderId)
    }

    @Test
    fun duplicateWorker_withoutDateFolderId_resolvesOrCreatesValidDateFolder() = runBlocking {
        val folderId = db.folderDao().insertFolder(WorkplaceFolderEntity(name = "کارگاه کپی"))
        val workerId = db.workerDao().insertWorker(
            WorkerEntity(folderId = folderId, name = "کاظم", role = "بنا", baseDailyWage = 900_000L)
        )
        val worker = db.workerDao().getWorkersByFolderSync(folderId).first { it.id == workerId }

        val copiedId = repository.duplicateWorker(
            worker = worker,
            targetDate = "1405/02/05",
            targetDateFolderId = 0L
        )

        val dateFolders = db.dateFolderDao().getDateFoldersByFolderSync(folderId)
        assertEquals(1, dateFolders.size)
        assertEquals("1405/02/05", dateFolders.first().date)

        val copiedAtt = db.attendanceDao().getAttendanceForWorkerAndDate(copiedId, "1405/02/05")
        assertNotNull(copiedAtt)
        assertEquals(dateFolders.first().id, copiedAtt!!.dateFolderId)
    }

    @Test
    fun viewModel_selectFolderSwitch_clearsSearchAndTransientState() = runBlocking {
        val f1 = WorkplaceFolderEntity(id = 1L, name = "کارگاه یک")
        val f2 = WorkplaceFolderEntity(id = 2L, name = "کارگاه دو")
        val viewModel = WorkerViewModel(app, flowOf(listOf(f1, f2)))
        try {
            viewModel.selectFolder(f1)
            viewModel.toggleWorkerSearch(true)
            viewModel.setWorkerSearchQuery("علی")
            assertEquals("علی", viewModel.workerSearchQuery.value)
            assertTrue(viewModel.isWorkerSearchVisible.value)

            viewModel.selectFolder(f2)
            assertEquals("", viewModel.workerSearchQuery.value)
            assertFalse(viewModel.isWorkerSearchVisible.value)
            assertNull(viewModel.lastUndoState.value)
        } finally {
            viewModel.stopCollectorsAndReceivers()
        }
    }

    @Test
    fun wageCalculator_overflowingExpenseDoesNotWipeValidTotals_andSafeDayHelpersDoNotThrow() {
        val worker = WorkerEntity(
            id = 1L,
            folderId = 1L,
            name = "مهدی",
            role = "بنا",
            baseDailyWage = 1_200_000L
        )
        val validAtt = AttendanceEntity(
            id = 1L,
            folderId = 1L,
            workerId = 1L,
            date = "1405/01/15",
            epochDay = 20000L,
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            dailyWage = 1_200_000L,
            hasCustomAllowances = true
        )
        val overflowingExpense = ExpenseEntity(
            id = 99L,
            folderId = 1L,
            title = "هزینه خراب",
            category = "OTHER",
            scope = "GROUP",
            impactType = "DEDUCTION",
            amount = Long.MAX_VALUE,
            date = "1405/01/15",
            epochDay = 20000L
        )

        val summary = WageCalculator.calculateFinancialSummary(
            workers = listOf(worker),
            attendances = listOf(validAtt),
            expenses = listOf(overflowingExpense),
            todayStr = "1405/01/15"
        )
        assertEquals(1_200_000L, summary.grandTotalProjectCost)

        val daily = WageCalculator.calculateDailyBookkeepingSafe(
            dateStr = "1405/01/15",
            workers = listOf(worker),
            attendances = listOf(validAtt),
            expenses = listOf(overflowingExpense)
        )
        assertEquals(1_200_000L, daily.grandDailyCost)

        val corruptAtt = validAtt.copy(
            id = 2L,
            dailyWage = Long.MAX_VALUE,
            transitAllowance = 1_000_000L
        )
        assertNull(WageCalculator.calculateDayOrNull(worker, corruptAtt))
        val safeDay = WageCalculator.calculateDaySafe(worker, corruptAtt)
        assertEquals(0L, safeDay.netPayout)
    }

    @Test
    fun excelXlsxExport_treatsTextColumnsAndNonFiniteStringsAsInlineStr() {
        val tempDir = java.nio.file.Files.createTempDirectory("karyar_xlsx_bugfix").toFile()
        val testContext = object : android.content.ContextWrapper(app) {
            override fun getCacheDir(): File = tempDir
        }

        val worker = WorkerEntity(
            id = 1L,
            folderId = 1L,
            name = "123",
            role = "NaN",
            notes = "007",
            baseDailyWage = 1_000_000L
        )
        val perf = WorkerPerformance(
            worker = worker,
            totalShifts = 4,
            fullDaysCount = 4,
            regularHours = 32.0,
            overtimeHours = 1.5,
            baseWageTotal = 4_000_000L,
            overtimePayTotal = 300_000L,
            netPayout = 4_300_000L
        )

        val xlsxFile = ExcelExportUtil.exportToExcelXlsx(
            context = testContext,
            workers = listOf(worker),
            attendanceList = emptyList(),
            performances = listOf(perf),
            projectName = "Infinity"
        )

        ZipFile(xlsxFile).use { zip ->
            val sheetXml = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")!!)
                .bufferedReader(Charsets.UTF_8)
                .readText()
            assertTrue(
                "Numeric-looking name '123' must be inlineStr",
                sheetXml.contains("<c r=\"A2\" s=\"2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">123</t></is></c>")
            )
            assertTrue(
                "Literal 'NaN' role must be inlineStr and never t=\"n\"",
                sheetXml.contains("<c r=\"B2\" s=\"2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">NaN</t></is></c>")
            )
            assertTrue(
                "Numeric shift count C2 must still be numeric t=\"n\"",
                sheetXml.contains("<c r=\"C2\" s=\"3\" t=\"n\"><v>4</v></c>")
            )
            assertFalse(
                "Workbook must never contain numeric NaN/Infinity tags",
                sheetXml.contains("t=\"n\"><v>NaN</v>") || sheetXml.contains("t=\"n\"><v>Infinity</v>")
            )
        }

        tempDir.deleteRecursively()
    }

    @Test
    fun pdfExport_handlesLongRtlTextWithoutCrashAndGeneratesValidPdfHeader() {
        val tempDir = java.nio.file.Files.createTempDirectory("karyar_pdf_bugfix").toFile()
        val testContext = object : android.content.ContextWrapper(app) {
            override fun getCacheDir(): File = tempDir
        }

        val worker = WorkerEntity(
            id = 5L,
            folderId = 1L,
            name = "سید محمدرضا حسینی مقدم تهرانی با نام بسیار طولانی برای تست سطر",
            role = "آرماتوربند و قالب‌بند تخصصی سازه‌های بتنی مرتفع",
            baseDailyWage = 1_500_000L
        )
        val perf = WorkerPerformance(
            worker = worker,
            totalShifts = 10,
            fullDaysCount = 6,
            halfDaysCount = 2,
            hourlyDaysCount = 2,
            regularHours = 56.0,
            hourlyHours = 9.5,
            overtimeHours = 4.25,
            baseWageTotal = 10_500_000L,
            hourlyPayTotal = 1_425_000L,
            overtimePayTotal = 850_000L,
            otherAllowanceTotal = 250_000L,
            otherDeductionTotal = 400_000L,
            totalAllowances = 250_000L,
            totalDeductions = 400_000L,
            debtAmount = 150_000L,
            netPayout = 0L
        )
        val analytics = DashboardAnalytics(
            totalWorkersCount = 1,
            activeWorkersCount = 1,
            totalWorkDaysCount = 10,
            totalPersonDays = 10,
            grandTotalProjectCost = 12_625_000L,
            totalWorkerDebt = 150_000L
        )

        val pdfFile = PdfExportUtil.exportToPdf(
            context = testContext,
            projectName = "پروژه تستی PDF چندخطی",
            employerName = "کارفرمای تستی",
            foremanName = "سرکارگر تستی",
            analytics = analytics,
            performances = List(18) { idx ->
                perf.copy(worker = worker.copy(id = idx + 1L))
            }
        )

        assertTrue(pdfFile.exists() && pdfFile.length() > 10L)
        val header = pdfFile.inputStream().use { s ->
            val buf = ByteArray(5)
            s.read(buf)
            String(buf, Charsets.US_ASCII)
        }
        assertEquals("%PDF-", header)
        tempDir.deleteRecursively()
    }
}
