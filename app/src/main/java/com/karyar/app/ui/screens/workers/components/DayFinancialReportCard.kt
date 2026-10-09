package com.karyar.app.ui.screens.workers.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.StatusBadge
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.ui.theme.Slate500
import com.karyar.app.util.Formatters
import com.karyar.app.util.WageCalculator

@Composable
fun DayFinancialReportCard(
    targetDayFolder: DateFolderEntity,
    allWorkers: List<WorkerEntity>,
    attendanceList: List<AttendanceEntity>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayWorkers = allWorkers
    var dayPresentCount = 0
    var dayHalfDayCount = 0
    var dayHourlyCount = 0
    var dayAbsentCount = 0

    var dayBaseWages = 0L
    var dayHourlyPay = 0L
    var dayHourlyHours = 0.0
    var dayOvertimePay = 0L
    var dayOvertimeHours = 0.0

    var transitAllowance = 0L
    var transitDeduction = 0L
    var foodAllowance = 0L
    var foodDeduction = 0L
    var accommodationAllowance = 0L
    var accommodationDeduction = 0L
    var medicalAllowance = 0L
    var medicalDeduction = 0L

    for (worker in dayWorkers) {
        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == targetDayFolder.date }
        val isHourly = WageCalculator.isHourly(worker, att)
        val isAbsent = WageCalculator.isAbsent(worker, att)
        val isHalfDay = WageCalculator.isHalfDay(worker, att)
        val isFullDay = WageCalculator.isFullDay(worker, att)

        when {
            isAbsent -> dayAbsentCount++
            isHourly -> {
                dayHourlyCount++
                val hHours = if (att != null && att.hourlyHours > 0) att.hourlyHours else (if (worker.hourlyHours > 0) worker.hourlyHours else 0.0)
                val hRate = if (att != null && att.hourlyWageRate > 0) att.hourlyWageRate else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage)
                if (hHours > 0 && hRate > 0) {
                    dayHourlyHours += hHours
                    dayHourlyPay += (hHours * hRate).toLong()
                }
            }
            isHalfDay -> {
                dayHalfDayCount++
                val wage = if (att?.dailyWage != null && att.dailyWage > 0) att.dailyWage else worker.baseDailyWage / 2
                dayBaseWages += wage
            }
            isFullDay -> {
                dayPresentCount++
                val wage = if (att?.dailyWage != null && att.dailyWage > 0) att.dailyWage else worker.baseDailyWage
                dayBaseWages += wage
            }
            else -> {}
        }

        if (!isAbsent) {
            val otHours = if (att != null && att.overtimeHours > 0) att.overtimeHours else (if (worker.isOvertimeEnabled) worker.overtimeHours else 0.0)
            val otRate = if (att != null && att.overtimeRate > 0) att.overtimeRate else worker.overtimeRate
            if (otHours > 0) {
                dayOvertimeHours += otHours
                val pay = if (otRate > 0) (otHours * otRate).toLong()
                else if (att != null && att.hourlyWage > 0) (otHours * att.hourlyWage * 1.4).toLong()
                else 0L
                dayOvertimePay += pay
            }

            // ایاب و ذهاب
            if (worker.transitAllowance > 0) {
                if (worker.transitImpact == "ALLOWANCE") {
                    transitAllowance += worker.transitAllowance
                } else {
                    transitDeduction += worker.transitAllowance
                }
            }

            // خوراک و ناهار
            if (worker.foodAllowance > 0) {
                if (worker.foodImpact == "ALLOWANCE") {
                    foodAllowance += worker.foodAllowance
                } else {
                    foodDeduction += worker.foodAllowance
                }
            }

            // اسکان و مسکن
            if (worker.accommodationAllowance > 0) {
                if (worker.accommodationImpact == "ALLOWANCE") {
                    accommodationAllowance += worker.accommodationAllowance
                } else {
                    accommodationDeduction += worker.accommodationAllowance
                }
            }

            // بیمه و درمان
            if (worker.medicalAllowance > 0) {
                if (worker.medicalImpact == "ALLOWANCE") {
                    medicalAllowance += worker.medicalAllowance
                } else {
                    medicalDeduction += worker.medicalAllowance
                }
            }
        }
    }

    val totalDayPayroll = dayWorkers.sumOf { worker ->
        val att = attendanceList.firstOrNull { a -> a.workerId == worker.id && a.date == targetDayFolder.date }
        WageCalculator.calculateDayPayout(worker, att)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, AmberAccent.copy(alpha = 0.5f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ۱. تیتر گزارش: متن گزارش مالی و پرداخت‌های امروز
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconicsBox(
                        icon = Icons.Default.ReceiptLong,
                        color = AmberAccent,
                        size = IconicsSize.SMALL
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "گزارش مالی و پرداخت‌ها",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ۲. زیرش: مجموع خالص پرداختی امروز با مبلغش به صورت پیش‌فرض + دکمه کوچک فلش سمت چپ کلمه تومان
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleExpanded() },
                shape = RoundedCornerShape(12.dp),
                color = EmeraldAccent.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "( مجموع خالص پرداختی )",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // دکمه کوچک فلش در سمت چپ کلمه تومان
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldAccent.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onToggleExpanded() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "بستن گزارش" else "مشاهده جزئیات گزارش",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = Formatters.formatCurrency(totalDayPayroll),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                    }
                }
            }

            // ۳. جزئیات فقط با زدن فلش و باز شدن کادر به صورت متحرک نمایش داده می‌شوند
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // مجموع دستمزد ناخالص
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "( مجموع دست مزد ناخالص )",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Formatters.formatCurrency(dayBaseWages),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // تفکیک اضافه کاری و ساعتی به همراه درج ساعت در جلوی مبلغ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // دستمزد ساعتی
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Slate100
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "دستمزد ساعتی:",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${Formatters.formatCurrency(dayHourlyPay)} (${Formatters.toPersianDigits(dayHourlyHours)} ساعت)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                        }

                        // اضافه کاری
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Slate100
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "اضافه‌کاری:",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${Formatters.formatCurrency(dayOvertimePay)} (${Formatters.toPersianDigits(dayOvertimeHours)} ساعت)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(thickness = 0.6.dp, color = Slate200)
                    Spacer(modifier = Modifier.height(8.dp))

                    // مزایا و کسورات با رنگ سبز (+) و قرمز (-) و فرمول مثبت/منفی
                    Text(
                        text = "مزایا و کسورات تفکیکی روز:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // ایاب و ذهاب
                    DayAllowanceDeductionItem(
                        title = "ایاب و ذهاب",
                        allowance = transitAllowance,
                        deduction = transitDeduction
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // خوراک و ناهار
                    DayAllowanceDeductionItem(
                        title = "خوراک و ناهار",
                        allowance = foodAllowance,
                        deduction = foodDeduction
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // اسکان و مسکن
                    DayAllowanceDeductionItem(
                        title = "اسکان و مسکن",
                        allowance = accommodationAllowance,
                        deduction = accommodationDeduction
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // بیمه و درمان
                    DayAllowanceDeductionItem(
                        title = "بیمه و درمان",
                        allowance = medicalAllowance,
                        deduction = medicalDeduction
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(thickness = 0.6.dp, color = Slate200)
                    Spacer(modifier = Modifier.height(8.dp))

                    // وضعیت پرسنل روز
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "وضعیت پرسنل روز:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusBadge(text = "${Formatters.toPersianDigits(dayPresentCount)} تمام روز", dotColor = EmeraldAccent)
                        if (dayHourlyCount > 0) {
                            StatusBadge(text = "${Formatters.toPersianDigits(dayHourlyCount)} ساعتی", dotColor = CyanAccent)
                        }
                        if (dayHalfDayCount > 0) {
                            StatusBadge(text = "${Formatters.toPersianDigits(dayHalfDayCount)} نصف روز", dotColor = AmberAccent)
                        }
                        if (dayAbsentCount > 0) {
                            StatusBadge(text = "${Formatters.toPersianDigits(dayAbsentCount)} غایب", dotColor = RoseAccent)
                        }
                    }
                }
            }
        }
    }
}
