package com.karyar.app.ui.screens.attendance.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.karyar.app.util.Formatters
import com.karyar.app.util.JalaliCalendar
import com.karyar.app.util.WageCalculator

@Composable
fun EditDayAttendanceDialog(
    worker: WorkerEntity,
    targetDate: String,
    attendance: AttendanceEntity?,
    onDismiss: () -> Unit,
    onConfirm: (AttendanceEntity) -> Unit
) {
    val initialStatus = attendance?.status ?: if (worker.isHourlyEnabled) AttendanceStatus.HOURLY else AttendanceStatus.FULL_DAY
    var status by remember { mutableStateOf(initialStatus) }

    // Hourly work hours
    val initialHourlyHours = if (attendance != null && attendance.hourlyHours > 0.0) {
        attendance.hourlyHours.toString().removeSuffix(".0")
    } else if (worker.hourlyHours > 0.0) {
        worker.hourlyHours.toString().removeSuffix(".0")
    } else "8"
    var hourlyHoursStr by remember { mutableStateOf(initialHourlyHours) }

    // Overtime hours
    val hasInitOt = (attendance != null && attendance.overtimeHours > 0.0) || worker.isOvertimeEnabled
    var hasOvertime by remember { mutableStateOf(hasInitOt) }

    val initialOtHours = if (attendance != null && attendance.overtimeHours > 0.0) {
        attendance.overtimeHours.toString().removeSuffix(".0")
    } else if (worker.overtimeHours > 0.0) {
        worker.overtimeHours.toString().removeSuffix(".0")
    } else ""
    var overtimeHoursStr by remember { mutableStateOf(initialOtHours) }

    var notes by remember { mutableStateOf(attendance?.notes ?: "") }

    val initWage = if (attendance != null && attendance.dailyWage > 0L) attendance.dailyWage else worker.baseDailyWage
    var dailyWageStr by remember { mutableStateOf(if (initWage > 0L) Formatters.formatThousandsPersian(initWage) else "") }

    val initHourlyRate = if (attendance != null && attendance.hourlyWageRate > 0L) attendance.hourlyWageRate else (if (worker.hourlyWageRate > 0L) worker.hourlyWageRate else worker.baseHourlyWage)
    var hourlyWageRateStr by remember { mutableStateOf(if (initHourlyRate > 0L) Formatters.formatThousandsPersian(initHourlyRate) else "") }

    val initOtRate = if (attendance != null && attendance.overtimeRate > 0L) attendance.overtimeRate else worker.overtimeRate
    var overtimeRateStr by remember { mutableStateOf(if (initOtRate > 0L) Formatters.formatThousandsPersian(initOtRate) else "") }

    // Custom Daily Allowances
    val initTransit = if (attendance != null && attendance.hasCustomAllowances) attendance.transitAllowance else worker.transitAllowance
    val initTransitImpact = if (attendance != null && attendance.hasCustomAllowances) attendance.transitImpact else worker.transitImpact
    var transitAllowanceStr by remember { mutableStateOf(if (initTransit > 0L) Formatters.formatThousandsPersian(initTransit) else "") }
    var transitImpact by remember { mutableStateOf(initTransitImpact) }

    val initFood = if (attendance != null && attendance.hasCustomAllowances) attendance.foodAllowance else worker.foodAllowance
    val initFoodImpact = if (attendance != null && attendance.hasCustomAllowances) attendance.foodImpact else worker.foodImpact
    var foodAllowanceStr by remember { mutableStateOf(if (initFood > 0L) Formatters.formatThousandsPersian(initFood) else "") }
    var foodImpact by remember { mutableStateOf(initFoodImpact) }

    val initAccom = if (attendance != null && attendance.hasCustomAllowances) attendance.accommodationAllowance else worker.accommodationAllowance
    val initAccomImpact = if (attendance != null && attendance.hasCustomAllowances) attendance.accommodationImpact else worker.accommodationImpact
    var accommodationAllowanceStr by remember { mutableStateOf(if (initAccom > 0L) Formatters.formatThousandsPersian(initAccom) else "") }
    var accommodationImpact by remember { mutableStateOf(initAccomImpact) }

    val initMed = if (attendance != null && attendance.hasCustomAllowances) attendance.medicalAllowance else worker.medicalAllowance
    val initMedImpact = if (attendance != null && attendance.hasCustomAllowances) attendance.medicalImpact else worker.medicalImpact
    var medicalAllowanceStr by remember { mutableStateOf(if (initMed > 0L) Formatters.formatThousandsPersian(initMed) else "") }
    var medicalImpact by remember { mutableStateOf(initMedImpact) }

    var isAllowancesExpanded by remember { mutableStateOf(false) }

    val parsedHHours = Formatters.parseDouble(hourlyHoursStr)
    val parsedOtHours = if (hasOvertime) Formatters.parseDouble(overtimeHoursStr) else 0.0

    Dialog(onDismissRequest = onDismiss) {
        HairlineCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Column {
                            Text(
                                text = "کارکرد این روز: ${worker.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تاریخ: ${Formatters.toPersianDigits(targetDate)} (${JalaliCalendar.getDayOfWeek(targetDate)})",
                                fontSize = 11.sp,
                                color = AmberAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // وضعیت حضور در این روز
                Text(
                    text = "نوع کارکرد در این روز:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusChip(
                        label = "تمام روز",
                        selected = status == AttendanceStatus.FULL_DAY,
                        color = EmeraldAccent,
                        onClick = { status = AttendanceStatus.FULL_DAY },
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(
                        label = "نصف روز",
                        selected = status == AttendanceStatus.HALF_DAY,
                        color = AmberAccent,
                        onClick = { status = AttendanceStatus.HALF_DAY },
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(
                        label = "ساعتی",
                        selected = status == AttendanceStatus.HOURLY,
                        color = CyanAccent,
                        onClick = { status = AttendanceStatus.HOURLY },
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(
                        label = "غایب",
                        selected = status == AttendanceStatus.ABSENT,
                        color = RoseAccent,
                        onClick = { status = AttendanceStatus.ABSENT },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // اگر وضعیت ساعتی است: تنظیم دقیق ساعات کارکرد ساعتی این روز
                AnimatedVisibility(visible = status == AttendanceStatus.HOURLY || status == AttendanceStatus.HALF_DAY) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate100,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تعداد ساعت کارکرد این روز:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                                Text(
                                    text = "${Formatters.toPersianDigits(parsedHHours.toString().removeSuffix(".0"))} ساعت",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = hourlyHoursStr,
                                onValueChange = { input ->
                                    val normalized = Formatters.toEnglishDigits(input)
                                        .replace('/', '.')
                                        .replace('٫', '.')
                                        .replace(',', '.')
                                    hourlyHoursStr = normalized.filter { it.isDigit() || it == '.' }
                                },
                                label = { Text("ساعت کارکرد (مثلاً ۵ یا ۵.۵)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // اضافه‌کاری این روز
                if (status != AttendanceStatus.ABSENT) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate100,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { hasOvertime = !hasOvertime },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = hasOvertime,
                                        onCheckedChange = { hasOvertime = it },
                                        colors = CheckboxDefaults.colors(checkedColor = AmberAccent),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ثبت اضافه کاری برای این روز",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasOvertime) AmberAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (hasOvertime && parsedOtHours > 0.0) {
                                    Text(
                                        text = "${Formatters.toPersianDigits(parsedOtHours.toString().removeSuffix(".0"))} ساعت",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberAccent
                                    )
                                }
                            }

                            AnimatedVisibility(visible = hasOvertime) {
                                Column {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = overtimeHoursStr,
                                        onValueChange = { input ->
                                            val normalized = Formatters.toEnglishDigits(input)
                                                .replace('/', '.')
                                                .replace('٫', '.')
                                                .replace(',', '.')
                                            overtimeHoursStr = normalized.filter { it.isDigit() || it == '.' }
                                        },
                                        label = { Text("تعداد ساعت اضافه کار (مثلاً ۲ یا ۰.۵)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                // بخش دستمزد و کمک‌هزینه‌های روزانه (مستقل برای این روز)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isAllowancesExpanded = !isAllowancesExpanded }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تنظیمات دستمزد و کمک‌هزینه‌های این روز",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (isAllowancesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = isAllowancesExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (status == AttendanceStatus.FULL_DAY || status == AttendanceStatus.HALF_DAY) {
                                    OutlinedTextField(
                                        value = dailyWageStr,
                                        onValueChange = { input ->
                                            val digits = Formatters.toEnglishDigits(input).filter { it.isDigit() }
                                            dailyWageStr = if (digits.isNotBlank()) Formatters.formatThousandsPersian(digits.toLong()) else ""
                                        },
                                        label = { Text("دستمزد روزانه این روز (تومان)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                if (status == AttendanceStatus.HOURLY) {
                                    OutlinedTextField(
                                        value = hourlyWageRateStr,
                                        onValueChange = { input ->
                                            val digits = Formatters.toEnglishDigits(input).filter { it.isDigit() }
                                            hourlyWageRateStr = if (digits.isNotBlank()) Formatters.formatThousandsPersian(digits.toLong()) else ""
                                        },
                                        label = { Text("مبلغ هر ساعت کار این روز (تومان)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                if (hasOvertime) {
                                    OutlinedTextField(
                                        value = overtimeRateStr,
                                        onValueChange = { input ->
                                            val digits = Formatters.toEnglishDigits(input).filter { it.isDigit() }
                                            overtimeRateStr = if (digits.isNotBlank()) Formatters.formatThousandsPersian(digits.toLong()) else ""
                                        },
                                        label = { Text("نرخ اضافه کار این روز (تومان)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                // مسکن
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = accommodationAllowanceStr,
                                        onValueChange = { input ->
                                            val digits = Formatters.toEnglishDigits(input).filter { it.isDigit() }
                                            accommodationAllowanceStr = if (digits.isNotBlank()) Formatters.formatThousandsPersian(digits.toLong()) else ""
                                        },
                                        label = { Text("کمک‌هزینه مسکن این روز (تومان)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    StatusChip(
                                        label = if (accommodationImpact == "ALLOWANCE") "افزایشی (+)" else "کاهشی (-)",
                                        selected = true,
                                        color = if (accommodationImpact == "ALLOWANCE") EmeraldAccent else RoseAccent,
                                        onClick = { accommodationImpact = if (accommodationImpact == "ALLOWANCE") "DEDUCTION" else "ALLOWANCE" }
                                    )
                                }

                                // غذا
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = foodAllowanceStr,
                                        onValueChange = { input ->
                                            val digits = Formatters.toEnglishDigits(input).filter { it.isDigit() }
                                            foodAllowanceStr = if (digits.isNotBlank()) Formatters.formatThousandsPersian(digits.toLong()) else ""
                                        },
                                        label = { Text("کمک‌هزینه خوراک این روز (تومان)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    StatusChip(
                                        label = if (foodImpact == "ALLOWANCE") "افزایشی (+)" else "کاهشی (-)",
                                        selected = true,
                                        color = if (foodImpact == "ALLOWANCE") EmeraldAccent else RoseAccent,
                                        onClick = { foodImpact = if (foodImpact == "ALLOWANCE") "DEDUCTION" else "ALLOWANCE" }
                                    )
                                }

                                // ایاب و ذهاب
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = transitAllowanceStr,
                                        onValueChange = { input ->
                                            val digits = Formatters.toEnglishDigits(input).filter { it.isDigit() }
                                            transitAllowanceStr = if (digits.isNotBlank()) Formatters.formatThousandsPersian(digits.toLong()) else ""
                                        },
                                        label = { Text("ایاب و ذهاب این روز (تومان)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    StatusChip(
                                        label = if (transitImpact == "ALLOWANCE") "افزایشی (+)" else "کاهشی (-)",
                                        selected = true,
                                        color = if (transitImpact == "ALLOWANCE") EmeraldAccent else RoseAccent,
                                        onClick = { transitImpact = if (transitImpact == "ALLOWANCE") "DEDUCTION" else "ALLOWANCE" }
                                    )
                                }

                                // درمان
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = medicalAllowanceStr,
                                        onValueChange = { input ->
                                            val digits = Formatters.toEnglishDigits(input).filter { it.isDigit() }
                                            medicalAllowanceStr = if (digits.isNotBlank()) Formatters.formatThousandsPersian(digits.toLong()) else ""
                                        },
                                        label = { Text("کمک‌هزینه درمان این روز (تومان)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    StatusChip(
                                        label = if (medicalImpact == "ALLOWANCE") "افزایشی (+)" else "کاهشی (-)",
                                        selected = true,
                                        color = if (medicalImpact == "ALLOWANCE") EmeraldAccent else RoseAccent,
                                        onClick = { medicalImpact = if (medicalImpact == "ALLOWANCE") "DEDUCTION" else "ALLOWANCE" }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Save & Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingButton(
                        text = "ذخیره کارکرد این روز",
                        onClick = {
                            val baseAtt = attendance ?: WageCalculator.defaultAttendanceFor(
                                worker = worker,
                                status = status,
                                date = targetDate
                            )

                            val regHours = when (status) {
                                AttendanceStatus.FULL_DAY -> 8.0
                                AttendanceStatus.HALF_DAY -> 4.0
                                else -> 0.0
                            }

                            val hHours = when (status) {
                                AttendanceStatus.HOURLY -> parsedHHours
                                else -> 0.0
                            }

                            val otHours = if (status != AttendanceStatus.ABSENT && hasOvertime) parsedOtHours else 0.0

                            val parsedDailyWage = if (dailyWageStr.isNotBlank()) Formatters.parsePrice(dailyWageStr)
                                else when (status) {
                                    AttendanceStatus.FULL_DAY -> worker.baseDailyWage
                                    AttendanceStatus.HALF_DAY -> WageCalculator.roundToLong(worker.baseDailyWage / 2.0)
                                    else -> 0L
                                }
                            val parsedHourlyRate = if (hourlyWageRateStr.isNotBlank()) Formatters.parsePrice(hourlyWageRateStr)
                                else if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage
                            val parsedOtRate = if (overtimeRateStr.isNotBlank()) Formatters.parsePrice(overtimeRateStr)
                                else if (worker.overtimeRate > 0) worker.overtimeRate else 0L

                            val updated = baseAtt.copy(
                                status = status,
                                regularHours = regHours,
                                hourlyHours = hHours,
                                overtimeHours = otHours,
                                dailyWage = parsedDailyWage,
                                hourlyWageRate = parsedHourlyRate,
                                hourlyWage = parsedHourlyRate,
                                overtimeRate = parsedOtRate,
                                transitAllowance = if (transitAllowanceStr.isNotBlank()) Formatters.parsePrice(transitAllowanceStr) else 0L,
                                transitImpact = transitImpact,
                                foodAllowance = if (foodAllowanceStr.isNotBlank()) Formatters.parsePrice(foodAllowanceStr) else 0L,
                                foodImpact = foodImpact,
                                accommodationAllowance = if (accommodationAllowanceStr.isNotBlank()) Formatters.parsePrice(accommodationAllowanceStr) else 0L,
                                accommodationImpact = accommodationImpact,
                                medicalAllowance = if (medicalAllowanceStr.isNotBlank()) Formatters.parsePrice(medicalAllowanceStr) else 0L,
                                medicalImpact = medicalImpact,
                                hasCustomAllowances = true,
                                notes = notes.trim()
                            )
                            onConfirm(updated)
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = EmeraldAccent,
                        height = 40.dp,
                        fontSize = 12.sp,
                        testTag = "save_day_attendance_button"
                    )

                    LoadingOutlinedButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        borderColor = Slate200,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        height = 40.dp,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color else Slate100,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
