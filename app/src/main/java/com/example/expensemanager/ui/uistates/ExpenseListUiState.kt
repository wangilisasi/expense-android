package com.example.expensemanager.ui.uistates

import com.example.expensemanager.models.ExpenseResponse

// This class represents a single, atomic snapshot of your UI's state
data class ExpenseListUiState(
    val isLoading: Boolean = false,
    val expenses: List<ExpenseResponse> = emptyList(),
    val error: String? = null
)