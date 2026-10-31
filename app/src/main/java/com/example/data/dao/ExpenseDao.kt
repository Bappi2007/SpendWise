package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesWithCategory(): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp DESC")
    fun getExpensesByRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp DESC")
    suspend fun getExpensesByRangeSync(startMillis: Long, endMillis: Long): List<ExpenseWithCategory>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesWithCategorySync(): List<ExpenseWithCategory>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllRawExpensesSync(): List<ExpenseEntity>

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: String): ExpenseWithCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpensesCount(): Int

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis")
    suspend fun getTotalSpentInRange(startMillis: Long, endMillis: Long): Double
}
