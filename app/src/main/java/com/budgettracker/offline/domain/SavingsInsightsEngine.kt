package com.budgettracker.offline.domain

import com.budgettracker.offline.data.local.ExpenseEntity
import com.budgettracker.offline.data.local.InvestmentEntity
import com.budgettracker.offline.data.local.LoanEntity
import com.budgettracker.offline.util.CurrencyFormatter
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

enum class InsightSeverity {
    CRITICAL,
    WARNING,
    OPPORTUNITY,
    POSITIVE
}

data class SavingsInsight(
    val id: String,
    val title: String,
    val description: String,
    val actionTip: String,
    val potentialMonthlySavings: Double,
    val severity: InsightSeverity
)

data class BudgetRuleBreakdown(
    val needsPercent: Double,         // Essential expenses + EMIs (Target <= 50%)
    val wantsPercent: Double,         // Discretionary expenses (Target <= 30%)
    val savingsPercent: Double        // Investments + Unallocated positive surplus (Target >= 20%)
)

data class InsightsReport(
    val healthScore: Int,
    val healthLabel: String,
    val totalIncome: Double,
    val totalEmi: Double,
    val totalInvestments: Double,
    val totalExpenses: Double,
    val essentialExpenses: Double,
    val discretionaryExpenses: Double,
    val netSurplus: Double,
    val emiToIncomePercent: Double,
    val investmentRatePercent: Double,
    val totalSavingsRatePercent: Double,
    val estimatedTotalMonthlySavingsPotential: Double,
    val ruleBreakdown: BudgetRuleBreakdown,
    val insights: List<SavingsInsight>
)

object SavingsInsightsEngine {

