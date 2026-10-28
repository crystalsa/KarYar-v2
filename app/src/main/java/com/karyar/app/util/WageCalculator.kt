package com.karyar.app.util

import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.domain.model.DailyBookkeeping
import com.karyar.app.domain.model.DeletionImpact
import com.karyar.app.domain.model.WorkerPerformance
import kotlin.math.floor

/**
 * WageCalculator is the single source of truth for all wage, overtime,
 * allowance, deduction, shift counting, and net payout calculations in KarYar.
 *
 * Rules:
 * 1. Absent day (قانون قطعی روز غیبت):
 *    - Absence is determined EXCLUSIVELY by AttendanceStatus.ABSENT on AttendanceEntity.status.
 *    - Base daily wage, hourly pay, overtime, and allowances (مزایای افزایشی) are ALL ZERO.
 *    - Deductions (کسورات کاهشی مانند سهم اسکان، ایاب و ذهاب و درمان) EVEN on absent days ARE APPLIED
 *      (کسورات ثابت مانند اسکان یا تعهدات حتی در روز غیبت کسر می‌گردد، ولی دستمزد و مزایا صفر است).
 *    - Absent days do NOT count towards working shifts (isWorkingDay = false).
 * 2. Status determination (تشخیص وضعیت کارکرد):
 *    - Strictly determined by AttendanceEntity.status (FULL_DAY, HALF_DAY, HOURLY, ABSENT).
 *    - The notes field is free-form description and NEVER determines or overrides worker status.
 * 3. Hours and Rates (ساعات کارکرد و نرخ‌ها):
 *    - Worker's overtimeHours and hourlyHours in their profile are initial templates for new records,
 *      NOT defaults during calculation. If an attendance record has 0.0 overtime hours, it means 0.0 overtime.
 *    - Rates (overtimeRate, hourlyWageRate) fallback to the worker profile if not specified in attendance.
 * 4. Half day (نصف روز):
 *    - Base daily wage is half of worker's daily wage: roundToLong(worker.baseDailyWage / 2.0).
 *    - Allowances and deductions apply once per working day.
 * 5. Hourly (ساعتی):
 *    - Hourly pay is roundToLong(hours * rate).
 *    - Overtime is calculated when overtimeHours > 0 and worker is not absent.
 * 6. Zero capping, Worker Debt and Settlement Consistency (قانون هماهنگی دستمزد روزانه با تسویه نهایی و بدهی کارگر):
 *    - DayCalculationResult.netPayout reflects the true net daily balance (gross - totalDeductions),
 *      allowing negative values when daily deductions exceed daily earnings (e.g. absent days with fixed deductions).
 *    - DayCalculationResult.clampedNetPayout provides the >= 0 display value where strictly non-negative display is required.
 *    - This guarantees that the sum of daily net payouts across all days equals the cumulative worker performance!
 *    - قانون تسویه و بدهی کارگر:
 *      قابل پرداخت (netPayout) = max(rawNetPayout, 0)
 *      بدهی کارگر (debtAmount) = max(-rawNetPayout, 0)
 *      اگر کسورات از دستمزد بیشتر باشد، کارگر بدهکار است و بدهی نباید بی‌صدا به صفر تبدیل یا پنهان شود.
 * 7. Unified Financial Engine (مسیر محاسباتی یکپارچه روزانه و کل پروژه):
 *    - calculateFinancialSummary و calculateDailyBookkeeping هر دو از تابع مشترک calculateNetCost استفاده می‌کنند:
 *      خالص هزینه = دستمزد پایه + دستمزد ساعتی + اضافه‌کاری + مزایای افزایشی - کسورات انفرادی - هزینه‌های گروهی کارگاه.
 *    - هیچ‌یک از grandDailyCost و grandTotalProjectCost در صفر قفل (clamp / coerceAtLeast(0)) نمی‌شوند؛
 *      در نتیجه جمع جبری grandDailyCost تمام روزهای پروژه همیشه و در همه سناریوها دقیقاً برابر grandTotalProjectCost است.
 *    - Deductions on absent days are treated identically in analytics, dailyBookkeeping, and workerPerformances.
 * 8. Expenses and Remainder Distribution (هزینه‌ها و باقیمانده تقسیم هزینه‌های گروهی):
 *    - GROUP expenses are divided among workers who worked at least one shift, with the remainder (باقیمانده تقسیم)
 *      deterministically distributed so that sum(groupExpenseShare) == totalGroupExpenses without losing any remainder.
 *    - Workshop group expenses are deducted identically in both daily bookkeeping (grandDailyCost) and project summary (grandTotalProjectCost).
 *    - INDIVIDUAL expenses are added/deducted strictly by workerId (never by workerName).
 */
object WageCalculator {

    const val DEFAULT_OVERTIME_MULTIPLIER: Double = 1.4
    const val MAX_WAGE_AMOUNT: Long = Formatters.MAX_WAGE_AMOUNT
    const val MAX_DAILY_WAGE: Long = Formatters.MAX_DAILY_WAGE
    const val MAX_HOURLY_RATE: Long = Formatters.MAX_HOURLY_RATE
    const val MAX_OVERTIME_RATE: Long = Formatters.MAX_OVERTIME_RATE
    const val MAX_ALLOWANCE_AMOUNT: Long = Formatters.MAX_ALLOWANCE_AMOUNT
    const val MAX_EXPENSE_AMOUNT: Long = 10_000_000_000L

    const val MAX_REGULAR_HOURS: Double = Formatters.MAX_REGULAR_HOURS
    const val MAX_HOURLY_HOURS: Double = Formatters.MAX_HOURLY_HOURS
    const val MAX_OVERTIME_HOURS: Double = Formatters.MAX_OVERTIME_HOURS
    const val MAX_WORK_HOURS: Double = Formatters.MAX_WORK_HOURS

    const val MAX_NAME_LENGTH: Int = Formatters.MAX_NAME_LENGTH

    val VALID_EXPENSE_CATEGORIES = setOf("TRANSIT", "FOOD", "ACCOMMODATION", "MEDICAL", "OTHER")
    val VALID_EXPENSE_SCOPES = setOf("INDIVIDUAL", "GROUP")
    val VALID_EXPENSE_IMPACTS = setOf("ALLOWANCE", "DEDUCTION")

    fun isValidIranianNationalId(input: String): Boolean =
        Formatters.isValidIranianNationalId(input)

    fun validateIranianNationalId(input: String, allowEmpty: Boolean = true): String? =
        Formatters.validateIranianNationalId(input, allowEmpty)

    /**
     * اعتبارسنجی مشخصات کارگر پیش از ذخیره:
     * - نام غیرخالی و حداکثر ۶۰ کاراکتر
     * - عنوان شغل حداکثر ۶۰ کاراکتر
     * - دستمزد روزانه، نرخ ساعتی، نرخ اضافه‌کاری و کمک‌هزینه‌ها بین ۰ تا [MAX_WAGE_AMOUNT]
     * - ساعات ساعتی و اضافه‌کاری بین ۰ تا ۲۴ ساعت
     * - کد ملی (در صورت عدم فعال بودن [allowForeignNationalId]) معتبر طبق الگوریتم ۱۰ رقمی ایران
     */
    fun validateWorker(
        worker: WorkerEntity,
        allowForeignNationalId: Boolean = false
    ): String? {
        Formatters.validateRequiredName(worker.name, "نام کارگر")?.let { return it }
        Formatters.validateOptionalName(worker.role, "عنوان شغل")?.let { return it }
        if (!allowForeignNationalId && worker.nationalId.isNotBlank()) {
            Formatters.validateIranianNationalId(worker.nationalId, allowEmpty = true)?.let { return it }
        }
        Formatters.validateWageAmount(worker.baseDailyWage, "دستمزد روزانه")?.let { return it }
        Formatters.validateWageAmount(worker.baseHourlyWage, "دستمزد ساعتی")?.let { return it }
        Formatters.validateWageAmount(worker.hourlyWageRate, "مبلغ هر ساعت")?.let { return it }
        Formatters.validateWageAmount(worker.overtimeRate, "نرخ اضافه‌کاری")?.let { return it }
        Formatters.validateHours(worker.hourlyHours, "ساعت کار ساعتی", MAX_HOURLY_HOURS)?.let { return it }
        Formatters.validateHours(worker.overtimeHours, "ساعت اضافه‌کاری", MAX_OVERTIME_HOURS)?.let { return it }
        Formatters.validateWageAmount(worker.transitAllowance, "کمک‌هزینه ایاب و ذهاب")?.let { return it }
        Formatters.validateWageAmount(worker.foodAllowance, "کمک‌هزینه خوراک")?.let { return it }
        Formatters.validateWageAmount(worker.accommodationAllowance, "کمک‌هزینه مسکن")?.let { return it }
        Formatters.validateWageAmount(worker.medicalAllowance, "کمک‌هزینه درمان")?.let { return it }
        return null
    }

