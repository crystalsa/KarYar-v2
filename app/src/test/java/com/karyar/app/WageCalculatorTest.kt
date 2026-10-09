package com.karyar.app

import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.util.WageCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertEquals(0L, result.netPayout)
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
        assertEquals(0L, result.netPayout)
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
        assertEquals(0L, resAbsent.netPayout) // Daily display is clamped to 0
        assertEquals(200_000L, resPresent.netPayout)

        // Cumulative: gross (300k) - total deductions (100k + 100k = 200k) = 100,000L
        val perf = WageCalculator.calculateWorkerPerformance(worker, listOf(dayAbsent, dayPresent))
        assertEquals(300_000L, perf.baseWageTotal)
        assertEquals(200_000L, perf.totalDeductions)
        assertEquals(100_000L, perf.netPayout) // 100,000L, NOT 200,000L from summing daily nets!

        // If total deductions exceed total gross, worker cumulative net is capped at 0
        val workerHighDeductions = worker.copy(
            accommodationAllowance = 250_000L // 250k * 2 = 500k deductions vs 300k gross
        )
        val perfCapped = WageCalculator.calculateWorkerPerformance(workerHighDeductions, listOf(dayAbsent, dayPresent))
        assertEquals(0L, perfCapped.netPayout)
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
        val expectedGrandTotal = summary.workerPerformances.sumOf { it.netPayoutBeforeGroup }
        assertEquals(2_000_000L, expectedGrandTotal)
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
}
