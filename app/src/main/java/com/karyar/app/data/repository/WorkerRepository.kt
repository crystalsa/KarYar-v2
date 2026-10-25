package com.karyar.app.data.repository

import androidx.room.withTransaction
import com.karyar.app.data.local.AppDatabase
import com.karyar.app.data.local.dao.AttendanceDao
import com.karyar.app.data.local.dao.DateFolderDao
import com.karyar.app.data.local.dao.ExpenseDao
import com.karyar.app.data.local.dao.WorkerDao
import com.karyar.app.data.local.dao.WorkplaceFolderDao
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.AttendanceStatus
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.domain.model.DeletionImpact
import com.karyar.app.util.Formatters
import com.karyar.app.util.JalaliCalendar
import com.karyar.app.util.WageCalculator
import kotlinx.coroutines.flow.Flow

class DateConflictException(
    val targetDate: String,
    val conflictingWorkers: List<WorkerEntity>,
    message: String = buildMessage(conflictingWorkers.size)
) : IllegalStateException(message) {
    val workers: List<WorkerEntity> get() = conflictingWorkers
    val workerIds: List<Long> get() = conflictingWorkers.map { it.id }
    val workerNames: List<String> get() = conflictingWorkers.map { it.name }
    val workerCount: Int get() = conflictingWorkers.size

    companion object {
        fun buildMessage(count: Int): String =
            "در تاریخ مقصد برای ${Formatters.toPersianDigits(count)} کارگر حضور ثبت شده است"
    }
}