    /**
     * اعتبارسنجی رکورد حضور و غیاب پیش از ذخیره:
     * - تاریخ شمسی معتبر
     * - ساعت عادی، ساعت کار ساعتی و ساعت اضافه‌کاری بین ۰ تا ۲۴ ساعت
     * - دستمزد روزانه، نرخ ساعتی، نرخ اضافه‌کاری و کمک‌هزینه‌ها بین ۰ تا [MAX_WAGE_AMOUNT]
     */
    fun validateAttendance(attendance: AttendanceEntity): String? {
        JalaliCalendar.validateDateError(attendance.date)?.let { return it }
        Formatters.validateHours(attendance.regularHours, "ساعات کار عادی", MAX_REGULAR_HOURS)?.let { return it }
        Formatters.validateHours(attendance.hourlyHours, "ساعت کارکرد ساعتی", MAX_HOURLY_HOURS)?.let { return it }
        Formatters.validateHours(attendance.overtimeHours, "ساعت اضافه‌کاری", MAX_OVERTIME_HOURS)?.let { return it }
        Formatters.validateWageAmount(attendance.dailyWage, "دستمزد روزانه")?.let { return it }
        Formatters.validateWageAmount(attendance.hourlyWage, "مبلغ هر ساعت")?.let { return it }
        Formatters.validateWageAmount(attendance.hourlyWageRate, "مبلغ هر ساعت")?.let { return it }
        Formatters.validateWageAmount(attendance.overtimeRate, "نرخ اضافه‌کاری")?.let { return it }
        Formatters.validateWageAmount(attendance.transitAllowance, "کمک‌هزینه ایاب و ذهاب")?.let { return it }
        Formatters.validateWageAmount(attendance.foodAllowance, "کمک‌هزینه خوراک")?.let { return it }
        Formatters.validateWageAmount(attendance.accommodationAllowance, "کمک‌هزینه مسکن")?.let { return it }
        Formatters.validateWageAmount(attendance.medicalAllowance, "کمک‌هزینه درمان")?.let { return it }
        Formatters.validateOptionalName(attendance.workplaceName, "نام محل کار")?.let { return it }
        Formatters.validateOptionalName(attendance.employerName, "نام کارفرما")?.let { return it }
        Formatters.validateOptionalName(attendance.foremanName, "نام سرکارگر")?.let { return it }
        return null
    }

    /**
     * اعتبارسنجی رکورد هزینه پیش از ثبت:
     * - عنوان غیرخالی و حداکثر ۶۰ کاراکتر
     * - دسته معتبر (TRANSIT / FOOD / ACCOMMODATION / MEDICAL / OTHER)
     * - نوع معتبر (INDIVIDUAL / GROUP)
     * - نوع تأثیر معتبر (ALLOWANCE / DEDUCTION)
     * - مبلغ بزرگ‌تر از صفر و کوچک‌تر یا مساوی سقف مجاز ([MAX_EXPENSE_AMOUNT])
     * - تاریخ شمسی معتبر
     * - در صورت INDIVIDUAL بودن، انتخاب کارگر اجباری است (`workerId != null && workerId > 0L`)
     */
    fun validateExpense(
        expense: ExpenseEntity,
        availableWorkerIds: Set<Long>? = null
    ): String? {
        if (expense.title.isBlank()) {
            return "عنوان هزینه نمی‌تواند خالی باشد"
        }
        if (expense.title.trim().length > MAX_NAME_LENGTH || expense.title.length > MAX_NAME_LENGTH) {
            return "طول عنوان هزینه نباید بیشتر از ${Formatters.toPersianDigits(MAX_NAME_LENGTH)} کاراکتر باشد"
        }
        val cat = expense.category.trim().uppercase()
        if (cat !in VALID_EXPENSE_CATEGORIES) {
            return "دسته‌بندی هزینه نامعتبر است"
        }
        val scope = expense.scope.trim().uppercase()
        if (scope !in VALID_EXPENSE_SCOPES) {
            return "نوع هزینه باید فردی یا گروهی باشد"
        }
        val impact = expense.impactType.trim().uppercase()
        if (impact !in VALID_EXPENSE_IMPACTS) {
            return "نوع تأثیر هزینه باید افزایشی یا کاهشی باشد"
        }
        if (expense.amount <= 0L) {
            return "مبلغ هزینه باید بیشتر از صفر باشد"
        }
        if (expense.amount > MAX_EXPENSE_AMOUNT) {
            return "مبلغ هزینه نباید بیشتر از ${Formatters.formatCurrency(MAX_EXPENSE_AMOUNT)} باشد"
        }
        val dateErr = JalaliCalendar.validateDateError(expense.date)
        if (dateErr != null) {
            return dateErr
        }
        if (scope == "INDIVIDUAL") {
            val wId = expense.workerId
            if (wId == null || wId <= 0L) {
                return "برای هزینه فردی انتخاب کارگر الزامی است"
            }
            if (availableWorkerIds != null && wId !in availableWorkerIds) {
                return "کارگر انتخاب‌شده در این پروژه وجود ندارد"
            }
        }
        return null
    }

    /**
     * Resolves the effective hourly wage rate for a worker, handling profile fallback.
     */
    fun defaultHourlyRate(worker: WorkerEntity): Long {
        return if (worker.hourlyWageRate > 0L) worker.hourlyWageRate else worker.baseHourlyWage
    }

    /**
     * Parses a status string ("FULL", "HALF", "HOURLY", "ABSENT") into [AttendanceStatus].
     */
    fun parseStatus(status: String): AttendanceStatus = when (status.trim().uppercase()) {
        "FULL", "FULL_DAY" -> AttendanceStatus.FULL_DAY
        "HALF", "HALF_DAY" -> AttendanceStatus.HALF_DAY
        "HOURLY" -> AttendanceStatus.HOURLY
        else -> AttendanceStatus.ABSENT
    }

    /**
     * Checks whether an attendance record contains user-entered manual values
     * (overtime hours/rate, hourly hours/rate, notes, or custom allowances/wages)
     * that would be lost if the record were deleted.
     */
    fun hasManualAttendanceValues(worker: WorkerEntity, att: AttendanceEntity): Boolean {
        val hasOvertime = att.overtimeHours > 0.0 ||
            att.overtimeHours != worker.overtimeHours ||
            (att.overtimeRate > 0L && att.overtimeRate != worker.overtimeRate)

        val hasHourly = att.hourlyHours > 0.0 ||
            (att.hourlyWageRate > 0L && att.hourlyWageRate != defaultHourlyRate(worker) && att.hourlyWageRate != worker.hourlyWageRate)

        val hasNotes = att.notes.isNotBlank()

        val hasCustomBenefits =
            att.transitAllowance != worker.transitAllowance ||
            (att.transitAllowance > 0L && att.transitImpact != worker.transitImpact) ||
            att.foodAllowance != worker.foodAllowance ||
            (att.foodAllowance > 0L && att.foodImpact != worker.foodImpact) ||
            att.accommodationAllowance != worker.accommodationAllowance ||
            (att.accommodationAllowance > 0L && att.accommodationImpact != worker.accommodationImpact) ||
            att.medicalAllowance != worker.medicalAllowance ||
            (att.medicalAllowance > 0L && att.medicalImpact != worker.medicalImpact)

        val expectedDailyWage = when (att.status) {
            AttendanceStatus.FULL_DAY -> worker.baseDailyWage
            AttendanceStatus.HALF_DAY -> roundToLong(worker.baseDailyWage / 2.0)
            else -> 0L
        }
        val hasCustomDailyWage = (att.status == AttendanceStatus.FULL_DAY || att.status == AttendanceStatus.HALF_DAY) &&
            att.dailyWage > 0L && att.dailyWage != expectedDailyWage

        return hasOvertime || hasHourly || hasNotes || hasCustomBenefits || hasCustomDailyWage
    }

    /**
     * Shared status transition helper that recalculates ONLY the status-dependent fields
     * (status, regularHours, dailyWage, hourlyWage, hourlyWageRate, hourlyHours) while
     * preserving user-entered manual fields (overtimeHours, overtimeRate, notes, and benefits/allowances)
     * when [preserveManualFields] is true.
     */
    fun applyStatusFields(
        attendance: AttendanceEntity,
        worker: WorkerEntity,
        targetStatus: AttendanceStatus,
        preserveManualFields: Boolean = true
    ): AttendanceEntity {
        val hourlyRate = defaultHourlyRate(worker)
        val resolvedOvertimeHours = when {
            !preserveManualFields -> if (targetStatus == AttendanceStatus.ABSENT) 0.0 else worker.overtimeHours
            attendance.status == AttendanceStatus.ABSENT && targetStatus != AttendanceStatus.ABSENT && attendance.overtimeHours == 0.0 -> worker.overtimeHours
            else -> attendance.overtimeHours
        }
        val resolvedOvertimeRate = when {
            !preserveManualFields -> if (targetStatus == AttendanceStatus.ABSENT) 0L else worker.overtimeRate
            attendance.status == AttendanceStatus.ABSENT && targetStatus != AttendanceStatus.ABSENT && attendance.overtimeRate == 0L -> worker.overtimeRate
            else -> attendance.overtimeRate
        }

        return when (targetStatus) {
            AttendanceStatus.FULL_DAY -> attendance.copy(
                status = AttendanceStatus.FULL_DAY,
                regularHours = 8.0,
                dailyWage = worker.baseDailyWage,
                hourlyWage = 0L,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                overtimeHours = resolvedOvertimeHours,
                overtimeRate = resolvedOvertimeRate
            )
            AttendanceStatus.HALF_DAY -> attendance.copy(
                status = AttendanceStatus.HALF_DAY,
                regularHours = 4.0,
                dailyWage = roundToLong(worker.baseDailyWage / 2.0),
                hourlyWage = hourlyRate,
                hourlyWageRate = if (preserveManualFields && attendance.hourlyWageRate > 0L) attendance.hourlyWageRate else worker.hourlyWageRate,
                hourlyHours = if (preserveManualFields && attendance.hourlyHours > 0.0) attendance.hourlyHours else worker.hourlyHours,
                overtimeHours = resolvedOvertimeHours,
                overtimeRate = resolvedOvertimeRate
            )
            AttendanceStatus.HOURLY -> {
                val defaultHourlyHours = if (worker.isHourlyEnabled) worker.hourlyHours else (if (worker.hourlyHours > 0) worker.hourlyHours else 8.0)
                val effectiveRate = if (preserveManualFields && attendance.hourlyWageRate > 0L) attendance.hourlyWageRate else hourlyRate
                val effectiveHours = if (preserveManualFields && attendance.hourlyHours > 0.0) attendance.hourlyHours else defaultHourlyHours
                attendance.copy(
                    status = AttendanceStatus.HOURLY,
                    regularHours = 0.0,
                    dailyWage = 0L,
                    hourlyWage = effectiveRate,
                    hourlyWageRate = effectiveRate,
                    hourlyHours = effectiveHours,
                    overtimeHours = resolvedOvertimeHours,
                    overtimeRate = resolvedOvertimeRate
                )
            }
            AttendanceStatus.ABSENT -> attendance.copy(
                status = AttendanceStatus.ABSENT,
                regularHours = 0.0,
                dailyWage = 0L,
                hourlyWage = 0L,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                overtimeHours = resolvedOvertimeHours,
                overtimeRate = resolvedOvertimeRate
            )
        }
    }

