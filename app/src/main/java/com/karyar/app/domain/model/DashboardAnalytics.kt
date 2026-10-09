package com.karyar.app.domain.model

data class DashboardAnalytics(
    val totalWorkersCount: Int = 0,
    val activeWorkersCount: Int = 0,
    val todayAttendanceCount: Int = 0,
    val totalWorkDaysCount: Int = 0,
    val totalPersonDays: Int = 0,
    val totalWorkHours: Double = 0.0,
    val totalOvertimeHours: Double = 0.0,
    // Costs
    val totalWagesPaid: Long = 0L,
    val totalHourlyPaid: Long = 0L,
    val totalOvertimePaid: Long = 0L,
    val totalTransitExpenses: Long = 0L,
    val totalAccommodationExpenses: Long = 0L,
    val totalAccommodationDays: Int = 0,
    val totalFoodExpenses: Long = 0L,
    val totalMedicalExpenses: Long = 0L,
    val totalOtherExpenses: Long = 0L,
    val totalIndividualExpenses: Long = 0L,
    val totalGroupExpenses: Long = 0L,
    val grandTotalExpenses: Long = 0L,
    val grandTotalProjectCost: Long = 0L
)
