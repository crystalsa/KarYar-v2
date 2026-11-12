package com.karyar.app.ui.screens.workers.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.util.Formatters
import com.karyar.app.util.WageCalculator

@Composable
fun DateFolderCard(
    dateFolder: DateFolderEntity,
    workers: List<WorkerEntity>,
    attendanceList: List<AttendanceEntity>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var totalWages = 0L
    var totalTransit = 0L
    var totalFood = 0L
    var totalAccommodation = 0L
    var totalMedical = 0L
    var presentCount = 0
    var folderTotalDayPayout = 0L

    for (worker in workers) {
        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == dateFolder.date } ?: continue
        val dayRes = WageCalculator.calculateDayOrNull(worker, att) ?: continue
        if (dayRes.isWorkingDay) {
            presentCount++
        }
        try {
            totalWages = Math.addExact(totalWages, Math.addExact(dayRes.baseWage, dayRes.hourlyPay))
            totalTransit = Math.addExact(totalTransit, dayRes.transitAllowance - dayRes.transitDeduction)
            totalFood = Math.addExact(totalFood, dayRes.foodAllowance - dayRes.foodDeduction)
            totalAccommodation = Math.addExact(totalAccommodation, dayRes.accommodationAllowance - dayRes.accommodationDeduction)
            totalMedical = Math.addExact(totalMedical, dayRes.medicalAllowance - dayRes.medicalDeduction)
            folderTotalDayPayout = Math.addExact(folderTotalDayPayout, dayRes.netPayout)
        } catch (_: ArithmeticException) {
            // Skip record sum if overflow
        }
    }

    HairlineCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("date_folder_card_${dateFolder.id}"),
        shape = RoundedCornerShape(12.dp),
        borderColor = Color(0xFFE2E8F0),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dateFolder.dayOfWeek,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Formatters.toPersianDigits(dateFolder.date),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (dateFolder.title.isNotBlank()) {
                            Text(
                                text = dateFolder.title,
                                fontSize = 10.5.sp,
                                color = AmberAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "ویرایش روز",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "حذف روز",
                            tint = RoseAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${Formatters.toPersianDigits(presentCount)} حاضر از ${Formatters.toPersianDigits(workers.size)} نفر",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${Formatters.formatThousandsPersian(totalWages)} تومان",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldAccent
                )
            }

            if (totalTransit != 0L || totalFood != 0L || totalAccommodation != 0L || totalMedical != 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (totalTransit != 0L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = if (totalTransit > 0) EmeraldAccent else RoseAccent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${Formatters.formatCurrency(Math.abs(totalTransit))}",
                                fontSize = 9.5.sp,
                                color = if (totalTransit > 0) EmeraldAccent else RoseAccent
                            )
                        }
                    }
                    if (totalFood != 0L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalDining,
                                contentDescription = null,
                                tint = if (totalFood > 0) EmeraldAccent else RoseAccent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${Formatters.formatCurrency(Math.abs(totalFood))}",
                                fontSize = 9.5.sp,
                                color = if (totalFood > 0) EmeraldAccent else RoseAccent
                            )
                        }
                    }
                    if (totalAccommodation != 0L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                tint = if (totalAccommodation > 0) EmeraldAccent else RoseAccent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${Formatters.formatCurrency(Math.abs(totalAccommodation))}",
                                fontSize = 9.5.sp,
                                color = if (totalAccommodation > 0) EmeraldAccent else RoseAccent
                            )
                        }
                    }
                    if (totalMedical != 0L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = if (totalMedical > 0) EmeraldAccent else RoseAccent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${Formatters.formatCurrency(Math.abs(totalMedical))}",
                                fontSize = 9.5.sp,
                                color = if (totalMedical > 0) EmeraldAccent else RoseAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "خالص روز کارگاه:",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (folderTotalDayPayout < 0L) "بدهکار: ${Formatters.formatCurrency(-folderTotalDayPayout)}" else Formatters.formatCurrency(folderTotalDayPayout),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (folderTotalDayPayout < 0L) RoseAccent else EmeraldAccent
                )
            }
        }
    }
}
