package com.example.expensemanager.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.ExpenseRepository
import com.example.expensemanager.models.Expense
import com.example.expensemanager.models.ExpenseRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ExpenseListViewModel"

// The ViewModel will be injected with the Repository
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {
    // --- A single StateFlow for the entire UI state ---
    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState

    init {
        // If you get it from navigation, you'll need a slightly different approach.
        loadExpenses(trackerId = 1)
    }


    fun addExpense(description: String, amount: Double, date: String) {
        viewModelScope.launch {
            try {
                val newExpense = ExpenseRequest(
                    // id = 0, // Or whatever your Expense model expects for a new item
                    trackerId=1, // Link to the current tracker
                    description = description,
                    amount = amount,
                    date = date // Assuming date is already a pre-formatted String
                )

                repository.addExpense(newExpense)
                Log.d(TAG, "Successfully added expense: $newExpense")

                // Reload the expenses after adding a new one
                loadExpenses(trackerId = 1)

            }catch (e: Exception) {
                Log.e(TAG, "An error occurred while adding an expense", e)
            }
        }

    }

    fun loadExpenses(trackerId: Int) {
        viewModelScope.launch {
            // Update the state using .copy() to ensure atomicity
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            Log.d(TAG, "UI State updated: isLoading = true, error = null")
            try {
                val loadedExpenses = repository.getExpenses(trackerId)
                Log.d(TAG, "Successfully loaded ${loadedExpenses.size} expenses for trackerId: $trackerId")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    expenses = loadedExpenses
                )
                Log.d(TAG, "UI State updated: isLoading = false, ${loadedExpenses.size} expenses loaded.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load expenses."
                )
                Log.d(TAG, "UI State updated: isLoading = false, error message set.")
                Log.e(TAG, "An error occurred while loading expenses", e)
            } finally {
             // Put some code here
            }
        }
    }
}