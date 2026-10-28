package com.karyar.app

import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.util.Formatters
import com.karyar.app.util.WageCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WageCalculatorTest {

    private val baseDailyWorker = WorkerEntity(
        id = 1L,
        folderId = 10L,
        name = "علی رضایی",
        role = "بنا",
        baseDailyWage = 1_000_000L,
        baseHourlyWage = 0L,
        isHourlyEnabled = false
    )

    private val baseHourlyWorker = WorkerEntity(
        id = 2L,
        folderId = 10L,
        name = "حسین مرادی",
        role = "آرماتوربند",
        baseDailyWage = 0L,
        baseHourlyWage = 150_000L,
        isHourlyEnabled = true,
        hourlyWageRate = 150_000L,
        hourlyHours = 6.0
    )

    // 1. Full day standard daily wage
    @Test
    fun testFullDay_standardDailyWage() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertTrue(result.isWorkingDay)
        assertEquals(1_000_000L, result.baseWage)
        assertEquals(0L, result.hourlyPay)
        assertEquals(0L, result.overtimePay)
        assertEquals(1_000_000L, result.netPayout)
        assertEquals(8.0, result.regularHours, 0.001)
    }

    // 2. Full day with explicit dailyWage override on attendance
    @Test
    fun testFullDay_overrideDailyWage() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            dailyWage = 1_200_000L
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(1_200_000L, result.baseWage)
        assertEquals(1_200_000L, result.netPayout)
    }

    // 3. Half day standard daily wage (half of baseDailyWage)
    @Test
    fun testHalfDay_standardHalfDailyWage() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.HALF_DAY,
            regularHours = 4.0
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertTrue(result.isWorkingDay)
        assertEquals(500_000L, result.baseWage)
        assertEquals(4.0, result.regularHours, 0.001)
        assertEquals(500_000L, result.netPayout)
    }

    // 4. Half day with explicit dailyWage on attendance
    @Test
    fun testHalfDay_explicitDailyWage() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.HALF_DAY,
            regularHours = 4.0,
            dailyWage = 600_000L
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(600_000L, result.baseWage)
        assertEquals(600_000L, result.netPayout)
    }

    // 5. Half-day is strictly determined by AttendanceStatus.HALF_DAY, notes never determine status
    @Test
    fun testHalfDay_strictlyDeterminedByStatus() {
        val attHalf = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.HALF_DAY,
            regularHours = 4.0
        )
        assertTrue(WageCalculator.isHalfDay(baseDailyWorker, attHalf))
        val resultHalf = WageCalculator.calculateDay(baseDailyWorker, attHalf)
        assertEquals(500_000L, resultHalf.baseWage)

        val attFullWithNotes = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            notes = "نصف روز کار کرد اما کامل ثبت شد"
        )
        assertFalse(WageCalculator.isHalfDay(baseDailyWorker, attFullWithNotes))
        assertTrue(WageCalculator.isFullDay(baseDailyWorker, attFullWithNotes))
    }

    // 6. Hourly worker calculation (hours * rate)
    @Test
    fun testHourly_standardCalculation() {
        val att = AttendanceEntity(
            workerId = 2L,
            date = "1405/01/01",
            status = AttendanceStatus.HOURLY,
            hourlyHours = 7.0,
            hourlyWageRate = 150_000L
        )
        val result = WageCalculator.calculateDay(baseHourlyWorker, att)
        assertTrue(result.isWorkingDay)
        assertEquals(0L, result.baseWage)
        assertEquals(1_050_000L, result.hourlyPay)
        assertEquals(1_050_000L, result.netPayout)
    }

    // 7. Hourly worker with 0 hours produces 0 hourly pay
    @Test
    fun testHourly_zeroHoursProducesZero() {
        val att = AttendanceEntity(
            workerId = 2L,
            date = "1405/01/01",
            status = AttendanceStatus.HOURLY,
            hourlyHours = 0.0
        )
        val workerNoDefault = baseHourlyWorker.copy(hourlyHours = 0.0)
        val result = WageCalculator.calculateDay(workerNoDefault, att)
        assertEquals(0L, result.hourlyPay)
        assertEquals(0L, result.netPayout)
    }

    // 8. Absent day (status = ABSENT): wages, overtime, allowances are 0, but deductions STILL APPLY
    @Test
    fun testAbsent_statusAbsent_zeroWagesAndNotWorkingDay() {
        val workerWithAllowances = baseDailyWorker.copy(
            transitAllowance = 50_000L,
            transitImpact = "ALLOWANCE",
            foodAllowance = 30_000L,
            foodImpact = "DEDUCTION"
        )
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.ABSENT,
            regularHours = 0.0,
            overtimeHours = 2.0
        )
        val result = WageCalculator.calculateDay(workerWithAllowances, att)
        assertFalse(result.isWorkingDay)
        assertEquals(0L, result.baseWage)
        assertEquals(0L, result.hourlyPay)
        assertEquals(0L, result.overtimePay)
        assertEquals(0L, result.totalAllowances)
        assertEquals(30_000L, result.totalDeductions) // Deductions apply to absent workers
        assertEquals(-30_000L, result.netPayout) // True daily balance reflects deduction
        assertEquals(0L, result.clampedNetPayout) // Clamped non-negative version
    }

    // 9. Notes containing "غیبت" in a present day's explanation must NOT make the worker absent
    @Test
    fun testAbsent_notesContainingGhiebatDoesNotMakeWorkerAbsent() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            notes = "توضیح: جلسه با مدیر، دیروز غیبت داشت ولی امروز کامل حاضر شد"
        )
        assertFalse(WageCalculator.isAbsent(baseDailyWorker, att))
        assertTrue(WageCalculator.isFullDay(baseDailyWorker, att))
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertTrue(result.isWorkingDay)
        assertEquals(1_000_000L, result.baseWage)
        assertEquals(1_000_000L, result.netPayout)
    }

    // 10. Status = ABSENT is required for absence; zero regularHours on FULL_DAY does not override status
    @Test
    fun testAbsent_strictlyRequiresStatusAbsent() {
        val attAbsent = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.ABSENT,
            regularHours = 0.0
        )
        assertTrue(WageCalculator.isAbsent(baseDailyWorker, attAbsent))
        val resultAbsent = WageCalculator.calculateDay(baseDailyWorker, attAbsent)
        assertFalse(resultAbsent.isWorkingDay)
        assertEquals(0L, resultAbsent.netPayout)
    }

    // 10b. Overtime zero on attendance when profile has overtimeHours
    @Test
    fun testOvertime_zeroOvertimeOnAttendanceWithProfileOvertimeHours() {
        val workerWithProfileOvertime = baseDailyWorker.copy(
            overtimeHours = 4.0,
            overtimeRate = 150_000L
        )
        val attZeroOt = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 0.0 // explicitly zero overtime
        )
        val result = WageCalculator.calculateDay(workerWithProfileOvertime, attZeroOt)
        assertEquals(0L, result.overtimePay)
        assertEquals(0.0, result.overtimeHours, 0.001)
        assertEquals(1_000_000L, result.netPayout)
    }

    // 11. Overtime with explicit overtime rate
    @Test
    fun testOvertime_explicitRate() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 3.0,
            overtimeRate = 200_000L
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(600_000L, result.overtimePay)
        assertEquals(1_600_000L, result.netPayout)
    }

    // 12. Overtime without explicit rate (uses baseDailyWage / 8 * 1.4 default)
    @Test
    fun testOvertime_defaultMultiplierFormula() {
        // baseDailyWage = 1,000,000 -> hourly = 125,000 -> 125,000 * 1.4 = 175,000 per hour
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 2.0,
            overtimeRate = 0L
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(350_000L, result.overtimePay)
        assertEquals(1_350_000L, result.netPayout)
    }

    // 13. Overtime with configurable multiplier
    @Test
    fun testOvertime_configurableMultiplier() {
        // baseDailyWage = 800,000 -> hourly = 100,000 -> multiplier 1.5 -> 150,000 per hour * 2 = 300,000
        val worker = baseDailyWorker.copy(baseDailyWage = 800_000L)
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 2.0
        )
        val result = WageCalculator.calculateDay(worker, att, overtimeMultiplier = 1.5)
        assertEquals(300_000L, result.overtimePay)
    }

    // 14. Overtime on absent day is ZERO
    @Test
    fun testOvertime_onAbsentDayIsZero() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.ABSENT,
            overtimeHours = 4.0,
            overtimeRate = 200_000L
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(0L, result.overtimePay)
    }

    // 15. Overtime on hourly worker with 0 hours is ZERO
    @Test
    fun testOvertime_hourlyWorkerZeroHoursIsZero() {
        val att = AttendanceEntity(
            workerId = 2L,
            date = "1405/01/01",
            status = AttendanceStatus.HOURLY,
            hourlyHours = 0.0,
            overtimeHours = 3.0,
            overtimeRate = 200_000L
        )
        val workerNoDefault = baseHourlyWorker.copy(hourlyHours = 0.0)
        val result = WageCalculator.calculateDay(workerNoDefault, att)
        assertEquals(0L, result.overtimePay)
    }

    // 16. Allowances added to gross pay
    @Test
    fun testAllowances_addedToNetPayout() {
        val worker = baseDailyWorker.copy(
            transitAllowance = 60_000L,
            transitImpact = "ALLOWANCE",
            foodAllowance = 40_000L,
            foodImpact = "ALLOWANCE"
        )
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val result = WageCalculator.calculateDay(worker, att)
        assertEquals(100_000L, result.totalAllowances)
        assertEquals(0L, result.totalDeductions)
        assertEquals(1_100_000L, result.netPayout)
    }

    // 17. Deductions subtracted from gross pay
    @Test
    fun testDeductions_subtractedFromNetPayout() {
        val worker = baseDailyWorker.copy(
            accommodationAllowance = 150_000L,
            accommodationImpact = "DEDUCTION"
        )
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val result = WageCalculator.calculateDay(worker, att)
        assertEquals(0L, result.totalAllowances)
        assertEquals(150_000L, result.totalDeductions)
        assertEquals(850_000L, result.netPayout)
    }

    // 18. Mixed allowances and deductions on the same working day
    @Test
    fun testMixedAllowancesAndDeductions() {
        val worker = baseDailyWorker.copy(
            transitAllowance = 70_000L,
            transitImpact = "ALLOWANCE",
            accommodationAllowance = 100_000L,
            accommodationImpact = "DEDUCTION"
        )
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val result = WageCalculator.calculateDay(worker, att)
        assertEquals(70_000L, result.totalAllowances)
        assertEquals(100_000L, result.totalDeductions)
        // 1,000,000 + 70,000 - 100,000 = 970,000
        assertEquals(970_000L, result.netPayout)
    }

    // 19. Zero-floor cap: net payout never negative when deductions exceed earnings
    @Test
    fun testZeroFloor_negativeNetPayoutCappedAtZero() {
        val worker = baseDailyWorker.copy(
            baseDailyWage = 200_000L,
            accommodationAllowance = 500_000L,
            accommodationImpact = "DEDUCTION"
        )
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val result = WageCalculator.calculateDay(worker, att)
        assertEquals(-300_000L, result.netPayout)
        assertEquals(0L, result.clampedNetPayout)
        assertEquals(0L, WageCalculator.calculateDayPayout(worker, att, coerceNonNegative = true))
    }

    // 20. Rounding verification with roundToLong (no truncate error)
    @Test
    fun testRounding_roundToLongFunction() {
        // Half day odd base wage 750,001 -> 750,001 / 2.0 = 375,000.5 -> rounds to 375,001
        val worker = baseDailyWorker.copy(baseDailyWage = 750_001L)
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.HALF_DAY,
            regularHours = 4.0
        )
        val result = WageCalculator.calculateDay(worker, att)
        assertEquals(375_001L, result.baseWage)

        // Mathematical rounding checks
        assertEquals(1235L, WageCalculator.roundToLong(1234.5))
        assertEquals(1234L, WageCalculator.roundToLong(1234.4))
        assertEquals(1235L, WageCalculator.roundToLong(1234.6))
    }

    // 21. Monthly aggregation comparison: sum of daily net payouts equals calculateWorkerPerformance
    @Test
    fun testMonthlyAggregation_matchesSumOfDaily() {
        val worker = baseDailyWorker.copy(
            transitAllowance = 20_000L,
            transitImpact = "ALLOWANCE"
        )
        val day1 = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val day2 = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/02",
            status = AttendanceStatus.HALF_DAY,
            regularHours = 4.0
        )
        val day3 = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/03",
            status = AttendanceStatus.ABSENT,
            regularHours = 0.0
        )

        val res1 = WageCalculator.calculateDay(worker, day1)
        val res2 = WageCalculator.calculateDay(worker, day2)
        val res3 = WageCalculator.calculateDay(worker, day3)

        val totalDailyNet = res1.netPayout + res2.netPayout + res3.netPayout

        val perf = WageCalculator.calculateWorkerPerformance(worker, listOf(day1, day2, day3))
        assertEquals(2, perf.totalShifts) // absent day is NOT counted
        assertEquals(totalDailyNet, perf.netPayout)
        assertEquals(1_500_000L, perf.baseWageTotal)
        assertEquals(40_000L, perf.totalAllowances) // 20,000 * 2 present days
        assertEquals(12.0, perf.regularHours, 0.001)
    }

    // 22. Multiple attendance records aggregation with mixed present and absent days
    @Test
    fun testMultipleDays_withMixedAttendance() {
        val worker = baseDailyWorker.copy(
            overtimeRate = 100_000L,
            foodAllowance = 50_000L,
            foodImpact = "ALLOWANCE"
        )
        val attList = listOf(
            AttendanceEntity(workerId = 1L, date = "1405/01/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0, overtimeHours = 2.0),
            AttendanceEntity(workerId = 1L, date = "1405/01/02", status = AttendanceStatus.ABSENT),
            AttendanceEntity(workerId = 1L, date = "1405/01/03", status = AttendanceStatus.FULL_DAY, regularHours = 8.0, overtimeHours = 1.0)
        )
        val perf = WageCalculator.calculateWorkerPerformance(worker, attList)
        assertEquals(2, perf.totalShifts)
        assertEquals(2_000_000L, perf.baseWageTotal)
        assertEquals(300_000L, perf.overtimePayTotal) // (2 + 1) * 100,000
        assertEquals(100_000L, perf.totalAllowances) // 50,000 * 2 days
        assertEquals(2_400_000L, perf.netPayout)
    }

    // 23. Group expense share in calculateWorkerPerformance
    @Test
    fun testGroupExpenseShare_deductedFromNet() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val perf = WageCalculator.calculateWorkerPerformance(
            worker = baseDailyWorker,
            attendances = listOf(att),
            groupExpenseShare = 100_000L
        )
        assertEquals(1_000_000L, perf.baseWageTotal)
        assertEquals(100_000L, perf.groupExpenseShare)
        assertEquals(900_000L, perf.netPayout)
    }

    // 24. Absent worker: wages, overtime, allowances are zero, deductions STILL apply in performance
    @Test
    fun testAbsentDay_deductionsApply_allowancesWagesOvertimeDoNotApply() {
        val worker = baseDailyWorker.copy(
            transitAllowance = 40_000L,
            transitImpact = "ALLOWANCE", // Should NOT be paid when absent
            foodAllowance = 35_000L,
            foodImpact = "DEDUCTION",    // MUST be deducted even when absent
            overtimeRate = 120_000L
        )
        val dayPresent = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 2.0
        )
        val dayAbsent = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/02",
            status = AttendanceStatus.ABSENT,
            regularHours = 8.0, // Should be ignored
            overtimeHours = 3.0  // Overtime should NOT apply to absent day
        )

        val perf = WageCalculator.calculateWorkerPerformance(worker, listOf(dayPresent, dayAbsent))
        assertEquals(1, perf.totalShifts) // Only 1 shift worked
        assertEquals(8.0, perf.regularHours, 0.001)
        assertEquals(2.0, perf.overtimeHours, 0.001) // Only present day's overtime (2h)
        assertEquals(1_000_000L, perf.baseWageTotal)
        assertEquals(240_000L, perf.overtimePayTotal) // 2h * 120,000
        assertEquals(40_000L, perf.totalAllowances)   // Only present day's allowance (40k * 1)
        // Deductions apply to BOTH days: 35k * 2 = 70k!
        assertEquals(70_000L, perf.totalDeductions)
        // Net: 1,000,000 + 240,000 + 40,000 - 70,000 = 1,210,000
        assertEquals(1_210_000L, perf.netPayout)
    }

    // 25. Zero-capping applied ONLY ONCE at worker cumulative level, NOT daily
    @Test
    fun testZeroFloor_onlyAppliedAtWorkerCumulativeLevelNotDaily() {
        val worker = baseDailyWorker.copy(
            baseDailyWage = 300_000L,
            accommodationAllowance = 100_000L,
            accommodationImpact = "DEDUCTION"
        )
        // Day 1: Absent -> gross 0, deduction 100k -> raw net -100k, but day net clamped to 0
        val dayAbsent = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.ABSENT
        )
        // Day 2: Present -> gross 300k, deduction 100k -> net 200k
        val dayPresent = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/02",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )

        val resAbsent = WageCalculator.calculateDay(worker, dayAbsent)
        val resPresent = WageCalculator.calculateDay(worker, dayPresent)
        assertEquals(-100_000L, resAbsent.netPayout) // Raw daily net reflects true negative deduction balance
        assertEquals(0L, resAbsent.clampedNetPayout)  // Clamped >= 0 version for non-negative display
        assertEquals(200_000L, resPresent.netPayout)

        // Cumulative: gross (300k) - total deductions (100k + 100k = 200k) = 100,000L
        val perf = WageCalculator.calculateWorkerPerformance(worker, listOf(dayAbsent, dayPresent))
        assertEquals(300_000L, perf.baseWageTotal)
        assertEquals(200_000L, perf.totalDeductions)
        assertEquals(100_000L, perf.netPayout)

        // Consistency: Sum of daily net payouts strictly equals cumulative netPayout!
        val sumOfDailyNets = resAbsent.netPayout + resPresent.netPayout
        assertEquals(100_000L, sumOfDailyNets)
        assertEquals(perf.netPayout, sumOfDailyNets)

        // If total deductions exceed total gross, worker cumulative net is capped at 0
        val workerHighDeductions = worker.copy(
            accommodationAllowance = 250_000L // 250k * 2 = 500k deductions vs 300k gross
        )
        val perfCapped = WageCalculator.calculateWorkerPerformance(workerHighDeductions, listOf(dayAbsent, dayPresent))
        assertEquals(0L, perfCapped.netPayout)
        assertEquals(-200_000L, perfCapped.rawNetPayout)
    }

    // 26. Unified FinancialSummary: grandTotalProjectCost strictly equals sum of netPayoutBeforeGroup
    @Test
    fun testFinancialSummary_grandTotalMatchesSumOfWorkersNetPayoutBeforeGroup() {
        val workerA = baseDailyWorker.copy(id = 101L, baseDailyWage = 1_000_000L)
        val workerB = baseHourlyWorker.copy(id = 102L, baseHourlyWage = 200_000L, hourlyWageRate = 200_000L)

        val attA = AttendanceEntity(workerId = 101L, date = "1405/01/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0)
        val attB = AttendanceEntity(workerId = 102L, date = "1405/01/01", status = AttendanceStatus.HOURLY, hourlyHours = 5.0, hourlyWageRate = 200_000L)

        val groupExpense = com.karyar.app.data.local.entity.ExpenseEntity(
            id = 1L,
            folderId = 10L,
            title = "هزینه کرایه وانت جمعی",
            category = "TRANSIT",
            scope = "GROUP",
            amount = 300_000L,
            date = "1405/01/01"
        )

        val summary = WageCalculator.calculateFinancialSummary(
            workers = listOf(workerA, workerB),
            attendances = listOf(attA, attB),
            expenses = listOf(groupExpense),
            todayStr = "1405/01/01"
        )

        assertEquals(2, summary.workerPerformances.size)
        // Worker A: 1,000,000 netBeforeGroup; Worker B: 5 * 200k = 1,000,000 netBeforeGroup
        val totalBeforeGroup = summary.workerPerformances.sumOf { it.netPayoutBeforeGroup }
        assertEquals(2_000_000L, totalBeforeGroup)
        // با تعریف واحد، هزینه گروهی (300,000) از هزینه کل پروژه نیز کسر می‌شود تا با حساب روزانه یکسان باشد:
        val expectedGrandTotal = summary.workerPerformances.sumOf { it.rawNetPayout }
        assertEquals(1_700_000L, expectedGrandTotal)
        assertEquals(expectedGrandTotal, summary.grandTotalProjectCost)
        assertEquals(2, summary.totalWorkDaysCount)
        assertEquals(13.0, summary.totalWorkHours, 0.001) // 8 + 5
    }

    // 27. Individual and Group expenses strictly mapped by workerId and divided equally among attendees
    @Test
    fun testIndividualAndGroupExpenses_strictlyByWorkerIdAndSplitEqually() {
        val worker1 = baseDailyWorker.copy(id = 501L, baseDailyWage = 1_000_000L)
        val worker2 = baseDailyWorker.copy(id = 502L, baseDailyWage = 1_200_000L)
        val workerAbsent = baseDailyWorker.copy(id = 503L, baseDailyWage = 800_000L)

        val att1 = AttendanceEntity(workerId = 501L, date = "1405/01/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0)
        val att2 = AttendanceEntity(workerId = 502L, date = "1405/01/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0)
        val attAbsent = AttendanceEntity(workerId = 503L, date = "1405/01/01", status = AttendanceStatus.ABSENT)

        val groupExp = com.karyar.app.data.local.entity.ExpenseEntity(
            id = 1L, folderId = 10L, title = "کرایه جمعی", category = "TRANSIT", scope = "GROUP",
            amount = 200_000L, date = "1405/01/01"
        )
        val indivExp1 = com.karyar.app.data.local.entity.ExpenseEntity(
            id = 2L, folderId = 10L, title = "مساعده کارگر ۱", category = "OTHER", scope = "INDIVIDUAL",
            impactType = "DEDUCTION", workerId = 501L, amount = 50_000L, date = "1405/01/01"
        )
        val indivExp2 = com.karyar.app.data.local.entity.ExpenseEntity(
            id = 3L, folderId = 10L, title = "پاداش کارگر ۲", category = "OTHER", scope = "INDIVIDUAL",
            impactType = "ALLOWANCE", workerId = 502L, amount = 100_000L, date = "1405/01/01"
        )

        val summary = WageCalculator.calculateFinancialSummary(
            workers = listOf(worker1, worker2, workerAbsent),
            attendances = listOf(att1, att2, attAbsent),
            expenses = listOf(groupExp, indivExp1, indivExp2),
            todayStr = "1405/01/01"
        )

        // Only 2 workers worked (worker1 and worker2). Worker 3 was absent.
        // Group expense 200,000 / 2 = 100,000 per present worker
        val perf1 = summary.workerPerformances.first { it.worker.id == 501L }
        val perf2 = summary.workerPerformances.first { it.worker.id == 502L }
        val perfAbsent = summary.workerPerformances.first { it.worker.id == 503L }

        assertEquals(100_000L, perf1.groupExpenseShare)
        assertEquals(50_000L, perf1.totalDeductions) // indivExp1
        assertEquals(950_000L, perf1.netPayoutBeforeGroup) // 1,000,000 - 50,000
        assertEquals(850_000L, perf1.netPayout) // 950,000 - 100,000 (group)

        assertEquals(100_000L, perf2.groupExpenseShare)
        assertEquals(100_000L, perf2.totalAllowances) // indivExp2
        assertEquals(1_300_000L, perf2.netPayoutBeforeGroup) // 1,200,000 + 100,000
        assertEquals(1_200_000L, perf2.netPayout) // 1,300,000 - 100,000 (group)

        // Absent worker does NOT get group expense share
        assertEquals(0L, perfAbsent.groupExpenseShare)
        assertEquals(0L, perfAbsent.netPayout)
    }

    // 28. Problem 1 Verification: Daily bookkeeping directly deducts workshop group expenses
    @Test
    fun testDailyBookkeeping_directGroupExpenseDeduction() {
        val workerA = baseDailyWorker.copy(id = 601L, baseDailyWage = 500_000L)
        val workerB = baseDailyWorker.copy(id = 602L, baseDailyWage = 500_000L)

        val attA = AttendanceEntity(workerId = 601L, date = "1405/01/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0)
        val attB = AttendanceEntity(workerId = 602L, date = "1405/01/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0)

        val groupExpense = com.karyar.app.data.local.entity.ExpenseEntity(
            id = 10L, folderId = 10L, title = "هزینه ناهار گروهی کارگاه", category = "FOOD",
            scope = "GROUP", amount = 100_000L, date = "1405/01/01"
        )
        val indivExpense = com.karyar.app.data.local.entity.ExpenseEntity(
            id = 11L, folderId = 10L, title = "مساعده کارگر A", category = "OTHER",
            scope = "INDIVIDUAL", impactType = "DEDUCTION", workerId = 601L, amount = 50_000L, date = "1405/01/01"
        )

        val daily = WageCalculator.calculateDailyBookkeeping(
            dateStr = "1405/01/01",
            workers = listOf(workerA, workerB),
            attendances = listOf(attA, attB),
            expenses = listOf(groupExpense, indivExpense)
        )

        assertEquals(1_000_000L, daily.totalDailyWages) // 500k + 500k
        assertEquals(150_000L, daily.dailyExpenses)     // 100k group + 50k indiv
        assertEquals(100_000L, daily.groupExpenses)     // 100k group
        assertEquals(50_000L, daily.individualExpenses) // 50k indiv
        assertEquals(2, daily.workersPresent)

        // Wages (1,000,000) - Deductions (50,000) - Group Expense (100,000) = 850,000L
        assertEquals(850_000L, daily.grandDailyCost)

        // Consistency check: The sum of worker performances matches grandDailyCost!
        val summary = WageCalculator.calculateFinancialSummary(
            workers = listOf(workerA, workerB),
            attendances = listOf(attA, attB),
            expenses = listOf(groupExpense, indivExpense),
            todayStr = "1405/01/01"
        )
        val sumOfWorkerNetPayouts = summary.workerPerformances.sumOf { it.netPayout }
        assertEquals(daily.grandDailyCost, sumOfWorkerNetPayouts)
    }

    // 29. Problem 2 Verification: Remainder of group expense division is fully distributed without loss
    @Test
    fun testGroupExpenseRemainder_splitEquallyWithRemainderDistributed() {
        val worker1 = baseDailyWorker.copy(id = 1L)
        val worker2 = baseDailyWorker.copy(id = 2L)
        val worker3 = baseDailyWorker.copy(id = 3L)

        // 100,000 Tomans divided among 3 workers: 33,333 each with remainder 1
        val shares = WageCalculator.calculateGroupExpenseShares(100_000L, listOf(worker1, worker2, worker3))
        assertEquals(3, shares.size)
        // First worker (by sorted ID) gets the remainder: 33,334
        assertEquals(33_334L, shares[1L])
        assertEquals(33_333L, shares[2L])
        assertEquals(33_333L, shares[3L])

        // Sum of worker shares strictly equals the original 100,000L!
        assertEquals(100_000L, shares.values.sum())

        // Also test with 7 workers and 100,000 Tomans (100,000 / 7 = 14,285 remainder 5)
        val sevenWorkers = (1L..7L).map { baseDailyWorker.copy(id = it) }
        val shares7 = WageCalculator.calculateGroupExpenseShares(100_000L, sevenWorkers)
        assertEquals(7, shares7.size)
        assertEquals(100_000L, shares7.values.sum())
    }

    // 30. Problem 3 Verification: Daily net payout and final settlement are strictly consistent across days
    @Test
    fun testDailyNetPayoutAndFinalSettlement_perfectConsistencyAcrossDays() {
        val worker = baseDailyWorker.copy(
            id = 10L,
            baseDailyWage = 400_000L,
            accommodationAllowance = 150_000L,
            accommodationImpact = "DEDUCTION"
        )

        // Day 1: Absent -> gross 0, deduction 150k -> daily net -150k
        val attDay1 = AttendanceEntity(workerId = 10L, date = "1405/01/01", status = AttendanceStatus.ABSENT)
        // Day 2: Half-day -> wage 200k, deduction 150k -> daily net +50k
        val attDay2 = AttendanceEntity(workerId = 10L, date = "1405/01/02", status = AttendanceStatus.HALF_DAY, regularHours = 4.0)
        // Day 3: Full-day -> wage 400k, deduction 150k -> daily net +250k
        val attDay3 = AttendanceEntity(workerId = 10L, date = "1405/01/03", status = AttendanceStatus.FULL_DAY, regularHours = 8.0)

        val res1 = WageCalculator.calculateDay(worker, attDay1)
        val res2 = WageCalculator.calculateDay(worker, attDay2)
        val res3 = WageCalculator.calculateDay(worker, attDay3)

        assertEquals(-150_000L, res1.netPayout)
        assertEquals(50_000L, res2.netPayout)
        assertEquals(250_000L, res3.netPayout)

        val sumDailyNets = res1.netPayout + res2.netPayout + res3.netPayout
        assertEquals(150_000L, sumDailyNets) // -150k + 50k + 250k = 150k

        val performance = WageCalculator.calculateWorkerPerformance(worker, listOf(attDay1, attDay2, attDay3))
        assertEquals(600_000L, performance.baseWageTotal)     // 0 + 200k + 400k
        assertEquals(450_000L, performance.totalDeductions)   // 150k * 3
        assertEquals(150_000L, performance.netPayout)         // 600k - 450k = 150k

        // The sum of daily numbers strictly matches the final settlement!
        assertEquals(sumDailyNets, performance.netPayout)
    }

    // 31. تست الف: کارگری با یک روز غیبت و کسورات بیشتر از دستمزد کل، که قابل پرداخت صفر و بدهی مثبت دارد
    @Test
    fun testAbsentDayWithDeductionsExceedingGross_hasZeroPayoutAndPositiveDebt() {
        val worker = baseDailyWorker.copy(
            id = 701L,
            baseDailyWage = 500_000L,
            accommodationAllowance = 200_000L,
            accommodationImpact = "DEDUCTION"
        )
        // یک روز غیبت: دستمزد و مزایا صفر، کسورات ۲۰۰,۰۰۰ تومان
        val attAbsent = AttendanceEntity(
            workerId = 701L,
            date = "1405/01/01",
            status = AttendanceStatus.ABSENT
        )

        val perf = WageCalculator.calculateWorkerPerformance(worker, listOf(attAbsent))

        // ۱. دستمزد و کارکرد صفر است چون غایب بوده
        assertEquals(0, perf.totalShifts)
        assertEquals(0L, perf.baseWageTotal)
        assertEquals(200_000L, perf.totalDeductions)

        // ۲. مبلغ خام برابر ۲۰۰,۰۰۰- تومان است
        assertEquals(-200_000L, perf.rawNetPayout)

        // ۳. قانون پروژه: قابل پرداخت = max(raw, 0) -> صفر
        assertEquals(0L, perf.netPayout)

        // ۴. قانون پروژه: بدهی کارگر = max(-raw, 0) -> ۲۰۰,۰۰۰ تومان
        assertEquals(200_000L, perf.debtAmount)
    }

    // 32. تست ب: جمع netPayout خام همه روزها برابر rawNetPayout کل باشد
    @Test
    fun testSumOfRawDailyNetsEqualsTotalRawNetPayout() {
        val worker = baseDailyWorker.copy(
            id = 702L,
            baseDailyWage = 600_000L,
            foodAllowance = 100_000L,
            foodImpact = "DEDUCTION",
            transitAllowance = 50_000L,
            transitImpact = "ALLOWANCE"
        )

        // روز اول: تمام روز (دستمزد ۶۰۰k + مزایا ۵۰k - کسورات ۱۰۰k = ۵۵۰k)
        val day1 = AttendanceEntity(workerId = 702L, date = "1405/01/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0)
        // روز دوم: غایب (دستمزد ۰ + مزایا ۰ - کسورات ۱۰۰k = ۱۰۰k-)
        val day2 = AttendanceEntity(workerId = 702L, date = "1405/01/02", status = AttendanceStatus.ABSENT)
        // روز سوم: نصف روز (دستمزد ۳۰۰k + مزایا ۵۰k - کسورات ۱۰۰k = ۲۵۰k)
        val day3 = AttendanceEntity(workerId = 702L, date = "1405/01/03", status = AttendanceStatus.HALF_DAY, regularHours = 4.0)
        // روز چهارم: غایب با کسورات اضافی (دستمزد ۰ + مزایا ۰ - کسورات ۱۰۰k = ۱۰۰k-)
        val day4 = AttendanceEntity(workerId = 702L, date = "1405/01/04", status = AttendanceStatus.ABSENT)

        val res1 = WageCalculator.calculateDay(worker, day1)
        val res2 = WageCalculator.calculateDay(worker, day2)
        val res3 = WageCalculator.calculateDay(worker, day3)
        val res4 = WageCalculator.calculateDay(worker, day4)

        assertEquals(550_000L, res1.netPayout)
        assertEquals(-100_000L, res2.netPayout)
        assertEquals(250_000L, res3.netPayout)
        assertEquals(-100_000L, res4.netPayout)

        val sumRawDailyNets = res1.netPayout + res2.netPayout + res3.netPayout + res4.netPayout
        assertEquals(600_000L, sumRawDailyNets)

        val perf = WageCalculator.calculateWorkerPerformance(worker, listOf(day1, day2, day3, day4))

        // جمع netPayout خام همه روزها باید دقیقاً برابر rawNetPayout کل باشد
        assertEquals(sumRawDailyNets, perf.rawNetPayout)

        // همچنین در حالتی که مجموع کسورات بیشتر از کل دستمزدها شود
        val workerIndebted = worker.copy(
            foodAllowance = 600_000L // 600k * 4 = 2,400,000L deductions vs 900,000 wage + 100,000 allowance = 1,000,000L gross
        )
        val resDebt1 = WageCalculator.calculateDay(workerIndebted, day1) // 600k + 50k - 600k = 50k
        val resDebt2 = WageCalculator.calculateDay(workerIndebted, day2) // -600k
        val resDebt3 = WageCalculator.calculateDay(workerIndebted, day3) // 300k + 50k - 600k = -250k
        val resDebt4 = WageCalculator.calculateDay(workerIndebted, day4) // -600k

        val sumDebtDailyNets = resDebt1.netPayout + resDebt2.netPayout + resDebt3.netPayout + resDebt4.netPayout
        assertEquals(-1_400_000L, sumDebtDailyNets)

        val perfDebt = WageCalculator.calculateWorkerPerformance(workerIndebted, listOf(day1, day2, day3, day4))
        assertEquals(sumDebtDailyNets, perfDebt.rawNetPayout)
        assertEquals(0L, perfDebt.netPayout)
        assertEquals(1_400_000L, perfDebt.debtAmount)
    }

    // 33. هماهنگی کامل حساب روزانه و کل پروژه (بدون هزینه گروهی)
    @Test
    fun testSumOfGrandDailyCostEqualsGrandTotalProjectCost_withoutGroupExpenses() {
        val worker1 = baseDailyWorker.copy(
            id = 801L,
            baseDailyWage = 1_000_000L,
            transitAllowance = 50_000L,
            transitImpact = "ALLOWANCE",
            foodAllowance = 100_000L,
            foodImpact = "DEDUCTION"
        )
        val worker2 = baseHourlyWorker.copy(
            id = 802L,
            baseHourlyWage = 150_000L,
            hourlyWageRate = 150_000L
        )

        val dates = listOf("1405/02/01", "1405/02/02", "1405/02/03")
        val attendances = listOf(
            AttendanceEntity(workerId = 801L, date = "1405/02/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0),
            AttendanceEntity(workerId = 802L, date = "1405/02/01", status = AttendanceStatus.HOURLY, hourlyHours = 6.0, hourlyWageRate = 150_000L),
            AttendanceEntity(workerId = 801L, date = "1405/02/02", status = AttendanceStatus.HALF_DAY, regularHours = 4.0),
            AttendanceEntity(workerId = 802L, date = "1405/02/02", status = AttendanceStatus.HOURLY, hourlyHours = 4.0, hourlyWageRate = 150_000L),
            AttendanceEntity(workerId = 801L, date = "1405/02/03", status = AttendanceStatus.FULL_DAY, regularHours = 8.0, overtimeHours = 2.0, overtimeRate = 200_000L)
        )
        val expenses = listOf(
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 101L, folderId = 10L, title = "پاداش کارگر ۱", category = "OTHER",
                scope = "INDIVIDUAL", impactType = "ALLOWANCE", workerId = 801L, amount = 80_000L, date = "1405/02/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 102L, folderId = 10L, title = "مساعده کارگر ۲", category = "OTHER",
                scope = "INDIVIDUAL", impactType = "DEDUCTION", workerId = 802L, amount = 120_000L, date = "1405/02/02"
            )
        )

        val workers = listOf(worker1, worker2)
        val sumGrandDailyCost = dates.sumOf { dateStr ->
            WageCalculator.calculateDailyBookkeeping(
                dateStr = dateStr,
                workers = workers,
                attendances = attendances,
                expenses = expenses
            ).grandDailyCost
        }

        val summary = WageCalculator.calculateFinancialSummary(
            workers = workers,
            attendances = attendances,
            expenses = expenses,
            todayStr = "1405/02/03"
        )

        assertEquals(sumGrandDailyCost, summary.grandTotalProjectCost)
    }

    // 34. هماهنگی کامل حساب روزانه و کل پروژه (با هزینه گروهی و مثال صریح صورت‌مسئله)
    @Test
    fun testSumOfGrandDailyCostEqualsGrandTotalProjectCost_withGroupExpenses() {
        // الف) مثال صریح صورت‌مسئله: یک کارگر با دستمزد 1,000,000 و هزینه گروهی 100,000 -> هر دو 900,000
        val singleWorker = baseDailyWorker.copy(id = 810L, baseDailyWage = 1_000_000L)
        val singleAtt = AttendanceEntity(workerId = 810L, date = "1405/02/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0)
        val singleGroupExp = com.karyar.app.data.local.entity.ExpenseEntity(
            id = 201L, folderId = 10L, title = "ناهار کارگاه", category = "FOOD",
            scope = "GROUP", amount = 100_000L, date = "1405/02/01"
        )

        val singleDaily = WageCalculator.calculateDailyBookkeeping(
            dateStr = "1405/02/01",
            workers = listOf(singleWorker),
            attendances = listOf(singleAtt),
            expenses = listOf(singleGroupExp)
        )
        val singleSummary = WageCalculator.calculateFinancialSummary(
            workers = listOf(singleWorker),
            attendances = listOf(singleAtt),
            expenses = listOf(singleGroupExp),
            todayStr = "1405/02/01"
        )

        assertEquals(900_000L, singleDaily.grandDailyCost)
        assertEquals(900_000L, singleSummary.grandTotalProjectCost)
        assertEquals(singleDaily.grandDailyCost, singleSummary.grandTotalProjectCost)

        // ب) سناریوی چندروزه و چندکارگره با هزینه‌های گروهی (شامل باقیمانده تقسیم بر ۳)
        val w1 = baseDailyWorker.copy(id = 811L, baseDailyWage = 1_000_000L)
        val w2 = baseDailyWorker.copy(id = 812L, baseDailyWage = 800_000L)
        val w3 = baseDailyWorker.copy(id = 813L, baseDailyWage = 900_000L)
        val workers = listOf(w1, w2, w3)

        val dates = listOf("1405/02/01", "1405/02/02")
        val attendances = listOf(
            AttendanceEntity(workerId = 811L, date = "1405/02/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0),
            AttendanceEntity(workerId = 812L, date = "1405/02/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0),
            AttendanceEntity(workerId = 813L, date = "1405/02/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0),
            AttendanceEntity(workerId = 811L, date = "1405/02/02", status = AttendanceStatus.HALF_DAY, regularHours = 4.0),
            AttendanceEntity(workerId = 812L, date = "1405/02/02", status = AttendanceStatus.FULL_DAY, regularHours = 8.0)
        )
        val expenses = listOf(
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 202L, folderId = 10L, title = "سرویس روز اول", category = "TRANSIT",
                scope = "GROUP", amount = 100_000L, date = "1405/02/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 203L, folderId = 10L, title = "ناهار روز دوم", category = "FOOD",
                scope = "GROUP", amount = 250_000L, date = "1405/02/02"
            )
        )

        val sumGrandDailyCost = dates.sumOf { dateStr ->
            WageCalculator.calculateDailyBookkeeping(
                dateStr = dateStr,
                workers = workers,
                attendances = attendances,
                expenses = expenses
            ).grandDailyCost
        }
        val summary = WageCalculator.calculateFinancialSummary(
            workers = workers,
            attendances = attendances,
            expenses = expenses,
            todayStr = "1405/02/02"
        )

        assertEquals(sumGrandDailyCost, summary.grandTotalProjectCost)
    }

    // 35. هماهنگی کامل حساب روزانه و کل پروژه (با روز غیبت، کسورات و بدون clamp روزانه)
    @Test
    fun testSumOfGrandDailyCostEqualsGrandTotalProjectCost_withAbsentDayAndDeductions() {
        val worker1 = baseDailyWorker.copy(
            id = 821L,
            baseDailyWage = 600_000L,
            accommodationAllowance = 200_000L,
            accommodationImpact = "DEDUCTION"
        )
        val worker2 = baseDailyWorker.copy(
            id = 822L,
            baseDailyWage = 500_000L,
            foodAllowance = 150_000L,
            foodImpact = "DEDUCTION"
        )
        val workers = listOf(worker1, worker2)

        // روز اول: هر دو غایب هستند -> دستمزد ۰، کسورات ۲۰۰k + ۱۵۰k = ۳۵۰k، هزینه گروهی ۵۰k -> هزینه خام روز اول = ۴۰۰,۰۰۰-
        // روز دوم: هر دو حاضر تمام‌وقت -> دستمزد ۱,۱۰۰k، کسورات ۳۵۰k، هزینه گروهی ۱۰۰k -> هزینه خام روز دوم = ۶۵۰,۰۰۰+
        val dates = listOf("1405/02/01", "1405/02/02")
        val attendances = listOf(
            AttendanceEntity(workerId = 821L, date = "1405/02/01", status = AttendanceStatus.ABSENT),
            AttendanceEntity(workerId = 822L, date = "1405/02/01", status = AttendanceStatus.ABSENT),
            AttendanceEntity(workerId = 821L, date = "1405/02/02", status = AttendanceStatus.FULL_DAY, regularHours = 8.0),
            AttendanceEntity(workerId = 822L, date = "1405/02/02", status = AttendanceStatus.FULL_DAY, regularHours = 8.0)
        )
        val expenses = listOf(
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 301L, folderId = 10L, title = "هزینه گروهی روز غیبت", category = "OTHER",
                scope = "GROUP", amount = 50_000L, date = "1405/02/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 302L, folderId = 10L, title = "هزینه گروهی روز کاری", category = "FOOD",
                scope = "GROUP", amount = 100_000L, date = "1405/02/02"
            )
        )

        val day1Bookkeeping = WageCalculator.calculateDailyBookkeeping("1405/02/01", workers, attendances, expenses)
        val day2Bookkeeping = WageCalculator.calculateDailyBookkeeping("1405/02/02", workers, attendances, expenses)

        // روز اول نباید به صفر clamp شود؛ باید مقدار خام منفی (-400,000) باشد
        assertEquals(-400_000L, day1Bookkeeping.grandDailyCost)
        assertEquals(650_000L, day2Bookkeeping.grandDailyCost)

        val sumGrandDailyCost = day1Bookkeeping.grandDailyCost + day2Bookkeeping.grandDailyCost
        assertEquals(250_000L, sumGrandDailyCost)

        val summary = WageCalculator.calculateFinancialSummary(
            workers = workers,
            attendances = attendances,
            expenses = expenses,
            todayStr = "1405/02/02"
        )

        // جمع grandDailyCost همه روزها باید دقیقاً برابر grandTotalProjectCost باشد
        assertEquals(sumGrandDailyCost, summary.grandTotalProjectCost)

        // همچنین اگر کل پروژه منفی باشد (فقط روز اول غیبت):
        val onlyDay1Summary = WageCalculator.calculateFinancialSummary(
            workers = workers,
            attendances = attendances.filter { it.date == "1405/02/01" },
            expenses = expenses.filter { it.date == "1405/02/01" },
            todayStr = "1405/02/01"
        )
        assertEquals(-400_000L, onlyDay1Summary.grandTotalProjectCost)
        assertEquals(day1Bookkeeping.grandDailyCost, onlyDay1Summary.grandTotalProjectCost)
    }

    // 36. تفکیک هزینه‌های فردی «سایر» (OTHER) از ایاب و ذهاب بدون تغییر در مجموع کل
    @Test
    fun testOtherIndividualExpensesSeparatedFromTransit() {
        val worker = baseDailyWorker.copy(
            id = 901L,
            baseDailyWage = 1_000_000L,
            transitAllowance = 40_000L,
            transitImpact = "ALLOWANCE",
            foodAllowance = 60_000L,
            foodImpact = "DEDUCTION"
        )
        val attendances = listOf(
            AttendanceEntity(
                workerId = 901L,
                date = "1405/03/01",
                status = AttendanceStatus.FULL_DAY,
                regularHours = 8.0
            )
        )
        val individualExpenses = listOf(
            // ایاب و ذهاب افزایشی و کاهشی (با حروف کوچک و بزرگ برای تست ignoreCase)
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 401L, folderId = 1L, title = "اسنپ", category = "transit",
                scope = "INDIVIDUAL", impactType = "allowance", workerId = 901L, amount = 30_000L, date = "1405/03/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 402L, folderId = 1L, title = "جریمه سرویس", category = "TRANSIT",
                scope = "INDIVIDUAL", impactType = "DEDUCTION", workerId = 901L, amount = 15_000L, date = "1405/03/01"
            ),
            // خوراک، مسکن و درمان
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 403L, folderId = 1L, title = "صبحانه", category = "food",
                scope = "INDIVIDUAL", impactType = "ALLOWANCE", workerId = 901L, amount = 25_000L, date = "1405/03/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 404L, folderId = 1L, title = "اجاره اتاق", category = "AcCoMmOdAtIoN",
                scope = "INDIVIDUAL", impactType = "deduction", workerId = 901L, amount = 80_000L, date = "1405/03/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 405L, folderId = 1L, title = "دارو", category = "Medical",
                scope = "INDIVIDUAL", impactType = "ALLOWANCE", workerId = 901L, amount = 50_000L, date = "1405/03/01"
            ),
            // هزینه‌های فردی OTHER (هم افزایشی و هم کاهشی) + یک دسته سفارشی متفرقه
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 406L, folderId = 1L, title = "پاداش ویژه", category = "OTHER",
                scope = "INDIVIDUAL", impactType = "ALLOWANCE", workerId = 901L, amount = 120_000L, date = "1405/03/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 407L, folderId = 1L, title = "خرید ابزار شخصی", category = "other",
                scope = "INDIVIDUAL", impactType = "ALLOWANCE", workerId = 901L, amount = 30_000L, date = "1405/03/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 408L, folderId = 1L, title = "مساعده نقدی", category = "OTHER",
                scope = "INDIVIDUAL", impactType = "DEDUCTION", workerId = 901L, amount = 200_000L, date = "1405/03/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 409L, folderId = 1L, title = "خسارت ابزار", category = "misc_custom",
                scope = "INDIVIDUAL", impactType = "deduction", workerId = 901L, amount = 45_000L, date = "1405/03/01"
            )
        )

        val perf = WageCalculator.calculateWorkerPerformance(
            worker = worker,
            attendances = attendances,
            individualExpenses = individualExpenses
        )

        // ۱. بررسی تفکیک دقیق دسته‌ها (به‌ویژه عدم قاطی شدن OTHER با ایاب و ذهاب)
        // ایاب و ذهاب افزایشی: ۴۰,۰۰۰ (پروفایل روزانه) + ۳۰,۰۰۰ (هزینه فردی) = ۷۰,۰۰۰
        assertEquals(70_000L, perf.transitAllowanceTotal)
        // ایاب و ذهاب کاهشی: فقط ۱۵,۰۰۰
        assertEquals(15_000L, perf.transitDeductionTotal)

        // خوراک، مسکن، درمان
        assertEquals(25_000L, perf.foodAllowanceTotal)
        assertEquals(60_000L, perf.foodDeductionTotal)
        assertEquals(0L, perf.accommodationAllowanceTotal)
        assertEquals(80_000L, perf.accommodationDeductionTotal)
        assertEquals(50_000L, perf.medicalAllowanceTotal)
        assertEquals(0L, perf.medicalDeductionTotal)

        // سایر (OTHER و دسته‌های متفرقه):
        // افزایشی: ۱۲۰,۰۰۰ + ۳۰,۰۰۰ = ۱۵۰,۰۰۰
        assertEquals(150_000L, perf.otherAllowanceTotal)
        // کاهشی: ۲۰۰,۰۰۰ + ۴۵,۰۰۰ = ۲۴۵,۰۰۰
        assertEquals(245_000L, perf.otherDeductionTotal)

        // ۲. بررسی عدم تغییر مجموع کل مزایا، کسورات و خالص دریافتی
        // کل مزایا: 70,000 + 25,000 + 0 + 50,000 + 150,000 = 295,000
        assertEquals(295_000L, perf.totalAllowances)
        // کل کسورات: 15,000 + 60,000 + 80,000 + 0 + 245,000 = 400,000
        assertEquals(400_000L, perf.totalDeductions)
        // خالص دریافتی: 1,000,000 + 295,000 - 400,000 = 895,000
        assertEquals(895_000L, perf.netPayout)
        assertEquals(895_000L, perf.rawNetPayout)
    }

    @Test
    fun testCustomDay_zeroDailyWage_doesNotFallbackToWorkerProfile() {
        val worker = baseDailyWorker.copy(baseDailyWage = 1_200_000L)
        val attFullZero = AttendanceEntity(
            workerId = worker.id,
            date = "1405/04/01",
            status = AttendanceStatus.FULL_DAY,
            dailyWage = 0L,
            hasCustomAllowances = true
        )
        val attHalfZero = AttendanceEntity(
            workerId = worker.id,
            date = "1405/04/02",
            status = AttendanceStatus.HALF_DAY,
            dailyWage = 0L,
            hasCustomAllowances = true
        )

        val resFull = WageCalculator.calculateDay(worker, attFullZero)
        val resHalf = WageCalculator.calculateDay(worker, attHalfZero)

        assertTrue(resFull.isWorkingDay)
        assertEquals(0L, resFull.baseWage)
        assertEquals(0L, resFull.netPayout)

        assertTrue(resHalf.isWorkingDay)
        assertEquals(0L, resHalf.baseWage)
        assertEquals(0L, resHalf.netPayout)
    }

    @Test
    fun testCustomDay_zeroHourlyRateOrHours_doesNotFallbackToWorkerProfile() {
        val worker = baseHourlyWorker.copy(
            isHourlyEnabled = true,
            hourlyWageRate = 180_000L,
            baseHourlyWage = 180_000L,
            hourlyHours = 8.0
        )
        val attZeroRate = AttendanceEntity(
            workerId = worker.id,
            date = "1405/04/01",
            status = AttendanceStatus.HOURLY,
            hourlyHours = 6.0,
            hourlyWageRate = 0L,
            hourlyWage = 0L,
            hasCustomAllowances = true
        )
        val attZeroHours = AttendanceEntity(
            workerId = worker.id,
            date = "1405/04/02",
            status = AttendanceStatus.HOURLY,
            hourlyHours = 0.0,
            hourlyWageRate = 180_000L,
            hourlyWage = 180_000L,
            hasCustomAllowances = true
        )

        val resZeroRate = WageCalculator.calculateDay(worker, attZeroRate)
        val resZeroHours = WageCalculator.calculateDay(worker, attZeroHours)

        assertEquals(0L, resZeroRate.hourlyPay)
        assertEquals(0L, resZeroRate.netPayout)
        assertEquals(0L, resZeroHours.hourlyPay)
        assertEquals(0L, resZeroHours.netPayout)
    }

    @Test
    fun testCustomDay_zeroOvertimeRateOrHours_doesNotFallbackToWorkerProfile() {
        val worker = baseDailyWorker.copy(
            baseDailyWage = 1_000_000L,
            isOvertimeEnabled = true,
            overtimeRate = 200_000L,
            overtimeHours = 3.0
        )
        val attZeroOtRate = AttendanceEntity(
            workerId = worker.id,
            date = "1405/04/01",
            status = AttendanceStatus.FULL_DAY,
            dailyWage = 1_000_000L,
            overtimeHours = 3.0,
            overtimeRate = 0L,
            hasCustomAllowances = true
        )
        val attZeroOtHours = AttendanceEntity(
            workerId = worker.id,
            date = "1405/04/02",
            status = AttendanceStatus.FULL_DAY,
            dailyWage = 1_000_000L,
            overtimeHours = 0.0,
            overtimeRate = 200_000L,
            hasCustomAllowances = true
        )

        val resZeroOtRate = WageCalculator.calculateDay(worker, attZeroOtRate)
        val resZeroOtHours = WageCalculator.calculateDay(worker, attZeroOtHours)

        assertEquals(0L, resZeroOtRate.overtimePay)
        assertEquals(1_000_000L, resZeroOtRate.netPayout)
        assertEquals(0L, resZeroOtHours.overtimePay)
        assertEquals(1_000_000L, resZeroOtHours.netPayout)
    }

    // 37. اعتبارسنجی ثبت هزینه (مبلغ > ۰، سقف معقول، اجباری بودن workerId در حالت INDIVIDUAL)
    @Test
    fun testValidateExpense_enforcesAmountBoundsAndMandatoryWorkerForIndividual() {
        val validWorkers = setOf(10L, 20L)

        // ۱. هزینه فردی بدون کارگر (workerId == null) -> خطا
        val indivWithoutWorker = com.karyar.app.data.local.entity.ExpenseEntity(
            title = "مساعده",
            category = "OTHER",
            scope = "INDIVIDUAL",
            impactType = "DEDUCTION",
            workerId = null,
            amount = 200_000L,
            date = "1405/02/10"
        )
        assertTrue(WageCalculator.validateExpense(indivWithoutWorker, validWorkers) != null)

        // ۲. هزینه با مبلغ صفر یا منفی -> خطا
        val zeroAmount = indivWithoutWorker.copy(workerId = 10L, amount = 0L)
        val negativeAmount = indivWithoutWorker.copy(workerId = 10L, amount = -50_000L)
        assertTrue(WageCalculator.validateExpense(zeroAmount, validWorkers) != null)
        assertTrue(WageCalculator.validateExpense(negativeAmount, validWorkers) != null)

        // ۳. هزینه بیش از سقف معقول -> خطا
        val excessiveAmount = indivWithoutWorker.copy(
            workerId = 10L,
            amount = WageCalculator.MAX_EXPENSE_AMOUNT + 1L
        )
        assertTrue(WageCalculator.validateExpense(excessiveAmount, validWorkers) != null)

        // ۴. هزینه فردی معتبر با کارگر مشخص -> بدون خطا
        val validIndividual = indivWithoutWorker.copy(workerId = 10L, amount = 500_000L)
        assertEquals(null, WageCalculator.validateExpense(validIndividual, validWorkers))

        // ۵. هزینه گروهی معتبر بدون کارگر -> بدون خطا
        val validGroup = com.karyar.app.data.local.entity.ExpenseEntity(
            title = "ناهار کارگاه",
            category = "FOOD",
            scope = "GROUP",
            impactType = "DEDUCTION",
            workerId = null,
            amount = 450_000L,
            date = "1405/02/10"
        )
        assertEquals(null, WageCalculator.validateExpense(validGroup, validWorkers))
    }

    // 38. تست جامع یکسان‌بودن جمع حساب روزانه و جمع کل پروژه با ترکیبی از هزینه‌های فردی (افزایشی/کاهشی) و گروهی در تمام دسته‌ها
    @Test
    fun testSumOfDailyBookkeepingEqualsProjectTotal_withAllExpenseCategoriesScopesAndImpacts() {
        val w1 = baseDailyWorker.copy(
            id = 951L,
            name = "رضا محمدی",
            baseDailyWage = 1_200_000L,
            transitAllowance = 50_000L,
            transitImpact = "ALLOWANCE"
        )
        val w2 = baseHourlyWorker.copy(
            id = 952L,
            name = "مهدی حسینی",
            isHourlyEnabled = true,
            hourlyWageRate = 180_000L,
            baseHourlyWage = 180_000L
        )
        val w3 = baseDailyWorker.copy(
            id = 953L,
            name = "سعید کریمی",
            baseDailyWage = 900_000L,
            accommodationAllowance = 100_000L,
            accommodationImpact = "DEDUCTION"
        )
        val workers = listOf(w1, w2, w3)

        val attendances = listOf(
            // روز اول: هر سه حاضر
            AttendanceEntity(workerId = 951L, date = "1405/03/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0, overtimeHours = 2.0, overtimeRate = 220_000L),
            AttendanceEntity(workerId = 952L, date = "1405/03/01", status = AttendanceStatus.HOURLY, hourlyHours = 7.0, hourlyWageRate = 180_000L),
            AttendanceEntity(workerId = 953L, date = "1405/03/01", status = AttendanceStatus.HALF_DAY, regularHours = 4.0),
            // روز دوم: کارگر ۳ غایب است (کسورات اسکان باید همچنان اعمال شود)
            AttendanceEntity(workerId = 951L, date = "1405/03/02", status = AttendanceStatus.FULL_DAY, regularHours = 8.0),
            AttendanceEntity(workerId = 952L, date = "1405/03/02", status = AttendanceStatus.HOURLY, hourlyHours = 5.0, hourlyWageRate = 180_000L),
            AttendanceEntity(workerId = 953L, date = "1405/03/02", status = AttendanceStatus.ABSENT)
        )

        // شامل روز سوم ("1405/03/03") که فقط هزینه ثبت شده است
        val expenses = listOf(
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 501L, folderId = 1L, title = "سرویس کارگاه", category = "TRANSIT",
                scope = "GROUP", impactType = "DEDUCTION", workerId = null, amount = 300_000L, date = "1405/03/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 502L, folderId = 1L, title = "کمک‌هزینه ایاب و ذهاب رضا", category = "TRANSIT",
                scope = "INDIVIDUAL", impactType = "ALLOWANCE", workerId = 951L, amount = 70_000L, date = "1405/03/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 503L, folderId = 1L, title = "ناهار مهدی", category = "FOOD",
                scope = "INDIVIDUAL", impactType = "DEDUCTION", workerId = 952L, amount = 95_000L, date = "1405/03/01"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 504L, folderId = 1L, title = "اجاره خوابگاه", category = "ACCOMMODATION",
                scope = "GROUP", impactType = "DEDUCTION", workerId = null, amount = 500_000L, date = "1405/03/02"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 505L, folderId = 1L, title = "کمک‌هزینه دارو سعید", category = "MEDICAL",
                scope = "INDIVIDUAL", impactType = "ALLOWANCE", workerId = 953L, amount = 140_000L, date = "1405/03/02"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 506L, folderId = 1L, title = "مساعده نقدی رضا", category = "OTHER",
                scope = "INDIVIDUAL", impactType = "DEDUCTION", workerId = 951L, amount = 250_000L, date = "1405/03/03"
            ),
            com.karyar.app.data.local.entity.ExpenseEntity(
                id = 507L, folderId = 1L, title = "پاداش ویژه مهدی", category = "OTHER",
                scope = "INDIVIDUAL", impactType = "ALLOWANCE", workerId = 952L, amount = 110_000L, date = "1405/03/03"
            )
        )

        val allDates = (attendances.map { it.date } + expenses.map { it.date }).distinct().sorted()
        val sumOfDailyBookkeeping = allDates.sumOf { dateStr ->
            WageCalculator.calculateDailyBookkeeping(
                dateStr = dateStr,
                workers = workers,
                attendances = attendances,
                expenses = expenses
            ).grandDailyCost
        }

        val projectSummary = WageCalculator.calculateFinancialSummary(
            workers = workers,
            attendances = attendances,
            expenses = expenses,
            todayStr = "1405/03/03"
        )

        // ۱. جمع حساب روزانه تمامی روزها باید دقیقاً برابر جمع کل پروژه باشد
        assertEquals(sumOfDailyBookkeeping, projectSummary.grandTotalProjectCost)

        // ۲. همچنین جمع خالص خام (rawNetPayout) تمام کارگران باید دقیقاً برابر جمع کل پروژه باشد
        val sumOfWorkersRawNet = projectSummary.workerPerformances.sumOf { it.rawNetPayout }
        assertEquals(projectSummary.grandTotalProjectCost, sumOfWorkersRawNet)
    }

    // ==================== تست‌های سقف ورودی‌ها، جلوگیری از سرریز و اعتبارسنجی کد ملی ====================

    @Test
    fun `boundary validation for wage amounts accepts 0 to 1_000_000_000 and rejects out of bounds`() {
        assertNull(Formatters.validateWageAmount(0L))
        assertNull(Formatters.validateWageAmount(1_000_000_000L))
        assertEquals(1_000_000_000L, Formatters.MAX_WAGE_AMOUNT)

        val aboveMaxError = Formatters.validateWageAmount(1_000_000_001L)
        assertTrue(aboveMaxError != null && aboveMaxError.contains("بیشتر از"))

        val negativeError = Formatters.validateWageAmount(-1L)
        assertTrue(negativeError != null && negativeError.contains("منفی"))

        // ورودی متنی خیلی بزرگ (> 64-bit) نباید به صفر تبدیل شود بلکه باید خطای سقف بدهد
        assertEquals(Long.MAX_VALUE, Formatters.parsePrice("999999999999999999999999"))
        assertEquals(Long.MIN_VALUE, Formatters.parsePrice("-999999999999999999999999"))
        assertTrue(Formatters.validateWageInput("1,000,000,001") != null)
        assertNull(Formatters.validateWageInput("1,000,000,000"))
    }

    @Test
    fun `boundary validation for hours accepts 0 to 24 and rejects negative or above 24 or NaN`() {
        assertNull(Formatters.validateHours(0.0))
        assertNull(Formatters.validateHours(24.0))
        assertNull(Formatters.validateHoursInput("24"))
        assertNull(Formatters.validateHoursInput("۲۳.۵"))

        assertTrue(Formatters.validateHours(24.001) != null)
        assertTrue(Formatters.validateHours(-0.1) != null)
        assertTrue(Formatters.validateHours(Double.NaN) != null)
        assertTrue(Formatters.validateHours(Double.POSITIVE_INFINITY) != null)
        assertTrue(Formatters.validateHoursInput("24.1") != null)
        assertTrue(Formatters.validateHoursInput("25") != null)
    }

    @Test
    fun `boundary validation for names accepts 1 to 60 characters and rejects blank or above 60`() {
        val exact60 = "الف".repeat(20) // 60 chars
        val over60 = exact60 + "ب" // 61 chars

        assertEquals(60, exact60.length)
        assertNull(Formatters.validateRequiredName(exact60))
        assertNull(Formatters.validateOptionalName(exact60))
        assertNull(Formatters.validateOptionalName(""))

        assertTrue(Formatters.validateRequiredName("") != null)
        assertTrue(Formatters.validateRequiredName("   ") != null)
        assertTrue(Formatters.validateRequiredName(over60) != null)
        assertTrue(Formatters.validateOptionalName(over60) != null)
    }

    @Test
    fun `iranian national ID check digit validation handles valid, invalid, repeated digits, and foreign worker bypass`() {
        // کدهای ملی معتبر واقعی از نظر الگوریتم رقم کنترل (هم با ارقام انگلیسی و هم فارسی)
        // 0013542419 -> sum = 0*10 + 0*9 + 1*8 + 3*7 + 5*6 + 4*5 + 2*4 + 4*3 + 1*2 = 8+21+30+20+8+12+2 = 101; 101 % 11 = 2; 11 - 2 = 9
        assertTrue(Formatters.isValidIranianNationalId("0013542419"))
        assertTrue(Formatters.isValidIranianNationalId("۰۰۱۳۵۴۲۴۱۹"))
        assertNull(Formatters.validateIranianNationalId("0013542419"))

        // حالتی که باقیمانده بزرگتر یا مساوی ۲ است:
        assertTrue(Formatters.isValidIranianNationalId("0499370899"))
        // 0010000003 -> 1*8 = 8; 8 % 11 = 8; 11 - 8 = 3
        assertTrue(Formatters.isValidIranianNationalId("0010000003"))
        // 0010000011 -> 1*8 + 1*2 = 10; 10 % 11 = 10; 11 - 10 = 1
        assertTrue(Formatters.isValidIranianNationalId("0010000011"))
        // حالتی که باقیمانده کمتر از ۲ است (rem = 0 و rem = 1):
        // 0010000100 -> 1*8 + 1*3 = 11; 11 % 11 = 0 -> check=0
        assertTrue(Formatters.isValidIranianNationalId("0010000100"))
        // 0010000021 -> 1*8 + 2*2 = 12; 12 % 11 = 1 -> check=1
        assertTrue(Formatters.isValidIranianNationalId("0010000021"))

        // رقم کنترل اشتباه
        assertFalse(Formatters.isValidIranianNationalId("0013542418"))
        assertTrue(Formatters.validateIranianNationalId("0013542418") != null)

        // طول نامعتبر
        assertFalse(Formatters.isValidIranianNationalId("123456789"))
        assertFalse(Formatters.isValidIranianNationalId("12345678901"))

        // ارقام تکراری مثل 1111111111 یا 0000000000 نامعتبرند حتی اگر رابطه ریاضی برقرار باشد
        for (d in '0'..'9') {
            val repeated = d.toString().repeat(10)
            assertFalse("Repeated digits $repeated must be rejected", Formatters.isValidIranianNationalId(repeated))
        }

        // بررسی رد کردن هشدار برای کارگران خارجی در validateWorker
        val foreignWorker = baseDailyWorker.copy(name = "احمد خان", nationalId = "1234567890")
        assertTrue(WageCalculator.validateWorker(foreignWorker, allowForeignNationalId = false) != null)
        assertNull(WageCalculator.validateWorker(foreignWorker, allowForeignNationalId = true))
    }

    @Test
    fun `validateWorker and validateAttendance enforce boundary constants`() {
        val validWorker = baseDailyWorker.copy(
            name = "کارگر معتبر",
            baseDailyWage = 1_000_000_000L,
            hourlyWageRate = 1_000_000_000L,
            overtimeRate = 1_000_000_000L
        )
        assertNull(WageCalculator.validateWorker(validWorker))

        val excessiveWageWorker = validWorker.copy(baseDailyWage = 1_000_000_001L)
        assertTrue(WageCalculator.validateWorker(excessiveWageWorker) != null)

        val excessiveHoursWorker = validWorker.copy(hourlyHours = 24.5)
        assertTrue(WageCalculator.validateWorker(excessiveHoursWorker) != null)

        val validAttendance = AttendanceEntity(
            id = 1L,
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 24.0,
            overtimeHours = 24.0,
            hourlyHours = 24.0,
            dailyWage = 1_000_000_000L
        )
        assertNull(WageCalculator.validateAttendance(validAttendance))

        val invalidAttendanceHours = validAttendance.copy(overtimeHours = 24.1)
        assertTrue(WageCalculator.validateAttendance(invalidAttendanceHours) != null)

        val invalidAttendanceWage = validAttendance.copy(dailyWage = 1_000_000_001L)
        assertTrue(WageCalculator.validateAttendance(invalidAttendanceWage) != null)
    }

    @Test(expected = ArithmeticException::class)
    fun `calculateDay throws ArithmeticException when hours multiplied by rate overflows Long`() {
        val worker = baseHourlyWorker.copy(
            isHourlyEnabled = true,
            hourlyWageRate = Long.MAX_VALUE / 2,
            hourlyHours = 3.0
        )
        val attendance = AttendanceEntity(
            id = 1L,
            workerId = 2L,
            date = "1405/01/01",
            status = AttendanceStatus.HOURLY,
            hourlyHours = 3.0,
            hourlyWageRate = Long.MAX_VALUE / 2
        )
        WageCalculator.calculateDay(worker, attendance)
    }

    @Test(expected = ArithmeticException::class)
    fun `calculateDay throws ArithmeticException when sum of wages and allowances overflows Long`() {
        val worker = baseDailyWorker.copy(
            baseDailyWage = Long.MAX_VALUE - 100L,
            isOvertimeEnabled = true,
            overtimeRate = 200L,
            overtimeHours = 1.0
        )
        val attendance = AttendanceEntity(
            id = 1L,
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            dailyWage = Long.MAX_VALUE - 100L,
            overtimeHours = 1.0,
            overtimeRate = 200L
        )
        WageCalculator.calculateDay(worker, attendance)
    }

    @Test(expected = ArithmeticException::class)
    fun `calculateWorkerPerformance throws ArithmeticException when multi-day totals overflow Long`() {
        val worker = baseDailyWorker.copy(baseDailyWage = Long.MAX_VALUE / 2 + 100L)
        val attendances = listOf(
            AttendanceEntity(
                id = 1L,
                workerId = 1L,
                date = "1405/01/01",
                status = AttendanceStatus.FULL_DAY,
                dailyWage = Long.MAX_VALUE / 2 + 100L
            ),
            AttendanceEntity(
                id = 2L,
                workerId = 1L,
                date = "1405/01/02",
                status = AttendanceStatus.FULL_DAY,
                dailyWage = Long.MAX_VALUE / 2 + 100L
            )
        )
        WageCalculator.calculateWorkerPerformance(
            worker = worker,
            attendances = attendances
        )
    }

    @Test(expected = ArithmeticException::class)
    fun `roundToLong throws ArithmeticException on values exceeding Long_MAX_VALUE`() {
        WageCalculator.roundToLong(1e20)
    }

    @Test
    fun `calculateFinancialSummary and calculateDailyBookkeeping skip record with Long_MAX_VALUE div 2 and return invalidAttendanceIds`() {
        val worker1 = baseDailyWorker.copy(id = 1L, name = "کارگر سالم اول", baseDailyWage = 1_200_000L)
        val worker2 = baseDailyWorker.copy(id = 2L, name = "کارگر سالم دوم", baseDailyWage = 800_000L)
        val workers = listOf(worker1, worker2)

        val healthyAtt1 = AttendanceEntity(
            id = 101L,
            workerId = 1L,
            date = "1405/01/10",
            status = AttendanceStatus.FULL_DAY,
            dailyWage = 1_200_000L,
            hasCustomAllowances = true
        )
        val corruptedAtt = AttendanceEntity(
            id = 999L,
            workerId = 1L,
            date = "1405/01/10",
            status = AttendanceStatus.FULL_DAY,
            dailyWage = Long.MAX_VALUE / 2,
            hasCustomAllowances = true
        )
        val healthyAtt2 = AttendanceEntity(
            id = 102L,
            workerId = 2L,
            date = "1405/01/10",
            status = AttendanceStatus.FULL_DAY,
            dailyWage = 800_000L,
            hasCustomAllowances = true
        )

        val attendances = listOf(healthyAtt1, corruptedAtt, healthyAtt2)

        val summary = WageCalculator.calculateFinancialSummary(
            workers = workers,
            attendances = attendances,
            expenses = emptyList(),
            todayStr = "1405/01/10"
        )

        assertEquals(listOf(999L), summary.invalidAttendanceIds)
        assertEquals(2_000_000L, summary.totalBaseWagesPaid)
        assertEquals(2_000_000L, summary.grandTotalProjectCost)
        assertEquals(2, summary.totalWorkDaysCount)

        val daily = WageCalculator.calculateDailyBookkeeping(
            dateStr = "1405/01/10",
            workers = workers,
            attendances = attendances,
            expenses = emptyList()
        )

        assertEquals(listOf(999L), daily.invalidAttendanceIds)
        assertEquals(2_000_000L, daily.totalDailyWages)
        assertEquals(2_000_000L, daily.grandDailyCost)
        assertEquals(2, daily.workersPresent)
    }
}


