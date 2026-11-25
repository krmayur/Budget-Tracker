package com.budgettracker.offline.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_salaries")
data class MonthlySalaryEntity(
    @PrimaryKey
    val monthKey: String, // Format: YYYY-MM, e.g., 2026-11
    val salaryAmount: Double,
    val otherIncome: Double = 0.0,
    val notes: String = ""
) {
    val totalIncome: Double
        get() = salaryAmount + otherIncome
}
