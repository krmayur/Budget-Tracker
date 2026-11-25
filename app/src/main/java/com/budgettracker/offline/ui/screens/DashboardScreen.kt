package com.budgettracker.offline.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budgettracker.offline.ui.components.AllocationSegment
import com.budgettracker.offline.ui.components.MonthSelectorBar
import com.budgettracker.offline.ui.components.StackedAllocationBar
import com.budgettracker.offline.ui.theme.AmberAccent
import com.budgettracker.offline.ui.theme.CoralDanger
import com.budgettracker.offline.ui.theme.EmeraldPrimary
import com.budgettracker.offline.ui.theme.IndigoInvestment
import com.budgettracker.offline.ui.viewmodel.BudgetUiState
import com.budgettracker.offline.util.CurrencyFormatter
import kotlin.math.max

@Composable
fun DashboardScreen(
    uiState: BudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSaveSalary: (Double, Double, String) -> Unit,
    onCopyPreviousMonth: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    var showSalaryDialog by remember { mutableStateOf(false) }
    val report = uiState.insightsReport

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MonthSelectorBar(
                monthKey = uiState.selectedMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )
        }

        // Hero Salary & Net Cashflow Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Income",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = CurrencyFormatter.format(report.totalIncome),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        FilledTonalButton(onClick = { showSalaryDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.salary == null) "Set Salary" else "Edit")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (report.netSurplus >= 0) "Unallocated Balance" else "Monthly Deficit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = CurrencyFormatter.format(report.netSurplus),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (report.netSurplus >= 0) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Health Score",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${report.healthScore}/100 · ${report.healthLabel}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // Visual Allocation Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Allocation",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val totalBase = max(
                        report.totalIncome,
                        report.totalEmi + report.totalInvestments + report.totalExpenses
                    )
                    StackedAllocationBar(
                        segments = listOf(
                            AllocationSegment("EMIs", report.totalEmi, CoralDanger),
                            AllocationSegment("Invest", report.totalInvestments, IndigoInvestment),
                            AllocationSegment("Spend", report.totalExpenses, AmberAccent),
                            AllocationSegment("Left", max(0.0, report.netSurplus), EmeraldPrimary)
                        ),
                        total = totalBase
                    )
                }
            }
        }

        // 2x2 Summary Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    title = "Loan EMIs",
                    value = CurrencyFormatter.format(report.totalEmi),
                    subtitle = "${uiState.loans.count { it.isActive }} active · ${CurrencyFormatter.formatPercent(report.emiToIncomePercent)}",
                    accent = CoralDanger,
                    onClick = onNavigateToLoans,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Investments",
                    value = CurrencyFormatter.format(report.totalInvestments),
                    subtitle = "${uiState.investments.size} entries · ${CurrencyFormatter.formatPercent(report.investmentRatePercent)}",
                    accent = IndigoInvestment,
                    onClick = onNavigateToInvestments,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    title = "Personal Spend",
                    value = CurrencyFormatter.format(report.totalExpenses),
                    subtitle = "Discretionary: ${CurrencyFormatter.format(report.discretionaryExpenses)}",
                    accent = AmberAccent,
                    onClick = onNavigateToExpenses,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Savings Rate",
                    value = CurrencyFormatter.formatPercent(report.totalSavingsRatePercent),
                    subtitle = "Invest + Surplus",
                    accent = EmeraldPrimary,
                    onClick = onNavigateToInsights,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top Insight Preview Card
        val topInsight = report.insights.firstOrNull()
        if (topInsight != null) {
            item {
                Card(
                    onClick = onNavigateToInsights,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Top Savings Insight",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = topInsight.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = topInsight.actionTip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Copy recurring from previous month shortcut
        if (uiState.salary == null || uiState.investments.isEmpty()) {
            item {
                OutlinedButton(
                    onClick = onCopyPreviousMonth,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Salary & Recurring Investments from Last Month")
                }
            }
        }
    }

    if (showSalaryDialog) {
        SalaryInputDialog(
            initialSalary = uiState.salary?.salaryAmount ?: 0.0,
            initialOtherIncome = uiState.salary?.otherIncome ?: 0.0,
            initialNotes = uiState.salary?.notes ?: "",
            onDismiss = { showSalaryDialog = false },
            onSave = { sal, other, notes ->
                onSaveSalary(sal, other, notes)
                showSalaryDialog = false
            }
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SalaryInputDialog(
    initialSalary: Double,
    initialOtherIncome: Double,
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (Double, Double, String) -> Unit
) {
    var salaryText by remember {
        mutableStateOf(if (initialSalary > 0) initialSalary.toLong().toString() else "")
    }
    var otherText by remember {
        mutableStateOf(if (initialOtherIncome > 0) initialOtherIncome.toLong().toString() else "")
    }
    var notes by remember { mutableStateOf(initialNotes) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly Salary & Income") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it; error = null },
                    label = { Text("Take-Home Salary (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = otherText,
                    onValueChange = { otherText = it },
                    label = { Text("Bonus / Other Income (₹, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val sal = salaryText.trim().toDoubleOrNull()
                val other = otherText.trim().toDoubleOrNull() ?: 0.0
                if (sal == null || sal < 0.0) {
                    error = "Enter a valid salary amount"
                } else {
                    onSave(sal, max(0.0, other), notes)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
