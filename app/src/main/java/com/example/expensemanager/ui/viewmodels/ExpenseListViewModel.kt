package com.example.expensemanager.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.ExpenseRepository
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.ExpenseTrackerResponse
import com.example.expensemanager.models.DailyExpense
import com.example.expensemanager.models.DailyExpensesResponse
import com.example.expensemanager.models.ExpenseTransaction
import com.example.expensemanager.models.StatsResponse
import com.example.expensemanager.ui.uistates.ExpenseListUiState
import com.example.expensemanager.ui.uistates.TrackerStatsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val TAG = "ExpenseListViewModel"
private const val SAVE_BUDGET_ERROR = "Failed to save budget."
private const val ADD_EXPENSE_ERROR = "Failed to add expense."

sealed class ExpenseListNavigationEvent {
    object NavigateToHome : ExpenseListNavigationEvent()
}

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val repository: ExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState

    private val _statsUiState = MutableStateFlow(TrackerStatsUiState())
    val statsUiState: StateFlow<TrackerStatsUiState> = _statsUiState

    private val _navigationEvents = MutableSharedFlow<ExpenseListNavigationEvent>(extraBufferCapacity = 1)
    val navigationEvents = _navigationEvents.asSharedFlow()

    private var currentTracker: ExpenseTrackerResponse? = null

    init {
        observeCurrentTrackerAndExpenses()
        refreshTrackersFromNetwork()
    }

    private fun observeCurrentTrackerAndExpenses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getTrackers().collectLatest { trackers ->
                val tracker = trackers.firstOrNull()
                currentTracker = tracker

                if (tracker == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            expenses = emptyList(),
                            dailyExpenses = DailyExpensesResponse(),
                            error = null
                        )
                    }
                    _statsUiState.update {
                        it.copy(
                            trackerStats = null,
                            trackerName = "",
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    return@collectLatest
                }

                _statsUiState.update {
                    it.copy(
                        trackerStats = buildLocalStats(tracker, _uiState.value.expenses),
                        trackerName = tracker.name,
                        isLoading = true,
                        errorMessage = null
                    )
                }

                coroutineScope {
                    launch {
                        repository.getExpenses(tracker.id).collect { expenses ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    expenses = expenses,
                                    dailyExpenses = buildLocalDailyExpenses(expenses),
                                    error = null
                                )
                            }
                            _statsUiState.update { state ->
                                state.copy(
                                    trackerStats = buildLocalStats(tracker, expenses),
                                    trackerName = tracker.name,
                                    isLoading = false,
                                    errorMessage = null
                                )
                            }
                        }
                    }

                    launch {
                        runCatching { repository.refreshExpenses(tracker.id) }
                            .onFailure { Log.w(TAG, "Could not refresh expenses from network", it) }
                    }
                }
            }
        }
    }

    private fun refreshTrackersFromNetwork() {
        viewModelScope.launch {
            runCatching { repository.syncAllFromBackend() }
                .onFailure { Log.w(TAG, "Could not sync all data from backend", it) }
            repository.triggerExpenseSync()
        }
    }

    fun submitBudget(name: String, budget: Double, startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                _statsUiState.update { it.copy(isLoading = true, errorMessage = null) }

                val trackerId = runCatching {
                    repository.refreshTrackers()
                    repository.getLatestTracker()?.id
                        ?: repository.getTrackers().firstOrNull()?.firstOrNull()?.id
                }.getOrNull() ?: currentTracker?.id
                Log.d(TAG, "submitBudget selected trackerId=$trackerId currentTrackerId=${currentTracker?.id}")

                if (trackerId == null) {
                    repository.createTracker(name, budget, startDate, endDate)
                } else {
                    repository.updateTracker(trackerId, name, budget, startDate, endDate)
                }

                runCatching { repository.refreshTrackers() }
                    .onFailure { Log.w(TAG, "Budget saved but tracker refresh failed", it) }
                _uiState.update { it.copy(isLoading = false, error = null) }
                _statsUiState.update { it.copy(isLoading = false, errorMessage = null) }
                _navigationEvents.emit(ExpenseListNavigationEvent.NavigateToHome)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save budget", e)
                val message = e.message ?: SAVE_BUDGET_ERROR
                _uiState.update { it.copy(isLoading = false, error = message) }
                _statsUiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        }
    }

    fun addExpense(description: String, amount: Double, date: String) {
        viewModelScope.launch {
            try {
                val trackerId = currentTracker?.id ?: return@launch
                repository.addExpense(description, amount, date, trackerId)
            } catch (e: Exception) {
                Log.e(TAG, "An error occurred while adding an expense", e)
                _uiState.update { it.copy(error = ADD_EXPENSE_ERROR) }
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expenseId)
            } catch (e: Exception) {
                Log.e(TAG, "An error occurred while deleting an expense", e)
                _uiState.update { it.copy(error = "Failed to delete expense.") }
            }
        }
    }
}

private fun buildLocalDailyExpenses(expenses: List<ExpenseResponse>): DailyExpensesResponse {
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
                        isSynced = expense.isSynced
                    )
                }
            )
        }

    return DailyExpensesResponse(daily_expenses = dailyTotals)
}

private fun buildLocalStats(
    tracker: ExpenseTrackerResponse,
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

private fun parseDateOrNull(rawDate: String): LocalDate? {
    return runCatching { LocalDate.parse(rawDate.take(10)) }.getOrNull()
}