    /**
     * Creates a standardized AttendanceEntity initialized with correct wages, hours, and rates
     * according to the worker profile and target attendance status.
     * Centralizes the hourly / overtime / wage logic across worker creation, duplication,
     * day creation, and attendance status toggling.
     */
    fun defaultAttendanceFor(
        worker: WorkerEntity,
        status: AttendanceStatus? = null,
        date: String,
        dateFolderId: Long = 0L,
        folderId: Long = worker.folderId,
        workplaceName: String = "",
        employerName: String = "",
        foremanName: String = "",
        notes: String = ""
    ): AttendanceEntity {
        val targetStatus = status ?: if (worker.isHourlyEnabled) AttendanceStatus.HOURLY else AttendanceStatus.FULL_DAY
        val epochDay = JalaliCalendar.safeEpochDay(date)
        val base = AttendanceEntity(
            folderId = folderId,
            workerId = worker.id,
            dateFolderId = dateFolderId,
            date = date,
            epochDay = epochDay,
            status = targetStatus,
            transitAllowance = worker.transitAllowance,
            transitImpact = worker.transitImpact,
            foodAllowance = worker.foodAllowance,
            foodImpact = worker.foodImpact,
            accommodationAllowance = worker.accommodationAllowance,
            accommodationImpact = worker.accommodationImpact,
            medicalAllowance = worker.medicalAllowance,
            medicalImpact = worker.medicalImpact,
            hasCustomAllowances = true,
            workplaceName = workplaceName,
            foremanName = foremanName,
            employerName = employerName,
            notes = notes
        )
        return applyStatusFields(
            attendance = base,
            worker = worker,
            targetStatus = targetStatus,
            preserveManualFields = false
        )
    }

    /**
     * Standard rounding function used across all financial calculations.
     * Prevents truncation errors by rounding mathematically (half-up) and throws
     * [ArithmeticException] if the result overflows a 64-bit signed Long.
     */
    fun roundToLong(value: Double): Long {
        if (!value.isFinite()) {
            throw ArithmeticException("مقدار عددی نامعتبر است (NaN یا بی‌نهایت)")
        }
        val rounded = floor(value + 0.5)
        // Note: Long.MAX_VALUE.toDouble() rounds up to 2^63 (9.223372036854776E18) in IEEE-754
        if (rounded >= 9.223372036854776E18 || rounded < Long.MIN_VALUE.toDouble()) {
            throw ArithmeticException("سرریز عدد صحیح (Long overflow) در گرد کردن مقدار: $value")
        }
        return rounded.toLong()
    }

    /**
     * ضرب امن ساعت کارکرد (اعشاری یا صحیح) در نرخ دستمزد (Long) با محافظت در برابر سرریز.
     * از [Math.multiplyExact] و [Math.addExact] استفاده می‌کند تا در صورت سرریز Long،
     * به‌جای عدد غلط، خطای واضح [ArithmeticException] پرتاب شود.
     */
    fun safeMultiplyHoursAndRate(hours: Double, rate: Long): Long {
        if (!hours.isFinite()) {
            throw ArithmeticException("مقدار ساعت نامعتبر است: $hours")
        }
        if (hours < 0.0 || rate < 0L) {
            throw ArithmeticException("ساعت کارکرد یا نرخ دستمزد نمی‌تواند منفی باشد (hours=$hours, rate=$rate)")
        }
        if (hours == 0.0 || rate == 0L) return 0L
        if (hours >= 9.223372036854776E18) {
            throw ArithmeticException("سرریز عدد صحیح (Long overflow) در ضرب ساعت ($hours) و نرخ ($rate)")
        }
        val wholeHours = floor(hours).toLong()
        val wholePay = if (wholeHours > 0L) Math.multiplyExact(wholeHours, rate) else 0L
        if (hours == wholeHours.toDouble()) {
            return wholePay
        }
        val fractionalHours = hours - wholeHours.toDouble()
        val fractionalPayApprox = roundToLong(fractionalHours * rate.toDouble())
        Math.addExact(wholePay, fractionalPayApprox)
        return roundToLong(hours * rate.toDouble())
    }

    private inline fun <T> Iterable<T>.sumOfExact(selector: (T) -> Long): Long {
        var sum = 0L
        for (element in this) {
            sum = Math.addExact(sum, selector(element))
        }
        return sum
    }

    /**
     * Determines whether a worker is an hourly worker based on their attendance record status.
     * Fallback to worker profile only if no attendance record exists.
     */
    fun isHourly(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (att != null) {
            return att.status == AttendanceStatus.HOURLY
        }
        return worker.isHourlyEnabled
    }

    /**
     * Determines whether a worker is absent for a given attendance record.
     * Rule: Strictly determined by AttendanceEntity.status == ABSENT.
     */
    fun isAbsent(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (att == null) return false
        return att.status == AttendanceStatus.ABSENT
    }

    /**
     * Determines whether a worker worked a half-day shift (نصف روز).
     * Rule: Strictly determined by AttendanceEntity.status == HALF_DAY.
     */
    fun isHalfDay(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (att == null) return false
        return att.status == AttendanceStatus.HALF_DAY
    }

    /**
     * Determines whether a worker worked a full-day shift (تمام روز).
     * Rule: Strictly determined by AttendanceEntity.status == FULL_DAY.
     */
    fun isFullDay(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (att == null) return !isHourly(worker, null)
        return att.status == AttendanceStatus.FULL_DAY
    }

    /**
     * نتیجه تفکیک‌شده مزایا (افزایشی) و کسورات (کاهشی) روزانه کارگر در ۴ دسته:
     * ایاب‌وذهاب، خوراک، مسکن و درمان.
     */
    data class ResolvedAllowances(
        val transitAllowance: Long,
        val foodAllowance: Long,
        val accommodationAllowance: Long,
        val medicalAllowance: Long,
        val transitDeduction: Long,
        val foodDeduction: Long,
        val accommodationDeduction: Long,
        val medicalDeduction: Long,
        val totalAllowances: Long,
        val totalDeductions: Long
    )

    /**
     * محاسبه دستمزد پایه روزانه (`baseWage`) بر اساس وضعیت حضور و فلگ [usesRecordValues]
     * (نام روشن‌تر برای [AttendanceEntity.hasCustomAllowances]).
     *
     * قوانین:
     * - اگر [usesRecordValues] فعال باشد، مقدار `att.dailyWage` مستقیماً ملاک است (حتی اگر صفر باشد؛
     *   و در حالت ساعتی مقادیر منفی یا صفر به `0L` تبدیل می‌شوند).
     * - اگر [usesRecordValues] غیرفعال باشد:
     *   - در حالت ساعتی (`isHourly`): دستمزد روزانه `0L` است.
     *   - در حالت نصف روز (`isHalfDay`): اگر `att.dailyWage > 0` باشد همان مقدار و در غیر این صورت نصف `worker.baseDailyWage` (با گرد کردن نیم به بالا).
     *   - در حالت تمام روز: اگر `att.dailyWage > 0` باشد همان مقدار و در غیر این صورت `worker.baseDailyWage`.
     */
    fun resolveBaseWage(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        isHourly: Boolean = isHourly(worker, att),
        isHalfDay: Boolean = isHalfDay(worker, att),
        usesRecordValues: Boolean = att?.usesRecordValues == true
    ): Long {
        return if (usesRecordValues && att != null) {
            if (isHourly && att.dailyWage <= 0L) 0L else att.dailyWage
        } else if (isHourly) {
            0L
        } else when {
            isHalfDay -> {
                if (att?.dailyWage != null && att.dailyWage > 0L) att.dailyWage
                else roundToLong(worker.baseDailyWage / 2.0)
            }
            att != null && att.dailyWage > 0L -> att.dailyWage
            else -> worker.baseDailyWage
        }
    }

