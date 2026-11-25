package com.budgettracker.offline.util

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object MonthUtils {

    private val keyFormatter = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)

    fun currentMonthKey(): String = YearMonth.now().format(keyFormatter)

    fun previousMonthKey(monthKey: String): String =
        parse(monthKey).minusMonths(1).format(keyFormatter)

    fun nextMonthKey(monthKey: String): String =
        parse(monthKey).plusMonths(1).format(keyFormatter)

    fun formatDisplay(monthKey: String): String =
        parse(monthKey).format(displayFormatter)

    private fun parse(monthKey: String): YearMonth =
        runCatching { YearMonth.parse(monthKey, keyFormatter) }.getOrElse { YearMonth.now() }
}
