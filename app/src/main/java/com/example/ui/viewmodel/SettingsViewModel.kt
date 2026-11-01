package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupPreview
import com.example.data.backup.BackupRestoreManager
import com.example.data.backup.RestoreResult
import com.example.data.export.ExportFormat
import com.example.data.export.ExportScope
import com.example.data.export.ReportExporter
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.Calendar

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val backupRestoreManager: BackupRestoreManager,
    private val reportExporter: ReportExporter,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _pendingRestore = MutableStateFlow<Pair<JSONObject, BackupPreview>?>(null)
    val pendingRestore: StateFlow<Pair<JSONObject, BackupPreview>?> = _pendingRestore.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun setCurrency(symbol: String) {
        viewModelScope.launch {
            preferencesRepository.setCurrencySymbol(symbol)
            _eventFlow.emit(UiEvent.ShowSnackbar("Currency updated to $symbol"))
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setMonthlyBudget(budget: Double) {
        if (budget <= 0) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a budget greater than 0")) }
            return
        }
        viewModelScope.launch {
            preferencesRepository.setMonthlyBudget(budget)
            _eventFlow.emit(UiEvent.ShowSnackbar("Monthly budget updated"))
        }
    }

    fun setMonthBudget(year: Int, month: Int, budget: Double) {
        if (budget <= 0) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid budget")) }
            return
        }
        viewModelScope.launch {
            preferencesRepository.setMonthBudget(year, month, budget)
            _eventFlow.emit(UiEvent.ShowSnackbar("Month budget updated"))
        }
    }

    fun setCategoryBudget(categoryId: String, budget: Double?) {
        viewModelScope.launch {
            preferencesRepository.setCategoryBudget(categoryId, budget)
            _eventFlow.emit(UiEvent.ShowSnackbar(if (budget != null) "Category budget updated" else "Category limit removed"))
        }
    }

    fun setMonthlyLimitEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setMonthlyLimitEnabled(enabled)
            _eventFlow.emit(UiEvent.ShowSnackbar(if (enabled) "Monthly spending limit enabled" else "Monthly spending limit disabled"))
        }
    }

    fun isBackupOverdue(lastBackupTimestamp: Long): Boolean {
        if (lastBackupTimestamp <= 0L) return true
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
        return (System.currentTimeMillis() - lastBackupTimestamp) > thirtyDaysMillis
    }

    fun exportBackup(contentResolver: android.content.ContentResolver, uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                    ?: throw java.io.IOException("Unable to open selected file for writing")
                outputStream.use { stream ->
                    backupRestoreManager.writeBackupToStream(stream)
                }
                _eventFlow.emit(UiEvent.ShowSnackbar("Backup created successfully"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Backup failed: ${e.localizedMessage ?: "Unknown error"}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportBackup(outputStream: OutputStream) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isProcessing.value = true
            try {
                backupRestoreManager.writeBackupToStream(outputStream)
                _eventFlow.emit(UiEvent.ShowSnackbar("Backup created successfully"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Backup failed: ${e.localizedMessage ?: "Unknown error"}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun processRestoreFile(contentResolver: android.content.ContentResolver, uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw java.io.IOException("Unable to open selected backup file")
                val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val result = backupRestoreManager.parseAndValidateBackup(jsonString)
                result.fold(
                    onSuccess = { (root, preview) ->
                        _pendingRestore.value = Pair(root, preview)
                    },
                    onFailure = { error ->
                        _eventFlow.emit(UiEvent.ShowSnackbar(error.localizedMessage ?: "Corrupt backup file"))
                    }
                )
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Cannot read backup: ${e.localizedMessage ?: "Error"}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun processRestoreFile(inputStream: InputStream) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val result = backupRestoreManager.parseAndValidateBackup(inputStream)
                result.fold(
                    onSuccess = { (root, preview) ->
                        _pendingRestore.value = Pair(root, preview)
                    },
                    onFailure = { error ->
                        _eventFlow.emit(UiEvent.ShowSnackbar(error.localizedMessage ?: "Corrupt backup file"))
                    }
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun cancelRestorePreview() {
        _pendingRestore.value = null
    }

    fun executeRestore(replaceAll: Boolean) {
        val pending = _pendingRestore.value ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val result = backupRestoreManager.restoreData(pending.first, replaceAll)
                when (result) {
                    is RestoreResult.Success -> {
                        _pendingRestore.value = null
                        _eventFlow.emit(
                            UiEvent.ShowSnackbar(
                                "Restore successful: ${result.expensesRestored} expenses (${result.mode})"
                            )
                        )
                    }
                    is RestoreResult.Error -> {
                        _eventFlow.emit(UiEvent.ShowSnackbar("Restore failed: ${result.message}"))
                    }
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportReportToUri(
        contentResolver: android.content.ContentResolver,
        uri: Uri,
        scope: ExportScope,
        format: ExportFormat,
        customStartMillis: Long,
        customEndMillis: Long
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val now = Calendar.getInstance()
                val currentYear = now.get(Calendar.YEAR)
                val currentMonth = now.get(Calendar.MONTH) + 1
                val expenses = reportExporter.fetchExpensesForScope(
                    scope = scope,
                    year = currentYear,
                    month = currentMonth,
                    customStartMillis = customStartMillis,
                    customEndMillis = customEndMillis
                )

                if (expenses.isEmpty()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("No expenses found in selected range to export"))
                    return@launch
                }

                val outputStream = contentResolver.openOutputStream(uri)
                    ?: throw java.io.IOException("Unable to open target file for writing")
                outputStream.use { stream ->
                    reportExporter.exportToStream(
                        format = format,
                        expenses = expenses,
                        currencySymbol = userPreferences.value.currencySymbol,
                        outputStream = stream
                    )
                }
                _eventFlow.emit(UiEvent.ShowSnackbar("Report exported successfully"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Export failed: ${e.localizedMessage ?: "Error"}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportReportToStream(
        scope: ExportScope,
        format: ExportFormat,
        customStartMillis: Long,
        customEndMillis: Long,
        outputStream: OutputStream
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val now = Calendar.getInstance()
                val currentYear = now.get(Calendar.YEAR)
                val currentMonth = now.get(Calendar.MONTH) + 1
                val expenses = reportExporter.fetchExpensesForScope(
                    scope = scope,
                    year = currentYear,
                    month = currentMonth,
                    customStartMillis = customStartMillis,
                    customEndMillis = customEndMillis
                )

                if (expenses.isEmpty()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("No expenses found in selected range to export"))
                    return@launch
                }

                reportExporter.exportToStream(
                    format = format,
                    expenses = expenses,
                    currencySymbol = userPreferences.value.currencySymbol,
                    outputStream = outputStream
                )
                _eventFlow.emit(UiEvent.ShowSnackbar("Report exported successfully"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Export failed: ${e.localizedMessage ?: "Error"}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun createShareIntent(
        scope: ExportScope,
        format: ExportFormat,
        customStartMillis: Long,
        customEndMillis: Long,
        onIntentReady: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val now = Calendar.getInstance()
                val currentYear = now.get(Calendar.YEAR)
                val currentMonth = now.get(Calendar.MONTH) + 1
                val expenses = reportExporter.fetchExpensesForScope(
                    scope = scope,
                    year = currentYear,
                    month = currentMonth,
                    customStartMillis = customStartMillis,
                    customEndMillis = customEndMillis
                )

                if (expenses.isEmpty()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("No expenses found in selected range to share"))
                    return@launch
                }

                val intent = reportExporter.createShareIntent(
                    format = format,
                    expenses = expenses,
                    currencySymbol = userPreferences.value.currencySymbol,
                    scopeName = scope.displayName
                )
                onIntentReady(intent)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Failed to prepare share file: ${e.localizedMessage}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun reseedSampleData() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                expenseRepository.reSeedSampleData()
                _eventFlow.emit(UiEvent.ShowSnackbar("Sample expenses loaded"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    suspend fun countExpensesInRange(startMillis: Long, endMillis: Long): Int {
        return expenseRepository.countExpensesInRange(startMillis, endMillis)
    }

    fun clearExpensesInRange(startMillis: Long, endMillis: Long, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val deletedCount = expenseRepository.clearExpensesInRange(startMillis, endMillis)
                onComplete?.invoke(deletedCount)
                _eventFlow.emit(UiEvent.ShowSnackbar("Cleared $deletedCount expense${if (deletedCount == 1) "" else "s"} in selected range"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Failed to clear expenses: ${e.localizedMessage ?: "Error"}"))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                expenseRepository.clearAllData()
                _eventFlow.emit(UiEvent.ShowSnackbar("All expense data cleared"))
            } finally {
                _isProcessing.value = false
            }
        }
    }
}

class SettingsViewModelFactory(
    private val preferencesRepository: UserPreferencesRepository,
    private val backupRestoreManager: BackupRestoreManager,
    private val reportExporter: ReportExporter,
    private val expenseRepository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(
                preferencesRepository = preferencesRepository,
                backupRestoreManager = backupRestoreManager,
                reportExporter = reportExporter,
                expenseRepository = expenseRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
