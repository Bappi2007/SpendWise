package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Add
import com.example.data.model.CategoryEntity
import com.example.data.model.CategorySpending
import com.example.data.model.FundEntity
import com.example.ui.common.CategoryIconBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Mint400
import com.example.ui.theme.RedDanger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetManipulationSheet(
    currentBudget: Double,
    monthSpent: Double,
    currencySymbol: String,
    year: Int,
    month: Int,
    categories: List<CategoryEntity>,
    categorySpendings: List<CategorySpending>,
    categoryBudgets: Map<String, Double>,
    funds: List<FundEntity> = emptyList(),
    isMonthlyLimitEnabled: Boolean = false,
    onAddFund: ((title: String, amountText: String, source: String, notes: String?) -> Unit)? = null,
    onDeleteFund: ((FundEntity) -> Unit)? = null,
    onToggleMonthlyLimit: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSaveOverallBudget: (budget: Double, applyToMonthOnly: Boolean) -> Unit,
    onSaveCategoryBudget: (categoryId: String, budget: Double?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Funds, 1: Monthly Budget, 2: Category Targets

    var tempBudget by remember { mutableDoubleStateOf(currentBudget) }
    var applyToMonthOnly by remember { mutableStateOf(false) }
    var isMonthlyLimitActive by remember(isMonthlyLimitEnabled) { mutableStateOf(isMonthlyLimitEnabled) }

    val daysInMonth = remember(year, month) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val daysRemaining = remember(year, month, daysInMonth) {
        val today = Calendar.getInstance()
        if (today.get(Calendar.YEAR) == year && (today.get(Calendar.MONTH) + 1) == month) {
            val currentDay = today.get(Calendar.DAY_OF_MONTH)
            (daysInMonth - currentDay + 1).coerceAtLeast(1)
        } else {
            daysInMonth
        }
    }

    val remaining = (tempBudget - monthSpent).coerceAtLeast(0.0)
    val dailyAllowance = if (remaining > 0 && daysRemaining > 0) remaining / daysRemaining else 0.0
    val progressFraction = if (tempBudget > 0) (monthSpent / tempBudget).toFloat() else 0f
    val isOver = monthSpent > tempBudget

    val presetValues = listOf(15000.0, 25000.0, 35000.0, 50000.0, 75000.0, 100000.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("budget_manipulation_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Budget Controller",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Funds", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Monthly Limit", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Category Targets", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // MY FUNDS & INFLOW (Flexible Tracker)
                var newFundAmountText by remember { mutableStateOf("") }
                var newFundSource by remember { mutableStateOf("From Parents") }
                var newFundNote by remember { mutableStateOf("") }
                var fundAmountError by remember { mutableStateOf<String?>(null) }

                val sourcePresets = listOf("From Parents", "Earned", "Pocket Money", "Gift", "Salary", "Other")

                val totalAdded = if (funds.isNotEmpty()) funds.sumOf { it.amount } else currentBudget
                val availBalance = totalAdded - monthSpent

                // Available Balance Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (availBalance >= 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Available to Spend",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", availBalance)}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (availBalance >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = if (availBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Funds Added: $currencySymbol${String.format(Locale.US, "%,.0f", totalAdded)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Spent: $currencySymbol${String.format(Locale.US, "%,.0f", monthSpent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // + Add Funds Form Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "+ Add Money (From Parents or Earned)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add whatever you received as you wish, with no rigid range or limits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newFundAmountText,
                            onValueChange = {
                                newFundAmountText = it
                                fundAmountError = null
                            },
                            label = { Text("Amount ($currencySymbol)") },
                            placeholder = { Text("e.g. 5000, 2000, 500") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = fundAmountError != null,
                            supportingText = fundAmountError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Source:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            sourcePresets.forEach { src ->
                                FilterChip(
                                    selected = newFundSource == src,
                                    onClick = { newFundSource = src },
                                    label = { Text(src, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newFundNote,
                            onValueChange = { newFundNote = it },
                            label = { Text("Note / Description (Optional)") },
                            placeholder = { Text("e.g. Allowance from Dad, freelance") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val amount = newFundAmountText.toDoubleOrNull()
                                if (amount == null || amount <= 0) {
                                    fundAmountError = "Please enter an amount > 0"
                                } else {
                                    onAddFund?.invoke(
                                        newFundNote.ifBlank { newFundSource },
                                        newFundAmountText,
                                        newFundSource,
                                        newFundNote.ifBlank { null }
                                    )
                                    newFundAmountText = ""
                                    newFundNote = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add to My Funds", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Inflow History
                Text(
                    text = "Money Received History:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (funds.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No custom funds logged yet. Add money you received from parents or earned above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                    funds.forEach { fund ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = fund.source,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!fund.notes.isNullOrBlank() && fund.notes != fund.source) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${fund.notes}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = dateFormat.format(Date(fund.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "+$currencySymbol${String.format(Locale.US, "%,.2f", fund.amount)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Emerald700
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onDeleteFund?.invoke(fund) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete Fund Entry",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // MONTHLY BUDGET MANIPULATION TAB

                // Monthly Limit ON/OFF Toggle Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMonthlyLimitActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Monthly Spending Limit",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isMonthlyLimitActive) "Active: spending is tracked against a fixed limit"
                                else "Off: spending is tracked freely against your available funds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isMonthlyLimitActive,
                            onCheckedChange = { checked ->
                                isMonthlyLimitActive = checked
                                onToggleMonthlyLimit?.invoke(checked)
                            },
                            modifier = Modifier.testTag("monthly_limit_toggle_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isMonthlyLimitActive) {
                    // Disabled State Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Monthly Limit is Disabled",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "You are tracking your money freely based on what you have (My Funds) instead of a rigid spending quota. Turn the switch above ON anytime if you want to set a fixed limit with alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    isMonthlyLimitActive = true
                                    onToggleMonthlyLimit?.invoke(true)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("turn_on_monthly_limit_btn")
                            ) {
                                Text("Turn On Monthly Limit")
                            }
                        }
                    }
                } else {
                    // Scope Choice: Global vs Specific Month
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !applyToMonthOnly,
                            onClick = { applyToMonthOnly = false },
                            label = { Text("Default for All Months") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = applyToMonthOnly,
                            onClick = { applyToMonthOnly = true },
                            label = { Text("This Month Only") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Target Display
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "New Monthly Target",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.0f", tempBudget)}",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Increment / Decrement Steppers
                Text(
                    text = "Quick Steppers",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-5000.0, -1000.0, 1000.0, 5000.0).forEach { delta ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (delta > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    tempBudget = (tempBudget + delta).coerceAtLeast(1000.0)
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (delta > 0) "+${delta.toInt() / 1000}k" else "${delta.toInt() / 1000}k",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (delta > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Percentage Tweaks
                Text(
                    text = "Percentage Adjustments",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-0.20, -0.10, 0.10, 0.20).forEach { pct ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val newBudget = (tempBudget * (1.0 + pct))
                                    tempBudget = (Math.round(newBudget / 500.0) * 500.0).coerceAtLeast(1000.0)
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%+d%%", (pct * 100).toInt()),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Fluid Slider
                Text(
                    text = "Drag to Tune",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = tempBudget.toFloat().coerceIn(2000f, 200000f),
                    onValueChange = {
                        val rounded = (Math.round(it / 500f) * 500).toDouble()
                        tempBudget = rounded.coerceAtLeast(1000.0)
                    },
                    valueRange = 2000f..200000f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Presets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetValues.forEach { preset ->
                        val isSelected = tempBudget == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { tempBudget = preset }
                        ) {
                            Text(
                                text = "$currencySymbol${preset.toInt() / 1000}k",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Real-time Impact Simulator Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOver) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isOver) Icons.Default.Warning else Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Simulated Spending Pace",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${(progressFraction * 100).toInt()}% utilized",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val progressColor = when {
                            isOver -> MaterialTheme.colorScheme.error
                            progressFraction > 0.8f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.secondary
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Already Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", monthSpent)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (isOver) "-$currencySymbol${String.format(Locale.US, "%,.0f", monthSpent - tempBudget)}" else "$currencySymbol${String.format(Locale.US, "%,.0f", remaining)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Safe Daily Pace", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (isOver) "$currencySymbol 0/day" else "$currencySymbol${String.format(Locale.US, "%,.0f", dailyAllowance)}/day",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isOver) "⚠️ Spending is already higher than this budget target!" else "Based on $daysRemaining days remaining in this month.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Apply Button
                Button(
                    onClick = {
                        onSaveOverallBudget(tempBudget, applyToMonthOnly)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_budget_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply ${if (applyToMonthOnly) "Month" else "Default"} Budget ($currencySymbol${String.format(Locale.US, "%,.0f", tempBudget)})",
                        fontWeight = FontWeight.Bold
                    )
                }
                }
            } else {
                // CATEGORY TARGETS TAB
                Text(
                    text = "Category Budget Targets",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set dedicated spending limits per category to guard against overspending in food, shopping, entertainment, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                val totalAllocated = categories.sumOf { categoryBudgets[it.id] ?: 0.0 }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Category Allocations:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.0f", totalAllocated)} / $currencySymbol${String.format(Locale.US, "%,.0f", currentBudget)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                categories.forEach { cat ->
                    val catSpend = categorySpendings.find { it.categoryId == cat.id }?.totalAmount ?: 0.0
                    val currentCatBudget = categoryBudgets[cat.id]

                    CategoryBudgetRow(
                        category = cat,
                        currentSpent = catSpend,
                        assignedBudget = currentCatBudget,
                        currencySymbol = currencySymbol,
                        onUpdateBudget = { newLimit ->
                            onSaveCategoryBudget(cat.id, newLimit)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryBudgetRow(
    category: CategoryEntity,
    currentSpent: Double,
    assignedBudget: Double?,
    currencySymbol: String,
    onUpdateBudget: (Double?) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputAmount by remember { mutableStateOf(assignedBudget?.toInt()?.toString() ?: "") }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBadge(
                    iconKey = category.iconKey,
                    colorHex = category.colorHex,
                    size = 36.dp,
                    iconSize = 18.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Spent: $currencySymbol${String.format(Locale.US, "%,.0f", currentSpent)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (assignedBudget != null && assignedBudget > 0) {
                    val catFraction = (currentSpent / assignedBudget).toFloat()
                    val isCatOver = currentSpent > assignedBudget
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCatOver) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Limit: $currencySymbol${String.format(Locale.US, "%,.0f", assignedBudget)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isCatOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    TextButton(onClick = { isEditing = true }) {
                        Text("+ Set Limit", style = MaterialTheme.typography.labelSmall)
                    }
                }

                IconButton(
                    onClick = {
                        isEditing = !isEditing
                        if (isEditing) {
                            inputAmount = assignedBudget?.toInt()?.toString() ?: ""
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Target", modifier = Modifier.size(16.dp))
                }
            }

            // Progress bar if assigned
            if (assignedBudget != null && assignedBudget > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val catFraction = (currentSpent / assignedBudget).toFloat()
                LinearProgressIndicator(
                    progress = { catFraction.coerceIn(0f, 1f) },
                    color = if (catFraction > 1f) MaterialTheme.colorScheme.error else Color(category.colorHex),
                    trackColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            // Quick Inline Edit Drawer
            AnimatedVisibility(visible = isEditing) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text("Budget Limit") },
                            prefix = { Text("$currencySymbol ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val amt = inputAmount.toDoubleOrNull()
                                onUpdateBudget(amt)
                                isEditing = false
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save")
                        }

                        if (assignedBudget != null) {
                            TextButton(
                                onClick = {
                                    onUpdateBudget(null)
                                    inputAmount = ""
                                    isEditing = false
                                }
                            ) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
