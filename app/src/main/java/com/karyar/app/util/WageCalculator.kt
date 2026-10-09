package com.karyar.app.util

import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.domain.model.DailyBookkeeping
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
 * 6. Zero capping (قانون سقف صفر coerceAtLeast(0)):
 *    - Minimum net payout cap of 0 is applied ONLY ONCE to the final cumulative total of each worker:
 *      (grossTotal - deductionsTotal).coerceAtLeast(0L).
 *    - It is NEVER applied on individual days before summing. DayCalculationResult.netPayout is provided
 *      strictly for isolated single-day UI display and is NOT used when computing cumulative totals,
 *      preventing distortion when a worker has negative daily balances (e.g. absent day with deductions).
 * 7. Unified Financial Engine (مسیر محاسباتی یکپارچه):
 *    - calculateFinancialSummary serves as the single calculation engine for DashboardAnalytics,
 *      workerPerformances, and grand project totals.
 *    - grandTotalProjectCost strictly equals the sum of netPayoutBeforeGroup across all workers.
 *    - Deductions on absent days are treated identically in analytics, dailyBookkeeping, and workerPerformances.
 * 8. Expenses (هزینه‌ها):
 *    - GROUP expenses are divided equally among workers who worked at least one shift.
 *    - INDIVIDUAL expenses are added/deducted strictly by workerId (never by workerName).
 */
object WageCalculator {

    const val DEFAULT_OVERTIME_MULTIPLIER: Double = 1.4

    /**
     * Resolves the effective hourly wage rate for a worker, handling profile fallback.
     */
    fun defaultHourlyRate(worker: WorkerEntity): Long {
        return if (worker.hourlyWageRate > 0L) worker.hourlyWageRate else worker.baseHourlyWage
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
        val epochDay = JalaliCalendar.toEpochDay(date)
        val hourlyRate = defaultHourlyRate(worker)

        return when (targetStatus) {
            AttendanceStatus.FULL_DAY -> AttendanceEntity(
                folderId = folderId,
                workerId = worker.id,
                dateFolderId = dateFolderId,
                date = date,
                epochDay = epochDay,
                status = AttendanceStatus.FULL_DAY,
                regularHours = 8.0,
                dailyWage = worker.baseDailyWage,
                hourlyWage = 0L,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                overtimeHours = worker.overtimeHours,
                overtimeRate = worker.overtimeRate,
                workplaceName = workplaceName,
                foremanName = foremanName,
                employerName = employerName,
                notes = notes
            )
            AttendanceStatus.HALF_DAY -> AttendanceEntity(
                folderId = folderId,
                workerId = worker.id,
                dateFolderId = dateFolderId,
                date = date,
                epochDay = epochDay,
                status = AttendanceStatus.HALF_DAY,
                regularHours = 4.0,
                dailyWage = roundToLong(worker.baseDailyWage / 2.0),
                hourlyWage = hourlyRate,
                hourlyWageRate = worker.hourlyWageRate,
                hourlyHours = worker.hourlyHours,
                overtimeHours = worker.overtimeHours,
                overtimeRate = worker.overtimeRate,
                workplaceName = workplaceName,
                foremanName = foremanName,
                employerName = employerName,
                notes = notes
            )
            AttendanceStatus.HOURLY -> AttendanceEntity(
                folderId = folderId,
                workerId = worker.id,
                dateFolderId = dateFolderId,
                date = date,
                epochDay = epochDay,
                status = AttendanceStatus.HOURLY,
                regularHours = 0.0,
                dailyWage = 0L,
                hourlyWage = hourlyRate,
                hourlyWageRate = hourlyRate,
                hourlyHours = if (worker.hourlyHours > 0) worker.hourlyHours else 8.0,
                overtimeHours = worker.overtimeHours,
                overtimeRate = worker.overtimeRate,
                workplaceName = workplaceName,
                foremanName = foremanName,
                employerName = employerName,
                notes = notes
            )
            AttendanceStatus.ABSENT -> AttendanceEntity(
                folderId = folderId,
                workerId = worker.id,
                dateFolderId = dateFolderId,
                date = date,
                epochDay = epochDay,
                status = AttendanceStatus.ABSENT,
                regularHours = 0.0,
                dailyWage = 0L,
                hourlyWage = 0L,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                overtimeHours = 0.0,
                overtimeRate = 0L,
                workplaceName = workplaceName,
                foremanName = foremanName,
                employerName = employerName,
                notes = notes
            )
        }
    }

