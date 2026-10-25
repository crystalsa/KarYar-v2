package com.karyar.app.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.MigrationReport
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.data.repository.DateConflictException
import com.karyar.app.data.repository.WorkerRepository
import com.karyar.app.domain.model.DailyBookkeeping
import com.karyar.app.domain.model.DashboardAnalytics
import com.karyar.app.domain.model.DeletionImpact
import com.karyar.app.domain.model.WorkerPerformance
import com.karyar.app.util.FinancialSummary
import com.karyar.app.util.JalaliCalendar
import com.karyar.app.util.WageCalculator
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerViewModel(
    application: Application,
    customRepository: WorkerRepository?,
    customFoldersFlow: Flow<List<WorkplaceFolderEntity>>?
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        customRepository = null,
        customFoldersFlow = null
    )

    constructor(
        application: Application,
        foldersFlow: Flow<List<WorkplaceFolderEntity>>
    ) : this(
        application = application,
        customRepository = null,
        customFoldersFlow = foldersFlow
    )

    private val repository: WorkerRepository = customRepository ?: run {
        val database = AppDatabase.getDatabase(application)
        WorkerRepository(
            database = database,
            folderDao = database.folderDao(),
            dateFolderDao = database.dateFolderDao(),
            workerDao = database.workerDao(),
            attendanceDao = database.attendanceDao(),
            expenseDao = database.expenseDao()
        )
    }

    private val foldersSourceFlow: Flow<List<WorkplaceFolderEntity>> =
        customFoldersFlow ?: repository.allFolders

    private var timeChangeReceiver: android.content.BroadcastReceiver? = null
    private var lastAutoToday: String = JalaliCalendar.refreshToday()

    // Reactive today date flow backed by JalaliCalendar.todayFlow
    val todayFlow: StateFlow<String> = JalaliCalendar.todayFlow

    // Currently Selected Workplace Folder
    val currentFolder = MutableStateFlow<WorkplaceFolderEntity?>(null)

    // Selected Date Folder (inside Workers & Attendance screens)
    val selectedDateFolder = MutableStateFlow<DateFolderEntity?>(null)

    // Selected Date for Daily Bookkeeping on Dashboard (synced with JalaliCalendar.todayFlow)
    val selectedDailyDate = MutableStateFlow<String>(lastAutoToday)

    // Pending migration report (e.g. from MIGRATION_5_6 duplicate/orphan cleanup)
    val migrationReport: StateFlow<MigrationReport?> = AppDatabase.migrationReportFlow

    // Worker search state (ذره‌بین بالای صفحه کنار سه نقطه)
    val workerSearchQuery = MutableStateFlow("")
    val isWorkerSearchVisible = MutableStateFlow(false)

    // Snackbar message events for user feedback (including Undo actions)
    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    // Confirmation dialog state before deleting an attendance record that has manual values
    private val _pendingDeleteAttendance = MutableStateFlow<PendingAttendanceDelete?>(null)
    val pendingDeleteAttendance: StateFlow<PendingAttendanceDelete?> = _pendingDeleteAttendance

    // Conflict state when moving a date folder to a date that already has attendance records
    private val _pendingDateConflict = MutableStateFlow<PendingDateConflict?>(null)
    val pendingDateConflict: StateFlow<PendingDateConflict?> = _pendingDateConflict

    // Undo state stack for attendance status changes and deletions
    private val undoStack = ArrayDeque<AttendanceUndoState>()
    private val _lastUndoState = MutableStateFlow<AttendanceUndoState?>(null)
    val lastUndoState: StateFlow<AttendanceUndoState?> = _lastUndoState

    // All Folders (Workplaces)
    val allFolders: StateFlow<List<WorkplaceFolderEntity>> = foldersSourceFlow
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Date/Day Folders for current workplace folder
    val dateFolders: StateFlow<List<DateFolderEntity>> = currentFolder
        .flatMapLatest { folder ->
            if (folder != null) repository.getDateFoldersByFolder(folder.id).distinctUntilChanged()
            else flowOf(emptyList())
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Workers for selected folder (all workers in workplace)
    val workers: StateFlow<List<WorkerEntity>> = currentFolder
        .flatMapLatest { folder ->
            if (folder != null) repository.getWorkersByFolder(folder.id).distinctUntilChanged()
            else flowOf(emptyList())
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance for selected folder
    val attendanceList: StateFlow<List<AttendanceEntity>> = currentFolder
        .flatMapLatest { folder ->
            if (folder != null) repository.getAttendanceByFolder(folder.id).distinctUntilChanged()
            else flowOf(emptyList())
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Indexed single-day attendance flow for the selected date folder
    private val selectedDateFolderAttendances: Flow<List<AttendanceEntity>> = selectedDateFolder
        .flatMapLatest { dateFolder ->
            if (dateFolder != null) {
                repository.getAttendanceForDateInFolder(dateFolder.folderId, dateFolder.date)
                    .distinctUntilChanged()
            } else {
                flowOf(emptyList())
            }
        }
        .distinctUntilChanged()

    // Workers inside the active date folder (همگام با حضور و غیاب و پوشه تاریخ)
    val workersInDateFolder: StateFlow<List<WorkerEntity>> = combine(
        selectedDateFolder,
        workers,
        selectedDateFolderAttendances
    ) { dateFolder, workerList, dayAttList ->
        if (dateFolder != null) {
            val attendedIds = dayAttList.asSequence().map { it.workerId }.toHashSet()
            workerList.filter { worker -> worker.id in attendedIds }
        } else {
            workerList
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses for selected folder
    val expenses: StateFlow<List<ExpenseEntity>> = currentFolder
        .flatMapLatest { folder ->
            if (folder != null) repository.getExpensesByFolder(folder.id).distinctUntilChanged()
            else flowOf(emptyList())
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Indexed single-day attendance & expense flows for Daily Bookkeeping
    private val selectedDailyDateAttendances: Flow<List<AttendanceEntity>> = combine(
        currentFolder,
        selectedDailyDate
    ) { folder, dateStr -> folder?.id to dateStr }
        .distinctUntilChanged()
        .flatMapLatest { (folderId, dateStr) ->
            if (folderId != null) {
                repository.getAttendanceForDateInFolder(folderId, dateStr).distinctUntilChanged()
            } else {
                flowOf(emptyList())
            }
        }
        .distinctUntilChanged()

    private val selectedDailyDateExpenses: Flow<List<ExpenseEntity>> = combine(
        currentFolder,
        selectedDailyDate
    ) { folder, dateStr -> folder?.id to dateStr }
        .distinctUntilChanged()
        .flatMapLatest { (folderId, dateStr) ->
            if (folderId != null) {
                repository.getExpensesForDateInFolder(folderId, dateStr).distinctUntilChanged()
            } else {
                flowOf(emptyList())
            }
        }
        .distinctUntilChanged()

    // Daily Bookkeeping (حساب و کتاب روزانه در صفحه اصلی)
    val dailyBookkeeping: StateFlow<DailyBookkeeping> = combine(
        selectedDailyDate,
        workers,
        selectedDailyDateAttendances,
        selectedDailyDateExpenses
    ) { dateStr, workerList, dayAttList, dayExpList ->
        WageCalculator.calculateDailyBookkeeping(
            dateStr = dateStr,
            workers = workerList,
            attendances = dayAttList,
            expenses = dayExpList
        )
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyBookkeeping())

    // Unified Project Financial Summary (single source of truth for analytics & performances)
    val financialSummary: StateFlow<FinancialSummary> = combine(
        workers,
        attendanceList,
        expenses,
        JalaliCalendar.todayFlow
    ) { workerList, attList, expList, todayStr ->
        WageCalculator.calculateFinancialSummary(
            workers = workerList,
            attendances = attList,
            expenses = expList,
            todayStr = todayStr
        )
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary.EMPTY)

    // Worker Performance Summaries directly from unified FinancialSummary
    val workerPerformances: StateFlow<List<WorkerPerformance>> = financialSummary
        .map { it.workerPerformances }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
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
            val attendedDates = attList.asSequence()
                .filter { it.status != AttendanceStatus.ABSENT }
                .map { it.date }
                .distinct()
                .count()
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
            grandTotalProjectCost = summary.grandTotalProjectCost,
            totalWorkerDebt = summary.totalWorkerDebt
        )
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardAnalytics())

    init {
        AppDatabase.getPendingMigrationReport(application)
        try {
            timeChangeReceiver = JalaliCalendar.registerTimeChangeReceiver(application)
        } catch (_: Exception) {}
        startInitialCollectors()
    }

    private fun startInitialCollectors() {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            JalaliCalendar.todayFlow.collect { newToday ->
                if (selectedDailyDate.value == lastAutoToday) {
                    selectedDailyDate.value = newToday
                }
                lastAutoToday = newToday
            }
        }
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            collectFolders(foldersSourceFlow)
        }
    }

    internal suspend fun collectFolders(flow: Flow<List<WorkplaceFolderEntity>>) {
        flow.collect { folderList ->
            applyFoldersUpdate(folderList)
        }
    }

    internal fun applyFoldersUpdate(folderList: List<WorkplaceFolderEntity>) {
        if (folderList.isEmpty()) {
            currentFolder.value = null
            selectedDateFolder.value = null
        } else if (currentFolder.value == null) {
            currentFolder.value = folderList.first()
        } else if (folderList.none { it.id == currentFolder.value?.id }) {
            currentFolder.value = folderList.firstOrNull()
        }
    }

    override fun onCleared() {
        timeChangeReceiver?.let { receiver ->
            try {
                getApplication<Application>().unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
        timeChangeReceiver = null
        super.onCleared()
    }

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

    fun dismissMigrationReport() {
        AppDatabase.clearMigrationReport(getApplication())
    }

    fun showSnackbarMessage(
        message: String,
        actionLabel: String? = null,
        withUndo: Boolean = false
    ) {
        _snackbarMessages.tryEmit(message)
        _snackbarEvents.tryEmit(
            SnackbarEvent(
                message = message,
                actionLabel = actionLabel,
                withUndo = withUndo
            )
        )
    }

    private fun recordUndoAndNotify(undoState: AttendanceUndoState, message: String) {
        synchronized(undoStack) {
            undoStack.addLast(undoState)
            _lastUndoState.value = undoState
        }
        showSnackbarMessage(
            message = message,
            actionLabel = "بازگردانی",
            withUndo = true
        )
    }

    private fun launchDb(actionName: String, block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                Log.e("WorkerViewModel", "Database error in $actionName", e)
                showSnackbarMessage("خطا در ذخیره‌سازی اطلاعات")
            }
        }
    }

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
        launchDb("addFolder") {
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

    suspend fun getFolderDeletionImpact(folderId: Long): DeletionImpact =
        repository.getFolderDeletionImpact(folderId)

    suspend fun getDateFolderDeletionImpact(dateFolder: DateFolderEntity): DeletionImpact =
        repository.getDateFolderDeletionImpact(dateFolder)

    suspend fun getWorkerDeletionImpact(workerId: Long): DeletionImpact =
        repository.getWorkerDeletionImpact(workerId)

    // Date/Day Folder Actions
    fun addDateFolder(date: String, dayOfWeek: String, title: String, notes: String = "") {
        val folder = currentFolder.value ?: return
        val error = JalaliCalendar.validateDateError(date)
        if (error != null) {
            showSnackbarMessage(error)
            return
        }
        val (y, m, d) = JalaliCalendar.parseDate(date)
        val normalizedDate = JalaliCalendar.formatDate(y, m, d)
        launchDb("addDateFolder") {
            val newId = repository.createNextDay(
                folderId = folder.id,
                date = normalizedDate,
                dayOfWeek = dayOfWeek,
                title = title,
                notes = notes
            )
            selectedDateFolder.value = DateFolderEntity(
                id = newId,
                folderId = folder.id,
                date = normalizedDate,
                dayOfWeek = dayOfWeek,
                title = title,
                notes = notes,
                epochDay = JalaliCalendar.toEpochDay(normalizedDate)
            )
        }
    }

    fun createDateFolder(folderId: Long, date: String, dayOfWeek: String, title: String, notes: String = "") {
        addDateFolder(date = date, dayOfWeek = dayOfWeek, title = title, notes = notes)
    }

    fun updateDateFolder(dateFolder: DateFolderEntity) {
        val error = JalaliCalendar.validateDateError(dateFolder.date)
        if (error != null) {
            showSnackbarMessage(error)
            return
        }
        launchDb("updateDateFolder") {
            updateDateFolderSync(dateFolder)
        }
    }

    suspend fun updateDateFolderSync(dateFolder: DateFolderEntity): Result<DateFolderEntity> {
        val error = JalaliCalendar.validateDateError(dateFolder.date)
        if (error != null) {
            showSnackbarMessage(error)
            return Result.failure(IllegalArgumentException(error))
        }
        val (y, m, d) = JalaliCalendar.parseDate(dateFolder.date)
        val normalizedDate = JalaliCalendar.formatDate(y, m, d)
        val updated = dateFolder.copy(
            date = normalizedDate,
            epochDay = JalaliCalendar.toEpochDay(normalizedDate)
        )
        return try {
            repository.updateDateFolder(updated)
            _pendingDateConflict.value = null
            if (selectedDateFolder.value?.id == dateFolder.id) {
                selectedDateFolder.value = updated
            }
            Result.success(updated)
        } catch (e: DateConflictException) {
            val conflictMsg = e.message ?: DateConflictException.buildMessage(e.workerCount)
            _pendingDateConflict.value = PendingDateConflict(
                requestedDateFolder = updated,
                targetDate = e.targetDate,
                conflictingWorkers = e.conflictingWorkers,
                message = conflictMsg
            )
            showSnackbarMessage(conflictMsg)
            Result.failure(e)
        }
    }

    fun mergeDateFolder(dateFolder: DateFolderEntity) {
        val error = JalaliCalendar.validateDateError(dateFolder.date)
        if (error != null) {
            showSnackbarMessage(error)
            return
        }
        launchDb("mergeDateFolder") {
            mergeDateFolderSync(dateFolder)
        }
    }

    suspend fun mergeDateFolderSync(dateFolder: DateFolderEntity): DateFolderEntity {
        val (y, m, d) = JalaliCalendar.parseDate(dateFolder.date)
        val normalizedDate = JalaliCalendar.formatDate(y, m, d)
        val updated = dateFolder.copy(
            date = normalizedDate,
            epochDay = JalaliCalendar.toEpochDay(normalizedDate)
        )
        val surviving = repository.mergeDateFolder(updated)
        _pendingDateConflict.value = null
        if (selectedDateFolder.value?.id == dateFolder.id || selectedDateFolder.value?.id == surviving.id) {
            selectedDateFolder.value = surviving
        }
        showSnackbarMessage("روزهای کاری با موفقیت ادغام شدند")
        return surviving
    }

    fun confirmMergeDateFolder() {
        launchDb("confirmMergeDateFolder") {
            confirmMergeDateFolderSync()
        }
    }

    suspend fun confirmMergeDateFolderSync(): Boolean {
        val pending = _pendingDateConflict.value ?: return false
        _pendingDateConflict.value = null
        val surviving = repository.mergeDateFolder(pending.requestedDateFolder)
        if (selectedDateFolder.value?.id == pending.requestedDateFolder.id || selectedDateFolder.value?.id == surviving.id) {
            selectedDateFolder.value = surviving
        }
        showSnackbarMessage("روزهای کاری با موفقیت ادغام شدند")
        return true
    }

    fun dismissDateConflict() {
        _pendingDateConflict.value = null
    }

    fun cancelDateConflict() {
        dismissDateConflict()
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

    fun updateWorker(
        worker: WorkerEntity,
        dayAttendance: AttendanceEntity? = null
    ) {
        launchDb("updateWorker") {
            repository.updateWorker(
                worker = worker,
                dayAttendance = dayAttendance
            )
        }
    }

    fun deactivateWorker(worker: WorkerEntity) {
        launchDb("deactivateWorker") {
            repository.deactivateWorker(worker)
            showSnackbarMessage("«${worker.name}» غیرفعال شد و سوابق مالی او محفوظ ماند")
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
        launchDb("updateAttendance") { repository.insertAttendance(attendance) }
    }

    private fun AttendanceEntity.withPreservedAllowancesFrom(
        worker: WorkerEntity,
        existing: AttendanceEntity
    ): AttendanceEntity {
        val hasAnyAllowanceSet = existing.transitAllowance > 0L ||
            existing.foodAllowance > 0L ||
            existing.accommodationAllowance > 0L ||
            existing.medicalAllowance > 0L
        return if (existing.hasCustomAllowances || hasAnyAllowanceSet) {
            this.copy(
                transitAllowance = existing.transitAllowance,
                transitImpact = existing.transitImpact,
                foodAllowance = existing.foodAllowance,
                foodImpact = existing.foodImpact,
                accommodationAllowance = existing.accommodationAllowance,
                accommodationImpact = existing.accommodationImpact,
                medicalAllowance = existing.medicalAllowance,
                medicalImpact = existing.medicalImpact,
                hasCustomAllowances = existing.hasCustomAllowances || hasAnyAllowanceSet
            )
        } else {
            this.copy(
                transitAllowance = worker.transitAllowance,
                transitImpact = worker.transitImpact,
                foodAllowance = worker.foodAllowance,
                foodImpact = worker.foodImpact,
                accommodationAllowance = worker.accommodationAllowance,
                accommodationImpact = worker.accommodationImpact,
                medicalAllowance = worker.medicalAllowance,
                medicalImpact = worker.medicalImpact,
                hasCustomAllowances = false
            )
        }
    }

    private suspend fun performAttendanceDeleteSync(
        workerName: String,
        existing: AttendanceEntity
    ) {
        repository.deleteAttendance(existing)
        recordUndoAndNotify(
            undoState = AttendanceUndoState(
                workerId = existing.workerId,
                workerName = workerName,
                date = existing.date,
                previousAttendance = existing
            ),
            message = "رکورد حضور «$workerName» حذف شد"
        )
    }

    fun setAttendanceStatus(
        worker: WorkerEntity,
        dateStr: String,
        status: String,
        forceDelete: Boolean = false
    ) {
        launchDb("setAttendanceStatus") {
            setAttendanceStatusSync(
                worker = worker,
                dateStr = dateStr,
                status = status,
                forceDelete = forceDelete
            )
        }
    }

    suspend fun setAttendanceStatusSync(
        worker: WorkerEntity,
        dateStr: String,
        status: String,
        forceDelete: Boolean = false
    ) {
        val folder = currentFolder.value ?: repository.getFolderById(worker.folderId)
        val folderId = folder?.id ?: worker.folderId
        val workplaceName = folder?.name ?: ""
        val foremanName = folder?.foremanName ?: ""
        val employerName = folder?.employerName ?: ""
        val epochDay = JalaliCalendar.toEpochDay(dateStr)
        val activeDf = selectedDateFolder.value
        val dfId = if (activeDf?.date == dateStr) activeDf.id else 0L
        val requestedStatus = WageCalculator.parseStatus(status)

        val existing = repository.getAttendanceForWorkerAndDate(worker.id, dateStr)
        if (existing != null) {
            val isToggleOffDelete =
                (requestedStatus == AttendanceStatus.FULL_DAY && existing.status == AttendanceStatus.FULL_DAY) ||
                (requestedStatus == AttendanceStatus.HALF_DAY && existing.status == AttendanceStatus.HALF_DAY)

            if (isToggleOffDelete) {
                if (!forceDelete && WageCalculator.hasManualAttendanceValues(worker, existing)) {
                    _pendingDeleteAttendance.value = PendingAttendanceDelete(
                        worker = worker,
                        attendance = existing,
                        dateStr = dateStr
                    )
                } else {
                    _pendingDeleteAttendance.value = null
                    performAttendanceDeleteSync(worker.name, existing)
                }
            } else {
                val targetStatus = when {
                    requestedStatus == AttendanceStatus.HOURLY && existing.status == AttendanceStatus.HOURLY ->
                        AttendanceStatus.ABSENT
                    requestedStatus == AttendanceStatus.ABSENT && WageCalculator.isAbsent(worker, existing) ->
                        if (worker.isHourlyEnabled) AttendanceStatus.HOURLY else AttendanceStatus.FULL_DAY
                    else -> requestedStatus
                }
                val baseForUpdate = existing.withPreservedAllowancesFrom(worker, existing).copy(
                    id = existing.id,
                    folderId = folderId,
                    date = dateStr,
                    epochDay = epochDay,
                    workplaceName = workplaceName.ifBlank { existing.workplaceName },
                    foremanName = foremanName.ifBlank { existing.foremanName },
                    employerName = employerName.ifBlank { existing.employerName },
                    notes = existing.notes
                )
                val updated = WageCalculator.applyStatusFields(
                    attendance = baseForUpdate,
                    worker = worker,
                    targetStatus = targetStatus,
                    preserveManualFields = true
                )
                repository.updateAttendance(updated)
                recordUndoAndNotify(
                    undoState = AttendanceUndoState(
                        workerId = worker.id,
                        workerName = worker.name,
                        date = dateStr,
                        previousAttendance = existing
                    ),
                    message = "وضعیت حضور «${worker.name}» تغییر کرد"
                )
            }
        } else {
            repository.insertAttendance(
                WageCalculator.defaultAttendanceFor(
                    worker = worker,
                    status = requestedStatus,
                    date = dateStr,
                    dateFolderId = dfId,
                    folderId = folderId,
                    workplaceName = workplaceName,
                    foremanName = foremanName,
                    employerName = employerName,
                    notes = ""
                )
            )
            recordUndoAndNotify(
                undoState = AttendanceUndoState(
                    workerId = worker.id,
                    workerName = worker.name,
                    date = dateStr,
                    previousAttendance = null
                ),
                message = "وضعیت حضور «${worker.name}» ثبت شد"
            )
        }
    }

    fun confirmDeleteAttendance() {
        launchDb("confirmDeleteAttendance") {
            confirmDeleteAttendanceSync()
        }
    }

    suspend fun confirmDeleteAttendanceSync(): Boolean {
        val pending = _pendingDeleteAttendance.value ?: return false
        _pendingDeleteAttendance.value = null
        performAttendanceDeleteSync(pending.worker.name, pending.attendance)
        return true
    }

    fun dismissDeleteAttendance() {
        _pendingDeleteAttendance.value = null
    }

    fun cancelDeleteAttendance() {
        dismissDeleteAttendance()
    }

    fun undoLastAttendanceAction() {
        launchDb("undoLastAttendanceAction") {
            undoLastAttendanceActionSync()
        }
    }

    suspend fun undoLastAttendanceActionSync(): Boolean {
        val undo = synchronized(undoStack) {
            val popped = undoStack.removeLastOrNull()
            _lastUndoState.value = undoStack.lastOrNull()
            popped
        } ?: return false

        if (undo.previousAttendance != null) {
            repository.insertAttendance(undo.previousAttendance)
        } else {
            repository.deleteAttendanceForWorkerAndDate(undo.workerId, undo.date)
        }
        showSnackbarMessage("تغییرات حضور «${undo.workerName}» بازگردانی شد")
        return true
    }

    fun toggleAttendanceStatus(worker: WorkerEntity, dateStr: String, isPresent: Boolean) {
        setAttendanceStatus(worker, dateStr, if (isPresent) "FULL" else "ABSENT")
    }

    fun deleteAttendance(attendance: AttendanceEntity) {
        launchDb("deleteAttendance") {
            performAttendanceDeleteSync("کارگر", attendance)
        }
    }

    fun removeWorkerFromDate(workerId: Long, date: String) {
        launchDb("removeWorkerFromDate") {
            val existing = repository.getAttendanceForWorkerAndDate(workerId, date)
            repository.deleteAttendanceForWorkerAndDate(workerId, date)
            if (existing != null) {
                val workerName = workers.value.firstOrNull { it.id == workerId }?.name ?: "کارگر"
                recordUndoAndNotify(
                    undoState = AttendanceUndoState(
                        workerId = workerId,
                        workerName = workerName,
                        date = date,
                        previousAttendance = existing
                    ),
                    message = "کارگر از این روز کاری حذف شد"
                )
            } else {
                showSnackbarMessage("کارگر از این روز کاری حذف شد")
            }
        }
    }

    fun addExistingWorkerToDate(worker: WorkerEntity, date: String, dateFolderId: Long) {
        val folder = currentFolder.value ?: return
        launchDb("addExistingWorkerToDate") {
            repository.insertAttendance(
                WageCalculator.defaultAttendanceFor(
                    worker = worker,
                    date = date,
                    dateFolderId = dateFolderId,
                    folderId = folder.id,
                    workplaceName = folder.name,
                    foremanName = folder.foremanName,
                    employerName = folder.employerName
                )
            )
            showSnackbarMessage("«${worker.name}» به این روز کاری افزوده شد")
        }
    }

    // Expense Actions
    fun addExpense(expense: ExpenseEntity) {
        launchDb("addExpense") {
            addExpenseSync(expense)
        }
    }

    suspend fun addExpenseSync(expense: ExpenseEntity): Result<Long> {
        val folder = currentFolder.value
            ?: (if (expense.folderId > 0L) repository.getFolderById(expense.folderId) else null)
            ?: run {
                val msg = "ابتدا یک پروژه انتخاب کنید"
                showSnackbarMessage(msg)
                return Result.failure(IllegalStateException(msg))
            }

        val folderWorkers = repository.getWorkersByFolderSync(folder.id).ifEmpty { workers.value }
        val workerMap = folderWorkers.associateBy { it.id }

        val validationError = WageCalculator.validateExpense(
            expense = expense,
            availableWorkerIds = if (folderWorkers.isNotEmpty()) workerMap.keys else null
        )
        if (validationError != null) {
            showSnackbarMessage(validationError)
            return Result.failure(IllegalArgumentException(validationError))
        }

        val (y, m, d) = JalaliCalendar.parseDate(expense.date)
        val normalizedDate = JalaliCalendar.formatDate(y, m, d)
        val normalizedScope = expense.scope.trim().uppercase()
        val normalizedCategory = expense.category.trim().uppercase()
        val normalizedImpact = expense.impactType.trim().uppercase()
        val resolvedWorkerId = if (normalizedScope == "INDIVIDUAL") expense.workerId else null
        val resolvedWorkerName = if (normalizedScope == "INDIVIDUAL") {
            resolvedWorkerId?.let { workerMap[it]?.name } ?: expense.workerName
        } else {
            null
        }

        val finalExpense = expense.copy(
            folderId = folder.id,
            title = expense.title.trim(),
            category = normalizedCategory,
            scope = normalizedScope,
            impactType = normalizedImpact,
            workerId = resolvedWorkerId,
            workerName = resolvedWorkerName,
            date = normalizedDate,
            epochDay = JalaliCalendar.toEpochDay(normalizedDate),
            workplaceName = folder.name,
            foremanName = folder.foremanName,
            employerName = folder.employerName,
            notes = expense.notes.trim()
        )
        val id = repository.insertExpense(finalExpense)
        showSnackbarMessage("هزینه «${finalExpense.title}» با موفقیت ثبت شد")
        return Result.success(id)
    }

    fun deleteExpense(expense: ExpenseEntity) {
        launchDb("deleteExpense") {
            deleteExpenseSync(expense)
        }
    }

    suspend fun deleteExpenseSync(expense: ExpenseEntity) {
        repository.deleteExpense(expense)
        showSnackbarMessage("هزینه «${expense.title}» حذف شد")
    }

    fun loadSampleData(clearFirst: Boolean = false) {
        launchDb("loadSampleData") {
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
        launchDb("clearAllData") {
            repository.clearAllData()
            currentFolder.value = null
            selectedDateFolder.value = null
        }
    }
}

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val withUndo: Boolean = false
)

data class PendingAttendanceDelete(
    val worker: WorkerEntity,
    val attendance: AttendanceEntity,
    val dateStr: String
)

data class PendingDateConflict(
    val requestedDateFolder: DateFolderEntity,
    val targetDate: String,
    val conflictingWorkers: List<WorkerEntity>,
    val message: String = DateConflictException.buildMessage(conflictingWorkers.size)
)

data class AttendanceUndoState(
    val workerId: Long,
    val workerName: String,
    val date: String,
    val previousAttendance: AttendanceEntity?
)
