package com.example.expensemanager.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.ExpenseRepository
import com.example.expensemanager.models.ExpenseRequest
import com.example.expensemanager.models.ExpenseTrackerRequest
import com.example.expensemanager.models.ExpenseTrackerResponse
import com.example.expensemanager.ui.uistates.ExpenseListUiState
import com.example.expensemanager.ui.uistates.TrackerStatsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ExpenseListViewModel"

sealed class ExpenseListNavigationEvent {
    object NavigateToBudgetSetup : ExpenseListNavigationEvent()
    object NavigateToHome : ExpenseListNavigationEvent()

}



@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    // tokenManager removed as it was unused
) : ViewModel() {


    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState

    private val _statsUiState = MutableStateFlow(TrackerStatsUiState())
    val statsUiState: StateFlow<TrackerStatsUiState> = _statsUiState

    private val _navigationEvents = MutableSharedFlow<ExpenseListNavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()
    private var currentTracker: ExpenseTrackerResponse? = null

    init {
        getCurrentTracker()

    }


    private fun getCurrentTracker() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                _statsUiState.update { it.copy(isLoading = true, errorMessage = null) }
                // Assuming getTrackers() returns a list and you're interested in the first one
                val tracker = repository.getTrackers().firstOrNull() // Or however you get the specific tracker
                currentTracker = tracker
                _uiState.update { it.copy(isLoading = false) }
                //Load Expenses and stats after the async op to load the tracker
                loadExpenses()
                loadStats()
                getDailyExpenses()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load current tracker details", e)
                // Handle error, maybe update UI state
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }



    fun updateTracker(name: String, budget: Double, startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val trackerId = currentTracker?.id
                if (trackerId == null) {
                    //Navigate to Budget Setup
                    _navigationEvents.emit(ExpenseListNavigationEvent.NavigateToBudgetSetup)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                val expenseTrackerRequest = ExpenseTrackerRequest(
                    name = name,
                    budget = budget,
                    description = "My Good Budget",
                    startDate = startDate,
                    endDate = endDate
                )

                val response = repository.updateTracker(trackerId, expenseTrackerRequest)
                currentTracker = response.body()
                _uiState.update { it.copy(isLoading = false) }
                // Reload expenses to show the new one
                loadExpenses()
                loadStats()

            }catch (e: Exception) {
                Log.e(TAG, "Failed to load current tracker details", e)
                // Handle error, maybe update UI state
            }
        }
    }



    fun addExpense(description: String, amount: Double, date: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val trackerId = currentTracker?.id
                if (trackerId == null) {
                    //Navigate to Budget Setup
                    _navigationEvents.emit(ExpenseListNavigationEvent.NavigateToBudgetSetup)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val newExpense = ExpenseRequest(
                    trackerId = trackerId,
                    description = description,
                    amount = amount,
                    date = date
                )

                repository.addExpense(newExpense)
                // On success, send the navigation event
                _uiState.update { it.copy(isLoading = false) }


                // Reload expenses to show the new one
                loadExpenses()
                loadStats()
                getDailyExpenses()

            } catch (e: Exception) {
                Log.e(TAG, "An error occurred while adding an expense", e)
                // ✅ Notify the UI of the failure
                _uiState.update { it.copy(error = "Failed to add expense.") }
                // On failure, send an error event

            }
        }
    }

    fun getDailyExpenses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try{
                val trackerId = currentTracker?.id
                if (trackerId == null) {
                    //Navigate to Budget Setup
                    _navigationEvents.emit(ExpenseListNavigationEvent.NavigateToBudgetSetup)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val dailyExpenses = repository.getDailyExpenses(trackerId)
                _uiState.update {
                    it.copy(isLoading = false, dailyExpenses = dailyExpenses)
                }


            }catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load stats.")
                }
            }
        }
    }


    fun loadStats() {
        viewModelScope.launch {
            _statsUiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val trackerId = currentTracker?.id
                if (trackerId == null) {
                    //Navigate to Budget Setup
                    _navigationEvents.emit(ExpenseListNavigationEvent.NavigateToBudgetSetup)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                val stats = repository.getStats(trackerId)
                _statsUiState.update {
                    it.copy(isLoading = false, trackerStats = stats)
                }
            } catch (e: Exception) {
                _statsUiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load stats.")
                }
            }
        }
    }

    fun createBudget(name: String, budget: Double, startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val expenseTrackerRequest = ExpenseTrackerRequest(
                    name = name,
                    budget = budget,
                    description = "My Good Budget",
                    startDate = startDate,
                    endDate = endDate
                )
                repository.createTracker(expenseTrackerRequest)
                // On success, send the navigation event
                _navigationEvents.emit(ExpenseListNavigationEvent.NavigateToHome)
                _uiState.update { it.copy(isLoading = false) }
                // Reload expenses to show the new one
                loadExpenses()
                loadStats()

            } catch (e: Exception) {
                Log.e(TAG, "An error occurred while adding an expense", e)
                // ✅ Notify the UI of the failure
                _uiState.update { it.copy(error = "Failed to add expense.") }
                // On failure, send an error event
            }
        }
    }

    fun loadExpenses() {
        viewModelScope.launch {
            // Using .update is a concise way to modify StateFlow
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // ✅ Use the new helper function
                val trackerId = currentTracker?.id
                if (trackerId == null) {
                    //Navigate to Budget Setup
                    _navigationEvents.emit(ExpenseListNavigationEvent.NavigateToBudgetSetup)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val loadedExpenses = repository.getExpenses(trackerId)
                _uiState.update {
                    it.copy(isLoading = false, expenses = loadedExpenses)
                }
                Log.d(TAG, "Successfully loaded ${loadedExpenses.size} expenses.")

            } catch (e: Exception) {
                _uiState.update {
                    // ✅ Provide a more specific error message from the exception if possible
                    it.copy(isLoading = false, error = e.message ?: "Failed to load expenses.")
                }
                Log.e(TAG, "An error occurred while loading expenses", e)
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isLoading = true, error = null)
                }
                repository.deleteExpense(expenseId)
                _uiState.update {
                    it.copy(isLoading = false)
                }
                // Update local list of expenses to show the new one
                _uiState.update { currentState ->
                    currentState.copy(
                        expenses = currentState.expenses.filterNot { it.id == expenseId }
                    )
                }
                loadExpenses()
                loadStats()
                getDailyExpenses()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete expense", e)
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to delete expense.")
                }
            }
        }
    }
}
