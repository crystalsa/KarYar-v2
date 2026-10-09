package com.karyar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.karyar.app.data.local.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY id DESC")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE folderId = :folderId ORDER BY id DESC")
    fun getWorkersByFolder(folderId: Long): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE folderId = :folderId")
    suspend fun getWorkersByFolderSync(folderId: Long): List<WorkerEntity>

    @Query("SELECT * FROM workers WHERE id = :id")
    fun getWorkerById(id: Long): Flow<WorkerEntity?>

    @Query("SELECT * FROM workers WHERE folderId = :folderId AND isActive = 1 ORDER BY name ASC")
    fun getActiveWorkersByFolder(folderId: Long): Flow<List<WorkerEntity>>

    @Upsert
    suspend fun insertWorker(worker: WorkerEntity): Long

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Query("""
        SELECT DISTINCT w.* FROM workers w 
        INNER JOIN attendance a ON w.id = a.workerId 
        WHERE a.dateFolderId = :dateFolderId 
        ORDER BY w.id DESC
    """)
    fun getWorkersByDateFolder(dateFolderId: Long): Flow<List<WorkerEntity>>

    @Delete
    suspend fun deleteWorker(worker: WorkerEntity)

    @Query("DELETE FROM workers WHERE folderId = :folderId")
    suspend fun deleteWorkersByFolder(folderId: Long)

    @Query("DELETE FROM workers")
    suspend fun clearAll()
}
