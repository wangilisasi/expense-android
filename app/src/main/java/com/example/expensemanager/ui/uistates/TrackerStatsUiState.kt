package com.example.expensemanager.ui.uistates

import com.example.expensemanager.models.StatsResponse


data class TrackerStatsUiState(
    val trackerStats: StatsResponse?= null,
    val trackerName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
