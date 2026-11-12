package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.ExpenseDao
import com.example.data.dao.FriendDao
import com.example.data.dao.FundDao
import com.example.data.database.SpendWiseDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FriendEntity
import com.example.data.model.FundEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.ExpenseWithCategory
import com.example.ui.viewmodel.DashboardTransactionItem
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FundsAndFriendsTest {

    private lateinit var context: Context
    private lateinit var database: SpendWiseDatabase
    private lateinit var friendDao: FriendDao
    private lateinit var fundDao: FundDao
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SpendWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        friendDao = database.friendDao()
        fundDao = database.fundDao()
        expenseDao = database.expenseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `friendDao supports insert, list and delete`() = runBlocking {
        val f1 = FriendEntity(id = "f_1", name = "Rahul", createdTimestamp = 1000L)
        val f2 = FriendEntity(id = "f_2", name = "Amit", createdTimestamp = 2000L)

        friendDao.insertFriend(f1)
        friendDao.insertFriend(f2)

        val friends = friendDao.getAllFriends().first()
        assertEquals(2, friends.size)
        // Check sorted by name ASC
        assertEquals("Amit", friends[0].name)
        assertEquals("Rahul", friends[1].name)

        // Delete friend
        friendDao.deleteFriend(f1)
        val remaining = friendDao.getAllFriends().first()
        assertEquals(1, remaining.size)
        assertEquals("Amit", remaining[0].name)
    }

    @Test
    fun `fundDao supports logging money from parents or earned with dynamic sum`() = runBlocking {
        val fund1 = FundEntity(
            id = "fund_1",
            title = "Pocket Money",
            amount = 5000.0,
            source = "From Parents",
            timestamp = 10000L
        )
        val fund2 = FundEntity(
            id = "fund_2",
            title = "Freelance Project",
            amount = 3500.0,
            source = "Earned",
            timestamp = 20000L
        )

        fundDao.insertFund(fund1)
        fundDao.insertFund(fund2)

        val allFunds = fundDao.getAllFunds().first()
        assertEquals(2, allFunds.size)

        val totalSum = fundDao.getTotalFundsAmount().first()
        assertEquals(8500.0, totalSum ?: 0.0, 0.01)

        // Delete a fund
        fundDao.deleteFund(fund1)
        val newSum = fundDao.getTotalFundsAmount().first()
        assertEquals(3500.0, newSum ?: 0.0, 0.01)
    }

    @Test
    fun `deleteExpensesInRange removes only expenses inside selected date range`() = runBlocking {
        val category = CategoryEntity(
            id = "cat_test",
            name = "General",
            iconKey = "general",
            colorHex = 0xFF10B981L,
            isDefault = true,
            sortOrder = 1
        )
        database.categoryDao().insertCategory(category)

        // Insert 3 expenses: before, during, and after a target window [1000L, 2000L]
        val expBefore = ExpenseEntity(
            id = "e_before",
            title = "Old Expense",
            amount = 100.0,
            categoryId = category.id,
            timestamp = 500L,
            paymentMethod = PaymentMethod.CASH.name
        )
        val expInside1 = ExpenseEntity(
            id = "e_in_1",
            title = "Inside Range 1",
            amount = 250.0,
            categoryId = category.id,
            timestamp = 1200L,
            paymentMethod = PaymentMethod.UPI.name
        )
        val expInside2 = ExpenseEntity(
            id = "e_in_2",
            title = "Inside Range 2",
            amount = 450.0,
            categoryId = category.id,
            timestamp = 1800L,
            paymentMethod = PaymentMethod.CARD.name
        )
        val expAfter = ExpenseEntity(
            id = "e_after",
            title = "Future Expense",
            amount = 300.0,
            categoryId = category.id,
            timestamp = 3000L,
            paymentMethod = PaymentMethod.UPI.name
        )

        expenseDao.insertExpense(expBefore)
        expenseDao.insertExpense(expInside1)
        expenseDao.insertExpense(expInside2)
        expenseDao.insertExpense(expAfter)

        val countInRange = expenseDao.countExpensesInRange(1000L, 2000L)
        assertEquals(2, countInRange)

        // Delete range
        val deletedCount = expenseDao.deleteExpensesInRange(1000L, 2000L)
        assertEquals(2, deletedCount)

        // Verify remaining expenses
        val remaining = expenseDao.getAllRawExpensesSync()
        assertEquals(2, remaining.size)
        val remainingIds = remaining.map { it.id }
        assertTrue(remainingIds.contains("e_before"))
        assertTrue(remainingIds.contains("e_after"))
    }

    @Test
    fun `calculateHomeSummary with zero budget and no funds shows zero everywhere`() {
        // When user clears all expenses and funds
        val summary = ExpenseViewModel.calculateHomeSummary(
            list = emptyList(),
            budget = 0.0,
            funds = emptyList(),
            isMonthlyLimitEnabled = false
        )

        assertEquals(0.0, summary.todaySpent, 0.001)
        assertEquals(0.0, summary.monthSpent, 0.001)
        assertEquals(0.0, summary.monthlyBudget, 0.001)
        assertEquals(0.0, summary.budgetRemaining, 0.001)
        assertEquals(0.0, summary.totalFundsAdded, 0.001)
        assertEquals(0.0, summary.availableBalance, 0.001)
    }

    @Test
    fun `groupTransactionsByDay merges expenses and added funds chronologically on main dashboard`() {
        val category = CategoryEntity(
            id = "cat_general",
            name = "General",
            iconKey = "general",
            colorHex = 0xFF10B981L,
            isDefault = true
        )
        val now = System.currentTimeMillis()
        val expense = ExpenseEntity(
            id = "e_1",
            title = "Coffee",
            amount = 150.0,
            categoryId = category.id,
            timestamp = now - 5000L
        )
        val fund = FundEntity(
            id = "f_1",
            title = "Pocket Money",
            amount = 2000.0,
            source = "From Parents",
            timestamp = now - 1000L
        )

        val grouped = ExpenseViewModel.groupTransactionsByDay(
            expenses = listOf(ExpenseWithCategory(expense, category)),
            funds = listOf(fund)
        )

        assertEquals(1, grouped.size)
        val dayGroup = grouped.first()
        assertEquals(2, dayGroup.items.size)
        assertEquals(150.0, dayGroup.dayExpenseTotal, 0.001)
        assertEquals(2000.0, dayGroup.dayFundTotal, 0.001)

        // Fund was logged at now - 1000L, Expense was logged at now - 5000L
        // So Fund must appear first in descending chronological order
        assertTrue(dayGroup.items[0] is DashboardTransactionItem.Fund)
        assertEquals("Pocket Money", dayGroup.items[0].title)
        assertTrue(dayGroup.items[1] is DashboardTransactionItem.Expense)
        assertEquals("Coffee", dayGroup.items[1].title)
    }

    @Test
    fun `when monthly limit is disabled month summary calculates budget according to total income`() = runBlocking {
        val category = CategoryEntity(
            id = "cat_general",
            name = "General",
            iconKey = "general",
            colorHex = 0xFF10B981L,
            isDefault = true
        )
        database.categoryDao().insertCategory(category)

        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 10)
            set(java.util.Calendar.HOUR_OF_DAY, 12)
        }
        val midMonth = cal.timeInMillis

        // Add 10,000 income from parents
        fundDao.insertFund(
            FundEntity(
                id = "fund_income_1",
                title = "Income from Parents",
                amount = 10000.0,
                source = "From Parents",
                timestamp = midMonth
            )
        )

        // Add 3,000 expense
        expenseDao.insertExpense(
            ExpenseEntity(
                id = "exp_1",
                title = "Groceries",
                amount = 3000.0,
                categoryId = category.id,
                timestamp = midMonth
            )
        )

        val repository = com.example.data.repository.ExpenseRepository(
            expenseDao = expenseDao,
            categoryDao = database.categoryDao(),
            preferencesRepository = com.example.data.preferences.UserPreferencesRepository(context),
            expenseSplitDao = database.expenseSplitDao(),
            friendDao = friendDao,
            fundDao = fundDao
        )
        repository.setMonthlyLimitEnabled(false)

        val summary = repository.calculateMonthSummary(
            year = cal.get(java.util.Calendar.YEAR),
            month = cal.get(java.util.Calendar.MONTH) + 1,
            monthlyBudget = 25000.0
        )

        assertEquals(10000.0, summary.monthlyBudget, 0.001)
        assertEquals(3000.0, summary.totalSpent, 0.001)
        assertEquals(0.3f, summary.budgetProgress, 0.01f)
        org.junit.Assert.assertFalse(summary.isOverBudget)
        org.junit.Assert.assertFalse(summary.isMonthlyLimitEnabled)
    }

    @Test
    fun `when money is lent to friend and repaid, available funds decreases then automatically restores`() = runBlocking {
        val repository = com.example.data.repository.ExpenseRepository(
            expenseDao = expenseDao,
            categoryDao = database.categoryDao(),
            preferencesRepository = com.example.data.preferences.UserPreferencesRepository(context),
            expenseSplitDao = database.expenseSplitDao(),
            friendDao = friendDao,
            fundDao = fundDao
        )

        // 1. Initial fund of 10,000
        fundDao.insertFund(
            FundEntity(
                id = "fund_init",
                title = "Salary",
                amount = 10000.0,
                source = "Job",
                timestamp = System.currentTimeMillis()
            )
        )

        val totalFundsBefore = fundDao.getTotalFundsAmountSync()
        assertEquals(10000.0, totalFundsBefore, 0.001)

        // 2. Lend 2,500 to Rahul
        val (expense, split) = repository.lendMoneyToFriend(
            friendName = "Rahul",
            amount = 2500.0,
            paymentMethod = PaymentMethod.UPI,
            notes = "Emergency money"
        )

        assertEquals("Lent to Rahul", expense.title)
        assertEquals(2500.0, expense.amount, 0.001)
        assertEquals("Rahul", split.personName)
        assertEquals(2500.0, split.allocatedAmount, 0.001)
        org.junit.Assert.assertFalse(split.isPaid)

        // Friend should automatically be saved in FriendDao
        val savedFriend = friendDao.getFriendByName("Rahul")
        org.junit.Assert.assertNotNull(savedFriend)

        // 3. Mark debt as settled / repaid
        val updatedSplit = repository.setSplitPaidStatus(split.id, true)
        org.junit.Assert.assertNotNull(updatedSplit)
        assertTrue(updatedSplit!!.isPaid)

        // Repayment fund should automatically be added
        val allFundsAfterRepaid = fundDao.getAllFundsSync()
        val repaymentFund = allFundsAfterRepaid.find { it.id == "repay_split_${split.id}" }
        org.junit.Assert.assertNotNull(repaymentFund)
        assertEquals("Repayment from Rahul", repaymentFund!!.title)
        assertEquals(2500.0, repaymentFund.amount, 0.001)

        val totalFundsAfterRepaid = fundDao.getTotalFundsAmountSync()
        assertEquals(12500.0, totalFundsAfterRepaid, 0.001)

        // 4. If unmarked as paid, repayment fund should be cleanly removed
        val unmarkedSplit = repository.setSplitPaidStatus(split.id, false)
        org.junit.Assert.assertNotNull(unmarkedSplit)
        org.junit.Assert.assertFalse(unmarkedSplit!!.isPaid)

        val allFundsAfterUnmarked = fundDao.getAllFundsSync()
        val removedRepayment = allFundsAfterUnmarked.find { it.id == "repay_split_${split.id}" }
        org.junit.Assert.assertNull(removedRepayment)
        assertEquals(10000.0, fundDao.getTotalFundsAmountSync(), 0.001)
    }
}
