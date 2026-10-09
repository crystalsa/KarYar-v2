package com.karyar.app.ui.screens.attendance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.ui.WorkerViewModel
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.StatusBadge
import com.karyar.app.ui.screens.attendance.components.AttendanceEmptyDayState
import com.karyar.app.ui.screens.attendance.components.AttendanceTableHeader
import com.karyar.app.ui.screens.attendance.components.AttendanceWorkerRow
import com.karyar.app.ui.screens.attendance.components.CopyWorkerConfirmDialog
import com.karyar.app.ui.screens.attendance.components.DeleteWorkerConfirmDialog
import com.karyar.app.ui.screens.attendance.components.HourlyProfileRequiredDialog
import com.karyar.app.ui.screens.attendance.components.MustCreateDayDialog
import com.karyar.app.ui.screens.workers.AddEditWorkerDialog
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.util.Formatters
import com.karyar.app.util.WageCalculator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttendanceScreen(
    viewModel: WorkerViewModel,
    modifier: Modifier = Modifier
) {
    val folder by viewModel.currentFolder.collectAsState()
    val dateFolders by viewModel.dateFolders.collectAsState()
    val selectedDateFolder by viewModel.selectedDateFolder.collectAsState()
    val allWorkers by viewModel.workers.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()

    var isAddingWorker by remember { mutableStateOf(false) }
    var showMustCreateDayDialog by remember { mutableStateOf(false) }
    var showHourlyProfileRequiredDialog by remember { mutableStateOf(false) }
    var editingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var deletingWorkerFromAttendance by remember { mutableStateOf<WorkerEntity?>(null) }
    var copyingWorkerFromAttendance by remember { mutableStateOf<WorkerEntity?>(null) }
    var isCreatingNextDay by remember { mutableStateOf(false) }

    // Day Management States (Long-press to edit/delete)
    var longPressedDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var editingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var deletingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }

    // Active day folder logic
    val activeDayFolder: DateFolderEntity? = selectedDateFolder ?: dateFolders.lastOrNull() ?: dateFolders.firstOrNull()
    val targetDate = activeDayFolder?.date ?: ""

    // Workers for the active day: all workers in the workplace belong to the active day
    val dayWorkers: List<WorkerEntity> = allWorkers

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
        ) {
            // Day Folders Selector Header (پوشه‌های روزهای هفته)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "روزهای کاری",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isCreatingNextDay = true }
                                .testTag("create_next_day_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "ساخت روز بعد",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "روز بعد",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Date folder horizontal chips (از راست به چپ)
                    val sortedDateFolders = remember(dateFolders) {
                        dateFolders.sortedWith(compareBy({ it.date }, { it.id }))
                    }

                    if (sortedDateFolders.isEmpty()) {
                        HairlineCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            backgroundColor = Slate100
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "هنوز روز کاری ثبت نشده است",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "شاید بخواهید از روز دیگری شروع کنید؛ روز کاری مورد نظر خود را ایجاد نمایید.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LoadingButton(
                                    text = "ایجاد اولین روز کاری",
                                    icon = Icons.Default.Add,
                                    onClick = { isCreatingNextDay = true },
                                    containerColor = AmberAccent,
                                    height = 36.dp,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sortedDateFolders.forEach { df ->
                                val isSelected = (activeDayFolder?.id == df.id)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AmberAccent else Slate100,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = { viewModel.selectDateFolder(df) },
                                            onLongClick = { longPressedDateFolder = df }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = df.dayOfWeek,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.5.sp
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = Formatters.toPersianDigits(df.date),
                                            color = if (isSelected) Color.White.copy(alpha = 0.95f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Attendance Checkbox Table
            if (activeDayFolder == null) {
                item {
                    AttendanceEmptyDayState(onCreateFirstDay = { isCreatingNextDay = true })
                }
            } else {
                item {
                    HairlineCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Header row: Folder icon, Day of week and date right next to each other
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconicsBox(
                                        icon = Icons.Default.Folder,
                                        color = AmberAccent,
                                        size = IconicsSize.TINY
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${activeDayFolder.dayOfWeek} ${Formatters.toPersianDigits(targetDate)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                StatusBadge(
                                    text = "${Formatters.toPersianDigits(dayWorkers.size)} نفر",
                                    dotColor = AmberAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (dayWorkers.isEmpty()) {
                                Text(
                                    text = "هنوز کارگری در این روز کاری تعریف نشده است. با دکمه + می‌توانید کارگر جدید اضافه کنید.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                AttendanceTableHeader()

                                Spacer(modifier = Modifier.height(2.dp))

                                // Worker Rows
                                dayWorkers.forEach { worker ->
                                    val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == targetDate }
                                    AttendanceWorkerRow(
                                        worker = worker,
                                        attendance = att,
                                        targetDate = targetDate,
                                        onEditWorker = { editingWorker = worker },
                                        onSetStatus = { status -> viewModel.setAttendanceStatus(worker, targetDate, status) },
                                        onHourlyRequired = { showHourlyProfileRequiredDialog = true },
                                        onCopyWorker = { copyingWorkerFromAttendance = worker },
                                        onDeleteWorker = { deletingWorkerFromAttendance = worker }
                                    )
                                }

                                // مجموع پرداختی روز: محاسبه دقیق و یکپارچه با استفاده از WageCalculator
                                val dayTotalPayout = dayWorkers.sumOf { worker ->
                                    val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == targetDate }
                                    WageCalculator.calculateDayPayout(worker, att)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(thickness = 0.8.dp, color = Slate200)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مجموع پرداختی روز:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(dayTotalPayout),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB to add a worker directly into this day and workplace
        FloatingActionButton(
            onClick = {
                if (dateFolders.isEmpty() || activeDayFolder == null) {
                    showMustCreateDayDialog = true
                } else {
                    isAddingWorker = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .testTag("add_worker_fab"),
            containerColor = AmberAccent,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "افزودن کارگر")
        }
    }

    // Dialogs
    if (showHourlyProfileRequiredDialog) {
        HourlyProfileRequiredDialog(onDismiss = { showHourlyProfileRequiredDialog = false })
    }

    if (isAddingWorker && activeDayFolder != null) {
        AddEditWorkerDialog(
            initialWorker = null,
            initialDate = activeDayFolder.date,
            initialDayOfWeek = activeDayFolder.dayOfWeek,
            onDismiss = { isAddingWorker = false },
            onConfirm = { newWorker ->
                viewModel.addWorkerWithDate(
                    worker = newWorker,
                    workDate = activeDayFolder.date,
                    dayOfWeek = activeDayFolder.dayOfWeek
                )
                isAddingWorker = false
            }
        )
    }

    if (showMustCreateDayDialog) {
        MustCreateDayDialog(
            onDismiss = { showMustCreateDayDialog = false },
            onCreateDay = {
                showMustCreateDayDialog = false
                isCreatingNextDay = true
            }
        )
    }

    if (editingWorker != null) {
        AddEditWorkerDialog(
            initialWorker = editingWorker,
            onDismiss = { editingWorker = null },
            onConfirm = { updatedWorker ->
                viewModel.updateWorker(updatedWorker)
                editingWorker = null
            }
        )
    }

    if (isCreatingNextDay) {
        val baseDateForNext = dateFolders.lastOrNull()?.date ?: activeDayFolder?.date
        CreateNextDayDialog(
            baseDate = baseDateForNext,
            onDismiss = { isCreatingNextDay = false },
            onConfirm = { date, dayOfWeek ->
                viewModel.addDateFolder(
                    date = date,
                    dayOfWeek = dayOfWeek,
                    title = "روز کاری"
                )
                isCreatingNextDay = false
            }
        )
    }

    if (longPressedDateFolder != null) {
        DayActionOptionsDialog(
            dateFolder = longPressedDateFolder!!,
            onDismiss = { longPressedDateFolder = null },
            onEdit = {
                editingDateFolder = longPressedDateFolder
                longPressedDateFolder = null
            },
            onDelete = {
                deletingDateFolder = longPressedDateFolder
                longPressedDateFolder = null
            }
        )
    }

    if (editingDateFolder != null) {
        EditDateFolderDialog(
            dateFolder = editingDateFolder!!,
            onDismiss = { editingDateFolder = null },
            onConfirm = { date, dayOfWeek, title ->
                viewModel.updateDateFolder(
                    editingDateFolder!!.copy(
                        date = date,
                        dayOfWeek = dayOfWeek,
                        title = title
                    )
                )
                editingDateFolder = null
            }
        )
    }

    if (deletingDateFolder != null) {
        DeleteDayConfirmDialog(
            dateFolder = deletingDateFolder!!,
            onDismiss = { deletingDateFolder = null },
            onConfirm = {
                deletingDateFolder?.let { viewModel.deleteDateFolder(it) }
                deletingDateFolder = null
            }
        )
    }

    if (deletingWorkerFromAttendance != null) {
        DeleteWorkerConfirmDialog(
            workerName = deletingWorkerFromAttendance!!.name,
            onDismiss = { deletingWorkerFromAttendance = null },
            onConfirm = {
                deletingWorkerFromAttendance?.let { viewModel.deleteWorker(it) }
                deletingWorkerFromAttendance = null
            }
        )
    }

    if (copyingWorkerFromAttendance != null) {
        val workerToCopy = copyingWorkerFromAttendance!!
        CopyWorkerConfirmDialog(
            workerName = workerToCopy.name,
            onDismiss = { copyingWorkerFromAttendance = null },
            onConfirm = {
                viewModel.duplicateWorker(
                    worker = workerToCopy,
                    targetDate = targetDate,
                    targetDateFolderId = activeDayFolder?.id,
                    targetDayOfWeek = activeDayFolder?.dayOfWeek
                )
                copyingWorkerFromAttendance = null
            }
        )
    }
}
