package com.karyar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkplaceFolderDao {
    @Query("SELECT * FROM workplace_folders ORDER BY id DESC")
    fun getAllFolders(): Flow<List<WorkplaceFolderEntity>>

    @Query("SELECT * FROM workplace_folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): WorkplaceFolderEntity?

    @Query("SELECT * FROM workplace_folders")
    suspend fun getAllFoldersSync(): List<WorkplaceFolderEntity>

    @Upsert
    suspend fun insertFolder(folder: WorkplaceFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: WorkplaceFolderEntity)

    @Delete
    suspend fun deleteFolder(folder: WorkplaceFolderEntity)

    @Query("DELETE FROM workplace_folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Long)

    @Query("DELETE FROM workplace_folders")
    suspend fun clearAll()
}