    /**
     * استخراج نرخ ساعتی مؤثر کارگر برای روز جاری با در نظر گرفتن [usesRecordValues] و ترتیب fallback:
     * `att.hourlyWageRate` -> `att.hourlyWage` -> (در صورت عدم فعال بودن `usesRecordValues`) `worker.hourlyWageRate` -> `worker.baseHourlyWage`.
     */
    fun resolveHourlyRate(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        usesRecordValues: Boolean = att?.usesRecordValues == true
    ): Long {
        return if (usesRecordValues && att != null) {
            if (att.hourlyWageRate > 0L) att.hourlyWageRate else att.hourlyWage
        } else if (att != null && att.hourlyWageRate > 0L) {
            att.hourlyWageRate
        } else if (att != null && att.hourlyWage > 0L) {
            att.hourlyWage
        } else if (worker.hourlyWageRate > 0L) {
            worker.hourlyWageRate
        } else {
            worker.baseHourlyWage
        }
    }

    /**
     * محاسبه دستمزد ساعتی (`hourlyPay`) برای یک روز کاری.
     * فقط از ساعت ثبت‌شده روی رکورد حضور (`att.hourlyHours`) استفاده می‌کند و در صورت سرریز یا ساعت منفی
     * خطای [ArithmeticException] پرتاب می‌کند.
     */
    fun resolveHourlyPay(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        isHourly: Boolean = isHourly(worker, att),
        usesRecordValues: Boolean = att?.usesRecordValues == true,
        hourlyRate: Long = resolveHourlyRate(worker, att, usesRecordValues)
    ): Long {
        val hHours = att?.hourlyHours ?: 0.0
        if (!hHours.isFinite() || hHours < 0.0) {
            throw ArithmeticException("ساعت کارکرد ساعتی نامعتبر است: $hHours")
        }
        val hasCustomHourly = usesRecordValues && att != null &&
            att.status == AttendanceStatus.FULL_DAY && hHours > 0.0 && hourlyRate > 0L
        return if ((isHourly || hasCustomHourly) && hHours > 0.0 && hourlyRate > 0L) {
            safeMultiplyHoursAndRate(hHours, hourlyRate)
        } else {
            0L
        }
    }

    /**
     * محاسبه دستمزد اضافه‌کاری (`overtimePay`) برای یک روز کاری.
     *
     * ترتیب تعیین نرخ اضافه‌کاری (`otRate`):
     * ۱. اگر [usesRecordValues] فعال باشد: مستقیماً `att.overtimeRate` (حتی اگر `0L` باشد).
     * ۲. اگر `att.overtimeRate > 0L` باشد: `att.overtimeRate`.
     * ۳. اگر `worker.overtimeRate > 0L` باشد: `worker.overtimeRate`.
     * ۴. در غیر این صورت: محاسبه خودکار با ضریب [overtimeMultiplier] (پیش‌فرض ۱٫۴ برابر نرخ پایه ساعتی یا `baseDailyWage / 8.0`).
     */
    fun resolveOvertimePay(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER,
        isHourly: Boolean = isHourly(worker, att),
        usesRecordValues: Boolean = att?.usesRecordValues == true,
        hourlyRate: Long = resolveHourlyRate(worker, att, usesRecordValues)
    ): Long {
        val hHours = att?.hourlyHours ?: 0.0
        val otHours = att?.overtimeHours ?: 0.0
        if (!otHours.isFinite() || otHours < 0.0) {
            throw ArithmeticException("ساعت اضافه‌کاری نامعتبر است: $otHours")
        }
        val otRate = if (usesRecordValues && att != null) {
            att.overtimeRate
        } else if (att != null && att.overtimeRate > 0L) {
            att.overtimeRate
        } else if (worker.overtimeRate > 0L) {
            worker.overtimeRate
        } else {
            val baseRate = if (hourlyRate > 0L) {
                hourlyRate.toDouble()
            } else if (worker.baseDailyWage > 0L) {
                worker.baseDailyWage.toDouble() / 8.0
            } else {
                0.0
            }
            roundToLong(baseRate * overtimeMultiplier)
        }

        val canHaveOvertime = if (isHourly) hHours > 0.0 else true
        return if (canHaveOvertime && otHours > 0.0 && otRate > 0L) {
            safeMultiplyHoursAndRate(otHours, otRate)
        } else {
            0L
        }
    }

    /**
     * محاسبه ساعات کار عادی (`regularHours`) برای یک روز کاری.
     */
    fun resolveRegularHours(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        isHourly: Boolean = isHourly(worker, att),
        isHalfDay: Boolean = isHalfDay(worker, att)
    ): Double {
        return if (isHourly) {
            0.0
        } else if (att != null && att.regularHours > 0.0) {
            att.regularHours
        } else if (isHalfDay) {
            4.0
        } else {
            8.0
        }
    }

    /**
     * تابع یکپارچه محاسبه مزایا و کسورات روزانه در هر دو حالت حضور و غیبت.
     *
     * - اگر [usesRecordValues] فعال باشد، مقادیر و نوع تأثیر (`ALLOWANCE` / `DEDUCTION`) از رکورد حضور (`att`)
     *   خوانده می‌شوند؛ در غیر این صورت از پروفایل کارگر (`worker`) استفاده می‌شود.
     * - در روز غیبت ([isAbsent] == true)، مزایای افزایشی (`ALLOWANCE`) صفر است ولی کسورات (`DEDUCTION`)
     *   طبق قانون کسورات ثابت روز غیبت لحاظ می‌گردد.
     */
    fun resolveAllowances(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        isAbsent: Boolean = isAbsent(worker, att),
        usesRecordValues: Boolean = att?.usesRecordValues == true
    ): ResolvedAllowances {
        fun resolveCategory(
            attAmount: Long,
            attImpact: String,
            workerAmount: Long,
            workerImpact: String
        ): Pair<Long, Long> {
            val effectiveAmount = if (usesRecordValues && att != null) attAmount else workerAmount
            val effectiveImpact = if (usesRecordValues && att != null) attImpact else workerImpact
            val allowance = if (!isAbsent && effectiveImpact == "ALLOWANCE") effectiveAmount else 0L
            val deduction = if (effectiveImpact == "DEDUCTION") effectiveAmount else 0L
            return allowance to deduction
        }

        val (transitAllowance, transitDeduction) = resolveCategory(
            attAmount = att?.transitAllowance ?: 0L,
            attImpact = att?.transitImpact ?: "ALLOWANCE",
            workerAmount = worker.transitAllowance,
            workerImpact = worker.transitImpact
        )
        val (foodAllowance, foodDeduction) = resolveCategory(
            attAmount = att?.foodAllowance ?: 0L,
            attImpact = att?.foodImpact ?: "ALLOWANCE",
            workerAmount = worker.foodAllowance,
            workerImpact = worker.foodImpact
        )
        val (accomAllowance, accomDeduction) = resolveCategory(
            attAmount = att?.accommodationAllowance ?: 0L,
            attImpact = att?.accommodationImpact ?: "ALLOWANCE",
            workerAmount = worker.accommodationAllowance,
            workerImpact = worker.accommodationImpact
        )
        val (medAllowance, medDeduction) = resolveCategory(
            attAmount = att?.medicalAllowance ?: 0L,
            attImpact = att?.medicalImpact ?: "ALLOWANCE",
            workerAmount = worker.medicalAllowance,
            workerImpact = worker.medicalImpact
        )

        val totalAllowances = if (isAbsent) {
            0L
        } else {
            Math.addExact(
                Math.addExact(transitAllowance, foodAllowance),
                Math.addExact(accomAllowance, medAllowance)
            )
        }
        val totalDeductions = Math.addExact(
            Math.addExact(transitDeduction, foodDeduction),
            Math.addExact(accomDeduction, medDeduction)
        )

        return ResolvedAllowances(
            transitAllowance = transitAllowance,
            foodAllowance = foodAllowance,
            accommodationAllowance = accomAllowance,
            medicalAllowance = medAllowance,
            transitDeduction = transitDeduction,
            foodDeduction = foodDeduction,
            accommodationDeduction = accomDeduction,
            medicalDeduction = medDeduction,
            totalAllowances = totalAllowances,
            totalDeductions = totalDeductions
        )
    }

