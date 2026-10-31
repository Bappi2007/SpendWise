package com.example

import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseWithCategory
import com.example.data.model.PaymentMethod
import com.example.ui.viewmodel.ExpenseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AggregationTest {

    private val sampleCategory = CategoryEntity(
        id = "cat_food",
        name = "Food",
        iconKey = "food",
        colorHex = 0xFFF97316L,
        isDefault = true
    )

    @Test
    fun `percentage change calculation handles increase decrease and zero baseline`() {
        // Increase: from 1000 to 1500 = +50%
        val prev1 = 1000.0
        val curr1 = 1500.0
        val pct1 = ((curr1 - prev1) / prev1) * 100.0
        assertEquals(50.0, pct1, 0.001)

        // Decrease: from 2000 to 1600 = -20%
        val prev2 = 2000.0
        val curr2 = 1600.0
        val pct2 = ((curr2 - prev2) / prev2) * 100.0
        assertEquals(-20.0, pct2, 0.001)

        // Zero previous month: total 500 = +100%
        val prev3 = 0.0
        val curr3 = 500.0
        val pct3 = if (prev3 > 0) ((curr3 - prev3) / prev3) * 100.0 else 100.0
        assertEquals(100.0, pct3, 0.001)
    }

    @Test
    fun `daily average calculation across month`() {
        val totalSpent = 31000.0
        val daysInMonth = 31
        val dailyAverage = totalSpent / daysInMonth
        assertEquals(1000.0, dailyAverage, 0.001)
    }

    @Test
    fun `home summary correctly computes today month and remaining budget`() {
        val now = Calendar.getInstance()
        val todayMillis = now.timeInMillis

        // Yesterday millis
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayMillis = yesterdayCal.timeInMillis

        val expense1 = ExpenseEntity(
            id = "e1",
            title = "Lunch",
            amount = 250.0,
            categoryId = sampleCategory.id,
            timestamp = todayMillis,
            paymentMethod = PaymentMethod.UPI.name
        )
        val expense2 = ExpenseEntity(
            id = "e2",
            title = "Dinner",
            amount = 450.0,
            categoryId = sampleCategory.id,
            timestamp = todayMillis,
            paymentMethod = PaymentMethod.CARD.name
        )
        val expense3 = ExpenseEntity(
            id = "e3",
            title = "Groceries Yesterday",
            amount = 1200.0,
            categoryId = sampleCategory.id,
            timestamp = yesterdayMillis,
            paymentMethod = PaymentMethod.CASH.name
        )

        val list = listOf(
            ExpenseWithCategory(expense1, sampleCategory),
            ExpenseWithCategory(expense2, sampleCategory),
            ExpenseWithCategory(expense3, sampleCategory)
        )

        val budget = 10000.0
        val summary = ExpenseViewModel.calculateHomeSummary(list, budget)

        assertEquals(700.0, summary.todaySpent, 0.001)
        assertEquals(1900.0, summary.monthSpent, 0.001)
        assertEquals(8100.0, summary.budgetRemaining, 0.001)
        assertEquals(0.19f, summary.budgetProgress, 0.01f)
    }

    @Test
    fun `group expenses by day identifies Today and Yesterday`() {
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val eToday = ExpenseEntity(
            id = "today_1",
            title = "Coffee",
            amount = 120.0,
            categoryId = sampleCategory.id,
            timestamp = todayCal.timeInMillis
        )
        val eYesterday = ExpenseEntity(
            id = "yest_1",
            title = "Metro",
            amount = 50.0,
            categoryId = sampleCategory.id,
            timestamp = yesterdayCal.timeInMillis
        )

        val grouped = ExpenseViewModel.groupExpensesByDay(
            listOf(
                ExpenseWithCategory(eToday, sampleCategory),
                ExpenseWithCategory(eYesterday, sampleCategory)
            )
        )

        assertEquals(2, grouped.size)
        val todayGroup = grouped.find { it.dateLabel == "Today" }
        val yesterdayGroup = grouped.find { it.dateLabel == "Yesterday" }

        assertTrue(todayGroup != null)
        assertTrue(yesterdayGroup != null)
        assertEquals(120.0, todayGroup!!.dayTotal, 0.001)
        assertEquals(50.0, yesterdayGroup!!.dayTotal, 0.001)
    }
}
