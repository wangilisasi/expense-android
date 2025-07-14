package com.example.expensemanager.data

import com.example.expensemanager.api.ExpenseApiService
import com.example.expensemanager.models.Expense
import com.example.expensemanager.models.ExpenseTracker
import javax.inject.Inject

// The repository will be injected with the API service
class ExpenseRepository @Inject constructor(private val apiService: ExpenseApiService) {
    suspend fun getTrackers(): List<ExpenseTracker> {
        return apiService.getTrackers()
    }

    suspend fun getExpenses(trackerId: Int): List<Expense> {
        return apiService.getExpensesForTracker(trackerId)
    }

    suspend fun addExpense(expense: Expense) {
        apiService.addExpense(expense)
    }

    // ... other functions like deleteExpense, etc.
}