    /**
     * Detailed single-day calculation result for a worker.
     * All multiplications, additions, and subtractions are protected with [Math.multiplyExact],
     * [Math.addExact], [Math.subtractExact], and [Math.negateExact].
     *
     * Delegates to named helper functions:
     * - [resolveBaseWage]
     * - [resolveHourlyRate] & [resolveHourlyPay]
     * - [resolveOvertimePay]
     * - [resolveRegularHours]
     * - [resolveAllowances] (unified across absent and working days)
     */
    fun calculateDay(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): DayCalculationResult {
        if (!overtimeMultiplier.isFinite() || overtimeMultiplier < 0.0) {
            throw ArithmeticException("ضریب اضافه‌کاری نامعتبر است: $overtimeMultiplier")
        }

        val usesRecordValues = att?.usesRecordValues == true

        if (isAbsent(worker, att)) {
            // Rule: Absent day (کارگر غایب):
            // - دستمزد روزانه، دستمزد ساعتی، اضافه کاری و مزایا (افزایشی) صفر است.
            // - کسورات (کسر ایاب و ذهاب، مسکن، خوراک، درمان) حتی در روز غیبت لحاظ می‌شود.
            val allowances = resolveAllowances(
                worker = worker,
                att = att,
                isAbsent = true,
                usesRecordValues = usesRecordValues
            )
            val absentNetPayout = Math.negateExact(allowances.totalDeductions)

            return DayCalculationResult(
                isWorkingDay = false,
                regularHours = 0.0,
                hourlyHours = 0.0,
                overtimeHours = 0.0,
                baseWage = 0L,
                hourlyPay = 0L,
                overtimePay = 0L,
                totalAllowances = allowances.totalAllowances,
                totalDeductions = allowances.totalDeductions,
                netPayout = absentNetPayout,
                clampedNetPayout = 0L,
                transitAllowance = allowances.transitAllowance,
                foodAllowance = allowances.foodAllowance,
                accommodationAllowance = allowances.accommodationAllowance,
                medicalAllowance = allowances.medicalAllowance,
                transitDeduction = allowances.transitDeduction,
                foodDeduction = allowances.foodDeduction,
                accommodationDeduction = allowances.accommodationDeduction,
                medicalDeduction = allowances.medicalDeduction
            )
        }

        val hourly = isHourly(worker, att)
        val halfDay = isHalfDay(worker, att)
        val hHours = att?.hourlyHours ?: 0.0
        val otHours = att?.overtimeHours ?: 0.0

        // 1. Base Daily Wage
        val baseWage = resolveBaseWage(
            worker = worker,
            att = att,
            isHourly = hourly,
            isHalfDay = halfDay,
            usesRecordValues = usesRecordValues
        )

        // 2. Hourly Pay
        val hourlyRate = resolveHourlyRate(
            worker = worker,
            att = att,
            usesRecordValues = usesRecordValues
        )
        val hourlyPay = resolveHourlyPay(
            worker = worker,
            att = att,
            isHourly = hourly,
            usesRecordValues = usesRecordValues,
            hourlyRate = hourlyRate
        )

        // 3. Overtime Pay
        val overtimePay = resolveOvertimePay(
            worker = worker,
            att = att,
            overtimeMultiplier = overtimeMultiplier,
            isHourly = hourly,
            usesRecordValues = usesRecordValues,
            hourlyRate = hourlyRate
        )

        // 4. Regular Hours
        val regHours = resolveRegularHours(
            worker = worker,
            att = att,
            isHourly = hourly,
            isHalfDay = halfDay
        )

        // 5. Allowances and Deductions (unified with absent-day logic via resolveAllowances)
        val allowances = resolveAllowances(
            worker = worker,
            att = att,
            isAbsent = false,
            usesRecordValues = usesRecordValues
        )

        // 6. Net Payout for single-day (preserves negative daily balance when deductions > gross)
        val gross = Math.addExact(
            Math.addExact(baseWage, hourlyPay),
            Math.addExact(overtimePay, allowances.totalAllowances)
        )
        val rawNet = Math.subtractExact(gross, allowances.totalDeductions)

        return DayCalculationResult(
            isWorkingDay = true,
            regularHours = regHours,
            hourlyHours = if (hourly) hHours else 0.0,
            overtimeHours = otHours,
            baseWage = baseWage,
            hourlyPay = hourlyPay,
            overtimePay = overtimePay,
            totalAllowances = allowances.totalAllowances,
            totalDeductions = allowances.totalDeductions,
            netPayout = rawNet,
            clampedNetPayout = rawNet.coerceAtLeast(0L),
            transitAllowance = allowances.transitAllowance,
            foodAllowance = allowances.foodAllowance,
            accommodationAllowance = allowances.accommodationAllowance,
            medicalAllowance = allowances.medicalAllowance,
            transitDeduction = allowances.transitDeduction,
            foodDeduction = allowances.foodDeduction,
            accommodationDeduction = allowances.accommodationDeduction,
            medicalDeduction = allowances.medicalDeduction
        )
    }

    /**
     * Calculates the exact net daily payout for a worker on a specific date.
     * By default, returns the true daily balance (gross - deductions), which can be negative
     * if deductions exceed daily gross earnings, ensuring that:
     * sum of calculateDayPayout across all days == worker's cumulative net earnings.
     *
     * Set [coerceNonNegative] = true if a strictly non-negative display value is required.
     */
    fun calculateDayPayout(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER,
        coerceNonNegative: Boolean = false
    ): Long {
        val result = calculateDay(worker, att, overtimeMultiplier)
        return if (coerceNonNegative) result.clampedNetPayout else result.netPayout
    }

    /**
     * Aggregates all attendance records and individual expenses for a single worker into a [WorkerPerformance].
     * Rule: Absent days are ignored for shifts, wages, and allowances, but deductions are accumulated.
     * Rule: قانون تسویه و بدهی کارگر:
     *       قابل پرداخت = max(raw, 0) و بدهی کارگر = max(-raw, 0).
     */
    fun calculateWorkerPerformance(
        worker: WorkerEntity,
        attendances: List<AttendanceEntity>,
        individualExpenses: List<ExpenseEntity> = emptyList(),
        groupExpenseShare: Long = 0L,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): WorkerPerformance {
        var shifts = 0
        var regHours = 0.0
        var hHours = 0.0
        var otHours = 0.0
        var baseWageTotal = 0L
        var hourlyPayTotal = 0L
        var otPayTotal = 0L

        var transitAllowanceTotal = 0L
        var foodAllowanceTotal = 0L
        var accomAllowanceTotal = 0L
        var medAllowanceTotal = 0L
        var otherAllowanceTotal = 0L

        var transitDeductionTotal = 0L
        var foodDeductionTotal = 0L
        var accomDeductionTotal = 0L
        var medDeductionTotal = 0L
        var otherDeductionTotal = 0L

        var fullDays = 0
        var halfDays = 0
        var hourlyDays = 0
        var absentDays = 0

        for (att in attendances) {
            val res = calculateDay(worker, att, overtimeMultiplier)
            when {
                isAbsent(worker, att) -> absentDays++
                isHourly(worker, att) -> hourlyDays++
                isHalfDay(worker, att) -> halfDays++
                else -> fullDays++
            }
            if (res.isWorkingDay) {
                shifts++
                regHours += res.regularHours
                hHours += res.hourlyHours
                otHours += res.overtimeHours
                baseWageTotal = Math.addExact(baseWageTotal, res.baseWage)
                hourlyPayTotal = Math.addExact(hourlyPayTotal, res.hourlyPay)
                otPayTotal = Math.addExact(otPayTotal, res.overtimePay)

                transitAllowanceTotal = Math.addExact(transitAllowanceTotal, res.transitAllowance)
                foodAllowanceTotal = Math.addExact(foodAllowanceTotal, res.foodAllowance)
                accomAllowanceTotal = Math.addExact(accomAllowanceTotal, res.accommodationAllowance)
                medAllowanceTotal = Math.addExact(medAllowanceTotal, res.medicalAllowance)
            }

            // کسورات حتی در صورت غیبت نیز کسر و تجمیع می‌شوند
            transitDeductionTotal = Math.addExact(transitDeductionTotal, res.transitDeduction)
            foodDeductionTotal = Math.addExact(foodDeductionTotal, res.foodDeduction)
            accomDeductionTotal = Math.addExact(accomDeductionTotal, res.accommodationDeduction)
            medDeductionTotal = Math.addExact(medDeductionTotal, res.medicalDeduction)
        }

        // Add individual expenses for this worker (strictly based on workerId)
        for (exp in individualExpenses) {
            val isAllowance = exp.impactType.equals("ALLOWANCE", ignoreCase = true)
            when {
                exp.category.equals("TRANSIT", ignoreCase = true) ->
                    if (isAllowance) transitAllowanceTotal = Math.addExact(transitAllowanceTotal, exp.amount)
                    else transitDeductionTotal = Math.addExact(transitDeductionTotal, exp.amount)
                exp.category.equals("FOOD", ignoreCase = true) ->
                    if (isAllowance) foodAllowanceTotal = Math.addExact(foodAllowanceTotal, exp.amount)
                    else foodDeductionTotal = Math.addExact(foodDeductionTotal, exp.amount)
                exp.category.equals("ACCOMMODATION", ignoreCase = true) ->
                    if (isAllowance) accomAllowanceTotal = Math.addExact(accomAllowanceTotal, exp.amount)
                    else accomDeductionTotal = Math.addExact(accomDeductionTotal, exp.amount)
                exp.category.equals("MEDICAL", ignoreCase = true) ->
                    if (isAllowance) medAllowanceTotal = Math.addExact(medAllowanceTotal, exp.amount)
                    else medDeductionTotal = Math.addExact(medDeductionTotal, exp.amount)
                else ->
                    if (isAllowance) otherAllowanceTotal = Math.addExact(otherAllowanceTotal, exp.amount)
                    else otherDeductionTotal = Math.addExact(otherDeductionTotal, exp.amount)
            }
        }

        val totalAllowances = Math.addExact(
            Math.addExact(
                Math.addExact(transitAllowanceTotal, foodAllowanceTotal),
                Math.addExact(accomAllowanceTotal, medAllowanceTotal)
            ),
            otherAllowanceTotal
        )
        val totalDeductions = Math.addExact(
            Math.addExact(
                Math.addExact(transitDeductionTotal, foodDeductionTotal),
                Math.addExact(accomDeductionTotal, medDeductionTotal)
            ),
            otherDeductionTotal
        )

        val gross = Math.addExact(
            Math.addExact(baseWageTotal, hourlyPayTotal),
            Math.addExact(otPayTotal, totalAllowances)
        )
        val rawNetBeforeGroup = Math.subtractExact(gross, totalDeductions)
        val rawFinalNet = Math.subtractExact(rawNetBeforeGroup, groupExpenseShare)
        val netBeforeGroup = rawNetBeforeGroup.coerceAtLeast(0L)
        val finalNet = rawFinalNet.coerceAtLeast(0L)
        // قانون: قابل پرداخت = max(raw, 0) و بدهی کارگر = max(-raw, 0)
        val debt = if (rawFinalNet < 0L) Math.negateExact(rawFinalNet) else 0L

        return WorkerPerformance(
            worker = worker,
            totalShifts = shifts,
            regularHours = regHours,
            hourlyHours = hHours,
            overtimeHours = otHours,
            baseWageTotal = baseWageTotal,
            hourlyPayTotal = hourlyPayTotal,
            overtimePayTotal = otPayTotal,
            fullDaysCount = fullDays,
            halfDaysCount = halfDays,
            hourlyDaysCount = hourlyDays,
            absentDaysCount = absentDays,
            totalAllowances = totalAllowances,
            transitAllowanceTotal = transitAllowanceTotal,
            foodAllowanceTotal = foodAllowanceTotal,
            accommodationAllowanceTotal = accomAllowanceTotal,
            medicalAllowanceTotal = medAllowanceTotal,
            otherAllowanceTotal = otherAllowanceTotal,
            totalDeductions = totalDeductions,
            transitDeductionTotal = transitDeductionTotal,
            foodDeductionTotal = foodDeductionTotal,
            accommodationDeductionTotal = accomDeductionTotal,
            medicalDeductionTotal = medDeductionTotal,
            otherDeductionTotal = otherDeductionTotal,
            groupExpenseShare = groupExpenseShare,
            netPayoutBeforeGroup = netBeforeGroup,
            netPayout = finalNet,
            rawNetPayoutBeforeGroup = rawNetBeforeGroup,
            rawNetPayout = rawFinalNet,
            debtAmount = debt
        )
    }

