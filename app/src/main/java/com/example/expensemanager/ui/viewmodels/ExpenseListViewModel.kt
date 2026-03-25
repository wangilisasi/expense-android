package com.example.expensemanager.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.ActiveTrackerResult
import com.example.expensemanager.data.ExpenseRepository
import com.example.expensemanager.data.isUnauthorized
import com.example.expensemanager.data.toUserFacingMessage
import com.example.expensemanager.models.CategoryAnalyticsItem
import com.example.expensemanager.models.CategoryAnalyticsResponse
import com.example.expensemanager.models.DEFAULT_EXPENSE_CATEGORY
import com.example.expensemanager.models.DEFAULT_EXPENSE_SELECTION_CATEGORY
import com.example.expensemanager.models.DailyExpense
import com.example.expensemanager.models.DailyExpensesResponse
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.ExpenseTransaction
import com.example.expensemanager.models.FALLBACK_EXPENSE_CATEGORIES
import com.example.expensemanager.models.StatsResponse
import com.example.expensemanager.models.TrackerSummaryResponse
import com.example.expensemanager.models.normalizeExpenseCategory
import com.example.expensemanager.ui.uistates.ExpenseListUiState
import com.example.expensemanager.ui.uistates.TrackerStatsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val TAG = "ExpenseListViewModel"
private const val LOAD_BUDGETS_ERROR = "Failed to load your budgets."
private const val SAVE_BUDGET_ERROR = "Failed to save budget."
private const val ADD_EXPENSE_ERROR = "Failed to add expense."

sealed class ExpenseListNavigationEvent {
    data object NavigateToHome : ExpenseListNavigationEvent()
}

