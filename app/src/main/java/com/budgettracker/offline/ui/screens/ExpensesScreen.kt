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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.FilterChip
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
import com.budgettracker.offline.data.local.ExpenseEntity
import com.budgettracker.offline.ui.components.CategoryDropdown
import com.budgettracker.offline.ui.components.EmptyStateCard
import com.budgettracker.offline.ui.components.MonthSelectorBar
import com.budgettracker.offline.ui.theme.AmberAccent
import com.budgettracker.offline.ui.theme.AmberContainer
import com.budgettracker.offline.ui.theme.AmberOnContainer
import com.budgettracker.offline.ui.theme.CoralDanger
import com.budgettracker.offline.ui.viewmodel.BudgetUiState
import com.budgettracker.offline.util.CurrencyFormatter
import java.time.LocalDate

private val EXPENSE_CATEGORIES = listOf(
    "Housing & Rent",
    "Groceries",
    "Utilities & Bills",
    "Food & Dining",
    "Transport & Fuel",
    "Shopping",
    "Healthcare",
    "Entertainment & Subscriptions",
    "Family & Misc"
)

private val DEFAULT_ESSENTIAL_CATEGORIES = setOf(
    "Housing & Rent",
    "Groceries",
    "Utilities & Bills",
    "Healthcare"
)

@Composable
fun ExpensesScreen(
    uiState: BudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSaveExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var editing by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val report = uiState.insightsReport
    val filteredExpenses = remember(uiState.expenses, selectedCategory) {
        if (selectedCategory == "All") uiState.expenses
        else uiState.expenses.filter { it.category == selectedCategory }
    }

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
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
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
                    colors = CardDefaults.cardColors(containerColor = AmberContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Personal Expenditure",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberOnContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(report.totalExpenses),
                            style = MaterialTheme.typography.headlineMedium,
                            color = AmberOnContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Essential: ${CurrencyFormatter.format(report.essentialExpenses)} · Discretionary: ${CurrencyFormatter.format(report.discretionaryExpenses)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberOnContainer
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All") + EXPENSE_CATEGORIES) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No expenses in this view",
                        subtitle = "Tap + to log rent, groceries, bills, dining, transport, or shopping."
                    )
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
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
                                    text = expense.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${expense.category} · ${if (expense.isEssential) "Essential" else "Discretionary"} · Day ${expense.dayOfMonth}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = CurrencyFormatter.format(expense.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AmberAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = {
                                    editing = expense
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit expense")
                                }
                                IconButton(onClick = { onDeleteExpense(expense) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete expense", tint = CoralDanger)
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
        ExpenseEditorDialog(
            monthKey = uiState.selectedMonth,
            existing = editing,
            onDismiss = { showDialog = false },
            onSave = { exp ->
                onSaveExpense(exp)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ExpenseEditorDialog(
    monthKey: String,
    existing: ExpenseEntity?,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: EXPENSE_CATEGORIES[1]) }
    var amountText by remember {
        mutableStateOf(existing?.amount?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var isEssential by remember {
        mutableStateOf(existing?.isEssential ?: (category in DEFAULT_ESSENTIAL_CATEGORIES))
    }
    var dayText by remember {
        mutableStateOf((existing?.dayOfMonth ?: LocalDate.now().dayOfMonth).toString())
    }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Personal Expense" else "Edit Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Expense Title (e.g., Weekly Groceries)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CategoryDropdown(
                    label = "Category",
                    options = EXPENSE_CATEGORIES,
                    selected = category,
                    onSelect = { selected ->
                        category = selected
                        if (existing == null) {
                            isEssential = selected in DEFAULT_ESSENTIAL_CATEGORIES
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it; error = null },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = dayText,
                        onValueChange = { dayText = it },
                        label = { Text("Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isEssential, onCheckedChange = { isEssential = it })
                    Text("Mark as Essential need (rent, groceries, bills)", style = MaterialTheme.typography.bodyMedium)
                }
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
                val amt = amountText.trim().toDoubleOrNull()
                val day = (dayText.trim().toIntOrNull() ?: 1).coerceIn(1, 31)
                when {
                    title.isBlank() -> error = "Enter an expense title"
                    amt == null || amt <= 0.0 -> error = "Enter a valid amount"
                    else -> {
                        onSave(
                            ExpenseEntity(
                                id = existing?.id ?: 0L,
                                monthKey = monthKey,
                                title = title.trim(),
                                category = category,
                                amount = amt,
                                isEssential = isEssential,
                                dayOfMonth = day,
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
