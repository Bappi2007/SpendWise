package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "funds")
data class FundEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val source: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)
