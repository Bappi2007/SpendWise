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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplitEntity
import com.example.data.model.ExpenseWithCategory
import com.example.data.model.FriendEntity
import com.example.data.model.FundEntity
import com.example.data.model.PaymentMethod
import com.example.ui.charts.SplitExpenseGraph
import com.example.ui.common.CategoryIconBadge
import com.example.ui.components.AddExpenseBottomSheet
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandYellow
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Mint400
import com.example.ui.theme.RedDanger
import androidx.compose.material.icons.filled.AccountBalanceWallet
import com.example.ui.viewmodel.DailyGroupedExpenses
import com.example.ui.viewmodel.DailyGroupedTransactions
import com.example.ui.viewmodel.DashboardTransactionItem
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.HomeSummary
import com.example.ui.viewmodel.PersonSettlementSummary
import com.example.ui.viewmodel.SettlementOverview
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    var expenseToViewSplits by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    var isSettlementSummaryOpen by remember { mutableStateOf(false) }
    var expenseToDeleteWithConfirm by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    var fundToDeleteWithConfirm by remember { mutableStateOf<FundEntity?>(null) }

    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val settlementOverview by viewModel.settlementOverview.collectAsStateWithLifecycle()
    val allFriends by viewModel.allFriends.collectAsStateWithLifecycle()
    val allFunds by viewModel.allFunds.collectAsStateWithLifecycle()
    val groupedTransactions by viewModel.groupedTransactions.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    expenseToEdit = null
                    isAddSheetVisible = true
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense",
                    tint = MaterialTheme.colorScheme.onTertiary
                )
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

            // Dedicated Split Settlement Banner ("Who Owes Me & Split Hub")
            item {
                WhoOwesMeCard(
                    settlementOverview = settlementOverview,
                    friendsCount = allFriends.size,
                    currencySymbol = currencySymbol,
                    onViewAll = { isSettlementSummaryOpen = true },
                    onTogglePaid = { splitId, isPaid ->
                        viewModel.toggleSplitPaidStatus(splitId, isPaid)
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("who_owes_me_card")
                )
            }

            // Section Header: Recent Transactions (Expenses & Added Funds)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${groupedTransactions.sumOf { it.items.size }} logged",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clickable { isBudgetSheetVisible = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Funds",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Add Fund",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Empty State
            if (groupedTransactions.isEmpty()) {
                item {
                    EmptyExpenseState(
                        onAddExpense = {
                            expenseToEdit = null
                            isAddSheetVisible = true
                        },
                        onAddFund = {
                            isBudgetSheetVisible = true
                        }
                    )
                }
            } else {
                // Grouped Transaction Items (Expenses & Inflows)
                groupedTransactions.forEach { dayGroup ->
                    item(key = "header_${dayGroup.dateLabel}_${dayGroup.dateEpochMillis}") {
                        DailyTransactionGroupHeader(
                            dateLabel = dayGroup.dateLabel,
                            dayExpenseTotal = dayGroup.dayExpenseTotal,
                            dayFundTotal = dayGroup.dayFundTotal,
                            currencySymbol = currencySymbol
                        )
                    }

                    items(
                        items = dayGroup.items,
                        key = { it.id }
                    ) { transactionItem ->
                        when (transactionItem) {
                            is DashboardTransactionItem.Expense -> {
                                SwipeableExpenseItem(
                                    item = transactionItem.expenseWithCategory,
                                    currencySymbol = currencySymbol,
                                    onEdit = {
                                        expenseToEdit = transactionItem.expenseWithCategory
                                        isAddSheetVisible = true
                                    },
                                    onDelete = {
                                        expenseToDeleteWithConfirm = transactionItem.expenseWithCategory
                                    },
                                    onViewSplits = {
                                        expenseToViewSplits = transactionItem.expenseWithCategory
                                    }
                                )
                            }
                            is DashboardTransactionItem.Fund -> {
                                DashboardFundItem(
                                    fund = transactionItem.fund,
                                    currencySymbol = currencySymbol,
                                    onDelete = {
                                        fundToDeleteWithConfirm = transactionItem.fund
                                    }
                                )
                            }
                        }
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
            friends = allFriends,
            onDismiss = {
                isAddSheetVisible = false
                expenseToEdit = null
            },
            onSaveExpense = { title, amountText, categoryId, timestamp, paymentMethod, notes, splits ->
                if (expenseToEdit == null) {
                    viewModel.addExpense(title, amountText, categoryId, timestamp, paymentMethod, notes, splits)
                } else {
                    viewModel.updateExpense(
                        expenseToEdit!!.expense.id,
                        title,
                        amountText,
                        categoryId,
                        timestamp,
                        paymentMethod,
                        notes,
                        splits
                    )
                }
            }
        )
    }

    // Interactive Split Details & Settlement Dialog for an Expense
    expenseToViewSplits?.let { item ->
        SplitSettlementDialog(
            item = item,
            currencySymbol = currencySymbol,
            onDismiss = { expenseToViewSplits = null },
            onTogglePaid = { splitId, isPaid ->
                viewModel.toggleSplitPaidStatus(splitId, isPaid)
            },
            onEditExpense = {
                expenseToEdit = item
                expenseToViewSplits = null
                isAddSheetVisible = true
            }
        )
    }

    // Dedicated Full Settlement Summary Dialog (with Split Graphs & Friends Manager)
    if (isSettlementSummaryOpen) {
        SettlementSummaryDialog(
            overview = settlementOverview,
            friends = allFriends,
            currencySymbol = currencySymbol,
            onDismiss = { isSettlementSummaryOpen = false },
            onTogglePaid = { splitId, isPaid ->
                viewModel.toggleSplitPaidStatus(splitId, isPaid)
            },
            onAddFriend = { name ->
                viewModel.addFriend(name)
            },
            onRemoveFriend = { friend ->
                viewModel.removeFriend(friend)
            }
        )
    }

    // Budget Manipulation Sheet
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
            funds = allFunds,
            isMonthlyLimitEnabled = userPrefs.isMonthlyLimitEnabled,
            onAddFund = { title, amt, src, notes ->
                viewModel.addFund(title, amt, src, notes)
            },
            onDeleteFund = { fund ->
                viewModel.deleteFund(fund)
            },
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

    // Delete Fund Confirmation Dialog
    fundToDeleteWithConfirm?.let { fund ->
        AlertDialog(
            onDismissRequest = { fundToDeleteWithConfirm = null },
            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Fund Entry?") },
            text = { Text("Are you sure you want to remove \"${fund.title}\" for +$currencySymbol${String.format(Locale.US, "%,.2f", fund.amount)} (${fund.source})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFund(fund)
                        fundToDeleteWithConfirm = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fundToDeleteWithConfirm = null }) {
                    Text("Cancel")
                }
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
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(BrandNavy, BrandBlue)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Top Row: Available Balance / My Funds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (summary.hasCustomFunds || !summary.isMonthlyLimitEnabled) "Available Balance (My Funds)" else "Available Budget",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", summary.availableBalance)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    // Button to Add Money or adjust budget (Brand Yellow with Navy text for high contrast)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandYellow,
                        modifier = Modifier.clickable(onClick = onAdjustBudget)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Funds",
                                tint = BrandNavy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Money",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandNavy
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Row: Total Funds Added & Spent This Month
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Today's Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", summary.todaySpent)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Month Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", summary.monthSpent)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (summary.hasCustomFunds) "Total Inflow" else if (summary.isMonthlyLimitEnabled) "Target Limit" else "Total Inflow",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.0f", if (summary.hasCustomFunds) summary.totalFundsAdded else summary.monthlyBudget)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                // If monthly budget limit is enabled by user, show progress bar
                if (summary.isMonthlyLimitEnabled && summary.monthlyBudget > 0) {
                    Spacer(modifier = Modifier.height(12.dp))

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
                                    text = "Monthly Spending Limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Adjust",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            val budgetStatusText = if (summary.monthSpent > summary.monthlyBudget) {
                                "Over by $currencySymbol${String.format(Locale.US, "%,.0f", summary.monthSpent - summary.monthlyBudget)}"
                            } else {
                                "$currencySymbol${String.format(Locale.US, "%,.0f", summary.budgetRemaining)} remaining"
                            }
                            Text(
                                text = budgetStatusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (summary.monthSpent > summary.monthlyBudget) Color(0xFFFECDD3) else BrandGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val animatedProgress by animateFloatAsState(
                            targetValue = summary.budgetProgress.coerceIn(0f, 1f),
                            label = "budget_progress"
                        )

                        val progressColor = when {
                            summary.budgetProgress > 1f -> RedDanger
                            summary.budgetProgress > 0.8f -> BrandYellow
                            else -> BrandGreen
                        }

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WhoOwesMeCard(
    settlementOverview: SettlementOverview,
    friendsCount: Int,
    currencySymbol: String,
    onViewAll: () -> Unit,
    onTogglePaid: (splitId: String, isPaid: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Split Hub & Friends",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (settlementOverview.totalPendingAmount > 0) {
                                "$currencySymbol${String.format(Locale.US, "%,.2f", settlementOverview.totalPendingAmount)} pending • ${settlementOverview.totalPendingPeopleCount} friends"
                            } else {
                                "$friendsCount friends saved • All settled"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(onClick = onViewAll) {
                    Text("Split Hub", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (settlementOverview.totalPendingAmount > 0) {
                Spacer(modifier = Modifier.height(10.dp))

                // Quick rows for the top 2 pending people
                settlementOverview.peopleSummaries.filter { it.pendingAmount > 0 }.take(2).forEach { person ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = person.personName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${person.pendingCount} unpaid)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", person.pendingAmount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                person.splits.firstOrNull { !it.isPaid }?.let { sp ->
                                    IconButton(
                                        onClick = { onTogglePaid(sp.id, true) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Mark received from ${person.personName}",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .clickable(onClick = onViewAll)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "View split graphs & manage friends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
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

@Composable
fun DailyTransactionGroupHeader(
    dateLabel: String,
    dayExpenseTotal: Double,
    dayFundTotal: Double,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (dayFundTotal > 0.0) {
                Text(
                    text = "+$currencySymbol${String.format(Locale.US, "%,.2f", dayFundTotal)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (dayExpenseTotal > 0.0) {
                Text(
                    text = "-$currencySymbol${String.format(Locale.US, "%,.2f", dayExpenseTotal)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (dayFundTotal == 0.0 && dayExpenseTotal == 0.0) {
                Text(
                    text = "$currencySymbol 0.00",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableExpenseItem(
    item: ExpenseWithCategory,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewSplits: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
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
            onClick = {
                if (item.isSplit) {
                    onViewSplits()
                } else {
                    onEdit()
                }
            },
            onSplitClick = onViewSplits
        )
    }
}

@Composable
fun ExpenseRowCard(
    item: ExpenseWithCategory,
    currencySymbol: String,
    onClick: () -> Unit,
    onSplitClick: () -> Unit
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
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

                // Split Badge if expense is split
                if (item.isSplit) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val isFullySettled = item.pendingSplitsCount == 0
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isFullySettled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.clickable(onClick = onSplitClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallSplit,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isFullySettled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFullySettled) {
                                    "Split (${item.splits.size}) • Settled"
                                } else {
                                    "Split (${item.splits.size}) • ${item.pendingSplitsCount} unpaid ($currencySymbol${String.format(Locale.US, "%,.0f", item.pendingAmountToReceive)})"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = if (isFullySettled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
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

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", item.expense.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.isSplit) {
                    Text(
                        text = "Your share: $currencySymbol${String.format(Locale.US, "%,.0f", item.myShare)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardFundItem(
    fund: FundEntity,
    currencySymbol: String,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
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
                        contentDescription = "Delete Fund",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        FundRowCard(
            fund = fund,
            currencySymbol = currencySymbol,
            onDelete = onDelete
        )
    }
}

@Composable
fun FundRowCard(
    fund: FundEntity,
    currencySymbol: String,
    onDelete: () -> Unit
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
            .testTag("fund_item_${fund.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Inflow Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Added Fund",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fund.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = fund.source,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = timeFormat.format(Date(fund.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!fund.notes.isNullOrBlank()) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = fund.notes,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+$currencySymbol${String.format(Locale.US, "%,.2f", fund.amount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Inflow",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove fund",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SplitSettlementDialog(
    item: ExpenseWithCategory,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onTogglePaid: (splitId: String, isPaid: Boolean) -> Unit,
    onEditExpense: () -> Unit
) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Split Settlements",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.expense.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total Expense: $currencySymbol${String.format(Locale.US, "%,.2f", item.expense.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown summary card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Your Share:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.2f", item.myShare)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Pending to Receive:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.2f", item.pendingAmountToReceive)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (item.pendingAmountToReceive > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Friends' Shares (Check off who gave you money):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height((item.splits.size * 56).coerceAtMost(220).dp)) {
                    items(items = item.splits, key = { it.id }) { split ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = split.isPaid,
                                    onCheckedChange = { checked ->
                                        onTogglePaid(split.id, checked)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("checkbox_paid_${split.id}")
                                )
                                Column {
                                    Text(
                                        text = split.personName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (split.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (split.isPaid) "Paid back" else "Unpaid",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (split.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.2f", split.allocatedAmount)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (split.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditExpense,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Expense")
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementSummaryDialog(
    overview: SettlementOverview,
    friends: List<FriendEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onTogglePaid: (splitId: String, isPaid: Boolean) -> Unit,
    onAddFriend: (String) -> Unit,
    onRemoveFriend: (FriendEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var newFriendName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Split Hub & Friends",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Receivables",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Graph",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "Friends (${friends.size})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 0: Who Owes Me Breakdown
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Total Pending Receivable", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", overview.totalPendingAmount)}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = "${overview.totalPendingPeopleCount} pending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Breakdown by Person:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (overview.peopleSummaries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No split expenses logged yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.height(240.dp)) {
                                items(items = overview.peopleSummaries, key = { it.personName }) { person ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = person.personName,
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (person.pendingAmount > 0) {
                                                        "$currencySymbol${String.format(Locale.US, "%,.2f", person.pendingAmount)} pending"
                                                    } else {
                                                        "All settled"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = if (person.pendingAmount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            person.splits.forEach { sp ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Checkbox(
                                                            checked = sp.isPaid,
                                                            onCheckedChange = { checked ->
                                                                onTogglePaid(sp.id, checked)
                                                            },
                                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                                        )
                                                        Text(
                                                            text = if (sp.isPaid) "Settled" else "Pending",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (sp.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    Text(
                                                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", sp.allocatedAmount)}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: Split Expense Graph
                        Box(modifier = Modifier.height(300.dp)) {
                            SplitExpenseGraph(
                                overview = overview,
                                currencySymbol = currencySymbol,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    2 -> {
                        // TAB 2: Manage Friends (Add / Remove)
                        Column(modifier = Modifier.height(300.dp)) {
                            // Add Friend Input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newFriendName,
                                    onValueChange = { newFriendName = it },
                                    label = { Text("Friend's Name") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("friend_name_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Button(
                                    onClick = {
                                        if (newFriendName.isNotBlank()) {
                                            onAddFriend(newFriendName.trim())
                                            newFriendName = ""
                                        }
                                    },
                                    enabled = newFriendName.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("add_friend_btn")
                                ) {
                                    Text("Add")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (friends.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "No saved friends yet",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Add friends above for quick 1-tap splitting!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    items(items = friends, key = { it.id }) { friend ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = friend.name.take(1).uppercase(),
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = friend.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { onRemoveFriend(friend) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Remove ${friend.name}",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun EmptyExpenseState(
    onAddExpense: () -> Unit,
    onAddFund: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No transactions yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add funds or log an expense to start tracking your balance",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onAddFund != null) {
                OutlinedButton(
                    onClick = onAddFund,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Funds")
                }
            }
            Button(
                onClick = onAddExpense,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Expense")
            }
        }
    }
}
