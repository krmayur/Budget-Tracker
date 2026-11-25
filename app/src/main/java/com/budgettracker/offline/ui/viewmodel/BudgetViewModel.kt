package com.budgettracker.offline.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budgettracker.offline.data.local.ExpenseEntity
import com.budgettracker.offline.data.local.InvestmentEntity
import com.budgettracker.offline.data.local.LoanEntity
import com.budgettracker.offline.data.local.MonthlySalaryEntity
import com.budgettracker.offline.data.repository.BudgetRepository
import com.budgettracker.offline.domain.InsightsReport
import com.budgettracker.offline.domain.SavingsInsightsEngine
import com.budgettracker.offline.util.MonthUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetUiState(
    val selectedMonth: String = MonthUtils.currentMonthKey(),
    val salary: MonthlySalaryEntity? = null,
    val loans: List<LoanEntity> = emptyList(),
    val investments: List<InvestmentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val insightsReport: InsightsReport = SavingsInsightsEngine.analyze(0.0, emptyList(), emptyList(), emptyList()),
    val statusMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val selectedMonthFlow = MutableStateFlow(MonthUtils.currentMonthKey())
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    private val salaryFlow = selectedMonthFlow.flatMapLatest { repository.observeSalary(it) }
    private val investmentsFlow = selectedMonthFlow.flatMapLatest { repository.observeInvestments(it) }
    private val expensesFlow = selectedMonthFlow.flatMapLatest { repository.observeExpenses(it) }
    private val loansFlow = repository.observeLoans()

    private val monthDataFlow = combine(
        selectedMonthFlow,
        salaryFlow,
        loansFlow,
        investmentsFlow,
        expensesFlow
    ) { month, salary, loans, investments, expenses ->
        val totalIncome = salary?.totalIncome ?: 0.0
        val report = SavingsInsightsEngine.analyze(
            totalIncome = totalIncome,
            loans = loans,
            investments = investments,
            expenses = expenses
        )
        BudgetUiState(
            selectedMonth = month,
            salary = salary,
            loans = loans,
            investments = investments,
            expenses = expenses,
            insightsReport = report
        )
    }

    val uiState: StateFlow<BudgetUiState> = combine(monthDataFlow, statusMessageFlow) { state, msg ->
        state.copy(statusMessage = msg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetUiState()
    )

    fun goToPreviousMonth() {
        selectedMonthFlow.value = MonthUtils.previousMonthKey(selectedMonthFlow.value)
    }

    fun goToNextMonth() {
        selectedMonthFlow.value = MonthUtils.nextMonthKey(selectedMonthFlow.value)
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    fun saveSalary(salaryAmount: Double, otherIncome: Double, notes: String) {
        val month = selectedMonthFlow.value
        viewModelScope.launch {
            repository.saveSalary(month, salaryAmount, otherIncome, notes)
            statusMessageFlow.value = "Salary updated for ${MonthUtils.formatDisplay(month)}"
        }
    }

    fun saveLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.saveLoan(loan)
            statusMessageFlow.value = if (loan.id == 0L) "Loan added" else "Loan updated"
        }
    }

    fun toggleLoanActive(loan: LoanEntity) {
        viewModelScope.launch {
            repository.saveLoan(loan.copy(isActive = !loan.isActive))
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
            statusMessageFlow.value = "Removed ${loan.name}"
        }
    }

    fun saveInvestment(investment: InvestmentEntity) {
        val month = selectedMonthFlow.value
        viewModelScope.launch {
            repository.saveInvestment(investment.copy(monthKey = month))
            statusMessageFlow.value = if (investment.id == 0L) "Investment logged" else "Investment updated"
        }
    }

    fun deleteInvestment(investment: InvestmentEntity) {
        viewModelScope.launch {
            repository.deleteInvestment(investment)
            statusMessageFlow.value = "Removed ${investment.name}"
        }
    }

    fun saveExpense(expense: ExpenseEntity) {
        val month = selectedMonthFlow.value
        viewModelScope.launch {
            repository.saveExpense(expense.copy(monthKey = month))
            statusMessageFlow.value = if (expense.id == 0L) "Expense logged" else "Expense updated"
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            statusMessageFlow.value = "Removed ${expense.title}"
        }
    }

    fun copyFromPreviousMonth() {
        val target = selectedMonthFlow.value
        val previous = MonthUtils.previousMonthKey(target)
        viewModelScope.launch {
            val count = repository.copyRecurringFromPreviousMonth(previous, target)
            statusMessageFlow.value = if (count > 0) {
                "Copied $count recurring item(s) from ${MonthUtils.formatDisplay(previous)}"
            } else {
                "No recurring salary or investments found in ${MonthUtils.formatDisplay(previous)}"
            }
        }
    }

    companion object {
        fun factory(repository: BudgetRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BudgetViewModel(repository) as T
                }
            }
    }
}
