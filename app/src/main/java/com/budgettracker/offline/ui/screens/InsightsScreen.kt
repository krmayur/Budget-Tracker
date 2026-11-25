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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgettracker.offline.domain.InsightSeverity
import com.budgettracker.offline.domain.SavingsInsight
import com.budgettracker.offline.domain.SavingsInsightsEngine
import com.budgettracker.offline.ui.theme.AmberAccent
import com.budgettracker.offline.ui.theme.AmberContainer
import com.budgettracker.offline.ui.theme.AmberOnContainer
import com.budgettracker.offline.ui.theme.CoralContainer
import com.budgettracker.offline.ui.theme.CoralDanger
import com.budgettracker.offline.ui.theme.CoralOnContainer
import com.budgettracker.offline.ui.theme.EmeraldContainer
import com.budgettracker.offline.ui.theme.EmeraldOnContainer
import com.budgettracker.offline.ui.theme.EmeraldPrimary
import com.budgettracker.offline.ui.theme.IndigoContainer
import com.budgettracker.offline.ui.theme.IndigoInvestment
import com.budgettracker.offline.ui.viewmodel.BudgetUiState
import com.budgettracker.offline.util.CurrencyFormatter
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(uiState: BudgetUiState) {
    val report = uiState.insightsReport
    var cutPercentSlider by remember { mutableFloatStateOf(20f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Financial Health Score Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Offline Financial Health Score",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${report.healthScore} / 100",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = report.healthLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (report.healthScore / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFA7F3D0),
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                    )
                    if (report.estimatedTotalMonthlySavingsPotential > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Estimated extra savings identified: ${CurrencyFormatter.format(report.estimatedTotalMonthlySavingsPotential)}/mo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // 50/30/20 Rule Benchmark Card
        if (report.totalIncome > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "50 / 30 / 20 Budget Check",
                            style = MaterialTheme.typography.titleMedium
                        )
                        RuleProgressRow(
                            label = "Needs (EMIs + Essential)",
                            actualPct = report.ruleBreakdown.needsPercent,
                            targetText = "Target ≤ 50%",
                            color = if (report.ruleBreakdown.needsPercent > 50.0) CoralDanger else EmeraldPrimary
                        )
                        RuleProgressRow(
                            label = "Wants (Discretionary)",
                            actualPct = report.ruleBreakdown.wantsPercent,
                            targetText = "Target ≤ 30%",
                            color = if (report.ruleBreakdown.wantsPercent > 30.0) AmberAccent else EmeraldPrimary
                        )
                        RuleProgressRow(
                            label = "Savings & Investments",
                            actualPct = report.ruleBreakdown.savingsPercent,
                            targetText = "Target ≥ 20%",
                            color = if (report.ruleBreakdown.savingsPercent < 20.0) AmberAccent else IndigoInvestment
                        )
                    }
                }
            }
        }

        // Interactive What-If Savings Simulator
        if (report.discretionaryExpenses > 0) {
            item {
                val pct = cutPercentSlider.roundToInt()
                val monthlyGain = report.discretionaryExpenses * (pct / 100.0)
                val yearlyGain = monthlyGain * 12.0
                val fiveYearSip = SavingsInsightsEngine.projectSipValue(monthlyGain, 10.0, 5)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "What-If Savings Simulator",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "If you trim discretionary spending (${CurrencyFormatter.format(report.discretionaryExpenses)}) by $pct%:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = cutPercentSlider,
                            onValueChange = { cutPercentSlider = it },
                            valueRange = 5f..50f,
                            steps = 8
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SimulatorStat("Monthly", CurrencyFormatter.format(monthlyGain))
                            SimulatorStat("1 Year", CurrencyFormatter.format(yearlyGain))
                            SimulatorStat("5-Yr SIP (10%)", CurrencyFormatter.format(fiveYearSip))
                        }
                    }
                }
            }
        }

        // Actionable Recommendations List
        item {
            Text(
                text = "How to Save More",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(report.insights, key = { it.id }) { insight ->
            InsightCard(insight = insight)
        }
    }
}

@Composable
private fun RuleProgressRow(
    label: String,
    actualPct: Double,
    targetText: String,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${CurrencyFormatter.formatPercent(actualPct)} ($targetText)",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
        LinearProgressIndicator(
            progress = { (actualPct / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color
        )
    }
}

@Composable
private fun SimulatorStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = EmeraldPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InsightCard(insight: SavingsInsight) {
    val (bg, fg, icon) = when (insight.severity) {
        InsightSeverity.CRITICAL -> Triple(CoralContainer, CoralOnContainer, Icons.Default.Warning)
        InsightSeverity.WARNING -> Triple(AmberContainer, AmberOnContainer, Icons.Default.Warning)
        InsightSeverity.OPPORTUNITY -> Triple(IndigoContainer, IndigoInvestment, Icons.Default.Lightbulb)
        InsightSeverity.POSITIVE -> Triple(EmeraldContainer, EmeraldOnContainer, Icons.Default.CheckCircle)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = fg)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = fg,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = fg
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = fg.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Next step: ${insight.actionTip}",
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
