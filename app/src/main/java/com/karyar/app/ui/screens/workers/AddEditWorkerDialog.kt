package com.karyar.app.ui.screens.workers

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.util.WageCalculator
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.LoadingOutlinedButton
import com.karyar.app.ui.screens.workers.components.OptionalWorkerFieldRow
import com.karyar.app.ui.screens.workers.components.WorkerDateBadgeCard
import com.karyar.app.ui.screens.workers.components.WorkerExpensesSection
import com.karyar.app.ui.screens.workers.components.WorkerHourlyWageSection
import com.karyar.app.ui.screens.workers.components.WorkerOvertimeSection
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.IndigoAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.ui.theme.Slate300
import com.karyar.app.ui.theme.Slate500
import com.karyar.app.util.Formatters
import com.karyar.app.util.JalaliCalendar

private val PRESET_ROLES = listOf(
    "کارگر ساده", "بنا", "گچ‌کار", "جوشکار", "برق‌کار", "لوله‌کش", "آرماتوربند", "قالب‌بند", "نقاش", "سرکارگر"
)

private val COLOR_OPTIONS = listOf(
    0xFFD97706L, // Amber
    0xFF0284C7L, // Blue
    0xFF059669L, // Emerald
    0xFFE11D48L, // Rose
    0xFF7C3AEDL  // Violet
)

