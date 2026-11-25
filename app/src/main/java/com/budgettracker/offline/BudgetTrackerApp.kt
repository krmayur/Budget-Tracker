package com.budgettracker.offline

import android.app.Application
import com.budgettracker.offline.data.local.BudgetDatabase
import com.budgettracker.offline.data.repository.BudgetRepository

class BudgetTrackerApp : Application() {

    val database: BudgetDatabase by lazy { BudgetDatabase.getInstance(this) }
    val repository: BudgetRepository by lazy { BudgetRepository(database.budgetDao()) }
}