    /**
     * Standard rounding function used across all financial calculations.
     * Prevents truncation errors by rounding mathematically (half-up).
     */
    fun roundToLong(value: Double): Long = floor(value + 0.5).toLong()

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
     * Detailed single-day calculation result for a worker.
     */
    fun calculateDay(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): DayCalculationResult {
        if (isAbsent(worker, att)) {
            // Rule: Absent day (کارگر غایب):
            // - دستمزد روزانه، دستمزد ساعتی، اضافه کاری و مزایا (افزایشی) صفر است.
            // - کسورات (کسر ایاب و ذهاب، مسکن، خوراک، درمان) حتی در روز غیبت لحاظ می‌شود.
            val transitDeduction = if (worker.transitImpact == "DEDUCTION") worker.transitAllowance else 0L
            val foodDeduction = if (worker.foodImpact == "DEDUCTION") worker.foodAllowance else 0L
            val accomDeduction = if (worker.accommodationImpact == "DEDUCTION") worker.accommodationAllowance else 0L
            val medDeduction = if (worker.medicalImpact == "DEDUCTION") worker.medicalAllowance else 0L
            val totalDeductions = transitDeduction + foodDeduction + accomDeduction + medDeduction

            return DayCalculationResult(
                isWorkingDay = false,
                regularHours = 0.0,
                hourlyHours = 0.0,
                overtimeHours = 0.0,
                baseWage = 0L,
                hourlyPay = 0L,
                overtimePay = 0L,
                totalAllowances = 0L,
                totalDeductions = totalDeductions,
                netPayout = 0L,
                transitAllowance = 0L,
                foodAllowance = 0L,
                accommodationAllowance = 0L,
                medicalAllowance = 0L,
                transitDeduction = transitDeduction,
                foodDeduction = foodDeduction,
                accommodationDeduction = accomDeduction,
                medicalDeduction = medDeduction
            )
        }

        val hourly = isHourly(worker, att)
        val halfDay = isHalfDay(worker, att)

        // 1. Base Daily Wage
        val baseWage = if (hourly) 0L else when {
            halfDay -> {
                if (att?.dailyWage != null && att.dailyWage > 0L) att.dailyWage
                else roundToLong(worker.baseDailyWage / 2.0)
            }
            att != null && att.dailyWage > 0L -> att.dailyWage
            else -> worker.baseDailyWage
        }

        // 2. Hourly Pay (only uses att.hourlyHours; does NOT fallback to profile template)
        val hHours = if (att != null) att.hourlyHours else 0.0
        val hRate = if (att != null && att.hourlyWageRate > 0L) att.hourlyWageRate
                    else if (att != null && att.hourlyWage > 0L) att.hourlyWage
                    else if (worker.hourlyWageRate > 0L) worker.hourlyWageRate
                    else worker.baseHourlyWage
        val hourlyPay = if (hourly && hHours > 0.0 && hRate > 0L) roundToLong(hHours * hRate.toDouble()) else 0L

        // 3. Overtime Pay (only uses att.overtimeHours; profile rate used as fallback)
        val otHours = if (att != null) att.overtimeHours else 0.0
        val otRate = if (att != null && att.overtimeRate > 0L) att.overtimeRate
                     else if (worker.overtimeRate > 0L) worker.overtimeRate
                     else {
                         val baseRate = if (hRate > 0L) hRate.toDouble()
                                        else if (worker.baseDailyWage > 0L) (worker.baseDailyWage.toDouble() / 8.0)
                                        else 0.0
                         roundToLong(baseRate * overtimeMultiplier)
                     }

        val canHaveOvertime = if (hourly) hHours > 0.0 else true
        val overtimePay = if (canHaveOvertime && otHours > 0.0 && otRate > 0L) roundToLong(otHours * otRate.toDouble()) else 0L

        // 4. Regular Hours
        val regHours = if (hourly) 0.0
                       else if (att != null && att.regularHours > 0.0) att.regularHours
                       else if (halfDay) 4.0
                       else 8.0

        // 5. Allowances and Deductions (applied once per working day)
        val transitAllowance = if (worker.transitImpact == "ALLOWANCE") worker.transitAllowance else 0L
        val transitDeduction = if (worker.transitImpact == "DEDUCTION") worker.transitAllowance else 0L

        val foodAllowance = if (worker.foodImpact == "ALLOWANCE") worker.foodAllowance else 0L
        val foodDeduction = if (worker.foodImpact == "DEDUCTION") worker.foodAllowance else 0L

        val accomAllowance = if (worker.accommodationImpact == "ALLOWANCE") worker.accommodationAllowance else 0L
        val accomDeduction = if (worker.accommodationImpact == "DEDUCTION") worker.accommodationAllowance else 0L

        val medAllowance = if (worker.medicalImpact == "ALLOWANCE") worker.medicalAllowance else 0L
        val medDeduction = if (worker.medicalImpact == "DEDUCTION") worker.medicalAllowance else 0L

        val totalAllowances = transitAllowance + foodAllowance + accomAllowance + medAllowance
        val totalDeductions = transitDeduction + foodDeduction + accomDeduction + medDeduction

        // 6. Net Payout for single-day display only
        val gross = baseWage + hourlyPay + overtimePay + totalAllowances
        val net = (gross - totalDeductions).coerceAtLeast(0L)

        return DayCalculationResult(
            isWorkingDay = true,
            regularHours = regHours,
            hourlyHours = if (hourly) hHours else 0.0,
            overtimeHours = otHours,
            baseWage = baseWage,
            hourlyPay = hourlyPay,
            overtimePay = overtimePay,
            totalAllowances = totalAllowances,
            totalDeductions = totalDeductions,
            netPayout = net,
            transitAllowance = transitAllowance,
            foodAllowance = foodAllowance,
            accommodationAllowance = accomAllowance,
            medicalAllowance = medAllowance,
            transitDeduction = transitDeduction,
            foodDeduction = foodDeduction,
            accommodationDeduction = accomDeduction,
            medicalDeduction = medDeduction
        )
    }

