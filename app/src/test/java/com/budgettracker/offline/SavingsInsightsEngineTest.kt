package com.budgettracker.offline

import com.budgettracker.offline.data.local.ExpenseEntity
import com.budgettracker.offline.data.local.InvestmentEntity
import com.budgettracker.offline.data.local.LoanEntity
import com.budgettracker.offline.domain.InsightSeverity
import com.budgettracker.offline.domain.SavingsInsightsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsInsightsEngineTest {

    @Test
    fun `detects cashflow deficit and high EMI burden`() {
        val loans = listOf(
            LoanEntity(id = 1, name = "Home Loan", loanType = "Home", emiAmount = 32000.0, interestRateAnnual = 8.7),
            LoanEntity(id = 2, name = "Personal Loan", loanType = "Personal", emiAmount = 15000.0, interestRateAnnual = 13.5)
        )
        val investments = listOf(
            InvestmentEntity(id = 1, monthKey = "2026-11", name = "Nifty SIP", category = "SIP / Mutual Fund", amount = 10000.0)
        )
        val expenses = listOf(
            ExpenseEntity(id = 1, monthKey = "2026-11", title = "Groceries", category = "Groceries", amount = 15000.0, isEssential = true),
            ExpenseEntity(id = 2, monthKey = "2026-11", title = "Dining", category = "Food & Dining", amount = 25000.0, isEssential = false),
            ExpenseEntity(id = 3, monthKey = "2026-11", title = "Shopping", category = "Shopping", amount = 12000.0, isEssential = false)
        )

        val report = SavingsInsightsEngine.analyze(
            totalIncome = 100000.0,
            loans = loans,
            investments = investments,
            expenses = expenses
        )

        assertEquals(47000.0, report.totalEmi, 0.01)
        assertEquals(-9000.0, report.netSurplus, 0.01)
        assertTrue(report.insights.any { it.id == "cashflow_deficit" && it.severity == InsightSeverity.CRITICAL })
        assertTrue(report.insights.any { it.id == "high_emi_ratio" && it.actionTip.contains("Personal Loan") })
    }

    @Test
    fun `healthy budget produces high score and positive investment feedback`() {
        val loans = listOf(
            LoanEntity(id = 1, name = "Car Loan", loanType = "Auto", emiAmount = 12000.0, interestRateAnnual = 9.0)
        )
        val investments = listOf(
            InvestmentEntity(id = 1, monthKey = "2026-11", name = "Index SIP", category = "SIP / Mutual Fund", amount = 25000.0),
            InvestmentEntity(id = 2, monthKey = "2026-11", name = "PPF", category = "PPF / EPF / NPS", amount = 10000.0)
        )
        val expenses = listOf(
            ExpenseEntity(id = 1, monthKey = "2026-11", title = "Rent", category = "Housing & Rent", amount = 20000.0, isEssential = true),
            ExpenseEntity(id = 2, monthKey = "2026-11", title = "Food", category = "Food & Dining", amount = 8000.0, isEssential = false)
        )

        val report = SavingsInsightsEngine.analyze(
            totalIncome = 100000.0,
            loans = loans,
            investments = investments,
            expenses = expenses
        )

        assertTrue("Expected score >= 80, got ${report.healthScore}", report.healthScore >= 80)
        assertEquals("Excellent", report.healthLabel)
        assertTrue(report.insights.any { it.id == "strong_investments" })
    }
}
