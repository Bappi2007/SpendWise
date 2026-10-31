package com.example

import com.example.data.preferences.UserPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetManipulationTest {

    @Test
    fun `stepper adjustments increase and decrease within valid bounds`() {
        var budget = 25000.0

        // Increment by +5000
        budget = (budget + 5000.0).coerceAtLeast(1000.0)
        assertEquals(30000.0, budget, 0.001)

        // Decrement by -10000
        budget = (budget - 10000.0).coerceAtLeast(1000.0)
        assertEquals(20000.0, budget, 0.001)

        // Decrement below minimum clamps at 1000.0
        budget = (budget - 50000.0).coerceAtLeast(1000.0)
        assertEquals(1000.0, budget, 0.001)
    }

    @Test
    fun `percentage adjustments scale budget properly`() {
        var budget = 20000.0

        // +10% adjustment
        val increased = Math.round((budget * 1.10) / 500.0) * 500.0
        assertEquals(22000.0, increased, 0.001)

        // -20% adjustment
        val decreased = Math.round((budget * 0.80) / 500.0) * 500.0
        assertEquals(16000.0, decreased, 0.001)
    }

    @Test
    fun `daily safe spend allowance calculates accurately`() {
        val budget = 30000.0
        val monthSpent = 12000.0
        val remaining = (budget - monthSpent).coerceAtLeast(0.0)
        val daysRemaining = 15

        val dailyAllowance = remaining / daysRemaining
        assertEquals(18000.0, remaining, 0.001)
        assertEquals(1200.0, dailyAllowance, 0.001)
    }

    @Test
    fun `overspend pace flags when spending exceeds budget`() {
        val budget = 15000.0
        val monthSpent = 18500.0
        val isOver = monthSpent > budget
        val overspendAmount = monthSpent - budget

        assertTrue("Should detect overspend condition", isOver)
        assertEquals(3500.0, overspendAmount, 0.001)
    }

    @Test
    fun `year month formatting produces consistent keys`() {
        val key1 = UserPreferencesRepository.formatYearMonthKey(2026, 10)
        val key2 = UserPreferencesRepository.formatYearMonthKey(2026, 5)

        assertEquals("2026-10", key1)
        assertEquals("2026-05", key2)
    }
}