    fun analyze(
        totalIncome: Double,
        loans: List<LoanEntity>,
        investments: List<InvestmentEntity>,
        expenses: List<ExpenseEntity>
    ): InsightsReport {
        val activeLoans = loans.filter { it.isActive }
        val totalEmi = activeLoans.sumOf { it.emiAmount }
        val totalInvestments = investments.sumOf { it.amount }
        val totalExpenses = expenses.sumOf { it.amount }
        val essentialExpenses = expenses.filter { it.isEssential }.sumOf { it.amount }
        val discretionaryExpenses = expenses.filter { !it.isEssential }.sumOf { it.amount }
        val netSurplus = totalIncome - totalEmi - totalInvestments - totalExpenses

        val emiPct = if (totalIncome > 0.0) (totalEmi / totalIncome) * 100.0 else 0.0
        val invPct = if (totalIncome > 0.0) (totalInvestments / totalIncome) * 100.0 else 0.0
        val positiveSurplus = max(0.0, netSurplus)
        val totalSavingsRate = if (totalIncome > 0.0) {
            ((totalInvestments + positiveSurplus) / totalIncome) * 100.0
        } else {
            0.0
        }

        val needsPct = if (totalIncome > 0.0) ((essentialExpenses + totalEmi) / totalIncome) * 100.0 else 0.0
        val wantsPct = if (totalIncome > 0.0) (discretionaryExpenses / totalIncome) * 100.0 else 0.0
        val ruleBreakdown = BudgetRuleBreakdown(
            needsPercent = needsPct,
            wantsPercent = wantsPct,
            savingsPercent = totalSavingsRate
        )

        val insights = mutableListOf<SavingsInsight>()
        var potentialSavingsSum = 0.0

        if (totalIncome <= 0.0) {
            insights.add(
                SavingsInsight(
                    id = "add_salary",
                    title = "Add your monthly salary to unlock full insights",
                    description = "Enter your monthly take-home salary on the Dashboard so PocketWise can calculate your EMI ratio, 50/30/20 split, and personalized savings plan.",
                    actionTip = "Tap 'Set Salary' on the Dashboard tab.",
                    potentialMonthlySavings = 0.0,
                    severity = InsightSeverity.WARNING
                )
            )
            return InsightsReport(
                healthScore = 0,
                healthLabel = "Setup Needed",
                totalIncome = totalIncome,
                totalEmi = totalEmi,
                totalInvestments = totalInvestments,
                totalExpenses = totalExpenses,
                essentialExpenses = essentialExpenses,
                discretionaryExpenses = discretionaryExpenses,
                netSurplus = netSurplus,
                emiToIncomePercent = emiPct,
                investmentRatePercent = invPct,
                totalSavingsRatePercent = totalSavingsRate,
                estimatedTotalMonthlySavingsPotential = 0.0,
                ruleBreakdown = ruleBreakdown,
                insights = insights
            )
        }

        // 1. Cashflow deficit check
        if (netSurplus < 0.0) {
            val deficit = -netSurplus
            val topDisc = expenses
                .filter { !it.isEssential }
                .groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .maxByOrNull { it.value }

            val cutHint = if (topDisc != null) {
                "Start by trimming ${topDisc.key} (${CurrencyFormatter.format(topDisc.value)} this month)."
            } else {
                "Review your discretionary expenses or pause non-essential outflows until cashflow is positive."
            }
            insights.add(
                SavingsInsight(
                    id = "cashflow_deficit",
                    title = "Monthly outflow exceeds salary by ${CurrencyFormatter.format(deficit)}",
                    description = "Your combined EMIs, investments, and personal expenses are higher than your monthly income. Running a deficit can lead to credit card debt or draining emergency funds.",
                    actionTip = cutHint,
                    potentialMonthlySavings = deficit,
                    severity = InsightSeverity.CRITICAL
                )
            )
            potentialSavingsSum += deficit
        }

        // 2. Debt-to-Income / EMI burden check + Avalanche/Snowball strategy
        if (emiPct > 35.0) {
            val targetEmi = totalIncome * 0.30
            val excessEmi = max(0.0, totalEmi - targetEmi)
            val highestRateLoan = activeLoans.filter { it.interestRateAnnual > 0.0 }
                .maxByOrNull { it.interestRateAnnual }
            val smallestLoan = activeLoans.filter { it.remainingPrincipal > 0.0 }
                .minByOrNull { it.remainingPrincipal }

            val debtStrategy = when {
                highestRateLoan != null ->
                    "Debt Avalanche: Direct any bonus or surplus toward '${highestRateLoan.name}' (${CurrencyFormatter.formatPercent(highestRateLoan.interestRateAnnual)} p.a.) to cut interest fastest."
                smallestLoan != null ->
                    "Debt Snowball: Pay off '${smallestLoan.name}' (${CurrencyFormatter.format(smallestLoan.remainingPrincipal)} left) first to free up ${CurrencyFormatter.format(smallestLoan.emiAmount)}/mo."
                else ->
                    "Add interest rates and remaining balances to your loans to get an exact payoff order, and avoid taking new EMIs."
            }

            insights.add(
                SavingsInsight(
                    id = "high_emi_ratio",
                    title = "EMIs consume ${CurrencyFormatter.formatPercent(emiPct)} of your income",
                    description = "Keeping total EMIs under 30%–35% of take-home pay protects your budget from shocks. You are currently ${CurrencyFormatter.format(excessEmi)}/mo above the 30% comfort benchmark across ${activeLoans.size} active loan(s).",
                    actionTip = debtStrategy,
                    potentialMonthlySavings = excessEmi,
                    severity = if (emiPct > 45.0) InsightSeverity.CRITICAL else InsightSeverity.WARNING
                )
            )
        } else if (activeLoans.size >= 2) {
            val highestRateLoan = activeLoans.filter { it.interestRateAnnual > 0.0 }
                .maxByOrNull { it.interestRateAnnual }
            if (highestRateLoan != null) {
                insights.add(
                    SavingsInsight(
                        id = "multi_loan_avalanche",
                        title = "Optimize your ${activeLoans.size} active loans with prepayment",
                        description = "Your EMI ratio (${CurrencyFormatter.formatPercent(emiPct)}) is manageable. Making even one extra EMI prepayment per year on your highest-interest loan ('${highestRateLoan.name}' at ${CurrencyFormatter.formatPercent(highestRateLoan.interestRateAnnual)}) can shave months off its tenure.",
                        actionTip = "Channel annual bonuses or monthly surplus into '${highestRateLoan.name}' principal prepayment.",
                        potentialMonthlySavings = highestRateLoan.emiAmount * 0.15,
                        severity = InsightSeverity.OPPORTUNITY
                    )
                )
            }
        }

        // 3. Top discretionary category cut opportunity
        val discretionaryByCategory = expenses
            .filter { !it.isEssential }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }

