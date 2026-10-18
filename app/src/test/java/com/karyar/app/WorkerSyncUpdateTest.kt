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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkerSyncUpdateTest {

    @Test
    fun testUpdateWorker_syncsWorkerInfoExpensesAndAttendance() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)
        val repository = WorkerRepository(db)

        db.attendanceDao().clearAll()
        db.expenseDao().clearAll()
        db.workerDao().clearAll()
        db.dateFolderDao().clearAll()
        db.folderDao().clearAll()

        val folderId = repository.insertFolder(
            WorkplaceFolderEntity(name = "کارگاه سینک اطلاعات")
        )

        val dfId = repository.insertDateFolder(
            DateFolderEntity(folderId = folderId, date = "1403/06/01", dayOfWeek = "پنج‌شنبه")
        )

        val originalWorker = WorkerEntity(
            folderId = folderId,
            name = "علی رضایی",
            role = "بنا",
            phone = "09121111111",
            nationalId = "0011223344",
            baseDailyWage = 500000L,
            overtimeRate = 70000L
        )
        val workerId = repository.insertWorker(originalWorker)

        // Add expense assigned to this worker
        val expense = ExpenseEntity(
            folderId = folderId,
            title = "کرایه تاکسی",
            category = "TRANSIT",
            scope = "INDIVIDUAL",
            workerId = workerId,
            workerName = "علی رضایی",
            amount = 50000L,
            date = "1403/06/01"
        )
        repository.insertExpense(expense)

        // Add full-day attendance
        val att = AttendanceEntity(
            folderId = folderId,
            workerId = workerId,
            dateFolderId = dfId,
            date = "1403/06/01",
            status = AttendanceStatus.FULL_DAY,
            dailyWage = 500000L,
            overtimeRate = 70000L
        )
        repository.insertAttendance(att)

        // Now edit worker's personal info and wage
        val updatedWorker = originalWorker.copy(
            id = workerId,
            name = "علی رضایی اصل",
            role = "استادکار بنا",
            phone = "09129999999",
            nationalId = "0099887766",
            baseDailyWage = 750000L,
            overtimeRate = 100000L
        )
        repository.updateWorker(updatedWorker)

        // 1. Verify worker entity in database is updated
        val storedWorkers = repository.getWorkersByFolder(folderId).first()
        val storedWorker = storedWorkers.first { it.id == workerId }
        assertEquals("علی رضایی اصل", storedWorker.name)
        assertEquals("استادکار بنا", storedWorker.role)
        assertEquals("09129999999", storedWorker.phone)
        assertEquals("0099887766", storedWorker.nationalId)
        assertEquals(750000L, storedWorker.baseDailyWage)
        assertEquals(100000L, storedWorker.overtimeRate)

        // 2. Verify expense assigned to worker has synced workerName
        val storedExpenses = repository.getExpensesByFolder(folderId).first()
        val storedExpense = storedExpenses.first { it.workerId == workerId }
        assertEquals("علی رضایی اصل", storedExpense.workerName)

        // 3. Verify existing attendance keeps its own independent day values
        val storedAttendances = repository.getAttendanceByFolder(folderId).first()
        val storedAtt = storedAttendances.first { it.workerId == workerId }
        assertEquals(500000L, storedAtt.dailyWage)
        assertEquals(70000L, storedAtt.overtimeRate)
    }

    @Test
    fun testUpdateWorkerProfile_preservesIndependentDayAttendances() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)
        val repository = WorkerRepository(db)

        db.attendanceDao().clearAll()
        db.expenseDao().clearAll()
        db.workerDao().clearAll()
        db.dateFolderDao().clearAll()
        db.folderDao().clearAll()

        val folderId = repository.insertFolder(
            WorkplaceFolderEntity(name = "پروژه برج سپهر")
        )

        val dfId = repository.insertDateFolder(
            DateFolderEntity(folderId = folderId, date = "1405/01/10", dayOfWeek = "شنبه")
        )

        // Similar to Saeid Ahmadi initially with 5.0 hours in profile
        val saeid = WorkerEntity(
            folderId = folderId,
            name = "سعید احمدی",
            role = "جوشکار",
            isHourlyEnabled = true,
            hourlyWageRate = 170000L,
            hourlyHours = 5.0,
            isOvertimeEnabled = true,
            overtimeRate = 200000L,
            overtimeHours = 2.0
        )
        val workerId = repository.insertWorker(saeid)

        val att = AttendanceEntity(
            folderId = folderId,
            workerId = workerId,
            dateFolderId = dfId,
            date = "1405/01/10",
            status = AttendanceStatus.HOURLY,
            regularHours = 0.0,
            hourlyHours = 6.0,
            hourlyWageRate = 170000L,
            overtimeHours = 2.0,
            overtimeRate = 200000L
        )
        repository.insertAttendance(att)

        // Edit Saeid's work hours to 5.5 in profile
        val editedSaeid = saeid.copy(
            id = workerId,
            hourlyHours = 5.5,
            overtimeHours = 2.0
        )
        repository.updateWorker(editedSaeid)

        // 1. Verify worker profile entity in database has updated hourlyHours to 5.5
        val workers = repository.getWorkersByFolder(folderId).first()
        val storedWorker = workers.first { it.id == workerId }
        assertEquals(5.5, storedWorker.hourlyHours, 0.001)

        // 2. Verify stored attendance record for 1405/01/10 kept its own 6.0 hours (independent)
        val attendances = repository.getAttendanceByFolder(folderId).first()
        val storedAtt = attendances.first { it.workerId == workerId }
        assertEquals(6.0, storedAtt.hourlyHours, 0.001)
        assertEquals(2.0, storedAtt.overtimeHours, 0.001)
    }
}
