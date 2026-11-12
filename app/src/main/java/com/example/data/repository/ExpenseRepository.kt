package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ExpenseSplitDao
import com.example.data.dao.FriendDao
import com.example.data.dao.FundDao
import com.example.data.model.CategoryEntity
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpending
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplitEntity
import com.example.data.model.ExpenseWithCategory
import com.example.data.model.FriendEntity
import com.example.data.model.FundEntity
import com.example.data.model.MonthSummary
import com.example.data.model.MonthlyTrend
import com.example.data.model.PaymentMethod
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.seeder.SampleDataSeeder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val expenseSplitDao: ExpenseSplitDao? = null,
    private val friendDao: FriendDao? = null,
    private val fundDao: FundDao? = null
) {

    val allExpensesWithCategory: Flow<List<ExpenseWithCategory>> =
        expenseDao.getAllExpensesWithCategory()

    val allCategories: Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    val userPreferences: Flow<UserPreferences> =
        preferencesRepository.userPreferencesFlow

    val allSplits: Flow<List<ExpenseSplitEntity>> =
        expenseSplitDao?.getAllSplits() ?: emptyFlow()

    val pendingSplits: Flow<List<ExpenseSplitEntity>> =
        expenseSplitDao?.getPendingSplits() ?: emptyFlow()

    val allFriends: Flow<List<FriendEntity>> =
        friendDao?.getAllFriends() ?: emptyFlow()

    val allFunds: Flow<List<FundEntity>> =
        fundDao?.getAllFunds() ?: emptyFlow()

    val totalFundsAmount: Flow<Double> =
        fundDao?.getTotalFundsAmount() ?: emptyFlow()

    fun getExpensesForDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ExpenseWithCategory>> =
        expenseDao.getExpensesByRange(startTimestamp, endTimestamp)

    suspend fun getExpensesForDateRangeSync(startTimestamp: Long, endTimestamp: Long): List<ExpenseWithCategory> =
        expenseDao.getExpensesByRangeSync(startTimestamp, endTimestamp)

    suspend fun getAllExpensesSync(): List<ExpenseWithCategory> =
        expenseDao.getAllExpensesWithCategorySync()

    suspend fun getAllCategoriesSync(): List<CategoryEntity> =
        categoryDao.getAllCategoriesSync()

    suspend fun insertExpense(expense: ExpenseEntity): Long =
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

    suspend fun saveExpenseWithSplits(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>) {
        expenseDao.insertExpense(expense)
        if (expenseSplitDao != null) {
            expenseSplitDao.deleteSplitsForExpense(expense.id)
            if (splits.isNotEmpty()) {
                val preparedSplits = splits.map { it.copy(expenseId = expense.id) }
                expenseSplitDao.insertSplits(preparedSplits)
            }
        }
    }

    suspend fun setSplitPaidStatus(splitId: String, isPaid: Boolean): ExpenseSplitEntity? {
        val settledTime = if (isPaid) System.currentTimeMillis() else null
        expenseSplitDao?.setPaidStatus(splitId, isPaid, settledTime)
        val split = expenseSplitDao?.getSplitById(splitId)
        if (split != null && fundDao != null) {
            val repaymentFundId = "repay_split_${split.id}"
            if (isPaid) {
                val fund = FundEntity(
                    id = repaymentFundId,
                    title = "Repayment from ${split.personName}",
                    amount = split.allocatedAmount,
                    source = "Friend Repayment",
                    timestamp = System.currentTimeMillis(),
                    notes = "Auto-added from settled friend debt"
                )
                fundDao.insertFund(fund)
            } else {
                fundDao.deleteFundById(repaymentFundId)
            }
        }
        return split
    }

    suspend fun lendMoneyToFriend(
        friendName: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        notes: String?,
        timestamp: Long = System.currentTimeMillis()
    ): Pair<ExpenseEntity, ExpenseSplitEntity> {
        val trimmedName = friendName.trim()
        if (friendDao != null) {
            val existing = friendDao.getFriendByName(trimmedName)
            if (existing == null) {
                friendDao.insertFriend(FriendEntity(name = trimmedName))
            }
        }

        val lentCategory = categoryDao.getCategoryById(SampleDataSeeder.CATEGORY_LENT_ID)
            ?: categoryDao.getCategoryByName("Money Lent")
            ?: run {
                val fallbackLent = CategoryEntity(
                    id = SampleDataSeeder.CATEGORY_LENT_ID,
                    name = "Money Lent",
                    iconKey = "money_lent",
                    colorHex = 0xFF0171DF,
                    isDefault = true,
                    sortOrder = 9
                )
                categoryDao.insertCategory(fallbackLent)
                fallbackLent
            }

        val expense = ExpenseEntity(
            title = "Lent to $trimmedName",
            amount = amount,
            categoryId = lentCategory.id,
            timestamp = timestamp,
            paymentMethod = paymentMethod.name,
            notes = notes?.trim()?.ifEmpty { null }
        )

        val split = ExpenseSplitEntity(
            expenseId = expense.id,
            personName = trimmedName,
            allocatedAmount = amount,
            percentage = 100.0,
            isPaid = false,
            settledTimestamp = null
        )

        saveExpenseWithSplits(expense, listOf(split))
        return Pair(expense, split)
    }

    suspend fun borrowMoneyFromFriend(
        friendName: String,
        amount: Double,
        notes: String?,
        timestamp: Long = System.currentTimeMillis()
    ): FundEntity {
        val trimmedName = friendName.trim()
        if (friendDao != null) {
            val existing = friendDao.getFriendByName(trimmedName)
            if (existing == null) {
                friendDao.insertFriend(FriendEntity(name = trimmedName))
            }
        }
        val fund = FundEntity(
            title = "Borrowed from $trimmedName",
            amount = amount,
            source = "Friend Loan",
            timestamp = timestamp,
            notes = notes?.trim()?.ifEmpty { null }
        )
        fundDao?.insertFund(fund)
        return fund
    }

    suspend fun getSplitsForExpense(expenseId: String): List<ExpenseSplitEntity> =
        expenseSplitDao?.getSplitsForExpenseSync(expenseId) ?: emptyList()

    /**
     * Requirement 3: Deleting a category moves its expenses to "Other"
     */
    suspend fun deleteCategory(category: CategoryEntity) {
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

        categoryDao.reassignExpenses(category.id, otherCategory.id)
        categoryDao.deleteCategory(category)
    }

    suspend fun calculateMonthSummary(
        year: Int,
        month: Int,
        monthlyBudget: Double
    ): MonthSummary {
        val (startCurrent, endCurrent) = getMonthBounds(year, month)
        val currentExpenses = expenseDao.getExpensesByRangeSync(startCurrent, endCurrent)

        val prevCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            add(Calendar.MONTH, -1)
        }
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH) + 1
        val (startPrev, endPrev) = getMonthBounds(prevYear, prevMonth)
        val prevExpenses = expenseDao.getExpensesByRangeSync(startPrev, endPrev)

        val totalSpent = currentExpenses.sumOf { it.expense.amount }
        val previousMonthTotal = prevExpenses.sumOf { it.expense.amount }

        val percentageChange = if (previousMonthTotal > 0) {
            ((totalSpent - previousMonthTotal) / previousMonthTotal) * 100.0
        } else if (totalSpent > 0) {
            100.0
        } else {
            0.0
        }

        val daysInMonth = getDaysInMonth(year, month)
        val dailyAverage = if (daysInMonth > 0) totalSpent / daysInMonth else 0.0

        val expensesByDay = currentExpenses.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.expense.timestamp }
            c.get(Calendar.DAY_OF_MONTH)
        }

        var maxSpendDayEpoch: Long? = null
        var maxSpendDayAmount = 0.0

        val dailyList = mutableListOf<DailySpending>()
        for (day in 1..daysInMonth) {
            val dayExpenses = expensesByDay[day] ?: emptyList()
            val dayTotal = dayExpenses.sumOf { it.expense.amount }

            val dayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 12)
            }

            val isWeekend = dayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || dayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            dailyList.add(
                DailySpending(
                    dayOfMonth = day,
                    dateEpochMillis = dayCal.timeInMillis,
                    totalAmount = dayTotal,
                    transactionCount = dayExpenses.size,
                    isWeekend = isWeekend
                )
            )

            if (dayTotal > maxSpendDayAmount) {
                maxSpendDayAmount = dayTotal
                maxSpendDayEpoch = dayCal.timeInMillis
            }
        }

        val allCategories = categoryDao.getAllCategoriesSync().associateBy { it.id }
        val expensesByCategory = currentExpenses.groupBy { it.expense.categoryId }
        val prefs = preferencesRepository.getPreferencesSync()
        val categoryBudgets = prefs.categoryBudgets

        val categoryBreakdowns = expensesByCategory.map { (catId, exps) ->
            val catTotal = exps.sumOf { it.expense.amount }
            val catEntity = allCategories[catId]
            val catPercentage = if (totalSpent > 0) ((catTotal / totalSpent) * 100.0).toFloat() else 0f
            val catLimit = categoryBudgets[catId]
            val catProgress = if (catLimit != null && catLimit > 0) (catTotal / catLimit).toFloat() else null
            CategorySpending(
                categoryId = catId,
                categoryName = catEntity?.name ?: "Other",
                colorHex = catEntity?.colorHex ?: 0xFF64748B,
                iconKey = catEntity?.iconKey ?: "other",
                totalAmount = catTotal,
                percentage = catPercentage,
                transactionCount = exps.size,
                budgetLimit = catLimit,
                budgetProgress = catProgress
            )
        }.sortedByDescending { it.totalAmount }

        val isLimitEnabled = prefs.isMonthlyLimitEnabled
        val monthBudgetKey = com.example.data.preferences.UserPreferencesRepository.formatYearMonthKey(year, month)
        val monthBudgets = prefs.monthBudgets
        val specificMonthBudget = monthBudgets[monthBudgetKey]

        // Fetch income/funds for the month (fallback to all funds if none in this month)
        val monthIncome = fundDao?.getFundsAmountInRange(startCurrent, endCurrent) ?: 0.0
        val allIncome = fundDao?.getTotalFundsAmountSync() ?: 0.0
        val totalIncome = if (monthIncome > 0) monthIncome else allIncome

        // When monthly limit is off, calculate the monthly budget according to total income
        val effectiveBudget = if (isLimitEnabled) {
            specificMonthBudget ?: monthlyBudget
        } else {
            totalIncome
        }
        val isMonthSpecific = isLimitEnabled && specificMonthBudget != null

        val monthlyTrends = mutableListOf<MonthlyTrend>()
        val trendCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -5)
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

        val isOverBudget = if (effectiveBudget > 0) {
            totalSpent > effectiveBudget
        } else {
            false
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
            isOverBudget = isOverBudget,
            isMonthSpecificBudget = isMonthSpecific,
            isMonthlyLimitEnabled = isLimitEnabled,
            totalIncome = totalIncome
        )
    }

    suspend fun setMonthlyBudget(budget: Double) {
        preferencesRepository.setMonthlyBudget(budget)
    }

    suspend fun setMonthBudget(year: Int, month: Int, budget: Double) {
        preferencesRepository.setMonthBudget(year, month, budget)
    }

    suspend fun setMonthlyLimitEnabled(enabled: Boolean) {
        preferencesRepository.setMonthlyLimitEnabled(enabled)
    }

    suspend fun setCategoryBudget(categoryId: String, budget: Double?) {
        preferencesRepository.setCategoryBudget(categoryId, budget)
    }

    suspend fun reSeedSampleData() {
        expenseSplitDao?.deleteAllSplits()
        expenseDao.deleteAllExpenses()
        SampleDataSeeder.seedSampleExpenses(expenseDao, expenseSplitDao)
        preferencesRepository.setSampleDataSeeded(true)
    }

    suspend fun clearAllData() {
        expenseSplitDao?.deleteAllSplits()
        expenseDao.deleteAllExpenses()
        fundDao?.deleteAllFunds()
        preferencesRepository.setMonthlyBudget(0.0)
        preferencesRepository.setSampleDataSeeded(true)
    }

    suspend fun clearExpensesInRange(startTime: Long, endTime: Long): Int {
        return expenseDao.deleteExpensesInRange(startTime, endTime)
    }

    suspend fun countExpensesInRange(startTime: Long, endTime: Long): Int {
        return expenseDao.countExpensesInRange(startTime, endTime)
    }

    suspend fun insertFriend(name: String): FriendEntity {
        val friend = FriendEntity(name = name.trim())
        friendDao?.insertFriend(friend)
        return friend
    }

    suspend fun deleteFriend(friend: FriendEntity) {
        friendDao?.deleteFriend(friend)
    }

    suspend fun deleteFriendById(id: String) {
        friendDao?.deleteFriendById(id)
    }

    suspend fun getAllFriendsSync(): List<FriendEntity> {
        return friendDao?.getAllFriendsSync() ?: emptyList()
    }

    suspend fun insertFund(
        title: String,
        amount: Double,
        source: String,
        timestamp: Long = System.currentTimeMillis(),
        notes: String? = null
    ): FundEntity {
        val fund = FundEntity(
            title = title.trim(),
            amount = amount,
            source = source.trim(),
            timestamp = timestamp,
            notes = notes?.trim()
        )
        fundDao?.insertFund(fund)
        return fund
    }

    suspend fun deleteFund(fund: FundEntity) {
        fundDao?.deleteFund(fund)
    }

    suspend fun deleteFundById(id: String) {
        fundDao?.deleteFundById(id)
    }

    suspend fun getAllFundsSync(): List<FundEntity> {
        return fundDao?.getAllFundsSync() ?: emptyList()
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
