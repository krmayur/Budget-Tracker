package com.budgettracker.offline.data.repository

import com.budgettracker.offline.data.local.BudgetDao
import com.budgettracker.offline.data.local.ExpenseEntity
import com.budgettracker.offline.data.local.InvestmentEntity
import com.budgettracker.offline.data.local.LoanEntity
import com.budgettracker.offline.data.local.MonthlySalaryEntity
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val dao: BudgetDao) {

    fun observeSalary(monthKey: String): Flow<MonthlySalaryEntity?> =
        dao.observeSalaryForMonth(monthKey)

    suspend fun saveSalary(monthKey: String, salaryAmount: Double, otherIncome: Double, notes: String) {
        dao.upsertSalary(
            MonthlySalaryEntity(
                monthKey = monthKey,
                salaryAmount = salaryAmount,
                otherIncome = otherIncome,
                notes = notes.trim()
            )
        )
    }

    fun observeLoans(): Flow<List<LoanEntity>> = dao.observeAllLoans()

    suspend fun saveLoan(loan: LoanEntity) {
        if (loan.id == 0L) {
            dao.insertLoan(loan)
        } else {
            dao.updateLoan(loan)
        }
    }

    suspend fun deleteLoan(loan: LoanEntity) = dao.deleteLoan(loan)

    fun observeInvestments(monthKey: String): Flow<List<InvestmentEntity>> =
        dao.observeInvestmentsForMonth(monthKey)

    suspend fun saveInvestment(investment: InvestmentEntity) {
        if (investment.id == 0L) {
            dao.insertInvestment(investment)
        } else {
            dao.updateInvestment(investment)
        }
    }

    suspend fun deleteInvestment(investment: InvestmentEntity) = dao.deleteInvestment(investment)

    fun observeExpenses(monthKey: String): Flow<List<ExpenseEntity>> =
        dao.observeExpensesForMonth(monthKey)

    suspend fun saveExpense(expense: ExpenseEntity) {
        if (expense.id == 0L) {
            dao.insertExpense(expense)
        } else {
            dao.updateExpense(expense)
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = dao.deleteExpense(expense)

    /**
     * Copies salary and recurring investments from the previous month into the target month
     * so users do not have to re-enter fixed monthly items manually.
     */
    suspend fun copyRecurringFromPreviousMonth(previousMonthKey: String, targetMonthKey: String): Int {
        var copiedCount = 0
        val currentSalary = dao.getSalaryForMonth(targetMonthKey)
        if (currentSalary == null) {
            val prevSalary = dao.getSalaryForMonth(previousMonthKey)
            if (prevSalary != null) {
                dao.upsertSalary(prevSalary.copy(monthKey = targetMonthKey))
                copiedCount++
            }
        }
        val prevRecurring = dao.getRecurringInvestmentsForMonth(previousMonthKey)
        for (item in prevRecurring) {
            dao.insertInvestment(item.copy(id = 0L, monthKey = targetMonthKey))
            copiedCount++
        }
        return copiedCount
    }
}
