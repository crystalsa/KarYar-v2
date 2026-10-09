package com.karyar.app.ui.screens.workers

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.ui.WorkerViewModel
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.ModernPillSelector
import com.karyar.app.ui.screens.attendance.CreateNextDayDialog
import com.karyar.app.ui.screens.attendance.DayActionOptionsDialog
import com.karyar.app.ui.screens.attendance.DeleteDayConfirmDialog
import com.karyar.app.ui.screens.attendance.EditDateFolderDialog
import com.karyar.app.ui.screens.attendance.components.DeleteWorkerConfirmDialog
import com.karyar.app.ui.screens.attendance.components.MustCreateDayDialog
import com.karyar.app.ui.screens.workers.components.DayFinancialReportCard
import com.karyar.app.ui.screens.workers.components.WorkerItemCard
import com.karyar.app.ui.screens.workers.components.WorkersEmptyState
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.util.Formatters
import com.karyar.app.util.JalaliCalendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkersScreen(
    viewModel: WorkerViewModel,
    modifier: Modifier = Modifier
) {
    val folder by viewModel.currentFolder.collectAsState()
    val allWorkers by viewModel.workers.collectAsState()
    val performances by viewModel.workerPerformances.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()

    val dateFolders by viewModel.dateFolders.collectAsState()
    val selectedDateFolder by viewModel.selectedDateFolder.collectAsState()
    val searchQuery by viewModel.workerSearchQuery.collectAsState()

    var isCreatingNextDay by remember { mutableStateOf(false) }
    var isDashboardExpanded by rememberSaveable { mutableStateOf(false) }
    var isAddingWorker by remember { mutableStateOf(false) }
    var showMustCreateDayDialog by remember { mutableStateOf(false) }
    var longPressedDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var editingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var deletingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }

    var editingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var viewingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var viewingAttendance by remember { mutableStateOf<AttendanceEntity?>(null) }
    var deletingWorker by remember { mutableStateOf<WorkerEntity?>(null) }

    var statusFilter by remember { mutableStateOf("همه") }

    val activeDayFolder: DateFolderEntity? = selectedDateFolder ?: dateFolders.lastOrNull() ?: dateFolders.firstOrNull()
    val targetDayFolder = activeDayFolder
    val targetDate = targetDayFolder?.date ?: JalaliCalendar.todayString()
    val targetDayOfWeek = targetDayFolder?.dayOfWeek ?: JalaliCalendar.todayDayOfWeek()

    val filteredWorkers = allWorkers.filter { worker ->
        val matchesSearch = worker.name.contains(searchQuery, ignoreCase = true) ||
                worker.role.contains(searchQuery, ignoreCase = true) ||
                worker.phone.contains(searchQuery)
        val matchesStatus = when (statusFilter) {
            "فعال" -> worker.isActive
            "غیرفعال" -> !worker.isActive
            else -> true
        }
        matchesSearch && matchesStatus
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
        ) {
            // Horizontal Day Folders Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                    Spacer(modifier = Modifier.height(6.dp))

                    val sortedDateFolders = remember(dateFolders) {
                        dateFolders.sortedWith(compareBy({ it.date }, { it.id }))
                    }

                    if (sortedDateFolders.isEmpty()) {
                        WorkersEmptyState(
                            isDateFoldersEmpty = true,
                            dayOfWeek = null,
                            onCreateDateFolder = { isCreatingNextDay = true }
                        )
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

            // Financial Dashboard Card
            if (targetDayFolder != null) {
                item {
                    DayFinancialReportCard(
                        targetDayFolder = targetDayFolder,
                        allWorkers = allWorkers,
                        attendanceList = attendanceList,
                        isExpanded = isDashboardExpanded,
                        onToggleExpanded = { isDashboardExpanded = !isDashboardExpanded }
                    )
                }
            }

            // Status Filter Pill
            item {
                ModernPillSelector(
                    items = listOf("همه", "فعال", "غیرفعال"),
                    selectedItem = statusFilter,
                    onItemSelected = { statusFilter = it },
                    labelProvider = { it }
                )
            }

            // Count indicator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (targetDayFolder != null)
                            "کارگران ${targetDayFolder.dayOfWeek} (${Formatters.toPersianDigits(filteredWorkers.size)} نفر)"
                        else
                            "کارگران (${Formatters.toPersianDigits(filteredWorkers.size)} نفر)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Empty State
            if (filteredWorkers.isEmpty()) {
                item {
                    WorkersEmptyState(
                        isDateFoldersEmpty = dateFolders.isEmpty(),
                        dayOfWeek = targetDayFolder?.dayOfWeek,
                        onCreateDateFolder = { isCreatingNextDay = true }
                    )
                }
            }

            // Worker Cards
            items(filteredWorkers, key = { it.id }) { worker ->
                val perf = performances.find { it.worker.id == worker.id }
                val cardTargetDate = targetDayFolder?.date ?: JalaliCalendar.todayString()
                val cardTargetDayOfWeek = targetDayFolder?.dayOfWeek ?: JalaliCalendar.getDayOfWeek(cardTargetDate)
                val att = if (cardTargetDate.isNotBlank()) {
                    attendanceList.firstOrNull { it.workerId == worker.id && it.date == cardTargetDate }
                } else null
                WorkerItemCard(
                    worker = worker,
                    attendance = att,
                    performance = perf,
                    targetDate = cardTargetDate,
                    targetDayOfWeek = cardTargetDayOfWeek,
                    onClick = {
                        viewingWorker = worker
                        viewingAttendance = att
                    },
                    onEdit = { editingWorker = worker },
                    onDelete = { deletingWorker = worker }
                )
            }
        }

        // FAB to add a worker
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

    if (isCreatingNextDay) {
        val baseDateForNext = dateFolders.lastOrNull()?.date ?: selectedDateFolder?.date
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

    if (editingWorker != null) {
        AddEditWorkerDialog(
            initialWorker = editingWorker,
            onDismiss = { editingWorker = null },
            onConfirm = {
                viewModel.updateWorker(it)
                editingWorker = null
            }
        )
    }

    if (viewingWorker != null) {
        val perf = performances.find { it.worker.id == viewingWorker?.id }
        val currentAtt = viewingAttendance ?: (targetDayFolder?.date ?: selectedDateFolder?.date)?.let { d ->
            attendanceList.firstOrNull { it.workerId == viewingWorker?.id && it.date == d }
        }
        WorkerDetailDialog(
            worker = viewingWorker!!,
            attendance = currentAtt,
            performance = perf,
            onDismiss = {
                viewingWorker = null
                viewingAttendance = null
            },
            onEdit = {
                editingWorker = viewingWorker
                viewingWorker = null
                viewingAttendance = null
            },
            onDelete = {
                deletingWorker = viewingWorker
                viewingWorker = null
                viewingAttendance = null
            }
        )
    }

    if (deletingWorker != null) {
        DeleteWorkerConfirmDialog(
            workerName = deletingWorker!!.name,
            onDismiss = { deletingWorker = null },
            onConfirm = {
                deletingWorker?.let { viewModel.deleteWorker(it) }
                deletingWorker = null
            }
        )
    }
}
