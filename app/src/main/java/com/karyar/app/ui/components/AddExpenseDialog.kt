package com.karyar.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.IndigoAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.util.Formatters
import com.karyar.app.util.JalaliCalendar
import com.karyar.app.util.WageCalculator

data class ExpenseCategoryOption(
    val code: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val presetTitles: List<String>
)

val EXPENSE_CATEGORY_OPTIONS = listOf(
    ExpenseCategoryOption(
        code = "TRANSIT",
        label = "ایاب و ذهاب",
        icon = Icons.Default.DirectionsBus,
        color = CyanAccent,
        presetTitles = listOf("کرایه سرویس رفت و برگشت", "اسنپ / آژانس", "کمک‌هزینه ایاب و ذهاب")
    ),
    ExpenseCategoryOption(
        code = "FOOD",
        label = "خوراک",
        icon = Icons.Default.LocalDining,
        color = AmberAccent,
        presetTitles = listOf("ناهار کارگاه", "صبحانه و عصرانه", "کمک‌هزینه خوراک")
    ),
    ExpenseCategoryOption(
        code = "ACCOMMODATION",
        label = "اسکان",
        icon = Icons.Default.Home,
        color = IndigoAccent,
        presetTitles = listOf("اجاره اسکان کارگران", "حق مسکن", "هزینه خوابگاه")
    ),
    ExpenseCategoryOption(
        code = "MEDICAL",
        label = "درمان",
        icon = Icons.Default.MedicalServices,
        color = RoseAccent,
        presetTitles = listOf("هزینه دارو و درمان", "کمک‌هزینه درمانی", "کمک‌های اولیه کارگاه")
    ),
    ExpenseCategoryOption(
        code = "OTHER",
        label = "سایر",
        icon = Icons.Default.ReceiptLong,
        color = EmeraldAccent,
        presetTitles = listOf("مساعده نقدی", "پاداش کارکرد", "خرید ابزار و ملزومات")
    )
)

fun expenseCategoryLabel(category: String): String =
    EXPENSE_CATEGORY_OPTIONS.firstOrNull { it.code.equals(category, ignoreCase = true) }?.label ?: "سایر"

fun expenseCategoryColor(category: String): Color =
    EXPENSE_CATEGORY_OPTIONS.firstOrNull { it.code.equals(category, ignoreCase = true) }?.color ?: EmeraldAccent

fun expenseCategoryIcon(category: String): ImageVector =
    EXPENSE_CATEGORY_OPTIONS.firstOrNull { it.code.equals(category, ignoreCase = true) }?.icon ?: Icons.Default.ReceiptLong

