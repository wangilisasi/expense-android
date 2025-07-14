package com.example.expensemanager.ui

import com.example.expensemanager.models.Expense

// This class represents a single, atomic snapshot of your UI's state
data class ExpenseListUiState(
    val isLoading: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val error: String? = null
)