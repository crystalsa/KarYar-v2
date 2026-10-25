package com.karyar.app

import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.util.DayCalculationResult
import com.karyar.app.util.WageCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * تست‌های طلایی (Golden Tests) برای قفل کردن رفتار دقیق `WageCalculator.calculateDay`
 * پیش از بازآرایی (Refactoring) به توابع کوچک‌تر (`resolveBaseWage`، `resolveHourlyPay`،
 * `resolveOvertimePay`، `resolveAllowances`) و یکپارچه‌سازی بلوک‌های مزایا/کسورات.
 *
 * تمام ترکیب‌های:
 * - وضعیت حضور (`FULL_DAY`, `HALF_DAY`, `HOURLY`, `ABSENT`, `att == null`)
 * - فلگ `hasCustomAllowances` (`usesRecordValues`: `true` / `false`)
 * - نوع کارگر (روزمزد `isHourlyEnabled = false` / ساعتی `isHourlyEnabled = true`)
 * - اضافه‌کاری (با نرخ رکورد، با نرخ پروفایل کارگر، بدون نرخ/محاسبه خودکار ۱٫۴ برابر، و نرخ صفر در حالت سفارشی)
 * - مزایا و کسورات (`ALLOWANCE` و `DEDUCTION` در هر ۴ دسته ایاب‌وذهاب، خوراک، مسکن، درمان)
 * در این تست‌ها ثبت و قفل شده‌اند تا هیچ نتیجه مالی پس از بازآرایی تغییر نکند.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WageCalculatorGoldenTest {

    private val dailyWorkerWithTemplate = WorkerEntity(
        id = 101L,
        folderId = 1L,
        name = "کارگر روزمزد نمونه",
        role = "استادکار",
        baseDailyWage = 1_200_000L,
        baseHourlyWage = 0L,
        isHourlyEnabled = false,
        hourlyWageRate = 0L,
        hourlyHours = 0.0,
        overtimeRate = 250_000L,
        overtimeHours = 2.0,
        transitAllowance = 50_000L,
        transitImpact = "ALLOWANCE",
        foodAllowance = 80_000L,
        foodImpact = "ALLOWANCE",
        accommodationAllowance = 120_000L,
        accommodationImpact = "DEDUCTION",
        medicalAllowance = 30_000L,
        medicalImpact = "DEDUCTION"
    )

    private val hourlyWorkerWithTemplate = WorkerEntity(
        id = 102L,
        folderId = 1L,
        name = "کارگر ساعتی نمونه",
        role = "جوشکار",
        baseDailyWage = 1_000_000L,
        baseHourlyWage = 180_000L,
        isHourlyEnabled = true,
        hourlyWageRate = 200_000L,
        hourlyHours = 6.0,
        overtimeRate = 0L, // بدون نرخ ثابت اضافه‌کاری تا مسیر fallback بررسی شود
        overtimeHours = 1.5,
        transitAllowance = 40_000L,
        transitImpact = "ALLOWANCE",
        foodAllowance = 60_000L,
        foodImpact = "DEDUCTION",
        accommodationAllowance = 90_000L,
        accommodationImpact = "ALLOWANCE",
        medicalAllowance = 20_000L,
        medicalImpact = "DEDUCTION"
    )

    private fun baseAtt(
        workerId: Long = 101L,
        status: AttendanceStatus = AttendanceStatus.FULL_DAY,
        hasCustomAllowances: Boolean = false
    ) = AttendanceEntity(
        id = 500L,
        folderId = 1L,
        workerId = workerId,
        date = "1405/02/10",
        status = status,
        regularHours = 0.0,
        hourlyHours = 0.0,
        hourlyWageRate = 0L,
        hourlyWage = 0L,
        dailyWage = 0L,
        overtimeHours = 0.0,
        overtimeRate = 0L,
        transitAllowance = 70_000L,
        transitImpact = "DEDUCTION",
        foodAllowance = 110_000L,
        foodImpact = "ALLOWANCE",
        accommodationAllowance = 150_000L,
        accommodationImpact = "ALLOWANCE",
        medicalAllowance = 45_000L,
        medicalImpact = "DEDUCTION",
        hasCustomAllowances = hasCustomAllowances
    )

    // =========================================================================
    // 1. FULL_DAY Combinations
    // =========================================================================

    @Test
    fun golden_fullDay_customFalse_dailyWorker_fallbackToWorkerProfile() {
        val att = baseAtt(status = AttendanceStatus.FULL_DAY, hasCustomAllowances = false).copy(
            overtimeHours = 2.0
        )
        val res = WageCalculator.calculateDay(dailyWorkerWithTemplate, att)
        // baseWage = 1,200,000 (worker), hourlyPay = 0, otPay = 2.0 * 250,000 (worker) = 500,000
        // allowances from worker: transit(50k) + food(80k) = 130,000
        // deductions from worker: accom(120k) + med(30k) = 150,000
        // gross = 1,200,000 + 500,000 + 130,000 = 1,830,000; net = 1,830,000 - 150,000 = 1,680,000
        assertEquals(
            DayCalculationResult(
                isWorkingDay = true,
                regularHours = 8.0,
                hourlyHours = 0.0,
                overtimeHours = 2.0,
                baseWage = 1_200_000L,
                hourlyPay = 0L,
                overtimePay = 500_000L,
                totalAllowances = 130_000L,
                totalDeductions = 150_000L,
                netPayout = 1_680_000L,
                clampedNetPayout = 1_680_000L,
                transitAllowance = 50_000L,
                foodAllowance = 80_000L,
                accommodationAllowance = 0L,
                medicalAllowance = 0L,
                transitDeduction = 0L,
                foodDeduction = 0L,
                accommodationDeduction = 120_000L,
                medicalDeduction = 30_000L
            ),
            res
        )
    }

    @Test
    fun golden_fullDay_customTrue_usesRecordValuesAndExplicitZeros() {
        // When hasCustomAllowances = true, dailyWage = 0L and overtimeRate = 0L are respected as explicit 0L!
        val att = baseAtt(status = AttendanceStatus.FULL_DAY, hasCustomAllowances = true).copy(
            dailyWage = 0L,
            overtimeHours = 3.0,
            overtimeRate = 0L,
            regularHours = 9.5
        )
        val res = WageCalculator.calculateDay(dailyWorkerWithTemplate, att)
        // baseWage = 0L (explicit record value), otPay = 0L (explicit 0 rate on record)
        // allowances from att: food(110k) + accom(150k) = 260,000
        // deductions from att: transit(70k) + med(45k) = 115,000
        // net = 260,000 - 115,000 = 145,000
        assertEquals(
            DayCalculationResult(
                isWorkingDay = true,
                regularHours = 9.5,
                hourlyHours = 0.0,
                overtimeHours = 3.0,
                baseWage = 0L,
                hourlyPay = 0L,
                overtimePay = 0L,
                totalAllowances = 260_000L,
                totalDeductions = 115_000L,
                netPayout = 145_000L,
                clampedNetPayout = 145_000L,
                transitAllowance = 0L,
                foodAllowance = 110_000L,
                accommodationAllowance = 150_000L,
                medicalAllowance = 0L,
                transitDeduction = 70_000L,
                foodDeduction = 0L,
                accommodationDeduction = 0L,
                medicalDeduction = 45_000L
            ),
            res
        )
    }

    @Test
    fun golden_fullDay_customTrue_withCustomHourlyAndOvertimeAndDailyWage() {
        // Edge case #1 locked: FULL_DAY with hasCustomAllowances = true and hourlyHours > 0
        // adds hourlyPay to gross, while DayCalculationResult.hourlyHours remains 0.0.
        val att = baseAtt(status = AttendanceStatus.FULL_DAY, hasCustomAllowances = true).copy(
            dailyWage = 1_500_000L,
            hourlyHours = 2.5,
            hourlyWageRate = 200_000L,
            overtimeHours = 1.5,
            overtimeRate = 300_000L
        )
        val res = WageCalculator.calculateDay(dailyWorkerWithTemplate, att)
        // baseWage = 1,500,000; hourlyPay = 2.5 * 200,000 = 500,000; otPay = 1.5 * 300,000 = 450,000
        // allowances = 260,000; deductions = 115,000
        // gross = 1,500,000 + 500,000 + 450,000 + 260,000 = 2,710,000; net = 2,595,000
        assertEquals(1_500_000L, res.baseWage)
        assertEquals(500_000L, res.hourlyPay)
        assertEquals(450_000L, res.overtimePay)
        assertEquals(0.0, res.hourlyHours, 0.0001) // Locked current behavior
        assertEquals(2_595_000L, res.netPayout)
    }

    // =========================================================================
    // 2. HALF_DAY Combinations
    // =========================================================================

    @Test
    fun golden_halfDay_customFalse_halvesWorkerBaseDailyWageAndAutoOvertimeFromDailyWage() {
        val workerNoOtRate = dailyWorkerWithTemplate.copy(
            baseDailyWage = 1_000_001L, // odd number to lock half-up rounding: 1,000,001 / 2.0 = 500,000.5 -> 500,001
            overtimeRate = 0L
        )
        val att = baseAtt(status = AttendanceStatus.HALF_DAY, hasCustomAllowances = false).copy(
            overtimeHours = 2.0
        )
        val res = WageCalculator.calculateDay(workerNoOtRate, att)
        // baseWage = roundToLong(1,000,001 / 2.0) = 500,001
        // otRate = roundToLong((1,000,001 / 8.0) * 1.4) = roundToLong(125000.125 * 1.4) = roundToLong(175000.175) = 175,000
        // otPay = 2.0 * 175,000 = 350,000
        assertEquals(4.0, res.regularHours, 0.0001)
        assertEquals(500_001L, res.baseWage)
        assertEquals(350_000L, res.overtimePay)
        assertEquals(130_000L, res.totalAllowances)
        assertEquals(150_000L, res.totalDeductions)
        assertEquals(830_001L, res.netPayout)
    }

    @Test
    fun golden_halfDay_customTrue_usesExactAttDailyWageAndIgnoresHourlyHours() {
        // Edge case #3 locked: HALF_DAY with hasCustomAllowances = true uses att.dailyWage directly
        // and does not include hourlyPay even if hourlyHours > 0.
        val att = baseAtt(status = AttendanceStatus.HALF_DAY, hasCustomAllowances = true).copy(
            dailyWage = 650_000L,
            hourlyHours = 3.0,
            hourlyWageRate = 200_000L,
            overtimeHours = 1.0,
            overtimeRate = 280_000L
        )
        val res = WageCalculator.calculateDay(dailyWorkerWithTemplate, att)
        assertEquals(4.0, res.regularHours, 0.0001)
        assertEquals(650_000L, res.baseWage)
        assertEquals(0L, res.hourlyPay)
        assertEquals(280_000L, res.overtimePay)
        assertEquals(260_000L, res.totalAllowances)
        assertEquals(115_000L, res.totalDeductions)
        assertEquals(1_075_000L, res.netPayout)
    }

    // =========================================================================
    // 3. HOURLY Combinations
    // =========================================================================

    @Test
    fun golden_hourly_customFalse_hourlyWorker_usesWorkerHourlyRateAndAutoOvertimeFromHRate() {
        val att = baseAtt(workerId = 102L, status = AttendanceStatus.HOURLY, hasCustomAllowances = false).copy(
            hourlyHours = 5.0,
            overtimeHours = 2.0
        )
        val res = WageCalculator.calculateDay(hourlyWorkerWithTemplate, att)
        // hRate = worker.hourlyWageRate = 200,000; hourlyPay = 5 * 200,000 = 1,000,000
        // otRate = roundToLong(200,000 * 1.4) = 280,000; otPay = 2 * 280,000 = 560,000
        // allowances from hourlyWorkerWithTemplate: transit(40k) + accom(90k) = 130,000
        // deductions from hourlyWorkerWithTemplate: food(60k) + med(20k) = 80,000
        // net = 1,000,000 + 560,000 + 130,000 - 80,000 = 1,610,000
        assertEquals(0.0, res.regularHours, 0.0001)
        assertEquals(5.0, res.hourlyHours, 0.0001)
        assertEquals(2.0, res.overtimeHours, 0.0001)
        assertEquals(0L, res.baseWage)
        assertEquals(1_000_000L, res.hourlyPay)
        assertEquals(560_000L, res.overtimePay)
        assertEquals(130_000L, res.totalAllowances)
        assertEquals(80_000L, res.totalDeductions)
        assertEquals(1_610_000L, res.netPayout)
    }

    @Test
    fun golden_hourly_customFalse_zeroHourlyHoursDisablesOvertimePay() {
        // Edge case #2 locked: When status == HOURLY and hourlyHours == 0.0, canHaveOvertime is false,
        // so overtimePay is 0L while overtimeHours is still reported in DayCalculationResult.
        val att = baseAtt(workerId = 102L, status = AttendanceStatus.HOURLY, hasCustomAllowances = false).copy(
            hourlyHours = 0.0,
            overtimeHours = 3.0,
            overtimeRate = 300_000L
        )
        val res = WageCalculator.calculateDay(hourlyWorkerWithTemplate, att)
        assertEquals(0L, res.hourlyPay)
        assertEquals(0L, res.overtimePay)
        assertEquals(3.0, res.overtimeHours, 0.0001)
        assertEquals(50_000L, res.netPayout) // 130,000 allowances - 80,000 deductions
    }

    @Test
    fun golden_hourly_customTrue_fallbackToAttHourlyWageFieldAndPositiveDailyWage() {
        // Edge case #3 & #4 locked:
        // - When hasCustomAllowances = true and att.hourlyWageRate == 0L, hRate falls back to att.hourlyWage.
        // - When status == HOURLY and hasCustomAllowances = true with att.dailyWage > 0L, baseWage = att.dailyWage.
        val att = baseAtt(workerId = 102L, status = AttendanceStatus.HOURLY, hasCustomAllowances = true).copy(
            dailyWage = 300_000L,
            hourlyHours = 4.0,
            hourlyWageRate = 0L,
            hourlyWage = 175_000L,
            overtimeHours = 1.0,
            overtimeRate = 240_000L
        )
        val res = WageCalculator.calculateDay(hourlyWorkerWithTemplate, att)
        // baseWage = 300,000; hourlyPay = 4 * 175,000 = 700,000; otPay = 240,000
        // allowances from att = 260,000; deductions from att = 115,000
        // net = 300,000 + 700,000 + 240,000 + 260,000 - 115,000 = 1,385,000
        assertEquals(300_000L, res.baseWage)
        assertEquals(700_000L, res.hourlyPay)
        assertEquals(240_000L, res.overtimePay)
        assertEquals(1_385_000L, res.netPayout)
    }

    // =========================================================================
    // 4. ABSENT Combinations (Unified Allowances/Deductions Check)
    // =========================================================================

    @Test
    fun golden_absent_customFalse_zeroWagesAndAllowances_appliesWorkerDeductions() {
        val att = baseAtt(status = AttendanceStatus.ABSENT, hasCustomAllowances = false).copy(
            dailyWage = 1_200_000L,
            hourlyHours = 5.0,
            overtimeHours = 2.0
        )
        val res = WageCalculator.calculateDay(dailyWorkerWithTemplate, att)
        assertFalse(res.isWorkingDay)
        assertEquals(0.0, res.regularHours, 0.0001)
        assertEquals(0.0, res.hourlyHours, 0.0001)
        assertEquals(0.0, res.overtimeHours, 0.0001)
        assertEquals(0L, res.baseWage)
        assertEquals(0L, res.hourlyPay)
        assertEquals(0L, res.overtimePay)
        assertEquals(0L, res.totalAllowances)
        assertEquals(0L, res.transitAllowance)
        assertEquals(0L, res.foodAllowance)
        assertEquals(0L, res.accommodationAllowance)
        assertEquals(0L, res.medicalAllowance)
        // Deductions from dailyWorkerWithTemplate: accom(120k) + med(30k) = 150,000
        assertEquals(0L, res.transitDeduction)
        assertEquals(0L, res.foodDeduction)
        assertEquals(120_000L, res.accommodationDeduction)
        assertEquals(30_000L, res.medicalDeduction)
        assertEquals(150_000L, res.totalDeductions)
        assertEquals(-150_000L, res.netPayout)
        assertEquals(0L, res.clampedNetPayout)
    }

    @Test
    fun golden_absent_customTrue_zeroWagesAndAllowances_appliesRecordDeductions() {
        val att = baseAtt(status = AttendanceStatus.ABSENT, hasCustomAllowances = true)
        val res = WageCalculator.calculateDay(dailyWorkerWithTemplate, att)
        assertFalse(res.isWorkingDay)
        assertEquals(0L, res.totalAllowances)
        assertEquals(0L, res.foodAllowance)
        assertEquals(0L, res.accommodationAllowance)
        // Deductions from att: transit(70k) + med(45k) = 115,000
        assertEquals(70_000L, res.transitDeduction)
        assertEquals(0L, res.foodDeduction)
        assertEquals(0L, res.accommodationDeduction)
        assertEquals(45_000L, res.medicalDeduction)
        assertEquals(115_000L, res.totalDeductions)
        assertEquals(-115_000L, res.netPayout)
        assertEquals(0L, res.clampedNetPayout)
    }

    // =========================================================================
    // 5. Null Attendance (att == null) Combinations
    // =========================================================================

    @Test
    fun golden_nullAttendance_dailyWorker_usesWorkerBaseDailyWageAndAllowances() {
        val res = WageCalculator.calculateDay(dailyWorkerWithTemplate, null)
        assertTrue(res.isWorkingDay)
        assertEquals(8.0, res.regularHours, 0.0001)
        assertEquals(0.0, res.hourlyHours, 0.0001)
        assertEquals(0.0, res.overtimeHours, 0.0001)
        assertEquals(1_200_000L, res.baseWage)
        assertEquals(0L, res.hourlyPay)
        assertEquals(0L, res.overtimePay)
        assertEquals(130_000L, res.totalAllowances)
        assertEquals(150_000L, res.totalDeductions)
        assertEquals(1_180_000L, res.netPayout)
    }

    @Test
    fun golden_nullAttendance_hourlyWorker_zeroBaseAndHourlyPay_appliesAllowancesAndDeductions() {
        // Edge case #5 locked: att == null for an hourly worker has hourly = true, hHours = 0.0,
        // baseWage = 0L, hourlyPay = 0L, and applies worker allowances/deductions.
        val res = WageCalculator.calculateDay(hourlyWorkerWithTemplate, null)
        assertTrue(res.isWorkingDay)
        assertEquals(0.0, res.regularHours, 0.0001)
        assertEquals(0.0, res.hourlyHours, 0.0001)
        assertEquals(0L, res.baseWage)
        assertEquals(0L, res.hourlyPay)
        assertEquals(0L, res.overtimePay)
        assertEquals(130_000L, res.totalAllowances)
        assertEquals(80_000L, res.totalDeductions)
        assertEquals(50_000L, res.netPayout)
    }

    // =========================================================================
    // 6. Comprehensive Matrix Golden Check (32 combinations)
    // =========================================================================

    @Test
    fun golden_matrixAllCombinations_matchesLockedSnapshots() {
        val statuses = listOf(
            AttendanceStatus.FULL_DAY,
            AttendanceStatus.HALF_DAY,
            AttendanceStatus.HOURLY,
            AttendanceStatus.ABSENT
        )
        val customFlags = listOf(false, true)
        val workers = listOf(dailyWorkerWithTemplate, hourlyWorkerWithTemplate)
        val overtimeModes = listOf(false, true) // false = 0L rate on att, true = explicit 320_000L rate on att

        val actualResults = mutableListOf<String>()
        for (worker in workers) {
            for (status in statuses) {
                for (custom in customFlags) {
                    for (explicitOtRate in overtimeModes) {
                        val att = baseAtt(
                            workerId = worker.id,
                            status = status,
                            hasCustomAllowances = custom
                        ).copy(
                            dailyWage = if (custom) 900_000L else 0L,
                            hourlyHours = 4.0,
                            hourlyWageRate = if (custom) 160_000L else 0L,
                            overtimeHours = 2.0,
                            overtimeRate = if (explicitOtRate) 320_000L else 0L
                        )
                        val r = WageCalculator.calculateDay(worker, att)
                        actualResults.add(
                            "w=${worker.id},s=$status,c=$custom,ot=$explicitOtRate -> " +
                                "work=${r.isWorkingDay},reg=${r.regularHours},hH=${r.hourlyHours},otH=${r.overtimeHours}," +
                                "base=${r.baseWage},hPay=${r.hourlyPay},otPay=${r.overtimePay}," +
                                "allow=${r.totalAllowances},ded=${r.totalDeductions},net=${r.netPayout},clamp=${r.clampedNetPayout}"
                        )
                    }
                }
            }
        }

        val expectedSnapshots = listOf(
            // dailyWorker (101): baseDaily=1,200,000, otRate=250,000, workerAllow=130,000, workerDed=150,000
            // att custom values: daily=900,000, hHours=4.0, hRate=160,000, otHours=2.0, attAllow=260,000, attDed=115,000
            "w=101,s=FULL_DAY,c=false,ot=false -> work=true,reg=8.0,hH=0.0,otH=2.0,base=1200000,hPay=0,otPay=500000,allow=130000,ded=150000,net=1680000,clamp=1680000",
            "w=101,s=FULL_DAY,c=false,ot=true -> work=true,reg=8.0,hH=0.0,otH=2.0,base=1200000,hPay=0,otPay=640000,allow=130000,ded=150000,net=1820000,clamp=1820000",
            "w=101,s=FULL_DAY,c=true,ot=false -> work=true,reg=8.0,hH=0.0,otH=2.0,base=900000,hPay=640000,otPay=0,allow=260000,ded=115000,net=1685000,clamp=1685000",
            "w=101,s=FULL_DAY,c=true,ot=true -> work=true,reg=8.0,hH=0.0,otH=2.0,base=900000,hPay=640000,otPay=640000,allow=260000,ded=115000,net=2325000,clamp=2325000",
            "w=101,s=HALF_DAY,c=false,ot=false -> work=true,reg=4.0,hH=0.0,otH=2.0,base=600000,hPay=0,otPay=500000,allow=130000,ded=150000,net=1080000,clamp=1080000",
            "w=101,s=HALF_DAY,c=false,ot=true -> work=true,reg=4.0,hH=0.0,otH=2.0,base=600000,hPay=0,otPay=640000,allow=130000,ded=150000,net=1220000,clamp=1220000",
            "w=101,s=HALF_DAY,c=true,ot=false -> work=true,reg=4.0,hH=0.0,otH=2.0,base=900000,hPay=0,otPay=0,allow=260000,ded=115000,net=1045000,clamp=1045000",
            "w=101,s=HALF_DAY,c=true,ot=true -> work=true,reg=4.0,hH=0.0,otH=2.0,base=900000,hPay=0,otPay=640000,allow=260000,ded=115000,net=1685000,clamp=1685000",
            "w=101,s=HOURLY,c=false,ot=false -> work=true,reg=0.0,hH=4.0,otH=2.0,base=0,hPay=0,otPay=500000,allow=130000,ded=150000,net=480000,clamp=480000",
            "w=101,s=HOURLY,c=false,ot=true -> work=true,reg=0.0,hH=4.0,otH=2.0,base=0,hPay=0,otPay=640000,allow=130000,ded=150000,net=620000,clamp=620000",
            "w=101,s=HOURLY,c=true,ot=false -> work=true,reg=0.0,hH=4.0,otH=2.0,base=900000,hPay=640000,otPay=0,allow=260000,ded=115000,net=1685000,clamp=1685000",
            "w=101,s=HOURLY,c=true,ot=true -> work=true,reg=0.0,hH=4.0,otH=2.0,base=900000,hPay=640000,otPay=640000,allow=260000,ded=115000,net=2325000,clamp=2325000",
            "w=101,s=ABSENT,c=false,ot=false -> work=false,reg=0.0,hH=0.0,otH=0.0,base=0,hPay=0,otPay=0,allow=0,ded=150000,net=-150000,clamp=0",
            "w=101,s=ABSENT,c=false,ot=true -> work=false,reg=0.0,hH=0.0,otH=0.0,base=0,hPay=0,otPay=0,allow=0,ded=150000,net=-150000,clamp=0",
            "w=101,s=ABSENT,c=true,ot=false -> work=false,reg=0.0,hH=0.0,otH=0.0,base=0,hPay=0,otPay=0,allow=0,ded=115000,net=-115000,clamp=0",
            "w=101,s=ABSENT,c=true,ot=true -> work=false,reg=0.0,hH=0.0,otH=0.0,base=0,hPay=0,otPay=0,allow=0,ded=115000,net=-115000,clamp=0",
            // hourlyWorker (102): baseDaily=1,000,000, hRate=200,000, otRate=0 (auto 1.4*200k=280k), workerAllow=130,000, workerDed=80,000
            "w=102,s=FULL_DAY,c=false,ot=false -> work=true,reg=8.0,hH=0.0,otH=2.0,base=1000000,hPay=0,otPay=560000,allow=130000,ded=80000,net=1610000,clamp=1610000",
            "w=102,s=FULL_DAY,c=false,ot=true -> work=true,reg=8.0,hH=0.0,otH=2.0,base=1000000,hPay=0,otPay=640000,allow=130000,ded=80000,net=1690000,clamp=1690000",
            "w=102,s=FULL_DAY,c=true,ot=false -> work=true,reg=8.0,hH=0.0,otH=2.0,base=900000,hPay=640000,otPay=0,allow=260000,ded=115000,net=1685000,clamp=1685000",
            "w=102,s=FULL_DAY,c=true,ot=true -> work=true,reg=8.0,hH=0.0,otH=2.0,base=900000,hPay=640000,otPay=640000,allow=260000,ded=115000,net=2325000,clamp=2325000",
            "w=102,s=HALF_DAY,c=false,ot=false -> work=true,reg=4.0,hH=0.0,otH=2.0,base=500000,hPay=0,otPay=560000,allow=130000,ded=80000,net=1110000,clamp=1110000",
            "w=102,s=HALF_DAY,c=false,ot=true -> work=true,reg=4.0,hH=0.0,otH=2.0,base=500000,hPay=0,otPay=640000,allow=130000,ded=80000,net=1190000,clamp=1190000",
            "w=102,s=HALF_DAY,c=true,ot=false -> work=true,reg=4.0,hH=0.0,otH=2.0,base=900000,hPay=0,otPay=0,allow=260000,ded=115000,net=1045000,clamp=1045000",
            "w=102,s=HALF_DAY,c=true,ot=true -> work=true,reg=4.0,hH=0.0,otH=2.0,base=900000,hPay=0,otPay=640000,allow=260000,ded=115000,net=1685000,clamp=1685000",
            "w=102,s=HOURLY,c=false,ot=false -> work=true,reg=0.0,hH=4.0,otH=2.0,base=0,hPay=800000,otPay=560000,allow=130000,ded=80000,net=1410000,clamp=1410000",
            "w=102,s=HOURLY,c=false,ot=true -> work=true,reg=0.0,hH=4.0,otH=2.0,base=0,hPay=800000,otPay=640000,allow=130000,ded=80000,net=1490000,clamp=1490000",
            "w=102,s=HOURLY,c=true,ot=false -> work=true,reg=0.0,hH=4.0,otH=2.0,base=900000,hPay=640000,otPay=0,allow=260000,ded=115000,net=1685000,clamp=1685000",
            "w=102,s=HOURLY,c=true,ot=true -> work=true,reg=0.0,hH=4.0,otH=2.0,base=900000,hPay=640000,otPay=640000,allow=260000,ded=115000,net=2325000,clamp=2325000",
            "w=102,s=ABSENT,c=false,ot=false -> work=false,reg=0.0,hH=0.0,otH=0.0,base=0,hPay=0,otPay=0,allow=0,ded=80000,net=-80000,clamp=0",
            "w=102,s=ABSENT,c=false,ot=true -> work=false,reg=0.0,hH=0.0,otH=0.0,base=0,hPay=0,otPay=0,allow=0,ded=80000,net=-80000,clamp=0",
            "w=102,s=ABSENT,c=true,ot=false -> work=false,reg=0.0,hH=0.0,otH=0.0,base=0,hPay=0,otPay=0,allow=0,ded=115000,net=-115000,clamp=0",
            "w=102,s=ABSENT,c=true,ot=true -> work=false,reg=0.0,hH=0.0,otH=0.0,base=0,hPay=0,otPay=0,allow=0,ded=115000,net=-115000,clamp=0"
        )

        assertEquals(expectedSnapshots.size, actualResults.size)
        for (i in expectedSnapshots.indices) {
            assertEquals("Mismatch at index $i", expectedSnapshots[i], actualResults[i])
        }
    }

    // =========================================================================
    // 7. Extracted Helper Functions & usesRecordValues Verification
    // =========================================================================

    @Test
    fun golden_extractedHelpersAndUsesRecordValues_matchCalculateDayComponents() {
        val attFalse = baseAtt(status = AttendanceStatus.FULL_DAY, hasCustomAllowances = false).copy(
            overtimeHours = 2.0
        )
        val attTrue = baseAtt(status = AttendanceStatus.FULL_DAY, hasCustomAllowances = true).copy(
            dailyWage = 950_000L,
            hourlyHours = 2.0,
            hourlyWageRate = 150_000L,
            overtimeHours = 1.5,
            overtimeRate = 200_000L
        )
        assertFalse(attFalse.usesRecordValues)
        assertTrue(attTrue.usesRecordValues)

        assertEquals(1_200_000L, WageCalculator.resolveBaseWage(dailyWorkerWithTemplate, attFalse))
        assertEquals(950_000L, WageCalculator.resolveBaseWage(dailyWorkerWithTemplate, attTrue))

        assertEquals(0L, WageCalculator.resolveHourlyPay(dailyWorkerWithTemplate, attFalse))
        assertEquals(300_000L, WageCalculator.resolveHourlyPay(dailyWorkerWithTemplate, attTrue))

        assertEquals(500_000L, WageCalculator.resolveOvertimePay(dailyWorkerWithTemplate, attFalse))
        assertEquals(300_000L, WageCalculator.resolveOvertimePay(dailyWorkerWithTemplate, attTrue))

        val allowWorking = WageCalculator.resolveAllowances(dailyWorkerWithTemplate, attTrue, isAbsent = false)
        assertEquals(260_000L, allowWorking.totalAllowances)
        assertEquals(115_000L, allowWorking.totalDeductions)

        val allowAbsent = WageCalculator.resolveAllowances(dailyWorkerWithTemplate, attTrue, isAbsent = true)
        assertEquals(0L, allowAbsent.totalAllowances)
        assertEquals(115_000L, allowAbsent.totalDeductions)
    }
}

