package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.database.SpendWiseDatabase
import com.example.data.model.ExpenseEntity
import com.example.data.model.PaymentMethod
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.seeder.SampleDataSeeder
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
@Config(sdk = [34])
class SeedingPersistenceTest {

    private lateinit var context: Context
    private lateinit var database: SpendWiseDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var expenseRepository: ExpenseRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SpendWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseDao = database.expenseDao()
        categoryDao = database.categoryDao()
        preferencesRepository = UserPreferencesRepository(context)
        expenseRepository = ExpenseRepository(expenseDao, categoryDao, preferencesRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `clearAllData keeps isSampleDataSeeded true so restart does not restore sample data`() = runBlocking {
        // First initial seed
        SampleDataSeeder.seedInitialDataIfNeeded(categoryDao, expenseDao, preferencesRepository)
        assertTrue(preferencesRepository.getPreferencesSync().isSampleDataSeeded)
        val initialCount = expenseDao.getExpensesCount()
        assertTrue("Initial expenses must be seeded", initialCount > 0)

        // User clears data
        expenseRepository.clearAllData()
        assertEquals(0, expenseDao.getExpensesCount())
        assertTrue("isSampleDataSeeded must remain true after clearing all data", preferencesRepository.getPreferencesSync().isSampleDataSeeded)

        // User adds their own expense
        val userExpense = ExpenseEntity(
            title = "My Real Coffee",
            amount = 120.0,
            categoryId = SampleDataSeeder.DEFAULT_CATEGORY_OTHER_ID,
            timestamp = System.currentTimeMillis(),
            paymentMethod = PaymentMethod.UPI.name
        )
        expenseDao.insertExpense(userExpense)
        assertEquals(1, expenseDao.getExpensesCount())

        // App restarts -> seedInitialDataIfNeeded is called again
        SampleDataSeeder.seedInitialDataIfNeeded(categoryDao, expenseDao, preferencesRepository)

        // Verify that sample data was NOT re-seeded and only user expense remains
        val expensesAfterRestart = expenseDao.getAllRawExpensesSync()
        assertEquals(1, expensesAfterRestart.size)
        assertEquals("My Real Coffee", expensesAfterRestart.first().title)
        assertEquals(120.0, expensesAfterRestart.first().amount, 0.001)
    }

    @Test
    fun `seedInitialDataIfNeeded never injects sample data when user expenses already exist`() = runBlocking {
        // Reset preferences to unseeded state for test
        preferencesRepository.setSampleDataSeeded(false)

        // Seed categories only
        categoryDao.insertCategories(SampleDataSeeder.getDefaultCategories())

        // User already has a custom expense
        val userExpense = ExpenseEntity(
            title = "Freelance Client Lunch",
            amount = 450.0,
            categoryId = "cat_default_food",
            timestamp = System.currentTimeMillis(),
            paymentMethod = PaymentMethod.CARD.name
        )
        expenseDao.insertExpense(userExpense)
        assertEquals(1, expenseDao.getExpensesCount())

        // Launch app
        SampleDataSeeder.seedInitialDataIfNeeded(categoryDao, expenseDao, preferencesRepository)

        // Must still only have 1 expense, not 1 + 22 sample expenses
        assertEquals(1, expenseDao.getExpensesCount())
        val expenses = expenseDao.getAllRawExpensesSync()
        assertEquals("Freelance Client Lunch", expenses.first().title)
        assertTrue(preferencesRepository.getPreferencesSync().isSampleDataSeeded)
    }
}
