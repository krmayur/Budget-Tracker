package com.budgettracker.offline.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val loanType: String,
    val emiAmount: Double,
    val remainingPrincipal: Double = 0.0,
    val interestRateAnnual: Double = 0.0,
    val remainingMonths: Int = 0,
    val isActive: Boolean = true
)
