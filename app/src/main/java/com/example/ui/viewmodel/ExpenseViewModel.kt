package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseWithCategory
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

data class HomeSummary(
    val todaySpent: Double,
    val monthSpent: Double,
    val monthlyBudget: Double,
    val budgetRemaining: Double,
    val budgetProgress: Float
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

    val homeSummary: StateFlow<HomeSummary> = allExpenses.combine(userPreferences) { list, prefs ->
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        val monthKey = com.example.data.preferences.UserPreferencesRepository.formatYearMonthKey(year, month)
        val effectiveBudget = prefs.monthBudgets[monthKey] ?: prefs.monthlyBudget
        calculateHomeSummary(list, effectiveBudget)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeSummary(0.0, 0.0, 25000.0, 25000.0, 0f)
    )

    fun setMonthlyBudget(budget: Double) {
        if (budget <= 0) {
            emitSnackbar("Please enter a valid budget")
            return
        }
        viewModelScope.launch {
            repository.setMonthlyBudget(budget)
            emitSnackbar("Monthly budget set to ${userPreferences.value.currencySymbol}${String.format(Locale.US, "%,.0f", budget)}")
        }
    }

    fun setMonthSpecificBudget(year: Int, month: Int, budget: Double) {
        if (budget <= 0) {
            emitSnackbar("Please enter a valid budget")
            return
        }
        viewModelScope.launch {
            repository.setMonthBudget(year, month, budget)
            val monthKey = com.example.data.preferences.UserPreferencesRepository.formatYearMonthKey(year, month)
            emitSnackbar("Budget for $monthKey set to ${userPreferences.value.currencySymbol}${String.format(Locale.US, "%,.0f", budget)}")
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
        notes: String?
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
            repository.insertExpense(expense)
            emitSnackbar("Expense added successfully")
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
        notes: String?
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
            repository.updateExpense(expense)
            emitSnackbar("Expense updated")
        }
        return true
    }

    fun deleteExpense(item: ExpenseWithCategory) {
        viewModelScope.launch {
            val expenseToDelete = item.expense
            recentlyDeletedExpense = expenseToDelete
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
        viewModelScope.launch {
            repository.insertExpense(expenseToRestore)
            recentlyDeletedExpense = null
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

            // Group by year-day key for proper chronological daily grouping
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

        fun calculateHomeSummary(list: List<ExpenseWithCategory>, budget: Double): HomeSummary {
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

            val remaining = (budget - monthTotal).coerceAtLeast(0.0)
            val progress = if (budget > 0) (monthTotal / budget).toFloat() else 0f

            return HomeSummary(
                todaySpent = todayTotal,
                monthSpent = monthTotal,
                monthlyBudget = budget,
                budgetRemaining = remaining,
                budgetProgress = progress
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
