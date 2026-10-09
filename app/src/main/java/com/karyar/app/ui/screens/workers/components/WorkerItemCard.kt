package com.karyar.app.ui.screens.workers.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.domain.model.WorkerPerformance
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.StatusBadge
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.util.Formatters
import com.karyar.app.util.WageCalculator

@Composable
fun WorkerItemCard(
    worker: WorkerEntity,
    attendance: AttendanceEntity? = null,
    performance: WorkerPerformance?,
    targetDate: String? = null,
    targetDayOfWeek: String? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isHourly = WageCalculator.isHourly(worker, attendance)
    val isAbsent = WageCalculator.isAbsent(worker, attendance)
    val isHalfDay = WageCalculator.isHalfDay(worker, attendance)
    val isFullDay = WageCalculator.isFullDay(worker, attendance)
    var showPhone by remember(worker.id) { mutableStateOf(false) }

    HairlineCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("worker_card_${worker.id}"),
        backgroundColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPhone = !showPhone }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(worker.colorTag)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = worker.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = worker.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val roleDisplay = if (worker.nationalId.length >= 4) {
                            "${worker.role} • کد: ${Formatters.toPersianDigits(worker.nationalId.takeLast(4))}"
                        } else {
                            worker.role
                        }
                        Text(
                            text = roleDisplay,
                            fontSize = 11.sp,
                            color = AmberAccent,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val statusBadgeInfo: Pair<String, Color> = when {
                        isAbsent -> Pair("غایب", RoseAccent)
                        isHourly -> {
                            val hHours = if (attendance != null && attendance.hourlyHours > 0) attendance.hourlyHours
                            else (if (worker.hourlyHours > 0) worker.hourlyHours else 0.0)
                            if (hHours > 0) {
                                Pair("${Formatters.toPersianDigits(hHours.toString().removeSuffix(".0"))} ساعت کار", CyanAccent)
                            } else {
                                Pair("ساعتی", CyanAccent)
                            }
                        }
                        isHalfDay -> Pair("نصف روز", AmberAccent)
                        isFullDay -> Pair("تمام روز", EmeraldAccent)
                        else -> Pair("تمام روز", EmeraldAccent)
                    }

                    StatusBadge(
                        text = statusBadgeInfo.first,
                        dotColor = statusBadgeInfo.second
                    )

                    // نمایش شماره تماس زیر نشانگر وضعیت کار هنگام کلیک روی نام کارگر
                    if (showPhone && !worker.phone.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val context = LocalContext.current
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${worker.phone}"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "شماره تماس",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = Formatters.toPersianDigits(worker.phone),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDirection = TextDirection.Ltr
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Wage & Allowances breakdown - مستقیماً مرتبط با ورود و خروج
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val dayCalc = WageCalculator.calculateDay(worker, attendance)
                    val hRate = if (attendance != null && attendance.hourlyWageRate > 0) attendance.hourlyWageRate
                    else if (attendance != null && attendance.hourlyWage > 0) attendance.hourlyWage
                    else if (worker.hourlyWageRate > 0) worker.hourlyWageRate
                    else worker.baseHourlyWage

                    val otRate = if (attendance != null && attendance.overtimeRate > 0) attendance.overtimeRate
                    else worker.overtimeRate

                    val netDayPayout = dayCalc.netPayout

                    // ۱. مبلغ پرداختی این روز در بالای دستمزدها با قرارگیری عدد درست جلوش بدون فاصله
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مبلغ پرداختی این روز: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAbsent) RoseAccent else EmeraldAccent
                        )
                        Text(
                            text = if (isAbsent) "۰ تومان (غیبت)" else Formatters.formatCurrency(netDayPayout),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAbsent) RoseAccent else EmeraldAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ۲. ریز دستمزدها و اضافه کار زیر مبلغ پرداختی این روز
                    if (isAbsent) {
                        // در صورت غیبت: مبلغی ثبت نمی‌شود و کلمه غیبت با رنگ قرمز به جای مبلغ نوشته می‌شود
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isHourly) "دستمزد ساعتی: " else "دستمزد روزانه: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "غیبت",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseAccent
                            )
                        }
                    } else if (isHourly) {
                        Text(
                            text = if (dayCalc.hourlyHours > 0) {
                                "دستمزد ساعتی: ${Formatters.toPersianDigits(dayCalc.hourlyHours.toString().removeSuffix(".0"))} ساعت (هر ساعت ${Formatters.formatCurrency(hRate)}) = ${Formatters.formatCurrency(dayCalc.hourlyPay)}"
                            } else {
                                "دستمزد ساعتی: (هر ساعت ${Formatters.formatCurrency(hRate)}) = ۰ تومان"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = CyanAccent
                        )
                    } else if (isHalfDay) {
                        Text(
                            text = "دستمزد روزانه (نصف روز): ${Formatters.formatCurrency(dayCalc.baseWage)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AmberAccent
                        )
                    } else if (isFullDay) {
                        Text(
                            text = "دستمزد روزانه: ${Formatters.formatCurrency(dayCalc.baseWage)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (worker.baseDailyWage > 0) {
                        Text(
                            text = "دستمزد روزانه: ${Formatters.formatCurrency(worker.baseDailyWage)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isAbsent && dayCalc.overtimeHours > 0) {
                        Text(
                            text = "اضافه کار: ${Formatters.toPersianDigits(dayCalc.overtimeHours.toString().removeSuffix(".0"))} ساعت (هر ساعت ${Formatters.formatCurrency(otRate)}) = ${Formatters.formatCurrency(dayCalc.overtimePay)}",
                            fontSize = 10.5.sp,
                            color = AmberAccent
                        )
                    }
                }
            }

            // گزینه های ایاب و ذهاب و غیره زیر مبلغ پرداختی نهایی
            // قانون غیبت: افزایش ایاب و ذهاب و غیره شامل غایب نمی‌شود، اما فقط کسورات شامل می‌شود
            val financialItems = buildList {
                if (worker.transitAllowance > 0) {
                    val isAllowance = worker.transitImpact == "ALLOWANCE"
                    if (!isAbsent || !isAllowance) {
                        add(Triple("ایاب و ذهاب", worker.transitAllowance, isAllowance))
                    }
                }
                if (worker.accommodationAllowance > 0) {
                    val isAllowance = worker.accommodationImpact == "ALLOWANCE"
                    if (!isAbsent || !isAllowance) {
                        add(Triple("حق مسکن", worker.accommodationAllowance, isAllowance))
                    }
                }
                if (worker.foodAllowance > 0) {
                    val isAllowance = worker.foodImpact == "ALLOWANCE"
                    if (!isAbsent || !isAllowance) {
                        add(Triple("خوراک", worker.foodAllowance, isAllowance))
                    }
                }
                if (worker.medicalAllowance > 0) {
                    val isAllowance = worker.medicalImpact == "ALLOWANCE"
                    if (!isAbsent || !isAllowance) {
                        add(Triple("درمان", worker.medicalAllowance, isAllowance))
                    }
                }
            }

            if (financialItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    financialItems.forEach { (label, amount, isIncrease) ->
                        val color = if (isIncrease) EmeraldAccent else RoseAccent
                        val bg = if (isIncrease) EmeraldAccent.copy(alpha = 0.12f) else RoseAccent.copy(alpha = 0.12f)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = bg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$label: ",
                                    fontSize = 10.sp,
                                    color = color,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isIncrease) "+${Formatters.formatThousandsPersian(amount)}" else "-${Formatters.formatThousandsPersian(amount)}",
                                    fontSize = 10.sp,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (worker.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "یادداشت: ${worker.notes}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}
