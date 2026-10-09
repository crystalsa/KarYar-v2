package com.karyar.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.data.repository.WorkerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AttendanceUpsertTest {

    @Test
    fun testTwoAttendanceInsertsOnSameWorkerAndDate_createsOnlyOneRowAndUpdates() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)
        val repository = WorkerRepository(db)

        db.attendanceDao().clearAll()
        db.expenseDao().clearAll()
        db.workerDao().clearAll()
        db.dateFolderDao().clearAll()
        db.folderDao().clearAll()

        val folderId = repository.insertFolder(
            WorkplaceFolderEntity(name = "کارگاه تست آپسرت")
        )

        val dfId = repository.insertDateFolder(
            DateFolderEntity(folderId = folderId, date = "1403/05/10", dayOfWeek = "چهارشنبه")
        )

        val workerId = repository.insertWorker(
            WorkerEntity(folderId = folderId, name = "بهرام راد", role = "گچ‌کار", baseDailyWage = 1000000L)
        )

        // First insert
        val firstAtt = AttendanceEntity(
            folderId = folderId,
            workerId = workerId,
            dateFolderId = dfId,
            date = "1403/05/10",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 1.0,
            overtimeRate = 150000L,
            notes = "ثبت اول"
        )
        val id1 = repository.insertAttendance(firstAtt)

        // Second insert for the exact same workerId and date with updated values
        val secondAtt = AttendanceEntity(
            folderId = folderId,
            workerId = workerId,
            dateFolderId = dfId,
            date = "1403/05/10",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 3.5, // updated overtime
            overtimeRate = 150000L,
            notes = "ویرایش و ثبت مجدد" // updated notes
        )
        val id2 = repository.insertAttendance(secondAtt)

        // Check total count and updated fields
        val allAtts = repository.getAttendanceByFolder(folderId).first()

        assertEquals("Only one attendance record must exist for the same worker and date", 1, allAtts.size)
        val record = allAtts.first()
        assertEquals(3.5, record.overtimeHours, 0.001)
        assertEquals("ویرایش و ثبت مجدد", record.notes)
        assertEquals(id1, record.id)
        assertEquals(id2, record.id)
    }
}