        if (discretionaryByCategory.isNotEmpty() && discretionaryExpenses > 0.0) {
            val topCat = discretionaryByCategory.first()
            val cut20 = topCat.value * 0.20
            val annualSave = cut20 * 12.0
            if (cut20 >= 250.0) {
                insights.add(
                    SavingsInsight(
                        id = "top_discretionary_cut",
                        title = "Trim '${topCat.key}' by 20% to save ${CurrencyFormatter.format(cut20)}/mo",
                        description = "'${topCat.key}' is your largest non-essential expense at ${CurrencyFormatter.format(topCat.value)} (${CurrencyFormatter.formatPercent((topCat.value / totalIncome) * 100.0)} of salary). A modest 20% cap saves ${CurrencyFormatter.format(annualSave)} per year.",
                        actionTip = "Set a hard monthly cap of ${CurrencyFormatter.format(topCat.value - cut20)} for ${topCat.key}.",
                        potentialMonthlySavings = cut20,
                        severity = if (wantsPct > 30.0) InsightSeverity.WARNING else InsightSeverity.OPPORTUNITY
                    )
                )
                potentialSavingsSum += cut20
            }
        }

        // 4. Frequent small-expense / impulse spending audit
        val frequentDiscretionary = expenses
            .filter { !it.isEssential }
            .groupBy { it.category }
            .filter { (_, list) -> list.size >= 4 }
            .maxByOrNull { (_, list) -> list.sumOf { it.amount } }

        if (frequentDiscretionary != null) {
            val catTotal = frequentDiscretionary.value.sumOf { it.amount }
            val trim15 = catTotal * 0.15
            insights.add(
                SavingsInsight(
                    id = "frequent_spends",
                    title = "${frequentDiscretionary.value.size} separate entries in '${frequentDiscretionary.key}'",
                    description = "Frequent small purchases add up quickly—totaling ${CurrencyFormatter.format(catTotal)} this month. Consolidating or skipping 1 in 6 purchases saves ${CurrencyFormatter.format(trim15)}/mo.",
                    actionTip = "Apply a 24-hour cooling-off rule before discretionary purchases in ${frequentDiscretionary.key}.",
                    potentialMonthlySavings = trim15,
                    severity = InsightSeverity.OPPORTUNITY
                )
            )
        }

        // 5. Investment rate benchmark (target >= 20%)
        val targetInvestment = totalIncome * 0.20
        if (invPct < 20.0) {
            val gap = max(0.0, targetInvestment - totalInvestments)
            insights.add(
                SavingsInsight(
                    id = "boost_investments",
                    title = "Increase investments by ${CurrencyFormatter.format(gap)}/mo to hit 20%",
                    description = "You are currently investing ${CurrencyFormatter.formatPercent(invPct)} of your salary (${CurrencyFormatter.format(totalInvestments)}). Automating at least 20% right after payday builds long-term wealth effortlessly.",
                    actionTip = if (netSurplus >= gap) {
                        "You already have ${CurrencyFormatter.format(netSurplus)} unallocated—move ${CurrencyFormatter.format(gap)} into a recurring SIP or PPF."
                    } else {
                        "Redirect savings from trimming discretionary categories into an automated SIP on salary day."
                    },
                    potentialMonthlySavings = 0.0,
                    severity = InsightSeverity.WARNING
                )
            )
        } else {
            insights.add(
                SavingsInsight(
                    id = "strong_investments",
                    title = "Strong investment discipline at ${CurrencyFormatter.formatPercent(invPct)}",
                    description = "You are investing ${CurrencyFormatter.format(totalInvestments)}/mo, beating the 20% baseline. Stepping up your SIP by 5%–10% every year compounds significantly.",
                    actionTip = "Keep recurring investments automated at the start of each month.",
                    potentialMonthlySavings = 0.0,
                    severity = InsightSeverity.POSITIVE
                )
            )
        }

