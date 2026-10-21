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
import com.karyar.app.util.WageCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeletionImpactTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WorkerRepository

    @Before
    fun setUp() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = AppDatabase.getDatabase(app)
        repository = WorkerRepository(db)
        repository.clearAllData()
    }

    @Test
    fun deletionImpact_withNoHistory_returnsZeroCountsAndZeroAmount() = runBlocking {
        // 1. Empty workplace folder without any workers, days, or expenses
        val emptyFolderId = repository.insertFolder(
            WorkplaceFolderEntity(name = "کارگاه بدون سابقه")
        )
        val folderImpact = repository.getFolderDeletionImpact(emptyFolderId)
        assertEquals(0, folderImpact.attendanceDaysCount)
        assertEquals(0L, folderImpact.netSettlementAmount)
        assertEquals(0, folderImpact.totalAttendanceRecordsCount)
        assertFalse(folderImpact.hasHistory)

        // 2. Empty work day without any attendance or expenses
        val emptyDateFolderId = repository.insertDateFolder(
            DateFolderEntity(
                folderId = emptyFolderId,
                date = "1404/01/15",
                dayOfWeek = "جمعه",
                title = "روز بدون کارکرد"
            )
        )
        val dayImpact = repository.getDateFolderDeletionImpact(emptyDateFolderId)
        assertEquals(0, dayImpact.attendanceDaysCount)
        assertEquals(0L, dayImpact.netSettlementAmount)
        assertEquals(0, dayImpact.totalAttendanceRecordsCount)
        assertFalse(dayImpact.hasHistory)

        // 3. Worker with no attendance and no expenses
        val newWorkerId = repository.insertWorker(
            WorkerEntity(
                folderId = emptyFolderId,
                name = "کارگر تازه استخدام",
                role = "کارگر ساده",
                baseDailyWage = 900_000L
            )
        )
        val workerImpact = repository.getWorkerDeletionImpact(newWorkerId)
        assertEquals(0, workerImpact.attendanceDaysCount)
        assertEquals(0L, workerImpact.netSettlementAmount)
        assertEquals(0, workerImpact.totalAttendanceRecordsCount)
        assertFalse(workerImpact.hasHistory)

        // 4. Non-existent IDs safely return empty impact
        val missingWorkerImpact = repository.getWorkerDeletionImpact(999_999L)
        assertFalse(missingWorkerImpact.hasHistory)
        assertEquals(0, missingWorkerImpact.attendanceDaysCount)
        assertEquals(0L, missingWorkerImpact.netSettlementAmount)

        val missingDayImpact = repository.getDateFolderDeletionImpact(999_999L)
        assertFalse(missingDayImpact.hasHistory)
        assertEquals(0, missingDayImpact.attendanceDaysCount)
        assertEquals(0L, missingDayImpact.netSettlementAmount)
    }

    @Test
    fun deletionImpact_withSampleData_accuratelyCountsDaysAndNetSettlementForWorkerDayAndFolder() = runBlocking {
        val sampleFolderId = repository.loadSampleData()

        val workers = repository.getWorkersByFolder(sampleFolderId).first()
        val dateFolders = repository.getDateFoldersByFolder(sampleFolderId).first()
        val attendances = repository.getAttendanceByFolder(sampleFolderId).first()
        val expenses = repository.getExpensesByFolder(sampleFolderId).first()

        assertEquals(5, workers.size)
        assertEquals(2, dateFolders.size)
        assertEquals(10, attendances.size)

        val expectedSummary = WageCalculator.calculateFinancialSummary(
            workers = workers,
            attendances = attendances,
            expenses = expenses
        )

        // 1. Folder deletion impact must match total person-days (10) and grandTotalProjectCost
        val folderImpact = repository.getFolderDeletionImpact(sampleFolderId)
        assertTrue(folderImpact.hasHistory)
        assertEquals(10, folderImpact.attendanceDaysCount)
        assertEquals(expectedSummary.totalWorkDaysCount, folderImpact.attendanceDaysCount)
        assertEquals(expectedSummary.grandTotalProjectCost, folderImpact.netSettlementAmount)

        // 2. Worker deletion impact for each of the 5 sample workers
        var sumOfWorkersImpact = 0L
        var sumOfWorkerDays = 0
        for (worker in workers) {
            val workerImpact = repository.getWorkerDeletionImpact(worker)
            val expectedPerf = expectedSummary.workerPerformances.first { it.worker.id == worker.id }

            assertTrue(workerImpact.hasHistory)
            assertEquals(2, workerImpact.attendanceDaysCount)
            assertEquals(expectedPerf.totalShifts, workerImpact.attendanceDaysCount)
            assertEquals(expectedPerf.rawNetPayoutBeforeGroup, workerImpact.netSettlementAmount)

            sumOfWorkersImpact += workerImpact.netSettlementAmount
            sumOfWorkerDays += workerImpact.attendanceDaysCount
        }
        assertEquals(folderImpact.attendanceDaysCount, sumOfWorkerDays)
        assertEquals(folderImpact.netSettlementAmount, sumOfWorkersImpact)

        // 3. DateFolder (work day) deletion impact for each of the 2 sample days
        var sumOfDaysImpact = 0L
        var sumOfDaysPresent = 0
        for (df in dateFolders) {
            val dayImpact = repository.getDateFolderDeletionImpact(df)
            val expectedDaily = WageCalculator.calculateDailyBookkeeping(
                dateStr = df.date,
                workers = workers,
                attendances = attendances,
                expenses = expenses
            )

            assertTrue(dayImpact.hasHistory)
            assertEquals(5, dayImpact.attendanceDaysCount)
            assertEquals(expectedDaily.workersPresent, dayImpact.attendanceDaysCount)
            assertEquals(expectedDaily.grandDailyCost, dayImpact.netSettlementAmount)

            sumOfDaysImpact += dayImpact.netSettlementAmount
            sumOfDaysPresent += dayImpact.attendanceDaysCount
        }
        assertEquals(folderImpact.attendanceDaysCount, sumOfDaysPresent)
        assertEquals(folderImpact.netSettlementAmount, sumOfDaysImpact)
    }

    @Test
    fun deletionImpact_withAbsentDayDeductionsAndGroupExpenses_matchesExactLostRecords() = runBlocking {
        val folderId = repository.insertFolder(
            WorkplaceFolderEntity(name = "پروژه تست کسورات و هزینه گروهی")
        )
        val df1Id = repository.insertDateFolder(
            DateFolderEntity(folderId = folderId, date = "1404/02/01", dayOfWeek = "دوشنبه")
        )
        val df2Id = repository.insertDateFolder(
            DateFolderEntity(folderId = folderId, date = "1404/02/02", dayOfWeek = "سه‌شنبه")
        )

        val workerId = repository.insertWorker(
            WorkerEntity(
                folderId = folderId,
                name = "محسن کاظمی",
                role = "جوشکار",
                baseDailyWage = 1_000_000L
            )
        )

        // Day 1: Present full day (1,000,000) + group expense (150,000)
        repository.insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = workerId,
                dateFolderId = df1Id,
                date = "1404/02/01",
                status = AttendanceStatus.FULL_DAY,
                dailyWage = 1_000_000L
            )
        )
        repository.insertExpense(
            ExpenseEntity(
                folderId = folderId,
                title = "نهار کارگاه",
                category = "FOOD",
                scope = "GROUP",
                impactType = "DEDUCTION",
                amount = 150_000L,
                date = "1404/02/01"
            )
        )

        // Day 2: Absent with 200,000 accommodation deduction
        repository.insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = workerId,
                dateFolderId = df2Id,
                date = "1404/02/02",
                status = AttendanceStatus.ABSENT,
                dailyWage = 1_000_000L,
                accommodationAllowance = 200_000L,
                accommodationImpact = "DEDUCTION",
                hasCustomAllowances = true
            )
        )

        // Worker impact: 1 working day, 2 total attendance records, 1,000,000 - 200,000 = 800,000
        val workerImpact = repository.getWorkerDeletionImpact(workerId)
        assertTrue(workerImpact.hasHistory)
        assertEquals(1, workerImpact.attendanceDaysCount)
        assertEquals(2, workerImpact.totalAttendanceRecordsCount)
        assertEquals(800_000L, workerImpact.netSettlementAmount)

        // Day 1 impact: 1 present worker, 1,000,000 - 150,000 group expense = 850,000
        val day1Impact = repository.getDateFolderDeletionImpact(df1Id)
        assertTrue(day1Impact.hasHistory)
        assertEquals(1, day1Impact.attendanceDaysCount)
        assertEquals(850_000L, day1Impact.netSettlementAmount)

        // Day 2 impact: 0 present workers, 1 attendance record (absent), -200,000 net cost
        val day2Impact = repository.getDateFolderDeletionImpact(df2Id)
        assertTrue(day2Impact.hasHistory)
        assertEquals(0, day2Impact.attendanceDaysCount)
        assertEquals(1, day2Impact.totalAttendanceRecordsCount)
        assertEquals(-200_000L, day2Impact.netSettlementAmount)

        // Folder impact: 1 working day, 850,000 + (-200,000) = 650,000
        val folderImpact = repository.getFolderDeletionImpact(folderId)
        assertTrue(folderImpact.hasHistory)
        assertEquals(1, folderImpact.attendanceDaysCount)
        assertEquals(650_000L, folderImpact.netSettlementAmount)
    }

    @Test
    fun deactivateWorker_preservesHistoricalAttendanceAndFinancialSummary() = runBlocking {
        val sampleFolderId = repository.loadSampleData()
        val beforeImpact = repository.getFolderDeletionImpact(sampleFolderId)
        val workerToDeactivate = repository.getWorkersByFolder(sampleFolderId).first().first()

        repository.deactivateWorker(workerToDeactivate)

        val updatedWorkers = repository.getWorkersByFolder(sampleFolderId).first()
        val deactivated = updatedWorkers.first { it.id == workerToDeactivate.id }
        assertFalse(deactivated.isActive)

        // All historical attendance and financial totals remain intact
        val afterImpact = repository.getFolderDeletionImpact(sampleFolderId)
        assertEquals(beforeImpact.attendanceDaysCount, afterImpact.attendanceDaysCount)
        assertEquals(beforeImpact.netSettlementAmount, afterImpact.netSettlementAmount)
    }

    @Test
    fun duplicateWorker_createsNewWorkerWithDistinctIdAndDoesNotMergeByName() = runBlocking {
        val folderId = repository.insertFolder(WorkplaceFolderEntity(name = "کارگاه تست کپی"))
        val dfId = repository.insertDateFolder(
            DateFolderEntity(folderId = folderId, date = "1404/03/01", dayOfWeek = "پنج‌شنبه")
        )
        val originalId = repository.insertWorker(
            WorkerEntity(
                folderId = folderId,
                name = "علی رضایی",
                role = "بنا",
                nationalId = "0012345678",
                baseDailyWage = 1_200_000L
            )
        )
        val originalWorker = repository.getWorkersByFolder(folderId).first().first { it.id == originalId }

        val copiedWorkerId = repository.duplicateWorker(
            worker = originalWorker,
            targetDate = "1404/03/01",
            targetDateFolderId = dfId
        )

        assertNotEquals(originalId, copiedWorkerId)
        val allWorkers = repository.getWorkersByFolder(folderId).first()
        assertEquals(2, allWorkers.size)
        val copiedWorker = allWorkers.first { it.id == copiedWorkerId }
        assertEquals("علی رضایی (کپی)", copiedWorker.name)
    }
}
