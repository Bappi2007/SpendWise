package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupRestoreManager
import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ExpenseSplitDao
import com.example.data.database.SpendWiseDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplitEntity
import com.example.data.model.PaymentMethod
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ExpenseRepository
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SplitExpensesTest {

    private lateinit var context: Context
    private lateinit var database: SpendWiseDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseSplitDao: ExpenseSplitDao
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var backupRestoreManager: BackupRestoreManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SpendWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseDao = database.expenseDao()
        categoryDao = database.categoryDao()
        expenseSplitDao = database.expenseSplitDao()
        preferencesRepository = UserPreferencesRepository(context)
        expenseRepository = ExpenseRepository(expenseDao, categoryDao, preferencesRepository, expenseSplitDao)
        backupRestoreManager = BackupRestoreManager(categoryDao, expenseDao, preferencesRepository, expenseSplitDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saveExpenseWithSplits persists expense and attached splits in Room`() = runBlocking {
        val category = CategoryEntity(
            id = "cat_test_food",
            name = "Food",
            iconKey = "food",
            colorHex = 0xFFF97316L,
            isDefault = true,
            sortOrder = 1
        )
        categoryDao.insertCategory(category)

        val expenseId = "exp_split_dinner_1"
        val totalAmount = 1800.0
        val expense = ExpenseEntity(
            id = expenseId,
            title = "Weekend Dinner",
            amount = totalAmount,
            categoryId = category.id,
            timestamp = System.currentTimeMillis(),
            paymentMethod = PaymentMethod.UPI.name
        )

        // Split with 2 friends: Alice ₹600 (Paid), Bob ₹600 (Unpaid)
        val splits = listOf(
            ExpenseSplitEntity(
                id = "split_alice",
                expenseId = expenseId,
                personName = "Alice",
                allocatedAmount = 600.0,
                percentage = 33.33,
                isPaid = true
            ),
            ExpenseSplitEntity(
                id = "split_bob",
                expenseId = expenseId,
                personName = "Bob",
                allocatedAmount = 600.0,
                percentage = 33.33,
                isPaid = false
            )
        )

        expenseRepository.saveExpenseWithSplits(expense, splits)

        // Verify with Category & Splits relation query
        val fetched = expenseDao.getExpenseById(expenseId)
        assertNotNull(fetched)
        assertEquals("Weekend Dinner", fetched!!.expense.title)
        assertTrue(fetched.isSplit)
        assertEquals(2, fetched.splits.size)
        assertEquals(1, fetched.settledSplitsCount)
        assertEquals(1, fetched.pendingSplitsCount)
        assertEquals(600.0, fetched.pendingAmountToReceive, 0.001)
        assertEquals(600.0, fetched.myShare, 0.001) // 1800 - (600 + 600) = 600
    }

    @Test
    fun `setSplitPaidStatus toggles payment settlement status accurately`() = runBlocking {
        val expenseId = "exp_split_cab_1"
        val expense = ExpenseEntity(
            id = expenseId,
            title = "Airport Cab",
            amount = 900.0,
            categoryId = "cat_test",
            timestamp = System.currentTimeMillis()
        )
        val split = ExpenseSplitEntity(
            id = "split_rahul",
            expenseId = expenseId,
            personName = "Rahul",
            allocatedAmount = 450.0,
            percentage = 50.0,
            isPaid = false
        )

        expenseRepository.saveExpenseWithSplits(expense, listOf(split))

        var splits = expenseSplitDao.getSplitsForExpenseSync(expenseId)
        assertFalse("Rahul should initially be unpaid", splits.first().isPaid)

        // Mark as paid
        expenseRepository.setSplitPaidStatus("split_rahul", true)
        splits = expenseSplitDao.getSplitsForExpenseSync(expenseId)
        assertTrue("Rahul should be marked as paid", splits.first().isPaid)
        assertNotNull(splits.first().settledTimestamp)

        // Mark back as unpaid
        expenseRepository.setSplitPaidStatus("split_rahul", false)
        splits = expenseSplitDao.getSplitsForExpenseSync(expenseId)
        assertFalse("Rahul should be marked unpaid again", splits.first().isPaid)
    }

    @Test
    fun `deleting an expense cascades and deletes its splits`() = runBlocking {
        val expenseId = "exp_cascade_test"
        val expense = ExpenseEntity(
            id = expenseId,
            title = "Concert Tickets",
            amount = 3000.0,
            categoryId = "cat_test",
            timestamp = System.currentTimeMillis()
        )
        val splits = listOf(
            ExpenseSplitEntity(
                id = "split_p1",
                expenseId = expenseId,
                personName = "Friend 1",
                allocatedAmount = 1000.0,
                isPaid = false
            ),
            ExpenseSplitEntity(
                id = "split_p2",
                expenseId = expenseId,
                personName = "Friend 2",
                allocatedAmount = 1000.0,
                isPaid = true
            )
        )

        expenseRepository.saveExpenseWithSplits(expense, splits)
        assertEquals(2, expenseSplitDao.getAllSplitsSync().size)

        // Delete expense
        expenseRepository.deleteExpenseById(expenseId)
        assertEquals(0, expenseDao.getExpensesCount())
        assertEquals(0, expenseSplitDao.getAllSplitsSync().size)
    }

    @Test
    fun `backup and restore preserves splits and payment checkbox statuses`() = runBlocking {
        val category = CategoryEntity(
            id = "cat_travel",
            name = "Travel",
            iconKey = "transport",
            colorHex = 0xFF0284C7L,
            isDefault = true,
            sortOrder = 1
        )
        categoryDao.insertCategory(category)

        val expenseId = "exp_roadtrip"
        val expense = ExpenseEntity(
            id = expenseId,
            title = "Weekend Road Trip Fuel",
            amount = 4000.0,
            categoryId = category.id,
            timestamp = 1700000000000L,
            paymentMethod = PaymentMethod.CARD.name
        )
        val splits = listOf(
            ExpenseSplitEntity(
                id = "sp_alex",
                expenseId = expenseId,
                personName = "Alex",
                allocatedAmount = 1000.0,
                percentage = 25.0,
                isPaid = true
            ),
            ExpenseSplitEntity(
                id = "sp_priya",
                expenseId = expenseId,
                personName = "Priya",
                allocatedAmount = 1000.0,
                percentage = 25.0,
                isPaid = false
            )
        )
        expenseRepository.saveExpenseWithSplits(expense, splits)

        // Create backup JSON
        val backupJson = backupRestoreManager.createBackupJson()
        assertTrue(backupJson.contains("schemaVersion\": 2"))
        assertTrue(backupJson.contains("Weekend Road Trip Fuel"))
        assertTrue(backupJson.contains("Alex"))
        assertTrue(backupJson.contains("Priya"))

        // Validate preview
        val parseResult = backupRestoreManager.parseAndValidateBackup(backupJson)
        assertTrue(parseResult.isSuccess)
        val (parsedRoot, preview) = parseResult.getOrThrow()
        assertEquals(1, preview.expensesCount)
        assertEquals(2, preview.splitsCount)
        assertEquals(4000.0, preview.totalExpenseAmount, 0.001)

        // Wipe database
        expenseRepository.clearAllData()
        assertEquals(0, expenseDao.getExpensesCount())
        assertEquals(0, expenseSplitDao.getAllSplitsSync().size)

        // Restore
        val restoreResult = backupRestoreManager.restoreData(parsedRoot, replaceAll = true)
        assertTrue(restoreResult is com.example.data.backup.RestoreResult.Success)

        // Check restored records
        val restoredSplits = expenseSplitDao.getAllSplitsSync()
        assertEquals(2, restoredSplits.size)
        val restoredAlex = restoredSplits.first { it.personName == "Alex" }
        assertTrue(restoredAlex.isPaid)
        assertEquals(1000.0, restoredAlex.allocatedAmount, 0.001)

        val restoredPriya = restoredSplits.first { it.personName == "Priya" }
        assertFalse(restoredPriya.isPaid)
        assertEquals(1000.0, restoredPriya.allocatedAmount, 0.001)
    }

    @Test
    fun `settlement overview aggregates pending amounts correctly by person`() {
        val splits = listOf(
            ExpenseSplitEntity(id = "1", expenseId = "e1", personName = "Rahul", allocatedAmount = 500.0, isPaid = false),
            ExpenseSplitEntity(id = "2", expenseId = "e2", personName = "Rahul", allocatedAmount = 250.0, isPaid = false),
            ExpenseSplitEntity(id = "3", expenseId = "e3", personName = "Rahul", allocatedAmount = 300.0, isPaid = true),
            ExpenseSplitEntity(id = "4", expenseId = "e1", personName = "Sara", allocatedAmount = 400.0, isPaid = false),
            ExpenseSplitEntity(id = "5", expenseId = "e2", personName = "Sara", allocatedAmount = 400.0, isPaid = true)
        )

        val overview = ExpenseViewModel.calculateSettlementOverview(splits)
        assertEquals(1150.0, overview.totalPendingAmount, 0.001) // 500 + 250 + 400 = 1150
        assertEquals(2, overview.totalPendingPeopleCount)

        val rahul = overview.peopleSummaries.first { it.personName.equals("Rahul", ignoreCase = true) }
        assertEquals(750.0, rahul.pendingAmount, 0.001)
        assertEquals(2, rahul.pendingCount)
        assertEquals(1, rahul.settledCount)

        val sara = overview.peopleSummaries.first { it.personName.equals("Sara", ignoreCase = true) }
        assertEquals(400.0, sara.pendingAmount, 0.001)
        assertEquals(1, sara.pendingCount)
        assertEquals(1, sara.settledCount)
    }
}