    /**
     * Distributes workshop group expenses among eligible attendees with deterministic allocation of the remainder.
     * Rule: If totalGroupExpenses = 100,000 and count = 3, base is 33,333 and remainder is 1.
     * The first remainder workers receive base + 1 so that sum of shares strictly equals totalGroupExpenses.
     */
    fun calculateGroupExpenseShares(
        totalGroupExpenses: Long,
        eligibleWorkers: List<WorkerEntity>
    ): Map<Long, Long> {
        if (eligibleWorkers.isEmpty() || totalGroupExpenses <= 0L) return emptyMap()
        val count = eligibleWorkers.size
        val baseShare = totalGroupExpenses / count
        val remainder = totalGroupExpenses % count
        val sortedWorkers = eligibleWorkers.sortedBy { it.id }
        return sortedWorkers.mapIndexed { index, worker ->
            val extra = if (index < remainder) 1L else 0L
            worker.id to Math.addExact(baseShare, extra)
        }.toMap()
    }

    /**
     * تابع مشترک و واحد محاسبه خالص هزینه (مورد استفاده در حساب روزانه و حساب کل پروژه).
     *
     * تعریف واحد:
     * خالص هزینه = دستمزد پایه روزانه + دستمزد ساعتی + دستمزد اضافه‌کاری + مزایای افزایشی - کسورات - هزینه‌های گروهی کارگاه.
     *
     * این تابع مقدار خام (Raw) را بدون `coerceAtLeast(0)` برمی‌گرداند تا جمع `grandDailyCost`
     * در تمام روزهای پروژه همیشه دقیقاً برابر `grandTotalProjectCost` باشد.
     */
    fun calculateNetCost(
        baseWages: Long,
        hourlyPay: Long,
        overtimePay: Long,
        allowances: Long,
        deductions: Long,
        groupExpenses: Long
    ): Long {
        val totalGross = Math.addExact(
            Math.addExact(baseWages, hourlyPay),
            Math.addExact(overtimePay, allowances)
        )
        val afterDeductions = Math.subtractExact(totalGross, deductions)
        return Math.subtractExact(afterDeductions, groupExpenses)
    }

    /**
     * بررسی می‌کند آیا رکورد حضور (یا مقادیر مؤثر کارگر در این رکورد) دارای مقدار خارج از سقف مجاز
     * ([MAX_WAGE_AMOUNT] یا ساعت بیشتر از ۲۴ یا منفی/NaN) است یا خیر، و فهرست دلایل فارسی را برمی‌گرداند.
     */
    fun findAttendanceAnomalies(worker: WorkerEntity?, att: AttendanceEntity): List<String> {
        val issues = mutableListOf<String>()
        if (!att.regularHours.isFinite() || att.regularHours < 0.0 || att.regularHours > MAX_REGULAR_HOURS) {
            issues.add("ساعات عادی خارج از محدوده مجاز (${Formatters.formatHours(att.regularHours)})")
        }
        if (!att.hourlyHours.isFinite() || att.hourlyHours < 0.0 || att.hourlyHours > MAX_HOURLY_HOURS) {
            issues.add("ساعت کارکرد ساعتی خارج از محدوده مجاز (${Formatters.formatHours(att.hourlyHours)})")
        }
        if (!att.overtimeHours.isFinite() || att.overtimeHours < 0.0 || att.overtimeHours > MAX_OVERTIME_HOURS) {
            issues.add("ساعت اضافه‌کاری خارج از محدوده مجاز (${Formatters.formatHours(att.overtimeHours)})")
        }
        if (att.dailyWage < 0L || att.dailyWage > MAX_WAGE_AMOUNT) {
            issues.add("دستمزد روزانه خارج از سقف مجاز")
        }
        if (att.hourlyWage < 0L || att.hourlyWage > MAX_WAGE_AMOUNT || att.hourlyWageRate < 0L || att.hourlyWageRate > MAX_WAGE_AMOUNT) {
            issues.add("نرخ دستمزد ساعتی خارج از سقف مجاز")
        }
        if (att.overtimeRate < 0L || att.overtimeRate > MAX_WAGE_AMOUNT) {
            issues.add("نرخ اضافه‌کاری خارج از سقف مجاز")
        }
        if (
            att.transitAllowance < 0L || att.transitAllowance > MAX_WAGE_AMOUNT ||
            att.foodAllowance < 0L || att.foodAllowance > MAX_WAGE_AMOUNT ||
            att.accommodationAllowance < 0L || att.accommodationAllowance > MAX_WAGE_AMOUNT ||
            att.medicalAllowance < 0L || att.medicalAllowance > MAX_WAGE_AMOUNT
        ) {
            issues.add("مبلغ مزایا یا کسورات روزانه خارج از سقف مجاز")
        }
        if (worker != null && !att.usesRecordValues) {
            if (worker.baseDailyWage < 0L || worker.baseDailyWage > MAX_WAGE_AMOUNT) {
                issues.add("دستمزد پایه کارگر خارج از سقف مجاز")
            }
            if (worker.hourlyWageRate < 0L || worker.hourlyWageRate > MAX_WAGE_AMOUNT || worker.baseHourlyWage < 0L || worker.baseHourlyWage > MAX_WAGE_AMOUNT) {
                issues.add("نرخ ساعتی کارگر خارج از سقف مجاز")
            }
            if (worker.overtimeRate < 0L || worker.overtimeRate > MAX_WAGE_AMOUNT) {
                issues.add("نرخ اضافه‌کاری کارگر خارج از سقف مجاز")
            }
            if (
                worker.transitAllowance < 0L || worker.transitAllowance > MAX_WAGE_AMOUNT ||
                worker.foodAllowance < 0L || worker.foodAllowance > MAX_WAGE_AMOUNT ||
                worker.accommodationAllowance < 0L || worker.accommodationAllowance > MAX_WAGE_AMOUNT ||
                worker.medicalAllowance < 0L || worker.medicalAllowance > MAX_WAGE_AMOUNT
            ) {
                issues.add("مزایا یا کسورات پروفایل کارگر خارج از سقف مجاز")
            }
        }
        return issues
    }

