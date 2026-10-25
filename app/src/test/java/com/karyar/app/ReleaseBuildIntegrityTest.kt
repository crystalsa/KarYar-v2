package com.karyar.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.core.content.res.ResourcesCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.converter.Converters
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.domain.model.DashboardAnalytics
import com.karyar.app.util.ExcelExportUtil
import com.karyar.app.util.PdfExportUtil
import com.karyar.app.util.WageCalculator
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

/**
 * سایه (Shadow) سبک برای PdfDocument در محیط JVM Robolectric که امکان اجرای کامل
 * کدهای رسم Canvas، فونت وزیرمتن و تولید فایل PDF در PdfExportUtil را بدون نیاز به کتابخانه بومی فراهم می‌کند.
 */
@Implements(PdfDocument::class)
class ShadowPdfDocument {
    private val pages = mutableListOf<PdfDocument.PageInfo>()

    @Implementation
    protected fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        val bitmap = Bitmap.createBitmap(
            pageInfo.pageWidth.coerceAtLeast(1),
            pageInfo.pageHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        pages.add(pageInfo)
        return ReflectionHelpers.callConstructor(
            PdfDocument.Page::class.java,
            ClassParameter.from(Canvas::class.java, canvas),
            ClassParameter.from(PdfDocument.PageInfo::class.java, pageInfo)
        )
    }

    @Implementation
    protected fun finishPage(page: PdfDocument.Page) {
        // Page finished
    }

    @Implementation
    protected fun writeTo(out: OutputStream) {
        val header = "%PDF-1.4\n%KarYar Test PDF (${pages.size} pages)\n%%EOF\n"
        out.write(header.toByteArray(Charsets.UTF_8))
    }

    @Implementation
    protected fun close() {
        pages.clear()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [ShadowPdfDocument::class])
class ReleaseBuildIntegrityTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `room database, type converters, entities, PDF with Vazirmatn font, and CSV export work end to end`() = runBlocking {
        // ۱. بررسی TypeConverter برای همه مقادیر AttendanceStatus
        val converters = Converters()
        for (status in AttendanceStatus.entries) {
            val serialized = converters.fromAttendanceStatus(status)
            val restored = converters.toAttendanceStatus(serialized)
            assertEquals(status, restored)
        }

        // ۲. بررسی ذخیره و بازیابی کامل در Room (پروژه، روز کاری، کارگر، حضور و غیاب و هزینه)
        val folderId = db.folderDao().insertFolder(
            WorkplaceFolderEntity(
                name = "پروژه برج میلاد",
                employerName = "شرکت توسعه ابنیه",
                foremanName = "مهندس کریمی"
            )
        )
        val dateFolderId = db.dateFolderDao().insertDateFolder(
            DateFolderEntity(
                folderId = folderId,
                date = "1405/02/15",
                epochDay = 19500L,
                dayOfWeek = "دوشنبه",
                title = "بتن‌ریزی فونداسیون"
            )
        )
        val workerId = db.workerDao().insertWorker(
            WorkerEntity(
                folderId = folderId,
                name = "محمدرضا احمدی",
                role = "آرماتوربند",
                baseDailyWage = 1_500_000L,
                overtimeRate = 250_000L,
                transitAllowance = 100_000L
            )
        )
        db.attendanceDao().insertAttendance(
            AttendanceEntity(
                folderId = folderId,
                dateFolderId = dateFolderId,
                workerId = workerId,
                date = "1405/02/15",
                epochDay = 19500L,
                status = AttendanceStatus.FULL_DAY,
                regularHours = 8.0,
                overtimeHours = 2.0,
                dailyWage = 1_500_000L,
                overtimeRate = 250_000L,
                transitAllowance = 100_000L,
                hasCustomAllowances = true
            )
        )
        db.expenseDao().insertExpense(
            ExpenseEntity(
                folderId = folderId,
                title = "ناهار کارگاه",
                category = "FOOD",
                scope = "GROUP",
                impactType = "DEDUCTION",
                amount = 200_000L,
                date = "1405/02/15",
                epochDay = 19500L
            )
        )

        val workers = db.workerDao().getWorkersByFolder(folderId).first()
        val attendances = db.attendanceDao().getAttendanceByFolder(folderId).first()
        val expenses = db.expenseDao().getExpensesByFolder(folderId).first()

        assertEquals(1, workers.size)
        assertEquals(1, attendances.size)
        assertEquals(AttendanceStatus.FULL_DAY, attendances.first().status)
        assertEquals(1, expenses.size)

        // ۳. بررسی محاسبات مالی
        val summary = WageCalculator.calculateFinancialSummary(
            workers = workers,
            attendances = attendances,
            expenses = expenses,
            todayStr = "1405/02/15"
        )
        // 1,500,000 (روزانه) + 500,000 (۲ ساعت اضافه‌کاری) + 100,000 (ایاب و ذهاب) - 200,000 (هزینه گروهی) = 1,900,000
        assertEquals(1_900_000L, summary.grandTotalProjectCost)

