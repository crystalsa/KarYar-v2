package com.karyar.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.ui.WorkerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ViewModelInitPreservesDataTest {

    @Test
    fun testViewModelInitPreservesCustomProjectWithoutSepehr() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)

        // Clear cleanly before setting up custom data
        db.attendanceDao().clearAll()
        db.expenseDao().clearAll()
        db.workerDao().clearAll()
        db.dateFolderDao().clearAll()
        db.folderDao().clearAll()

        // 1. Insert a custom project that has NO "سپهر" in its name
        val customProjectName = "کارگاه احداث پل طبیعت ۲"
        val folderId = db.folderDao().insertFolder(
            WorkplaceFolderEntity(
                name = customProjectName,
                foremanName = "مهندس مرادی",
                employerName = "شهرداری تهران",
                colorTag = 0xFF10B981L,
                notes = "پروژه آزمایشی با مشخصات دلخواه"
            )
        )

        // 2. Insert 4 date folders (arbitrary day count != 2)
        val sampleDates = listOf("1403/07/01", "1403/07/02", "1403/07/03", "1403/07/04")
        sampleDates.forEach { dateStr ->
            db.dateFolderDao().insertDateFolder(
                DateFolderEntity(
                    folderId = folderId,
                    date = dateStr,
                    dayOfWeek = "شنبه",
                    title = "روز کاری $dateStr",
                    notes = "تست"
                )
            )
        }

        // 3. Insert 7 workers (arbitrary worker count != 5)
        val workerNames = listOf(
            "بهرام رادمنش",
            "کامران نجفی",
            "سهراب سپهری‌نیا",
            "فرهاد مجیدی",
            "پیمان قاسم‌خانی",
            "داریوش ارجمند",
            "شهاب حسینی"
        )
        workerNames.forEachIndexed { index, name ->
            db.workerDao().insertWorker(
                WorkerEntity(
                    folderId = folderId,
                    name = name,
                    role = "کارگر فنی $index",
                    baseDailyWage = 1000000L + (index * 50000L),
                    isActive = true
                )
            )
        }

        // Verify initial state before ViewModel initialization
        val foldersBefore = db.folderDao().getAllFoldersSync()
        val dateFoldersBefore = db.dateFolderDao().getDateFoldersByFolderSync(folderId)
        val workersBefore = db.workerDao().getWorkersByFolderSync(folderId)

        assertEquals(1, foldersBefore.size)
        assertEquals(customProjectName, foldersBefore[0].name)
        assertEquals(4, dateFoldersBefore.size)
        assertEquals(7, workersBefore.size)

        // 4. Initialize ViewModel (simulating app start)
        val viewModel = WorkerViewModel(app)

        // Give coroutines in init time to execute
        delay(600)

        // 5. Verify that WorkerViewModel initialization DID NOT delete or wipe any data
        val foldersAfter = db.folderDao().getAllFoldersSync()
        val dateFoldersAfter = db.dateFolderDao().getDateFoldersByFolderSync(folderId)
        val workersAfter = db.workerDao().getWorkersByFolderSync(folderId)

        assertEquals("Custom project must be preserved", 1, foldersAfter.size)
        assertEquals("Project name must not be altered", customProjectName, foldersAfter[0].name)
        assertEquals("All date folders must be preserved", 4, dateFoldersAfter.size)
        assertEquals("All workers must be preserved", 7, workersAfter.size)
    }

    @Test
    fun testViewModelInitWithSynchronousFlowOf_doesNotCrash() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val folderA = WorkplaceFolderEntity(
            id = 101L,
            name = "پروژه همگام الف",
            foremanName = "سرکارگر الف",
            employerName = "کارفرما الف"
        )
        val folderB = WorkplaceFolderEntity(
            id = 102L,
            name = "پروژه همگام ب",
            foremanName = "سرکارگر ب",
            employerName = "کارفرما ب"
        )

        // ۱. ساخت ViewModel با یک Flow کاملاً همگام (flowOf) که بلافاصله در لحظه init مقدار منتشر می‌کند
        val vmWithItems = WorkerViewModel(
            application = app,
            foldersFlow = flowOf(listOf(folderA, folderB))
        )

        assertNotNull(vmWithItems.currentFolder)
        assertNotNull(vmWithItems.selectedDateFolder)
        assertNotNull(vmWithItems.selectedDailyDate)
        assertNotNull(vmWithItems.allFolders)
        assertNotNull(vmWithItems.dateFolders)
        assertNotNull(vmWithItems.workers)
        assertNotNull(vmWithItems.attendanceList)
        assertNotNull(vmWithItems.expenses)
        assertNotNull(vmWithItems.dailyBookkeeping)
        assertNotNull(vmWithItems.financialSummary)
        assertNotNull(vmWithItems.workerPerformances)
        assertNotNull(vmWithItems.analytics)

        // به دلیل اجرای همگام (UNDISPATCHED) روی flowOf، مقدار currentFolder بلافاصله بدون کرش مقداردهی شده است
        assertEquals(folderA, vmWithItems.currentFolder.value)
        assertNull(vmWithItems.selectedDateFolder.value)

        // ۲. بررسی حالتی که flowOf ابتدا لیست پر، سپس لیست خالی (ریست شدن currentFolder و selectedDateFolder) و دوباره لیست جدید منتشر می‌کند
        val vmMultiEmit = WorkerViewModel(
            application = app,
            foldersFlow = flowOf(
                listOf(folderA),
                emptyList(),
                listOf(folderB)
            )
        )
        assertEquals(folderB, vmMultiEmit.currentFolder.value)
        assertNull(vmMultiEmit.selectedDateFolder.value)

        // ۳. بررسی حالتی که flowOf لیست خالی منتشر می‌کند
        val vmEmpty = WorkerViewModel(
            application = app,
            foldersFlow = flowOf(emptyList())
        )
        assertNull(vmEmpty.currentFolder.value)
        assertNull(vmEmpty.selectedDateFolder.value)
    }

    @Test
    fun testViewModelFlowsHandleOverflowRecordWithoutCrash() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)

        db.attendanceDao().clearAll()
        db.expenseDao().clearAll()
        db.workerDao().clearAll()
        db.dateFolderDao().clearAll()
        db.folderDao().clearAll()

        val folderEntity = WorkplaceFolderEntity(
            name = "پروژه تست سرریز",
            foremanName = "سرکارگر",
            employerName = "کارفرما"
        )
        val folderId = db.folderDao().insertFolder(folderEntity)
        val workerId = db.workerDao().insertWorker(
            WorkerEntity(
                folderId = folderId,
                name = "کارگر تست",
                role = "استادکار",
                baseDailyWage = 1_000_000L,
                isActive = true
            )
        )
        val dateFolder = DateFolderEntity(
            folderId = folderId,
            date = "1404/01/10",
            dayOfWeek = "یکشنبه",
            title = "روز دهم"
        )
        val dateFolderId = db.dateFolderDao().insertDateFolder(dateFolder)

        // ۱. رکورد سالم
        db.attendanceDao().insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = workerId,
                dateFolderId = dateFolderId,
                date = "1404/01/10",
                status = AttendanceStatus.FULL_DAY,
                regularHours = 8.0,
                dailyWage = 1_000_000L,
                hasCustomAllowances = true
            )
        )
        // ۲. رکورد سرریز شونده (dailyWage = Long.MAX_VALUE / 2 با کسورات و اضافه کاری که سرریز می‌کند)
        val badId = db.attendanceDao().insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = workerId,
                dateFolderId = dateFolderId,
                date = "1404/01/11",
                status = AttendanceStatus.FULL_DAY,
                regularHours = 8.0,
                dailyWage = Long.MAX_VALUE / 2,
                overtimeHours = 10.0,
                overtimeRate = Long.MAX_VALUE / 2,
                hasCustomAllowances = true
            )
        )

        val savedFolder = folderEntity.copy(id = folderId)
        val viewModel = WorkerViewModel(
            application = app,
            foldersFlow = flowOf(listOf(savedFolder))
        )
        viewModel.selectFolder(savedFolder)
        viewModel.selectDailyDate("1404/01/11")

        // مشترک شدن روی StateFlowهای WhileSubscribed تا محاسبات فعال شوند
        val collectJob = launch(kotlinx.coroutines.Dispatchers.Default) {
            launch { viewModel.financialSummary.collect {} }
            launch { viewModel.dailyBookkeeping.collect {} }
            launch { viewModel.workerPerformances.collect {} }
            launch { viewModel.analytics.collect {} }
            launch { viewModel.problematicRecords.collect {} }
        }

        // پیش بردن Main Looper در Robolectric تا نتایج از Dispatchers.IO و Dispatchers.Default به viewModelScope برسند
        val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
        repeat(30) {
            mainLooper.idle()
            if (viewModel.financialSummary.value.invalidAttendanceIds.isNotEmpty() &&
                viewModel.problematicRecords.value.isNotEmpty()
            ) {
                return@repeat
            }
            delay(50)
        }
        mainLooper.idle()

        val summary = viewModel.financialSummary.value
        assertEquals(1_000_000L, summary.grandTotalProjectCost)
        assertTrue(summary.invalidAttendanceIds.contains(badId))
        assertNotNull(viewModel.calculationError.value)
        assertTrue(viewModel.problematicRecords.value.any { it.attendanceId == badId })

        collectJob.cancel()
    }
}

