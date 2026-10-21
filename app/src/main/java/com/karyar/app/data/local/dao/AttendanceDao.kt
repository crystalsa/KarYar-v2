package com.karyar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.karyar.app.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY timestamp DESC, id DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE folderId = :folderId ORDER BY timestamp DESC, id DESC")
    fun getAttendanceByFolder(folderId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE folderId = :folderId")
    suspend fun getAttendanceByFolderSync(folderId: Long): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId ORDER BY timestamp DESC")
    fun getAttendanceForWorker(workerId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId")
    suspend fun getAttendanceForWorkerSync(workerId: Long): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE folderId = :folderId AND date = :date ORDER BY id DESC")
    fun getAttendanceForDateInFolder(folderId: Long, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId AND date = :date LIMIT 1")
    suspend fun getAttendanceForWorkerAndDate(workerId: Long, date: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE dateFolderId = :dateFolderId ORDER BY id ASC")
    fun getAttendanceByDateFolder(dateFolderId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE dateFolderId = :dateFolderId")
    suspend fun getAttendanceByDateFolderSync(dateFolderId: Long): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE dateFolderId = :dateFolderId OR (folderId = :folderId AND date = :date)")
    suspend fun getAttendanceForDateFolderSync(folderId: Long, dateFolderId: Long, date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE folderId = :folderId AND epochDay BETWEEN :startEpoch AND :endEpoch ORDER BY epochDay ASC")
    fun getAttendanceBetweenEpochDays(folderId: Long, startEpoch: Long, endEpoch: Long): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Transaction
    suspend fun upsertAttendance(attendance: AttendanceEntity): Long {
        val existing = getAttendanceForWorkerAndDate(attendance.workerId, attendance.date)
        return if (existing != null) {
            val updated = attendance.copy(id = existing.id)
            updateAttendance(updated)
            existing.id
        } else {
            insertAttendance(attendance)
        }
    }

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE workerId = :workerId AND date = :date")
    suspend fun deleteAttendanceForWorkerAndDate(workerId: Long, date: String)

    @Query("DELETE FROM attendance WHERE folderId = :folderId")
    suspend fun deleteAttendanceByFolder(folderId: Long)

    @Query("DELETE FROM attendance WHERE dateFolderId = :dateFolderId")
    suspend fun deleteAttendanceByDateFolder(dateFolderId: Long)

    @Query("DELETE FROM attendance WHERE dateFolderId = :dateFolderId OR (folderId = :folderId AND date = :date)")
    suspend fun deleteAttendanceForDateFolder(folderId: Long, dateFolderId: Long, date: String)

    @Query(
        """
        UPDATE attendance
        SET
            transitAllowance = :oldTransitAllowance,
            transitImpact = :oldTransitImpact,
            foodAllowance = :oldFoodAllowance,
            foodImpact = :oldFoodImpact,
            accommodationAllowance = :oldAccommodationAllowance,
            accommodationImpact = :oldAccommodationImpact,
            medicalAllowance = :oldMedicalAllowance,
            medicalImpact = :oldMedicalImpact,
            hasCustomAllowances = 1
        WHERE workerId = :workerId AND hasCustomAllowances = 0
        """
    )
    suspend fun freezeLegacyAllowancesForWorker(
        workerId: Long,
        oldTransitAllowance: Long,
        oldTransitImpact: String,
        oldFoodAllowance: Long,
        oldFoodImpact: String,
        oldAccommodationAllowance: Long,
        oldAccommodationImpact: String,
        oldMedicalAllowance: Long,
        oldMedicalImpact: String
    )

    @Query("DELETE FROM attendance")
    suspend fun clearAll()
}
