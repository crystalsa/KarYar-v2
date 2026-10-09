package com.karyar.app.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.domain.model.WorkerPerformance
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.LoadingOutlinedButton
import com.karyar.app.ui.components.StatusBadge
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate400
import com.karyar.app.util.Formatters
import com.karyar.app.util.WageCalculator

@Composable
fun WorkerDetailDialog(
    worker: WorkerEntity,
    attendance: AttendanceEntity? = null,
    performance: WorkerPerformance? = null,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Android-Iconics styled avatar container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(worker.colorTag))
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = worker.name.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = worker.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = worker.role,
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberAccent,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status badge: مشغول به کار یا غایب
                val isWorkerAbsent = WageCalculator.isAbsent(worker, attendance)
                StatusBadge(
                    text = when {
                        isWorkerAbsent -> "غایب"
                        worker.isActive -> "مشغول به کار"
                        else -> "مرخصی"
                    },
                    dotColor = when {
                        isWorkerAbsent -> RoseAccent
                        worker.isActive -> EmeraldAccent
                        else -> Slate400
                    }
                )

                // شماره تلفن: زیر مشغول به کار و بالای کد ملی
                if (worker.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = Icons.Default.Phone,
                            color = EmeraldAccent,
                            size = IconicsSize.TINY
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Formatters.toPersianDigits(worker.phone),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // کد ملی: زیر شماره تلفن
                if (worker.nationalId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = Icons.Default.Badge,
                            color = CyanAccent,
                            size = IconicsSize.TINY
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "کد ملی: ${Formatters.toPersianDigits(worker.nationalId)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Financial Overview Card (Tight Rows)
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Slate100,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "خلاصه مالی و کارکرد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val isHourly = WageCalculator.isHourly(worker, attendance)
                        val isAbsent = WageCalculator.isAbsent(worker, attendance)
                        val isHalfDay = WageCalculator.isHalfDay(worker, attendance)
                        val isFullDay = WageCalculator.isFullDay(worker, attendance)

                        val dayCalc = WageCalculator.calculateDay(worker, attendance)

                        if (attendance != null) {
                            val attStatusText = when {
                                isAbsent -> "غیبت"
                                isHourly -> "ساعتی"
                                isHalfDay -> "نصف روز"
                                isFullDay -> "تمام روز"
                                else -> "ثبت شده"
                            }
                            val attStatusColor = when {
                                isAbsent -> RoseAccent
                                isHourly -> CyanAccent
                                isHalfDay -> AmberAccent
                                else -> EmeraldAccent
                            }
                            CompactDetailRow(
                                label = "وضعیت تردد این روز:",
                                value = attStatusText,
                                valueColor = attStatusColor
                            )
                        }

                        if (isAbsent) {
                            CompactDetailRow(
                                label = if (isHourly) "دستمزد ساعتی این روز:" else "دستمزد روزانه این روز:",
                                value = "غایب (دستمزد و اضافه کار تعلق نمی‌گیرد)",
                                valueColor = RoseAccent
                            )
                        } else if (isHourly) {
                            val hRate = if (attendance != null && attendance.hourlyWageRate > 0) attendance.hourlyWageRate
                                        else if (attendance != null && attendance.hourlyWage > 0) attendance.hourlyWage
                                        else if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage
                            CompactDetailRow(
                                label = "دستمزد ساعتی:",
                                value = if (dayCalc.hourlyHours > 0) {
                                    "${Formatters.toPersianDigits(dayCalc.hourlyHours.toString().removeSuffix(".0"))} ساعت (هر ساعت ${Formatters.formatCurrency(hRate)}) = ${Formatters.formatCurrency(dayCalc.hourlyPay)}"
                                } else {
                                    "(هر ساعت ${Formatters.formatCurrency(hRate)}) = ۰ تومان"
                                },
                                valueColor = CyanAccent
                            )
                        } else if (isHalfDay) {
                            CompactDetailRow(
                                label = "دستمزد روزانه (نصف روز):",
                                value = Formatters.formatCurrency(dayCalc.baseWage),
                                valueColor = AmberAccent
                            )
                        } else if (dayCalc.baseWage > 0L) {
                            CompactDetailRow("دستمزد روزانه:", Formatters.formatCurrency(dayCalc.baseWage))
                        }

                        if (!isAbsent && dayCalc.overtimeHours > 0.0) {
                            val otRate = if (attendance != null && attendance.overtimeRate > 0) attendance.overtimeRate else worker.overtimeRate
                            CompactDetailRow(
                                "اضافه کاری:",
                                "${Formatters.toPersianDigits(dayCalc.overtimeHours.toString().removeSuffix(".0"))} ساعت (هر ساعت ${Formatters.formatCurrency(otRate)}) = ${Formatters.formatCurrency(dayCalc.overtimePay)}",
                                valueColor = AmberAccent
                            )
                        }

                        if (!isAbsent && dayCalc.totalAllowances > 0) {
                            CompactDetailRow(
                                label = "(+) کمک‌هزینه‌های روزانه:",
                                value = "+${Formatters.formatCurrency(dayCalc.totalAllowances)}",
                                valueColor = EmeraldAccent
                            )
                        }

                        if (dayCalc.totalDeductions > 0) {
                            CompactDetailRow(
                                label = "(-) کسورات روزانه:",
                                value = "-${Formatters.formatCurrency(dayCalc.totalDeductions)}",
                                valueColor = RoseAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مبلغ پرداختی نهایی:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAbsent) "۰ تومان (غیبت)" else Formatters.formatCurrency(dayCalc.netPayout),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = if (isAbsent) RoseAccent else EmeraldAccent
                            )
                        }
                    }
                }

                // Personal Description / Notes
                if (worker.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "یادداشت: ${worker.notes}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action: Close
                LoadingButton(
                    text = "بستن",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AmberAccent,
                    height = 38.dp,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

@Composable
private fun CompactDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
