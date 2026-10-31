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
    val category: CategoryEntity?
) {
    val categoryName: String
        get() = category?.name ?: "Other"

    val categoryColorHex: Long
        get() = category?.colorHex ?: 0xFF64748B

    val categoryIconKey: String
        get() = category?.iconKey ?: "other"
}
