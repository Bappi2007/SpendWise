package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplitEntity
import com.example.data.model.ExpenseWithCategory
import com.example.data.model.FriendEntity
import com.example.data.model.FundEntity
import com.example.data.model.PaymentMethod
import com.example.data.preferences.UserPreferences
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyGroupedExpenses(
    val dateLabel: String,
    val dateEpochMillis: Long,
    val dayTotal: Double,
    val expenses: List<ExpenseWithCategory>
)

sealed class DashboardTransactionItem {
    abstract val id: String
    abstract val timestamp: Long
    abstract val amount: Double
    abstract val title: String

    data class Expense(val expenseWithCategory: ExpenseWithCategory) : DashboardTransactionItem() {
        override val id: String get() = expenseWithCategory.expense.id
        override val timestamp: Long get() = expenseWithCategory.expense.timestamp
        override val amount: Double get() = expenseWithCategory.expense.amount
        override val title: String get() = expenseWithCategory.expense.title
    }

    data class Fund(val fund: FundEntity) : DashboardTransactionItem() {
        override val id: String get() = fund.id
        override val timestamp: Long get() = fund.timestamp
        override val amount: Double get() = fund.amount
        override val title: String get() = fund.title
    }
}

data class DailyGroupedTransactions(
    val dateLabel: String,
    val dateEpochMillis: Long,
    val dayExpenseTotal: Double,
    val dayFundTotal: Double,
    val items: List<DashboardTransactionItem>
)

data class HomeSummary(
    val todaySpent: Double,
    val monthSpent: Double,
    val monthlyBudget: Double,
    val budgetRemaining: Double,
    val budgetProgress: Float,
    val totalFundsAdded: Double = 0.0,
    val availableBalance: Double = 0.0,
    val hasCustomFunds: Boolean = false,
    val isMonthlyLimitEnabled: Boolean = false
)

data class SplitDraftItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val personName: String,
    val allocatedAmount: Double,
    val percentage: Double = 0.0,
    val isPaid: Boolean = false
)

data class PersonSettlementSummary(
    val personName: String,
    val pendingAmount: Double,
    val pendingCount: Int,
    val settledCount: Int,
    val splits: List<ExpenseSplitEntity>
)

data class FriendSplitStat(
    val friendName: String,
    val totalAllocated: Double,
    val totalPaid: Double,
    val totalPending: Double,
    val percentageOfTotalSplits: Float
)