@Composable
fun AddEditWorkerDialog(
    initialWorker: WorkerEntity? = null,
    initialAttendance: AttendanceEntity? = null,
    initialDate: String? = null,
    initialDayOfWeek: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (WorkerEntity) -> Unit,
    onConfirmAttendance: ((AttendanceEntity) -> Unit)? = null
) {
    var workDate by remember {
        mutableStateOf(
            if (!initialDate.isNullOrBlank()) initialDate
            else JalaliCalendar.todayString()
        )
    }
    var dayOfWeek by remember {
        mutableStateOf(
            if (!initialDayOfWeek.isNullOrBlank()) initialDayOfWeek
            else JalaliCalendar.getDayOfWeek(workDate)
        )
    }

    var name by remember { mutableStateOf(initialWorker?.name ?: "") }
    var role by remember { mutableStateOf(initialWorker?.role ?: "کارگر ساده") }
    var isPhoneEnabled by remember {
        mutableStateOf(initialWorker?.phone?.isNotBlank() == true)
    }
    var phone by remember { mutableStateOf(initialWorker?.phone ?: "") }

    var isNationalIdEnabled by remember {
        mutableStateOf(initialWorker?.nationalId?.isNotBlank() == true)
    }
    var nationalId by remember { mutableStateOf(initialWorker?.nationalId ?: "") }

    val initWage = if (initialAttendance != null && initialAttendance.dailyWage > 0L) {
        initialAttendance.dailyWage
    } else if (initialWorker != null && initialWorker.baseDailyWage > 0L) {
        initialWorker.baseDailyWage
    } else 0L

    var baseDailyWage by remember {
        mutableStateOf(if (initWage > 0L) Formatters.formatThousandsPersian(initWage) else "")
    }

    // Hourly Wage State
    val initHourlyHours = if (initialAttendance != null && initialAttendance.hourlyHours > 0.0) {
        initialAttendance.hourlyHours
    } else if (initialWorker != null && initialWorker.hourlyHours > 0.0) {
        initialWorker.hourlyHours
    } else 0.0

    val initHourlyRate = if (initialAttendance != null && initialAttendance.hourlyWageRate > 0L) {
        initialAttendance.hourlyWageRate
    } else if (initialWorker != null && (initialWorker.hourlyWageRate > 0L || initialWorker.baseHourlyWage > 0L)) {
        if (initialWorker.hourlyWageRate > 0L) initialWorker.hourlyWageRate else initialWorker.baseHourlyWage
    } else 0L

    var isHourlyEnabled by remember {
        mutableStateOf(
            (initialAttendance != null && initialAttendance.status == AttendanceStatus.HOURLY) ||
            initialWorker?.isHourlyEnabled == true ||
            initHourlyHours > 0.0 ||
            initHourlyRate > 0L
        )
    }
    var hourlyWageRate by remember {
        mutableStateOf(if (initHourlyRate > 0L) Formatters.formatThousandsPersian(initHourlyRate) else "")
    }
    var hourlyHours by remember {
        mutableStateOf(if (initHourlyHours > 0.0) initHourlyHours.toString().removeSuffix(".0") else "")
    }

    // Overtime State
    val initOtHours = if (initialAttendance != null && initialAttendance.overtimeHours > 0.0) {
        initialAttendance.overtimeHours
    } else if (initialWorker != null && initialWorker.overtimeHours > 0.0) {
        initialWorker.overtimeHours
    } else 0.0

    val initOtRate = if (initialAttendance != null && initialAttendance.overtimeRate > 0L) {
        initialAttendance.overtimeRate
    } else if (initialWorker != null && initialWorker.overtimeRate > 0L) {
        initialWorker.overtimeRate
    } else 0L

    var isOvertimeEnabled by remember {
        mutableStateOf(
            initialWorker?.isOvertimeEnabled == true ||
            initOtHours > 0.0 ||
            initOtRate > 0L
        )
    }
    var overtimeRate by remember {
        mutableStateOf(if (initOtRate > 0L) Formatters.formatThousandsPersian(initOtRate) else "")
    }
    var overtimeHours by remember {
        mutableStateOf(if (initOtHours > 0.0) initOtHours.toString().removeSuffix(".0") else "")
    }

    var isActive by remember { mutableStateOf(initialWorker?.isActive ?: true) }
    var notes by remember {
        mutableStateOf(
            if (initialAttendance != null && initialAttendance.notes.isNotBlank()) initialAttendance.notes
            else (initialWorker?.notes ?: "")
        )
    }
    var selectedColor by remember { mutableStateOf(initialWorker?.colorTag ?: 0xFFD97706L) }

    // Allowances & Deductions: read from attendance if customized for this day, otherwise fallback to worker
    val useAttAllowances = initialAttendance != null && initialAttendance.hasCustomAllowances

    val initTransit = if (useAttAllowances) initialAttendance.transitAllowance else (initialWorker?.transitAllowance ?: 0L)
    val initTransitImpact = if (useAttAllowances) initialAttendance.transitImpact else (initialWorker?.transitImpact ?: "ALLOWANCE")
    var transitEnabled by remember { mutableStateOf(initTransit > 0L) }
    var transitAllowance by remember {
        mutableStateOf(if (initTransit > 0L) Formatters.formatThousandsPersian(initTransit) else "")
    }
    var transitImpact by remember { mutableStateOf(initTransitImpact) }

    val initFood = if (useAttAllowances) initialAttendance.foodAllowance else (initialWorker?.foodAllowance ?: 0L)
    val initFoodImpact = if (useAttAllowances) initialAttendance.foodImpact else (initialWorker?.foodImpact ?: "ALLOWANCE")
    var foodEnabled by remember { mutableStateOf(initFood > 0L) }
    var foodAllowance by remember {
        mutableStateOf(if (initFood > 0L) Formatters.formatThousandsPersian(initFood) else "")
    }
    var foodImpact by remember { mutableStateOf(initFoodImpact) }

    val initAccom = if (useAttAllowances) initialAttendance.accommodationAllowance else (initialWorker?.accommodationAllowance ?: 0L)
    val initAccomImpact = if (useAttAllowances) initialAttendance.accommodationImpact else (initialWorker?.accommodationImpact ?: "ALLOWANCE")
    var accommodationEnabled by remember { mutableStateOf(initAccom > 0L) }
    var accommodationAllowance by remember {
        mutableStateOf(if (initAccom > 0L) Formatters.formatThousandsPersian(initAccom) else "")
    }
    var accommodationImpact by remember { mutableStateOf(initAccomImpact) }

    val initMed = if (useAttAllowances) initialAttendance.medicalAllowance else (initialWorker?.medicalAllowance ?: 0L)
    val initMedImpact = if (useAttAllowances) initialAttendance.medicalImpact else (initialWorker?.medicalImpact ?: "ALLOWANCE")
    var medicalEnabled by remember { mutableStateOf(initMed > 0L) }
    var medicalAllowance by remember {
        mutableStateOf(if (initMed > 0L) Formatters.formatThousandsPersian(initMed) else "")
    }
    var medicalImpact by remember { mutableStateOf(initMedImpact) }

    val initialDailyWage = remember { if (initWage > 0L) Formatters.formatThousandsPersian(initWage) else "" }
    val initialHourlyRate = remember { if (initHourlyRate > 0L) Formatters.formatThousandsPersian(initHourlyRate) else "" }
    val initialHourlyHours = remember { if (initHourlyHours > 0.0) initHourlyHours.toString().removeSuffix(".0") else "" }
    val initialOvertimeRate = remember { if (initOtRate > 0L) Formatters.formatThousandsPersian(initOtRate) else "" }
    val initialOvertimeHours = remember { if (initOtHours > 0.0) initOtHours.toString().removeSuffix(".0") else "" }

    val isModified = (name != (initialWorker?.name ?: "")) ||
            (role != (initialWorker?.role ?: "کارگر ساده")) ||
            (isPhoneEnabled != (initialWorker?.phone?.isNotBlank() == true)) ||
            (phone != (initialWorker?.phone ?: "")) ||
            (isNationalIdEnabled != (initialWorker?.nationalId?.isNotBlank() == true)) ||
            (nationalId != (initialWorker?.nationalId ?: "")) ||
            (baseDailyWage != initialDailyWage) ||
            (isHourlyEnabled != (initialWorker?.isHourlyEnabled == true)) ||
            (hourlyWageRate != initialHourlyRate) ||
            (hourlyHours != initialHourlyHours) ||
            (isOvertimeEnabled != (initialWorker?.isOvertimeEnabled == true)) ||
            (overtimeRate != initialOvertimeRate) ||
            (overtimeHours != initialOvertimeHours) ||
            (notes != (initialWorker?.notes ?: "")) ||
            (transitEnabled != (initialWorker != null && initialWorker.transitAllowance > 0)) ||
            (foodEnabled != (initialWorker != null && initialWorker.foodAllowance > 0)) ||
            (accommodationEnabled != (initialWorker != null && initialWorker.accommodationAllowance > 0)) ||
            (medicalEnabled != (initialWorker != null && initialWorker.medicalAllowance > 0))

    var showUnsavedAlert by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    fun handleRequestClose() {
        if (isModified) {
            showUnsavedAlert = true
        } else {
            onDismiss()
        }
    }

    fun saveWorker() {
        if (name.isNotBlank()) {
            isSaving = true
            val parsedHourlyRate = if (isHourlyEnabled) Formatters.parsePrice(hourlyWageRate) else 0L
            val parsedHourlyHours = if (isHourlyEnabled) Formatters.parseDouble(hourlyHours) else 0.0
            val parsedOvertimeRate = if (isOvertimeEnabled) Formatters.parsePrice(overtimeRate) else 0L
            val parsedOvertimeHours = if (isOvertimeEnabled) Formatters.parseDouble(overtimeHours) else 0.0

            val finalPhone = if (isPhoneEnabled) phone.trim() else ""
            val finalNationalId = if (isNationalIdEnabled) nationalId.trim() else ""

            val entity = (initialWorker ?: WorkerEntity(name = name, role = role)).copy(
                name = name.trim(),
                role = role.trim(),
                phone = finalPhone,
                nationalId = finalNationalId,
                baseDailyWage = if (isHourlyEnabled) 0L else Formatters.parsePrice(baseDailyWage),
                baseHourlyWage = if (isHourlyEnabled) parsedHourlyRate else 0L,
                isHourlyEnabled = isHourlyEnabled,
                hourlyWageRate = if (isHourlyEnabled) parsedHourlyRate else 0L,
                hourlyHours = if (isHourlyEnabled) parsedHourlyHours else 0.0,
                isOvertimeEnabled = isOvertimeEnabled,
                overtimeRate = parsedOvertimeRate,
                overtimeHours = parsedOvertimeHours,
                isActive = isActive,
                notes = notes.trim(),
                colorTag = selectedColor,
                transitAllowance = if (transitEnabled) Formatters.parsePrice(transitAllowance) else 0L,
                transitImpact = transitImpact,
                foodAllowance = if (foodEnabled) Formatters.parsePrice(foodAllowance) else 0L,
                foodImpact = foodImpact,
                accommodationAllowance = if (accommodationEnabled) Formatters.parsePrice(accommodationAllowance) else 0L,
                accommodationImpact = accommodationImpact,
                medicalAllowance = if (medicalEnabled) Formatters.parsePrice(medicalAllowance) else 0L,
                medicalImpact = medicalImpact
            )
            if (onConfirmAttendance != null) {
                val parsedDailyWage = if (isHourlyEnabled) 0L else Formatters.parsePrice(baseDailyWage)
                val baseAtt = initialAttendance ?: WageCalculator.defaultAttendanceFor(
                    worker = entity,
                    date = workDate,
                    notes = notes.trim()
                )
                val status = when {
                    isHourlyEnabled -> AttendanceStatus.HOURLY
                    baseAtt.status == AttendanceStatus.ABSENT -> AttendanceStatus.ABSENT
                    baseAtt.status == AttendanceStatus.HALF_DAY -> AttendanceStatus.HALF_DAY
                    else -> AttendanceStatus.FULL_DAY
                }
                val regHours = when (status) {
                    AttendanceStatus.FULL_DAY -> 8.0
                    AttendanceStatus.HALF_DAY -> 4.0
                    else -> 0.0
                }
                val hHours = when (status) {
                    AttendanceStatus.HOURLY -> parsedHourlyHours
                    else -> 0.0
                }
                val otHours = when (status) {
                    AttendanceStatus.ABSENT -> 0.0
                    else -> parsedOvertimeHours
                }
                val updatedAtt = baseAtt.copy(
                    status = status,
                    regularHours = regHours,
                    dailyWage = parsedDailyWage,
                    hourlyHours = hHours,
                    hourlyWageRate = parsedHourlyRate,
                    hourlyWage = parsedHourlyRate,
                    overtimeHours = otHours,
                    overtimeRate = parsedOvertimeRate,
                    transitAllowance = if (transitEnabled) Formatters.parsePrice(transitAllowance) else 0L,
                    transitImpact = transitImpact,
                    foodAllowance = if (foodEnabled) Formatters.parsePrice(foodAllowance) else 0L,
                    foodImpact = foodImpact,
                    accommodationAllowance = if (accommodationEnabled) Formatters.parsePrice(accommodationAllowance) else 0L,
                    accommodationImpact = accommodationImpact,
                    medicalAllowance = if (medicalEnabled) Formatters.parsePrice(medicalAllowance) else 0L,
                    medicalImpact = medicalImpact,
                    hasCustomAllowances = true,
                    notes = notes.trim()
                )
                onConfirmAttendance(updatedAtt)
                if (initialWorker != null) {
                    val updatedProfile = initialWorker.copy(
                        name = name.trim(),
                        role = role.trim(),
                        phone = finalPhone,
                        nationalId = finalNationalId,
                        isActive = isActive,
                        colorTag = selectedColor,
                        notes = notes.trim()
                    )
                    onConfirm(updatedProfile)
                } else {
                    onConfirm(entity)
                }
            } else {
                onConfirm(entity)
            }
        }
    }

    BackHandler(enabled = true) {
        handleRequestClose()
    }

    Dialog(
        onDismissRequest = { handleRequestClose() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Android-Iconics styled container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = if (initialWorker == null) Icons.Default.PersonAdd else Icons.Default.Engineering,
                            color = AmberAccent,
                            size = IconicsSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (initialWorker == null) "افزودن کارگر" else "ویرایش کارگر",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { handleRequestClose() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Date & Day Display Card
                WorkerDateBadgeCard(workDate = workDate, dayOfWeek = dayOfWeek)

                Spacer(modifier = Modifier.height(8.dp))

                // Name & Role compactly stacked
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام کارگر", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Role Presets Chips - compact scrollable row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PRESET_ROLES.forEach { preset ->
                        val isSelected = role == preset
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) AmberAccent else Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { role = preset }
                        ) {
                            Text(
                                text = preset,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Custom Role Text Field
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("عنوان شغل", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Engineering, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 1. Phone Section
                OptionalWorkerFieldRow(
                    title = "شماره تماس",
                    enabled = isPhoneEnabled,
                    onEnabledChange = { isPhoneEnabled = it },
                    value = phone,
                    onValueChange = { input ->
                        val digitsOnly = Formatters.toEnglishDigits(input).filter { it.isDigit() }.take(11)
                        phone = digitsOnly
                    },
                    icon = Icons.Default.Phone,
                    accentColor = EmeraldAccent,
                    placeholder = "09123456789",
                    testTag = "worker_phone_input"
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. National ID Section
                OptionalWorkerFieldRow(
                    title = "کد ملی",
                    enabled = isNationalIdEnabled,
                    onEnabledChange = { isNationalIdEnabled = it },
                    value = nationalId,
                    onValueChange = { input ->
                        val digitsOnly = Formatters.toEnglishDigits(input).filter { it.isDigit() }.take(10)
                        nationalId = digitsOnly
                    },
                    icon = Icons.Default.Badge,
                    accentColor = IndigoAccent,
                    placeholder = "0012345678",
                    testTag = "worker_national_id_input"
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Base Daily Wage
                OutlinedTextField(
                    value = baseDailyWage,
                    onValueChange = { baseDailyWage = Formatters.formatPriceInput(it) },
                    enabled = !isHourlyEnabled,
                    label = {
                        Text(
                            text = if (isHourlyEnabled) "دستمزد روزانه (خاموش شده)" else "دستمزد روزانه ثابت (تومان)",
                            fontSize = 12.sp,
                            color = if (isHourlyEnabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else AmberAccent
                        )
                    },
                    placeholder = {
                        Text(
                            text = if (isHourlyEnabled) "خاموش (دستمزد ساعتی فعال است)" else "مثلاً ۱,۲۰۰,۰۰۰",
                            fontSize = 11.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = Slate100.copy(alpha = 0.6f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        disabledBorderColor = Slate200.copy(alpha = 0.7f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Hourly Wage Section
                WorkerHourlyWageSection(
                    isHourlyEnabled = isHourlyEnabled,
                    onHourlyEnabledChange = { isHourlyEnabled = it },
                    hourlyWageRate = hourlyWageRate,
                    onHourlyWageRateChange = { hourlyWageRate = it },
                    hourlyHours = hourlyHours,
                    onHourlyHoursChange = { hourlyHours = it }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Overtime Section
                WorkerOvertimeSection(
                    isOvertimeEnabled = isOvertimeEnabled,
                    onOvertimeEnabledChange = { isOvertimeEnabled = it },
                    overtimeRate = overtimeRate,
                    onOvertimeRateChange = { overtimeRate = it },
                    overtimeHours = overtimeHours,
                    onOvertimeHoursChange = { overtimeHours = it }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Individual Expenses Section
                WorkerExpensesSection(
                    transitEnabled = transitEnabled,
                    onTransitEnabledChange = { transitEnabled = it },
                    transitAllowance = transitAllowance,
                    onTransitAllowanceChange = { transitAllowance = it },
                    transitImpact = transitImpact,
                    onTransitImpactChange = { transitImpact = it },

                    accommodationEnabled = accommodationEnabled,
                    onAccommodationEnabledChange = { accommodationEnabled = it },
                    accommodationAllowance = accommodationAllowance,
                    onAccommodationAllowanceChange = { accommodationAllowance = it },
                    accommodationImpact = accommodationImpact,
                    onAccommodationImpactChange = { accommodationImpact = it },

                    foodEnabled = foodEnabled,
                    onFoodEnabledChange = { foodEnabled = it },
                    foodAllowance = foodAllowance,
                    onFoodAllowanceChange = { foodAllowance = it },
                    foodImpact = foodImpact,
                    onFoodImpactChange = { foodImpact = it },

                    medicalEnabled = medicalEnabled,
                    onMedicalEnabledChange = { medicalEnabled = it },
                    medicalAllowance = medicalAllowance,
                    onMedicalAllowanceChange = { medicalAllowance = it },
                    medicalImpact = medicalImpact,
                    onMedicalImpactChange = { medicalImpact = it }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Active Switch & Colors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldAccent,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Slate300
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isActive) "کارگر فعال" else "کارگر غیرفعال",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) EmeraldAccent else Slate500
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        COLOR_OPTIONS.forEach { colorVal ->
                            val isSelected = selectedColor == colorVal
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .clickable { selectedColor = colorVal },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingOutlinedButton(
                        text = "انصراف",
                        onClick = { handleRequestClose() },
                        modifier = Modifier.weight(1f),
                        height = 42.dp
                    )
                    LoadingButton(
                        text = if (initialWorker == null) "ثبت" else "ذخیره",
                        icon = if (initialWorker == null) Icons.Default.PersonAdd else Icons.Default.Save,
                        onClick = { saveWorker() },
                        isLoading = isSaving,
                        modifier = Modifier.weight(1.3f),
                        containerColor = AmberAccent,
                        height = 42.dp,
                        testTag = "submit_worker_button"
                    )
                }
            }
        }
    }

    if (showUnsavedAlert) {
        AlertDialog(
            onDismissRequest = { showUnsavedAlert = false },
            title = {
                Text(
                    text = "تغییرات ذخیره‌نشده",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "اطلاعات ذخیره نشده است. خارج می‌شوید؟",
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                LoadingButton(
                    text = "ذخیره",
                    onClick = {
                        showUnsavedAlert = false
                        saveWorker()
                    },
                    containerColor = AmberAccent,
                    height = 38.dp,
                    fontSize = 12.sp
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedAlert = false
                    onDismiss()
                }) {
                    Text("انصراف", color = RoseAccent, fontSize = 12.sp)
                }
            }
        )
    }
}
