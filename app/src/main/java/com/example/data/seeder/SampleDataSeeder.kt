package com.example.data.seeder

import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.PaymentMethod
import com.example.data.preferences.UserPreferencesRepository
import java.util.Calendar
import java.util.UUID

object SampleDataSeeder {

    val DEFAULT_CATEGORY_OTHER_ID = "cat_default_other"

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
                id = DEFAULT_CATEGORY_OTHER_ID,
                name = "Other",
                iconKey = "other",
                colorHex = 0xFF64748B, // Slate
                isDefault = true,
                sortOrder = 9
            )
        )
    }

    suspend fun seedInitialDataIfNeeded(
        categoryDao: CategoryDao,
        expenseDao: ExpenseDao,
        preferencesRepository: UserPreferencesRepository
    ) {
        val existingCategoriesCount = categoryDao.getCount()
        if (existingCategoriesCount == 0) {
            categoryDao.insertCategories(getDefaultCategories())
        }

        val prefs = preferencesRepository.getPreferencesSync()
        if (prefs.isSampleDataSeeded) {
            return
        }

        // Only seed sample data if no expenses exist in the database, preventing accidental injection over user records
        val existingExpensesCount = expenseDao.getExpensesCount()
        if (existingExpensesCount == 0) {
            seedSampleExpenses(expenseDao)
        }
        preferencesRepository.setSampleDataSeeded(true)
    }

    suspend fun seedSampleExpenses(expenseDao: ExpenseDao) {
        val expenses = mutableListOf<ExpenseEntity>()
        val calendar = Calendar.getInstance()

        // Helper to get timestamp offset by months and days
        fun getTimestamp(monthOffset: Int, day: Int, hour: Int, minute: Int): Long {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -monthOffset)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(maxDay))
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        // Current month (0 offset) - rich data across various days
        expenses.add(
            ExpenseEntity(
                title = "Monthly House Rent",
                amount = 12000.0,
                categoryId = "cat_default_rent",
                timestamp = getTimestamp(0, 1, 9, 30),
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Paid apartment rent via UPI"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Fiber Broadband & Wi-Fi",
                amount = 999.0,
                categoryId = "cat_default_bills",
                timestamp = getTimestamp(0, 3, 11, 15),
                paymentMethod = PaymentMethod.CARD.name,
                notes = "Monthly internet plan"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Organic Grocery Haul",
                amount = 1850.0,
                categoryId = "cat_default_food",
                timestamp = getTimestamp(0, 5, 17, 45),
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Vegetables, oats, almond milk, fruits"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Metro Smartcard Recharge",
                amount = 500.0,
                categoryId = "cat_default_transport",
                timestamp = getTimestamp(0, 7, 8, 20),
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Weekly commute top-up"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Movie Night & Popcorn",
                amount = 750.0,
                categoryId = "cat_default_entertainment",
                timestamp = getTimestamp(0, 9, 20, 0),
                paymentMethod = PaymentMethod.CARD.name,
                notes = "Watched latest sci-fi release"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Electricity Bill",
                amount = 1420.0,
                categoryId = "cat_default_bills",
                timestamp = getTimestamp(0, 11, 14, 10),
                paymentMethod = PaymentMethod.UPI.name,
                notes = "State power board bill"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Pharmacy & Vitamins",
                amount = 620.0,
                categoryId = "cat_default_health",
                timestamp = getTimestamp(0, 14, 16, 30),
                paymentMethod = PaymentMethod.CASH.name,
                notes = "Multivitamins and first aid"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Casual Sneakers Sale",
                amount = 2499.0,
                categoryId = "cat_default_shopping",
                timestamp = getTimestamp(0, 17, 18, 50),
                paymentMethod = PaymentMethod.CARD.name,
                notes = "Weekend shopping mall discount"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Tech Learning Course",
                amount = 1299.0,
                categoryId = "cat_default_education",
                timestamp = getTimestamp(0, 20, 10, 0),
                paymentMethod = PaymentMethod.CARD.name,
                notes = "Kotlin advanced course"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Cafe Coffee & Croissant",
                amount = 340.0,
                categoryId = "cat_default_food",
                timestamp = getTimestamp(0, 22, 16, 15),
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Work session at cafe"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Cab Ride to Airport",
                amount = 680.0,
                categoryId = "cat_default_transport",
                timestamp = getTimestamp(0, 25, 6, 45),
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Early morning ride"
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Dining Out with Friends",
                amount = 1650.0,
                categoryId = "cat_default_food",
                timestamp = getTimestamp(0, 28, 20, 30),
                paymentMethod = PaymentMethod.UPI.name,
                notes = "Italian dinner celebration"
            )
        )

        // Previous Month (-1 offset)
        expenses.add(
            ExpenseEntity(
                title = "Monthly House Rent",
                amount = 12000.0,
                categoryId = "cat_default_rent",
                timestamp = getTimestamp(1, 1, 10, 0),
                paymentMethod = PaymentMethod.UPI.name
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Groceries & Supermarket",
                amount = 3200.0,
                categoryId = "cat_default_food",
                timestamp = getTimestamp(1, 6, 15, 0),
                paymentMethod = PaymentMethod.CARD.name
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Utility Bills (Gas & Power)",
                amount = 2100.0,
                categoryId = "cat_default_bills",
                timestamp = getTimestamp(1, 12, 11, 0),
                paymentMethod = PaymentMethod.UPI.name
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Dental Cleaning",
                amount = 1500.0,
                categoryId = "cat_default_health",
                timestamp = getTimestamp(1, 18, 14, 0),
                paymentMethod = PaymentMethod.UPI.name
            )
        )
        expenses.add(
            ExpenseEntity(
                title = "Fuel & Toll",
                amount = 1800.0,
                categoryId = "cat_default_transport",
                timestamp = getTimestamp(1, 24, 18, 0),
                paymentMethod = PaymentMethod.CASH.name
            )
        )

        // Months -2, -3, -4, -5 for the 6-month trend chart
        val pastRents = listOf(12000.0, 12000.0, 12000.0, 12000.0)
        val pastVariable = listOf(8400.0, 9600.0, 7800.0, 11200.0)
        for (i in 2..5) {
            expenses.add(
                ExpenseEntity(
                    title = "House Rent",
                    amount = pastRents[i - 2],
                    categoryId = "cat_default_rent",
                    timestamp = getTimestamp(i, 1, 9, 0),
                    paymentMethod = PaymentMethod.UPI.name
                )
            )
            expenses.add(
                ExpenseEntity(
                    title = "Monthly Food & Living",
                    amount = pastVariable[i - 2],
                    categoryId = "cat_default_food",
                    timestamp = getTimestamp(i, 15, 12, 0),
                    paymentMethod = PaymentMethod.UPI.name
                )
            )
            expenses.add(
                ExpenseEntity(
                    title = "Utilities & Commute",
                    amount = 2200.0 + (i * 150),
                    categoryId = "cat_default_bills",
                    timestamp = getTimestamp(i, 20, 16, 0),
                    paymentMethod = PaymentMethod.CARD.name
                )
            )
        }

        expenseDao.insertExpenses(expenses)
    }
}