@Composable
fun AddExpenseDialog(
    workers: List<WorkerEntity>,
    initialDate: String = JalaliCalendar.todayString(),
    onDismiss: () -> Unit,
    onConfirm: (ExpenseEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("TRANSIT") }
    var scope by remember {
        mutableStateOf(if (workers.isNotEmpty()) "INDIVIDUAL" else "GROUP")
    }
    var impactType by remember { mutableStateOf("DEDUCTION") }
    var selectedWorkerId by remember(workers) {
        mutableStateOf(workers.firstOrNull()?.id)
    }
    var amountInput by remember { mutableStateOf("") }
    var date by remember {
        mutableStateOf(initialDate.ifBlank { JalaliCalendar.todayString() })
    }
    var notes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    var titleTouched by remember { mutableStateOf(false) }
    var amountTouched by remember { mutableStateOf(false) }

    val titleError = Formatters.validateRequiredName(title, "عنوان هزینه")
    val amountError = Formatters.validateWageInput(
        input = amountInput,
        fieldLabel = "مبلغ هزینه",
        maxAmount = WageCalculator.MAX_EXPENSE_AMOUNT,
        allowZero = false,
        allowEmpty = false
    )
    val dateError = JalaliCalendar.validateDateError(date)
    val workerRequiredError = scope == "INDIVIDUAL" && (selectedWorkerId == null || workers.none { it.id == selectedWorkerId })
    val isFormValid = titleError == null && amountError == null && dateError == null && !workerRequiredError

    val selectedCategoryOption = remember(category) {
        EXPENSE_CATEGORY_OPTIONS.firstOrNull { it.code == category } ?: EXPENSE_CATEGORY_OPTIONS.first()
    }

    fun submitExpense() {
        titleTouched = true
        amountTouched = true
        val parsedAmount = Formatters.parsePrice(amountInput)
        val effectiveWorker = if (scope == "INDIVIDUAL") {
            workers.firstOrNull { it.id == selectedWorkerId }
        } else {
            null
        }
        val candidate = ExpenseEntity(
            title = title.trim(),
            category = category,
            scope = scope,
            impactType = if (scope == "GROUP") "DEDUCTION" else impactType,
            workerId = if (scope == "INDIVIDUAL") effectiveWorker?.id else null,
            workerName = if (scope == "INDIVIDUAL") effectiveWorker?.name else null,
            amount = parsedAmount,
            date = date.trim(),
            epochDay = JalaliCalendar.toEpochDay(date.trim()),
            notes = notes.trim()
        )
        val err = WageCalculator.validateExpense(
            expense = candidate,
            availableWorkerIds = workers.map { it.id }.toSet()
        )
        if (err != null) {
            validationError = err
            return
        }
        validationError = null
        onConfirm(candidate)
    }

    Dialog(onDismissRequest = onDismiss) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = Icons.Default.ReceiptLong,
                            color = AmberAccent,
                            size = IconicsSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ثبت هزینه جدید (فردی یا گروهی)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 1. Scope selector (INDIVIDUAL vs GROUP)
                Text(
                    text = "نوع هزینه:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScopeOptionChip(
                        title = "فردی (تکی)",
                        subtitle = "مخصوص یک کارگر",
                        icon = Icons.Default.Person,
                        selected = scope == "INDIVIDUAL",
                        accentColor = CyanAccent,
                        onClick = {
                            scope = "INDIVIDUAL"
                            if (selectedWorkerId == null) {
                                selectedWorkerId = workers.firstOrNull()?.id
                            }
                            validationError = null
                        },
                        testTag = "expense_scope_INDIVIDUAL",
                        modifier = Modifier.weight(1f)
                    )
                    ScopeOptionChip(
                        title = "گروهی (کارگاه)",
                        subtitle = "تقسیم بین حاضرین",
                        icon = Icons.Default.Groups,
                        selected = scope == "GROUP",
                        accentColor = AmberAccent,
                        onClick = {
                            scope = "GROUP"
                            impactType = "DEDUCTION"
                            validationError = null
                        },
                        testTag = "expense_scope_GROUP",
                        modifier = Modifier.weight(1f)
                    )
                }

                // 2. Worker selection (Mandatory when scope == INDIVIDUAL)
                AnimatedVisibility(
                    visible = scope == "INDIVIDUAL",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "انتخاب کارگر (اجباری):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${Formatters.toPersianDigits(workers.size)} کارگر در پروژه",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (workers.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RoseAccent.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "هنوز کارگری در این پروژه ثبت نشده است. برای هزینه فردی ابتدا کارگر اضافه کنید یا نوع هزینه را «گروهی» انتخاب نمایید.",
                                    fontSize = 11.sp,
                                    color = RoseAccent,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                workers.forEach { worker ->
                                    val isSelected = selectedWorkerId == worker.id
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) CyanAccent.copy(alpha = 0.16f) else Slate100,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) CyanAccent else Slate200,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                selectedWorkerId = worker.id
                                                validationError = null
                                            }
                                            .testTag("expense_worker_${worker.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(worker.colorTag)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = worker.name.take(1),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = worker.name,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) CyanAccent else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (worker.role.isNotBlank()) {
                                                    Text(
                                                        text = worker.role,
                                                        fontSize = 9.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Impact Type (ALLOWANCE vs DEDUCTION)
                Text(
                    text = "نوع تأثیر مالی:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (scope == "GROUP") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoseAccent.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = RoseAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "هزینه گروهی کارگاه به‌صورت کاهشی (کسر از حساب و تقسیم بین کارگران حاضر) ثبت می‌شود.",
                                fontSize = 11.sp,
                                color = RoseAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScopeOptionChip(
                            title = "کاهشی (کسر از حساب)",
                            subtitle = "مساعده، کسورات، سهم کارگر",
                            icon = Icons.Default.TrendingDown,
                            selected = impactType == "DEDUCTION",
                            accentColor = RoseAccent,
                            onClick = {
                                impactType = "DEDUCTION"
                                validationError = null
                            },
                            testTag = "expense_impact_DEDUCTION",
                            modifier = Modifier.weight(1f)
                        )
                        ScopeOptionChip(
                            title = "افزایشی (کمک‌هزینه)",
                            subtitle = "پاداش، حق ایاب و ذهاب و...",
                            icon = Icons.Default.TrendingUp,
                            selected = impactType == "ALLOWANCE",
                            accentColor = EmeraldAccent,
                            onClick = {
                                impactType = "ALLOWANCE"
                                validationError = null
                            },
                            testTag = "expense_impact_ALLOWANCE",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Category selector (TRANSIT / FOOD / ACCOMMODATION / MEDICAL / OTHER)
                Text(
                    text = "دسته‌بندی هزینه:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EXPENSE_CATEGORY_OPTIONS.forEach { option ->
                        val isSelected = category == option.code
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) option.color.copy(alpha = 0.16f) else Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) option.color else Slate200,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    category = option.code
                                    validationError = null
                                }
                                .testTag("expense_category_${option.code}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.label,
                                    tint = if (isSelected) option.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = option.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) option.color else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 5. Title field + quick preset chips
                val showTitleError = titleError != null && (titleTouched || title.isNotEmpty())
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleTouched = true
                        validationError = null
                    },
                    label = { Text("عنوان هزینه (مثلاً ناهار کارگاه یا مساعده)", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = selectedCategoryOption.icon,
                            contentDescription = null,
                            tint = selectedCategoryOption.color,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    isError = showTitleError,
                    supportingText = if (showTitleError) {
                        {
                            Text(
                                text = titleError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.5.sp
                            )
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedCategoryOption.presetTitles.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    title = preset
                                    validationError = null
                                }
                        ) {
                            Text(
                                text = preset,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 6. Amount field (Toman)
                val showAmountError = amountError != null && (amountTouched || amountInput.isNotEmpty())
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { raw ->
                        amountInput = Formatters.formatPriceInput(raw)
                        amountTouched = true
                        validationError = null
                    },
                    label = { Text("مبلغ هزینه (تومان)", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showAmountError,
                    supportingText = if (showAmountError) {
                        {
                            Text(
                                text = amountError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.5.sp
                            )
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                // 7. Date field + Next/Previous day helpers
                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                        validationError = null
                    },
                    label = { Text("تاریخ شمسی (مثلاً ۱۴۰۵/۰۲/۰۱)", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    isError = dateError != null,
                    supportingText = if (dateError != null) {
                        {
                            Text(
                                text = dateError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.5.sp
                            )
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_date_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AmberAccent.copy(alpha = 0.14f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (JalaliCalendar.validateDateError(date) == null) {
                                    date = JalaliCalendar.nextDay(date).first
                                    validationError = null
                                }
                            }
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("روز بعدی (+۱)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (JalaliCalendar.validateDateError(date) == null) {
                                    date = JalaliCalendar.previousDay(date).first
                                    validationError = null
                                }
                            }
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("روز قبلی (-۱)", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // 8. Optional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("توضیحات (اختیاری)", fontSize = 11.5.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_notes_input")
                )

                // Validation Error Banner
                if (validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoseAccent.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_validation_error")
                    ) {
                        Text(
                            text = validationError!!,
                            color = RoseAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingOutlinedButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 40.dp,
                        fontSize = 12.sp,
                        testTag = "expense_cancel_button"
                    )
                    LoadingButton(
                        text = "ثبت هزینه",
                        icon = Icons.Default.Save,
                        onClick = { submitExpense() },
                        enabled = isFormValid,
                        containerColor = EmeraldAccent,
                        modifier = Modifier.weight(1.3f),
                        height = 40.dp,
                        fontSize = 12.5.sp,
                        testTag = "expense_submit_button"
                    )
                }
            }
        }
    }
}


@Composable
private fun ScopeOptionChip(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accentColor.copy(alpha = 0.14f) else Slate100,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) accentColor else Slate200,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * بخش مدیریت هزینه‌های فردی و گروهی (قابل استفاده در صفحه داشبورد و گزارش‌ها)
 * شامل دکمه باز کردن دیالوگ ثبت هزینه، فهرست هزینه‌های ثبت‌شده و دیالوگ تأیید حذف هزینه.
 */
@Composable
fun ExpensesManagementSection(
    expenses: List<ExpenseEntity>,
    workers: List<WorkerEntity>,
    onAddExpenseClick: () -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeleteExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    val workerById = remember(workers) { workers.associateBy { it.id } }

    if (pendingDeleteExpense != null) {
        val target = pendingDeleteExpense!!
        AlertDialog(
            onDismissRequest = { pendingDeleteExpense = null },
            title = {
                Text(
                    text = "حذف هزینه ثبت‌شده",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "آیا از حذف هزینه «${target.title}» به مبلغ ${Formatters.formatCurrency(target.amount)} در تاریخ ${Formatters.toPersianDigits(target.date)} اطمینان دارید؟",
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                LoadingButton(
                    text = "حذف هزینه",
                    icon = Icons.Default.DeleteOutline,
                    onClick = {
                        onDeleteExpense(target)
                        pendingDeleteExpense = null
                    },
                    containerColor = RoseAccent,
                    height = 38.dp,
                    fontSize = 12.sp,
                    testTag = "confirm_delete_expense_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteExpense = null }) {
                    Text("انصراف", fontSize = 12.sp)
                }
            }
        )
    }

    HairlineCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "هزینه‌های فردی و گروهی کارگاه",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ثبت و مدیریت ایاب و ذهاب، خوراک، اسکان، درمان و سایر",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                LoadingButton(
                    text = "ثبت هزینه",
                    icon = Icons.Default.Add,
                    onClick = onAddExpenseClick,
                    containerColor = AmberAccent,
                    height = 36.dp,
                    fontSize = 11.5.sp,
                    testTag = "open_add_expense_dialog_button"
                )
            }

            if (expenses.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "هنوز هزینه مجزایی ثبت نشده است. با دکمه «ثبت هزینه» می‌توانید هزینه‌های فردی یا گروهی کارگاه را وارد کنید.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                HorizontalDivider(thickness = 0.6.dp, color = Slate200)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    expenses.forEach { expense ->
                        val catColor = expenseCategoryColor(expense.category)
                        val catIcon = expenseCategoryIcon(expense.category)
                        val catLabel = expenseCategoryLabel(expense.category)
                        val isGroup = expense.scope.equals("GROUP", ignoreCase = true)
                        val isAllowance = !isGroup && expense.impactType.equals("ALLOWANCE", ignoreCase = true)
                        val workerName = if (!isGroup) {
                            expense.workerId?.let { workerById[it]?.name } ?: expense.workerName ?: "کارگر"
                        } else null

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate100,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_item_${expense.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = catLabel,
                                            tint = catColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = expense.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(catLabel)
                                                append(" • ")
                                                if (isGroup) {
                                                    append("گروهی کارگاه")
                                                } else {
                                                    append("فردی: $workerName")
                                                }
                                                append(" • ")
                                                append(Formatters.toPersianDigits(expense.date))
                                            },
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val sign = if (isAllowance) "+" else "-"
                                    val amountColor = if (isAllowance) EmeraldAccent else RoseAccent
                                    Text(
                                        text = "$sign${Formatters.formatCurrency(expense.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = amountColor
                                    )
                                    IconButton(
                                        onClick = { pendingDeleteExpense = expense },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_expense_${expense.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "حذف هزینه",
                                            tint = RoseAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
