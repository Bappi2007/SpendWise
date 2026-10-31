package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseWithCategory
import com.example.data.model.PaymentMethod
import com.example.ui.common.CategoryIconBadge
import com.example.ui.components.AddExpenseBottomSheet
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Mint400
import com.example.ui.viewmodel.DailyGroupedExpenses
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.HomeSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Tune
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    summary: HomeSummary,
    groupedExpenses: List<DailyGroupedExpenses>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    var isAddSheetVisible by remember { mutableStateOf(false) }
    var isBudgetSheetVisible by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    var expenseToDeleteWithConfirm by remember { mutableStateOf<ExpenseWithCategory?>(null) }

    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    expenseToEdit = null
                    isAddSheetVisible = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Gradient Header Summary Card
            item {
                HomeHeaderCard(
                    summary = summary,
                    currencySymbol = currencySymbol,
                    onAdjustBudget = { isBudgetSheetVisible = true },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("home_header_card")
                )
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${groupedExpenses.sumOf { it.expenses.size }} logged",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Empty State
            if (groupedExpenses.isEmpty()) {
                item {
                    EmptyExpenseState(
                        onAddExpense = {
                            expenseToEdit = null
                            isAddSheetVisible = true
                        }
                    )
                }
            } else {
                // Grouped Expense Items
                groupedExpenses.forEach { dayGroup ->
                    // Sticky / Group Date Header with daily sum
                    item(key = "header_${dayGroup.dateLabel}_${dayGroup.dateEpochMillis}") {
                        DailyGroupHeader(
                            dateLabel = dayGroup.dateLabel,
                            dayTotal = dayGroup.dayTotal,
                            currencySymbol = currencySymbol
                        )
                    }

                    items(
                        items = dayGroup.expenses,
                        key = { it.expense.id }
                    ) { item ->
                        SwipeableExpenseItem(
                            item = item,
                            currencySymbol = currencySymbol,
                            onEdit = {
                                expenseToEdit = item
                                isAddSheetVisible = true
                            },
                            onDelete = {
                                expenseToDeleteWithConfirm = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Bottom Sheet
    if (isAddSheetVisible) {
        AddExpenseBottomSheet(
            categories = categories,
            currencySymbol = currencySymbol,
            expenseToEdit = expenseToEdit,
            onDismiss = {
                isAddSheetVisible = false
                expenseToEdit = null
            },
            onSaveExpense = { title, amountText, categoryId, timestamp, paymentMethod, notes ->
                if (expenseToEdit == null) {
                    viewModel.addExpense(title, amountText, categoryId, timestamp, paymentMethod, notes)
                } else {
                    viewModel.updateExpense(
                        expenseToEdit!!.expense.id,
                        title,
                        amountText,
                        categoryId,
                        timestamp,
                        paymentMethod,
                        notes
                    )
                }
            }
        )
    }

    // Swipe-to-Delete Confirmation Dialog
    expenseToDeleteWithConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { expenseToDeleteWithConfirm = null },
            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Expense?") },
            text = { Text("Are you sure you want to delete \"${item.expense.title}\" for $currencySymbol${String.format(Locale.US, "%,.2f", item.expense.amount)}? You can also undo right after.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(item)
                        expenseToDeleteWithConfirm = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDeleteWithConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Budget Manipulation Bottom Sheet
    if (isBudgetSheetVisible) {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1

        com.example.ui.components.BudgetManipulationSheet(
            currentBudget = summary.monthlyBudget,
            monthSpent = summary.monthSpent,
            currencySymbol = currencySymbol,
            year = currentYear,
            month = currentMonth,
            categories = categories,
            categorySpendings = emptyList(),
            categoryBudgets = userPrefs.categoryBudgets,
            onDismiss = { isBudgetSheetVisible = false },
            onSaveOverallBudget = { newBudget, applyToMonthOnly ->
                if (applyToMonthOnly) {
                    viewModel.setMonthSpecificBudget(currentYear, currentMonth, newBudget)
                } else {
                    viewModel.setMonthlyBudget(newBudget)
                }
            },
            onSaveCategoryBudget = { catId, limit ->
                viewModel.setCategoryBudget(catId, limit)
            }
        )
    }
}

@Composable
fun HomeHeaderCard(
    summary: HomeSummary,
    currencySymbol: String,
    onAdjustBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Emerald900,
            Emerald700
        )
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SpendWise",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Mint400
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Offline & Private",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Today and Month Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Today's Spend",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", summary.todaySpent)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Month to Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", summary.monthSpent)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Mint400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier.clickable(onClick = onAdjustBudget)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Monthly Budget",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable(onClick = onAdjustBudget)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Adjust Budget",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Adjust",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        val budgetStatusText = if (summary.monthSpent > summary.monthlyBudget) {
                            "Over budget by $currencySymbol${String.format(Locale.US, "%,.0f", summary.monthSpent - summary.monthlyBudget)}"
                        } else {
                            "$currencySymbol${String.format(Locale.US, "%,.0f", summary.budgetRemaining)} remaining"
                        }
                        Text(
                            text = budgetStatusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (summary.monthSpent > summary.monthlyBudget) Color(0xFFFCA5A5) else Mint400
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val animatedProgress by animateFloatAsState(
                        targetValue = summary.budgetProgress.coerceIn(0f, 1f),
                        label = "budget_progress"
                    )

                    val progressColor = when {
                        summary.budgetProgress > 1f -> Color(0xFFEF4444)
                        summary.budgetProgress > 0.8f -> Color(0xFFF59E0B)
                        else -> Mint400
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(summary.budgetProgress * 100).toInt()}% used",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Budget: $currencySymbol${String.format(Locale.US, "%,.0f", summary.monthlyBudget)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyGroupHeader(
    dateLabel: String,
    dayTotal: Double,
    currencySymbol: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$currencySymbol${String.format(Locale.US, "%,.2f", dayTotal)}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableExpenseItem(
    item: ExpenseWithCategory,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // Wait for confirmation
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        ExpenseRowCard(
            item = item,
            currencySymbol = currencySymbol,
            onClick = onEdit
        )
    }
}

@Composable
fun ExpenseRowCard(
    item: ExpenseWithCategory,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("expense_item_${item.expense.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                iconKey = item.categoryIconKey,
                colorHex = item.categoryColorHex,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.expense.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Payment Method Pill
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = item.expense.paymentMethod,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = timeFormat.format(Date(item.expense.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!item.expense.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.expense.notes,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$currencySymbol${String.format(Locale.US, "%,.2f", item.expense.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptyExpenseState(onAddExpense: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No expenses recorded yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap the + button to log your first daily spend",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