    /**
     * Unified financial summary for all workers, attendances, and expenses in a workplace folder.
     * Serves as the single calculation engine for analytics, workerPerformances, and grand project totals.
     *
     * قانون هماهنگی حساب روزانه و کل پروژه:
     * `grandTotalProjectCost` از طریق تابع مشترک [calculateNetCost] (پس از کسر کسورات و هزینه‌های گروهی و بدون clamp به صفر)
     * محاسبه می‌شود تا جمع `grandDailyCost` همه روزها دقیقاً برابر `grandTotalProjectCost` باشد.
     *
     * محافظت در برابر سرریز و رکوردهای معیوب:
     * محاسبه هر رکورد حضور در `try/catch` مجزا انجام می‌شود تا یک رکورد خراب کل پروژه را صفر یا متوقف نکند؛
     * رکورد معیوب از جمع کنار گذاشته شده و شناسه آن در [FinancialSummary.invalidAttendanceIds] برگردانده می‌شود.
     */
    fun calculateFinancialSummary(
        workers: List<WorkerEntity>,
        attendances: List<AttendanceEntity>,
        expenses: List<ExpenseEntity>,
        todayStr: String = JalaliCalendar.todayFlow.value.let { JalaliCalendar.refreshToday() },
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): FinancialSummary {
        val invalidAttendanceIds = mutableListOf<Long>()
        val validAttendancesByWorker = mutableMapOf<Long, List<AttendanceEntity>>()

        var runningProjectBase = 0L
        var runningProjectHourly = 0L
        var runningProjectOvertime = 0L
        var runningProjectAllowances = 0L
        var runningProjectDeductions = 0L

        for (worker in workers) {
            val workerAtts = attendances.filter { it.workerId == worker.id }
            val validForWorker = mutableListOf<AttendanceEntity>()
            var wBase = 0L
            var wHourly = 0L
            var wOvertime = 0L
            var wAllowances = 0L
            var wDeductions = 0L

            for (att in workerAtts) {
                try {
                    val anomalies = findAttendanceAnomalies(worker, att)
                    if (anomalies.isNotEmpty()) {
                        throw ArithmeticException(anomalies.first())
                    }
                    val res = calculateDay(worker, att, overtimeMultiplier)
                    val nextWBase = if (res.isWorkingDay) Math.addExact(wBase, res.baseWage) else wBase
                    val nextWHourly = if (res.isWorkingDay) Math.addExact(wHourly, res.hourlyPay) else wHourly
                    val nextWOvertime = if (res.isWorkingDay) Math.addExact(wOvertime, res.overtimePay) else wOvertime
                    val nextWAllowances = if (res.isWorkingDay) Math.addExact(wAllowances, res.totalAllowances) else wAllowances
                    val nextWDeductions = Math.addExact(wDeductions, res.totalDeductions)

                    val nextProjBase = if (res.isWorkingDay) Math.addExact(runningProjectBase, res.baseWage) else runningProjectBase
                    val nextProjHourly = if (res.isWorkingDay) Math.addExact(runningProjectHourly, res.hourlyPay) else runningProjectHourly
                    val nextProjOvertime = if (res.isWorkingDay) Math.addExact(runningProjectOvertime, res.overtimePay) else runningProjectOvertime
                    val nextProjAllowances = if (res.isWorkingDay) Math.addExact(runningProjectAllowances, res.totalAllowances) else runningProjectAllowances
                    val nextProjDeductions = Math.addExact(runningProjectDeductions, res.totalDeductions)

                    // Ensure gross/net will not overflow
                    val wGross = Math.addExact(
                        Math.addExact(nextWBase, nextWHourly),
                        Math.addExact(nextWOvertime, nextWAllowances)
                    )
                    Math.subtractExact(wGross, nextWDeductions)

                    wBase = nextWBase
                    wHourly = nextWHourly
                    wOvertime = nextWOvertime
                    wAllowances = nextWAllowances
                    wDeductions = nextWDeductions

                    runningProjectBase = nextProjBase
                    runningProjectHourly = nextProjHourly
                    runningProjectOvertime = nextProjOvertime
                    runningProjectAllowances = nextProjAllowances
                    runningProjectDeductions = nextProjDeductions

                    validForWorker.add(att)
                } catch (_: Exception) {
                    invalidAttendanceIds.add(att.id)
                }
            }
            validAttendancesByWorker[worker.id] = validForWorker
        }

        val validAttendances = validAttendancesByWorker.values.flatten()

        val groupExpenses = expenses.filter { it.scope.equals("GROUP", ignoreCase = true) }
        val totalGroupExpenses = groupExpenses.sumOfExact { it.amount }

        // Workers who worked at least 1 present shift among valid attendance records
        val workersWithShifts = workers.filter { worker ->
            validAttendancesByWorker[worker.id].orEmpty().any { it.status != AttendanceStatus.ABSENT }
        }
        val presentWorkerCount = workersWithShifts.size
        val groupExpenseShares = calculateGroupExpenseShares(totalGroupExpenses, workersWithShifts)
        val groupExpenseRemainder = if (presentWorkerCount > 0) totalGroupExpenses % presentWorkerCount else 0L

        val performances = workers.map { worker ->
            val workerAtts = validAttendancesByWorker[worker.id].orEmpty()
            val workerIndivExpenses = expenses.filter {
                it.scope.equals("INDIVIDUAL", ignoreCase = true) && it.workerId == worker.id
            }
            calculateWorkerPerformance(
                worker = worker,
                attendances = workerAtts,
                individualExpenses = workerIndivExpenses,
                groupExpenseShare = groupExpenseShares[worker.id] ?: 0L,
                overtimeMultiplier = overtimeMultiplier
            )
        }

        val totalWorkDaysCount = performances.sumOf { it.totalShifts }
        val totalWorkHours = performances.sumOf { it.regularHours + it.hourlyHours + it.overtimeHours }
        val totalOvertimeHours = performances.sumOf { it.overtimeHours }
        val totalBaseWagesPaid = performances.sumOfExact { it.baseWageTotal }
        val totalHourlyPaid = performances.sumOfExact { it.hourlyPayTotal }
        val totalOvertimePaid = performances.sumOfExact { it.overtimePayTotal }
        val totalAllowances = performances.sumOfExact { it.totalAllowances }
        val totalDeductions = performances.sumOfExact { it.totalDeductions }

        // محاسبه یکپارچه هزینه کل پروژه با همان فرمول حساب روزانه (کسر هزینه گروهی و بدون clamp)
        val grandTotalProjectCost = calculateNetCost(
            baseWages = totalBaseWagesPaid,
            hourlyPay = totalHourlyPaid,
            overtimePay = totalOvertimePaid,
            allowances = totalAllowances,
            deductions = totalDeductions,
            groupExpenses = totalGroupExpenses
        )

        val todayAttendanceCount = validAttendances.count { it.date == todayStr && it.status != AttendanceStatus.ABSENT }

        val totalTransitExpenses = expenses.filter { it.category.equals("TRANSIT", ignoreCase = true) }.sumOfExact { it.amount }
        val totalAccommodationExpenses = expenses.filter { it.category.equals("ACCOMMODATION", ignoreCase = true) }.sumOfExact { it.amount }
        val totalAccommodationDays = expenses.filter { it.category.equals("ACCOMMODATION", ignoreCase = true) }.sumOf { it.accommodationDays }
        val totalFoodExpenses = expenses.filter { it.category.equals("FOOD", ignoreCase = true) }.sumOfExact { it.amount }
        val totalMedicalExpenses = expenses.filter { it.category.equals("MEDICAL", ignoreCase = true) }.sumOfExact { it.amount }
        val totalOtherExpenses = expenses.filter { it.category.equals("OTHER", ignoreCase = true) }.sumOfExact { it.amount }
        val totalIndividualExpenses = expenses.filter { it.scope.equals("INDIVIDUAL", ignoreCase = true) }.sumOfExact { it.amount }
        val grandTotalExpenses = expenses.sumOfExact { it.amount }
        val totalWorkerDebt = performances.sumOfExact { it.debtAmount }

        return FinancialSummary(
            workerPerformances = performances,
            totalWorkDaysCount = totalWorkDaysCount,
            totalWorkHours = totalWorkHours,
            totalOvertimeHours = totalOvertimeHours,
            totalBaseWagesPaid = totalBaseWagesPaid,
            totalHourlyPaid = totalHourlyPaid,
            totalOvertimePaid = totalOvertimePaid,
            totalAllowances = totalAllowances,
            totalDeductions = totalDeductions,
            grandTotalProjectCost = grandTotalProjectCost,
            todayAttendanceCount = todayAttendanceCount,
            totalTransitExpenses = totalTransitExpenses,
            totalAccommodationExpenses = totalAccommodationExpenses,
            totalAccommodationDays = totalAccommodationDays,
            totalFoodExpenses = totalFoodExpenses,
            totalMedicalExpenses = totalMedicalExpenses,
            totalOtherExpenses = totalOtherExpenses,
            totalIndividualExpenses = totalIndividualExpenses,
            totalGroupExpenses = totalGroupExpenses,
            groupExpenseRemainder = groupExpenseRemainder,
            grandTotalExpenses = grandTotalExpenses,
            totalWorkerDebt = totalWorkerDebt,
            invalidAttendanceIds = invalidAttendanceIds
        )
    }

