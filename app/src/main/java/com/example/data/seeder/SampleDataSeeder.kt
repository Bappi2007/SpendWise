package com.example.data.seeder

import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ExpenseSplitDao
import com.example.data.dao.FriendDao
import com.example.data.dao.FundDao
import com.example.data.model.CategoryEntity
import com.example.data.preferences.UserPreferencesRepository

object SampleDataSeeder {

    val DEFAULT_CATEGORY_OTHER_ID = "cat_default_other"
    val CATEGORY_LENT_ID = "cat_default_money_lent"

    fun getDefaultCategories(): List<CategoryEntity> {
        return listOf(
            CategoryEntity(
                id = "cat_default_food",
                name = "Food",
                iconKey = "food",
                colorHex = 0xFFF97316, // Orange
                isDefault = true,
                sortOrder = 1
            ),
            CategoryEntity(
                id = "cat_default_transport",
                name = "Transport",
                iconKey = "transport",
                colorHex = 0xFF0284C7, // Blue
                isDefault = true,
                sortOrder = 2
            ),
            CategoryEntity(
                id = "cat_default_rent",
                name = "Rent",
                iconKey = "rent",
                colorHex = 0xFF8B5CF6, // Purple
                isDefault = true,
                sortOrder = 3
            ),
            CategoryEntity(
                id = "cat_default_bills",
                name = "Bills",
                iconKey = "bills",
                colorHex = 0xFFDC2626, // Red
                isDefault = true,
                sortOrder = 4
            ),
            CategoryEntity(
                id = "cat_default_shopping",
                name = "Shopping",
                iconKey = "shopping",
                colorHex = 0xFFDB2777, // Pink
                isDefault = true,
                sortOrder = 5
            ),
            CategoryEntity(
                id = "cat_default_health",
                name = "Health",
                iconKey = "health",
                colorHex = 0xFF0D9488, // Teal
                isDefault = true,
                sortOrder = 6
            ),
            CategoryEntity(
                id = "cat_default_entertainment",
                name = "Entertainment",
                iconKey = "entertainment",
                colorHex = 0xFFD97706, // Amber
                isDefault = true,
                sortOrder = 7
            ),
            CategoryEntity(
                id = "cat_default_education",
                name = "Education",
                iconKey = "education",
                colorHex = 0xFF4F46E5, // Indigo
                isDefault = true,
                sortOrder = 8
            ),
            CategoryEntity(
                id = CATEGORY_LENT_ID,
                name = "Money Lent",
                iconKey = "money_lent",
                colorHex = 0xFF0171DF, // Brand Blue
                isDefault = true,
                sortOrder = 9
            ),
            CategoryEntity(
                id = DEFAULT_CATEGORY_OTHER_ID,
                name = "Other",
                iconKey = "other",
                colorHex = 0xFF64748B, // Slate
                isDefault = true,
                sortOrder = 10
            )
        )
    }

    /**
     * Ensures default categories exist and clears any pre-populated/default sample transactions
     * so the app starts completely empty by default.
     */
    suspend fun seedInitialDataIfNeeded(
        categoryDao: CategoryDao,
        expenseDao: ExpenseDao,
        preferencesRepository: UserPreferencesRepository,
        expenseSplitDao: ExpenseSplitDao? = null,
        friendDao: FriendDao? = null,
        fundDao: FundDao? = null
    ) {
        val existingCategoriesCount = categoryDao.getCount()
        if (existingCategoriesCount == 0) {
            categoryDao.insertCategories(getDefaultCategories())
        }

        val prefs = preferencesRepository.getPreferencesSync()
        if (!prefs.isDefaultSamplePurged) {
            expenseSplitDao?.deleteAllSplits()
            expenseDao.deleteAllExpenses()
            fundDao?.deleteAllFunds()
            friendDao?.deleteAllFriends()
            preferencesRepository.setMonthlyBudget(0.0)
            preferencesRepository.setDefaultSamplePurged(true)
        }
        if (!prefs.isSampleDataSeeded) {
            preferencesRepository.setSampleDataSeeded(true)
        }
    }

    suspend fun seedSampleExpenses(
        expenseDao: ExpenseDao,
        expenseSplitDao: ExpenseSplitDao? = null
    ) {
        // Default sample expenses disabled — app starts clean with zero transactions by default.
    }
}
