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
import kotlinx.coroutines.flow.firstOrNull
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
        getCurrentTrackerAndObserveExpenses()

    }


    private fun getCurrentTrackerAndObserveExpenses() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val tracker = repository.getTrackers().firstOrNull()?.firstOrNull()
                currentTracker = tracker
                _uiState.update { it.copy(isLoading = false) }

              if (tracker != null) {
                  repository.getExpenses(tracker.id).collect { expenses ->
                      _uiState.update {
                          it.copy(isLoading = false, expenses = expenses)
                      }
                  }

                  // Also trigger a one-time refresh from the network
                  repository.refreshExpenses(tracker.id)
              }


            } catch (e: Exception) {
                Log.e(TAG, "Failed to load current tracker details", e)
                // Handle error, maybe update UI state
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }




    fun addExpense(description: String, amount: Double, date: String) {
        viewModelScope.launch {
            try {
                val trackerId = currentTracker?.id?:return@launch

                repository.addExpense(description, amount, date, trackerId)


            } catch (e: Exception) {
                Log.e(TAG, "An error occurred while adding an expense", e)
                // ✅ Notify the UI of the failure
                _uiState.update { it.copy(error = "Failed to add expense.") }
                // On failure, send an error event

            }
        }
    }


}