    /**
     * Unified daily bookkeeping calculation for a specific date.
     * Uses the shared [calculateNetCost] function without clamping to zero (`coerceAtLeast(0)`),
     * ensuring that the sum of [DailyBookkeeping.grandDailyCost] across all days strictly equals
     * [FinancialSummary.grandTotalProjectCost].
     *
     * محافظت در برابر سرریز و رکوردهای معیوب:
     * هر رکورد حضور در `try/catch` مجزا محاسبه می‌شود؛ رکورد معیوب از جمع روز کنار گذاشته شده و شناسه آن
     * در [DailyBookkeeping.invalidAttendanceIds] قرار می‌گیرد.
     */
    fun calculateDailyBookkeeping(
        dateStr: String,
        workers: List<WorkerEntity>,
        attendances: List<AttendanceEntity>,
        expenses: List<ExpenseEntity>,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): DailyBookkeeping {
        val dayAtts = attendances.filter { it.date == dateStr }
        val dayExps = expenses.filter { it.date == dateStr }

        val workerMap = workers.associateBy { it.id }
        var wages = 0L
        var hourlyPay = 0L
        var overtimePay = 0L
        var allowances = 0L
        var deductions = 0L
        var hours = 0.0
        var presentCount = 0
        val invalidAttendanceIds = mutableListOf<Long>()
        val validDayAtts = mutableListOf<AttendanceEntity>()

        for (att in dayAtts) {
            val worker = workerMap[att.workerId] ?: continue
            try {
                val anomalies = findAttendanceAnomalies(worker, att)
                if (anomalies.isNotEmpty()) {
                    throw ArithmeticException(anomalies.first())
                }
                val res = calculateDay(worker, att, overtimeMultiplier)
                val nextWages = if (res.isWorkingDay) Math.addExact(wages, res.baseWage) else wages
                val nextHourlyPay = if (res.isWorkingDay) Math.addExact(hourlyPay, res.hourlyPay) else hourlyPay
                val nextOvertimePay = if (res.isWorkingDay) Math.addExact(overtimePay, res.overtimePay) else overtimePay
                val nextAllowances = if (res.isWorkingDay) Math.addExact(allowances, res.totalAllowances) else allowances
                val nextDeductions = Math.addExact(deductions, res.totalDeductions)

                // Verify net cost won't overflow with this record
                val totalGross = Math.addExact(
                    Math.addExact(nextWages, nextHourlyPay),
                    Math.addExact(nextOvertimePay, nextAllowances)
                )
                Math.subtractExact(totalGross, nextDeductions)

                wages = nextWages
                hourlyPay = nextHourlyPay
                overtimePay = nextOvertimePay
                allowances = nextAllowances
                deductions = nextDeductions
                if (res.isWorkingDay) {
                    presentCount++
                    hours += (res.regularHours + res.hourlyHours + res.overtimeHours)
                }
                validDayAtts.add(att)
            } catch (_: Exception) {
                invalidAttendanceIds.add(att.id)
            }
        }

        // Add daily individual expenses for project workers (strictly requires non-null workerId in workerMap, matching calculateFinancialSummary)
        for (exp in dayExps.filter { it.scope.equals("INDIVIDUAL", ignoreCase = true) && it.workerId != null && workerMap.containsKey(it.workerId) }) {
            if (exp.impactType.equals("ALLOWANCE", ignoreCase = true)) {
                allowances = Math.addExact(allowances, exp.amount)
            } else {
                deductions = Math.addExact(deductions, exp.amount)
            }
        }

        val dailyIndivExpenses = dayExps.filter { it.scope.equals("INDIVIDUAL", ignoreCase = true) }.sumOfExact { it.amount }
        val dailyGroupExpenses = dayExps.filter { it.scope.equals("GROUP", ignoreCase = true) }.sumOfExact { it.amount }
        val dailyExpenses = dayExps.sumOfExact { it.amount }

        // محاسبه هزینه خالص روز از طریق تابع مشترک و بدون clamp به صفر
        val grandDailyCost = calculateNetCost(
            baseWages = wages,
            hourlyPay = hourlyPay,
            overtimePay = overtimePay,
            allowances = allowances,
            deductions = deductions,
            groupExpenses = dailyGroupExpenses
        )

        return DailyBookkeeping(
            date = dateStr,
            dayOfWeek = JalaliCalendar.getDayOfWeek(dateStr),
            grandDailyCost = grandDailyCost,
            totalDailyWages = wages,
            totalHourlyPay = hourlyPay,
            totalOvertimePay = overtimePay,
            dailyExpenses = dailyExpenses,
            groupExpenses = dailyGroupExpenses,
            individualExpenses = dailyIndivExpenses,
            workersPresent = presentCount,
            totalHours = hours,
            transitCost = dayExps.filter { it.category.equals("TRANSIT", ignoreCase = true) }.sumOfExact { it.amount },
            accommodationCost = dayExps.filter { it.category.equals("ACCOMMODATION", ignoreCase = true) }.sumOfExact { it.amount },
            foodCost = dayExps.filter { it.category.equals("FOOD", ignoreCase = true) }.sumOfExact { it.amount },
            medicalCost = dayExps.filter { it.category.equals("MEDICAL", ignoreCase = true) }.sumOfExact { it.amount },
            attendances = validDayAtts,
            expenses = dayExps,
            invalidAttendanceIds = invalidAttendanceIds
        )
    }

    /**
     * محاسبه تعداد روزهای حضور و مبلغ خالص تسویه‌ای که با حذف یک کارگر از بین می‌رود.
     */
    fun calculateWorkerDeletionImpact(
        worker: WorkerEntity,
        workerAttendances: List<AttendanceEntity>,
        workerIndividualExpenses: List<ExpenseEntity> = emptyList(),
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): DeletionImpact {
        val perf = calculateWorkerPerformance(
            worker = worker,
            attendances = workerAttendances,
            individualExpenses = workerIndividualExpenses,
            groupExpenseShare = 0L,
            overtimeMultiplier = overtimeMultiplier
        )
        return DeletionImpact(
            attendanceDaysCount = perf.totalShifts,
            netSettlementAmount = perf.rawNetPayoutBeforeGroup,
            totalAttendanceRecordsCount = workerAttendances.size
        )
    }

    /**
     * محاسبه تعداد روزهای حضور (نفر-روز کاری) و مبلغ خالص تسویه‌ای که با حذف یک روز کاری از بین می‌رود.
     */
    fun calculateDateFolderDeletionImpact(
        dateStr: String,
        workers: List<WorkerEntity>,
        dayAttendances: List<AttendanceEntity>,
        dayExpenses: List<ExpenseEntity> = emptyList(),
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): DeletionImpact {
        val daily = calculateDailyBookkeeping(
            dateStr = dateStr,
            workers = workers,
            attendances = dayAttendances,
            expenses = dayExpenses,
            overtimeMultiplier = overtimeMultiplier
        )
        return DeletionImpact(
            attendanceDaysCount = daily.workersPresent,
            netSettlementAmount = daily.grandDailyCost,
            totalAttendanceRecordsCount = dayAttendances.size
        )
    }

    /**
     * محاسبه تعداد روزهای حضور (مجموع نفر-روزهای کاری) و مبلغ خالص تسویه‌ای که با حذف یک پوشه کارگاه از بین می‌رود.
     */
    fun calculateFolderDeletionImpact(
        workers: List<WorkerEntity>,
        attendances: List<AttendanceEntity>,
        expenses: List<ExpenseEntity>,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): DeletionImpact {
        val summary = calculateFinancialSummary(
            workers = workers,
            attendances = attendances,
            expenses = expenses,
            overtimeMultiplier = overtimeMultiplier
        )
        return DeletionImpact(
            attendanceDaysCount = summary.totalWorkDaysCount,
            netSettlementAmount = summary.grandTotalProjectCost,
            totalAttendanceRecordsCount = attendances.size
        )
    }
}

/**
 * Breakdown of a single worker's calculation for one date/attendance entry.
 */
data class DayCalculationResult(
    val isWorkingDay: Boolean,
    val regularHours: Double,
    val hourlyHours: Double,
    val overtimeHours: Double,
    val baseWage: Long,
    val hourlyPay: Long,
    val overtimePay: Long,
    val totalAllowances: Long,
    val totalDeductions: Long,
    val netPayout: Long,
    val clampedNetPayout: Long = netPayout.coerceAtLeast(0L),
    val transitAllowance: Long = 0L,
    val foodAllowance: Long = 0L,
    val accommodationAllowance: Long = 0L,
    val medicalAllowance: Long = 0L,
    val transitDeduction: Long = 0L,
    val foodDeduction: Long = 0L,
    val accommodationDeduction: Long = 0L,
    val medicalDeduction: Long = 0L
) {
    val rawNetPayout: Long get() = netPayout
}

/**
 * Aggregated financial summary across all workers and expenses in a project.
 */
data class FinancialSummary(
    val workerPerformances: List<WorkerPerformance>,
    val totalWorkDaysCount: Int,
    val totalWorkHours: Double,
    val totalOvertimeHours: Double,
    val totalBaseWagesPaid: Long,
    val totalHourlyPaid: Long,
    val totalOvertimePaid: Long,
    val totalAllowances: Long,
    val totalDeductions: Long,
    val grandTotalProjectCost: Long,
    val todayAttendanceCount: Int,
    val totalTransitExpenses: Long,
    val totalAccommodationExpenses: Long,
    val totalAccommodationDays: Int,
    val totalFoodExpenses: Long,
    val totalMedicalExpenses: Long,
    val totalOtherExpenses: Long,
    val totalIndividualExpenses: Long,
    val totalGroupExpenses: Long,
    val groupExpenseRemainder: Long = 0L,
    val grandTotalExpenses: Long,
    val totalWorkerDebt: Long = 0L,
    val invalidAttendanceIds: List<Long> = emptyList()
) {
    companion object {
        val EMPTY = FinancialSummary(
            workerPerformances = emptyList(),
            totalWorkDaysCount = 0,
            totalWorkHours = 0.0,
            totalOvertimeHours = 0.0,
            totalBaseWagesPaid = 0L,
            totalHourlyPaid = 0L,
            totalOvertimePaid = 0L,
            totalAllowances = 0L,
            totalDeductions = 0L,
            grandTotalProjectCost = 0L,
            todayAttendanceCount = 0,
            totalTransitExpenses = 0L,
            totalAccommodationExpenses = 0L,
            totalAccommodationDays = 0,
            totalFoodExpenses = 0L,
            totalMedicalExpenses = 0L,
            totalOtherExpenses = 0L,
            totalIndividualExpenses = 0L,
            totalGroupExpenses = 0L,
            groupExpenseRemainder = 0L,
            grandTotalExpenses = 0L,
            totalWorkerDebt = 0L,
            invalidAttendanceIds = emptyList()
        )
    }
}
