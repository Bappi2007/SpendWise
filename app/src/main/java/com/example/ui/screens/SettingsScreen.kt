package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.backup.BackupPreview
import com.example.data.export.ExportFormat
import com.example.data.export.ExportScope
import com.example.data.preferences.UserPreferences
import com.example.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    categories: List<com.example.data.model.CategoryEntity> = emptyList(),
    pendingRestore: Pair<org.json.JSONObject, BackupPreview>?,
    isProcessing: Boolean,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isCurrencyDialogOpen by remember { mutableStateOf(false) }
    var isBudgetSheetVisible by remember { mutableStateOf(false) }
    var isExportDialogOpen by remember { mutableStateOf(false) }
    var isClearConfirmOpen by remember { mutableStateOf(false) }
    var isClearRangeDialogOpen by remember { mutableStateOf(false) }

    // SAF Launcher for Backup Export
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportBackup(context.contentResolver, it)
        }
    }

    // SAF Launcher for Backup Restore
    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.processRestoreFile(context.contentResolver, it)
        }
    }

    val isBackupOverdue = viewModel.isBackupOverdue(preferences.lastBackupTimestamp)
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Backup Reminder Card (if overdue)
        if (isBackupOverdue) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Backup Recommended",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (preferences.lastBackupTimestamp == 0L) "No backups have been created yet." else "Last backup was over 30 days ago.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                                createBackupLauncher.launch("spendwise_backup_$dateStr.json")
                            }
                        ) {
                            Text("Back Up", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Section: Preferences
        item {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Currency Setting
                    SettingsRow(
                        icon = Icons.Default.AttachMoney,
                        title = "Currency Symbol",
                        subtitle = "Current: ${preferences.currencySymbol}",
                        onClick = { isCurrencyDialogOpen = true }
                    )

                    // Monthly Budget Setting
                    SettingsRow(
                        icon = Icons.Default.CalendarMonth,
                        title = "Monthly Budget Goal",
                        subtitle = if (preferences.isMonthlyLimitEnabled)
                            "${preferences.currencySymbol}${String.format(Locale.US, "%,.0f", preferences.monthlyBudget)} per month (Tap to tune)"
                        else
                            "Limit is currently OFF (Tap to configure)",
                        onClick = { isBudgetSheetVisible = true }
                    )

                    // Monthly Limit ON/OFF Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Monthly Spending Limit",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (preferences.isMonthlyLimitEnabled) "Limit is active (${preferences.currencySymbol}${String.format(Locale.US, "%,.0f", preferences.monthlyBudget)})"
                                    else "Turned off (Track spending freely by available funds)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = preferences.isMonthlyLimitEnabled,
                            onCheckedChange = { checked ->
                                viewModel.setMonthlyLimitEnabled(checked)
                            },
                            modifier = Modifier.testTag("settings_monthly_limit_switch")
                        )
                    }

                    // Theme Mode Setting
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Theme Mode",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                                val isSelected = preferences.themeMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Backup & Restore
        item {
            Text(
                text = "Backup & Restore (SAF)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Local Device Backups",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100% offline JSON export via Android Storage Access Framework. You choose where to store it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Last Backup: " + if (preferences.lastBackupTimestamp > 0) dateFormat.format(Date(preferences.lastBackupTimestamp)) else "Never",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isBackupOverdue) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                createBackupLauncher.launch("spendwise_backup_$dateStr.json")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_button")
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back Up")
                        }

                        OutlinedButton(
                            onClick = {
                                openBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_button")
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }
            }
        }

        // Section: Export Reports
        item {
            Text(
                text = "Export Financial Reports",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CSV & Excel (.xlsx) Exporter",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Export multi-sheet Excel workbooks or UTF-8 BOM CSV files",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { isExportDialogOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_export_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure & Export Report")
                    }
                }
            }
        }

        // Section: Data Maintenance
        item {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.CalendarMonth,
                        title = "Clear Expenses in Date Range",
                        subtitle = "Select start and end dates from calendar to delete",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { isClearRangeDialogOpen = true }
                    )

                    SettingsRow(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear All Expenses",
                        subtitle = "Wipe all expense entries (categories remain)",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { isClearConfirmOpen = true }
                    )
                }
            }
        }

        // App Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SpendWise v1.0",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Offline-first Jetpack Compose Room Database",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Developed by Bishal Mandal",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Currency Picker Dialog
    if (isCurrencyDialogOpen) {
        CurrencyPickerDialog(
            currentSymbol = preferences.currencySymbol,
            onDismiss = { isCurrencyDialogOpen = false },
            onSelect = {
                viewModel.setCurrency(it)
                isCurrencyDialogOpen = false
            }
        )
    }

    // Budget Manipulation Sheet
    if (isBudgetSheetVisible) {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1

        com.example.ui.components.BudgetManipulationSheet(
            currentBudget = preferences.monthlyBudget,
            monthSpent = 0.0,
            currencySymbol = preferences.currencySymbol,
            year = currentYear,
            month = currentMonth,
            categories = categories,
            categorySpendings = emptyList(),
            categoryBudgets = preferences.categoryBudgets,
            isMonthlyLimitEnabled = preferences.isMonthlyLimitEnabled,
            onToggleMonthlyLimit = { enabled ->
                viewModel.setMonthlyLimitEnabled(enabled)
            },
            onDismiss = { isBudgetSheetVisible = false },
            onSaveOverallBudget = { newBudget, applyToMonthOnly ->
                if (applyToMonthOnly) {
                    viewModel.setMonthBudget(currentYear, currentMonth, newBudget)
                } else {
                    viewModel.setMonthlyBudget(newBudget)
                }
            },
            onSaveCategoryBudget = { catId, limit ->
                viewModel.setCategoryBudget(catId, limit)
            }
        )
    }

    // Export Reports Dialog
    if (isExportDialogOpen) {
        ExportReportDialog(
            currencySymbol = preferences.currencySymbol,
            onDismiss = { isExportDialogOpen = false },
            viewModel = viewModel
        )
    }

    // Restore Preview Dialog
    pendingRestore?.let { (root, preview) ->
        RestorePreviewDialog(
            preview = preview,
            onDismiss = { viewModel.cancelRestorePreview() },
            onConfirm = { replaceAll ->
                viewModel.executeRestore(replaceAll)
            }
        )
    }

    // Clear All Confirmation Dialog
    if (isClearConfirmOpen) {
        AlertDialog(
            onDismissRequest = { isClearConfirmOpen = false },
            icon = { Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All Expense Data?") },
            text = { Text("This will delete all expense transactions from the local database. Your custom categories will be preserved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        isClearConfirmOpen = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isClearConfirmOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Expenses in Date Range Dialog
    if (isClearRangeDialogOpen) {
        ClearDateRangeDialog(
            onDismiss = { isClearRangeDialogOpen = false },
            onConfirmClear = { startMillis, endMillis ->
                viewModel.clearExpensesInRange(startMillis, endMillis)
            }
        )
    }

    // Loading overlay if processing
    if (isProcessing) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Processing...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (titleColor != MaterialTheme.colorScheme.onSurface) titleColor else MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CurrencyPickerDialog(
    currentSymbol: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val presets = listOf("₹" to "Indian Rupee (₹)", "$" to "US Dollar ($)", "€" to "Euro (€)", "£" to "British Pound (£)", "¥" to "Japanese Yen (¥)", "C$" to "Canadian Dollar (C$)", "A$" to "Australian Dollar (A$)")
    var customSymbol by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency Symbol") },
        text = {
            Column {
                presets.forEach { (sym, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(sym) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSymbol == sym,
                            onClick = { onSelect(sym) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customSymbol,
                    onValueChange = { customSymbol = it },
                    label = { Text("Or custom symbol") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (customSymbol.isNotBlank()) {
                TextButton(onClick = { onSelect(customSymbol.trim()) }) {
                    Text("Apply Custom")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BudgetDialog(
    currentBudget: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetText by remember { mutableStateOf(currentBudget.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly Budget Goal") },
        text = {
            Column {
                Text(
                    text = "SpendWise will monitor your progress and warn you if spending exceeds this limit in the monthly analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("Monthly Target") },
                    prefix = { Text("$currencySymbol ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = budgetText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onSave(amount)
                    }
                }
            ) {
                Text("Save Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RestorePreviewDialog(
    preview: BackupPreview,
    onDismiss: () -> Unit,
    onConfirm: (replaceAll: Boolean) -> Unit
) {
    var selectedModeIsReplace by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Restore Backup Preview") },
        text = {
            Column {
                Text(
                    text = "Backup file validated successfully (v${preview.schemaVersion}).",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "• Transactions: ${preview.expensesCount}", style = MaterialTheme.typography.bodySmall)
                        if (preview.splitsCount > 0) {
                            Text(text = "• Split Allocations: ${preview.splitsCount}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(text = "• Categories: ${preview.categoriesCount}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "• Total Value: ${preview.currencySymbol}${String.format(Locale.US, "%,.2f", preview.totalExpenseAmount)}", style = MaterialTheme.typography.bodySmall)
                        if (preview.earliestExpenseDateEpoch != null && preview.latestExpenseDateEpoch != null) {
                            Text(
                                text = "• Date Span: ${dateFormat.format(Date(preview.earliestExpenseDateEpoch))} - ${dateFormat.format(Date(preview.latestExpenseDateEpoch))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Choose Restore Mode:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedModeIsReplace = false }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = !selectedModeIsReplace, onClick = { selectedModeIsReplace = false })
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = "Merge with existing data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "Keep current records; skip duplicate IDs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedModeIsReplace = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedModeIsReplace, onClick = { selectedModeIsReplace = true })
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = "Replace all data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        Text(text = "Wipe local database and restore backup", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedModeIsReplace) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedModeIsReplace) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (selectedModeIsReplace) "Replace All" else "Merge Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExportReportDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    var selectedScope by remember { mutableStateOf(ExportScope.CURRENT_MONTH) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.EXCEL) }

    val cal = Calendar.getInstance()
    var customStartMillis by remember {
        val c = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        mutableLongStateOf(c.timeInMillis)
    }
    var customEndMillis by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // SAF Document Launcher for Report
    val createReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedFormat.mimeType)
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportReportToUri(
                contentResolver = context.contentResolver,
                uri = it,
                scope = selectedScope,
                format = selectedFormat,
                customStartMillis = customStartMillis,
                customEndMillis = customEndMillis
            )
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Export Expense Report",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scope Selection
                Text(text = "Export Scope", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ExportScope.entries.forEach { scope ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedScope = scope }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedScope == scope, onClick = { selectedScope = scope })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = scope.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Custom Range Pickers
                if (selectedScope == ExportScope.CUSTOM_RANGE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = customStartMillis }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val newC = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                        customStartMillis = newC.timeInMillis
                                    },
                                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("From: ${dateFormat.format(Date(customStartMillis))}", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = customEndMillis }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val newC = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                                        customEndMillis = newC.timeInMillis
                                    },
                                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("To: ${dateFormat.format(Date(customEndMillis))}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Format Selection
                Text(text = "File Format", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ExportFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedFormat == format, onClick = { selectedFormat = format })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = format.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Save via SAF or Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.createShareIntent(
                                scope = selectedScope,
                                format = selectedFormat,
                                customStartMillis = customStartMillis,
                                customEndMillis = customEndMillis
                            ) { intent ->
                                context.startActivity(Intent.createChooser(intent, "Share Expense Report"))
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = {
                            val timeStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                            createReportLauncher.launch("spendwise_report_$timeStr.${selectedFormat.extension}")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save File")
                    }
                }
            }
        }
    }
}

@Composable
fun ClearDateRangeDialog(
    onDismiss: () -> Unit,
    onConfirmClear: (startMillis: Long, endMillis: Long) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var startMillis by remember {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var endMillis by remember {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Clear Expenses by Date Range") },
        text = {
            Column {
                Text(
                    text = "Select calendar dates to delete recorded expenses:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance().apply { timeInMillis = startMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newC = Calendar.getInstance().apply {
                                        set(y, m, d, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    startMillis = newC.timeInMillis
                                },
                                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("From Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(dateFormat.format(Date(startMillis)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance().apply { timeInMillis = endMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newC = Calendar.getInstance().apply {
                                        set(y, m, d, 23, 59, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }
                                    endMillis = newC.timeInMillis
                                },
                                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("To Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(dateFormat.format(Date(endMillis)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Expenses within these dates will be permanently removed. Custom categories are preserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmClear(startMillis, endMillis)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Expenses")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

