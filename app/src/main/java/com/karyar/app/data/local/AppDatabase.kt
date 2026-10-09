package com.karyar.app.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.karyar.app.data.local.converter.Converters
import com.karyar.app.data.local.dao.AttendanceDao
import com.karyar.app.data.local.dao.DateFolderDao
import com.karyar.app.data.local.dao.ExpenseDao
import com.karyar.app.data.local.dao.WorkerDao
import com.karyar.app.data.local.dao.WorkplaceFolderDao
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.DateFolderEntity
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.util.JalaliCalendar

@Database(
    entities = [
        WorkplaceFolderEntity::class,
        DateFolderEntity::class,
        WorkerEntity::class,
        AttendanceEntity::class,
        ExpenseEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): WorkplaceFolderDao
    abstract fun dateFolderDao(): DateFolderDao
    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun addColumnIfNotExists(
            db: SupportSQLiteDatabase,
            table: String,
            column: String,
            typeAndConstraints: String
        ) {
            val cursor = db.query("PRAGMA table_info(`$table`)")
            var exists = false
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1 && cursor.getString(nameIndex).equals(column, ignoreCase = true)) {
                    exists = true
                    break
                }
            }
            cursor.close()
            if (!exists) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $typeAndConstraints")
            }
        }

        private fun ensureAllTablesAndColumns(db: SupportSQLiteDatabase) {
            // Ensure workplace_folders exists and has all columns
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workplace_folders` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `foremanName` TEXT NOT NULL DEFAULT '',
                    `employerName` TEXT NOT NULL DEFAULT '',
                    `colorTag` INTEGER NOT NULL DEFAULT 16086790,
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `notes` TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            addColumnIfNotExists(db, "workplace_folders", "foremanName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "workplace_folders", "employerName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "workplace_folders", "colorTag", "INTEGER NOT NULL DEFAULT 16086790")
            addColumnIfNotExists(db, "workplace_folders", "createdAt", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "workplace_folders", "notes", "TEXT NOT NULL DEFAULT ''")

            // Ensure date_folders exists and has all columns
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `date_folders` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `folderId` INTEGER NOT NULL,
                    `date` TEXT NOT NULL,
                    `dayOfWeek` TEXT NOT NULL,
                    `title` TEXT NOT NULL DEFAULT '',
                    `notes` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`folderId`) REFERENCES `workplace_folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            addColumnIfNotExists(db, "date_folders", "title", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "date_folders", "notes", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "date_folders", "createdAt", "INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_date_folders_folderId` ON `date_folders` (`folderId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_date_folders_folderId_date` ON `date_folders` (`folderId`, `date`)")

            // Ensure workers exists and has all columns
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workers` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `folderId` INTEGER NOT NULL DEFAULT 0,
                    `dateFolderId` INTEGER NOT NULL DEFAULT 0,
                    `workDate` TEXT NOT NULL DEFAULT '',
                    `dayOfWeek` TEXT NOT NULL DEFAULT '',
                    `name` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `phone` TEXT NOT NULL DEFAULT '',
                    `nationalId` TEXT NOT NULL DEFAULT '',
                    `baseDailyWage` INTEGER NOT NULL DEFAULT 0,
                    `baseHourlyWage` INTEGER NOT NULL DEFAULT 0,
                    `isHourlyEnabled` INTEGER NOT NULL DEFAULT 0,
                    `hourlyWageRate` INTEGER NOT NULL DEFAULT 0,
                    `hourlyHours` REAL NOT NULL DEFAULT 0.0,
                    `isOvertimeEnabled` INTEGER NOT NULL DEFAULT 0,
                    `overtimeRate` INTEGER NOT NULL DEFAULT 0,
                    `overtimeHours` REAL NOT NULL DEFAULT 0.0,
                    `isActive` INTEGER NOT NULL DEFAULT 1,
                    `notes` TEXT NOT NULL DEFAULT '',
                    `colorTag` INTEGER NOT NULL DEFAULT 16086790,
                    `transitAllowance` INTEGER NOT NULL DEFAULT 0,
                    `transitImpact` TEXT NOT NULL DEFAULT 'ALLOWANCE',
                    `foodAllowance` INTEGER NOT NULL DEFAULT 0,
                    `foodImpact` TEXT NOT NULL DEFAULT 'ALLOWANCE',
                    `accommodationAllowance` INTEGER NOT NULL DEFAULT 0,
                    `accommodationImpact` TEXT NOT NULL DEFAULT 'ALLOWANCE',
                    `medicalAllowance` INTEGER NOT NULL DEFAULT 0,
                    `medicalImpact` TEXT NOT NULL DEFAULT 'ALLOWANCE',
                    `createdAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            addColumnIfNotExists(db, "workers", "dateFolderId", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "workers", "workDate", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "workers", "dayOfWeek", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "workers", "phone", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "workers", "nationalId", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "workers", "baseHourlyWage", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "workers", "isHourlyEnabled", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "workers", "hourlyWageRate", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "workers", "hourlyHours", "REAL NOT NULL DEFAULT 0.0")
            addColumnIfNotExists(db, "workers", "isOvertimeEnabled", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "workers", "overtimeRate", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "workers", "overtimeHours", "REAL NOT NULL DEFAULT 0.0")
            addColumnIfNotExists(db, "workers", "isActive", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfNotExists(db, "workers", "notes", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "workers", "colorTag", "INTEGER NOT NULL DEFAULT 16086790")
            addColumnIfNotExists(db, "workers", "transitAllowance", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "workers", "transitImpact", "TEXT NOT NULL DEFAULT 'ALLOWANCE'")
            addColumnIfNotExists(db, "workers", "foodAllowance", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "workers", "foodImpact", "TEXT NOT NULL DEFAULT 'ALLOWANCE'")
            addColumnIfNotExists(db, "workers", "accommodationAllowance", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "workers", "accommodationImpact", "TEXT NOT NULL DEFAULT 'ALLOWANCE'")
            addColumnIfNotExists(db, "workers", "medicalAllowance", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "workers", "medicalImpact", "TEXT NOT NULL DEFAULT 'ALLOWANCE'")
            addColumnIfNotExists(db, "workers", "createdAt", "INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workers_folderId` ON `workers` (`folderId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workers_dateFolderId` ON `workers` (`dateFolderId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workers_workDate` ON `workers` (`workDate`)")

            // Ensure attendance exists and has all columns
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `attendance` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `folderId` INTEGER NOT NULL DEFAULT 0,
                    `workerId` INTEGER NOT NULL,
                    `date` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL DEFAULT 0,
                    `entryTime` TEXT NOT NULL DEFAULT '08:00',
                    `exitTime` TEXT NOT NULL DEFAULT '17:00',
                    `regularHours` REAL NOT NULL DEFAULT 8.0,
                    `overtimeHours` REAL NOT NULL DEFAULT 0.0,
                    `overtimeRate` INTEGER NOT NULL DEFAULT 0,
                    `hourlyWageRate` INTEGER NOT NULL DEFAULT 0,
                    `hourlyHours` REAL NOT NULL DEFAULT 0.0,
                    `dailyWage` INTEGER NOT NULL DEFAULT 0,
                    `hourlyWage` INTEGER NOT NULL DEFAULT 0,
                    `workplaceName` TEXT NOT NULL DEFAULT '',
                    `employerName` TEXT NOT NULL DEFAULT '',
                    `foremanName` TEXT NOT NULL DEFAULT '',
                    `notes` TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            addColumnIfNotExists(db, "attendance", "overtimeHours", "REAL NOT NULL DEFAULT 0.0")
            addColumnIfNotExists(db, "attendance", "overtimeRate", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "attendance", "hourlyWageRate", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "attendance", "hourlyHours", "REAL NOT NULL DEFAULT 0.0")
            addColumnIfNotExists(db, "attendance", "dailyWage", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "attendance", "hourlyWage", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "attendance", "workplaceName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "attendance", "employerName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "attendance", "foremanName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "attendance", "notes", "TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_workerId` ON `attendance` (`workerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_folderId` ON `attendance` (`folderId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_date` ON `attendance` (`date`)")

            // Ensure expenses exists and has all columns
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `expenses` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `folderId` INTEGER NOT NULL DEFAULT 0,
                    `title` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `scope` TEXT NOT NULL,
                    `impactType` TEXT NOT NULL DEFAULT 'DEDUCTION',
                    `workerId` INTEGER DEFAULT NULL,
                    `workerName` TEXT DEFAULT NULL,
                    `amount` INTEGER NOT NULL,
                    `accommodationDays` INTEGER NOT NULL DEFAULT 0,
                    `date` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL DEFAULT 0,
                    `workplaceName` TEXT NOT NULL DEFAULT '',
                    `employerName` TEXT NOT NULL DEFAULT '',
                    `foremanName` TEXT NOT NULL DEFAULT '',
                    `notes` TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            addColumnIfNotExists(db, "expenses", "accommodationDays", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "expenses", "workplaceName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "expenses", "employerName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "expenses", "foremanName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "expenses", "notes", "TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_folderId` ON `expenses` (`folderId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_workerId` ON `expenses` (`workerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)")
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Calculate epochDay for all distinct dates
                val dateCursor = db.query("SELECT DISTINCT `date` FROM `date_folders` UNION SELECT DISTINCT `date` FROM `attendance` UNION SELECT DISTINCT `date` FROM `expenses`")
                val dateEpochMap = mutableMapOf<String, Long>()
                while (dateCursor.moveToNext()) {
                    val dateStr = dateCursor.getString(0)
                    if (!dateStr.isNullOrBlank()) {
                        dateEpochMap[dateStr] = JalaliCalendar.toEpochDay(dateStr)
                    }
                }
                dateCursor.close()

                // 2. Deduplicate attendance rows before creating unique index on (workerId, date).
                // Keep the record with the latest timestamp (tie-break with max id).
                db.execSQL("""
                    DELETE FROM `attendance`
                    WHERE `id` IN (
                        SELECT a1.id
                        FROM `attendance` a1
                        JOIN `attendance` a2 ON a1.workerId = a2.workerId AND a1.date = a2.date
                        WHERE (a1.timestamp < a2.timestamp) OR (a1.timestamp = a2.timestamp AND a1.id < a2.id)
                    )
                """.trimIndent())

                // 3. Recreate workers table (without dateFolderId, workDate, dayOfWeek; with ForeignKey to workplace_folders)
                // RULE: Worker identity is strictly worker.id. NO worker merging across names or folders.
                db.execSQL("""
                    CREATE TABLE `workers_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `folderId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `phone` TEXT NOT NULL DEFAULT '',
                        `nationalId` TEXT NOT NULL DEFAULT '',
                        `baseDailyWage` INTEGER NOT NULL DEFAULT 0,
                        `baseHourlyWage` INTEGER NOT NULL DEFAULT 0,
                        `isHourlyEnabled` INTEGER NOT NULL DEFAULT 0,
                        `hourlyWageRate` INTEGER NOT NULL DEFAULT 0,
                        `hourlyHours` REAL NOT NULL DEFAULT 0.0,
                        `isOvertimeEnabled` INTEGER NOT NULL DEFAULT 0,
                        `overtimeRate` INTEGER NOT NULL DEFAULT 0,
                        `overtimeHours` REAL NOT NULL DEFAULT 0.0,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `colorTag` INTEGER NOT NULL DEFAULT 16086790,
                        `transitAllowance` INTEGER NOT NULL DEFAULT 0,
                        `transitImpact` TEXT NOT NULL DEFAULT 'ALLOWANCE',
                        `foodAllowance` INTEGER NOT NULL DEFAULT 0,
                        `foodImpact` TEXT NOT NULL DEFAULT 'ALLOWANCE',
                        `accommodationAllowance` INTEGER NOT NULL DEFAULT 0,
                        `accommodationImpact` TEXT NOT NULL DEFAULT 'ALLOWANCE',
                        `medicalAllowance` INTEGER NOT NULL DEFAULT 0,
                        `medicalImpact` TEXT NOT NULL DEFAULT 'ALLOWANCE',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`folderId`) REFERENCES `workplace_folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // Copy all existing workers preserving their exact ids
                db.execSQL("""
                    INSERT INTO `workers_new` (
                        `id`, `folderId`, `name`, `role`, `phone`, `nationalId`, `baseDailyWage`, `baseHourlyWage`,
                        `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`, `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`,
                        `isActive`, `notes`, `colorTag`, `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                        `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
                    )
                    SELECT 
                        `id`, `folderId`, `name`, `role`, `phone`, `nationalId`, `baseDailyWage`, `baseHourlyWage`,
                        `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`, `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`,
                        `isActive`, `notes`, `colorTag`, `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                        `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
                    FROM `workers`
                """.trimIndent())

                db.execSQL("DROP TABLE `workers`")
                db.execSQL("ALTER TABLE `workers_new` RENAME TO `workers`")
                db.execSQL("CREATE INDEX `index_workers_folderId` ON `workers` (`folderId`)")

                // 4. Recreate date_folders table with epochDay
                db.execSQL("""
                    CREATE TABLE `date_folders_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `folderId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `dayOfWeek` TEXT NOT NULL,
                        `title` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `epochDay` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`folderId`) REFERENCES `workplace_folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `date_folders_new` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`, `epochDay`)
                    SELECT `id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`, 0
                    FROM `date_folders`
                """.trimIndent())

                for ((dateStr, epoch) in dateEpochMap) {
                    db.execSQL("UPDATE `date_folders_new` SET `epochDay` = ? WHERE `date` = ?", arrayOf<Any>(epoch, dateStr))
                }

                db.execSQL("DROP TABLE `date_folders`")
                db.execSQL("ALTER TABLE `date_folders_new` RENAME TO `date_folders`")
                db.execSQL("CREATE INDEX `index_date_folders_folderId` ON `date_folders` (`folderId`)")
                db.execSQL("CREATE INDEX `index_date_folders_folderId_date` ON `date_folders` (`folderId`, `date`)")
                db.execSQL("CREATE INDEX `index_date_folders_epochDay` ON `date_folders` (`epochDay`)")

                // 5. Recreate attendance table with status, dateFolderId, epochDay, foreign keys and unique index
                db.execSQL("""
                    CREATE TABLE `attendance_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `folderId` INTEGER NOT NULL,
                        `workerId` INTEGER NOT NULL,
                        `dateFolderId` INTEGER NOT NULL DEFAULT 0,
                        `date` TEXT NOT NULL,
                        `epochDay` INTEGER NOT NULL DEFAULT 0,
                        `status` TEXT NOT NULL DEFAULT 'FULL_DAY',
                        `timestamp` INTEGER NOT NULL DEFAULT 0,
                        `entryTime` TEXT NOT NULL DEFAULT '08:00',
                        `exitTime` TEXT NOT NULL DEFAULT '17:00',
                        `regularHours` REAL NOT NULL DEFAULT 8.0,
                        `overtimeHours` REAL NOT NULL DEFAULT 0.0,
                        `overtimeRate` INTEGER NOT NULL DEFAULT 0,
                        `hourlyWageRate` INTEGER NOT NULL DEFAULT 0,
                        `hourlyHours` REAL NOT NULL DEFAULT 0.0,
                        `dailyWage` INTEGER NOT NULL DEFAULT 0,
                        `hourlyWage` INTEGER NOT NULL DEFAULT 0,
                        `workplaceName` TEXT NOT NULL DEFAULT '',
                        `employerName` TEXT NOT NULL DEFAULT '',
                        `foremanName` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`folderId`) REFERENCES `workplace_folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `attendance_new` (
                        `id`, `folderId`, `workerId`, `dateFolderId`, `date`, `epochDay`, `status`, `timestamp`,
                        `entryTime`, `exitTime`, `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`,
                        `hourlyHours`, `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
                    )
                    SELECT 
                        `id`, `folderId`, `workerId`, 0, `date`, 0, 'FULL_DAY', `timestamp`,
                        `entryTime`, `exitTime`, `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`,
                        `hourlyHours`, `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
                    FROM `attendance`
                """.trimIndent())

                // Link dateFolderId in attendance. If no matching date_folder exists for (folderId, date), create one so it's not lost
                val orphanDatesCursor = db.query("SELECT DISTINCT `folderId`, `date` FROM `attendance_new` WHERE `folderId` IN (SELECT `id` FROM `workplace_folders`) AND NOT EXISTS (SELECT 1 FROM `date_folders` df WHERE df.folderId = `attendance_new`.folderId AND df.date = `attendance_new`.date)")
                val orphanDates = mutableListOf<Pair<Long, String>>()
                while (orphanDatesCursor.moveToNext()) {
                    val fId = orphanDatesCursor.getLong(0)
                    val dStr = orphanDatesCursor.getString(1)
                    if (!dStr.isNullOrBlank()) {
                        orphanDates.add(fId to dStr)
                    }
                }
                orphanDatesCursor.close()

                for ((fId, dStr) in orphanDates) {
                    val dayOfWeek = JalaliCalendar.getDayOfWeek(dStr)
                    val epoch = JalaliCalendar.toEpochDay(dStr)
                    db.execSQL(
                        "INSERT INTO `date_folders` (`folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`, `epochDay`) VALUES (?, ?, ?, 'روز کاری', '', ?, ?)",
                        arrayOf<Any>(fId, dStr, dayOfWeek, System.currentTimeMillis(), epoch)
                    )
                }

                db.execSQL("""
                    UPDATE `attendance_new`
                    SET `dateFolderId` = COALESCE(
                        (SELECT df.id FROM `date_folders` df WHERE df.folderId = `attendance_new`.folderId AND df.date = `attendance_new`.date LIMIT 1),
                        COALESCE((SELECT df2.id FROM `date_folders` df2 WHERE df2.folderId = `attendance_new`.folderId ORDER BY df2.id DESC LIMIT 1), 0)
                    )
                """.trimIndent())

                // Fix any attendance rows where dateFolderId is still 0 (create date_folder or connect to nearest/latest day)
                val unresolvedCursor = db.query("""
                    SELECT DISTINCT `folderId`, `date` 
                    FROM `attendance_new` 
                    WHERE `dateFolderId` <= 0 AND `folderId` IN (SELECT `id` FROM `workplace_folders`)
                """.trimIndent())
                val unresolvedDates = mutableListOf<Pair<Long, String>>()
                while (unresolvedCursor.moveToNext()) {
                    val fId = unresolvedCursor.getLong(0)
                    val dStr = unresolvedCursor.getString(1) ?: JalaliCalendar.todayString()
                    unresolvedDates.add(fId to dStr)
                }
                unresolvedCursor.close()

                for ((fId, dStr) in unresolvedDates) {
                    val effectiveDate = if (dStr.isNotBlank()) dStr else JalaliCalendar.todayString()
                    val dayOfWeek = JalaliCalendar.getDayOfWeek(effectiveDate)
                    val epoch = JalaliCalendar.toEpochDay(effectiveDate)
                    db.execSQL(
                        "INSERT INTO `date_folders` (`folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`, `epochDay`) VALUES (?, ?, ?, 'روز کاری', '', ?, ?)",
                        arrayOf<Any>(fId, effectiveDate, dayOfWeek, System.currentTimeMillis(), epoch)
                    )
                }

                db.execSQL("""
                    UPDATE `attendance_new`
                    SET `dateFolderId` = COALESCE(
                        (SELECT df.id FROM `date_folders` df WHERE df.folderId = `attendance_new`.folderId AND df.date = `attendance_new`.date LIMIT 1),
                        (SELECT df2.id FROM `date_folders` df2 WHERE df2.folderId = `attendance_new`.folderId ORDER BY df2.id DESC LIMIT 1)
                    )
                    WHERE `dateFolderId` <= 0 AND `folderId` IN (SELECT `id` FROM `workplace_folders`)
                """.trimIndent())

                // Convert status with single CASE WHEN statement respecting explicit priorities
                db.execSQL("""
                    UPDATE `attendance_new`
                    SET `status` = CASE
                        WHEN `notes` LIKE '%غیبت%' THEN 'ABSENT'
                        WHEN `hourlyHours` > 0.0 OR `notes` LIKE '%ساعتی%' THEN 'HOURLY'
                        WHEN `notes` LIKE '%نصف روز%' OR `regularHours` = 4.0 THEN 'HALF_DAY'
                        WHEN `regularHours` = 0.0 AND `hourlyHours` = 0.0 THEN 'ABSENT'
                        ELSE 'FULL_DAY'
                    END
                """.trimIndent())
                db.execSQL("UPDATE `attendance_new` SET `notes` = '' WHERE `notes` IN ('تمام روز', 'نصف روز', 'ساعتی', 'غیبت')")

                for ((dateStr, epoch) in dateEpochMap) {
                    db.execSQL("UPDATE `attendance_new` SET `epochDay` = ? WHERE `date` = ?", arrayOf<Any>(epoch, dateStr))
                }

                db.execSQL("DROP TABLE `attendance`")
                db.execSQL("ALTER TABLE `attendance_new` RENAME TO `attendance`")
                db.execSQL("CREATE INDEX `index_attendance_folderId` ON `attendance` (`folderId`)")
                db.execSQL("CREATE INDEX `index_attendance_workerId` ON `attendance` (`workerId`)")
                db.execSQL("CREATE INDEX `index_attendance_dateFolderId` ON `attendance` (`dateFolderId`)")
                db.execSQL("CREATE INDEX `index_attendance_date` ON `attendance` (`date`)")
                db.execSQL("CREATE INDEX `index_attendance_epochDay` ON `attendance` (`epochDay`)")
                db.execSQL("CREATE UNIQUE INDEX `index_attendance_workerId_date` ON `attendance` (`workerId`, `date`)")

                // 6. Recreate expenses table with ForeignKey and epochDay
                db.execSQL("""
                    CREATE TABLE `expenses_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `folderId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `scope` TEXT NOT NULL,
                        `impactType` TEXT NOT NULL DEFAULT 'DEDUCTION',
                        `workerId` INTEGER DEFAULT NULL,
                        `workerName` TEXT DEFAULT NULL,
                        `amount` INTEGER NOT NULL DEFAULT 0,
                        `accommodationDays` INTEGER NOT NULL DEFAULT 0,
                        `date` TEXT NOT NULL,
                        `epochDay` INTEGER NOT NULL DEFAULT 0,
                        `timestamp` INTEGER NOT NULL DEFAULT 0,
                        `workplaceName` TEXT NOT NULL DEFAULT '',
                        `employerName` TEXT NOT NULL DEFAULT '',
                        `foremanName` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`folderId`) REFERENCES `workplace_folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `expenses_new` (
                        `id`, `folderId`, `title`, `category`, `scope`, `impactType`, `workerId`, `workerName`,
                        `amount`, `accommodationDays`, `date`, `epochDay`, `timestamp`, `workplaceName`, `employerName`, `foremanName`, `notes`
                    )
                    SELECT 
                        `id`, `folderId`, `title`, `category`, `scope`, `impactType`, `workerId`, `workerName`,
                        `amount`, `accommodationDays`, `date`, 0, `timestamp`, `workplaceName`, `employerName`, `foremanName`, `notes`
                    FROM `expenses`
                """.trimIndent())

                for ((dateStr, epoch) in dateEpochMap) {
                    db.execSQL("UPDATE `expenses_new` SET `epochDay` = ? WHERE `date` = ?", arrayOf<Any>(epoch, dateStr))
                }

                db.execSQL("DROP TABLE `expenses`")
                db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
                db.execSQL("CREATE INDEX `index_expenses_folderId` ON `expenses` (`folderId`)")
                db.execSQL("CREATE INDEX `index_expenses_workerId` ON `expenses` (`workerId`)")
                db.execSQL("CREATE INDEX `index_expenses_date` ON `expenses` (`date`)")
                db.execSQL("CREATE INDEX `index_expenses_epochDay` ON `expenses` (`epochDay`)")

                // 7. Check and enforce foreign key integrity
                val fkCursor = db.query("PRAGMA foreign_key_check")
                val fkViolations = mutableListOf<Pair<String, Long>>()
                while (fkCursor.moveToNext()) {
                    val table = fkCursor.getString(0)
                    val rowid = fkCursor.getLong(1)
                    fkViolations.add(table to rowid)
                }
                fkCursor.close()

                if (fkViolations.isNotEmpty()) {
                    val summary = fkViolations.groupBy { it.first }.map { "${it.key}: ${it.value.size}" }.joinToString(", ")
                    Log.w("AppDatabase", "MIGRATION_5_6: Removing ${fkViolations.size} orphan row(s) violating foreign keys ($summary).")
                }

                for ((table, rowid) in fkViolations) {
                    db.execSQL("DELETE FROM `$table` WHERE rowid = ?", arrayOf<Any>(rowid))
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "worker_management_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON;")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

