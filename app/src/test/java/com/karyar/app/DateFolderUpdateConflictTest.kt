package com.karyar.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.data.repository.DateConflictException
import com.karyar.app.data.repository.WorkerRepository
import com.karyar.app.ui.WorkerViewModel
import com.karyar.app.util.JalaliCalendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DateFolderUpdateConflictTest {

    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var repository: WorkerRepository

    @Before
    fun setUp() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        db = AppDatabase.getDatabase(app)
        repository = WorkerRepository(db)
        repository.clearAllData()
    }

    @Test
    fun updateDateFolder_withoutConflict_updatesFolderAndAllAttendanceDatesAndEpochDays() = runBlocking {
        val folderId = repository.insertFolder(
            WorkplaceFolderEntity(name = "کارگاه تست انتقال بدون تداخل")
        )

        val oldDate = "1405/01/10"
        val newDate = "1405/01/15"
        val oldEpochDay = JalaliCalendar.toEpochDay(oldDate)
        val newEpochDay = JalaliCalendar.toEpochDay(newDate)

        val dfId = repository.insertDateFolder(
            DateFolderEntity(
                folderId = folderId,
                date = oldDate,
                dayOfWeek = JalaliCalendar.getDayOfWeek(oldDate),
                title = "روز کاری اول"
            )
        )

        val w1Id = repository.insertWorker(
            WorkerEntity(folderId = folderId, name = "علی رضایی", role = "بنا", baseDailyWage = 1_200_000L)
        )
        val w2Id = repository.insertWorker(
            WorkerEntity(folderId = folderId, name = "حسین مرادی", role = "آرماتوربند", baseDailyWage = 1_000_000L)
        )

        repository.insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = w1Id,
                dateFolderId = dfId,
                date = oldDate,
                epochDay = oldEpochDay,
                status = AttendanceStatus.FULL_DAY,
                dailyWage = 1_200_000L,
                overtimeHours = 2.0,
                overtimeRate = 180_000L,
                notes = "حضور علی"
            )
        )
        repository.insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = w2Id,
                dateFolderId = dfId,
                date = oldDate,
                epochDay = oldEpochDay,
                status = AttendanceStatus.HALF_DAY,
                regularHours = 4.0,
                dailyWage = 500_000L,
                notes = "حضور حسین"
            )
        )

        val originalFolder = db.dateFolderDao().getDateFolderById(dfId)!!
        repository.updateDateFolder(
            originalFolder.copy(
                date = newDate,
                dayOfWeek = JalaliCalendar.getDayOfWeek(newDate),
                title = "روز کاری منتقل‌شده"
            )
        )

        // ۱. پوشه روز باید تاریخ و epochDay جدید گرفته باشد
        val updatedFolder = db.dateFolderDao().getDateFolderById(dfId)
        assertNotNull(updatedFolder)
        assertEquals(newDate, updatedFolder!!.date)
        assertEquals(newEpochDay, updatedFolder.epochDay)
        assertEquals("روز کاری منتقل‌شده", updatedFolder.title)

        // ۲. همه رکوردهای حضور باید به تاریخ و epochDay جدید منتقل شده باشند و هیچ رکوردی در تاریخ قدیم نمانده باشد
        val oldDateAtts = db.attendanceDao().getAttendanceForDateInFolderSync(folderId, oldDate)
        assertTrue("No attendance records should remain on oldDate", oldDateAtts.isEmpty())

        val newDateAtts = db.attendanceDao().getAttendanceForDateInFolderSync(folderId, newDate)
        assertEquals(2, newDateAtts.size)
        newDateAtts.forEach { att ->
            assertEquals(newDate, att.date)
            assertEquals(newEpochDay, att.epochDay)
            assertEquals(dfId, att.dateFolderId)
        }

        val w1Att = db.attendanceDao().getAttendanceForWorkerAndDate(w1Id, newDate)
        val w2Att = db.attendanceDao().getAttendanceForWorkerAndDate(w2Id, newDate)
        assertNotNull(w1Att)
        assertNotNull(w2Att)
        assertEquals("حضور علی", w1Att!!.notes)
        assertEquals(2.0, w1Att.overtimeHours, 0.001)
        assertEquals("حضور حسین", w2Att!!.notes)
    }

    @Test
    fun updateDateFolder_withConflict_throwsDateConflictExceptionAndDoesNotModifyAnyData() = runBlocking {
        val folderId = repository.insertFolder(
            WorkplaceFolderEntity(name = "کارگاه تست تداخل تاریخ")
        )

        val oldDate = "1405/02/01"
        val targetDate = "1405/02/05"
        val oldEpochDay = JalaliCalendar.toEpochDay(oldDate)
        val targetEpochDay = JalaliCalendar.toEpochDay(targetDate)

        val dfOldId = repository.insertDateFolder(
            DateFolderEntity(
                folderId = folderId,
                date = oldDate,
                dayOfWeek = JalaliCalendar.getDayOfWeek(oldDate),
                title = "روز مبدا"
            )
        )
        val dfTargetId = repository.insertDateFolder(
            DateFolderEntity(
                folderId = folderId,
                date = targetDate,
                dayOfWeek = JalaliCalendar.getDayOfWeek(targetDate),
                title = "روز مقصد"
            )
        )

        // دو کارگر در روز مبدا داریم، اما فقط کارگر اول در روز مقصد از قبل رکورد حضور دارد
        val w1Id = repository.insertWorker(
            WorkerEntity(folderId = folderId, name = "رضا کریمی", role = "جوشکار", baseDailyWage = 1_100_000L)
        )
        val w2Id = repository.insertWorker(
            WorkerEntity(folderId = folderId, name = "سعید احمدی", role = "لوله‌کش", baseDailyWage = 950_000L)
        )

        repository.insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = w1Id,
                dateFolderId = dfOldId,
                date = oldDate,
                epochDay = oldEpochDay,
                status = AttendanceStatus.FULL_DAY,
                dailyWage = 1_100_000L,
                notes = "رکورد رضا در مبدا"
            )
        )
        repository.insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = w2Id,
                dateFolderId = dfOldId,
                date = oldDate,
                epochDay = oldEpochDay,
                status = AttendanceStatus.FULL_DAY,
                dailyWage = 950_000L,
                notes = "رکورد سعید در مبدا"
            )
        )

        // رکورد قبلی رضا در تاریخ مقصد (عامل تداخل workerId + date)
        repository.insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = w1Id,
                dateFolderId = dfTargetId,
                date = targetDate,
                epochDay = targetEpochDay,
                status = AttendanceStatus.HALF_DAY,
                regularHours = 4.0,
                dailyWage = 550_000L,
                notes = "رکورد قبلی رضا در مقصد"
            )
        )

        val dfOldBefore = db.dateFolderDao().getDateFolderById(dfOldId)!!

        // تلاش برای انتقال روز مبدا به تاریخ مقصد باید DateConflictException پرتاب کند
        try {
            repository.updateDateFolder(
                dfOldBefore.copy(
                    date = targetDate,
                    dayOfWeek = JalaliCalendar.getDayOfWeek(targetDate),
                    title = "عنوان تغییریافته"
                )
            )
            fail("Expected DateConflictException to be thrown due to existing attendance on target date")
        } catch (e: DateConflictException) {
            assertEquals(targetDate, e.targetDate)
            assertEquals(1, e.conflictingWorkers.size)
            assertEquals(w1Id, e.conflictingWorkers.first().id)
            assertEquals("رضا کریمی", e.conflictingWorkers.first().name)
            assertEquals("در تاریخ مقصد برای ۱ کارگر حضور ثبت شده است", e.message)
        }

        // ۱. پوشه روز مبدا نباید تغییر کرده باشد
        val dfOldAfter = db.dateFolderDao().getDateFolderById(dfOldId)
        assertNotNull(dfOldAfter)
        assertEquals(oldDate, dfOldAfter!!.date)
        assertEquals(oldEpochDay, dfOldAfter.epochDay)
        assertEquals("روز مبدا", dfOldAfter.title)

        // ۲. هیچ‌یک از رکوردهای حضور مبدا (حتی کارگر دوم که تداخل مستقیم نداشت!) نباید منتقل شده باشد
        val w1OldAtt = db.attendanceDao().getAttendanceForWorkerAndDate(w1Id, oldDate)
        val w2OldAtt = db.attendanceDao().getAttendanceForWorkerAndDate(w2Id, oldDate)
        assertNotNull("Worker 1 attendance on oldDate must remain intact", w1OldAtt)
        assertNotNull("Worker 2 attendance on oldDate must NOT partially move to targetDate", w2OldAtt)
        assertEquals(oldDate, w1OldAtt!!.date)
        assertEquals(oldEpochDay, w1OldAtt.epochDay)
        assertEquals(dfOldId, w1OldAtt.dateFolderId)
        assertEquals("رکورد رضا در مبدا", w1OldAtt.notes)

        assertEquals(oldDate, w2OldAtt!!.date)
        assertEquals(oldEpochDay, w2OldAtt.epochDay)
        assertEquals(dfOldId, w2OldAtt.dateFolderId)
        assertEquals("رکورد سعید در مبدا", w2OldAtt.notes)

        // ۳. رکوردهای تاریخ مقصد نیز باید بدون هیچ تغییری دست‌نخورده باقی مانده باشند
        val targetAtts = db.attendanceDao().getAttendanceForDateInFolderSync(folderId, targetDate)
        assertEquals("Target date must still have only the 1 original attendance record", 1, targetAtts.size)
        val w1TargetAtt = targetAtts.first()
        assertEquals(w1Id, w1TargetAtt.workerId)
        assertEquals(targetDate, w1TargetAtt.date)
        assertEquals(targetEpochDay, w1TargetAtt.epochDay)
        assertEquals(dfTargetId, w1TargetAtt.dateFolderId)
        assertEquals("رکورد قبلی رضا در مقصد", w1TargetAtt.notes)
        assertNull(db.attendanceDao().getAttendanceForWorkerAndDate(w2Id, targetDate))
    }

    @Test
    fun viewModel_updateDateFolderWithConflict_setsPendingConflictAndSupportsCancelOrMerge() = runBlocking {
        val viewModel = WorkerViewModel(app)
        val folder = WorkplaceFolderEntity(name = "کارگاه تست ویومدل و ادغام")
        val folderId = repository.insertFolder(folder)
        viewModel.selectFolder(folder.copy(id = folderId))

        val oldDate = "1405/03/10"
        val targetDate = "1405/03/12"

        val dfOldId = repository.insertDateFolder(
            DateFolderEntity(folderId = folderId, date = oldDate, dayOfWeek = "یکشنبه", title = "روز اول")
        )
        val dfTargetId = repository.insertDateFolder(
            DateFolderEntity(folderId = folderId, date = targetDate, dayOfWeek = "سه‌شنبه", title = "روز دوم")
        )

        val w1Id = repository.insertWorker(
            WorkerEntity(folderId = folderId, name = "مجتبی بیات", role = "تأسیسات", baseDailyWage = 1_000_000L)
        )
        val w2Id = repository.insertWorker(
            WorkerEntity(folderId = folderId, name = "امیر حسینی", role = "برق‌کار", baseDailyWage = 1_100_000L)
        )

        repository.insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = w1Id,
                dateFolderId = dfOldId,
                date = oldDate,
                dailyWage = 1_000_000L,
                notes = "مجتبی در روز اول"
            )
        )
        repository.insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = w2Id,
                dateFolderId = dfOldId,
                date = oldDate,
                dailyWage = 1_100_000L,
                notes = "امیر در روز اول"
            )
        )
        repository.insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                workerId = w1Id,
                dateFolderId = dfTargetId,
                date = targetDate,
                dailyWage = 1_000_000L,
                notes = "مجتبی در روز دوم"
            )
        )

        val dfOld = db.dateFolderDao().getDateFolderById(dfOldId)!!
        val result = viewModel.updateDateFolderSync(dfOld.copy(date = targetDate))
        assertTrue(result.isFailure)

        val conflict = viewModel.pendingDateConflict.value
        assertNotNull(conflict)
        assertEquals("در تاریخ مقصد برای ۱ کارگر حضور ثبت شده است", conflict!!.message)
        assertEquals(1, conflict.conflictingWorkers.size)
        assertEquals("مجتبی بیات", conflict.conflictingWorkers.first().name)

        // گزینه لغو: هیچ داده‌ای تغییر نمی‌کند
        viewModel.dismissDateConflict()
        assertNull(viewModel.pendingDateConflict.value)
        assertEquals(2, db.attendanceDao().getAttendanceForDateInFolderSync(folderId, oldDate).size)
        assertEquals(1, db.attendanceDao().getAttendanceForDateInFolderSync(folderId, targetDate).size)

        // تلاش مجدد و انتخاب گزینه ادغام
        viewModel.updateDateFolderSync(dfOld.copy(date = targetDate))
        assertNotNull(viewModel.pendingDateConflict.value)
        assertTrue(viewModel.confirmMergeDateFolderSync())
        assertNull(viewModel.pendingDateConflict.value)

        // پس از ادغام، رکوردهای غیرتکراری (امیر) به تاریخ مقصد منتقل شده و رکورد مقصد (مجتبی) حفظ شده است
        assertTrue(db.attendanceDao().getAttendanceForDateInFolderSync(folderId, oldDate).isEmpty())
        val mergedTargetAtts = db.attendanceDao().getAttendanceForDateInFolderSync(folderId, targetDate)
        assertEquals(2, mergedTargetAtts.size)
        val targetEpoch = JalaliCalendar.toEpochDay(targetDate)
        mergedTargetAtts.forEach { att ->
            assertEquals(targetDate, att.date)
            assertEquals(targetEpoch, att.epochDay)
        }
        val remainingDateFolders = repository.getDateFoldersByFolder(folderId).first()
        assertEquals(1, remainingDateFolders.size)
        assertEquals(targetDate, remainingDateFolders.first().date)
    }
}
