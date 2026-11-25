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
@Config(sdk = [36])
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
        runBlocking {
            preferencesRepository.setSampleDataSeeded(false)
        }
        expenseRepository = ExpenseRepository(expenseDao, categoryDao, preferencesRepository, database.expenseSplitDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `seedInitialDataIfNeeded starts with zero default expenses and preserves subsequent user expenses`() = runBlocking {
        // First initial launch -> purges any default sample data and leaves 0 expenses
        SampleDataSeeder.seedInitialDataIfNeeded(categoryDao, expenseDao, preferencesRepository)
        val prefs = preferencesRepository.getPreferencesSync()
        assertTrue(prefs.isSampleDataSeeded)
        assertTrue(prefs.isDefaultSamplePurged)
        assertEquals(0, expenseDao.getExpensesCount())

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

        // Verify that the user's expense remains untouched
        val expensesAfterRestart = expenseDao.getAllRawExpensesSync()
        assertEquals(1, expensesAfterRestart.size)
        assertEquals("My Real Coffee", expensesAfterRestart.first().title)
        assertEquals(120.0, expensesAfterRestart.first().amount, 0.001)
    }
}
