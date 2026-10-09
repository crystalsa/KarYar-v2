package com.karyar.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.domain.model.DashboardAnalytics
import com.karyar.app.domain.model.WorkerPerformance
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExcelExportUtil {

    /**
     * Helper to safely escape CSV fields according to RFC 4180 and prevent CSV injection.
     * Preserves exact user value (no trim).
     * Guards formula injection (=, +, -, @, \t, \r) while preserving purely numeric values (like -150000, -5) and "-" placeholder.
     */
    fun escapeCsv(value: Any?): String {
        if (value == null) return "\"\""
        var str = value.toString()
        val isNumeric = isNumericString(str)
        val isPlaceholder = str == "-"
        if (!isNumeric && !isPlaceholder && str.isNotEmpty()) {
            val firstChar = str[0]
            if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
                str = "'$str"
            }
        }
        val escaped = str.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun isNumericString(str: String): Boolean {
        if (str.isEmpty() || str == "-" || str == "+") return false
        val trimmed = str.trim()
        if (trimmed.isEmpty() || trimmed == "-" || trimmed == "+") return false
        return trimmed.toDoubleOrNull() != null
    }

    /**
     * Exports full project records to a properly formatted, injection-safe CSV file with UTF-8 BOM.
     * Compatible with Microsoft Excel, LibreOffice Calc, and Google Sheets.
     */
    fun exportToExcelCsv(
        context: Context,
        workers: List<WorkerEntity>,
        attendanceList: List<AttendanceEntity>,
        performances: List<WorkerPerformance>,
        projectName: String = "پروژه کارگاهی",
        expenses: List<ExpenseEntity> = emptyList(),
        analytics: DashboardAnalytics? = null
    ): File {
        val fileName = "گزارش_کارگران_${System.currentTimeMillis()}.csv"
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM (0xEF, 0xBB, 0xBF) so Excel correctly recognizes Persian characters
            fos.write(0xEF)
            fos.write(0xBB)
            fos.write(0xBF)

            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Header & Title
                writer.append("سامانه مدیریت جامع کارگران\n")
                writer.append("پروژه,${escapeCsv(projectName)}\n")
                writer.append("تاریخ صدور خروجی,${escapeCsv(JalaliCalendar.todayString())}\n\n")

                // -------------------------------------------------------------
                // Section 1: Performance Summary (تسویه حساب و کارکرد پرسنل)
                // -------------------------------------------------------------
                writer.append("=== خلاصه کارکرد و تسویه حساب هر کارگر ===\n")
                writer.append("ردیف,شناسه کارگر,نام کارگر,شغل / تخصص,تفکیک وضعیت کارکرد,تعداد شیفت (روز),ساعات عادی (ساعت),ساعت کار ساعتی (ساعت),اضافه کاری (ساعت),دستمزد پایه (تومان),دستمزد ساعتی (تومان),اضافه کاری (تومان),کمک‌هزینه‌ها (تومان),کسورات (تومان),سهم هزینه‌های گروهی (تومان),خالص دریافتی نهایی (تومان)\n")

                for ((index, p) in performances.withIndex()) {
                    writer.append("${index + 1},")
                    writer.append("${p.worker.id},")
                    writer.append("${escapeCsv(p.worker.name)},")
                    writer.append("${escapeCsv(p.worker.role)},")
                    writer.append("${escapeCsv(p.formatWorkSummary())},")
                    writer.append("${p.totalShifts},")
                    writer.append("${p.regularHours},")
                    writer.append("${p.hourlyHours},")
                    writer.append("${p.overtimeHours},")
                    writer.append("${p.baseWageTotal},")
                    writer.append("${p.hourlyPayTotal},")
                    writer.append("${p.overtimePayTotal},")
                    writer.append("${p.totalAllowances},")
                    writer.append("${p.totalDeductions},")
                    writer.append("${p.groupExpenseShare},")
                    writer.append("${p.netPayout}\n")
                }
                writer.append("\n")

                // -------------------------------------------------------------
                // Section 2: Attendance Logs (ثبت تردد روزانه)
                // -------------------------------------------------------------
                writer.append("=== گزارش ثبت ورود و خروج و وضعیت روزهای کاری ===\n")
                writer.append("ردیف,تاریخ,شناسه کارگر,نام کارگر,وضعیت حضور,ساعت ورود,ساعت خروج,ساعات عادی,ساعت کار ساعتی,ساعات اضافه کار,دستمزد روزانه (تومان),دستمزد ساعتی (تومان),محل کار,کارفرما,سرکارگر,توضیحات\n")

                // Rule: Worker identity is strictly worker.id
                val workerMap = workers.associateBy { it.id }

                for ((index, att) in attendanceList.withIndex()) {
                    val w = workerMap[att.workerId]
                    val wName = w?.name ?: "کارگر #${att.workerId}"
                    val statusStr = when (att.status) {
                        AttendanceStatus.FULL_DAY -> "تمام روز"
                        AttendanceStatus.HALF_DAY -> "نصف روز"
                        AttendanceStatus.HOURLY -> "ساعتی"
                        AttendanceStatus.ABSENT -> "غایب"
                    }

                    writer.append("${index + 1},")
                    writer.append("${escapeCsv(att.date)},")
                    writer.append("${att.workerId},")
                    writer.append("${escapeCsv(wName)},")
                    writer.append("${escapeCsv(statusStr)},")
                    writer.append("${escapeCsv(att.entryTime)},")
                    writer.append("${escapeCsv(att.exitTime)},")
                    writer.append("${att.regularHours},")
                    writer.append("${att.hourlyHours},")
                    writer.append("${att.overtimeHours},")
                    writer.append("${att.dailyWage},")
                    writer.append("${att.hourlyWage},")
                    writer.append("${escapeCsv(att.workplaceName)},")
                    writer.append("${escapeCsv(att.employerName)},")
                    writer.append("${escapeCsv(att.foremanName)},")
                    writer.append("${escapeCsv(att.notes)}\n")
                }
                writer.append("\n")

                // -------------------------------------------------------------
                // Section 3: Expenses (هزینه‌های ثبت‌شده پروژه)
                // -------------------------------------------------------------
                if (expenses.isNotEmpty()) {
                    writer.append("=== صورت هزینه‌های کارگاه و پروژه ===\n")
                    writer.append("ردیف,تاریخ,عنوان هزینه,دسته‌بندی,نوع تسهیم,کارگر منتسب,مبلغ (تومان),توضیحات\n")
                    for ((index, exp) in expenses.withIndex()) {
                        val scopeStr = if (exp.scope == "GROUP") "سهم گروهی (تقسیم بین کل کارگران)" else "اختصاصی فرد"
                        val assignedWorker = exp.workerName ?: "-"
                        writer.append("${index + 1},")
                        writer.append("${escapeCsv(exp.date)},")
                        writer.append("${escapeCsv(exp.title)},")
                        writer.append("${escapeCsv(exp.category)},")
                        writer.append("${escapeCsv(scopeStr)},")
                        writer.append("${escapeCsv(assignedWorker)},")
                        writer.append("${exp.amount},")
                        writer.append("${escapeCsv(exp.notes)}\n")
                    }
                    writer.append("\n")
                }

                // -------------------------------------------------------------
                // Section 4: Analytics Totals (تطابق کامل با داشبورد مالی)
                // -------------------------------------------------------------
                if (analytics != null) {
                    writer.append("=== خلاصه آمار و تعهدات کل کارگاه ===\n")
                    writer.append("شاخص,مقدار\n")
                    writer.append("تعداد کل کارگران,${analytics.totalWorkersCount}\n")
                    writer.append("تعداد پرسنل فعال,${analytics.activeWorkersCount}\n")
                    writer.append("تعداد روزهای کاری کارگاه (تقویمی),${analytics.totalWorkDaysCount}\n")
                    writer.append("مجموع کارکرد پرسنل (نفر-روز / شیفت),${analytics.totalPersonDays}\n")
                    writer.append("مجموع ساعات کارکرد عادی,${analytics.totalWorkHours}\n")
                    writer.append("مجموع ساعات اضافه کاری,${analytics.totalOvertimeHours}\n")
                    writer.append("مجموع دستمزد پایه پرداخت‌شده (تومان),${analytics.totalWagesPaid}\n")
                    writer.append("مجموع اضافه کاری و ساعتی (تومان),${analytics.totalOvertimePaid + analytics.totalHourlyPaid}\n")
                    writer.append("کل هزینه‌های جانبی کارگاه (تومان),${analytics.grandTotalExpenses}\n")
                    writer.append("مجموع کل مخارج و هزینه‌های پروژه (تومان),${analytics.grandTotalProjectCost}\n")
                }

                writer.flush()
            }
        }
        return file
    }

    /**
     * Share exported file via standard Android share sheet
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }
}