class WorkerRepository(
    private val database: AppDatabase,
    private val folderDao: WorkplaceFolderDao,
    private val dateFolderDao: DateFolderDao,
    private val workerDao: WorkerDao,
    private val attendanceDao: AttendanceDao,
    private val expenseDao: ExpenseDao
) {
    constructor(database: AppDatabase) : this(
        database = database,
        folderDao = database.folderDao(),
        dateFolderDao = database.dateFolderDao(),
        workerDao = database.workerDao(),
        attendanceDao = database.attendanceDao(),
        expenseDao = database.expenseDao()
    )
    val allFolders: Flow<List<WorkplaceFolderEntity>> = folderDao.getAllFolders()

    fun getDateFoldersByFolder(folderId: Long): Flow<List<DateFolderEntity>> = dateFolderDao.getDateFoldersByFolder(folderId)
    fun getWorkersByFolder(folderId: Long): Flow<List<WorkerEntity>> = workerDao.getWorkersByFolder(folderId)
    suspend fun getWorkersByFolderSync(folderId: Long): List<WorkerEntity> = workerDao.getWorkersByFolderSync(folderId)
    fun getWorkersByDateFolder(dateFolderId: Long): Flow<List<WorkerEntity>> = workerDao.getWorkersByDateFolder(dateFolderId)
    fun getAttendanceByFolder(folderId: Long): Flow<List<AttendanceEntity>> = attendanceDao.getAttendanceByFolder(folderId)
    fun getAttendanceForDateInFolder(folderId: Long, date: String): Flow<List<AttendanceEntity>> = attendanceDao.getAttendanceForDateInFolder(folderId, date)
    suspend fun getAttendanceForWorkerAndDate(workerId: Long, date: String): AttendanceEntity? = attendanceDao.getAttendanceForWorkerAndDate(workerId, date)
    fun getExpensesByFolder(folderId: Long): Flow<List<ExpenseEntity>> = expenseDao.getExpensesByFolder(folderId)
    suspend fun getExpensesByFolderSync(folderId: Long): List<ExpenseEntity> = expenseDao.getExpensesByFolderSync(folderId)
    fun getExpensesForDateInFolder(folderId: Long, date: String): Flow<List<ExpenseEntity>> = expenseDao.getExpensesForDateInFolder(folderId, date)

    suspend fun insertFolder(folder: WorkplaceFolderEntity): Long = folderDao.insertFolder(folder)
    suspend fun updateFolder(folder: WorkplaceFolderEntity) = folderDao.updateFolder(folder)
    suspend fun getFolderById(folderId: Long): WorkplaceFolderEntity? = folderDao.getFolderById(folderId)

    suspend fun insertDateFolder(dateFolder: DateFolderEntity): Long = dateFolderDao.insertDateFolder(
        dateFolder.copy(epochDay = JalaliCalendar.toEpochDay(dateFolder.date))
    )

    suspend fun updateDateFolder(dateFolder: DateFolderEntity) = database.withTransaction {
        val oldDateFolder = dateFolderDao.getDateFolderById(dateFolder.id)
        val newEpochDay = JalaliCalendar.toEpochDay(dateFolder.date)
        val updatedFolder = dateFolder.copy(epochDay = newEpochDay)
        if (oldDateFolder != null && oldDateFolder.date != dateFolder.date) {
            val directConflicts = attendanceDao.getConflictingAttendancesForDateMove(
                folderId = dateFolder.folderId,
                dateFolderId = dateFolder.id,
                oldDate = oldDateFolder.date,
                newDate = dateFolder.date
            )
            val sourceAttendances = attendanceDao.getAttendanceForDateFolderSync(
                folderId = dateFolder.folderId,
                dateFolderId = dateFolder.id,
                date = oldDateFolder.date
            )
            val sourceIds = sourceAttendances.asSequence().map { it.id }.toHashSet()
            val folderTargetAttendances = attendanceDao
                .getAttendanceForDateInFolderSync(dateFolder.folderId, dateFolder.date)
                .filter { it.id !in sourceIds }

            val conflictingAttendances = if (directConflicts.isNotEmpty()) {
                directConflicts
            } else {
                folderTargetAttendances
            }

            if (conflictingAttendances.isNotEmpty()) {
                val workerById = workerDao.getWorkersByFolderSync(dateFolder.folderId).associateBy { it.id }
                val conflictingWorkerIds = conflictingAttendances.map { it.workerId }.distinct()
                val conflictingWorkers = conflictingWorkerIds.map { wId ->
                    workerById[wId]
                        ?: workerDao.getWorkerByIdSync(wId)
                        ?: WorkerEntity(id = wId, folderId = dateFolder.folderId, name = "کارگر", role = "")
                }
                throw DateConflictException(
                    targetDate = dateFolder.date,
                    conflictingWorkers = conflictingWorkers
                )
            }

            dateFolderDao.updateDateFolder(updatedFolder)
            attendanceDao.updateDateForDateFolder(
                folderId = dateFolder.folderId,
                dateFolderId = dateFolder.id,
                oldDate = oldDateFolder.date,
                newDate = dateFolder.date,
                newEpochDay = newEpochDay
            )
        } else {
            dateFolderDao.updateDateFolder(updatedFolder)
        }
    }

    suspend fun mergeDateFolder(dateFolder: DateFolderEntity): DateFolderEntity = database.withTransaction {
        val oldDateFolder = dateFolderDao.getDateFolderById(dateFolder.id)
        val newEpochDay = JalaliCalendar.toEpochDay(dateFolder.date)
        val existingTargetFolder = dateFolderDao.getDateFolderByDate(dateFolder.folderId, dateFolder.date)
        val survivingFolder = if (existingTargetFolder != null && existingTargetFolder.id != dateFolder.id) {
            existingTargetFolder.copy(
                dayOfWeek = dateFolder.dayOfWeek.ifBlank { existingTargetFolder.dayOfWeek },
                title = dateFolder.title.ifBlank { existingTargetFolder.title },
                notes = dateFolder.notes.ifBlank { existingTargetFolder.notes },
                epochDay = newEpochDay
            )
        } else {
            dateFolder.copy(epochDay = newEpochDay)
        }

        if (oldDateFolder != null && oldDateFolder.date != dateFolder.date) {
            val sourceAttendances = attendanceDao.getAttendanceForDateFolderSync(
                folderId = dateFolder.folderId,
                dateFolderId = dateFolder.id,
                date = oldDateFolder.date
            )
            for (srcAtt in sourceAttendances) {
                val targetAtt = attendanceDao.getAttendanceForWorkerAndDate(srcAtt.workerId, dateFolder.date)
                if (targetAtt != null && targetAtt.id != srcAtt.id) {
                    attendanceDao.deleteAttendance(srcAtt)
                    attendanceDao.updateAttendance(
                        targetAtt.copy(
                            dateFolderId = survivingFolder.id,
                            epochDay = newEpochDay
                        )
                    )
                }
            }
            attendanceDao.updateDateForDateFolder(
                folderId = dateFolder.folderId,
                dateFolderId = dateFolder.id,
                oldDate = oldDateFolder.date,
                newDate = dateFolder.date,
                newEpochDay = newEpochDay
            )
            if (survivingFolder.id != dateFolder.id) {
                val movedAttendances = attendanceDao.getAttendanceByDateFolderSync(dateFolder.id)
                for (att in movedAttendances) {
                    attendanceDao.updateAttendance(att.copy(dateFolderId = survivingFolder.id))
                }
                dateFolderDao.deleteDateFolderById(dateFolder.id)
            }
        }
        dateFolderDao.updateDateFolder(survivingFolder)
        survivingFolder
    }
    suspend fun getDateFolderByDate(folderId: Long, date: String): DateFolderEntity? = dateFolderDao.getDateFolderByDate(folderId, date)
    suspend fun deleteDateFolder(dateFolder: DateFolderEntity) = database.withTransaction {
        attendanceDao.deleteAttendanceForDateFolder(
            folderId = dateFolder.folderId,
            dateFolderId = dateFolder.id,
            date = dateFolder.date
        )
        dateFolderDao.deleteDateFolder(dateFolder)
    }

    suspend fun deleteFolder(folder: WorkplaceFolderEntity) = database.withTransaction {
        folderDao.deleteFolder(folder)
    }

    /**
     * قبل از حذف کارگر، تعداد روزهای حضور و مبلغ خالص تسویه‌ای را که از بین می‌رود برمی‌گرداند.
     */
    suspend fun getWorkerDeletionImpact(workerId: Long): DeletionImpact {
        val worker = workerDao.getWorkerByIdSync(workerId) ?: return DeletionImpact()
        val workerAttendances = attendanceDao.getAttendanceForWorkerSync(workerId)
        val workerExpenses = expenseDao.getExpensesForWorkerSync(worker.folderId, workerId)
            .filter { it.scope.equals("INDIVIDUAL", ignoreCase = true) }
        return WageCalculator.calculateWorkerDeletionImpact(
            worker = worker,
            workerAttendances = workerAttendances,
            workerIndividualExpenses = workerExpenses
        )
    }

    suspend fun getWorkerDeletionImpact(worker: WorkerEntity): DeletionImpact =
        getWorkerDeletionImpact(worker.id)

    /**
     * قبل از حذف روز کاری، تعداد روزهای حضور و مبلغ خالص تسویه‌ای را که از بین می‌رود برمی‌گرداند.
     */
    suspend fun getDateFolderDeletionImpact(dateFolderId: Long): DeletionImpact {
        val dateFolder = dateFolderDao.getDateFolderById(dateFolderId) ?: return DeletionImpact()
        return getDateFolderDeletionImpact(dateFolder)
    }

    suspend fun getDateFolderDeletionImpact(dateFolder: DateFolderEntity): DeletionImpact {
        val workers = workerDao.getWorkersByFolderSync(dateFolder.folderId)
        val dayAttendances = attendanceDao.getAttendanceForDateFolderSync(
            folderId = dateFolder.folderId,
            dateFolderId = dateFolder.id,
            date = dateFolder.date
        )
        val dayExpenses = expenseDao.getExpensesForDateInFolderSync(
            folderId = dateFolder.folderId,
            date = dateFolder.date
        )
        return WageCalculator.calculateDateFolderDeletionImpact(
            dateStr = dateFolder.date,
            workers = workers,
            dayAttendances = dayAttendances,
            dayExpenses = dayExpenses
        )
    }

    /**
     * قبل از حذف پوشه کارگاه، تعداد روزهای حضور و مبلغ خالص تسویه‌ای را که از بین می‌رود برمی‌گرداند.
     */
    suspend fun getFolderDeletionImpact(folderId: Long): DeletionImpact {
        val workers = workerDao.getWorkersByFolderSync(folderId)
        val attendances = attendanceDao.getAttendanceByFolderSync(folderId)
        val expenses = expenseDao.getExpensesByFolderSync(folderId)
        return WageCalculator.calculateFolderDeletionImpact(
            workers = workers,
            attendances = attendances,
            expenses = expenses
        )
    }

    suspend fun getFolderDeletionImpact(folder: WorkplaceFolderEntity): DeletionImpact =
        getFolderDeletionImpact(folder.id)

    suspend fun duplicateFolder(folder: WorkplaceFolderEntity): Long = database.withTransaction {
        val newFolderId = folderDao.insertFolder(
            folder.copy(
                id = 0,
                name = "${folder.name} (کپی)",
                createdAt = JalaliCalendar.todayString()
            )
        )

        val workers = workerDao.getWorkersByFolderSync(folder.id)
        val workerIdMap = mutableMapOf<Long, Long>()

        for (w in workers) {
            val newWId = workerDao.insertWorker(w.copy(id = 0, folderId = newFolderId))
            workerIdMap[w.id] = newWId
        }

        val dateFolders = dateFolderDao.getDateFoldersByFolderSync(folder.id)
        val dateFolderIdMap = mutableMapOf<Long, Long>()
        val dateFolderByDateMap = mutableMapOf<String, Long>()
        for (df in dateFolders) {
            val newDfId = dateFolderDao.insertDateFolder(df.copy(id = 0, folderId = newFolderId))
            dateFolderIdMap[df.id] = newDfId
            dateFolderByDateMap[df.date] = newDfId
        }

        val attendances = attendanceDao.getAttendanceByFolderSync(folder.id)
        for (att in attendances) {
            val mappedWorkerId = workerIdMap[att.workerId] ?: continue
            val mappedDateFolderId = dateFolderIdMap[att.dateFolderId] ?: dateFolderByDateMap[att.date] ?: 0L
            attendanceDao.insertAttendance(
                att.copy(
                    id = 0,
                    folderId = newFolderId,
                    workerId = mappedWorkerId,
                    dateFolderId = mappedDateFolderId,
                    workplaceName = "${folder.name} (کپی)"
                )
            )
        }

        val expenses = expenseDao.getExpensesByFolderSync(folder.id)
        for (exp in expenses) {
            val mappedWorkerId = if (exp.workerId != null) workerIdMap[exp.workerId] else null
            expenseDao.insertExpense(
                exp.copy(
                    id = 0,
                    folderId = newFolderId,
                    workerId = mappedWorkerId,
                    workplaceName = "${folder.name} (کپی)"
                )
            )
        }

        newFolderId
    }

    suspend fun createNextDay(
        folderId: Long,
        date: String,
        dayOfWeek: String,
        title: String = "روز کاری",
        notes: String = ""
    ): Long = database.withTransaction {
        val existingDf = dateFolderDao.getDateFolderByDate(folderId, date)
        val targetDfId = if (existingDf != null) {
            existingDf.id
        } else {
            dateFolderDao.insertDateFolder(
                DateFolderEntity(
                    folderId = folderId,
                    date = date,
                    dayOfWeek = dayOfWeek,
                    title = title,
                    notes = notes,
                    epochDay = JalaliCalendar.toEpochDay(date)
                )
            )
        }
        val activeWorkers = workerDao.getWorkersByFolderSync(folderId).filter { it.isActive }
        for (w in activeWorkers) {
            attendanceDao.insertAttendance(
                WageCalculator.defaultAttendanceFor(
                    worker = w,
                    date = date,
                    dateFolderId = targetDfId,
                    folderId = folderId
                )
            )
        }
        targetDfId
    }

    suspend fun duplicateWorker(
        worker: WorkerEntity,
        targetDate: String,
        targetDateFolderId: Long
    ): Long = database.withTransaction {
        val newWorkerId = workerDao.insertWorker(
            worker.copy(
                id = 0,
                name = "${worker.name} (کپی)"
            )
        )
        val copiedWorker = worker.copy(id = newWorkerId)
        attendanceDao.insertAttendance(
            WageCalculator.defaultAttendanceFor(
                worker = copiedWorker,
                date = targetDate,
                dateFolderId = targetDateFolderId,
                folderId = worker.folderId
            )
        )
        newWorkerId
    }

    suspend fun insertWorker(worker: WorkerEntity): Long = workerDao.insertWorker(worker)
    suspend fun updateWorker(
        worker: WorkerEntity,
        dayAttendance: AttendanceEntity? = null
    ) = database.withTransaction {
        val oldWorker = workerDao.getWorkerByIdSync(worker.id)
        if (oldWorker != null) {
            attendanceDao.freezeLegacyAllowancesForWorker(
                workerId = worker.id,
                oldTransitAllowance = oldWorker.transitAllowance,
                oldTransitImpact = oldWorker.transitImpact,
                oldFoodAllowance = oldWorker.foodAllowance,
                oldFoodImpact = oldWorker.foodImpact,
                oldAccommodationAllowance = oldWorker.accommodationAllowance,
                oldAccommodationImpact = oldWorker.accommodationImpact,
                oldMedicalAllowance = oldWorker.medicalAllowance,
                oldMedicalImpact = oldWorker.medicalImpact
            )
        }
        if (dayAttendance != null) {
            attendanceDao.upsertAttendance(dayAttendance.copy(hasCustomAllowances = true))
        }
        workerDao.updateWorker(worker)
        expenseDao.updateWorkerName(worker.id, worker.name)
    }
    suspend fun deactivateWorker(worker: WorkerEntity) = updateWorker(worker.copy(isActive = false))
    suspend fun deleteWorker(worker: WorkerEntity) = database.withTransaction {
        expenseDao.deleteExpensesForWorker(worker.id)
        workerDao.deleteWorker(worker)
    }

    suspend fun insertAttendance(attendance: AttendanceEntity): Long = attendanceDao.upsertAttendance(attendance)
    suspend fun updateAttendance(attendance: AttendanceEntity) = attendanceDao.updateAttendance(attendance)
    suspend fun deleteAttendance(attendance: AttendanceEntity) = attendanceDao.deleteAttendance(attendance)
    suspend fun deleteAttendanceForWorkerAndDate(workerId: Long, date: String) = attendanceDao.deleteAttendanceForWorkerAndDate(workerId, date)

    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

    suspend fun clearAllData() = database.withTransaction {
        folderDao.clearAll()
        dateFolderDao.clearAll()
        workerDao.clearAll()
        attendanceDao.clearAll()
        expenseDao.clearAll()
    }

    /**
     * Loads demo / sample data inside a database transaction.
     * Can be parameterized with project details.
     */
    suspend fun loadSampleData(
        workplaceName: String = "پروژه برج سپهر",
        foremanName: String = "حاج اصغر کریمی",
        employerName: String = "مهندس سعیدی",
        notes: String = "پروژه احداث مجتمع تجاری مسکونی ۲۴ طبقه"
    ): Long = database.withTransaction {
        val todayJalali = JalaliCalendar.todayString()
        val yesterdayJalali = JalaliCalendar.previousDay(todayJalali).first
        val twoDaysAgoJalali = JalaliCalendar.previousDay(yesterdayJalali).first

        // ==========================================
        // WORKPLACE
        // ==========================================
        val folder1Id = folderDao.insertFolder(
            WorkplaceFolderEntity(
                name = workplaceName,
                foremanName = foremanName,
                employerName = employerName,
                colorTag = 0xFFD97706L,
                createdAt = twoDaysAgoJalali,
                notes = notes
            )
        )

        // 2 Working Days (دو روز کاری در پروژه برج سپهر)
        val df1Yesterday = dateFolderDao.insertDateFolder(
            DateFolderEntity(
                folderId = folder1Id,
                date = yesterdayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(yesterdayJalali),
                title = "روز کاری قبل",
                notes = "آرماتوربندی و قالب‌بندی ستون‌ها"
            )
        )

        val df1Today = dateFolderDao.insertDateFolder(
            DateFolderEntity(
                folderId = folder1Id,
                date = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                title = "روز کاری جاری",
                notes = "بتن‌ریزی سقف طبقه پنجم"
            )
        )

        // 5 Workers for پروژه برج سپهر با کمک‌هزینه‌ها و کسورات متنوع
        val w1Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder1Id,
                name = "علی رضایی",
                role = "استادکار بنا",
                phone = "09121112233",
                nationalId = "0012345678",
                baseDailyWage = 1200000L,
                baseHourlyWage = 0L,
                isHourlyEnabled = false,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                isOvertimeEnabled = true,
                overtimeRate = 180000L,
                overtimeHours = 2.0,
                isActive = true,
                notes = "با سابقه و مسلط به دیوارچینی و نماکاری",
                colorTag = 0xFFD97706L,
                transitAllowance = 50000L,
                transitImpact = "ALLOWANCE",
                medicalAllowance = 30000L,
                medicalImpact = "ALLOWANCE"
            )
        )

        val w2Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder1Id,
                name = "حسین مرادی",
                role = "آرماتوربند",
                phone = "09359876543",
                nationalId = "0087654321",
                baseDailyWage = 0L,
                baseHourlyWage = 140000L,
                isHourlyEnabled = true,
                hourlyWageRate = 140000L,
                hourlyHours = 5.0,
                isOvertimeEnabled = true,
                overtimeRate = 160000L,
                overtimeHours = 1.5,
                isActive = true,
                notes = "دقیق در نقشه‌خوانی فونداسیون",
                colorTag = 0xFF0284C7L,
                accommodationAllowance = 80000L,
                accommodationImpact = "DEDUCTION",
                transitAllowance = 30000L,
                transitImpact = "DEDUCTION"
            )
        )

        val w3Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder1Id,
                name = "رضا کریمی",
                role = "کارگر ساده",
                phone = "09194445566",
                nationalId = "0441122334",
                baseDailyWage = 800000L,
                baseHourlyWage = 0L,
                isHourlyEnabled = false,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                isOvertimeEnabled = true,
                overtimeRate = 120000L,
                overtimeHours = 3.0,
                isActive = true,
                notes = "منظم در جابجایی مصالح و نظافت کارگاه",
                colorTag = 0xFF059669L,
                foodAllowance = 60000L,
                foodImpact = "ALLOWANCE",
                transitAllowance = 40000L,
                transitImpact = "ALLOWANCE"
            )
        )

        val w4Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder1Id,
                name = "سعید احمدی",
                role = "جوشکار اسکلت فلزی",
                phone = "09123334455",
                nationalId = "0055667788",
                baseDailyWage = 0L,
                baseHourlyWage = 170000L,
                isHourlyEnabled = true,
                hourlyWageRate = 170000L,
                hourlyHours = 5.0,
                isOvertimeEnabled = true,
                overtimeRate = 200000L,
                overtimeHours = 2.0,
                isActive = true,
                notes = "دارای گواهینامه جوشکاری نفوذی و استاندارد",
                colorTag = 0xFFE11D48L,
                accommodationAllowance = 90000L,
                accommodationImpact = "DEDUCTION",
                foodAllowance = 50000L,
                foodImpact = "ALLOWANCE",
                transitAllowance = 35000L,
                transitImpact = "ALLOWANCE"
            )
        )

        val w5Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder1Id,
                name = "مجتبی بیات",
                role = "تأسیسات و لوله‌کش",
                phone = "09187778899",
                nationalId = "0489988776",
                baseDailyWage = 1250000L,
                baseHourlyWage = 0L,
                isHourlyEnabled = false,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                isOvertimeEnabled = true,
                overtimeRate = 190000L,
                overtimeHours = 1.0,
                isActive = true,
                notes = "مسلط به لوله‌کشی پنج‌لایه و فاضلاب پوش‌فیت",
                colorTag = 0xFF7C3AEDL,
                transitAllowance = 60000L,
                transitImpact = "ALLOWANCE",
                accommodationAllowance = 100000L,
                accommodationImpact = "DEDUCTION",
                medicalAllowance = 25000L,
                medicalImpact = "DEDUCTION"
            )
        )

        // ==========================================
        // ATTENDANCE RECORDS FOR DAY 1: روز کاری قبل (yesterdayJalali) - ۵ نفر
        // ==========================================
        attendanceDao.upsertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w1Id,
                dateFolderId = df1Yesterday,
                date = yesterdayJalali,
                entryTime = "07:30",
                exitTime = "18:00",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 2.0,
                overtimeRate = 180000L,
                dailyWage = 1200000L,
                hourlyWage = 0L,
                transitAllowance = 50000L,
                transitImpact = "ALLOWANCE",
                foodAllowance = 60000L,
                foodImpact = "ALLOWANCE",
                accommodationAllowance = 80000L,
                accommodationImpact = "ALLOWANCE",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        attendanceDao.upsertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w2Id,
                dateFolderId = df1Yesterday,
                date = yesterdayJalali,
                status = AttendanceStatus.HOURLY,
                entryTime = "08:00",
                exitTime = "17:30",
                regularHours = 0.0,
                hourlyHours = 6.0,
                hourlyWageRate = 140000L,
                overtimeHours = 1.5,
                overtimeRate = 160000L,
                dailyWage = 0L,
                hourlyWage = 140000L,
                transitAllowance = 30000L,
                transitImpact = "DEDUCTION",
                foodAllowance = 40000L,
                foodImpact = "ALLOWANCE",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        attendanceDao.upsertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w3Id,
                dateFolderId = df1Yesterday,
                date = yesterdayJalali,
                status = AttendanceStatus.FULL_DAY,
                entryTime = "07:45",
                exitTime = "18:30",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 2.5,
                overtimeRate = 120000L,
                dailyWage = 800000L,
                hourlyWage = 0L,
                foodAllowance = 50000L,
                foodImpact = "ALLOWANCE",
                transitAllowance = 40000L,
                transitImpact = "ALLOWANCE",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        attendanceDao.upsertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w4Id,
                dateFolderId = df1Yesterday,
                date = yesterdayJalali,
                status = AttendanceStatus.HOURLY,
                entryTime = "08:00",
                exitTime = "18:00",
                regularHours = 0.0,
                hourlyHours = 6.0,
                hourlyWageRate = 170000L,
                overtimeHours = 2.0,
                overtimeRate = 200000L,
                dailyWage = 0L,
                hourlyWage = 170000L,
                accommodationAllowance = 90000L,
                accommodationImpact = "DEDUCTION",
                foodAllowance = 50000L,
                foodImpact = "ALLOWANCE",
                transitAllowance = 35000L,
                transitImpact = "ALLOWANCE",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        attendanceDao.upsertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w5Id,
                dateFolderId = df1Yesterday,
                date = yesterdayJalali,
                entryTime = "08:30",
                exitTime = "17:30",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 1.0,
                overtimeRate = 190000L,
                dailyWage = 1250000L,
                hourlyWage = 0L,
                transitAllowance = 60000L,
                transitImpact = "ALLOWANCE",
                accommodationAllowance = 100000L,
                accommodationImpact = "DEDUCTION",
                medicalAllowance = 25000L,
                medicalImpact = "DEDUCTION",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        // ==========================================
        // ATTENDANCE RECORDS FOR DAY 2: روز کاری جاری (todayJalali) - ۵ نفر
        // ==========================================
        attendanceDao.upsertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w1Id,
                dateFolderId = df1Today,
                date = todayJalali,
                entryTime = "07:30",
                exitTime = "18:00",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 2.0,
                overtimeRate = 180000L,
                dailyWage = 1200000L,
                hourlyWage = 0L,
                transitAllowance = 50000L,
                transitImpact = "ALLOWANCE",
                foodAllowance = 60000L,
                foodImpact = "ALLOWANCE",
                accommodationAllowance = 80000L,
                accommodationImpact = "ALLOWANCE",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        attendanceDao.upsertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w2Id,
                dateFolderId = df1Today,
                date = todayJalali,
                status = AttendanceStatus.HOURLY,
                entryTime = "08:00",
                exitTime = "17:30",
                regularHours = 0.0,
                hourlyHours = 5.0,
                hourlyWageRate = 140000L,
                overtimeHours = 1.5,
                overtimeRate = 160000L,
                dailyWage = 0L,
                hourlyWage = 140000L,
                transitAllowance = 30000L,
                transitImpact = "DEDUCTION",
                foodAllowance = 40000L,
                foodImpact = "ALLOWANCE",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        attendanceDao.upsertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w3Id,
                dateFolderId = df1Today,
                date = todayJalali,
                status = AttendanceStatus.FULL_DAY,
                entryTime = "07:45",
                exitTime = "19:00",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 3.0,
                overtimeRate = 120000L,
                dailyWage = 800000L,
                hourlyWage = 0L,
                foodAllowance = 50000L,
                foodImpact = "ALLOWANCE",
                transitAllowance = 40000L,
                transitImpact = "ALLOWANCE",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        attendanceDao.upsertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w4Id,
                dateFolderId = df1Today,
                date = todayJalali,
                status = AttendanceStatus.HOURLY,
                entryTime = "08:00",
                exitTime = "18:00",
                regularHours = 0.0,
                hourlyHours = 5.0,
                hourlyWageRate = 170000L,
                overtimeHours = 2.0,
                overtimeRate = 200000L,
                dailyWage = 0L,
                hourlyWage = 170000L,
                accommodationAllowance = 90000L,
                accommodationImpact = "DEDUCTION",
                foodAllowance = 50000L,
                foodImpact = "ALLOWANCE",
                transitAllowance = 35000L,
                transitImpact = "ALLOWANCE",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        attendanceDao.upsertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w5Id,
                dateFolderId = df1Today,
                date = todayJalali,
                entryTime = "08:15",
                exitTime = "17:15",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 1.0,
                overtimeRate = 190000L,
                dailyWage = 1250000L,
                hourlyWage = 0L,
                transitAllowance = 60000L,
                transitImpact = "ALLOWANCE",
                accommodationAllowance = 100000L,
                accommodationImpact = "DEDUCTION",
                medicalAllowance = 25000L,
                medicalImpact = "DEDUCTION",
                hasCustomAllowances = true,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        folder1Id
    }
}
