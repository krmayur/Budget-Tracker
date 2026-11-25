package com.budgettracker.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.budgettracker.offline.ui.screens.DashboardScreen
import com.budgettracker.offline.ui.screens.ExpensesScreen
import com.budgettracker.offline.ui.screens.InsightsScreen
import com.budgettracker.offline.ui.screens.InvestmentsScreen
import com.budgettracker.offline.ui.screens.LoansScreen
import com.budgettracker.offline.ui.theme.BudgetTrackerTheme
import com.budgettracker.offline.ui.viewmodel.BudgetViewModel

private enum class AppTab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Home", Icons.Default.AccountBalanceWallet),
    LOANS("loans", "Loans", Icons.Default.AccountBalance),
    INVESTMENTS("investments", "Invest", Icons.Default.TrendingUp),
    EXPENSES("expenses", "Spend", Icons.Default.ShoppingBag),
    INSIGHTS("insights", "Insights", Icons.Default.Lightbulb)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BudgetTrackerApp
        setContent {
            BudgetTrackerTheme {
                val vm: BudgetViewModel = viewModel(factory = BudgetViewModel.factory(app.repository))
                BudgetTrackerRoot(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetTrackerRoot(viewModel: BudgetViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppTab.DASHBOARD.route

    fun navigateToTab(tab: AppTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (currentRoute) {
                        AppTab.LOANS.route -> "Loans & EMIs"
                        AppTab.INVESTMENTS.route -> "Investments"
                        AppTab.EXPENSES.route -> "Personal Expenses"
                        AppTab.INSIGHTS.route -> "Savings Insights"
                        else -> "PocketWise Budget"
                    }
                    Text(title)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navigateToTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppTab.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppTab.DASHBOARD.route) {
                DashboardScreen(
                    uiState = uiState,
                    onPreviousMonth = viewModel::goToPreviousMonth,
                    onNextMonth = viewModel::goToNextMonth,
                    onSaveSalary = viewModel::saveSalary,
                    onCopyPreviousMonth = viewModel::copyFromPreviousMonth,
                    onNavigateToLoans = { navigateToTab(AppTab.LOANS) },
                    onNavigateToInvestments = { navigateToTab(AppTab.INVESTMENTS) },
                    onNavigateToExpenses = { navigateToTab(AppTab.EXPENSES) },
                    onNavigateToInsights = { navigateToTab(AppTab.INSIGHTS) }
                )
            }
            composable(AppTab.LOANS.route) {
                LoansScreen(
                    uiState = uiState,
                    onSaveLoan = viewModel::saveLoan,
                    onToggleActive = viewModel::toggleLoanActive,
                    onDeleteLoan = viewModel::deleteLoan
                )
            }
            composable(AppTab.INVESTMENTS.route) {
                InvestmentsScreen(
                    uiState = uiState,
                    onPreviousMonth = viewModel::goToPreviousMonth,
                    onNextMonth = viewModel::goToNextMonth,
                    onSaveInvestment = viewModel::saveInvestment,
                    onDeleteInvestment = viewModel::deleteInvestment
                )
            }
            composable(AppTab.EXPENSES.route) {
                ExpensesScreen(
                    uiState = uiState,
                    onPreviousMonth = viewModel::goToPreviousMonth,
                    onNextMonth = viewModel::goToNextMonth,
                    onSaveExpense = viewModel::saveExpense,
                    onDeleteExpense = viewModel::deleteExpense
                )
            }
            composable(AppTab.INSIGHTS.route) {
                InsightsScreen(uiState = uiState)
            }
        }
    }
}
