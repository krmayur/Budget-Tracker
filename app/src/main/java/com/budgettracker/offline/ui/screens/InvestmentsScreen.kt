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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budgettracker.offline.data.local.InvestmentEntity
import com.budgettracker.offline.ui.components.CategoryDropdown
import com.budgettracker.offline.ui.components.EmptyStateCard
import com.budgettracker.offline.ui.components.MonthSelectorBar
import com.budgettracker.offline.ui.theme.CoralDanger
import com.budgettracker.offline.ui.theme.IndigoContainer
import com.budgettracker.offline.ui.theme.IndigoInvestment
import com.budgettracker.offline.ui.viewmodel.BudgetUiState
import com.budgettracker.offline.util.CurrencyFormatter

private val INVESTMENT_CATEGORIES = listOf(
    "SIP / Mutual Fund",
    "Stocks / Equity",
    "PPF / EPF / NPS",
    "Fixed Deposit / RD",
    "Emergency Fund",
    "Gold / SGB",
    "Other"
)

@Composable
fun InvestmentsScreen(
    uiState: BudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSaveInvestment: (InvestmentEntity) -> Unit,
    onDeleteInvestment: (InvestmentEntity) -> Unit
) {
    var editing by remember { mutableStateOf<InvestmentEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val report = uiState.insightsReport

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Investment")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MonthSelectorBar(
                    monthKey = uiState.selectedMonth,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = IndigoContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Monthly Investments",
                            style = MaterialTheme.typography.labelMedium,
                            color = IndigoInvestment
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(report.totalInvestments),
                            style = MaterialTheme.typography.headlineMedium,
                            color = IndigoInvestment
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${CurrencyFormatter.formatPercent(report.investmentRatePercent)} of monthly income · Target ≥ 20%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoInvestment
                        )
                    }
                }
            }

            if (uiState.investments.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No investments logged for this month",
                        subtitle = "Tap + to add SIPs, PPF, Fixed Deposits, Stocks, or Emergency Fund contributions."
                    )
                }
            } else {
                items(uiState.investments, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = buildString {
                                        append(item.category)
                                        if (item.isRecurring) append(" · Recurring")
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = CurrencyFormatter.format(item.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IndigoInvestment,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = {
                                    editing = item
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit investment")
                                }
                                IconButton(onClick = { onDeleteInvestment(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete investment", tint = CoralDanger)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showDialog) {
        InvestmentEditorDialog(
            monthKey = uiState.selectedMonth,
            existing = editing,
            onDismiss = { showDialog = false },
            onSave = { inv ->
                onSaveInvestment(inv)
                showDialog = false
            }
        )
    }
}

@Composable
private fun InvestmentEditorDialog(
    monthKey: String,
    existing: InvestmentEntity?,
    onDismiss: () -> Unit,
    onSave: (InvestmentEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: INVESTMENT_CATEGORIES.first()) }
    var amountText by remember {
        mutableStateOf(existing?.amount?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var isRecurring by remember { mutableStateOf(existing?.isRecurring ?: true) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Investment" else "Edit Investment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Investment Name (e.g., Nifty 50 SIP)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CategoryDropdown(
                    label = "Category",
                    options = INVESTMENT_CATEGORIES,
                    selected = category,
                    onSelect = { category = it }
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Amount (₹)") },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    Text("Recurring monthly investment", style = MaterialTheme.typography.bodyMedium)
                }
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
                val amt = amountText.trim().toDoubleOrNull()
                when {
                    name.isBlank() -> error = "Enter an investment name"
                    amt == null || amt <= 0.0 -> error = "Enter a valid amount"
                    else -> {
                        onSave(
                            InvestmentEntity(
                                id = existing?.id ?: 0L,
                                monthKey = monthKey,
                                name = name.trim(),
                                category = category,
                                amount = amt,
                                isRecurring = isRecurring,
                                notes = notes.trim()
                            )
                        )
                    }
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
