package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.FundEntity
import com.example.data.model.MonthSummary
import com.example.data.preferences.UserPreferences
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class AnalysisViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val currentCal = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(currentCal.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(currentCal.get(Calendar.MONTH) + 1) // 1-based
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> = repository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val allFunds: StateFlow<List<FundEntity>> = repository.allFunds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _monthSummary = MutableStateFlow<MonthSummary?>(null)
    val monthSummary: StateFlow<MonthSummary?> = _monthSummary.asStateFlow()

    init {
        // Reactively recompute summary when year, month, expenses, funds (income), or preferences update
        viewModelScope.launch {
            combine(
                _selectedYear,
                _selectedMonth,
                repository.allExpensesWithCategory,
                repository.allFunds,
                userPreferences
            ) { year, month, _, _, prefs ->
                Triple(year, month, prefs.monthlyBudget)
            }.collect { (year, month, budget) ->
                refreshSummary(year, month, budget)
            }
        }
    }

    private suspend fun refreshSummary(year: Int, month: Int, budget: Double) {
        val summary = repository.calculateMonthSummary(year, month, budget)
        _monthSummary.value = summary
    }

    fun goToPreviousMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value - 1) // 0-based
            add(Calendar.MONTH, -1)
        }
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH) + 1
    }

    fun goToNextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value - 1)
            add(Calendar.MONTH, 1)
        }
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH) + 1
    }

    fun resetToCurrentMonth() {
        val cal = Calendar.getInstance()
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH) + 1
    }

    fun setMonthlyBudget(budget: Double) {
        viewModelScope.launch {
            repository.setMonthlyBudget(budget)
        }
    }

    fun setMonthBudget(year: Int, month: Int, budget: Double) {
        viewModelScope.launch {
            repository.setMonthBudget(year, month, budget)
        }
    }

    fun setCategoryBudget(categoryId: String, budget: Double?) {
        viewModelScope.launch {
            repository.setCategoryBudget(categoryId, budget)
        }
    }

    fun setMonthlyLimitEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMonthlyLimitEnabled(enabled)
        }
    }

    fun addFund(title: String, amountText: String, source: String, notes: String? = null) {
        val amount = amountText.toDoubleOrNull() ?: return
        if (amount <= 0) return
        val displayTitle = if (title.isBlank()) source else title
        viewModelScope.launch {
            repository.insertFund(displayTitle, amount, source, notes = notes)
        }
    }

    fun deleteFund(fund: FundEntity) {
        viewModelScope.launch {
            repository.deleteFund(fund)
        }
    }
}

class AnalysisViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            return AnalysisViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
