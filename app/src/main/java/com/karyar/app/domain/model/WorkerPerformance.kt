package com.karyar.app.domain.model

import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.util.Formatters

data class WorkerPerformance(
    val worker: WorkerEntity,
    val totalShifts: Int,
    val regularHours: Double,
    val hourlyHours: Double = 0.0,
    val overtimeHours: Double,
    val baseWageTotal: Long,
    val hourlyPayTotal: Long = 0L,
    val overtimePayTotal: Long,

    val fullDaysCount: Int = 0,
    val halfDaysCount: Int = 0,
    val hourlyDaysCount: Int = 0,
    val absentDaysCount: Int = 0,

    // Allowances (افزایشی - اضافه به دریافتی شخص مانند کمک هزینه)
    val totalAllowances: Long = 0L,
    val transitAllowanceTotal: Long = 0L,
    val foodAllowanceTotal: Long = 0L,
    val accommodationAllowanceTotal: Long = 0L,
    val medicalAllowanceTotal: Long = 0L,

    // Deductions (کاهشی - کسورات از حقوق شخص مانند سهم اسکان/غذا)
    val totalDeductions: Long = 0L,
    val transitDeductionTotal: Long = 0L,
    val foodDeductionTotal: Long = 0L,
    val accommodationDeductionTotal: Long = 0L,
    val medicalDeductionTotal: Long = 0L,

    val groupExpenseShare: Long = 0L,
    val netPayoutBeforeGroup: Long = 0L,
    val netPayout: Long
) {
    val individualExpensesTotal: Long
        get() = totalAllowances + totalDeductions

    /**
     * فرمت‌کننده دقیق و بدون اختصار کارکرد پرسنل بر اساس قوانین پروژه:
     * - برای کارگران فقط تمام‌روز: صرفاً «X روز» بدون ذکر ساعت
     * - برای کارکرد مختلط یا مشخص: تفکیک دقیق هر وضعیت با متن کامل بدون کلمات مخفف:
     *   مثلاً: (۱ روز: تمام‌روز) (۲ روز: نصف‌روز) (۱۶ ساعت: ساعتی)
     */
    fun formatWorkSummary(): String {
        if (totalShifts == 0 && hourlyHours == 0.0) return "۰ روز"
        val isPureFullDays = fullDaysCount > 0 && halfDaysCount == 0 && hourlyDaysCount == 0 && hourlyHours == 0.0
        if (isPureFullDays) {
            return "${Formatters.toPersianDigits(fullDaysCount)} روز"
        }
        val parts = mutableListOf<String>()
        if (fullDaysCount > 0) {
            parts.add("(${Formatters.toPersianDigits(fullDaysCount)} روز: تمام‌روز)")
        }
        if (halfDaysCount > 0) {
            parts.add("(${Formatters.toPersianDigits(halfDaysCount)} روز: نصف‌روز)")
        }
        if (hourlyDaysCount > 0 || hourlyHours > 0.0) {
            val hStr = Formatters.toPersianDigits(hourlyHours.toString().removeSuffix(".0"))
            parts.add("(${hStr} ساعت: ساعتی)")
        }
        return if (parts.isNotEmpty()) parts.joinToString(" ") else "${Formatters.toPersianDigits(totalShifts)} روز"
    }
}
