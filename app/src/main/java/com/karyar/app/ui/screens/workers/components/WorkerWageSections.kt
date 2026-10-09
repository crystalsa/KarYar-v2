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
fun WorkerHourlyWageSection(
    isHourlyEnabled: Boolean,
    onHourlyEnabledChange: (Boolean) -> Unit,
    hourlyWageRate: String,
    onHourlyWageRateChange: (String) -> Unit,
    hourlyHours: String,
    onHourlyHoursChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedHourlyRateVal = Formatters.parsePrice(hourlyWageRate)
    val parsedHourlyHoursVal = Formatters.parseDouble(hourlyHours)
    val hourlyTotal = if (isHourlyEnabled && parsedHourlyRateVal > 0 && parsedHourlyHoursVal > 0) {
        (parsedHourlyRateVal * parsedHourlyHoursVal).toLong()
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
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "دستمزد ساعتی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isHourlyEnabled) CyanAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isHourlyEnabled && hourlyTotal > 0) {
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
                            placeholder = { Text("۱۵۰,۰۰۰", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = hourlyHours,
                            onValueChange = { input ->
                                onHourlyHoursChange(Formatters.toEnglishDigits(input).filter { ch -> ch.isDigit() || ch == '.' })
                            },
                            label = { Text("تعداد ساعت کار", fontSize = 11.sp) },
                            placeholder = { Text("۸", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.8f),
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
    modifier: Modifier = Modifier
) {
    val parsedOtRateVal = Formatters.parsePrice(overtimeRate)
    val parsedOtHoursVal = Formatters.parseDouble(overtimeHours)
    val overtimeTotal = if (isOvertimeEnabled && parsedOtRateVal > 0 && parsedOtHoursVal > 0) {
        (parsedOtRateVal * parsedOtHoursVal).toLong()
    } else 0L

    HairlineCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = Slate100,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isOvertimeEnabled,
                        onCheckedChange = onOvertimeEnabledChange,
                        colors = CheckboxDefaults.colors(checkedColor = AmberAccent),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "اضافه کاری",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (overtimeTotal > 0) {
                    Text(
                        text = "جمع: ${Formatters.formatCurrency(overtimeTotal)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                }
            }

            if (isOvertimeEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = overtimeRate,
                        onValueChange = { onOvertimeRateChange(Formatters.formatPriceInput(it)) },
                        label = { Text("نرخ اضافه کار (تومان)", fontSize = 11.sp) },
                        placeholder = { Text("نرخ هر ساعت", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = overtimeHours,
                        onValueChange = { input ->
                            onOvertimeHoursChange(Formatters.toEnglishDigits(input).filter { ch -> ch.isDigit() || ch == '.' })
                        },
                        label = { Text("ساعت اضافه", fontSize = 11.sp) },
                        placeholder = { Text("۲", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }
            }
        }
    }
}
