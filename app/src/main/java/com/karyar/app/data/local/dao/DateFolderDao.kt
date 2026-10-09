package com.karyar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.karyar.app.data.local.entity.DateFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DateFolderDao {
    @Query("SELECT * FROM date_folders WHERE folderId = :folderId ORDER BY epochDay ASC, date ASC, id ASC")
    fun getDateFoldersByFolder(folderId: Long): Flow<List<DateFolderEntity>>

    @Query("SELECT * FROM date_folders WHERE folderId = :folderId ORDER BY epochDay ASC, date ASC, id ASC")
    suspend fun getDateFoldersByFolderSync(folderId: Long): List<DateFolderEntity>

    @Query("SELECT * FROM date_folders WHERE id = :id")
    suspend fun getDateFolderById(id: Long): DateFolderEntity?

    @Query("SELECT * FROM date_folders WHERE folderId = :folderId AND date = :date LIMIT 1")
    suspend fun getDateFolderByDate(folderId: Long, date: String): DateFolderEntity?

    @Upsert
    suspend fun insertDateFolder(dateFolder: DateFolderEntity): Long

    @Update
    suspend fun updateDateFolder(dateFolder: DateFolderEntity)

    @Delete
    suspend fun deleteDateFolder(dateFolder: DateFolderEntity)

    @Query("DELETE FROM date_folders WHERE folderId = :folderId")
    suspend fun deleteDateFoldersByFolder(folderId: Long)

    @Query("DELETE FROM date_folders WHERE id = :id")
    suspend fun deleteDateFolderById(id: Long)

    @Query("DELETE FROM date_folders")
    suspend fun clearAll()
}
