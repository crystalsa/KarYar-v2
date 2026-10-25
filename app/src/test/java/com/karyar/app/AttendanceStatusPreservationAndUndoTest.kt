package com.karyar.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.ui.SnackbarEvent
import com.karyar.app.ui.WorkerViewModel
import com.karyar.app.util.WageCalculator
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
@Config(sdk = [36])
class AttendanceStatusPreservationAndUndoTest {

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
        viewModel = WorkerViewModel(app)
    }

    @Test
    fun testStatusChange_FullToHalfToFull_preservesManualOvertimeNotesAndAllowances() = runBlocking {
        val folder = WorkplaceFolderEntity(name = "کارگاه تست تغییر وضعیت")
        val folderId = db.folderDao().insertFolder(folder)
        val savedFolder = folder.copy(id = folderId)
        viewModel.selectFolder(savedFolder)

        val dateStr = "1405/01/20"
        val dfId = db.dateFolderDao().insertDateFolder(
            DateFolderEntity(folderId = folderId, date = dateStr, dayOfWeek = "پنج‌شنبه")
        )

        // کارگر با دستمزد پایه ۱,۰۰۰,۰۰۰ و اضافه‌کاری پیش‌فرض ۰ در پروفایل
        val worker = WorkerEntity(
            folderId = folderId,
            name = "رضا محمدی",
            role = "آرماتوربند",
            baseDailyWage = 1_000_000L,
            overtimeHours = 0.0,
            overtimeRate = 150_000L,
            foodAllowance = 50_000L,
            foodImpact = "ALLOWANCE"
        )
        val workerId = db.workerDao().insertWorker(worker)
        val savedWorker = worker.copy(id = workerId)

        // ثبت اولیه تمام روز همراه با مقادیر دستی (۳.۵ ساعت اضافه‌کاری، نرخ ۲۰۰,۰۰۰، توضیحات و مزایای سفارشی)
        val manualFullDay = AttendanceEntity(
            folderId = folderId,
            workerId = workerId,
            dateFolderId = dfId,
            date = dateStr,
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            dailyWage = 1_000_000L,
            overtimeHours = 3.5,
            overtimeRate = 200_000L,
            foodAllowance = 90_000L,
            foodImpact = "ALLOWANCE",
            transitAllowance = 40_000L,
            transitImpact = "DEDUCTION",
            hasCustomAllowances = true,
            notes = "بتن‌ریزی شبانه ستون‌ها"
        )
        db.attendanceDao().upsertAttendance(manualFullDay)

        // ۱. تغییر وضعیت از FULL به HALF
        viewModel.setAttendanceStatusSync(savedWorker, dateStr, "HALF")
        val afterHalf = db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr)
        assertNotNull(afterHalf)
        assertEquals(AttendanceStatus.HALF_DAY, afterHalf!!.status)
        assertEquals(4.0, afterHalf.regularHours, 0.001)
        assertEquals(500_000L, afterHalf.dailyWage)
        // مقادیر دستی باید کاملاً حفظ شده باشند
        assertEquals(3.5, afterHalf.overtimeHours, 0.001)
        assertEquals(200_000L, afterHalf.overtimeRate)
        assertEquals("بتن‌ریزی شبانه ستون‌ها", afterHalf.notes)
        assertEquals(90_000L, afterHalf.foodAllowance)
        assertEquals(40_000L, afterHalf.transitAllowance)
        assertEquals("DEDUCTION", afterHalf.transitImpact)

        // بررسی صحت محاسبه مالی در وضعیت HALF با اضافه‌کاری دستی
        val halfCalc = WageCalculator.calculateDay(savedWorker, afterHalf)
        assertEquals(500_000L, halfCalc.baseWage)
        assertEquals(700_000L, halfCalc.overtimePay) // 3.5 * 200,000
        assertEquals(90_000L, halfCalc.totalAllowances)
        assertEquals(40_000L, halfCalc.totalDeductions)
        assertEquals(1_250_000L, halfCalc.netPayout)

        // ۲. تغییر وضعیت از HALF به FULL
        viewModel.setAttendanceStatusSync(savedWorker, dateStr, "FULL")
        val afterFullAgain = db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr)
        assertNotNull(afterFullAgain)
        assertEquals(AttendanceStatus.FULL_DAY, afterFullAgain!!.status)
        assertEquals(8.0, afterFullAgain.regularHours, 0.001)
        assertEquals(1_000_000L, afterFullAgain.dailyWage)
        // مقادیر دستی همچنان باید دست‌نخورده باقی مانده باشند
        assertEquals(3.5, afterFullAgain.overtimeHours, 0.001)
        assertEquals(200_000L, afterFullAgain.overtimeRate)
        assertEquals("بتن‌ریزی شبانه ستون‌ها", afterFullAgain.notes)
        assertEquals(90_000L, afterFullAgain.foodAllowance)
        assertEquals(40_000L, afterFullAgain.transitAllowance)
        assertEquals("DEDUCTION", afterFullAgain.transitImpact)

        val fullCalc = WageCalculator.calculateDay(savedWorker, afterFullAgain)
        assertEquals(1_000_000L, fullCalc.baseWage)
        assertEquals(700_000L, fullCalc.overtimePay)
        assertEquals(1_750_000L, fullCalc.netPayout)
    }

    @Test
    fun testDeleteRecordWithManualData_requiresConfirmationAndSupportsUndo() = runBlocking {
        val folder = WorkplaceFolderEntity(name = "کارگاه تست حذف و بازگردانی")
        val folderId = db.folderDao().insertFolder(folder)
        viewModel.selectFolder(folder.copy(id = folderId))

        val dateStr = "1405/02/10"
        val dfId = db.dateFolderDao().insertDateFolder(
            DateFolderEntity(folderId = folderId, date = dateStr, dayOfWeek = "دوشنبه")
        )

        val worker = WorkerEntity(
            folderId = folderId,
            name = "مهدی کریمی",
            role = "جوشکار",
            baseDailyWage = 1_200_000L,
            overtimeHours = 0.0,
            overtimeRate = 180_000L
        )
        val workerId = db.workerDao().insertWorker(worker)
        val savedWorker = worker.copy(id = workerId)

        // رکورد حضور دارای اضافه‌کاری دستی و توضیحات
        val recordWithManualData = AttendanceEntity(
            folderId = folderId,
            workerId = workerId,
            dateFolderId = dfId,
            date = dateStr,
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            dailyWage = 1_200_000L,
            overtimeHours = 2.5,
            overtimeRate = 180_000L,
            notes = "اضافه‌کاری نصب اسکلت",
            hasCustomAllowances = true
        )
        db.attendanceDao().upsertAttendance(recordWithManualData)

        // ۱. زدن دوباره روی FULL نباید بلافاصله رکورد دارای داده دستی را حذف کند؛ باید دیالوگ تأیید فعال شود
        viewModel.setAttendanceStatusSync(savedWorker, dateStr, "FULL")
        val pending = viewModel.pendingDeleteAttendance.value
        assertNotNull("Confirmation dialog state must be set for record with manual values", pending)
        assertEquals(workerId, pending!!.worker.id)
        assertEquals(dateStr, pending.dateStr)

        // رکورد هنوز در دیتابیس موجود است
        val stillInDb = db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr)
        assertNotNull("Record must not be deleted before user confirmation", stillInDb)
        assertEquals(2.5, stillInDb!!.overtimeHours, 0.001)

        // ۲. انصراف از دیالوگ تأیید
        viewModel.dismissDeleteAttendance()
        assertNull(viewModel.pendingDeleteAttendance.value)
        assertNotNull(db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr))

        // ۳. درخواست مجدد حذف و تأیید حذف (همراه با بررسی رویداد Snackbar دارای دکمه بازگردانی)
        viewModel.setAttendanceStatusSync(savedWorker, dateStr, "FULL")
        assertNotNull(viewModel.pendingDeleteAttendance.value)

        val snackbarDeferred = async { viewModel.snackbarEvents.first() }
        val confirmed = viewModel.confirmDeleteAttendanceSync()
        assertTrue(confirmed)
        assertNull(viewModel.pendingDeleteAttendance.value)

        val snackbarEvent: SnackbarEvent = snackbarDeferred.await()
        assertEquals("بازگردانی", snackbarEvent.actionLabel)
        assertTrue(snackbarEvent.withUndo)

        // رکورد اکنون از دیتابیس حذف شده است
        val afterConfirmDelete = db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr)
        assertNull("Record must be deleted after confirmation", afterConfirmDelete)

        // ۴. زدن دکمه بازگردانی (Undo): رکورد قبلی باید با تمام فیلدهای دستی کامل برگردد
        val undone = viewModel.undoLastAttendanceActionSync()
        assertTrue(undone)

        val restored = db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr)
        assertNotNull("Record must be restored after undo", restored)
        assertEquals(AttendanceStatus.FULL_DAY, restored!!.status)
        assertEquals(8.0, restored.regularHours, 0.001)
        assertEquals(1_200_000L, restored.dailyWage)
        assertEquals(2.5, restored.overtimeHours, 0.001)
        assertEquals(180_000L, restored.overtimeRate)
        assertEquals("اضافه‌کاری نصب اسکلت", restored.notes)
    }

    @Test
    fun testStatusChangeUndo_andDefaultRecordDirectDeleteAndUndo() = runBlocking {
        val folder = WorkplaceFolderEntity(name = "کارگاه تست بازگردانی تغییر وضعیت")
        val folderId = db.folderDao().insertFolder(folder)
        viewModel.selectFolder(folder.copy(id = folderId))

        val dateStr = "1405/03/05"
        val worker = WorkerEntity(
            folderId = folderId,
            name = "سعید نوری",
            role = "بنا",
            baseDailyWage = 900_000L
        )
        val workerId = db.workerDao().insertWorker(worker)
        val savedWorker = worker.copy(id = workerId)

        // ۱. ایجاد رکورد پیش‌فرض بدون مقدار دستی (FULL)
        viewModel.setAttendanceStatusSync(savedWorker, dateStr, "FULL")
        val initial = db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr)
        assertNotNull(initial)
        assertEquals(AttendanceStatus.FULL_DAY, initial!!.status)
        assertFalse(WageCalculator.hasManualAttendanceValues(savedWorker, initial))

        // ۲. تغییر وضعیت به HALF و سپس بازگردانی (Undo) به FULL
        viewModel.setAttendanceStatusSync(savedWorker, dateStr, "HALF")
        assertEquals(
            AttendanceStatus.HALF_DAY,
            db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr)?.status
        )

        assertTrue(viewModel.undoLastAttendanceActionSync())
        val restoredToFull = db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr)
        assertNotNull(restoredToFull)
        assertEquals(AttendanceStatus.FULL_DAY, restoredToFull!!.status)
        assertEquals(900_000L, restoredToFull.dailyWage)

        // ۳. زدن دوباره روی FULL برای رکورد بدون مقدار دستی: حذف مستقیم و سپس بازگردانی (Undo)
        viewModel.setAttendanceStatusSync(savedWorker, dateStr, "FULL")
        assertNull("Default record without manual values deletes directly", viewModel.pendingDeleteAttendance.value)
        assertNull(db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr))

        assertTrue(viewModel.undoLastAttendanceActionSync())
        val restoredAfterDelete = db.attendanceDao().getAttendanceForWorkerAndDate(workerId, dateStr)
        assertNotNull(restoredAfterDelete)
        assertEquals(AttendanceStatus.FULL_DAY, restoredAfterDelete!!.status)
        assertEquals(900_000L, restoredAfterDelete.dailyWage)
    }
}