sealed interface TrackerSessionState {
    data object Idle : TrackerSessionState
    data object Loading : TrackerSessionState
    data object ActiveBudget : TrackerSessionState
    data object NoActiveBudget : TrackerSessionState
    data object RequiresLogin : TrackerSessionState
    data class Error(val message: String) : TrackerSessionState
}

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val repository: ExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState

    private val _statsUiState = MutableStateFlow(TrackerStatsUiState())
    val statsUiState: StateFlow<TrackerStatsUiState> = _statsUiState

    private val _navigationEvents =
        MutableSharedFlow<ExpenseListNavigationEvent>(extraBufferCapacity = 1)
    val navigationEvents = _navigationEvents.asSharedFlow()

    private val _sessionState = MutableStateFlow<TrackerSessionState>(TrackerSessionState.Idle)
    val sessionState: StateFlow<TrackerSessionState> = _sessionState

    private var currentTracker: TrackerSummaryResponse? = null
    private var currentTrackerId: String? = null
    private var expensesObservationJob: Job? = null
    private var lastRefreshedTrackerId: String? = null

    init {
        observeCurrentTrackerAndExpenses()
        refreshCategoriesFromNetwork()
    }

    fun bootstrapSession(force: Boolean = false) {
        if (!force && _sessionState.value == TrackerSessionState.Loading) {
            return
        }

        viewModelScope.launch {
            loadSession(showLoading = true)
        }
    }

    fun clearSession() {
        currentTracker = null
        currentTrackerId = null
        lastRefreshedTrackerId = null
        expensesObservationJob?.cancel()
        expensesObservationJob = null
        _sessionState.value = TrackerSessionState.Idle
        _uiState.value = ExpenseListUiState(availableCategories = _uiState.value.availableCategories)
        _statsUiState.value = TrackerStatsUiState()

        viewModelScope.launch {
            repository.clearLocalData()
        }
    }

    fun dismissInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
        _statsUiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun loadSession(showLoading: Boolean) {
        if (showLoading) {
            _sessionState.value = TrackerSessionState.Loading
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            _statsUiState.update { it.copy(isLoading = true, errorMessage = null) }
        }

        runCatching { repository.refreshTrackers() }
            .onFailure { throwable ->
                if (throwable.isUnauthorized()) {
                    _sessionState.value = TrackerSessionState.RequiresLogin
                    return
                }
                Log.w(TAG, "Could not refresh trackers from backend", throwable)
            }

        val activeResult = runCatching { repository.getActiveTracker() }
        activeResult.onFailure { throwable ->
            if (throwable.isUnauthorized()) {
                _sessionState.value = TrackerSessionState.RequiresLogin
                return
            }

            Log.e(TAG, "Failed to load active tracker", throwable)
            val message = throwable.toUserFacingMessage(LOAD_BUDGETS_ERROR)
            _sessionState.value = TrackerSessionState.Error(message)
            _uiState.update { it.copy(isLoading = false, error = message) }
            _statsUiState.update { it.copy(isLoading = false, errorMessage = message) }
            return
        }

        when (val result = activeResult.getOrThrow()) {
            is ActiveTrackerResult.Found -> {
                currentTrackerId = result.tracker.id
                _sessionState.value = TrackerSessionState.ActiveBudget
                _uiState.update { it.copy(error = null, infoMessage = null) }
                _statsUiState.update { it.copy(errorMessage = null) }
            }

            ActiveTrackerResult.None -> {
                currentTracker = null
                currentTrackerId = null
                lastRefreshedTrackerId = null
                expensesObservationJob?.cancel()
                expensesObservationJob = null
                _sessionState.value = TrackerSessionState.NoActiveBudget
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeTracker = null,
                        expenses = emptyList(),
                        dailyExpenses = DailyExpensesResponse(),
                        error = null
                    )
                }
                _statsUiState.update {
                    it.copy(
                        trackerStats = null,
                        categoryAnalytics = null,
                        trackerName = "",
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }

        repository.triggerExpenseSync()
    }

    private fun observeCurrentTrackerAndExpenses() {
        viewModelScope.launch {
            repository.getTrackers().collect { trackers ->
                val previousTrackerId = currentTracker?.id
                val tracker = currentTrackerId?.let { selectedId ->
                    trackers.firstOrNull { it.id == selectedId }
                }
                currentTracker = tracker

                _uiState.update { state ->
                    state.copy(
                        trackers = trackers,
                        activeTracker = tracker
                    )
                }

                if (tracker == null) {
                    lastRefreshedTrackerId = null
                    expensesObservationJob?.cancel()
                    expensesObservationJob = null
                    _uiState.update {
                        it.copy(
                            isLoading = _sessionState.value == TrackerSessionState.Loading,
                            expenses = emptyList(),
                            dailyExpenses = DailyExpensesResponse()
                        )
                    }
                    _statsUiState.update {
                        it.copy(
                            trackerStats = null,
                            categoryAnalytics = null,
                            trackerName = "",
                            isLoading = _sessionState.value == TrackerSessionState.Loading
                        )
                    }
                    return@collect
                }

                val trackerChanged = previousTrackerId != tracker.id

                _statsUiState.update {
                    it.copy(
                        trackerStats = buildLocalStats(tracker, _uiState.value.expenses),
                        categoryAnalytics = buildLocalCategoryAnalytics(tracker.id, _uiState.value.expenses),
                        trackerName = tracker.name,
                        isLoading = trackerChanged,
                        errorMessage = null
                    )
                }

                if (trackerChanged || expensesObservationJob == null) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    expensesObservationJob?.cancel()
                    expensesObservationJob = viewModelScope.launch {
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
                                    categoryAnalytics = buildLocalCategoryAnalytics(tracker.id, expenses),
                                    trackerName = tracker.name,
                                    isLoading = false,
                                    errorMessage = null
                                )
                            }
                        }
                    }
                }

                if (lastRefreshedTrackerId != tracker.id) {
                    lastRefreshedTrackerId = tracker.id
                    viewModelScope.launch {
                        runCatching { repository.refreshExpenses(tracker.id) }
                            .onFailure { Log.w(TAG, "Could not refresh expenses from network", it) }
                        refreshCategoryAnalyticsFromNetwork(tracker.id)
                    }
                }
            }
        }
    }

    private fun refreshCategoriesFromNetwork() {
        viewModelScope.launch {
            val categories = runCatching { repository.getCategories() }
                .getOrElse { FALLBACK_EXPENSE_CATEGORIES }
                .ifEmpty { FALLBACK_EXPENSE_CATEGORIES }
            _uiState.update {
                it.copy(availableCategories = categories)
            }
        }
    }

    private suspend fun refreshCategoryAnalyticsFromNetwork(trackerId: String) {
        if (_uiState.value.expenses.any { !it.isSynced }) {
            return
        }
        val analytics = repository.getCategoryAnalyticsForTracker(trackerId) ?: return
        _statsUiState.update { state ->
            state.copy(categoryAnalytics = analytics)
        }
    }

    fun submitBudget(name: String, budget: Double, startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
                _statsUiState.update { it.copy(isLoading = true, errorMessage = null) }

                val trackerId = currentTracker?.id
                if (trackerId == null) {
                    repository.createTracker(name, budget, startDate, endDate)
                } else {
                    repository.updateTracker(trackerId, name, budget, startDate, endDate)
                }

                loadSession(showLoading = false)
                when (_sessionState.value) {
                    TrackerSessionState.ActiveBudget -> {
                        _uiState.update { it.copy(isLoading = false, error = null, infoMessage = null) }
                        _statsUiState.update { it.copy(isLoading = false, errorMessage = null) }
                        _navigationEvents.emit(ExpenseListNavigationEvent.NavigateToHome)
                    }

                    TrackerSessionState.NoActiveBudget -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                infoMessage = buildSavedBudgetMessage(startDate)
                            )
                        }
                        _statsUiState.update { it.copy(isLoading = false, errorMessage = null) }
                    }

                    else -> Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save budget", e)
                val message = e.toUserFacingMessage(SAVE_BUDGET_ERROR)
                _uiState.update { it.copy(isLoading = false, error = message) }
                _statsUiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        }
    }

    fun addExpense(
        description: String,
        amount: Double,
        date: String,
        category: String = DEFAULT_EXPENSE_CATEGORY
    ) {
        viewModelScope.launch {
            try {
                val trackerId = currentTracker?.id
                if (trackerId == null) {
                    _uiState.update { it.copy(error = "No active budget available. Create one first.") }
                    return@launch
                }

                val allowedCategories = _uiState.value.availableCategories
                val requestedCategory = normalizeExpenseCategory(category)
                val safeCategory = allowedCategories.firstOrNull { it == requestedCategory }
                    ?: allowedCategories.firstOrNull { it == DEFAULT_EXPENSE_SELECTION_CATEGORY }
                    ?: DEFAULT_EXPENSE_CATEGORY
                repository.addExpense(description.trim(), amount, date, trackerId, safeCategory)
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

    private fun buildSavedBudgetMessage(startDate: String): String {
        val start = parseDateOrNull(startDate)
        return if (start != null && start.isAfter(LocalDate.now())) {
            "Budget saved. It will become active on $startDate."
        } else {
            "Budget saved. No active budget is available for today yet."
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
                        category = normalizeExpenseCategory(expense.category),
                        isSynced = expense.isSynced
                    )
                }
            )
        }

    return DailyExpensesResponse(daily_expenses = dailyTotals)
}

private fun buildLocalStats(
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

private fun buildLocalCategoryAnalytics(
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

private fun parseDateOrNull(rawDate: String): LocalDate? {
    return runCatching { LocalDate.parse(rawDate.take(10)) }.getOrNull()
}
