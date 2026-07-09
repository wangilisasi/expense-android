package com.example.expensemanager.ui.viewmodels

import com.example.expensemanager.models.CategoryAnalyticsItem
import com.example.expensemanager.models.CategoryAnalyticsResponse
import com.example.expensemanager.models.DailyExpense
import com.example.expensemanager.models.DailyExpensesResponse
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.ExpenseTransaction
import com.example.expensemanager.models.StatsResponse
import com.example.expensemanager.models.TrackerSummaryResponse
import com.example.expensemanager.models.normalizeExpenseCategory
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun buildLocalDailyExpenses(expenses: List<ExpenseResponse>): DailyExpensesResponse {
    val groupedByDate = expenses.groupBy { it.date }
    val dailyTotals = groupedByDate.entries
        .sortedByDescending { it.key }
        .map { (date, dayExpenses) ->
            DailyExpense(
                date = date,
                total_amount = dayExpenses.sumOf { it.amount },
                transactions = dayExpenses.map { expense ->
                    ExpenseTransaction(
                        id = expense.id,
                        name = expense.description,
                        amount = expense.amount,
                        category = normalizeExpenseCategory(expense.category),
                        createdAt = expense.createdAt,
                        updatedAt = expense.updatedAt,
                        isSynced = expense.isSynced
                    )
                }
            )
        }

    return DailyExpensesResponse(daily_expenses = dailyTotals)
}

internal fun buildLocalStats(
    tracker: TrackerSummaryResponse,
    expenses: List<ExpenseResponse>
): StatsResponse {
    val today = LocalDate.now()
    val startDate = parseDateOrNull(tracker.startDate)
    val endDate = parseDateOrNull(tracker.endDate)

    val totalSpent = expenses.sumOf { it.amount }
    val todaysSpent = expenses.filter { it.date == today.toString() }.sumOf { it.amount }
    val remainingDays = endDate
        ?.let { ChronoUnit.DAYS.between(today, it).toInt().coerceAtLeast(0) }
        ?: 0

    val elapsedDays = startDate
        ?.takeIf { !today.isBefore(it) }
        ?.let { ChronoUnit.DAYS.between(it, today).toInt() + 1 }
        ?.coerceAtLeast(1)
        ?: 1

    val remainingBudget = (tracker.budget - totalSpent).coerceAtLeast(0.0)
    val targetPerDay = if (remainingDays > 0) remainingBudget / remainingDays else remainingBudget
    val averageExpenditure = if (expenses.isNotEmpty()) totalSpent / elapsedDays else 0.0

    return StatsResponse(
        startDate = tracker.startDate,
        endDate = tracker.endDate,
        budget = tracker.budget,
        remainingDays = remainingDays,
        targetExpenditurePerDay = targetPerDay,
        totalExpenditure = totalSpent,
        todaysExpenditure = todaysSpent,
        averageExpenditure = averageExpenditure
    )
}

internal fun buildLocalCategoryAnalytics(
    trackerId: String,
    expenses: List<ExpenseResponse>
): CategoryAnalyticsResponse {
    val totalSpent = expenses.sumOf { it.amount }
    val categories = expenses
        .groupBy { expense ->
            normalizeExpenseCategory(expense.category)
        }
        .map { (category, categoryExpenses) ->
            val categoryTotal = categoryExpenses.sumOf { it.amount }
            val percentage = if (totalSpent > 0.0) (categoryTotal / totalSpent) * 100 else 0.0
            CategoryAnalyticsItem(
                category = category,
                totalAmount = categoryTotal,
                percentage = percentage,
                expenseCount = categoryExpenses.size
            )
        }
        .sortedByDescending { it.totalAmount }

    return CategoryAnalyticsResponse(
        trackerId = trackerId,
        totalExpenditure = totalSpent,
        categories = categories
    )
}

internal fun parseDateOrNull(rawDate: String): LocalDate? {
    return runCatching { LocalDate.parse(rawDate.take(10)) }.getOrNull()
}

internal fun TrackerSummaryResponse.isActiveOn(date: LocalDate): Boolean {
    val start = parseDateOrNull(startDate) ?: return false
    val end = parseDateOrNull(endDate) ?: return false
    return !date.isBefore(start) && !date.isAfter(end)
}
