package com.karyar.app.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.data.repository.WorkerRepository
import com.karyar.app.domain.model.DailyBookkeeping
import com.karyar.app.domain.model.DashboardAnalytics
import com.karyar.app.domain.model.WorkerPerformance
import com.karyar.app.util.FinancialSummary
import com.karyar.app.util.JalaliCalendar
import com.karyar.app.util.WageCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkerRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WorkerRepository(
            database = database,
            folderDao = database.folderDao(),
            dateFolderDao = database.dateFolderDao(),
            workerDao = database.workerDao(),
            attendanceDao = database.attendanceDao(),
            expenseDao = database.expenseDao()
        )
        viewModelScope.launch {
            repository.allFolders.collect { folderList ->
                if (folderList.isEmpty()) {
                    currentFolder.value = null
                    selectedDateFolder.value = null
                } else if (currentFolder.value == null) {
                    currentFolder.value = folderList.first()
                } else if (folderList.none { it.id == currentFolder.value?.id }) {
                    currentFolder.value = folderList.firstOrNull()
                }
            }
        }
    }

    // All Folders (Workplaces)
    val allFolders: StateFlow<List<WorkplaceFolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Selected Workplace Folder
    val currentFolder = MutableStateFlow<WorkplaceFolderEntity?>(null)

    // Date/Day Folders for current workplace folder
    val dateFolders: StateFlow<List<DateFolderEntity>> = currentFolder.flatMapLatest { folder ->
        if (folder != null) repository.getDateFoldersByFolder(folder.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Date Folder (inside Workers & Attendance screens)
    val selectedDateFolder = MutableStateFlow<DateFolderEntity?>(null)

    // Workers for selected folder (all workers in workplace)
    val workers: StateFlow<List<WorkerEntity>> = currentFolder.flatMapLatest { folder ->
        if (folder != null) repository.getWorkersByFolder(folder.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance for selected folder
    val attendanceList: StateFlow<List<AttendanceEntity>> = currentFolder.flatMapLatest { folder ->
        if (folder != null) repository.getAttendanceByFolder(folder.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Workers inside the active date folder (همگام با حضور و غیاب و پوشه تاریخ)
    val workersInDateFolder: StateFlow<List<WorkerEntity>> = combine(
        selectedDateFolder,
        workers,
        attendanceList
    ) { dateFolder, workerList, attList ->
        if (dateFolder != null) {
            val attendedIds = attList.filter { it.date == dateFolder.date }.map { it.workerId }.toSet()
            val specific = workerList.filter { worker -> worker.id in attendedIds }
            if (specific.isNotEmpty()) specific else workerList
        } else {
            workerList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses for selected folder
    val expenses: StateFlow<List<ExpenseEntity>> = currentFolder.flatMapLatest { folder ->
        if (folder != null) repository.getExpensesByFolder(folder.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Date for Daily Bookkeeping on Dashboard
    val selectedDailyDate = MutableStateFlow<String>(JalaliCalendar.todayString())

    // Worker search state (ذره‌بین بالای صفحه کنار سه نقطه)
    val workerSearchQuery = MutableStateFlow("")
    val isWorkerSearchVisible = MutableStateFlow(false)

    fun setWorkerSearchQuery(query: String) {
        workerSearchQuery.value = query
    }

    fun toggleWorkerSearch(visible: Boolean? = null) {
        val next = visible ?: !isWorkerSearchVisible.value
        isWorkerSearchVisible.value = next
        if (!next) {
            workerSearchQuery.value = ""
        }
    }

    // Snackbar message events for user feedback
    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    fun showSnackbarMessage(message: String) {
        _snackbarMessages.tryEmit(message)
    }

    private fun launchDb(actionName: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e("WorkerViewModel", "Database error in $actionName", e)
                showSnackbarMessage("خطا در ذخیره‌سازی اطلاعات")
            }
        }
    }

    // Daily Bookkeeping (حساب و کتاب روزانه در صفحه اصلی)
    val dailyBookkeeping: StateFlow<DailyBookkeeping> = combine(
        selectedDailyDate,
        workers,
        attendanceList,
        expenses
    ) { dateStr, workerList, attList, expList ->
        WageCalculator.calculateDailyBookkeeping(
            dateStr = dateStr,
            workers = workerList,
            attendances = attList,
            expenses = expList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyBookkeeping())

    // Unified Project Financial Summary (single source of truth for analytics & performances)
    val financialSummary: StateFlow<FinancialSummary> = combine(
        workers,
        attendanceList,
        expenses
    ) { workerList, attList, expList ->
        WageCalculator.calculateFinancialSummary(
            workers = workerList,
            attendances = attList,
            expenses = expList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary.EMPTY)

    // Worker Performance Summaries directly from unified FinancialSummary
    val workerPerformances: StateFlow<List<WorkerPerformance>> = financialSummary
        .map { it.workerPerformances }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive Analytics for the current folder - synced with all days & all workers
    val analytics: StateFlow<DashboardAnalytics> = combine(
        financialSummary,
        workers,
        dateFolders,
        attendanceList
    ) { summary, workerList, dayFolders, attList ->
        val distinctCalendarDays = if (dayFolders.isNotEmpty()) {
            dayFolders.size
        } else {
            val attendedDates = attList.filter { it.status != com.karyar.app.data.local.entity.AttendanceStatus.ABSENT }
                .map { it.date }
                .distinct()
                .size
            if (attendedDates > 0) attendedDates else if (summary.totalWorkDaysCount > 0) 1 else 0
        }
        DashboardAnalytics(
            totalWorkersCount = workerList.size,
            activeWorkersCount = workerList.count { it.isActive },
            todayAttendanceCount = summary.todayAttendanceCount,
            totalWorkDaysCount = distinctCalendarDays,
            totalPersonDays = summary.totalWorkDaysCount,
            totalWorkHours = summary.totalWorkHours,
            totalOvertimeHours = summary.totalOvertimeHours,
            totalWagesPaid = summary.totalBaseWagesPaid,
            totalHourlyPaid = summary.totalHourlyPaid,
            totalOvertimePaid = summary.totalOvertimePaid,
            totalTransitExpenses = summary.totalTransitExpenses,
            totalAccommodationExpenses = summary.totalAccommodationExpenses,
            totalAccommodationDays = summary.totalAccommodationDays,
            totalFoodExpenses = summary.totalFoodExpenses,
            totalMedicalExpenses = summary.totalMedicalExpenses,
            totalOtherExpenses = summary.totalOtherExpenses,
            totalIndividualExpenses = summary.totalIndividualExpenses,
            totalGroupExpenses = summary.totalGroupExpenses,
            grandTotalExpenses = summary.grandTotalExpenses,
            grandTotalProjectCost = summary.grandTotalProjectCost
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardAnalytics())

    // Selection Handlers
    fun selectFolder(folder: WorkplaceFolderEntity?) {
        currentFolder.value = folder
        selectedDateFolder.value = null
    }

    fun selectDateFolder(dateFolder: DateFolderEntity?) {
        selectedDateFolder.value = dateFolder
    }

    fun selectDailyDate(dateStr: String) {
        selectedDailyDate.value = dateStr
    }

    // Workplace Folder Actions
    fun addFolder(name: String, foremanName: String, employerName: String, colorTag: Long, notes: String = "") {
        viewModelScope.launch {
            val newId = repository.insertFolder(
                WorkplaceFolderEntity(
                    name = name,
                    foremanName = foremanName,
                    employerName = employerName,
                    colorTag = colorTag,
                    notes = notes
                )
            )
            currentFolder.value = WorkplaceFolderEntity(
                id = newId,
                name = name,
                foremanName = foremanName,
                employerName = employerName,
                colorTag = colorTag,
                notes = notes
            )
        }
    }

    fun createFolder(name: String, foremanName: String, employerName: String, notes: String = "", colorTag: Long = 0xFFD97706L) {
        addFolder(name = name, foremanName = foremanName, employerName = employerName, colorTag = colorTag, notes = notes)
    }

    fun updateFolder(folder: WorkplaceFolderEntity) {
        launchDb("updateFolder") {
            repository.updateFolder(folder)
            if (currentFolder.value?.id == folder.id) {
                currentFolder.value = folder
            }
        }
    }

    fun duplicateFolder(folder: WorkplaceFolderEntity) {
        launchDb("duplicateFolder") {
            repository.duplicateFolder(folder)
        }
    }

    fun deleteFolder(folder: WorkplaceFolderEntity) {
        launchDb("deleteFolder") {
            repository.deleteFolder(folder)
            if (currentFolder.value?.id == folder.id) {
                currentFolder.value = null
            }
        }
    }

    // Date/Day Folder Actions
    fun addDateFolder(date: String, dayOfWeek: String, title: String, notes: String = "") {
        val folder = currentFolder.value ?: return
        launchDb("addDateFolder") {
            val newId = repository.insertDateFolder(
                DateFolderEntity(
                    folderId = folder.id,
                    date = date,
                    dayOfWeek = dayOfWeek,
                    title = title,
                    notes = notes
                )
            )
            selectedDateFolder.value = DateFolderEntity(
                id = newId,
                folderId = folder.id,
                date = date,
                dayOfWeek = dayOfWeek,
                title = title,
                notes = notes
            )
        }
    }

    fun createDateFolder(folderId: Long, date: String, dayOfWeek: String, title: String, notes: String = "") {
        addDateFolder(date = date, dayOfWeek = dayOfWeek, title = title, notes = notes)
    }

    fun updateDateFolder(dateFolder: DateFolderEntity) {
        launchDb("updateDateFolder") {
            repository.updateDateFolder(dateFolder)
            if (selectedDateFolder.value?.id == dateFolder.id) {
                selectedDateFolder.value = dateFolder
            }
        }
    }

    fun deleteDateFolder(dateFolder: DateFolderEntity) {
        launchDb("deleteDateFolder") {
            repository.deleteDateFolder(dateFolder)
            if (selectedDateFolder.value?.id == dateFolder.id) {
                selectedDateFolder.value = null
            }
        }
    }

    // Worker Actions
    fun addWorkerWithDate(
        worker: WorkerEntity,
        workDate: String,
        dayOfWeek: String,
        title: String = "روز کاری"
    ) {
        val folder = currentFolder.value ?: return
        launchDb("addWorkerWithDate") {
            val existingDf = repository.getDateFolderByDate(folder.id, workDate)
            val dateFolder = if (existingDf != null) {
                existingDf
            } else {
                val newDfId = repository.insertDateFolder(
                    DateFolderEntity(
                        folderId = folder.id,
                        date = workDate,
                        dayOfWeek = dayOfWeek,
                        title = title
                    )
                )
                DateFolderEntity(
                    id = newDfId,
                    folderId = folder.id,
                    date = workDate,
                    dayOfWeek = dayOfWeek,
                    title = title
                )
            }
            selectedDateFolder.value = dateFolder

            val finalWorker = worker.copy(
                folderId = folder.id
            )
            val workerId = repository.insertWorker(finalWorker)

            repository.insertAttendance(
                WageCalculator.defaultAttendanceFor(
                    worker = finalWorker.copy(id = workerId),
                    date = workDate,
                    dateFolderId = dateFolder.id,
                    folderId = folder.id,
                    workplaceName = folder.name,
                    foremanName = folder.foremanName,
                    employerName = folder.employerName
                )
            )
        }
    }

    fun addWorker(worker: WorkerEntity) {
        val folder = currentFolder.value ?: return
        val activeDateFolder = selectedDateFolder.value
        launchDb("addWorker") {
            val finalWorker = worker.copy(
                folderId = folder.id
            )
            val workerId = repository.insertWorker(finalWorker)

            // If registered in a specific date folder, automatically create a default attendance entry for this day
            if (activeDateFolder != null) {
                repository.insertAttendance(
                    WageCalculator.defaultAttendanceFor(
                        worker = finalWorker.copy(id = workerId),
                        date = activeDateFolder.date,
                        dateFolderId = activeDateFolder.id,
                        folderId = folder.id,
                        workplaceName = folder.name,
                        foremanName = folder.foremanName,
                        employerName = folder.employerName
                    )
                )
            }
        }
    }

    fun duplicateWorker(
        worker: WorkerEntity,
        targetDate: String? = null,
        targetDateFolderId: Long? = null,
        targetDayOfWeek: String? = null
    ) {
        val folder = currentFolder.value ?: return
        val activeDf = selectedDateFolder.value
        val date = targetDate ?: activeDf?.date ?: JalaliCalendar.todayString()
        val dfId = targetDateFolderId ?: activeDf?.id ?: 0L

        launchDb("duplicateWorker") {
            repository.duplicateWorker(
                worker = worker,
                targetDate = date,
                targetDateFolderId = dfId
            )
        }
    }

    fun updateWorker(worker: WorkerEntity) {
        launchDb("updateWorker") {
            repository.updateWorker(worker)
        }
    }

    fun deleteWorker(worker: WorkerEntity) {
        launchDb("deleteWorker") { repository.deleteWorker(worker) }
    }

    // Attendance Actions
    fun addAttendance(attendance: AttendanceEntity) {
        val folder = currentFolder.value ?: return
        launchDb("addAttendance") {
            repository.insertAttendance(
                attendance.copy(
                    folderId = folder.id,
                    workplaceName = folder.name,
                    foremanName = folder.foremanName,
                    employerName = folder.employerName
                )
            )
        }
    }

    fun updateAttendance(attendance: AttendanceEntity) {
        launchDb("updateAttendance") { repository.updateAttendance(attendance) }
    }

    fun setAttendanceStatus(worker: WorkerEntity, dateStr: String, status: String) {
        val folder = currentFolder.value ?: return
        val epochDay = JalaliCalendar.toEpochDay(dateStr)
        val hourlyRate = WageCalculator.defaultHourlyRate(worker)
        val activeDf = selectedDateFolder.value
        val dfId = if (activeDf?.date == dateStr) activeDf.id else 0L

        viewModelScope.launch {
            val existing = attendanceList.value.firstOrNull { it.workerId == worker.id && it.date == dateStr }
            if (existing != null) {
                when (status) {
                    "FULL" -> {
                        if (existing.status == AttendanceStatus.FULL_DAY) {
                            // Already marked full present: toggle off (delete)
                            repository.deleteAttendance(existing)
                        } else {
                            repository.updateAttendance(
                                WageCalculator.defaultAttendanceFor(
                                    worker = worker,
                                    status = AttendanceStatus.FULL_DAY,
                                    date = dateStr,
                                    dateFolderId = existing.dateFolderId,
                                    folderId = folder.id,
                                    workplaceName = folder.name,
                                    foremanName = folder.foremanName,
                                    employerName = folder.employerName,
                                    notes = existing.notes
                                ).copy(id = existing.id, epochDay = epochDay)
                            )
                        }
                    }
                    "HALF" -> {
                        if (existing.status == AttendanceStatus.HALF_DAY) {
                            // Already marked half day: toggle off (delete)
                            repository.deleteAttendance(existing)
                        } else {
                            repository.updateAttendance(
                                WageCalculator.defaultAttendanceFor(
                                    worker = worker,
                                    status = AttendanceStatus.HALF_DAY,
                                    date = dateStr,
                                    dateFolderId = existing.dateFolderId,
                                    folderId = folder.id,
                                    workplaceName = folder.name,
                                    foremanName = folder.foremanName,
                                    employerName = folder.employerName,
                                    notes = existing.notes
                                ).copy(id = existing.id, epochDay = epochDay)
                            )
                        }
                    }
                    "HOURLY" -> {
                        val isAlreadyHourly = existing.status == AttendanceStatus.HOURLY
                        if (isAlreadyHourly) {
                            // Toggle to absent
                            repository.updateAttendance(
                                WageCalculator.defaultAttendanceFor(
                                    worker = worker,
                                    status = AttendanceStatus.ABSENT,
                                    date = dateStr,
                                    dateFolderId = existing.dateFolderId,
                                    folderId = folder.id,
                                    workplaceName = folder.name,
                                    foremanName = folder.foremanName,
                                    employerName = folder.employerName,
                                    notes = existing.notes
                                ).copy(id = existing.id, epochDay = epochDay)
                            )
                        } else {
                            repository.updateAttendance(
                                WageCalculator.defaultAttendanceFor(
                                    worker = worker,
                                    status = AttendanceStatus.HOURLY,
                                    date = dateStr,
                                    dateFolderId = existing.dateFolderId,
                                    folderId = folder.id,
                                    workplaceName = folder.name,
                                    foremanName = folder.foremanName,
                                    employerName = folder.employerName,
                                    notes = existing.notes
                                ).copy(id = existing.id, epochDay = epochDay)
                            )
                        }
                    }
                    "ABSENT" -> {
                        val isAlreadyAbsent = WageCalculator.isAbsent(worker, existing)
                        if (isAlreadyAbsent) {
                            val revertStatus = if (worker.isHourlyEnabled) AttendanceStatus.HOURLY else AttendanceStatus.FULL_DAY
                            repository.updateAttendance(
                                WageCalculator.defaultAttendanceFor(
                                    worker = worker,
                                    status = revertStatus,
                                    date = dateStr,
                                    dateFolderId = existing.dateFolderId,
                                    folderId = folder.id,
                                    workplaceName = folder.name,
                                    foremanName = folder.foremanName,
                                    employerName = folder.employerName,
                                    notes = existing.notes
                                ).copy(id = existing.id, epochDay = epochDay)
                            )
                        } else {
                            repository.updateAttendance(
                                WageCalculator.defaultAttendanceFor(
                                    worker = worker,
                                    status = AttendanceStatus.ABSENT,
                                    date = dateStr,
                                    dateFolderId = existing.dateFolderId,
                                    folderId = folder.id,
                                    workplaceName = folder.name,
                                    foremanName = folder.foremanName,
                                    employerName = folder.employerName,
                                    notes = existing.notes
                                ).copy(id = existing.id, epochDay = epochDay)
                            )
                        }
                    }
                }
            } else {
                val targetStatus = when (status) {
                    "FULL" -> AttendanceStatus.FULL_DAY
                    "HALF" -> AttendanceStatus.HALF_DAY
                    "HOURLY" -> AttendanceStatus.HOURLY
                    else -> AttendanceStatus.ABSENT
                }
                repository.insertAttendance(
                    WageCalculator.defaultAttendanceFor(
                        worker = worker,
                        status = targetStatus,
                        date = dateStr,
                        dateFolderId = dfId,
                        folderId = folder.id,
                        workplaceName = folder.name,
                        foremanName = folder.foremanName,
                        employerName = folder.employerName,
                        notes = ""
                    )
                )
            }
        }
    }

    fun toggleAttendanceStatus(worker: WorkerEntity, dateStr: String, isPresent: Boolean) {
        setAttendanceStatus(worker, dateStr, if (isPresent) "FULL" else "ABSENT")
    }

    fun deleteAttendance(attendance: AttendanceEntity) {
        launchDb("deleteAttendance") { repository.deleteAttendance(attendance) }
    }

    // Expense Actions
    fun addExpense(expense: ExpenseEntity) {
        val folder = currentFolder.value ?: return
        launchDb("addExpense") {
            repository.insertExpense(
                expense.copy(
                    folderId = folder.id,
                    workplaceName = folder.name,
                    foremanName = folder.foremanName,
                    employerName = folder.employerName
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        launchDb("deleteExpense") { repository.deleteExpense(expense) }
    }

    fun loadSampleData(clearFirst: Boolean = false) {
        viewModelScope.launch {
            if (clearFirst) {
                repository.clearAllData()
                currentFolder.value = null
                selectedDateFolder.value = null
            }
            val newFolderId = repository.loadSampleData()
            val newFolder = repository.getFolderById(newFolderId)
            if (newFolder != null) {
                currentFolder.value = newFolder
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            currentFolder.value = null
            selectedDateFolder.value = null
        }
    }
}