data class SettlementOverview(
    val totalPendingAmount: Double,
    val totalPendingPeopleCount: Int,
    val peopleSummaries: List<PersonSettlementSummary>,
    val totalSplitVolume: Double = 0.0,
    val totalSettledAmount: Double = 0.0,
    val friendStats: List<FriendSplitStat> = emptyList()
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null) : UiEvent()
}

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var recentlyDeletedExpense: ExpenseEntity? = null
    private var recentlyDeletedSplits: List<ExpenseSplitEntity> = emptyList()

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userPreferences: StateFlow<UserPreferences> = repository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val allExpenses: StateFlow<List<ExpenseWithCategory>> = repository.allExpensesWithCategory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groupedExpenses: StateFlow<List<DailyGroupedExpenses>> = allExpenses.combine(userPreferences) { list, _ ->
        groupExpensesByDay(list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allFriends: StateFlow<List<FriendEntity>> = repository.allFriends
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allFunds: StateFlow<List<FundEntity>> = repository.allFunds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groupedTransactions: StateFlow<List<DailyGroupedTransactions>> = combine(allExpenses, allFunds) { expenses, funds ->
        groupTransactionsByDay(expenses, funds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val homeSummary: StateFlow<HomeSummary> = combine(allExpenses, userPreferences, repository.allFunds) { list, prefs, funds ->
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        val monthKey = com.example.data.preferences.UserPreferencesRepository.formatYearMonthKey(year, month)
        val effectiveBudget = prefs.monthBudgets[monthKey] ?: prefs.monthlyBudget
        calculateHomeSummary(list, effectiveBudget, funds, prefs.isMonthlyLimitEnabled)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeSummary(0.0, 0.0, 0.0, 0.0, 0f, 0.0, 0.0, false, false)
    )

    val allSplits: StateFlow<List<ExpenseSplitEntity>> = repository.allSplits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settlementOverview: StateFlow<SettlementOverview> = combine(repository.allSplits, repository.allFriends) { splits, friends ->
        calculateSettlementOverview(splits, friends)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettlementOverview(0.0, 0, emptyList())
    )

    fun addFund(title: String, amountText: String, source: String, notes: String? = null) {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            emitSnackbar("Please enter a valid amount")
            return
        }
        val displayTitle = if (title.isBlank()) source else title
        viewModelScope.launch {
            repository.insertFund(displayTitle, amount, source, notes = notes)
            emitSnackbar("Added $amount to funds ($source)")
        }
    }

    fun deleteFund(fund: FundEntity) {
        viewModelScope.launch {
            repository.deleteFund(fund)
            emitSnackbar("Fund entry removed")
        }
    }

    fun addFriend(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            emitSnackbar("Please enter friend's name")
            return
        }
        viewModelScope.launch {
            repository.insertFriend(trimmed)
            emitSnackbar("Added '$trimmed' to friends")
        }
    }

    fun removeFriend(friend: FriendEntity) {
        viewModelScope.launch {
            repository.deleteFriend(friend)
            emitSnackbar("Removed '${friend.name}'")
        }
    }

    fun removeFriendById(id: String) {
        viewModelScope.launch {
            repository.deleteFriendById(id)
            emitSnackbar("Friend removed")
        }
    }

    fun setMonthlyBudget(budget: Double) {
        if (budget <= 0) {
            emitSnackbar("Please enter a valid budget")
            return
        }
        viewModelScope.launch {
            repository.setMonthlyBudget(budget)
            emitSnackbar("Monthly budget updated")
        }
    }

    fun setMonthBudget(year: Int, month: Int, budget: Double) {
        if (budget <= 0) {
            emitSnackbar("Please enter a valid budget")
            return
        }
        viewModelScope.launch {
            repository.setMonthBudget(year, month, budget)
            emitSnackbar("Month budget updated")
        }
    }

    fun setMonthlyLimitEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMonthlyLimitEnabled(enabled)
            emitSnackbar(if (enabled) "Monthly spending limit enabled" else "Monthly spending limit disabled")
        }
    }

    fun setCategoryBudget(categoryId: String, budget: Double?) {
        viewModelScope.launch {
            repository.setCategoryBudget(categoryId, budget)
            emitSnackbar(if (budget != null) "Category budget updated" else "Category budget limit removed")
        }
    }

    fun addExpense(
        title: String,
        amountText: String,
        categoryId: String,
        timestamp: Long,
        paymentMethod: PaymentMethod,
        notes: String?,
        splits: List<SplitDraftItem> = emptyList()
    ): Boolean {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            emitSnackbar("Please enter a valid amount greater than 0")
            return false
        }
        if (title.isBlank()) {
            emitSnackbar("Please enter a title for the expense")
            return false
        }
        if (categoryId.isBlank()) {
            emitSnackbar("Please select a category")
            return false
        }

        viewModelScope.launch {
            val expense = ExpenseEntity(
                title = title.trim(),
                amount = amount,
                categoryId = categoryId,
                timestamp = timestamp,
                paymentMethod = paymentMethod.name,
                notes = notes?.trim()?.ifEmpty { null }
            )

            val splitEntities = splits.map {
                ExpenseSplitEntity(
                    id = it.id,
                    expenseId = expense.id,
                    personName = it.personName.trim(),
                    allocatedAmount = it.allocatedAmount,
                    percentage = it.percentage,
                    isPaid = it.isPaid,
                    settledTimestamp = if (it.isPaid) System.currentTimeMillis() else null
                )
            }

            repository.saveExpenseWithSplits(expense, splitEntities)
            emitSnackbar(if (splitEntities.isNotEmpty()) "Split expense added with ${splitEntities.size} people" else "Expense added successfully")
        }
        return true
    }

    fun updateExpense(
        id: String,
        title: String,
        amountText: String,
        categoryId: String,
        timestamp: Long,
        paymentMethod: PaymentMethod,
        notes: String?,
        splits: List<SplitDraftItem> = emptyList()
    ): Boolean {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            emitSnackbar("Please enter a valid amount")
            return false
        }
        if (title.isBlank()) {
            emitSnackbar("Please enter a title")
            return false
        }

        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = id,
                title = title.trim(),
                amount = amount,
                categoryId = categoryId,
                timestamp = timestamp,
                paymentMethod = paymentMethod.name,
                notes = notes?.trim()?.ifEmpty { null }
            )

            val splitEntities = splits.map {
                ExpenseSplitEntity(
                    id = it.id,
                    expenseId = expense.id,
                    personName = it.personName.trim(),
                    allocatedAmount = it.allocatedAmount,
                    percentage = it.percentage,
                    isPaid = it.isPaid,
                    settledTimestamp = if (it.isPaid) System.currentTimeMillis() else null
                )
            }

            repository.saveExpenseWithSplits(expense, splitEntities)
            emitSnackbar("Expense updated")
        }
        return true
    }

    fun toggleSplitPaidStatus(splitId: String, isPaid: Boolean) {
        viewModelScope.launch {
            repository.setSplitPaidStatus(splitId, isPaid)
            emitSnackbar(if (isPaid) "Marked share as paid" else "Marked share as unpaid")
        }
    }

    fun deleteExpense(item: ExpenseWithCategory) {
        viewModelScope.launch {
            val expenseToDelete = item.expense
            recentlyDeletedExpense = expenseToDelete
            recentlyDeletedSplits = item.splits
            repository.deleteExpense(expenseToDelete)

            _eventFlow.emit(
                UiEvent.ShowSnackbar(
                    message = "Deleted \"${expenseToDelete.title}\"",
                    actionLabel = "UNDO",
                    onAction = {
                        undoDelete()
                    }
                )
            )
        }
    }

    fun undoDelete() {
        val expenseToRestore = recentlyDeletedExpense ?: return
        val splitsToRestore = recentlyDeletedSplits
        viewModelScope.launch {
            repository.saveExpenseWithSplits(expenseToRestore, splitsToRestore)
            recentlyDeletedExpense = null
            recentlyDeletedSplits = emptyList()
            _eventFlow.emit(UiEvent.ShowSnackbar("Expense restored"))
        }
    }

    fun addCategory(name: String, iconKey: String, colorHex: Long): Boolean {
        if (name.isBlank()) {
            emitSnackbar("Category name cannot be empty")
            return false
        }
        viewModelScope.launch {
            val category = CategoryEntity(
                name = name.trim(),
                iconKey = iconKey,
                colorHex = colorHex,
                isDefault = false,
                sortOrder = (categories.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            )
            repository.insertCategory(category)
            emitSnackbar("Category \"${category.name}\" created")
        }
        return true
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
            emitSnackbar("Category updated")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            emitSnackbar("Category removed. Associated expenses moved to 'Other'")
        }
    }

    private fun emitSnackbar(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar(message))
        }
    }

    companion object {
        fun groupExpensesByDay(list: List<ExpenseWithCategory>): List<DailyGroupedExpenses> {
            val calendar = Calendar.getInstance()
            val todayCal = Calendar.getInstance()
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

            val fullDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())

            return list.groupBy { item ->
                calendar.timeInMillis = item.expense.timestamp
                val year = calendar.get(Calendar.YEAR)
                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                year * 1000 + dayOfYear
            }.map { (_, dayItems) ->
                val firstTime = dayItems.first().expense.timestamp
                calendar.timeInMillis = firstTime

                val isToday = calendar.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                val isYesterday = calendar.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

                val label = when {
                    isToday -> "Today"
                    isYesterday -> "Yesterday"
                    else -> fullDateFormat.format(Date(firstTime))
                }

                DailyGroupedExpenses(
                    dateLabel = label,
                    dateEpochMillis = firstTime,
                    dayTotal = dayItems.sumOf { it.expense.amount },
                    expenses = dayItems
                )
            }.sortedByDescending { it.dateEpochMillis }
        }

        fun calculateHomeSummary(
            list: List<ExpenseWithCategory>,
            budget: Double,
            funds: List<FundEntity> = emptyList(),
            isMonthlyLimitEnabled: Boolean = false
        ): HomeSummary {
            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH)
            val currentDay = now.get(Calendar.DAY_OF_YEAR)

            val itemCal = Calendar.getInstance()
            var todayTotal = 0.0
            var monthTotal = 0.0

            for (item in list) {
                itemCal.timeInMillis = item.expense.timestamp
                val iYear = itemCal.get(Calendar.YEAR)
                val iMonth = itemCal.get(Calendar.MONTH)
                val iDay = itemCal.get(Calendar.DAY_OF_YEAR)

                if (iYear == currentYear && iMonth == currentMonth) {
                    monthTotal += item.expense.amount
                    if (iDay == currentDay) {
                        todayTotal += item.expense.amount
                    }
                }
            }

            val totalFundsAdded = funds.sumOf { it.amount }
            val hasCustomFunds = funds.isNotEmpty()

            return if (hasCustomFunds) {
                val availableBalance = totalFundsAdded - monthTotal
                val progress = if (budget > 0) (monthTotal / budget).toFloat() else 0f
                HomeSummary(
                    todaySpent = todayTotal,
                    monthSpent = monthTotal,
                    monthlyBudget = budget,
                    budgetRemaining = (budget - monthTotal).coerceAtLeast(0.0),
                    budgetProgress = progress,
                    totalFundsAdded = totalFundsAdded,
                    availableBalance = availableBalance,
                    hasCustomFunds = true,
                    isMonthlyLimitEnabled = isMonthlyLimitEnabled
                )
            } else {
                val remaining = if (budget > 0) (budget - monthTotal).coerceAtLeast(0.0) else 0.0
                val progress = if (budget > 0) (monthTotal / budget).toFloat() else 0f
                val availableBalance = if (budget > 0 && isMonthlyLimitEnabled) {
                    budget - monthTotal
                } else {
                    0.0 - monthTotal
                }
                HomeSummary(
                    todaySpent = todayTotal,
                    monthSpent = monthTotal,
                    monthlyBudget = budget,
                    budgetRemaining = remaining,
                    budgetProgress = progress,
                    totalFundsAdded = 0.0,
                    availableBalance = availableBalance,
                    hasCustomFunds = false,
                    isMonthlyLimitEnabled = isMonthlyLimitEnabled
                )
            }
        }

        fun groupTransactionsByDay(
            expenses: List<ExpenseWithCategory>,
            funds: List<FundEntity>
        ): List<DailyGroupedTransactions> {
            val calendar = Calendar.getInstance()
            val todayCal = Calendar.getInstance()
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

            val fullDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())

            val allItems: MutableList<DashboardTransactionItem> = ArrayList(expenses.size + funds.size)
            for (e in expenses) {
                allItems.add(DashboardTransactionItem.Expense(e))
            }
            for (f in funds) {
                allItems.add(DashboardTransactionItem.Fund(f))
            }

            return allItems.groupBy { item ->
                calendar.timeInMillis = item.timestamp
                val year = calendar.get(Calendar.YEAR)
                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                year * 1000 + dayOfYear
            }.map { (_, dayItems) ->
                val sortedItems = dayItems.sortedByDescending { it.timestamp }
                val firstTime = sortedItems.first().timestamp
                calendar.timeInMillis = firstTime

                val isToday = calendar.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                val isYesterday = calendar.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

                val label = when {
                    isToday -> "Today"
                    isYesterday -> "Yesterday"
                    else -> fullDateFormat.format(Date(firstTime))
                }

                var expenseSum = 0.0
                var fundSum = 0.0
                for (item in sortedItems) {
                    when (item) {
                        is DashboardTransactionItem.Expense -> expenseSum += item.amount
                        is DashboardTransactionItem.Fund -> fundSum += item.amount
                    }
                }

                DailyGroupedTransactions(
                    dateLabel = label,
                    dateEpochMillis = firstTime,
                    dayExpenseTotal = expenseSum,
                    dayFundTotal = fundSum,
                    items = sortedItems
                )
            }.sortedByDescending { it.dateEpochMillis }
        }

        fun calculateSettlementOverview(
            splits: List<ExpenseSplitEntity>,
            friends: List<FriendEntity> = emptyList()
        ): SettlementOverview {
            val pendingSplits = splits.filter { !it.isPaid }
            val totalPending = pendingSplits.sumOf { it.allocatedAmount }
            val totalSplitVolume = splits.sumOf { it.allocatedAmount }
            val totalSettled = splits.filter { it.isPaid }.sumOf { it.allocatedAmount }

            val groupedByPerson = splits.groupBy { it.personName.trim().lowercase() }
            val peopleSummaries = groupedByPerson.map { (_, personSplits) ->
                val displayName = personSplits.first().personName.trim()
                val pendingForPerson = personSplits.filter { !it.isPaid }
                val settledForPerson = personSplits.filter { it.isPaid }
                PersonSettlementSummary(
                    personName = displayName,
                    pendingAmount = pendingForPerson.sumOf { it.allocatedAmount },
                    pendingCount = pendingForPerson.size,
                    settledCount = settledForPerson.size,
                    splits = personSplits
                )
            }.sortedByDescending { it.pendingAmount }

            val totalPendingPeople = peopleSummaries.count { it.pendingAmount > 0 }

            val friendNamesSet = (peopleSummaries.map { it.personName.lowercase() } + friends.map { it.name.trim().lowercase() }).distinct()
            val friendStats = friendNamesSet.map { lowerName ->
                val summary = peopleSummaries.firstOrNull { it.personName.lowercase() == lowerName }
                val savedFriend = friends.firstOrNull { it.name.trim().lowercase() == lowerName }
                val friendName = summary?.personName ?: savedFriend?.name ?: lowerName.replaceFirstChar { it.uppercase() }
                val allocated = summary?.splits?.sumOf { it.allocatedAmount } ?: 0.0
                val paid = summary?.splits?.filter { it.isPaid }?.sumOf { it.allocatedAmount } ?: 0.0
                val pending = summary?.pendingAmount ?: 0.0
                val percentage = if (totalSplitVolume > 0) ((allocated / totalSplitVolume) * 100).toFloat() else 0f
                FriendSplitStat(
                    friendName = friendName,
                    totalAllocated = allocated,
                    totalPaid = paid,
                    totalPending = pending,
                    percentageOfTotalSplits = percentage
                )
            }.sortedByDescending { it.totalAllocated }

            return SettlementOverview(
                totalPendingAmount = totalPending,
                totalPendingPeopleCount = totalPendingPeople,
                peopleSummaries = peopleSummaries,
                totalSplitVolume = totalSplitVolume,
                totalSettledAmount = totalSettled,
                friendStats = friendStats
            )
        }
    }
}

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
