package com.example.expensemanager.data

import com.example.expensemanager.api.ExpenseApiService
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.ExpenseRequest
import com.example.expensemanager.models.ExpenseTrackerResponse
import javax.inject.Inject

// The repository will be injected with the API service
class ExpenseRepository @Inject constructor(private val apiService: ExpenseApiService) {
    suspend fun getTrackers(): List<ExpenseTrackerResponse> {
        return apiService.getTrackers()
    }

    suspend fun getExpenses(trackerId: Int): List<ExpenseResponse> {
        return apiService.getExpensesForTracker(trackerId)
    }

    suspend fun addExpense(expenseRequest: ExpenseRequest) {
        apiService.addExpense(expenseRequest)
    }

    // ... other functions like deleteExpense, etc.
}