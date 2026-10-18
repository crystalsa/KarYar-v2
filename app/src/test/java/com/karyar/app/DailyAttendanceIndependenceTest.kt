package com.karyar.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.data.repository.WorkerRepository
import com.karyar.app.util.WageCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyAttendanceIndependenceTest {

    @Test
    fun testDaysHaveIndependentHoursOvertimeAndAllowances() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)
        val repository = WorkerRepository(db)

        db.attendanceDao().clearAll()
        db.expenseDao().clearAll()
        db.workerDao().clearAll()
        db.dateFolderDao().clearAll()
        db.folderDao().clearAll()

        val folderId = repository.insertFolder(
            WorkplaceFolderEntity(name = "کارگاه آزمایشی")
        )

        // Day 1
        val df1Id = repository.createNextDay(
            folderId = folderId,
            date = "1405/01/15",
            dayOfWeek = "شنبه"
        )

        val worker = WorkerEntity(
            folderId = folderId,
            name = "سعید احمدی",
            role = "برق‌کار",
            baseDailyWage = 1000000L,
            hourlyWageRate = 120000L,
            hourlyHours = 8.0,
            overtimeRate = 150000L,
            overtimeHours = 0.0,
            accommodationAllowance = 200000L,
            accommodationImpact = "ALLOWANCE"
        )
        val workerId = repository.insertWorker(worker)

        // Day 1 attendance customization
        val attDay1 = AttendanceEntity(
            folderId = folderId,
            workerId = workerId,
            dateFolderId = df1Id,
            date = "1405/01/15",
            status = AttendanceStatus.HOURLY,
            hourlyWageRate = 120000L,
            hourlyHours = 6.0,
            overtimeHours = 1.0,
            overtimeRate = 150000L,
            accommodationAllowance = 200000L,
            accommodationImpact = "ALLOWANCE",
            hasCustomAllowances = true
        )
        repository.insertAttendance(attDay1)

        // Day 2
        val df2Id = repository.createNextDay(
            folderId = folderId,
            date = "1405/01/16",
            dayOfWeek = "یکشنبه"
        )

        // Day 2 attendance customization (different hours, no overtime, no accommodation)
        val attDay2 = AttendanceEntity(
            folderId = folderId,
            workerId = workerId,
            dateFolderId = df2Id,
            date = "1405/01/16",
            status = AttendanceStatus.HOURLY,
            hourlyWageRate = 120000L,
            hourlyHours = 5.0,
            overtimeHours = 0.0,
            overtimeRate = 150000L,
            accommodationAllowance = 0L,
            accommodationImpact = "ALLOWANCE",
            hasCustomAllowances = true
        )
        repository.insertAttendance(attDay2)

        // Retrieve both attendances
        val savedAtt1 = db.attendanceDao().getAttendanceForWorkerAndDate(workerId, "1405/01/15")!!
        val savedAtt2 = db.attendanceDao().getAttendanceForWorkerAndDate(workerId, "1405/01/16")!!

        // Verify Day 1 values remained unchanged
        assertEquals(6.0, savedAtt1.hourlyHours, 0.001)
        assertEquals(1.0, savedAtt1.overtimeHours, 0.001)
        assertEquals(200000L, savedAtt1.accommodationAllowance)

        // Verify Day 2 has its own separate values
        assertEquals(5.0, savedAtt2.hourlyHours, 0.001)
        assertEquals(0.0, savedAtt2.overtimeHours, 0.001)
        assertEquals(0L, savedAtt2.accommodationAllowance)

        // Calculate single day payouts
        val day1Result = WageCalculator.calculateDay(worker, savedAtt1)
        val day2Result = WageCalculator.calculateDay(worker, savedAtt2)

        // Day 1: 6h * 120,000 (720,000) + 1h * 150,000 (150,000) + 200,000 accom = 1,070,000
        assertEquals(720000L, day1Result.hourlyPay)
        assertEquals(150000L, day1Result.overtimePay)
        assertEquals(200000L, day1Result.accommodationAllowance)
        assertEquals(1070000L, day1Result.netPayout)

        // Day 2: 5h * 120,000 (600,000) + 0 overtime + 0 accom = 600,000
        assertEquals(600000L, day2Result.hourlyPay)
        assertEquals(0L, day2Result.overtimePay)
        assertEquals(0L, day2Result.accommodationAllowance)
        assertEquals(600000L, day2Result.netPayout)
    }
}
