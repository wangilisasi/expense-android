package com.example.expensemanager.ui.viewmodels

import com.example.expensemanager.models.ExpenseResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseListDerivationsTest {

    @Test
    fun `daily expenses sorts days and transactions by occurred time descending`() {
        val response = buildLocalDailyExpenses(
            listOf(
                expense(
                    id = "old",
                    date = "2026-07-09",
                    occurredAt = "2026-07-09T08:00:00Z",
                    description = "Breakfast"
                ),
                expense(
                    id = "yesterday",
                    date = "2026-07-08",
                    occurredAt = "2026-07-08T20:00:00Z",
                    description = "Dinner yesterday"
                ),
                expense(
                    id = "new",
                    date = "2026-07-09",
                    occurredAt = "2026-07-09T20:00:00Z",
                    description = "Dinner"
                )
            )
        )

        assertEquals(listOf("2026-07-09", "2026-07-08"), response.daily_expenses.map { it.date })
        assertEquals(
            listOf("Dinner", "Breakfast"),
            response.daily_expenses.first().transactions.map { it.name }
        )
    }

    private fun expense(
        id: String,
        date: String,
        occurredAt: String,
        description: String
    ): ExpenseResponse {
        return ExpenseResponse(
            id = id,
            description = description,
            amount = 1.0,
            date = date,
            category = "Food",
            trackerId = "tracker",
            occurredAt = occurredAt,
            createdAt = occurredAt,
            updatedAt = occurredAt,
            isSynced = true
        )
    }
}
