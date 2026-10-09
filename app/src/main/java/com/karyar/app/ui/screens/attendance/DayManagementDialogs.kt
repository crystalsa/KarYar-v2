package com.karyar.app.ui.screens.attendance

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.LoadingOutlinedButton
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.ui.theme.Slate700
import com.karyar.app.util.Formatters
import com.karyar.app.util.JalaliCalendar

/**
 * Menu shown when long-pressing a day chip.
 * Offers "ویرایش روز" (Edit) and "حذف روز" (Delete).
 */
@Composable
fun DayActionOptionsDialog(
    dateFolder: DateFolderEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("day_action_options_dialog"),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = Icons.Default.CalendarMonth,
                            color = AmberAccent,
                            size = IconicsSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "مدیریت روز کاری",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${dateFolder.dayOfWeek} ${Formatters.toPersianDigits(dateFolder.date)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberAccent
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action 1: Edit Day
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberAccent.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onEdit() }
                        .testTag("day_action_edit_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ویرایش روز",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAccent
                            )
                            Text(
                                text = "تغییر تاریخ، روز هفته یا عنوان روز کاری",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 2: Delete Day
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoseAccent.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoseAccent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDelete() }
                        .testTag("day_action_delete_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = RoseAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "حذف روز",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseAccent
                            )
                            Text(
                                text = "حذف این روز کاری از کارگاه",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LoadingOutlinedButton(
                    text = "انصراف",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    height = 38.dp,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Edit dialog for an existing DateFolderEntity, with full Persian calendar stepper and phone sync.
 */
@Composable
fun EditDateFolderDialog(
    dateFolder: DateFolderEntity,
    onDismiss: () -> Unit,
    onConfirm: (date: String, dayOfWeek: String, title: String) -> Unit
) {
    var date by remember(dateFolder) { mutableStateOf(dateFolder.date) }
    var dayOfWeek by remember(dateFolder) { mutableStateOf(dateFolder.dayOfWeek) }
    var title by remember(dateFolder) { mutableStateOf(dateFolder.title) }

    val (year, month, day) = JalaliCalendar.parseDate(date)
    val monthName = JalaliCalendar.getMonthName(month)
    val daysInCurrentMonth = JalaliCalendar.getDaysInMonth(year, month)
    val isLeap = JalaliCalendar.isLeapYear(year)

    Dialog(onDismissRequest = onDismiss) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("edit_date_folder_dialog"),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = Icons.Default.Edit,
                            color = AmberAccent,
                            size = IconicsSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ویرایش روز کاری",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Navigation: Next Day / Previous Day & Sync with Phone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Next Day (+1 Day)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberAccent.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val (nextD, nextDow) = JalaliCalendar.nextDay(date)
                                date = nextD
                                dayOfWeek = nextDow
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "روز بعد (+۱)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAccent
                            )
                        }
                    }

                    // Previous Day (-1 Day)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate100,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val (prevD, prevDow) = JalaliCalendar.previousDay(date)
                                date = prevD
                                dayOfWeek = prevDow
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "روز قبل (-۱)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Sync with Phone Calendar
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyanAccent.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                date = JalaliCalendar.todayString()
                                dayOfWeek = JalaliCalendar.todayDayOfWeek()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Smartphone,
                                contentDescription = "امروز گوشی",
                                tint = CyanAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "امروز",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- OPTION 1: Days of Week (گزینه روزها) ---
                Text(
                    text = "۱. روز هفته:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    JalaliCalendar.DAYS_OF_WEEK.forEach { dow ->
                        val isSelected = (dayOfWeek == dow)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AmberAccent else Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val currentIdx = JalaliCalendar.DAYS_OF_WEEK.indexOf(dayOfWeek).let { if (it == -1) 0 else it }
                                    val targetIdx = JalaliCalendar.DAYS_OF_WEEK.indexOf(dow).let { if (it == -1) 0 else it }
                                    var diff = targetIdx - currentIdx
                                    if (diff < 0) diff += 7
                                    if (diff > 0) {
                                        val (newD, newDow) = JalaliCalendar.addDays(date, diff)
                                        date = newD
                                        dayOfWeek = newDow
                                    } else {
                                        dayOfWeek = dow
                                    }
                                }
                        ) {
                            Text(
                                text = dow,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- OPTION 2: Date (زیرش تاریخ) ---
                Text(
                    text = "۲. تاریخ شمسی:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Prominent Date Display Card
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Slate100
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$dayOfWeek ${Formatters.toPersianDigits(day)} $monthName ${Formatters.toPersianDigits(year)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = Formatters.toPersianDigits(date),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberAccent
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Steppers for Day, Month, Year
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Day Stepper
                            DateStepperBox(
                                label = "روز ($daysInCurrentMonth روزه)",
                                value = Formatters.toPersianDigits(day),
                                onIncrement = {
                                    val (nextD, nextDow) = JalaliCalendar.nextDay(date)
                                    date = nextD
                                    dayOfWeek = nextDow
                                },
                                onDecrement = {
                                    val (prevD, prevDow) = JalaliCalendar.previousDay(date)
                                    date = prevD
                                    dayOfWeek = prevDow
                                },
                                modifier = Modifier.weight(1.1f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Month Stepper
                            DateStepperBox(
                                label = "ماه",
                                value = monthName,
                                onIncrement = {
                                    val nextM = if (month < 12) month + 1 else 1
                                    val nextY = if (month < 12) year else year + 1
                                    val maxD = JalaliCalendar.getDaysInMonth(nextY, nextM)
                                    val clampedD = day.coerceAtMost(maxD)
                                    val newDateStr = JalaliCalendar.formatDate(nextY, nextM, clampedD)
                                    date = newDateStr
                                    dayOfWeek = JalaliCalendar.getDayOfWeek(newDateStr)
                                },
                                onDecrement = {
                                    val prevM = if (month > 1) month - 1 else 12
                                    val prevY = if (month > 1) year else year - 1
                                    val maxD = JalaliCalendar.getDaysInMonth(prevY, prevM)
                                    val clampedD = day.coerceAtMost(maxD)
                                    val newDateStr = JalaliCalendar.formatDate(prevY, prevM, clampedD)
                                    date = newDateStr
                                    dayOfWeek = JalaliCalendar.getDayOfWeek(newDateStr)
                                },
                                modifier = Modifier.weight(1.1f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Year Stepper
                            DateStepperBox(
                                label = "سال",
                                value = Formatters.toPersianDigits(year),
                                onIncrement = {
                                    val nextY = year + 1
                                    val maxD = JalaliCalendar.getDaysInMonth(nextY, month)
                                    val clampedD = day.coerceAtMost(maxD)
                                    val newDateStr = JalaliCalendar.formatDate(nextY, month, clampedD)
                                    date = newDateStr
                                    dayOfWeek = JalaliCalendar.getDayOfWeek(newDateStr)
                                },
                                onDecrement = {
                                    val prevY = year - 1
                                    val maxD = JalaliCalendar.getDaysInMonth(prevY, month)
                                    val clampedD = day.coerceAtMost(maxD)
                                    val newDateStr = JalaliCalendar.formatDate(prevY, month, clampedD)
                                    date = newDateStr
                                    dayOfWeek = JalaliCalendar.getDayOfWeek(newDateStr)
                                },
                                modifier = Modifier.weight(0.9f)
                            )
                        }

                        if (month == 12 && isLeap) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "سال $year کبیسه است (اسفند ۳۰ روزه)",
                                fontSize = 10.5.sp,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان یا توضیح روز", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingOutlinedButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 42.dp
                    )
                    LoadingButton(
                        text = "ذخیره",
                        icon = Icons.Default.Edit,
                        onClick = { onConfirm(date, dayOfWeek, title) },
                        modifier = Modifier.weight(1.3f),
                        containerColor = AmberAccent,
                        height = 42.dp,
                        testTag = "confirm_edit_day_button"
                    )
                }
            }
        }
    }
}

/**
 * Confirmation dialog for day deletion.
 */
@Composable
fun DeleteDayConfirmDialog(
    dateFolder: DateFolderEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "حذف روز کاری",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Text(
                text = "آیا از حذف روز ${dateFolder.dayOfWeek} (${Formatters.toPersianDigits(dateFolder.date)}) اطمینان دارید؟ تمام ترددهای ثبت‌شده برای این روز نیز حذف خواهند شد.",
                fontSize = 12.5.sp
            )
        },
        confirmButton = {
            LoadingButton(
                text = "حذف",
                onClick = onConfirm,
                containerColor = RoseAccent,
                height = 38.dp,
                fontSize = 12.sp,
                testTag = "confirm_delete_day_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
private fun DateStepperBox(
    label: String,
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = Slate700,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrement (-)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Slate100,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onDecrement() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    }
                }

                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                // Increment (+)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Slate100,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onIncrement() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    }
                }
            }
        }
    }
}
