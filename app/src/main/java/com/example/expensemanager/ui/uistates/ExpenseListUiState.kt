package com.example.expensemanager.ui.uistates

import com.example.expensemanager.models.DailyExpensesResponse
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.FALLBACK_EXPENSE_CATEGORIES
import com.example.expensemanager.models.TrackerSummaryResponse

// This class represents a single, atomic snapshot of your UI's state
data class ExpenseListUiState(
    val isLoading: Boolean = false,
    val expenses: List<ExpenseResponse> = emptyList(),
    val dailyExpenses: DailyExpensesResponse = DailyExpensesResponse(),
    val availableCategories: List<String> = FALLBACK_EXPENSE_CATEGORIES,
    val trackers: List<TrackerSummaryResponse> = emptyList(),
    val activeTracker: TrackerSummaryResponse? = null,
    val infoMessage: String? = null,
    val error: String? = null
)
