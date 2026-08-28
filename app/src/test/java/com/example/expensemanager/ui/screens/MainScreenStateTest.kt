package com.example.expensemanager.ui.screens

import com.example.expensemanager.ui.viewmodels.TrackerSessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenStateTest {

    @Test
    fun `active budget stays loading until expenses are hydrated`() {
        val state = resolveRenderedSessionState(
            sessionState = TrackerSessionState.ActiveBudget,
            hasActiveTracker = true,
            isExpenseDataReady = false
        )

        assertEquals(TrackerSessionState.Loading, state)
    }

    @Test
    fun `hydrated budget remains visible during background loading`() {
        val state = resolveRenderedSessionState(
            sessionState = TrackerSessionState.Loading,
            hasActiveTracker = true,
            isExpenseDataReady = true
        )

        assertEquals(TrackerSessionState.ActiveBudget, state)
    }

    @Test
    fun `no active budget keeps its session state`() {
        val state = resolveRenderedSessionState(
            sessionState = TrackerSessionState.NoActiveBudget,
            hasActiveTracker = false,
            isExpenseDataReady = false
        )

        assertEquals(TrackerSessionState.NoActiveBudget, state)
    }

    @Test
    fun `session error is not hidden by expense hydration`() {
        val error = TrackerSessionState.Error("Could not load budget")
        val state = resolveRenderedSessionState(
            sessionState = error,
            hasActiveTracker = true,
            isExpenseDataReady = false
        )

        assertEquals(error, state)
    }
}
