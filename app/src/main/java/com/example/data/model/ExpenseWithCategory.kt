package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ExpenseWithCategory(
    @Embedded
    val expense: ExpenseEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "expenseId"
    )
    val splits: List<ExpenseSplitEntity> = emptyList()
) {
    val categoryName: String
        get() = category?.name ?: "Other"

    val categoryColorHex: Long
        get() = category?.colorHex ?: 0xFF64748B

    val categoryIconKey: String
        get() = category?.iconKey ?: "other"

    val isSplit: Boolean
        get() = splits.isNotEmpty()

    val settledSplitsCount: Int
        get() = splits.count { it.isPaid }

    val pendingSplitsCount: Int
        get() = splits.count { !it.isPaid }

    val totalAllocatedToFriends: Double
        get() = splits.sumOf { it.allocatedAmount }

    val pendingAmountToReceive: Double
        get() = splits.filter { !it.isPaid }.sumOf { it.allocatedAmount }

    val myShare: Double
        get() = (expense.amount - totalAllocatedToFriends).coerceAtLeast(0.0)
}
