package com.example.expensemanager.ui.viewmodels

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
