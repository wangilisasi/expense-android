package com.example.expensemanager.ui.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DefaultMonthlyBudgetCycleTest {

    @Test
    fun `cycle starts on current month 24th when today is 24th`() {
        val cycle = defaultMonthlyBudgetCycle(LocalDate.of(2026, 4, 24))

        assertEquals("2026-04-24", cycle.startDate)
        assertEquals("2026-05-23", cycle.endDate)
    }

    @Test
    fun `cycle uses previous month 24th when today is before 24th`() {
        val cycle = defaultMonthlyBudgetCycle(LocalDate.of(2026, 4, 23))

        assertEquals("2026-03-24", cycle.startDate)
        assertEquals("2026-04-23", cycle.endDate)
    }

    @Test
    fun `automatic budget spec for tomorrow creates next cycle with default amount`() {
        val spec = defaultMonthlyBudgetSpec(LocalDate.of(2026, 4, 24))

        assertEquals("Monthly Budget", spec.name)
        assertEquals(1_000_000.0, spec.amount, 0.0)
        assertEquals("2026-04-24", spec.cycle.startDate)
        assertEquals("2026-05-23", spec.cycle.endDate)
    }
}
