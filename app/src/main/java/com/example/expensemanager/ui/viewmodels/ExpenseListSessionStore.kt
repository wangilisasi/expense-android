package com.example.expensemanager.ui.viewmodels

import kotlinx.coroutines.Job
import com.example.expensemanager.models.TrackerSummaryResponse

internal class ExpenseListSessionStore {
    var currentTracker: TrackerSummaryResponse? = null
    var currentTrackerId: String? = null
    var hydratedTrackerId: String? = null
    var lastRefreshedTrackerId: String? = null
    var expensesObservationJob: Job? = null

    fun clearTrackerSelection() {
        currentTracker = null
        currentTrackerId = null
        hydratedTrackerId = null
        lastRefreshedTrackerId = null
        expensesObservationJob?.cancel()
        expensesObservationJob = null
    }
}