    /**
     * Calculates the exact net daily payout for a worker on a specific date.
     */
    fun calculateDayPayout(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): Long = calculateDay(worker, att, overtimeMultiplier).netPayout

    /**
     * Aggregates all attendance records and individual expenses for a single worker into a [WorkerPerformance].
     * Rule: Absent days are ignored for shifts, wages, and allowances, but deductions are accumulated.
     * Rule: coerceAtLeast(0) is applied ONLY ONCE to the final cumulative total of the worker.
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

        var transitDeductionTotal = 0L
        var foodDeductionTotal = 0L
        var accomDeductionTotal = 0L
        var medDeductionTotal = 0L

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
                baseWageTotal += res.baseWage
                hourlyPayTotal += res.hourlyPay
                otPayTotal += res.overtimePay

                transitAllowanceTotal += res.transitAllowance
                foodAllowanceTotal += res.foodAllowance
                accomAllowanceTotal += res.accommodationAllowance
                medAllowanceTotal += res.medicalAllowance
            }

            // کسورات حتی در صورت غیبت نیز کسر و تجمیع می‌شوند
            transitDeductionTotal += res.transitDeduction
            foodDeductionTotal += res.foodDeduction
            accomDeductionTotal += res.accommodationDeduction
            medDeductionTotal += res.medicalDeduction
        }

        // Add individual expenses for this worker (strictly based on workerId)
        for (exp in individualExpenses) {
            val isAllowance = exp.impactType.equals("ALLOWANCE", ignoreCase = true)
            when (exp.category.uppercase()) {
                "TRANSIT" -> if (isAllowance) transitAllowanceTotal += exp.amount else transitDeductionTotal += exp.amount
                "FOOD" -> if (isAllowance) foodAllowanceTotal += exp.amount else foodDeductionTotal += exp.amount
                "ACCOMMODATION" -> if (isAllowance) accomAllowanceTotal += exp.amount else accomDeductionTotal += exp.amount
                "MEDICAL" -> if (isAllowance) medAllowanceTotal += exp.amount else medDeductionTotal += exp.amount
                else -> if (isAllowance) transitAllowanceTotal += exp.amount else transitDeductionTotal += exp.amount
            }
        }

        val totalAllowances = transitAllowanceTotal + foodAllowanceTotal + accomAllowanceTotal + medAllowanceTotal
        val totalDeductions = transitDeductionTotal + foodDeductionTotal + accomDeductionTotal + medDeductionTotal

        val gross = baseWageTotal + hourlyPayTotal + otPayTotal + totalAllowances
        val netBeforeGroup = (gross - totalDeductions).coerceAtLeast(0L)
        val finalNet = (gross - totalDeductions - groupExpenseShare).coerceAtLeast(0L)

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
            totalDeductions = totalDeductions,
            transitDeductionTotal = transitDeductionTotal,
            foodDeductionTotal = foodDeductionTotal,
            accommodationDeductionTotal = accomDeductionTotal,
            medicalDeductionTotal = medDeductionTotal,
            groupExpenseShare = groupExpenseShare,
            netPayoutBeforeGroup = netBeforeGroup,
            netPayout = finalNet
        )
    }

    /**
     * Unified financial summary for all workers, attendances, and expenses in a workplace folder.
     * Serves as the single calculation engine for analytics, workerPerformances, and grand project totals.
     */
    fun calculateFinancialSummary(
        workers: List<WorkerEntity>,
        attendances: List<AttendanceEntity>,
        expenses: List<ExpenseEntity>,
        todayStr: String = JalaliCalendar.todayString(),
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): FinancialSummary {
        val groupExpenses = expenses.filter { it.scope.equals("GROUP", ignoreCase = true) }
        val totalGroupExpenses = groupExpenses.sumOf { it.amount }

        // Workers who worked at least 1 present shift
        val workersWithShifts = workers.filter { worker ->
            attendances.any { it.workerId == worker.id && it.status != AttendanceStatus.ABSENT }
        }
        val presentWorkerCount = workersWithShifts.size
        val sharePerWorker = if (presentWorkerCount > 0) totalGroupExpenses / presentWorkerCount else 0L

        val performances = workers.map { worker ->
            val workerAtts = attendances.filter { it.workerId == worker.id }
            val workerIndivExpenses = expenses.filter {
                it.scope.equals("INDIVIDUAL", ignoreCase = true) && it.workerId == worker.id
            }
            calculateWorkerPerformance(
                worker = worker,
                attendances = workerAtts,
                individualExpenses = workerIndivExpenses,
                groupExpenseShare = if (worker in workersWithShifts) sharePerWorker else 0L,
                overtimeMultiplier = overtimeMultiplier
            )
        }

        val totalWorkDaysCount = performances.sumOf { it.totalShifts }
        val totalWorkHours = performances.sumOf { it.regularHours + it.hourlyHours + it.overtimeHours }
        val totalOvertimeHours = performances.sumOf { it.overtimeHours }
        val totalBaseWagesPaid = performances.sumOf { it.baseWageTotal }
        val totalHourlyPaid = performances.sumOf { it.hourlyPayTotal }
        val totalOvertimePaid = performances.sumOf { it.overtimePayTotal }
        val totalAllowances = performances.sumOf { it.totalAllowances }
        val totalDeductions = performances.sumOf { it.totalDeductions }

        // grandTotalProjectCost strictly equals sum of netPayoutBeforeGroup across all workers
        val grandTotalProjectCost = performances.sumOf { it.netPayoutBeforeGroup }

        val todayAttendanceCount = attendances.count { it.date == todayStr && it.status != AttendanceStatus.ABSENT }

        val totalTransitExpenses = expenses.filter { it.category.equals("TRANSIT", ignoreCase = true) }.sumOf { it.amount }
        val totalAccommodationExpenses = expenses.filter { it.category.equals("ACCOMMODATION", ignoreCase = true) }.sumOf { it.amount }
        val totalAccommodationDays = expenses.filter { it.category.equals("ACCOMMODATION", ignoreCase = true) }.sumOf { it.accommodationDays }
        val totalFoodExpenses = expenses.filter { it.category.equals("FOOD", ignoreCase = true) }.sumOf { it.amount }
        val totalMedicalExpenses = expenses.filter { it.category.equals("MEDICAL", ignoreCase = true) }.sumOf { it.amount }
        val totalOtherExpenses = expenses.filter { it.category.equals("OTHER", ignoreCase = true) }.sumOf { it.amount }
        val totalIndividualExpenses = expenses.filter { it.scope.equals("INDIVIDUAL", ignoreCase = true) }.sumOf { it.amount }
        val grandTotalExpenses = expenses.sumOf { it.amount }

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
            grandTotalExpenses = grandTotalExpenses
        )
    }

    /**
     * Unified daily bookkeeping calculation for a specific date.
     * Uses the exact same rules as the overall project analytics.
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

        for (att in dayAtts) {
            val worker = workerMap[att.workerId] ?: continue
            val res = calculateDay(worker, att, overtimeMultiplier)
            if (res.isWorkingDay) {
                presentCount++
                wages += res.baseWage
                hourlyPay += res.hourlyPay
                overtimePay += res.overtimePay
                allowances += res.totalAllowances
                hours += (res.regularHours + res.hourlyHours + res.overtimeHours)
            }
            // Deductions apply even when worker is absent
            deductions += res.totalDeductions
        }

        // Add daily individual expenses
        for (exp in dayExps.filter { it.scope.equals("INDIVIDUAL", ignoreCase = true) }) {
            if (exp.impactType.equals("ALLOWANCE", ignoreCase = true)) {
                allowances += exp.amount
            } else {
                deductions += exp.amount
            }
        }

        val dailyExpenses = dayExps.sumOf { it.amount }
        val grandDailyCost = (wages + hourlyPay + overtimePay + allowances - deductions).coerceAtLeast(0L)

        return DailyBookkeeping(
            date = dateStr,
            dayOfWeek = JalaliCalendar.getDayOfWeek(dateStr),
            grandDailyCost = grandDailyCost,
            totalDailyWages = wages,
            totalHourlyPay = hourlyPay,
            totalOvertimePay = overtimePay,
            dailyExpenses = dailyExpenses,
            workersPresent = presentCount,
            totalHours = hours,
            transitCost = dayExps.filter { it.category.equals("TRANSIT", ignoreCase = true) }.sumOf { it.amount },
            accommodationCost = dayExps.filter { it.category.equals("ACCOMMODATION", ignoreCase = true) }.sumOf { it.amount },
            foodCost = dayExps.filter { it.category.equals("FOOD", ignoreCase = true) }.sumOf { it.amount },
            medicalCost = dayExps.filter { it.category.equals("MEDICAL", ignoreCase = true) }.sumOf { it.amount },
            attendances = dayAtts,
            expenses = dayExps
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
    val transitAllowance: Long = 0L,
    val foodAllowance: Long = 0L,
    val accommodationAllowance: Long = 0L,
    val medicalAllowance: Long = 0L,
    val transitDeduction: Long = 0L,
    val foodDeduction: Long = 0L,
    val accommodationDeduction: Long = 0L,
    val medicalDeduction: Long = 0L
)

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
    val grandTotalExpenses: Long
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
            grandTotalExpenses = 0L
        )
    }
}
