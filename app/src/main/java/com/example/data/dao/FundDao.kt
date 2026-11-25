package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FundDao {

    @Query("SELECT * FROM funds WHERE id NOT LIKE 'repay_split_%' ORDER BY timestamp DESC")
    fun getAllFunds(): Flow<List<FundEntity>>

    @Query("SELECT * FROM funds WHERE id NOT LIKE 'repay_split_%' ORDER BY timestamp DESC")
    suspend fun getAllFundsSync(): List<FundEntity>

    @Query("SELECT * FROM funds WHERE id NOT LIKE 'repay_split_%' AND timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp DESC")
    suspend fun getFundsInRangeSync(startMillis: Long, endMillis: Long): List<FundEntity>

    @Query("SELECT * FROM funds WHERE id NOT LIKE 'repay_split_%' AND timestamp < :beforeMillis ORDER BY timestamp DESC")
    suspend fun getFundsBeforeSync(beforeMillis: Long): List<FundEntity>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM funds WHERE id NOT LIKE 'repay_split_%'")
    fun getTotalFundsAmount(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM funds WHERE id NOT LIKE 'repay_split_%'")
    suspend fun getTotalFundsAmountSync(): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM funds WHERE id NOT LIKE 'repay_split_%' AND timestamp >= :startMillis AND timestamp <= :endMillis")
    suspend fun getFundsAmountInRange(startMillis: Long, endMillis: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFund(fund: FundEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFunds(funds: List<FundEntity>)

    @Update
    suspend fun updateFund(fund: FundEntity)

    @Delete
    suspend fun deleteFund(fund: FundEntity)

    @Query("DELETE FROM funds WHERE id = :id")
    suspend fun deleteFundById(id: String)

    @Query("DELETE FROM funds")
    suspend fun deleteAllFunds()
}