        val analytics = DashboardAnalytics(
            totalWorkersCount = workers.size,
            activeWorkersCount = workers.count { it.isActive },
            todayAttendanceCount = summary.todayAttendanceCount,
            totalWorkDaysCount = summary.totalWorkDaysCount,
            totalPersonDays = summary.workerPerformances.sumOf { it.totalShifts },
            totalWorkHours = summary.totalWorkHours,
            totalOvertimeHours = summary.totalOvertimeHours,
            totalWagesPaid = summary.totalBaseWagesPaid,
            totalHourlyPaid = summary.totalHourlyPaid,
            totalOvertimePaid = summary.totalOvertimePaid,
            totalTransitExpenses = summary.totalTransitExpenses,
            totalAccommodationExpenses = summary.totalAccommodationExpenses,
            totalAccommodationDays = summary.totalAccommodationDays,
            totalFoodExpenses = summary.totalFoodExpenses,
            totalMedicalExpenses = summary.totalMedicalExpenses,
            totalOtherExpenses = summary.totalOtherExpenses,
            totalIndividualExpenses = summary.totalIndividualExpenses,
            totalGroupExpenses = summary.totalGroupExpenses,
            grandTotalExpenses = summary.grandTotalExpenses,
            grandTotalProjectCost = summary.grandTotalProjectCost,
            totalWorkerDebt = summary.totalWorkerDebt
        )

        // ۴. بررسی بارگذاری فونت وزیرمتن از منابع اندروید
        val vazirRegular = ResourcesCompat.getFont(context, R.font.vazirmatn)
        val vazirBold = ResourcesCompat.getFont(context, R.font.vazirmatn_bold)
        assertNotNull("فونت وزیرمتن (R.font.vazirmatn) باید بارگذاری شود", vazirRegular)
        assertNotNull("فونت وزیرمتن ضخیم (R.font.vazirmatn_bold) باید بارگذاری شود", vazirBold)

        // ۵. بررسی تولید فایل PDF
        val pdfFile = PdfExportUtil.exportToPdf(
            context = context,
            projectName = "پروژه برج میلاد",
            employerName = "شرکت توسعه ابنیه",
            foremanName = "مهندس کریمی",
            analytics = analytics,
            performances = summary.workerPerformances,
            expenses = expenses
        )
        assertTrue("فایل PDF باید ایجاد شود", pdfFile.exists())
        assertTrue("حجم فایل PDF باید بزرگ‌تر از صفر باشد", pdfFile.length() > 10L)
        val pdfHeader = pdfFile.inputStream().use { stream ->
            val buf = ByteArray(5)
            stream.read(buf)
            String(buf, Charsets.US_ASCII)
        }
        assertEquals("%PDF-", pdfHeader)

        // ۶. بررسی تولید فایل CSV/Excel
        val csvFile = ExcelExportUtil.exportToExcelCsv(
            context = context,
            workers = workers,
            attendanceList = attendances,
            performances = summary.workerPerformances,
            projectName = "پروژه برج میلاد",
            expenses = expenses,
            analytics = analytics
        )
        assertTrue("فایل CSV باید ایجاد شود", csvFile.exists())
        val csvContent = csvFile.readText(Charsets.UTF_8)
        assertTrue(csvContent.contains("محمدرضا احمدی"))
        assertTrue(csvContent.contains("پروژه برج میلاد"))
        assertTrue(csvContent.contains("ناهار کارگاه"))

        // ۷. در صورت وجود APK ریلیز، بررسی حفظ فونت وزیرمتن و عدم حذف آن توسط Resource Shrinker
        val releaseApk = listOf(
            File("build/outputs/apk/release/app-release.apk"),
            File("app/build/outputs/apk/release/app-release.apk")
        ).firstOrNull { it.exists() }
        if (releaseApk != null) {
            ZipFile(releaseApk).use { zip ->
                val ttfEntries = zip.entries().asSequence().filter { it.name.endsWith(".ttf") }.toList()
                assertEquals("هر دو فایل فونت وزیرمتن باید در APK ریلیز حفظ شوند", 2, ttfEntries.size)
                for (entry in ttfEntries) {
                    assertTrue(
                        "فونت ${entry.name} نباید توسط shrinkResources خالی شود (حجم فعلی: ${entry.size})",
                        entry.size > 100_000L
                    )
                }
            }
        }
    }

    @Test
    fun `all 33 retained extended icons initialize valid ImageVectors without material-icons-extended`() {
        val icons = listOf(
            Icons.Filled.AccessTime,
            Icons.Filled.Assessment,
            Icons.Filled.AttachMoney,
            Icons.Filled.Badge,
            Icons.Filled.Business,
            Icons.Filled.CalendarMonth,
            Icons.Filled.CalendarToday,
            Icons.Filled.ContentCopy,
            Icons.Filled.Dashboard,
            Icons.Filled.DeleteOutline,
            Icons.Filled.DeleteSweep,
            Icons.Filled.Description,
            Icons.Filled.DirectionsBus,
            Icons.Filled.Engineering,
            Icons.Filled.Event,
            Icons.Filled.ExpandLess,
            Icons.Filled.ExpandMore,
            Icons.Filled.Folder,
            Icons.Filled.FolderOpen,
            Icons.Filled.Groups,
            Icons.Filled.LocalDining,
            Icons.Filled.MedicalServices,
            Icons.Filled.Payments,
            Icons.Filled.People,
            Icons.Filled.PersonAdd,
            Icons.Filled.PictureAsPdf,
            Icons.Filled.PlaylistAdd,
            Icons.Filled.ReceiptLong,
            Icons.Filled.Save,
            Icons.Filled.Smartphone,
            Icons.Filled.TableChart,
            Icons.Filled.TrendingDown,
            Icons.Filled.TrendingUp
        )
        assertEquals(33, icons.size)
        for (icon in icons) {
            assertNotNull(icon)
            assertTrue("Icon ${icon.name} must have vector content", icon.defaultWidth.value > 0f)
        }
    }
}
