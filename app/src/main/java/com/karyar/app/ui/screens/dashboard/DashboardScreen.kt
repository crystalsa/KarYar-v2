package com.karyar.app.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.domain.model.WorkerPerformance
import com.karyar.app.ui.WorkerViewModel
import com.karyar.app.ui.components.GlowStatCard
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.StatusBadge
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
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: WorkerViewModel,
    onChangeFolder: () -> Unit = {},
    onNavigateToTab: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val folder by viewModel.currentFolder.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val workerPerformances by viewModel.workerPerformances.collectAsState()
    val dateFolders by viewModel.dateFolders.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()

    // 1. When app starts fresh without any folder selected:
    // "صفحه اول کار وقتی شروع میشه باید خالی باشه"
    if (folder == null) {
        var showSampleConfirm by remember { mutableStateOf(false) }

        if (showSampleConfirm) {
            AlertDialog(
                onDismissRequest = { showSampleConfirm = false },
                title = { Text("بارگذاری داده‌های نمونه", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                text = { Text("همه داده‌های فعلی پاک می‌شود. آیا مطمئن هستید که می‌خواهید داده‌های نمونه را بارگذاری کنید؟", fontSize = 12.5.sp) },
                confirmButton = {
                    LoadingButton(
                        text = "بارگذاری داده نمونه",
                        icon = Icons.Default.PlaylistAdd,
                        onClick = {
                            viewModel.loadSampleData(clearFirst = true)
                            showSampleConfirm = false
                        },
                        containerColor = EmeraldAccent,
                        height = 38.dp,
                        fontSize = 12.sp,
                        testTag = "dashboard_confirm_load_sample_data"
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showSampleConfirm = false }) {
                        Text("انصراف", fontSize = 12.sp)
                    }
                }
            )
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            HairlineCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconicsBox(
                        icon = Icons.Default.Business,
                        color = AmberAccent,
                        size = IconicsSize.HERO
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هیچ پروژه‌ای انتخاب نشده است",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "برای شروع حساب و کتاب، لطفاً ابتدا یک پروژه ایجاد کنید یا داده‌های نمونه را بارگذاری نمایید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LoadingButton(
                            text = "ساخت اولین پروژه",
                            icon = Icons.Default.Add,
                            onClick = onChangeFolder,
                            containerColor = AmberAccent,
                            modifier = Modifier.weight(1f),
                            height = 42.dp,
                            fontSize = 13.sp,
                            testTag = "dashboard_create_first_project_button"
                        )
                        LoadingButton(
                            text = "بارگذاری داده نمونه",
                            icon = Icons.Default.PlaylistAdd,
                            onClick = { showSampleConfirm = true },
                            containerColor = EmeraldAccent,
                            modifier = Modifier.weight(1f),
                            height = 42.dp,
                            fontSize = 13.sp,
                            testTag = "dashboard_load_sample_data_button"
                        )
                    }
                }
            }
        }
        return
    }

    // Dynamic project-wide aggregated sums directly from calculated performances
    val totalProjectBaseWages = remember(workerPerformances) { workerPerformances.sumOf { it.baseWageTotal } }
    val totalProjectHourlyPay = remember(workerPerformances) { workerPerformances.sumOf { it.hourlyPayTotal } }
    val totalProjectOvertimePay = remember(workerPerformances) { workerPerformances.sumOf { it.overtimePayTotal } }
    val totalProjectOvertimeHours = remember(workerPerformances) { workerPerformances.sumOf { it.overtimeHours } }
    val totalProjectHours = remember(workerPerformances) { workerPerformances.sumOf { it.regularHours + it.hourlyHours + it.overtimeHours } }
    val totalProjectAllowances = remember(workerPerformances) { workerPerformances.sumOf { it.totalAllowances } }
    val totalProjectDeductions = remember(workerPerformances) { workerPerformances.sumOf { it.totalDeductions } }
    val totalProjectNetPayout = remember(workerPerformances, analytics) {
        val fromPerf = workerPerformances.sumOf { it.netPayoutBeforeGroup }
        if (fromPerf > 0L) fromPerf else analytics.grandTotalProjectCost
    }

    // مجموع تمام نفرات کارکرده در تمامی روزهای پروژه (نفر-روز)
    // مثلاً روز اول ۵ نفر + روز دوم ۵ نفر = ۱۰ نفر کل پرسنل تا امروز
    val totalPersonDays = remember(workerPerformances, attendanceList, analytics) {
        val fromPerf = workerPerformances.sumOf { it.totalShifts }
        if (fromPerf > 0) fromPerf
        else if (analytics.totalPersonDays > 0) analytics.totalPersonDays
        else attendanceList.count { it.regularHours > 0 || it.hourlyHours > 0 || it.dailyWage > 0 || it.hourlyWage > 0 }
    }

    // خلاصه شمارش نفرات هر روز جهت نمایش در زیرنویس کارت
    val dailyAttendanceSummary = remember(dateFolders, attendanceList) {
        val counts = dateFolders.map { df ->
            attendanceList.count { it.date == df.date && (it.regularHours > 0 || it.hourlyHours > 0 || it.dailyWage > 0 || it.hourlyWage > 0) }
        }.filter { it > 0 }
        if (counts.size >= 2) {
            "مجموع روزها: " + counts.joinToString(" + ") { Formatters.toPersianDigits(it) } + " نفر"
        } else {
            "مجموع حضور پرسنل در تمام روزها"
        }
    }

    var isProjectBreakdownExpanded by rememberSaveable { mutableStateOf(false) }

    // 2. Normal Dashboard when a workplace folder is selected
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
    ) {
        // بخش ۱: کل اطلاعات مالی پروژه تا امروز
        item {
            HairlineCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Big Grand Total Cost Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isProjectBreakdownExpanded = !isProjectBreakdownExpanded },
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldAccent.copy(alpha = 0.10f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "کل پرداختی‌های پروژه تا امروز:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "مجموع خالص کل دستمزدها، اضافه کار و مزایا",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = Formatters.formatCurrency(totalProjectNetPayout),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldAccent
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldAccent.copy(alpha = 0.2f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isProjectBreakdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "ریز ارقام کل پروژه",
                                                tint = EmeraldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // ریز ارقام مالی تمام پروژه با فلش
                            AnimatedVisibility(
                                visible = isProjectBreakdownExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    HorizontalDivider(
                                        thickness = 0.6.dp,
                                        color = EmeraldAccent.copy(alpha = 0.25f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    ProjectDetailRow(
                                        title = "مجموع دستمزد پایه روزانه کل پروژه:",
                                        amount = totalProjectBaseWages,
                                        color = EmeraldAccent
                                    )
                                    if (totalProjectHourlyPay > 0L) {
                                        ProjectDetailRow(
                                            title = "مجموع دستمزد کارکرد ساعتی کل پروژه:",
                                            amount = totalProjectHourlyPay,
                                            color = CyanAccent
                                        )
                                    }
                                    if (totalProjectOvertimePay > 0L) {
                                        ProjectDetailRow(
                                            title = "مجموع دستمزد اضافه کاری کل پروژه:",
                                            amount = totalProjectOvertimePay,
                                            color = AmberAccent
                                        )
                                    }
                                    if (totalProjectAllowances > 0L) {
                                        ProjectDetailRow(
                                            title = "مجموع کمک‌هزینه‌ها و ایاب و ذهاب افزایشی:",
                                            amount = totalProjectAllowances,
                                            color = IndigoAccent
                                        )
                                    }
                                    if (totalProjectDeductions > 0L) {
                                        ProjectDetailRow(
                                            title = "مجموع کسورات کاهشی (ایاب و ذهاب، مسکن، غذا):",
                                            amount = totalProjectDeductions,
                                            color = RoseAccent,
                                            isNegative = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4-Card Overview Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlowStatCard(
                            title = "کل روزهای کاری",
                            value = "${Formatters.toPersianDigits(dateFolders.size)} روز کاری",
                            subtitle = "روزهای ثبت‌شده در پروژه",
                            icon = Icons.Default.CalendarMonth,
                            accentColor = EmeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        GlowStatCard(
                            title = "کل پرسنل تا امروز",
                            value = "${Formatters.toPersianDigits(totalPersonDays)} نفر",
                            subtitle = dailyAttendanceSummary,
                            icon = Icons.Default.People,
                            accentColor = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hoursStr = if (totalProjectHours % 1.0 == 0.0) totalProjectHours.toLong().toString() else String.format(Locale.US, "%.1f", totalProjectHours)
                        GlowStatCard(
                            title = "کل ساعات کارکرد",
                            value = "${Formatters.toPersianDigits(hoursStr)} ساعت",
                            subtitle = "مجموع ساعات عادی و اضافه کار",
                            icon = Icons.Default.AccessTime,
                            accentColor = AmberAccent,
                            modifier = Modifier.weight(1f)
                        )
                        val otHoursStr = if (totalProjectOvertimeHours % 1.0 == 0.0) totalProjectOvertimeHours.toLong().toString() else String.format(Locale.US, "%.1f", totalProjectOvertimeHours)
                        GlowStatCard(
                            title = "کل اضافه کاری",
                            value = "${Formatters.toPersianDigits(otHoursStr)} ساعت",
                            subtitle = "ارزش: ${Formatters.formatCurrency(totalProjectOvertimePay)}",
                            icon = Icons.Default.TrendingUp,
                            accentColor = IndigoAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // بخش ۲: خلاصه کارکرد و دستمزد هر پرسنل در کل پروژه با فلش رو به پایین (پیش‌فرض مخفی)
        // User request:
        // "در زیر هم مثلا کلا روز هایی که سعید احمدی کار کرده ، کل دستمزدی که گرفته کل اضافه کاری یا کل دستمزد ساعتی که گرفته یا کل ایاب و ذهاب و غیره کاهشی افزایش رو بنویس و برای این که شلوغ نشه پیشفرض مخفی باشه و با یک فلش رو به پایین جزئیاتش دیده بشه"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "کارکرد و دریافتی پرسنل در کل پروژه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "پیش‌فرض خلاصه؛ برای مشاهده جزئیات روی فلش هر شخص کلیک کنید",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(
                    text = "${Formatters.toPersianDigits(workerPerformances.size)} پرسنل",
                    dotColor = AmberAccent
                )
            }
        }

        if (workerPerformances.isEmpty()) {
            item {
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هنوز کارگری در این پروژه ثبت نشده است.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(workerPerformances, key = { it.worker.id }) { perf ->
                WorkerCumulativePerformanceCard(perf = perf)
            }
        }
    }
}

/**
 * کارت کارکرد تجمیعی هر پرسنل در کل پروژه با قابلیت باز و بسته شدن جزئیات (فلش رو به پایین، پیش‌فرض مخفی)
 */
@Composable
private fun WorkerCumulativePerformanceCard(
    perf: WorkerPerformance,
    modifier: Modifier = Modifier
) {
    val worker = perf.worker
    // پیش‌فرض مخفی تا صفحه شلوغ نشه (کاربر با زدن فلش جزئیات رو می‌بینه)
    var isExpanded by rememberSaveable(worker.id) { mutableStateOf(false) }

    HairlineCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_worker_${worker.id}")
            .clickable { isExpanded = !isExpanded },
        backgroundColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // سطر خلاصه بالا: اطلاعات شخص، روزهای کاری، دریافتی کل و فلش بازشونده
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // آواتار و نام و شغل
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(worker.colorTag)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = worker.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = worker.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (worker.role.isNotBlank()) {
                            Text(
                                text = worker.role,
                                fontSize = 11.sp,
                                color = AmberAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ستون مبلغ کل پرداختی + دکمه فلش رو به پایین
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "کل دریافتی:",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatCurrency(perf.netPayout),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldAccent
                        )
                    }

                    // دکمه فلش رو به پایین / بالا با استایل دایره‌ای زیبا
                    Surface(
                        shape = CircleShape,
                        color = if (isExpanded) EmeraldAccent.copy(alpha = 0.2f) else Slate100,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "بستن جزئیات" else "مشاهده جزئیات کارکرد",
                                tint = if (isExpanded) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // بخش جزئیات کامل: فقط هنگام باز شدن با فلش نمایش داده می‌شود
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(thickness = 0.6.dp, color = Slate200)
                    Spacer(modifier = Modifier.height(8.dp))

                    // ریز دستمزدها، اضافه کار و ساعات به فرمت دقیق مشخص شده
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate100,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // ۱. کل روزهای کارکرد و ساعات عادی
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "وضعیت کارکرد پرسنل:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = perf.formatWorkSummary(),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }

                            // ۲. کل دستمزد روزانه پایه گرفته شده
                            if (perf.baseWageTotal > 0L) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "کل دستمزد پایه روزانه:",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(perf.baseWageTotal),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // ۳. کل دستمزد ساعتی گرفته شده به فرمت درخواستی:
                            // دستمزد ساعتی: ۲ ساعت ( هر ساعت 100000 تومان)= 200000 تومان
                            val hHours = perf.hourlyHours
                            val hRate = if (worker.hourlyWageRate > 0L) worker.hourlyWageRate
                                        else if (worker.baseHourlyWage > 0L) worker.baseHourlyWage
                                        else if (hHours > 0.0) (perf.hourlyPayTotal / hHours).toLong()
                                        else 0L

                            if (perf.hourlyPayTotal > 0L || hHours > 0.0 || worker.isHourlyEnabled) {
                                val hHoursClean = Formatters.toPersianDigits(hHours.toString().removeSuffix(".0"))
                                val hRateStr = Formatters.formatCurrency(hRate)
                                val hTotalStr = Formatters.formatCurrency(perf.hourlyPayTotal)
                                Text(
                                    text = if (hHours > 0.0) {
                                        "دستمزد ساعتی: $hHoursClean ساعت (هر ساعت $hRateStr) = $hTotalStr"
                                    } else {
                                        "دستمزد ساعتی: (هر ساعت $hRateStr) = ۰ تومان"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyanAccent
                                )
                            }

                            // ۴. کل اضافه کاری گرفته شده به فرمت درخواستی:
                            // اضافه کار: ۲ ساعت ( هر ساعت 100000 تومان)= 200000 تومان
                            val otHours = perf.overtimeHours
                            val otRate = if (worker.overtimeRate > 0L) worker.overtimeRate
                                         else if (otHours > 0.0) (perf.overtimePayTotal / otHours).toLong()
                                         else 0L

                            if (perf.overtimePayTotal > 0L || otHours > 0.0) {
                                val otHoursClean = Formatters.toPersianDigits(otHours.toString().removeSuffix(".0"))
                                val otRateStr = Formatters.formatCurrency(otRate)
                                val otTotalStr = Formatters.formatCurrency(perf.overtimePayTotal)
                                Text(
                                    text = "اضافه کار: $otHoursClean ساعت (هر ساعت $otRateStr) = $otTotalStr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberAccent
                                )
                            }

                            // ۵. کل ایاب و ذهاب و کمک هزینه‌های افزایشی (مبالغ افزایشی)
                            if (perf.totalAllowances > 0L) {
                                HorizontalDivider(thickness = 0.5.dp, color = Slate200)
                                Text(
                                    text = "کمک‌هزینه‌ها و ایاب و ذهاب (افزایشی):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoAccent
                                )
                                if (perf.transitAllowanceTotal > 0L) {
                                    ProjectDetailRow(
                                        title = "ایاب و ذهاب (افزایش پرداختی):",
                                        amount = perf.transitAllowanceTotal,
                                        color = IndigoAccent
                                    )
                                }
                                if (perf.foodAllowanceTotal > 0L) {
                                    ProjectDetailRow(
                                        title = "کمک‌هزینه خوراک:",
                                        amount = perf.foodAllowanceTotal,
                                        color = IndigoAccent
                                    )
                                }
                                if (perf.accommodationAllowanceTotal > 0L) {
                                    ProjectDetailRow(
                                        title = "حق مسکن و اسکان:",
                                        amount = perf.accommodationAllowanceTotal,
                                        color = IndigoAccent
                                    )
                                }
                                if (perf.medicalAllowanceTotal > 0L) {
                                    ProjectDetailRow(
                                        title = "کمک‌هزینه درمانی:",
                                        amount = perf.medicalAllowanceTotal,
                                        color = IndigoAccent
                                    )
                                }
                                ProjectDetailRow(
                                    title = "مجموع کل افزایش‌ها:",
                                    amount = perf.totalAllowances,
                                    color = IndigoAccent
                                )
                            }

                            // ۶. کل کسورات کاهشی (ایاب و ذهاب کاهشی، مسکن، خوراک، درمان)
                            if (perf.totalDeductions > 0L) {
                                HorizontalDivider(thickness = 0.5.dp, color = Slate200)
                                Text(
                                    text = "کسورات و کاهشی‌های حقوق شخص:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseAccent
                                )
                                if (perf.transitDeductionTotal > 0L) {
                                    ProjectDetailRow(
                                        title = "کسر ایاب و ذهاب:",
                                        amount = perf.transitDeductionTotal,
                                        color = RoseAccent,
                                        isNegative = true
                                    )
                                }
                                if (perf.accommodationDeductionTotal > 0L) {
                                    ProjectDetailRow(
                                        title = "کسر هزینه مسکن:",
                                        amount = perf.accommodationDeductionTotal,
                                        color = RoseAccent,
                                        isNegative = true
                                    )
                                }
                                if (perf.foodDeductionTotal > 0L) {
                                    ProjectDetailRow(
                                        title = "کسر هزینه خوراک:",
                                        amount = perf.foodDeductionTotal,
                                        color = RoseAccent,
                                        isNegative = true
                                    )
                                }
                                if (perf.medicalDeductionTotal > 0L) {
                                    ProjectDetailRow(
                                        title = "کسر هزینه درمان:",
                                        amount = perf.medicalDeductionTotal,
                                        color = RoseAccent,
                                        isNegative = true
                                    )
                                }
                                ProjectDetailRow(
                                    title = "مجموع کل کسورات:",
                                    amount = perf.totalDeductions,
                                    color = RoseAccent,
                                    isNegative = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ۷. کادر خالص پرداختی نهایی این شخص در کل پروژه
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldAccent.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مجموع خالص دریافتی این شخص تا امروز:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Formatters.formatCurrency(perf.netPayout),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailRow(
    title: String,
    amount: Long,
    color: Color,
    isNegative: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val prefix = if (isNegative && amount > 0L) "- " else ""
        Text(
            text = "$prefix${Formatters.formatCurrency(amount)}",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
