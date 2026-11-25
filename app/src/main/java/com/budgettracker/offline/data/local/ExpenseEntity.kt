package com.budgettracker.offline.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val monthKey: String,
    val title: String,
    val category: String,
    val amount: Double,
    val isEssential: Boolean = false,
    val dayOfMonth: Int = 1,
    val notes: String = ""
)
