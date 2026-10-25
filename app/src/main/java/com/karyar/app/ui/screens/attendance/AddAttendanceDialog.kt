package com.karyar.app.ui.screens.attendance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.screens.workers.components.WorkerDailyWageSection
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.util.Formatters
import com.karyar.app.util.JalaliCalendar

@Composable
fun AddAttendanceDialog(
    folder: WorkplaceFolderEntity?,
    workers: List<WorkerEntity>,
    onDismiss: () -> Unit,
    onConfirm: (AttendanceEntity) -> Unit
) {
    var selectedWorker by remember { mutableStateOf(workers.firstOrNull()) }
    var date by remember { mutableStateOf(JalaliCalendar.todayString()) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var entryTime by remember { mutableStateOf("08:00") }
    var exitTime by remember { mutableStateOf("17:00") }

    // Numeric inputs start empty by default
    var regularHours by remember { mutableStateOf("") }
    var hasOvertime by remember { mutableStateOf(false) }
    var overtimeHours by remember { mutableStateOf("") }

    var isDailyWageEnabled by remember {
        mutableStateOf(selectedWorker?.isHourlyEnabled != true && (selectedWorker?.baseDailyWage ?: 0L) > 0L)
    }

    var isHourly by remember {
        mutableStateOf(selectedWorker?.isHourlyEnabled == true)
    }

    var dailyWage by remember {
        mutableStateOf(
            if (selectedWorker?.isHourlyEnabled != true && (selectedWorker?.baseDailyWage ?: 0L) > 0L)
                Formatters.formatThousandsPersian(selectedWorker!!.baseDailyWage)
            else ""
        )
    }
    var hourlyWage by remember {
        mutableStateOf(
            if (selectedWorker?.isHourlyEnabled == true) {
                val r = if ((selectedWorker?.hourlyWageRate ?: 0L) > 0L) selectedWorker!!.hourlyWageRate else (selectedWorker?.baseHourlyWage ?: 0L)
                if (r > 0L) Formatters.formatThousandsPersian(r) else ""
            } else ""
        )
    }

    var workplaceName by remember { mutableStateOf(folder?.name ?: "") }
    var employerName by remember { mutableStateOf(folder?.employerName ?: "") }
    var foremanName by remember { mutableStateOf(folder?.foremanName ?: "") }
    var notes by remember { mutableStateOf("") }

    val regularHoursError = Formatters.validateHoursInput(
        regularHours,
        fieldLabel = if (isHourly) "ساعت کار ساعتی" else "ساعات کار عادی",
        maxHours = Formatters.MAX_REGULAR_HOURS
    )
    val overtimeHoursError = if (hasOvertime) {
        Formatters.validateHoursInput(
            overtimeHours,
            fieldLabel = "ساعات اضافه‌کاری",
            maxHours = Formatters.MAX_OVERTIME_HOURS
        )
    } else null
    val dailyWageError = if (isDailyWageEnabled) {
        Formatters.validateWageInput(dailyWage, fieldLabel = "دستمزد روزانه")
    } else null
    val hourlyWageError = if (isHourly) {
        Formatters.validateWageInput(hourlyWage, fieldLabel = "نرخ دستمزد ساعتی")
    } else null
    val workplaceNameError = Formatters.validateOptionalName(workplaceName, fieldLabel = "نام محل کار")
    val employerNameError = Formatters.validateOptionalName(employerName, fieldLabel = "نام کارفرما")
    val foremanNameError = Formatters.validateOptionalName(foremanName, fieldLabel = "نام سرکارگر")
    val liveDateError = dateError ?: JalaliCalendar.validateDateError(date)

    val isFormValid = selectedWorker != null &&
        liveDateError == null &&
        regularHoursError == null &&
        overtimeHoursError == null &&
        dailyWageError == null &&
        hourlyWageError == null &&
        workplaceNameError == null &&
        employerNameError == null &&
        foremanNameError == null

    // Auto update wage when selected worker changes
    fun onWorkerChanged(worker: WorkerEntity) {
        selectedWorker = worker
        isHourly = worker.isHourlyEnabled
        isDailyWageEnabled = !worker.isHourlyEnabled && worker.baseDailyWage > 0L
        if (worker.isHourlyEnabled) {
            dailyWage = ""
            val r = if (worker.hourlyWageRate > 0L) worker.hourlyWageRate else worker.baseHourlyWage
            hourlyWage = if (r > 0L) Formatters.formatThousandsPersian(r) else ""
        } else {
            dailyWage = if (worker.baseDailyWage > 0L) Formatters.formatThousandsPersian(worker.baseDailyWage) else ""
            hourlyWage = ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ثبت ورود و خروج (تردد)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select Worker Chips
                Text(
                    text = "انتخاب کارگر:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    workers.forEach { worker ->
                        val isSelected = selectedWorker?.id == worker.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AmberAccent else Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onWorkerChanged(worker) }
                        ) {
                            Text(
                                text = "${worker.name} (${worker.role})",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.5.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date & Time inputs
                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                        dateError = JalaliCalendar.validateDateError(it)
                    },
                    label = { Text("تاریخ (شمسی)") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AmberAccent) },
                    isError = liveDateError != null,
                    supportingText = if (liveDateError != null) {
                        {
                            Text(
                                text = liveDateError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Entry and Exit Times
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = entryTime,
                        onValueChange = { entryTime = it },
                        label = { Text("ساعت ورود") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = EmeraldAccent) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exitTime,
                        onValueChange = { exitTime = it },
                        label = { Text("ساعت خروج") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = CyanAccent) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Regular Hours
                OutlinedTextField(
                    value = regularHours,
                    onValueChange = { regularHours = it },
                    label = { Text("ساعات کار عادی") },
                    placeholder = { Text("۸") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = regularHoursError != null,
                    supportingText = if (regularHoursError != null) {
                        {
                            Text(
                                text = regularHoursError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.5.sp
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attendance_regular_hours_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Overtime activated separately with checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { hasOvertime = !hasOvertime }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = hasOvertime,
                        onCheckedChange = { hasOvertime = it },
                        colors = CheckboxDefaults.colors(checkedColor = AmberAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ثبت اضافه کاری برای این روز",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (hasOvertime) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = overtimeHours,
                        onValueChange = { overtimeHours = it },
                        label = { Text("ساعات اضافه کاری") },
                        placeholder = { Text("مثلاً ۲") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = overtimeHoursError != null,
                        supportingText = if (overtimeHoursError != null) {
                            {
                                Text(
                                    text = overtimeHoursError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.5.sp
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attendance_overtime_hours_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = Slate200
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Base Daily Wage (دستمزد روزانه ثابت با تیک‌باکس) ---
                WorkerDailyWageSection(
                    isDailyWageEnabled = isDailyWageEnabled,
                    onDailyWageEnabledChange = { isDailyWageEnabled = it },
                    baseDailyWage = dailyWage,
                    onBaseDailyWageChange = { dailyWage = it },
                    dailyWageError = dailyWageError
                )

                Spacer(modifier = Modifier.height(8.dp))

                // --- SECTION: Hourly Wage (فعال‌سازی با یک تیک‌باکس) ---
                val parsedAttHourlyRate = Formatters.parsePrice(hourlyWage)
                val parsedAttHourlyHours = Formatters.parseDouble(regularHours)
                val attHourlyTotal = if (
                    isHourly &&
                    hourlyWageError == null &&
                    regularHoursError == null &&
                    parsedAttHourlyRate > 0 &&
                    parsedAttHourlyHours > 0
                ) {
                    runCatching {
                        Math.round(parsedAttHourlyRate * parsedAttHourlyHours)
                    }.getOrDefault(0L)
                } else 0L

                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (isHourly) CyanAccent.copy(alpha = 0.08f) else Slate100,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHourly = !isHourly },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isHourly,
                                    onCheckedChange = { isHourly = it },
                                    colors = CheckboxDefaults.colors(checkedColor = CyanAccent),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "دستمزد ساعتی",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isHourly) CyanAccent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isHourly && hourlyWageError == null && regularHoursError == null && (hourlyWage.isNotBlank() || regularHours.isNotBlank())) {
                                Text(
                                    text = "جمع: ${Formatters.formatCurrency(attHourlyTotal)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                        }

                        // وقتی تیک باکس زده میشه دو کادر باز بشه و مبلغ و تعداد ساعت نوشته بشه
                        AnimatedVisibility(visible = isHourly) {
                            Column {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // کادر اول: مبلغ هر ساعت
                                    OutlinedTextField(
                                        value = hourlyWage,
                                        onValueChange = { hourlyWage = Formatters.formatPriceInput(it) },
                                        label = { Text("مبلغ هر ساعت (تومان)", fontSize = 11.sp) },
                                        placeholder = { Text("۱۵۰,۰۰۰ یا ۰", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        isError = hourlyWageError != null,
                                        supportingText = if (hourlyWageError != null) {
                                            {
                                                Text(
                                                    text = hourlyWageError,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .testTag("attendance_hourly_rate_input"),
                                        singleLine = true
                                    )
                                    // کادر دوم: تعداد ساعت کار
                                    OutlinedTextField(
                                        value = regularHours,
                                        onValueChange = {
                                            regularHours = Formatters.toEnglishDigits(it).filter { ch -> ch.isDigit() || ch == '.' }
                                        },
                                        label = { Text("تعداد ساعت کار", fontSize = 11.sp) },
                                        placeholder = { Text("۸ یا ۰", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        isError = regularHoursError != null,
                                        supportingText = if (regularHoursError != null) {
                                            {
                                                Text(
                                                    text = regularHoursError,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .testTag("attendance_hourly_hours_input"),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Workplace Name
                OutlinedTextField(
                    value = workplaceName,
                    onValueChange = { workplaceName = it },
                    label = { Text("نام محل کار / پروژه") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyanAccent) },
                    isError = workplaceNameError != null,
                    supportingText = if (workplaceNameError != null) {
                        {
                            Text(
                                text = workplaceNameError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.5.sp
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Employer Name & Foreman Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = employerName,
                        onValueChange = { employerName = it },
                        label = { Text("نام کارفرما") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AmberAccent) },
                        isError = employerNameError != null,
                        supportingText = if (employerNameError != null) {
                            {
                                Text(
                                    text = employerNameError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp
                                )
                            }
                        } else null,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = foremanName,
                        onValueChange = { foremanName = it },
                        label = { Text("نام سرکارگر") },
                        leadingIcon = { Icon(Icons.Default.Engineering, contentDescription = null, tint = EmeraldAccent) },
                        isError = foremanNameError != null,
                        supportingText = if (foremanNameError != null) {
                            {
                                Text(
                                    text = foremanNameError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp
                                )
                            }
                        } else null,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("توضیحات و یادداشت") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Submit
                Button(
                    onClick = {
                        val worker = selectedWorker
                        val err = JalaliCalendar.validateDateError(date)
                        if (err != null) {
                            dateError = err
                        } else if (worker != null && isFormValid) {
                            val (jy, jm, jd) = JalaliCalendar.parseDate(date)
                            val normalizedDate = JalaliCalendar.formatDate(jy, jm, jd)
                            val parsedRegHours = Formatters.parseDouble(regularHours)
                            val finalRegHours = if (regularHours.isNotBlank()) parsedRegHours else if (isHourly) 0.0 else 8.0
                            val parsedHourlyRateVal = if (isHourly) Formatters.parsePrice(hourlyWage) else 0L

                            val entity = AttendanceEntity(
                                folderId = folder?.id ?: 1L,
                                workerId = worker.id,
                                date = normalizedDate,
                                epochDay = JalaliCalendar.toEpochDay(normalizedDate),
                                status = if (isHourly && !isDailyWageEnabled) AttendanceStatus.HOURLY else if (finalRegHours == 4.0) AttendanceStatus.HALF_DAY else AttendanceStatus.FULL_DAY,
                                entryTime = entryTime.trim(),
                                exitTime = exitTime.trim(),
                                regularHours = if (isHourly && !isDailyWageEnabled) 0.0 else finalRegHours,
                                overtimeHours = if (hasOvertime) Formatters.parseDouble(overtimeHours) else 0.0,
                                overtimeRate = if (hasOvertime) worker.overtimeRate else 0L,
                                dailyWage = if (isDailyWageEnabled) Formatters.parsePrice(dailyWage) else 0L,
                                hourlyWage = parsedHourlyRateVal,
                                hourlyWageRate = parsedHourlyRateVal,
                                hourlyHours = if (isHourly) finalRegHours else 0.0,
                                transitAllowance = worker.transitAllowance,
                                transitImpact = worker.transitImpact,
                                foodAllowance = worker.foodAllowance,
                                foodImpact = worker.foodImpact,
                                accommodationAllowance = worker.accommodationAllowance,
                                accommodationImpact = worker.accommodationImpact,
                                medicalAllowance = worker.medicalAllowance,
                                medicalImpact = worker.medicalImpact,
                                hasCustomAllowances = true,
                                workplaceName = workplaceName.trim(),
                                employerName = employerName.trim(),
                                foremanName = foremanName.trim(),
                                notes = notes.trim()
                            )
                            onConfirm(entity)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_attendance_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                    enabled = isFormValid
                ) {
                    Text(
                        text = "ثبت نهایی ورود و خروج",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

