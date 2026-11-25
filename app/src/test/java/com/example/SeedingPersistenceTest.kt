package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ExpenseSplitDao
import com.example.data.dao.FriendDao
import com.example.data.dao.FundDao
import com.example.data.database.SpendWiseDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplitEntity
import com.example.data.model.FriendEntity
import com.example.data.model.FundEntity
import com.example.data.model.PaymentMethod
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.dataStore
import com.example.data.repository.ExpenseRepository
import com.example.data.seeder.SampleDataSeeder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
    private lateinit var expenseSplitDao: ExpenseSplitDao
    private lateinit var friendDao: FriendDao
    private lateinit var fundDao: FundDao
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
        expenseSplitDao = database.expenseSplitDao()
        friendDao = database.friendDao()
        fundDao = database.fundDao()
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
        preferencesRepository = UserPreferencesRepository(context)
        expenseRepository = ExpenseRepository(
            expenseDao = expenseDao,
            categoryDao = categoryDao,
            preferencesRepository = preferencesRepository,
            expenseSplitDao = expenseSplitDao,
            friendDao = friendDao,
            fundDao = fundDao,
            database = database
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedExistingUserFinancialData(): ExistingSeedFixture {
        val category = CategoryEntity(
            id = "cat_custom_groceries",
            name = "Groceries",
            iconKey = "groceries",
            colorHex = 0xFF3CAE8BL,
            isDefault = false,
            sortOrder = 1
        )
        categoryDao.insertCategory(category)

        val expense = ExpenseEntity(
            id = "exp_user_1",
            title = "Weekly Groceries",
            amount = 1450.0,
            categoryId = category.id,
            timestamp = 1700000000000L,
            paymentMethod = PaymentMethod.UPI.name,
            notes = "Supermarket"
        )
        expenseDao.insertExpense(expense)

        val fund = FundEntity(
            id = "fund_user_1",
            title = "Monthly Allowance",
            amount = 15000.0,
            source = "From Parents",
            timestamp = 1699900000000L,
            notes = "Bank transfer",
            paymentMethod = PaymentMethod.UPI.name
        )
        fundDao.insertFund(fund)

        val friend = FriendEntity(
            id = "friend_user_1",
            name = "Rahul",
            createdTimestamp = 1699800000000L
        )
        friendDao.insertFriend(friend)

        val split = ExpenseSplitEntity(
            id = "split_user_1",
            expenseId = expense.id,
            personName = friend.name,
            allocatedAmount = 725.0,
            percentage = 50.0,
            isPaid = false,
            settledTimestamp = null,
            repaymentMethod = null
        )
        expenseSplitDao.insertSplit(split)

        return ExistingSeedFixture(category, expense, fund, friend, split)
    }

    private suspend fun assertExistingFixtureIntact(fixture: ExistingSeedFixture) {
        val expenses = expenseDao.getAllRawExpensesSync()
        assertEquals(1, expenses.size)
        assertEquals(fixture.expense, expenses.first())

        val funds = fundDao.getAllFundsSync()
        assertEquals(1, funds.size)
        assertEquals(fixture.fund, funds.first())

        val friends = friendDao.getAllFriendsSync()
        assertEquals(1, friends.size)
        assertEquals(fixture.friend, friends.first())

        val splits = expenseSplitDao.getAllSplitsSync()
        assertEquals(1, splits.size)
        assertEquals(fixture.split, splits.first())

        val categories = categoryDao.getAllCategoriesSync()
        assertTrue(categories.any { it == fixture.category })
    }

    @Test
    fun `test 1 - existing user data with missing preference is completely preserved`() = runBlocking {
        val fixture = seedExistingUserFinancialData()
        preferencesRepository.setMonthlyBudget(25000.0)
        preferencesRepository.setMonthlyLimitEnabled(true)
        preferencesRepository.setCurrencySymbol("$")

        // Ensure isDefaultSamplePurged and startup_migration_version are completely unset in DataStore
        context.dataStore.edit { prefs ->
            prefs.remove(booleanPreferencesKey("is_default_sample_purged"))
            prefs.remove(androidx.datastore.preferences.core.intPreferencesKey("startup_migration_version"))
        }
        val freshPrefsRepo = UserPreferencesRepository(context)

        SampleDataSeeder.seedInitialDataIfNeeded(
            categoryDao = categoryDao,
            expenseDao = expenseDao,
            preferencesRepository = freshPrefsRepo,
            expenseSplitDao = expenseSplitDao,
            friendDao = friendDao,
            fundDao = fundDao,
            database = database
        )

        assertExistingFixtureIntact(fixture)
        val afterPrefs = freshPrefsRepo.getPreferencesSync()
        assertEquals(25000.0, afterPrefs.monthlyBudget, 0.001)
        assertTrue(afterPrefs.isMonthlyLimitEnabled)
        assertEquals("$", afterPrefs.currencySymbol)
        assertEquals(SampleDataSeeder.CURRENT_STARTUP_MIGRATION_VERSION, afterPrefs.startupMigrationVersion)
    }

    @Test
    fun `test 2 - existing user data with isDefaultSamplePurged false is completely preserved`() = runBlocking {
        val fixture = seedExistingUserFinancialData()
        preferencesRepository.setMonthlyBudget(18000.0)
        preferencesRepository.setDefaultSamplePurged(false)

        SampleDataSeeder.seedInitialDataIfNeeded(
            categoryDao = categoryDao,
            expenseDao = expenseDao,
            preferencesRepository = preferencesRepository,
            expenseSplitDao = expenseSplitDao,
            friendDao = friendDao,
            fundDao = fundDao,
            database = database
        )

        assertExistingFixtureIntact(fixture)
        val afterPrefs = preferencesRepository.getPreferencesSync()
        assertEquals(18000.0, afterPrefs.monthlyBudget, 0.001)
        assertEquals(SampleDataSeeder.CURRENT_STARTUP_MIGRATION_VERSION, afterPrefs.startupMigrationVersion)
    }

    @Test
    fun `test 3 - existing user data with isDefaultSamplePurged true remains unchanged`() = runBlocking {
        val fixture = seedExistingUserFinancialData()
        preferencesRepository.setMonthlyBudget(32000.0)
        preferencesRepository.setDefaultSamplePurged(true)

        SampleDataSeeder.seedInitialDataIfNeeded(
            categoryDao = categoryDao,
            expenseDao = expenseDao,
            preferencesRepository = preferencesRepository,
            expenseSplitDao = expenseSplitDao,
            friendDao = friendDao,
            fundDao = fundDao,
            database = database
        )

        assertExistingFixtureIntact(fixture)
        assertEquals(32000.0, preferencesRepository.getPreferencesSync().monthlyBudget, 0.001)
    }

    @Test
    fun `test 4 - fresh installation initializes default categories with zero sample expenses`() = runBlocking {
        assertEquals(0, categoryDao.getCount())

        SampleDataSeeder.seedInitialDataIfNeeded(
            categoryDao = categoryDao,
            expenseDao = expenseDao,
            preferencesRepository = preferencesRepository,
            expenseSplitDao = expenseSplitDao,
            friendDao = friendDao,
            fundDao = fundDao,
            database = database
        )

        val categories = categoryDao.getAllCategoriesSync()
        assertEquals(SampleDataSeeder.getDefaultCategories().size, categories.size)
        assertTrue(categories.any { it.id == SampleDataSeeder.DEFAULT_CATEGORY_OTHER_ID })
        assertTrue(categories.any { it.id == SampleDataSeeder.CATEGORY_LENT_ID })
        assertEquals(0, expenseDao.getExpensesCount())
        assertEquals(0, expenseSplitDao.getAllSplitsSync().size)
        assertEquals(0, fundDao.getAllFundsSync().size)
        assertEquals(0, friendDao.getAllFriendsSync().size)

        val prefs = preferencesRepository.getPreferencesSync()
        assertTrue(prefs.isSampleDataSeeded)
        assertTrue(prefs.isDefaultSamplePurged)
        assertEquals(SampleDataSeeder.CURRENT_STARTUP_MIGRATION_VERSION, prefs.startupMigrationVersion)
    }

    @Test
    fun `test 5 - repeated and concurrent startup initialization is idempotent and preserves user data`() = runBlocking {
        val fixture = seedExistingUserFinancialData()

        // Sequential repeated startups
        repeat(3) {
            SampleDataSeeder.seedInitialDataIfNeeded(
                categoryDao = categoryDao,
                expenseDao = expenseDao,
                preferencesRepository = preferencesRepository,
                expenseSplitDao = expenseSplitDao,
                friendDao = friendDao,
                fundDao = fundDao,
                database = database
            )
        }

        // Concurrent startup invocations
        val jobs = (1..5).map {
            async {
                SampleDataSeeder.seedInitialDataIfNeeded(
                    categoryDao = categoryDao,
                    expenseDao = expenseDao,
                    preferencesRepository = preferencesRepository,
                    expenseSplitDao = expenseSplitDao,
                    friendDao = friendDao,
                    fundDao = fundDao,
                    database = database
                )
            }
        }
        jobs.awaitAll()

        assertExistingFixtureIntact(fixture)
        assertEquals(1, categoryDao.getCount())
    }

    @Test
    fun `test 6 - migration failure leaves migration marker incomplete and preserves financial records`() = runBlocking {
        val fixture = seedExistingUserFinancialData()
        preferencesRepository.setMonthlyBudget(22000.0)

        try {
            SampleDataSeeder.seedInitialDataIfNeeded(
                categoryDao = categoryDao,
                expenseDao = expenseDao,
                preferencesRepository = preferencesRepository,
                expenseSplitDao = expenseSplitDao,
                friendDao = friendDao,
                fundDao = fundDao,
                database = database,
                migrationStep = {
                    throw IllegalStateException("Simulated migration failure")
                }
            )
            fail("Expected simulated migration failure to propagate")
        } catch (expected: IllegalStateException) {
            assertEquals("Simulated migration failure", expected.message)
        }

        // Migration marker must NOT be marked completed after failure
        val prefsAfterFailure = preferencesRepository.getPreferencesSync()
        assertEquals(0, prefsAfterFailure.startupMigrationVersion)
        assertFalse(prefsAfterFailure.isDefaultSamplePurged)
        assertEquals(22000.0, prefsAfterFailure.monthlyBudget, 0.001)
        assertExistingFixtureIntact(fixture)

        // Subsequent retry without failure completes safely and still preserves all data
        SampleDataSeeder.seedInitialDataIfNeeded(
            categoryDao = categoryDao,
            expenseDao = expenseDao,
            preferencesRepository = preferencesRepository,
            expenseSplitDao = expenseSplitDao,
            friendDao = friendDao,
            fundDao = fundDao,
            database = database
        )
        assertExistingFixtureIntact(fixture)
        assertEquals(
            SampleDataSeeder.CURRENT_STARTUP_MIGRATION_VERSION,
            preferencesRepository.getPreferencesSync().startupMigrationVersion
        )
    }

    @Test
    fun `test 7 - upgrade simulation from older SpendWise version preserves database and preferences`() = runBlocking {
        val dbName = "legacy_spendwise_upgrade_test.db"
        context.deleteDatabase(dbName)

        // 1. Create a v3 SQLite database matching older SpendWise schema before MIGRATION_3_4
        val legacyHelper = object : SQLiteOpenHelper(context, dbName, null, 3) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expenses` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `notes` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_timestamp` ON `expenses` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expense_splits` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `expenseId` TEXT NOT NULL,
                        `personName` TEXT NOT NULL,
                        `allocatedAmount` REAL NOT NULL,
                        `percentage` REAL NOT NULL,
                        `isPaid` INTEGER NOT NULL,
                        `settledTimestamp` INTEGER,
                        FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_splits_expenseId` ON `expense_splits` (`expenseId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `friends` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `createdTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `funds` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `source` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `notes` TEXT
                    )
                    """.trimIndent()
                )

                // Insert legacy v3 user records
                db.execSQL("INSERT INTO categories VALUES ('cat_default_food', 'Food', 'food', -432362, 1, 1)")
                db.execSQL("INSERT INTO expenses VALUES ('exp_old_1', 'Old Dinner', 900.0, 'cat_default_food', 1700000000000, 'UPI', 'With Priya')")
                db.execSQL("INSERT INTO expense_splits VALUES ('sp_old_1', 'exp_old_1', 'Priya', 450.0, 50.0, 0, NULL)")
                db.execSQL("INSERT INTO friends VALUES ('fr_old_1', 'Priya', 1699900000000)")
                db.execSQL("INSERT INTO funds VALUES ('fund_old_1', 'Legacy Allowance', 12000.0, 'From Parents', 1699800000000, 'Oct allowance')")
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        legacyHelper.writableDatabase.close()

        // 2. Simulate legacy DataStore without is_default_sample_purged or startup_migration_version
        context.dataStore.edit { prefs ->
            prefs.clear()
            prefs[stringPreferencesKey("currency_symbol")] = "€"
            prefs[stringPreferencesKey("theme_mode")] = "DARK"
            prefs[doublePreferencesKey("monthly_budget")] = 40000.0
            prefs[booleanPreferencesKey("is_monthly_limit_enabled")] = true
            prefs[booleanPreferencesKey("is_sample_data_seeded")] = true
            prefs[stringPreferencesKey("month_budgets_json")] = """{"2026-10":35000.0}"""
            prefs[stringPreferencesKey("category_budgets_json")] = """{"cat_default_food":8000.0}"""
        }

        // 3. Open upgraded Room database and run latest startup initialization
        val upgradedDb = Room.databaseBuilder(context, SpendWiseDatabase::class.java, dbName)
            .addMigrations(
                SpendWiseDatabase.MIGRATION_1_2,
                SpendWiseDatabase.MIGRATION_2_3,
                SpendWiseDatabase.MIGRATION_3_4
            )
            .allowMainThreadQueries()
            .build()

        try {
            val upgradedPrefsRepo = UserPreferencesRepository(context)
            SampleDataSeeder.seedInitialDataIfNeeded(
                categoryDao = upgradedDb.categoryDao(),
                expenseDao = upgradedDb.expenseDao(),
                preferencesRepository = upgradedPrefsRepo,
                expenseSplitDao = upgradedDb.expenseSplitDao(),
                friendDao = upgradedDb.friendDao(),
                fundDao = upgradedDb.fundDao(),
                database = upgradedDb
            )

            // Verify all records survived the upgrade
            val expenses = upgradedDb.expenseDao().getAllRawExpensesSync()
            assertEquals(1, expenses.size)
            assertEquals("Old Dinner", expenses.first().title)
            assertEquals(900.0, expenses.first().amount, 0.001)

            val splits = upgradedDb.expenseSplitDao().getAllSplitsSync()
            assertEquals(1, splits.size)
            assertEquals("Priya", splits.first().personName)
            assertEquals(450.0, splits.first().allocatedAmount, 0.001)

            val friends = upgradedDb.friendDao().getAllFriendsSync()
            assertEquals(1, friends.size)
            assertEquals("Priya", friends.first().name)

            val funds = upgradedDb.fundDao().getAllFundsSync()
            assertEquals(1, funds.size)
            assertEquals("Legacy Allowance", funds.first().title)
            assertEquals(12000.0, funds.first().amount, 0.001)

            // Verify all preferences survived intact
            val prefs = upgradedPrefsRepo.getPreferencesSync()
            assertEquals("€", prefs.currencySymbol)
            assertEquals("DARK", prefs.themeMode)
            assertEquals(40000.0, prefs.monthlyBudget, 0.001)
            assertTrue(prefs.isMonthlyLimitEnabled)
            assertEquals(35000.0, prefs.monthBudgets["2026-10"] ?: 0.0, 0.001)
            assertEquals(8000.0, prefs.categoryBudgets["cat_default_food"] ?: 0.0, 0.001)
            assertEquals(SampleDataSeeder.CURRENT_STARTUP_MIGRATION_VERSION, prefs.startupMigrationVersion)
        } finally {
            upgradedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `reSeedSampleData never deletes existing user expenses or splits`() = runBlocking {
        val fixture = seedExistingUserFinancialData()

        expenseRepository.reSeedSampleData()

        assertExistingFixtureIntact(fixture)
    }

    @Test
    fun `deleteExpensesInRange cascades cleanly without leaving orphaned splits`() = runBlocking {
        val fixture = seedExistingUserFinancialData()

        val deleted = expenseDao.deleteExpensesInRange(1699999999000L, 1700000001000L)
        assertEquals(1, deleted)
        assertEquals(0, expenseDao.getExpensesCount())
        assertEquals(0, expenseSplitDao.getAllSplitsSync().size)
        // Other tables remain untouched
        assertEquals(1, fundDao.getAllFundsSync().size)
        assertEquals(1, friendDao.getAllFriendsSync().size)
        assertEquals(fixture.fund, fundDao.getAllFundsSync().first())
    }

    private data class ExistingSeedFixture(
        val category: CategoryEntity,
        val expense: ExpenseEntity,
        val fund: FundEntity,
        val friend: FriendEntity,
        val split: ExpenseSplitEntity
    )
}
