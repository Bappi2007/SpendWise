package com.example.data.model

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val colorHex: Long,
    val iconKey: String,
    val totalAmount: Double,
    val percentage: Float, // 0..100
    val transactionCount: Int,
    val budgetLimit: Double? = null,
    val budgetProgress: Float? = null
)

data class DailySpending(
    val dayOfMonth: Int,
    val dateEpochMillis: Long,
    val totalAmount: Double,
    val transactionCount: Int,
    val isWeekend: Boolean
)

data class MonthlyTrend(
    val yearMonth: String,
    val displayLabel: String,
    val totalAmount: Double
)

data class MonthSummary(
    val year: Int,
    val month: Int,
    val monthDisplayName: String,
    val totalSpent: Double,
    val previousMonthTotal: Double,
    val percentageChange: Double?, // null if previous month was 0
    val dailyAverage: Double,
    val highestSpendDayEpoch: Long?,
    val highestSpendDayAmount: Double,
    val categoryBreakdowns: List<CategorySpending>,
    val dailySpendings: List<DailySpending>,
    val monthlyTrends: List<MonthlyTrend>,
    val monthlyBudget: Double,
    val budgetProgress: Float,
    val isOverBudget: Boolean,
    val isMonthSpecificBudget: Boolean = false,
    val isMonthlyLimitEnabled: Boolean = true,
    val totalIncome: Double = 0.0
)