        // 6. Idle unallocated surplus suggestion
        if (netSurplus > totalIncome * 0.10 && netSurplus >= 1000.0) {
            val deployAmount = netSurplus * 0.60
            val fiveYearValue = projectSipValue(deployAmount, annualRatePercent = 10.0, years = 5)
            insights.add(
                SavingsInsight(
                    id = "idle_surplus",
                    title = "Put ${CurrencyFormatter.format(netSurplus)} unallocated surplus to work",
                    description = "Leaving unallocated cash in a standard savings account often leads to lifestyle creep. Investing 60% (${CurrencyFormatter.format(deployAmount)}/mo) could grow to ~${CurrencyFormatter.format(fiveYearValue)} in 5 years at an illustrative 10% p.a.",
                    actionTip = "Keep 40% as a liquid buffer and route 60% to an emergency fund, SIP, or loan prepayment.",
                    potentialMonthlySavings = deployAmount,
                    severity = InsightSeverity.OPPORTUNITY
                )
            )
        }

        // Compute Financial Health Score (0..100)
        val score = computeHealthScore(
            totalSavingsRate = totalSavingsRate,
            emiPct = emiPct,
            wantsPct = wantsPct,
            netSurplus = netSurplus
        )
        val label = when {
            score >= 80 -> "Excellent"
            score >= 65 -> "Healthy"
            score >= 45 -> "Needs Attention"
            else -> "At Risk"
        }

        return InsightsReport(
            healthScore = score,
            healthLabel = label,
            totalIncome = totalIncome,
            totalEmi = totalEmi,
            totalInvestments = totalInvestments,
            totalExpenses = totalExpenses,
            essentialExpenses = essentialExpenses,
            discretionaryExpenses = discretionaryExpenses,
            netSurplus = netSurplus,
            emiToIncomePercent = emiPct,
            investmentRatePercent = invPct,
            totalSavingsRatePercent = totalSavingsRate,
            estimatedTotalMonthlySavingsPotential = potentialSavingsSum,
            ruleBreakdown = ruleBreakdown,
            insights = insights.sortedBy { it.severity.ordinal }
        )
    }

    private fun computeHealthScore(
        totalSavingsRate: Double,
        emiPct: Double,
        wantsPct: Double,
        netSurplus: Double
    ): Int {
        // 1. Savings & investment score (max 35 pts)
        val savingsPts = ((totalSavingsRate / 30.0).coerceIn(0.0, 1.0) * 35.0)
        // 2. EMI ratio score (max 30 pts: <=20% gets full 30, >=55% gets 0)
        val emiPts = when {
            emiPct <= 20.0 -> 30.0
            emiPct >= 55.0 -> 0.0
            else -> ((55.0 - emiPct) / 35.0) * 30.0
        }
        // 3. Discretionary control score (max 20 pts: <=25% gets full 20, >=55% gets 0)
        val wantsPts = when {
            wantsPct <= 25.0 -> 20.0
            wantsPct >= 55.0 -> 0.0
            else -> ((55.0 - wantsPct) / 30.0) * 20.0
        }
        // 4. Positive cashflow bonus (15 pts)
        val cashflowPts = if (netSurplus >= 0.0) 15.0 else 0.0

        return (savingsPts + emiPts + wantsPts + cashflowPts).roundToInt().coerceIn(0, 100)
    }

    /**
     * Standard monthly SIP future value formula for the offline What-If Simulator.
     */
    fun projectSipValue(monthlyAmount: Double, annualRatePercent: Double, years: Int): Double {
        if (monthlyAmount <= 0.0 || years <= 0) return 0.0
        val months = years * 12
        val r = (annualRatePercent / 100.0) / 12.0
        if (r == 0.0) return monthlyAmount * months
        return monthlyAmount * (((1.0 + r).pow(months) - 1.0) / r) * (1.0 + r)
    }
}
