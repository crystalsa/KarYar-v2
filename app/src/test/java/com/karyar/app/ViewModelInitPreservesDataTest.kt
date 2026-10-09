package com.karyar.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.ui.WorkerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
}
