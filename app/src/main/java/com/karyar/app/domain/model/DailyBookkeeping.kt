package com.karyar.app.domain.model

import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.util.JalaliCalendar

data class DailyBookkeeping(
    val date: String = JalaliCalendar.todayString(),
    val dayOfWeek: String = JalaliCalendar.todayDayOfWeek(),
    val grandDailyCost: Long = 0L,
    val totalDailyWages: Long = 0L,
    val totalHourlyPay: Long = 0L,
    val totalOvertimePay: Long = 0L,
    val dailyExpenses: Long = 0L,
    val workersPresent: Int = 0,
    val totalHours: Double = 0.0,
    val transitCost: Long = 0L,
    val accommodationCost: Long = 0L,
    val foodCost: Long = 0L,
    val medicalCost: Long = 0L,
    val attendances: List<AttendanceEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList()
) {
    val totalDailyCost: Long get() = grandDailyCost
    val totalWagesPaid: Long get() = totalDailyWages + totalHourlyPay
    val totalOvertimePaid: Long get() = totalOvertimePay
    val totalExpensesPaid: Long get() = dailyExpenses
    val workersPresentCount: Int get() = workersPresent
}
