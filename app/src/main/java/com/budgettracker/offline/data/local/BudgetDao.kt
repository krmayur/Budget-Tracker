package com.budgettracker.offline.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // Salary
    @Query("SELECT * FROM monthly_salaries WHERE monthKey = :monthKey LIMIT 1")
    fun observeSalaryForMonth(monthKey: String): Flow<MonthlySalaryEntity?>

    @Query("SELECT * FROM monthly_salaries WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getSalaryForMonth(monthKey: String): MonthlySalaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSalary(salary: MonthlySalaryEntity)

    // Loans (global active/inactive list so user manages multiple ongoing EMIs)
    @Query("SELECT * FROM loans ORDER BY isActive DESC, emiAmount DESC")
    fun observeAllLoans(): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    // Investments
    @Query("SELECT * FROM investments WHERE monthKey = :monthKey ORDER BY amount DESC, id DESC")
    fun observeInvestmentsForMonth(monthKey: String): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE monthKey = :monthKey AND isRecurring = 1")
    suspend fun getRecurringInvestmentsForMonth(monthKey: String): List<InvestmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: InvestmentEntity): Long

    @Update
    suspend fun updateInvestment(investment: InvestmentEntity)

    @Delete
    suspend fun deleteInvestment(investment: InvestmentEntity)

    // Personal Expenses
    @Query("SELECT * FROM expenses WHERE monthKey = :monthKey ORDER BY dayOfMonth DESC, id DESC")
    fun observeExpensesForMonth(monthKey: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}
