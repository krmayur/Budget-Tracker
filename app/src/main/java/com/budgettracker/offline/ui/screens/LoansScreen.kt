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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.budgettracker.offline.data.local.LoanEntity
import com.budgettracker.offline.ui.components.CategoryDropdown
import com.budgettracker.offline.ui.components.EmptyStateCard
import com.budgettracker.offline.ui.theme.CoralDanger
import com.budgettracker.offline.ui.viewmodel.BudgetUiState
import com.budgettracker.offline.util.CurrencyFormatter

private val LOAN_TYPES = listOf(
    "Home Loan",
    "Car / Auto Loan",
    "Personal Loan",
    "Education Loan",
    "Credit Card EMI",
    "Consumer Durable EMI",
    "Other Loan"
)

@Composable
fun LoansScreen(
    uiState: BudgetUiState,
    onSaveLoan: (LoanEntity) -> Unit,
    onToggleActive: (LoanEntity) -> Unit,
    onDeleteLoan: (LoanEntity) -> Unit
) {
    var editingLoan by remember { mutableStateOf<LoanEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val report = uiState.insightsReport

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingLoan = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Total Monthly Loan EMIs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(report.totalEmi),
                            style = MaterialTheme.typography.headlineMedium,
                            color = CoralDanger
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.loans.count { it.isActive }} active loan(s) · ${CurrencyFormatter.formatPercent(report.emiToIncomePercent)} of monthly income",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (uiState.loans.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No loans added yet",
                        subtitle = "Tap + to add your Home Loan, Car Loan, Personal Loan, or Credit Card EMIs. You can track multiple loans simultaneously."
                    )
                }
            } else {
                items(uiState.loans, key = { it.id }) { loan ->
                    LoanCard(
                        loan = loan,
                        onToggleActive = { onToggleActive(loan) },
                        onEdit = {
                            editingLoan = loan
                            showAddDialog = true
                        },
                        onDelete = { onDeleteLoan(loan) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        LoanEditorDialog(
            existing = editingLoan,
            onDismiss = { showAddDialog = false },
            onSave = { loan ->
                onSaveLoan(loan)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun LoanCard(
    loan: LoanEntity,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (loan.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = buildString {
                            append(loan.loanType)
                            if (loan.interestRateAnnual > 0) {
                                append(" · ${CurrencyFormatter.formatPercent(loan.interestRateAnnual)} p.a.")
                            }
                            if (!loan.isActive) append(" · Paid Off / Paused")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${CurrencyFormatter.format(loan.emiAmount)}/mo",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (loan.isActive) CoralDanger else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            if (loan.remainingPrincipal > 0 || loan.remainingMonths > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (loan.remainingPrincipal > 0) {
                        Text(
                            text = "Balance: ${CurrencyFormatter.format(loan.remainingPrincipal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (loan.remainingMonths > 0) {
                        Text(
                            text = "${loan.remainingMonths} months left",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = loan.isActive, onCheckedChange = { onToggleActive() })
                    Text(
                        text = if (loan.isActive) "Active EMI" else "Inactive",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit loan")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete loan", tint = CoralDanger)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanEditorDialog(
    existing: LoanEntity?,
    onDismiss: () -> Unit,
    onSave: (LoanEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var loanType by remember { mutableStateOf(existing?.loanType ?: LOAN_TYPES.first()) }
    var emiText by remember {
        mutableStateOf(existing?.emiAmount?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var principalText by remember {
        mutableStateOf(existing?.remainingPrincipal?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var rateText by remember {
        mutableStateOf(existing?.interestRateAnnual?.takeIf { it > 0 }?.toString() ?: "")
    }
    var monthsText by remember {
        mutableStateOf(existing?.remainingMonths?.takeIf { it > 0 }?.toString() ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Loan & EMI" else "Edit Loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Loan Name (e.g., HDFC Home Loan)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CategoryDropdown(
                    label = "Loan Type",
                    options = LOAN_TYPES,
                    selected = loanType,
                    onSelect = { loanType = it }
                )
                OutlinedTextField(
                    value = emiText,
                    onValueChange = { emiText = it; error = null },
                    label = { Text("Monthly EMI Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Interest Rate % p.a. (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = principalText,
                        onValueChange = { principalText = it },
                        label = { Text("Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthsText,
                        onValueChange = { monthsText = it },
                        label = { Text("Months Left") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
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
                val emi = emiText.trim().toDoubleOrNull()
                when {
                    name.isBlank() -> error = "Enter a loan name"
                    emi == null || emi <= 0.0 -> error = "Enter a valid monthly EMI"
                    else -> {
                        onSave(
                            LoanEntity(
                                id = existing?.id ?: 0L,
                                name = name.trim(),
                                loanType = loanType,
                                emiAmount = emi,
                                remainingPrincipal = principalText.trim().toDoubleOrNull() ?: 0.0,
                                interestRateAnnual = rateText.trim().toDoubleOrNull() ?: 0.0,
                                remainingMonths = monthsText.trim().toIntOrNull() ?: 0,
                                isActive = existing?.isActive ?: true
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
