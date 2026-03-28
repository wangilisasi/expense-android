package com.example.expensemanager.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.ActiveTrackerResult
import com.example.expensemanager.data.ExpenseRepository
import com.example.expensemanager.data.isUnauthorized
import com.example.expensemanager.data.toUserFacingMessage
import com.example.expensemanager.models.DEFAULT_EXPENSE_CATEGORY
import com.example.expensemanager.models.DEFAULT_EXPENSE_SELECTION_CATEGORY
import com.example.expensemanager.models.DailyExpensesResponse
import com.example.expensemanager.models.FALLBACK_EXPENSE_CATEGORIES
import com.example.expensemanager.models.TrackerSummaryResponse
import com.example.expensemanager.models.normalizeExpenseCategory
import com.example.expensemanager.ui.uistates.ExpenseListUiState
import com.example.expensemanager.ui.uistates.TrackerStatsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "ExpenseListViewModel"
private const val LOAD_BUDGETS_ERROR = "Failed to load your budgets."
private const val SAVE_BUDGET_ERROR = "Failed to save budget."
private const val ADD_EXPENSE_ERROR = "Failed to add expense."
private const val OFFLINE_MODE_MESSAGE = "Offline mode"

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

    private val sessionStore = ExpenseListSessionStore()

    init {
        observeCurrentTrackerAndExpenses()
        refreshCategoriesFromNetwork()
    }

    fun bootstrapSession(force: Boolean = false) {
        if (!force) {
            when (_sessionState.value) {
                TrackerSessionState.Idle -> Unit
                TrackerSessionState.Loading,
                TrackerSessionState.ActiveBudget,
                TrackerSessionState.NoActiveBudget,
                TrackerSessionState.RequiresLogin,
                is TrackerSessionState.Error -> return
            }
        }

        viewModelScope.launch {
            loadSession(showLoading = true)
        }
    }

    fun clearSession() {
        sessionStore.clearTrackerSelection()
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

        val cachedTrackers = repository.getCachedTrackers()
        applyLocalSession(cachedTrackers)

        val trackersRefresh = runCatching { repository.refreshTrackers() }
        trackersRefresh.onFailure { throwable ->
            if (throwable.isUnauthorized()) {
                _sessionState.value = TrackerSessionState.RequiresLogin
                return
            }
            Log.w(TAG, "Could not refresh trackers from backend", throwable)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = null,
                    infoMessage = OFFLINE_MODE_MESSAGE
                )
            }
            _statsUiState.update { it.copy(isLoading = false, errorMessage = null) }
            repository.triggerExpenseSync()
            return
        }

        val activeResult = runCatching { repository.getActiveTracker() }
        activeResult.onFailure { throwable ->
            if (throwable.isUnauthorized()) {
                _sessionState.value = TrackerSessionState.RequiresLogin
                return
            }

            Log.w(TAG, "Failed to refresh active tracker, continuing with cached data", throwable)
            applyLocalSession(repository.getCachedTrackers(), OFFLINE_MODE_MESSAGE)
            return
        }

        when (val result = activeResult.getOrThrow()) {
            is ActiveTrackerResult.Found -> {
                sessionStore.currentTrackerId = result.tracker.id
                _sessionState.value = TrackerSessionState.ActiveBudget
                _uiState.update { it.copy(isLoading = false, error = null, infoMessage = null) }
                _statsUiState.update { it.copy(errorMessage = null) }
            }

            ActiveTrackerResult.None -> {
                showNoActiveBudget(infoMessage = null)
            }
        }

        repository.triggerExpenseSync()
    }

    private fun observeCurrentTrackerAndExpenses() {
        viewModelScope.launch {
            repository.getTrackers().collect { trackers ->
                val previousTrackerId = sessionStore.currentTracker?.id
                val tracker = sessionStore.currentTrackerId?.let { selectedId ->
                    trackers.firstOrNull { it.id == selectedId }
                } ?: trackers.firstOrNull { it.isActiveOn(LocalDate.now()) }
                sessionStore.currentTrackerId = tracker?.id
                sessionStore.currentTracker = tracker

                _uiState.update { state ->
                    state.copy(
                        trackers = trackers,
                        activeTracker = tracker
                    )
                }

                if (tracker == null) {
                    sessionStore.clearTrackerSelection()
                    if (_sessionState.value != TrackerSessionState.Loading) {
                        _sessionState.value = TrackerSessionState.NoActiveBudget
                    }
                    clearTrackerPresentation(
                        isLoading = _sessionState.value == TrackerSessionState.Loading
                    )
                    return@collect
                }

                if (_sessionState.value != TrackerSessionState.Loading) {
                    _sessionState.value = TrackerSessionState.ActiveBudget
                }

                val trackerChanged = previousTrackerId != tracker.id
                val waitingForTrackerExpenses = sessionStore.hydratedTrackerId != tracker.id

                _statsUiState.update {
                    it.copy(
                        trackerStats = buildLocalStats(tracker, _uiState.value.expenses),
                        categoryAnalytics = buildLocalCategoryAnalytics(tracker.id, _uiState.value.expenses),
                        trackerName = tracker.name,
                        isLoading = waitingForTrackerExpenses,
                        errorMessage = null
                    )
                }

                if (trackerChanged || sessionStore.expensesObservationJob == null) {
                    sessionStore.hydratedTrackerId = null
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    sessionStore.expensesObservationJob?.cancel()
                    sessionStore.expensesObservationJob = viewModelScope.launch {
                        repository.getExpenses(tracker.id).collect { expenses ->
                            sessionStore.hydratedTrackerId = tracker.id
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

                if (sessionStore.lastRefreshedTrackerId != tracker.id) {
                    sessionStore.lastRefreshedTrackerId = tracker.id
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

                val trackerId = sessionStore.currentTracker?.id
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
                val trackerId = sessionStore.currentTracker?.id
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

    private fun applyLocalSession(
        trackers: List<TrackerSummaryResponse>,
        infoMessage: String? = null
    ) {
        val activeTracker = trackers.firstOrNull { tracker ->
            tracker.isActiveOn(LocalDate.now())
        }
        sessionStore.currentTrackerId = activeTracker?.id

        if (activeTracker != null) {
            _sessionState.value = TrackerSessionState.ActiveBudget
            sessionStore.hydratedTrackerId = null
            _uiState.update {
                it.copy(
                    isLoading = true,
                    trackers = trackers,
                    activeTracker = activeTracker,
                    error = null,
                    infoMessage = infoMessage
                )
            }
            _statsUiState.update { it.copy(isLoading = true, errorMessage = null) }
        } else {
            showNoActiveBudget(trackers = trackers, infoMessage = infoMessage)
        }
    }

    private fun showNoActiveBudget(
        trackers: List<TrackerSummaryResponse> = _uiState.value.trackers,
        infoMessage: String? = _uiState.value.infoMessage
    ) {
        sessionStore.clearTrackerSelection()
        _sessionState.value = TrackerSessionState.NoActiveBudget
        _uiState.update {
            it.copy(
                isLoading = false,
                trackers = trackers,
                activeTracker = null,
                expenses = emptyList(),
                dailyExpenses = DailyExpensesResponse(),
                error = null,
                infoMessage = infoMessage
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

    private fun clearTrackerPresentation(isLoading: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = isLoading,
                activeTracker = null,
                expenses = emptyList(),
                dailyExpenses = DailyExpensesResponse()
            )
        }
        _statsUiState.update {
            it.copy(
                trackerStats = null,
                categoryAnalytics = null,
                trackerName = "",
                isLoading = isLoading,
                errorMessage = null
            )
        }
    }
}
