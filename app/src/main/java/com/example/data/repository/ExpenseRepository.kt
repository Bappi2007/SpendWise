package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.model.CategoryEntity
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpending
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseWithCategory
import com.example.data.model.MonthSummary
import com.example.data.model.MonthlyTrend
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.seeder.SampleDataSeeder
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val preferencesRepository: UserPreferencesRepository
) {

    val allExpensesWithCategory: Flow<List<ExpenseWithCategory>> =
        expenseDao.getAllExpensesWithCategory()

    val allCategories: Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    val userPreferences: Flow<UserPreferences> =
        preferencesRepository.userPreferencesFlow

    fun getExpensesForDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ExpenseWithCategory>> =
        expenseDao.getExpensesByRange(startTimestamp, endTimestamp)

    suspend fun getExpensesForDateRangeSync(startTimestamp: Long, endTimestamp: Long): List<ExpenseWithCategory> =
        expenseDao.getExpensesByRangeSync(startTimestamp, endTimestamp)

    suspend fun getAllExpensesSync(): List<ExpenseWithCategory> =
        expenseDao.getAllExpensesWithCategorySync()

    suspend fun getAllCategoriesSync(): List<CategoryEntity> =
        categoryDao.getAllCategoriesSync()

    suspend fun insertExpense(expense: ExpenseEntity) =
        expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: ExpenseEntity) =
        expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) =
        expenseDao.deleteExpense(expense)

    suspend fun deleteExpenseById(id: String) =
        expenseDao.deleteExpenseById(id)

    suspend fun insertCategory(category: CategoryEntity) =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    /**
     * Requirement 3: Deleting a category moves its expenses to "Other"
     */
    suspend fun deleteCategory(category: CategoryEntity) {
        // Ensure Other category exists
        val otherCategory = categoryDao.getCategoryById(SampleDataSeeder.DEFAULT_CATEGORY_OTHER_ID)
            ?: categoryDao.getCategoryByName("Other")
            ?: run {
                val fallbackOther = CategoryEntity(
                    id = SampleDataSeeder.DEFAULT_CATEGORY_OTHER_ID,
                    name = "Other",
                    iconKey = "other",
                    colorHex = 0xFF64748B,
                    isDefault = true,
                    sortOrder = 99
                )
                categoryDao.insertCategory(fallbackOther)
                fallbackOther
            }

        // Reassign all expenses belonging to this category to Other
        categoryDao.reassignExpenses(category.id, otherCategory.id)
        categoryDao.deleteCategory(category)
    }

    suspend fun calculateMonthSummary(
        year: Int,
        month: Int, // 1-based: 1 for January, 12 for December
        monthlyBudget: Double
    ): MonthSummary {
        val (startCurrent, endCurrent) = getMonthBounds(year, month)
        val currentExpenses = expenseDao.getExpensesByRangeSync(startCurrent, endCurrent)

        // Previous month bounds
        val prevCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // 0-based
            add(Calendar.MONTH, -1)
        }
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH) + 1
        val (startPrev, endPrev) = getMonthBounds(prevYear, prevMonth)
        val prevExpenses = expenseDao.getExpensesByRangeSync(startPrev, endPrev)

        val totalSpent = currentExpenses.sumOf { it.expense.amount }
        val previousMonthTotal = prevExpenses.sumOf { it.expense.amount }

        val percentageChange: Double? = if (previousMonthTotal > 0.0) {
            ((totalSpent - previousMonthTotal) / previousMonthTotal) * 100.0
        } else if (totalSpent > 0.0) {
            100.0
        } else {
            0.0
        }

        val daysInMonth = getDaysInMonth(year, month)
        val dailyAverage = if (daysInMonth > 0) totalSpent / daysInMonth else 0.0

        // Daily breakdowns
        val dayExpensesMap = currentExpenses.groupBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.expense.timestamp }
            cal.get(Calendar.DAY_OF_MONTH)
        }

        var maxSpendDayEpoch: Long? = null
        var maxSpendDayAmount = 0.0

        val dailyList = mutableListOf<DailySpending>()
        val calDay = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }

        for (day in 1..daysInMonth) {
            calDay.set(Calendar.DAY_OF_MONTH, day)
            calDay.set(Calendar.HOUR_OF_DAY, 12)
            val dayEpoch = calDay.timeInMillis
            val dayOfWeek = calDay.get(Calendar.DAY_OF_WEEK)
            val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

            val dayExpenses = dayExpensesMap[day] ?: emptyList()
            val dayTotal = dayExpenses.sumOf { it.expense.amount }
            if (dayTotal > maxSpendDayAmount) {
                maxSpendDayAmount = dayTotal
                maxSpendDayEpoch = dayEpoch
            }

            dailyList.add(
                DailySpending(
                    dayOfMonth = day,
                    dateEpochMillis = dayEpoch,
                    totalAmount = dayTotal,
                    transactionCount = dayExpenses.size,
                    isWeekend = isWeekend
                )
            )
        }

        // Category breakdown
        val categoryMap = currentExpenses.groupBy { it.categoryName }
        val allCategories = categoryDao.getAllCategoriesSync().associateBy { it.name }

        val prefs = preferencesRepository.getPreferencesSync()
        val monthKey = UserPreferencesRepository.formatYearMonthKey(year, month)
        val isMonthSpecific = prefs.monthBudgets.containsKey(monthKey)
        val effectiveBudget = prefs.monthBudgets[monthKey] ?: if (monthlyBudget > 0) monthlyBudget else prefs.monthlyBudget

        val categoryBreakdowns = categoryMap.map { (catName, items) ->
            val catTotal = items.sumOf { it.expense.amount }
            val catEntity = allCategories[catName] ?: items.firstOrNull()?.category
            val catId = catEntity?.id ?: catName
            val colorHex = catEntity?.colorHex ?: 0xFF64748B
            val iconKey = catEntity?.iconKey ?: "other"
            val pct = if (totalSpent > 0) ((catTotal / totalSpent) * 100).toFloat() else 0f
            val catBudget = prefs.categoryBudgets[catId]
            val catProgress = if (catBudget != null && catBudget > 0) (catTotal / catBudget).toFloat() else null

            CategorySpending(
                categoryId = catId,
                categoryName = catName,
                colorHex = colorHex,
                iconKey = iconKey,
                totalAmount = catTotal,
                percentage = pct,
                transactionCount = items.size,
                budgetLimit = catBudget,
                budgetProgress = catProgress
            )
        }.sortedByDescending { it.totalAmount }

        // Last 6 months trend
        val monthlyTrends = mutableListOf<MonthlyTrend>()
        val trendCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -5) // Go back 5 months to include current month = 6 total
        }

        val monthLabelFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val yearMonthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        for (i in 0 until 6) {
            val tYear = trendCal.get(Calendar.YEAR)
            val tMonth = trendCal.get(Calendar.MONTH) + 1
            val (tStart, tEnd) = getMonthBounds(tYear, tMonth)
            val tExpenses = expenseDao.getExpensesByRangeSync(tStart, tEnd)
            val tTotal = tExpenses.sumOf { it.expense.amount }

            monthlyTrends.add(
                MonthlyTrend(
                    yearMonth = yearMonthKeyFormat.format(trendCal.time),
                    displayLabel = monthLabelFormat.format(trendCal.time),
                    totalAmount = tTotal
                )
            )
            trendCal.add(Calendar.MONTH, 1)
        }

        val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val displayCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthDisplayName = monthNameFormat.format(displayCal.time)

        val budgetProgress = if (effectiveBudget > 0) {
            (totalSpent / effectiveBudget).toFloat()
        } else {
            0f
        }

        return MonthSummary(
            year = year,
            month = month,
            monthDisplayName = monthDisplayName,
            totalSpent = totalSpent,
            previousMonthTotal = previousMonthTotal,
            percentageChange = percentageChange,
            dailyAverage = dailyAverage,
            highestSpendDayEpoch = maxSpendDayEpoch,
            highestSpendDayAmount = maxSpendDayAmount,
            categoryBreakdowns = categoryBreakdowns,
            dailySpendings = dailyList,
            monthlyTrends = monthlyTrends,
            monthlyBudget = effectiveBudget,
            budgetProgress = budgetProgress,
            isOverBudget = totalSpent > effectiveBudget,
            isMonthSpecificBudget = isMonthSpecific
        )
    }

    suspend fun setMonthlyBudget(budget: Double) {
        preferencesRepository.setMonthlyBudget(budget)
    }

    suspend fun setMonthBudget(year: Int, month: Int, budget: Double) {
        preferencesRepository.setMonthBudget(year, month, budget)
    }

    suspend fun setCategoryBudget(categoryId: String, budget: Double?) {
        preferencesRepository.setCategoryBudget(categoryId, budget)
    }

    suspend fun reSeedSampleData() {
        expenseDao.deleteAllExpenses()
        SampleDataSeeder.seedSampleExpenses(expenseDao)
        preferencesRepository.setSampleDataSeeded(true)
    }

    suspend fun clearAllData() {
        expenseDao.deleteAllExpenses()
        preferencesRepository.setSampleDataSeeded(true)
    }

    companion object {
        fun getMonthBounds(year: Int, month: Int): Pair<Long, Long> {
            val startCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return Pair(startCal.timeInMillis, endCal.timeInMillis)
        }

        fun getDaysInMonth(year: Int, month: Int): Int {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    }
}
