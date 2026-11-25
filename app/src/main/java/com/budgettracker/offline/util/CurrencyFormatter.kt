package com.budgettracker.offline.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {

    private val indianLocale = Locale("en", "IN")

    fun format(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        val rounded = abs(amount).roundToLong()
        val prefix = if (amount < -0.5) "-₹" else "₹"
        return "$prefix${formatter.format(rounded)}"
    }

    fun formatPercent(value: Double): String {
        return String.format(Locale.US, "%.1f%%", value)
    }
}
