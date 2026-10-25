package com.karyar.app.ui.screens.workers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.repository.DateConflictException
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.LoadingOutlinedButton
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.util.Formatters
import com.karyar.app.util.JalaliCalendar

private val DAYS_OF_WEEK = listOf(
    "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
)

@Composable
fun AddEditDateFolderDialog(
    initialDateFolder: DateFolderEntity? = null,
    existingAttendances: List<AttendanceEntity> = emptyList(),
    workers: List<WorkerEntity> = emptyList(),
    conflictError: String? = null,
    conflictingWorkerNames: List<String> = emptyList(),
    onMergeConflict: ((date: String, dayOfWeek: String, title: String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (date: String, dayOfWeek: String, title: String) -> Unit
) {
    val initialDate = initialDateFolder?.date ?: JalaliCalendar.todayString()
    var date by remember { mutableStateOf(initialDate) }
    var dayOfWeek by remember {
        mutableStateOf(initialDateFolder?.dayOfWeek ?: JalaliCalendar.getDayOfWeek(initialDate))
    }
    var title by remember {
        mutableStateOf(initialDateFolder?.title ?: "روز کاری")
    }
    var dateError by remember { mutableStateOf<String?>(null) }
    var detectedConflictMessage by remember(conflictError) { mutableStateOf(conflictError) }
    var detectedConflictWorkerNames by remember(conflictingWorkerNames) { mutableStateOf(conflictingWorkerNames) }

    val titleError = Formatters.validateOptionalName(title, "عنوان روز کاری")
    val liveDateError = dateError ?: JalaliCalendar.validateDateError(date)
    val isFormValid = liveDateError == null && titleError == null && dayOfWeek.isNotBlank() && detectedConflictMessage == null

    fun findConflictingWorkersForTarget(targetNormalizedDate: String): List<String>? {
        val df = initialDateFolder ?: return null
        if (targetNormalizedDate == df.date || existingAttendances.isEmpty()) return null
        val sourceAtts = existingAttendances.filter {
            it.dateFolderId == df.id || (it.folderId == df.folderId && it.date == df.date)
        }
        val sourceIds = sourceAtts.map { it.id }.toSet()
        val sourceWorkerIds = sourceAtts.map { it.workerId }.toSet()
        val directConflicts = existingAttendances.filter {
            it.date == targetNormalizedDate && it.id !in sourceIds && it.workerId in sourceWorkerIds
        }
        val folderConflicts = existingAttendances.filter {
            it.date == targetNormalizedDate && it.id !in sourceIds && it.folderId == df.folderId
        }
        val conflicts = if (directConflicts.isNotEmpty()) directConflicts else folderConflicts
        if (conflicts.isEmpty()) return null
        val workerById = workers.associateBy { it.id }
        val distinctIds = conflicts.map { it.workerId }.distinct()
        return distinctIds.map { wId -> workerById[wId]?.name ?: "کارگر" }
    }

    Dialog(onDismissRequest = onDismiss) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Header with Android-Iconics box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = Icons.Default.Folder,
                            color = AmberAccent,
                            size = IconicsSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (initialDateFolder == null) "پوشه جدید تاریخ و روز" else "ویرایش پوشه روز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date input
                val activeErrorText = detectedConflictMessage ?: liveDateError
                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                        detectedConflictMessage = null
                        detectedConflictWorkerNames = emptyList()
                        val err = JalaliCalendar.validateDateError(it)
                        dateError = err
                        if (err == null) {
                            val detectedDay = JalaliCalendar.getDayOfWeekOrNull(it)
                            if (detectedDay != null && detectedDay in DAYS_OF_WEEK) {
                                dayOfWeek = detectedDay
                            }
                        }
                    },
                    label = { Text("تاریخ شمسی (مثلاً ۱۴۰۵/۰۱/۱۵)", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    },
                    isError = activeErrorText != null,
                    supportingText = if (activeErrorText != null) {
                        {
                            Text(
                                text = activeErrorText,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_folder_date_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Auto-advance buttons: روز بعدی (حرکت خودکار تاریخ به جلو) و روز قبلی
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AmberAccent.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val err = JalaliCalendar.validateDateError(date)
                                if (err == null) {
                                    val (nextD, nextDow) = JalaliCalendar.nextDay(date)
                                    date = nextD
                                    dayOfWeek = nextDow
                                    dateError = null
                                    detectedConflictMessage = null
                                } else {
                                    dateError = err
                                }
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "روز بعدی (+۱ روز)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAccent
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val err = JalaliCalendar.validateDateError(date)
                                if (err == null) {
                                    val (prevD, prevDow) = JalaliCalendar.previousDay(date)
                                    date = prevD
                                    dayOfWeek = prevDow
                                    dateError = null
                                    detectedConflictMessage = null
                                } else {
                                    dateError = err
                                }
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "روز قبلی (-۱ روز)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Day of Week selector
                Text(
                    text = "روز هفته:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DAYS_OF_WEEK.forEach { day ->
                        val isSelected = dayOfWeek == day
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) AmberAccent else Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { dayOfWeek = day }
                        ) {
                            Text(
                                text = day,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                fontSize = 11.5.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان یا توضیح پوشه (اختیاری)", fontSize = 12.sp) },
                    placeholder = { Text("مثلاً روز کاری، بتن‌ریزی سقف", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    },
                    isError = titleError != null,
                    supportingText = if (titleError != null) {
                        {
                            Text(
                                text = titleError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.5.sp
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_folder_title_input"),
                    singleLine = true
                )

                if (detectedConflictMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoseAccent.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("date_conflict_warning_box")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = detectedConflictMessage!!,
                                color = RoseAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (detectedConflictWorkerNames.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "کارگران دارای تداخل: ${detectedConflictWorkerNames.joinToString("، ")}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                            }
                            if (onMergeConflict != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LoadingOutlinedButton(
                                        text = "لغو",
                                        onClick = {
                                            detectedConflictMessage = null
                                            detectedConflictWorkerNames = emptyList()
                                            onDismiss()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("cancel_merge_date_folder_button"),
                                        height = 36.dp,
                                        fontSize = 11.5.sp
                                    )
                                    LoadingButton(
                                        text = "ادغام",
                                        onClick = {
                                            val parsed = JalaliCalendar.parseDateOrNull(date)
                                            if (parsed != null && titleError == null) {
                                                val normalizedDate = JalaliCalendar.formatDate(parsed.first, parsed.second, parsed.third)
                                                onMergeConflict(normalizedDate, dayOfWeek.trim(), title.trim())
                                            }
                                        },
                                        enabled = titleError == null,
                                        modifier = Modifier.weight(1f),
                                        containerColor = AmberAccent,
                                        height = 36.dp,
                                        fontSize = 11.5.sp,
                                        testTag = "confirm_merge_date_folder_button"
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions with LoadingButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingOutlinedButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    LoadingButton(
                        text = if (initialDateFolder == null) "ایجاد پوشه" else "ذخیره تغییرات",
                        icon = Icons.Default.Folder,
                        onClick = {
                            val err = JalaliCalendar.validateDateError(date)
                            if (err != null) {
                                dateError = err
                            } else if (dayOfWeek.isNotBlank() && titleError == null) {
                                val (y, m, d) = JalaliCalendar.parseDate(date)
                                val normalizedDate = JalaliCalendar.formatDate(y, m, d)
                                val conflictingNames = findConflictingWorkersForTarget(normalizedDate)
                                if (!conflictingNames.isNullOrEmpty()) {
                                    val msg = DateConflictException.buildMessage(conflictingNames.size)
                                    detectedConflictMessage = msg
                                    detectedConflictWorkerNames = conflictingNames
                                    dateError = msg
                                } else {
                                    onConfirm(normalizedDate, dayOfWeek.trim(), title.trim())
                                }
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier.weight(1.3f),
                        containerColor = AmberAccent,
                        height = 40.dp,
                        testTag = "submit_date_folder_button"
                    )
                }
            }
        }
    }
}

