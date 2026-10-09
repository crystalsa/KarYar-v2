package com.karyar.app.ui.screens.attendance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.util.Formatters
import com.karyar.app.util.WageCalculator

@Composable
fun AttendanceWorkerRow(
    worker: WorkerEntity,
    attendance: AttendanceEntity?,
    targetDate: String,
    onEditWorker: () -> Unit,
    onSetStatus: (String) -> Unit,
    onHourlyRequired: () -> Unit,
    onCopyWorker: () -> Unit,
    onDeleteWorker: () -> Unit
) {
    val isHourlyWorker = WageCalculator.isHourly(worker, attendance)
    val isAbsent = WageCalculator.isAbsent(worker, attendance)
    val isHalfDay = WageCalculator.isHalfDay(worker, attendance)
    val isFullDay = WageCalculator.isFullDay(worker, attendance)
    val isHourlyChecked = isHourlyWorker && !isAbsent
    val otHours = if (attendance != null && attendance.overtimeHours > 0.0) attendance.overtimeHours else worker.overtimeHours
    val hasOvertime = otHours > 0.0

    val hHours = if (attendance != null && attendance.hourlyHours > 0.0) attendance.hourlyHours else (if (worker.hourlyHours > 0.0) worker.hourlyHours else 0.0)
    val hasHourly = hHours > 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Worker avatar + name + role (Clickable to edit profile)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onEditWorker)
                .padding(vertical = 2.dp, horizontal = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(worker.colorTag)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = worker.name.take(1),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = worker.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val statusDetails = buildList {
                    add(worker.role)
                    if (isHourlyChecked) add("ساعتی")
                    else if (isFullDay) add("تمام روز")
                    else if (isHalfDay) add("نصف روز")
                    else if (isAbsent) add("غایب")
                    if (hasHourly && !isHourlyChecked) add("${Formatters.toPersianDigits(hHours.toString().removeSuffix(".0"))} ساعت ساعتی")
                    if (hasOvertime) add("+${Formatters.toPersianDigits(otHours.toString().removeSuffix(".0"))} ساعت اضافه")
                }.joinToString(" • ")

                Text(
                    text = statusDetails,
                    fontSize = 9.5.sp,
                    color = if (isAbsent) RoseAccent else if (isHalfDay) AmberAccent else if (isHourlyChecked) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action Checkboxes: Full Day, Half Day, Hourly, Absent + 3-dots Menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Full Day Presence Checkbox
            Box(
                modifier = Modifier.width(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isFullDay,
                    onCheckedChange = {
                        if (!isHourlyWorker) {
                            onSetStatus("FULL")
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldAccent,
                        checkmarkColor = Color.White,
                        uncheckedColor = EmeraldAccent.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            // 2. Half Day Presence Checkbox
            Box(
                modifier = Modifier.width(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isHalfDay,
                    onCheckedChange = {
                        if (!isHourlyWorker) {
                            onSetStatus("HALF")
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AmberAccent,
                        checkmarkColor = Color.White,
                        uncheckedColor = AmberAccent.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3. Hourly Presence Checkbox
            Box(
                modifier = Modifier.width(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isHourlyChecked,
                    onCheckedChange = {
                        if (!worker.isHourlyEnabled || (worker.hourlyWageRate <= 0 && worker.baseHourlyWage <= 0)) {
                            onHourlyRequired()
                        } else {
                            onSetStatus("HOURLY")
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = CyanAccent,
                        checkmarkColor = Color.White,
                        uncheckedColor = CyanAccent.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            // 4. Absence Checkbox
            Box(
                modifier = Modifier.width(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isAbsent,
                    onCheckedChange = {
                        onSetStatus("ABSENT")
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = RoseAccent,
                        checkmarkColor = Color.White,
                        uncheckedColor = RoseAccent.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3-dots Menu Button
            var menuExpanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.width(26.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("worker_menu_${worker.id}")
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "گزینه‌های کارگر",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "کپی",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onCopyWorker()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "حذف",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseAccent
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = RoseAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteWorker()
                        }
                    )
                }
            }
        }
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = Slate200.copy(alpha = 0.5f)
    )
}
