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
     * Formats a single CSV row by passing every cell through [escapeCsv] so no cell
     * (whether static title/header or dynamic value) can bypass escaping.
     */
    fun formatRow(vararg cells: Any?): String {
        return cells.joinToString(",") { escapeCsv(it) } + "\n"
    }

    private fun Appendable.writeRow(vararg cells: Any?) {
        append(formatRow(*cells))
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
                writer.writeRow("سامانه مدیریت جامع کارگران")
                writer.writeRow("پروژه", projectName)
                writer.writeRow("تاریخ صدور خروجی", JalaliCalendar.todayString())
                writer.append("\n")

                // -------------------------------------------------------------
                // Section 1: Performance Summary (تسویه حساب و کارکرد پرسنل)
                // -------------------------------------------------------------
                writer.writeRow("■ خلاصه کارکرد و تسویه حساب هر کارگر")
                writer.writeRow(
                    "ردیف",
                    "شناسه کارگر",
                    "نام کارگر",
                    "شغل / تخصص",
                    "تفکیک وضعیت کارکرد",
                    "تعداد شیفت (روز)",
                    "ساعات عادی (ساعت)",
                    "ساعت کار ساعتی (ساعت)",
                    "اضافه کاری (ساعت)",
                    "دستمزد پایه (تومان)",
                    "دستمزد ساعتی (تومان)",
                    "اضافه کاری (تومان)",
                    "کمک‌هزینه‌ها (تومان)",
                    "سایر مزایا (تومان)",
                    "کسورات (تومان)",
                    "سایر کسورات (تومان)",
                    "سهم هزینه‌های گروهی (تومان)",
                    "خالص دریافتی نهایی (تومان)",
                    "وضعیت بدهی (تومان)"
                )

                for ((index, p) in performances.withIndex()) {
                    val netCol: Any = if (p.debtAmount > 0L) {
                        "بدهکار: ${Formatters.formatCurrency(p.debtAmount)}"
                    } else {
                        p.netPayout
                    }
                    val debtCol = if (p.debtAmount > 0L) {
                        "بدهکار: ${Formatters.formatCurrency(p.debtAmount)}"
                    } else {
                        "۰ تومان"
                    }
                    writer.writeRow(
                        index + 1,
                        p.worker.id,
                        p.worker.name,
                        p.worker.role,
                        p.formatWorkSummary(),
                        p.totalShifts,
                        Formatters.formatHours(p.regularHours),
                        Formatters.formatHours(p.hourlyHours),
                        Formatters.formatHours(p.overtimeHours),
                        p.baseWageTotal,
                        p.hourlyPayTotal,
                        p.overtimePayTotal,
                        p.totalAllowances,
                        p.otherAllowanceTotal,
                        p.totalDeductions,
                        p.otherDeductionTotal,
                        p.groupExpenseShare,
                        netCol,
                        debtCol
                    )
                }
                writer.append("\n")

                // -------------------------------------------------------------
                // Section 2: Attendance Logs (ثبت تردد روزانه)
                // -------------------------------------------------------------
                writer.writeRow("■ گزارش ثبت ورود و خروج و وضعیت روزهای کاری")
                writer.writeRow(
                    "ردیف",
                    "تاریخ",
                    "شناسه کارگر",
                    "نام کارگر",
                    "وضعیت حضور",
                    "ساعت ورود",
                    "ساعت خروج",
                    "ساعات عادی",
                    "ساعت کار ساعتی",
                    "ساعات اضافه کار",
                    "دستمزد روزانه (تومان)",
                    "دستمزد ساعتی (تومان)",
                    "محل کار",
                    "کارفرما",
                    "سرکارگر",
                    "توضیحات"
                )

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

                    writer.writeRow(
                        index + 1,
                        att.date,
                        att.workerId,
                        wName,
                        statusStr,
                        att.entryTime,
                        att.exitTime,
                        Formatters.formatHours(att.regularHours),
                        Formatters.formatHours(att.hourlyHours),
                        Formatters.formatHours(att.overtimeHours),
                        att.dailyWage,
                        att.hourlyWage,
                        att.workplaceName,
                        att.employerName,
                        att.foremanName,
                        att.notes
                    )
                }
                writer.append("\n")

                // -------------------------------------------------------------
                // Section 3: Expenses (هزینه‌های ثبت‌شده پروژه)
                // -------------------------------------------------------------
                if (expenses.isNotEmpty()) {
                    writer.writeRow("■ صورت هزینه‌های کارگاه و پروژه")
                    writer.writeRow(
                        "ردیف",
                        "تاریخ",
                        "عنوان هزینه",
                        "دسته‌بندی",
                        "نوع تسهیم",
                        "کارگر منتسب",
                        "مبلغ (تومان)",
                        "توضیحات"
                    )
                    for ((index, exp) in expenses.withIndex()) {
                        val scopeStr = if (exp.scope == "GROUP") "سهم گروهی (تقسیم بین کل کارگران)" else "اختصاصی فرد"
                        val assignedWorker = exp.workerName ?: "—"
                        writer.writeRow(
                            index + 1,
                            exp.date,
                            exp.title,
                            exp.category,
                            scopeStr,
                            assignedWorker,
                            exp.amount,
                            exp.notes
                        )
                    }
                    writer.append("\n")
                }

                // -------------------------------------------------------------
                // Section 4: Analytics Totals (تطابق کامل با داشبورد مالی)
                // -------------------------------------------------------------
                if (analytics != null) {
                    writer.writeRow("■ خلاصه آمار و تعهدات کل کارگاه")
                    writer.writeRow("شاخص", "مقدار")
                    writer.writeRow("تعداد کل کارگران", analytics.totalWorkersCount)
                    writer.writeRow("تعداد پرسنل فعال", analytics.activeWorkersCount)
                    writer.writeRow("تعداد روزهای کاری کارگاه (تقویمی)", analytics.totalWorkDaysCount)
                    writer.writeRow("مجموع کارکرد پرسنل (نفر-روز / شیفت)", analytics.totalPersonDays)
                    writer.writeRow("مجموع ساعات کارکرد عادی", Formatters.formatHours(analytics.totalWorkHours))
                    writer.writeRow("مجموع ساعات اضافه کاری", Formatters.formatHours(analytics.totalOvertimeHours))
                    writer.writeRow("مجموع دستمزد پایه پرداخت‌شده (تومان)", analytics.totalWagesPaid)
                    writer.writeRow("مجموع اضافه کاری و ساعتی (تومان)", analytics.totalOvertimePaid + analytics.totalHourlyPaid)
                    writer.writeRow("کل هزینه‌های جانبی کارگاه (تومان)", analytics.grandTotalExpenses)
                    writer.writeRow("مجموع کل مخارج و هزینه‌های پروژه (تومان)", analytics.grandTotalProjectCost)
                    writer.writeRow("مجموع بدهی کارگران (تومان)", analytics.totalWorkerDebt)
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
