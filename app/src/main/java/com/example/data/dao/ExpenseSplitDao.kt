package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExpenseSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseSplitDao {

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsForExpense(expenseId: String): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpenseSync(expenseId: String): List<ExpenseSplitEntity>

    @Query("SELECT * FROM expense_splits ORDER BY isPaid ASC, personName ASC")
    fun getAllSplits(): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits ORDER BY isPaid ASC, personName ASC")
    suspend fun getAllSplitsSync(): List<ExpenseSplitEntity>

    @Query("SELECT * FROM expense_splits WHERE isPaid = 0 ORDER BY personName ASC")
    fun getPendingSplits(): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE isPaid = 0")
    suspend fun getPendingSplitsSync(): List<ExpenseSplitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: ExpenseSplitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Update
    suspend fun updateSplit(split: ExpenseSplitEntity)

    @Query("UPDATE expense_splits SET isPaid = :isPaid, settledTimestamp = :settledTimestamp WHERE id = :splitId")
    suspend fun setPaidStatus(splitId: String, isPaid: Boolean, settledTimestamp: Long?)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    @Query("DELETE FROM expense_splits WHERE id = :id")
    suspend fun deleteSplitById(id: String)

    @Query("DELETE FROM expense_splits")
    suspend fun deleteAllSplits()
}
