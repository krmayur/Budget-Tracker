package com.budgettracker.offline.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val monthKey: String,
    val name: String,
    val category: String,
    val amount: Double,
    val isRecurring: Boolean = true,
    val notes: String = ""
)
