package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["expenseId"])
    ]
)
data class ExpenseSplitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val expenseId: String,
    val personName: String,
    val allocatedAmount: Double,
    val percentage: Double = 0.0,
    val isPaid: Boolean = false,
    val settledTimestamp: Long? = null
)
