package com.karyar.app.ui.screens.workers.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.util.Formatters

@Composable
fun WorkerDailyWageSection(
    isDailyWageEnabled: Boolean,
    onDailyWageEnabledChange: (Boolean) -> Unit,
    baseDailyWage: String,
    onBaseDailyWageChange: (String) -> Unit,
    dailyWageError: String? = if (isDailyWageEnabled) {
        Formatters.validateWageInput(baseDailyWage, fieldLabel = "دستمزد روزانه")
    } else null,
    modifier: Modifier = Modifier
) {
    val parsedDailyWageVal = Formatters.parsePrice(baseDailyWage)

    HairlineCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = if (isDailyWageEnabled) AmberAccent.copy(alpha = 0.08f) else Slate100,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDailyWageEnabledChange(!isDailyWageEnabled) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDailyWageEnabled,
                        onCheckedChange = onDailyWageEnabledChange,
                        colors = CheckboxDefaults.colors(checkedColor = AmberAccent),
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("daily_wage_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "دستمزد روزانه ثابت",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isDailyWageEnabled) AmberAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isDailyWageEnabled && baseDailyWage.isNotBlank() && dailyWageError == null) {
                    Text(
                        text = Formatters.formatCurrency(parsedDailyWageVal),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                }
            }

            AnimatedVisibility(visible = isDailyWageEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = baseDailyWage,
                        onValueChange = { onBaseDailyWageChange(Formatters.formatPriceInput(it)) },
                        label = { Text("دستمزد روزانه ثابت (تومان)", fontSize = 11.sp) },
                        placeholder = { Text("مثلاً ۱,۲۰۰,۰۰۰ یا ۰", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = dailyWageError != null,
                        supportingText = if (dailyWageError != null) {
                            {
                                Text(
                                    text = dailyWageError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.5.sp
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_wage_input"),
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
fun WorkerHourlyWageSection(
    isHourlyEnabled: Boolean,
    onHourlyEnabledChange: (Boolean) -> Unit,
    hourlyWageRate: String,
    onHourlyWageRateChange: (String) -> Unit,
    hourlyHours: String,
    onHourlyHoursChange: (String) -> Unit,
    hourlyRateError: String? = if (isHourlyEnabled) {
        Formatters.validateWageInput(hourlyWageRate, fieldLabel = "نرخ دستمزد ساعتی")
    } else null,
    hourlyHoursError: String? = if (isHourlyEnabled) {
        Formatters.validateHoursInput(hourlyHours, fieldLabel = "ساعت کار ساعتی", maxHours = Formatters.MAX_HOURLY_HOURS)
    } else null,
    modifier: Modifier = Modifier
) {
    val parsedHourlyRateVal = Formatters.parsePrice(hourlyWageRate)
    val parsedHourlyHoursVal = Formatters.parseDouble(hourlyHours)
    val hourlyTotal = if (
        isHourlyEnabled &&
        hourlyRateError == null &&
        hourlyHoursError == null &&
        parsedHourlyRateVal > 0 &&
        parsedHourlyHoursVal > 0
    ) {
        runCatching {
            Math.round(parsedHourlyRateVal * parsedHourlyHoursVal)
        }.getOrDefault(0L)
    } else 0L

    HairlineCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = if (isHourlyEnabled) CyanAccent.copy(alpha = 0.08f) else Slate100,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHourlyEnabledChange(!isHourlyEnabled) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isHourlyEnabled,
                        onCheckedChange = onHourlyEnabledChange,
                        colors = CheckboxDefaults.colors(checkedColor = CyanAccent),
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("hourly_wage_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "دستمزد ساعتی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isHourlyEnabled) CyanAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isHourlyEnabled && hourlyRateError == null && hourlyHoursError == null && (hourlyWageRate.isNotBlank() || hourlyHours.isNotBlank())) {
                    Text(
                        text = "جمع: ${Formatters.formatCurrency(hourlyTotal)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                }
            }

            AnimatedVisibility(visible = isHourlyEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hourlyWageRate,
                            onValueChange = { onHourlyWageRateChange(Formatters.formatPriceInput(it)) },
                            label = { Text("مبلغ هر ساعت (تومان)", fontSize = 11.sp) },
                            placeholder = { Text("۱۵۰,۰۰۰ یا ۰", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = hourlyRateError != null,
                            supportingText = if (hourlyRateError != null) {
                                {
                                    Text(
                                        text = hourlyRateError,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp
                                    )
                                }
                            } else null,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("hourly_rate_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = hourlyHours,
                            onValueChange = { input ->
                                val normalized = Formatters.toEnglishDigits(input)
                                    .replace('/', '.')
                                    .replace('٫', '.')
                                    .replace(',', '.')
                                onHourlyHoursChange(normalized.filter { ch -> ch.isDigit() || ch == '.' })
                            },
                            label = { Text("تعداد ساعت کار", fontSize = 11.sp) },
                            placeholder = { Text("۸ یا ۰", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = hourlyHoursError != null,
                            supportingText = if (hourlyHoursError != null) {
                                {
                                    Text(
                                        text = hourlyHoursError,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp
                                    )
                                }
                            } else null,
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("hourly_hours_input"),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerOvertimeSection(
    isOvertimeEnabled: Boolean,
    onOvertimeEnabledChange: (Boolean) -> Unit,
    overtimeRate: String,
    onOvertimeRateChange: (String) -> Unit,
    overtimeHours: String,
    onOvertimeHoursChange: (String) -> Unit,
    overtimeRateError: String? = if (isOvertimeEnabled) {
        Formatters.validateWageInput(overtimeRate, fieldLabel = "نرخ اضافه‌کاری")
    } else null,
    overtimeHoursError: String? = if (isOvertimeEnabled) {
        Formatters.validateHoursInput(overtimeHours, fieldLabel = "ساعت اضافه‌کاری", maxHours = Formatters.MAX_OVERTIME_HOURS)
    } else null,
    modifier: Modifier = Modifier
) {
    val parsedOtRateVal = Formatters.parsePrice(overtimeRate)
    val parsedOtHoursVal = Formatters.parseDouble(overtimeHours)
    val overtimeTotal = if (
        isOvertimeEnabled &&
        overtimeRateError == null &&
        overtimeHoursError == null &&
        parsedOtRateVal > 0 &&
        parsedOtHoursVal > 0
    ) {
        runCatching {
            Math.round(parsedOtRateVal * parsedOtHoursVal)
        }.getOrDefault(0L)
    } else 0L

    HairlineCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = if (isOvertimeEnabled) AmberAccent.copy(alpha = 0.08f) else Slate100,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOvertimeEnabledChange(!isOvertimeEnabled) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isOvertimeEnabled,
                        onCheckedChange = onOvertimeEnabledChange,
                        colors = CheckboxDefaults.colors(checkedColor = AmberAccent),
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("overtime_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "اضافه کاری",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = if (isOvertimeEnabled) AmberAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isOvertimeEnabled && overtimeRateError == null && overtimeHoursError == null && (overtimeRate.isNotBlank() || overtimeHours.isNotBlank())) {
                    Text(
                        text = "جمع: ${Formatters.formatCurrency(overtimeTotal)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                }
            }

            AnimatedVisibility(visible = isOvertimeEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = overtimeRate,
                            onValueChange = { onOvertimeRateChange(Formatters.formatPriceInput(it)) },
                            label = { Text("نرخ اضافه کار (تومان)", fontSize = 11.sp) },
                            placeholder = { Text("نرخ هر ساعت یا ۰", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = overtimeRateError != null,
                            supportingText = if (overtimeRateError != null) {
                                {
                                    Text(
                                        text = overtimeRateError,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp
                                    )
                                }
                            } else null,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("overtime_rate_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = overtimeHours,
                            onValueChange = { input ->
                                val normalized = Formatters.toEnglishDigits(input)
                                    .replace('/', '.')
                                    .replace('٫', '.')
                                    .replace(',', '.')
                                onOvertimeHoursChange(normalized.filter { ch -> ch.isDigit() || ch == '.' })
                            },
                            label = { Text("ساعت اضافه", fontSize = 11.sp) },
                            placeholder = { Text("۲ یا ۰", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = overtimeHoursError != null,
                            supportingText = if (overtimeHoursError != null) {
                                {
                                    Text(
                                        text = overtimeHoursError,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp
                                    )
                                }
                            } else null,
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("overtime_hours_input"),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

