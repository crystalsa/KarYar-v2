package com.karyar.app.ui.screens.attendance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
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
import com.karyar.app.ui.screens.attendance.components.EditDayAttendanceDialog
import com.karyar.app.ui.screens.attendance.components.HourlyProfileRequiredDialog
import com.karyar.app.ui.screens.attendance.components.MustCreateDayDialog
import com.karyar.app.ui.screens.workers.AddEditWorkerDialog
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
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
    var editingDayAttendanceWorker by remember { mutableStateOf<WorkerEntity?>(null) }
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

    // Workers for the active day: strictly workers who have an attendance record on this day
    val dayWorkers: List<WorkerEntity> = remember(allWorkers, attendanceList, targetDate) {
        if (targetDate.isBlank()) emptyList()
        else {
            val attendedIds = attendanceList
                .filter { it.date == targetDate }
                .map { it.workerId }
                .toSet()
            allWorkers.filter { it.id in attendedIds }
        }
    }

    // Workers in the project who are not yet added to this active day
    val unassignedWorkers: List<WorkerEntity> = remember(allWorkers, dayWorkers) {
        val assignedIds = dayWorkers.map { it.id }.toSet()
        allWorkers.filter { it.id !in assignedIds }
    }
    var showAddWorkerChoiceDialog by remember { mutableStateOf(false) }

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
                                        onEditDayAttendance = { editingDayAttendanceWorker = worker },
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
                                        text = if (dayTotalPayout < 0L) "بدهکار: ${Formatters.formatCurrency(-dayTotalPayout)}" else Formatters.formatCurrency(dayTotalPayout),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dayTotalPayout < 0L) RoseAccent else EmeraldAccent
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
                } else if (unassignedWorkers.isNotEmpty()) {
                    showAddWorkerChoiceDialog = true
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
        val worker = editingWorker!!
        val currentAtt = attendanceList.firstOrNull { it.workerId == worker.id && it.date == targetDate }
        var pendingDayAttendance: com.karyar.app.data.local.entity.AttendanceEntity? = null
        AddEditWorkerDialog(
            initialWorker = worker,
            initialAttendance = currentAtt,
            initialDate = targetDate,
            initialDayOfWeek = activeDayFolder?.dayOfWeek,
            onDismiss = { editingWorker = null },
            onConfirmAttendance = { updatedAtt ->
                pendingDayAttendance = updatedAtt
            },
            onConfirm = { updatedWorker ->
                viewModel.updateWorker(updatedWorker, dayAttendance = pendingDayAttendance)
                editingWorker = null
            }
        )
    }

    if (editingDayAttendanceWorker != null) {
        val worker = editingDayAttendanceWorker!!
        val currentAtt = attendanceList.firstOrNull { it.workerId == worker.id && it.date == targetDate }
        EditDayAttendanceDialog(
            worker = worker,
            targetDate = targetDate,
            attendance = currentAtt,
            onDismiss = { editingDayAttendanceWorker = null },
            onConfirm = { updatedAtt ->
                viewModel.updateAttendance(updatedAtt)
                editingDayAttendanceWorker = null
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
        val dfToDelete = deletingDateFolder!!
        val initialDayImpact = remember(dfToDelete, allWorkers, attendanceList) {
            val dayAtts = attendanceList.filter { it.dateFolderId == dfToDelete.id || it.date == dfToDelete.date }
            WageCalculator.calculateDateFolderDeletionImpact(
                dateStr = dfToDelete.date,
                workers = allWorkers,
                dayAttendances = dayAtts
            )
        }
        var dayDeletionImpact by remember(dfToDelete.id) { mutableStateOf(initialDayImpact) }
        androidx.compose.runtime.LaunchedEffect(dfToDelete.id) {
            dayDeletionImpact = viewModel.getDateFolderDeletionImpact(dfToDelete)
        }
        DeleteDayConfirmDialog(
            dateFolder = dfToDelete,
            deletionImpact = dayDeletionImpact,
            onDismiss = { deletingDateFolder = null },
            onConfirm = {
                deletingDateFolder?.let { viewModel.deleteDateFolder(it) }
                deletingDateFolder = null
            }
        )
    }

    if (showAddWorkerChoiceDialog && activeDayFolder != null) {
        AlertDialog(
            onDismissRequest = { showAddWorkerChoiceDialog = false },
            title = {
                Text(
                    text = "افزودن کارگر به این روز",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "می‌توانید یکی از پرسنل پروژه را به این روز کاری اضافه کنید یا کارگر جدیدی تعریف نمایید:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "پرسنل موجود در پروژه:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(unassignedWorkers, key = { it.id }) { worker ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate100,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.addExistingWorkerToDate(
                                            worker = worker,
                                            date = targetDate,
                                            dateFolderId = activeDayFolder.id
                                        )
                                        showAddWorkerChoiceDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(worker.colorTag)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = worker.name.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = worker.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${worker.role})",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddWorkerChoiceDialog = false
                        isAddingWorker = true
                    }
                ) {
                    Text(
                        text = "+ تعریف کارگر جدید",
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent,
                        fontSize = 12.5.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkerChoiceDialog = false }) {
                    Text("انصراف", fontSize = 12.sp)
                }
            }
        )
    }

    if (deletingWorkerFromAttendance != null) {
        val workerToDelete = deletingWorkerFromAttendance!!
        val initialWorkerImpact = remember(workerToDelete, attendanceList) {
            val workerAtts = attendanceList.filter { it.workerId == workerToDelete.id }
            WageCalculator.calculateWorkerDeletionImpact(
                worker = workerToDelete,
                workerAttendances = workerAtts
            )
        }
        var workerDeletionImpact by remember(workerToDelete.id) { mutableStateOf(initialWorkerImpact) }
        androidx.compose.runtime.LaunchedEffect(workerToDelete.id) {
            workerDeletionImpact = viewModel.getWorkerDeletionImpact(workerToDelete.id)
        }
        val dayInfoText = if (targetDate.isNotBlank())
            "${activeDayFolder?.dayOfWeek ?: ""} ${Formatters.toPersianDigits(targetDate)}".trim()
        else null
        val effectiveRemoveDate = targetDate.ifBlank { com.karyar.app.util.JalaliCalendar.todayString() }
        DeleteWorkerConfirmDialog(
            workerName = workerToDelete.name,
            dayInfo = dayInfoText,
            deletionImpact = workerDeletionImpact,
            onDismiss = { deletingWorkerFromAttendance = null },
            onRemoveFromDay = {
                viewModel.removeWorkerFromDate(workerToDelete.id, effectiveRemoveDate)
                deletingWorkerFromAttendance = null
            },
            onDeactivateWorker = {
                viewModel.deactivateWorker(workerToDelete)
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
