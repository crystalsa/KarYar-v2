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
import com.karyar.app.util.JalaliCalendar
import com.karyar.app.util.WageCalculator
import kotlinx.coroutines.flow.Flow

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
    fun getWorkersByDateFolder(dateFolderId: Long): Flow<List<WorkerEntity>> = workerDao.getWorkersByDateFolder(dateFolderId)
    fun getAttendanceByFolder(folderId: Long): Flow<List<AttendanceEntity>> = attendanceDao.getAttendanceByFolder(folderId)
    fun getExpensesByFolder(folderId: Long): Flow<List<ExpenseEntity>> = expenseDao.getExpensesByFolder(folderId)

    suspend fun insertFolder(folder: WorkplaceFolderEntity): Long = folderDao.insertFolder(folder)
    suspend fun updateFolder(folder: WorkplaceFolderEntity) = folderDao.updateFolder(folder)
    suspend fun getFolderById(folderId: Long): WorkplaceFolderEntity? = folderDao.getFolderById(folderId)

    suspend fun insertDateFolder(dateFolder: DateFolderEntity): Long = dateFolderDao.insertDateFolder(dateFolder)
    suspend fun updateDateFolder(dateFolder: DateFolderEntity) = dateFolderDao.updateDateFolder(dateFolder)
    suspend fun getDateFolderByDate(folderId: Long, date: String): DateFolderEntity? = dateFolderDao.getDateFolderByDate(folderId, date)
    suspend fun deleteDateFolder(dateFolder: DateFolderEntity) = database.withTransaction {
        attendanceDao.deleteAttendanceByDateFolder(dateFolder.id)
        dateFolderDao.deleteDateFolder(dateFolder)
    }

    suspend fun deleteFolder(folder: WorkplaceFolderEntity) = database.withTransaction {
        folderDao.deleteFolder(folder)
    }

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
        for (df in dateFolders) {
            val newDfId = dateFolderDao.insertDateFolder(df.copy(id = 0, folderId = newFolderId))
            dateFolderIdMap[df.id] = newDfId
        }

        val attendances = attendanceDao.getAttendanceByFolderSync(folder.id)
        for (att in attendances) {
            val mappedWorkerId = workerIdMap[att.workerId] ?: continue
            val mappedDateFolderId = dateFolderIdMap[att.dateFolderId] ?: 0L
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
        val newDfId = dateFolderDao.insertDateFolder(
            DateFolderEntity(
                folderId = folderId,
                date = date,
                dayOfWeek = dayOfWeek,
                title = title,
                notes = notes,
                epochDay = JalaliCalendar.toEpochDay(date)
            )
        )
        val activeWorkers = workerDao.getWorkersByFolderSync(folderId).filter { it.isActive }
        for (w in activeWorkers) {
            attendanceDao.insertAttendance(
                WageCalculator.defaultAttendanceFor(
                    worker = w,
                    date = date,
                    dateFolderId = newDfId,
                    folderId = folderId
                )
            )
        }
        newDfId
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
    suspend fun updateWorker(worker: WorkerEntity) = workerDao.updateWorker(worker)
    suspend fun deleteWorker(worker: WorkerEntity) = workerDao.deleteWorker(worker)

    suspend fun insertAttendance(attendance: AttendanceEntity): Long = attendanceDao.upsertAttendance(attendance)
    suspend fun updateAttendance(attendance: AttendanceEntity) = attendanceDao.updateAttendance(attendance)
    suspend fun deleteAttendance(attendance: AttendanceEntity) = attendanceDao.deleteAttendance(attendance)

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
        val yesterdayJalali = JalaliCalendar.fromTimestamp(System.currentTimeMillis() - 86400000L).format()
        val twoDaysAgoJalali = JalaliCalendar.fromTimestamp(System.currentTimeMillis() - 172800000L).format()

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
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = ""
            )
        )

        folder1Id
    }
}
