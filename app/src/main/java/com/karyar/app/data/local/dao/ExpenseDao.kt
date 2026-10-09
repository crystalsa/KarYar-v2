package com.karyar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.karyar.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE folderId = :folderId ORDER BY timestamp DESC, id DESC")
    fun getExpensesByFolder(folderId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE folderId = :folderId")
    suspend fun getExpensesByFolderSync(folderId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE folderId = :folderId AND workerId = :workerId ORDER BY timestamp DESC")
    fun getExpensesForWorker(folderId: Long, workerId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE folderId = :folderId AND scope = 'GROUP' ORDER BY timestamp DESC")
    fun getGroupExpensesByFolder(folderId: Long): Flow<List<ExpenseEntity>>

    @Upsert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE folderId = :folderId")
    suspend fun deleteExpensesByFolder(folderId: Long)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}
