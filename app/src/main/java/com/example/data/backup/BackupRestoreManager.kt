package com.example.data.backup

import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ExpenseSplitDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplitEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

data class BackupPreview(
    val schemaVersion: Int,
    val exportTimestamp: Long,
    val categoriesCount: Int,
    val expensesCount: Int,
    val splitsCount: Int = 0,
    val earliestExpenseDateEpoch: Long?,
    val latestExpenseDateEpoch: Long?,
    val totalExpenseAmount: Double,
    val currencySymbol: String
)

sealed class RestoreResult {
    data class Success(
        val categoriesRestored: Int,
        val expensesRestored: Int,
        val splitsRestored: Int,
        val mode: String
    ) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

class BackupRestoreManager(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val expenseSplitDao: ExpenseSplitDao? = null
) {

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
        const val APP_IDENTIFIER = "SpendWise"
    }

    suspend fun createBackupJson(): String {
        val root = JSONObject()
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("app", APP_IDENTIFIER)
        root.put("exportTimestamp", System.currentTimeMillis())

        // Preferences
        val prefs = preferencesRepository.getPreferencesSync()
        val prefsObj = JSONObject()
        prefsObj.put("currencySymbol", prefs.currencySymbol)
        prefsObj.put("themeMode", prefs.themeMode)
        prefsObj.put("monthlyBudget", prefs.monthlyBudget)
        root.put("settings", prefsObj)

        // Categories
        val categories = categoryDao.getAllCategoriesSync()
        val catArray = JSONArray()
        for (cat in categories) {
            val cObj = JSONObject()
            cObj.put("id", cat.id)
            cObj.put("name", cat.name)
            cObj.put("iconKey", cat.iconKey)
            cObj.put("colorHex", cat.colorHex)
            cObj.put("isDefault", cat.isDefault)
            cObj.put("sortOrder", cat.sortOrder)
            catArray.put(cObj)
        }
        root.put("categories", catArray)

        // All splits map by expenseId
        val allSplits = expenseSplitDao?.getAllSplitsSync() ?: emptyList()
        val splitsByExpense = allSplits.groupBy { it.expenseId }

        // Expenses
        val expenses = expenseDao.getAllRawExpensesSync()
        val expArray = JSONArray()
        for (exp in expenses) {
            val eObj = JSONObject()
            eObj.put("id", exp.id)
            eObj.put("title", exp.title)
            eObj.put("amount", exp.amount)
            eObj.put("categoryId", exp.categoryId)
            eObj.put("timestamp", exp.timestamp)
            eObj.put("paymentMethod", exp.paymentMethod)
            if (exp.notes != null) {
                eObj.put("notes", exp.notes)
            }

            // Attached splits
            val attachedSplits = splitsByExpense[exp.id] ?: emptyList()
            if (attachedSplits.isNotEmpty()) {
                val splitArray = JSONArray()
                for (sp in attachedSplits) {
                    val sObj = JSONObject()
                    sObj.put("id", sp.id)
                    sObj.put("expenseId", sp.expenseId)
                    sObj.put("personName", sp.personName)
                    sObj.put("allocatedAmount", sp.allocatedAmount)
                    sObj.put("percentage", sp.percentage)
                    sObj.put("isPaid", sp.isPaid)
                    if (sp.settledTimestamp != null) {
                        sObj.put("settledTimestamp", sp.settledTimestamp)
                    }
                    splitArray.put(sObj)
                }
                eObj.put("splits", splitArray)
            }

            expArray.put(eObj)
        }
        root.put("expenses", expArray)

        return root.toString(2)
    }

    suspend fun writeBackupToStream(outputStream: OutputStream) {
        val json = createBackupJson()
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json)
            writer.flush()
        }
        preferencesRepository.setLastBackupTimestamp(System.currentTimeMillis())
    }

    fun parseAndValidateBackup(jsonString: String): Result<Pair<JSONObject, BackupPreview>> {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("schemaVersion")) {
                return Result.failure(IllegalArgumentException("Invalid backup file: Missing schemaVersion"))
            }
            val schemaVersion = root.getInt("schemaVersion")
            if (schemaVersion > CURRENT_SCHEMA_VERSION) {
                return Result.failure(IllegalArgumentException("Unsupported backup version ($schemaVersion). Please update SpendWise."))
            }

            val appName = root.optString("app", "")
            if (appName.isNotEmpty() && appName != APP_IDENTIFIER) {
                return Result.failure(IllegalArgumentException("Backup file belongs to '$appName', not $APP_IDENTIFIER."))
            }

            val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())
            val settingsObj = root.optJSONObject("settings")
            val currencySymbol = settingsObj?.optString("currencySymbol", "₹") ?: "₹"

            val categoriesArray = root.optJSONArray("categories") ?: JSONArray()
            val expensesArray = root.optJSONArray("expenses") ?: JSONArray()

            var earliestDate: Long? = null
            var latestDate: Long? = null
            var totalAmount = 0.0
            var totalSplits = 0

            for (i in 0 until expensesArray.length()) {
                val expObj = expensesArray.getJSONObject(i)
                val amount = expObj.optDouble("amount", 0.0)
                val time = expObj.optLong("timestamp", 0L)
                totalAmount += amount
                if (earliestDate == null || time < earliestDate) earliestDate = time
                if (latestDate == null || time > latestDate) latestDate = time

                val splitsArray = expObj.optJSONArray("splits")
                if (splitsArray != null) {
                    totalSplits += splitsArray.length()
                }
            }

            val preview = BackupPreview(
                schemaVersion = schemaVersion,
                exportTimestamp = exportTimestamp,
                categoriesCount = categoriesArray.length(),
                expensesCount = expensesArray.length(),
                splitsCount = totalSplits,
                earliestExpenseDateEpoch = earliestDate,
                latestExpenseDateEpoch = latestDate,
                totalExpenseAmount = totalAmount,
                currencySymbol = currencySymbol
            )

            Result.success(Pair(root, preview))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to parse backup JSON: ${e.localizedMessage ?: "Corrupt file"}", e))
        }
    }

    fun parseAndValidateBackup(inputStream: InputStream): Result<Pair<JSONObject, BackupPreview>> {
        return try {
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseAndValidateBackup(jsonString)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Unable to read backup file stream: ${e.localizedMessage}", e))
        }
    }

    suspend fun restoreData(root: JSONObject, replaceAll: Boolean): RestoreResult {
        return try {
            val categoriesArray = root.optJSONArray("categories") ?: JSONArray()
            val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
            val settingsObj = root.optJSONObject("settings")

            val categoriesToRestore = mutableListOf<CategoryEntity>()
            for (i in 0 until categoriesArray.length()) {
                val c = categoriesArray.getJSONObject(i)
                categoriesToRestore.add(
                    CategoryEntity(
                        id = c.getString("id"),
                        name = c.getString("name"),
                        iconKey = c.optString("iconKey", "other"),
                        colorHex = c.optLong("colorHex", 0xFF64748BL),
                        isDefault = c.optBoolean("isDefault", false),
                        sortOrder = c.optInt("sortOrder", i)
                    )
                )
            }

            val expensesToRestore = mutableListOf<ExpenseEntity>()
            val splitsToRestore = mutableListOf<ExpenseSplitEntity>()

            for (i in 0 until expensesArray.length()) {
                val e = expensesArray.getJSONObject(i)
                val expenseId = e.getString("id")
                expensesToRestore.add(
                    ExpenseEntity(
                        id = expenseId,
                        title = e.getString("title"),
                        amount = e.getDouble("amount"),
                        categoryId = e.getString("categoryId"),
                        timestamp = e.getLong("timestamp"),
                        paymentMethod = e.optString("paymentMethod", "Cash"),
                        notes = if (e.has("notes") && !e.isNull("notes")) e.getString("notes") else null
                    )
                )

                val splitsArray = e.optJSONArray("splits")
                if (splitsArray != null) {
                    for (j in 0 until splitsArray.length()) {
                        val s = splitsArray.getJSONObject(j)
                        splitsToRestore.add(
                            ExpenseSplitEntity(
                                id = s.optString("id", java.util.UUID.randomUUID().toString()),
                                expenseId = expenseId,
                                personName = s.getString("personName"),
                                allocatedAmount = s.getDouble("allocatedAmount"),
                                percentage = s.optDouble("percentage", 0.0),
                                isPaid = s.optBoolean("isPaid", false),
                                settledTimestamp = if (s.has("settledTimestamp") && !s.isNull("settledTimestamp")) s.getLong("settledTimestamp") else null
                            )
                        )
                    }
                }
            }

            if (replaceAll) {
                // Wipe and replace
                expenseSplitDao?.deleteAllSplits()
                expenseDao.deleteAllExpenses()
                categoryDao.deleteAll()

                categoryDao.insertCategories(categoriesToRestore)
                expenseDao.insertExpenses(expensesToRestore)
                if (splitsToRestore.isNotEmpty()) {
                    expenseSplitDao?.insertSplits(splitsToRestore)
                }

                if (settingsObj != null) {
                    if (settingsObj.has("currencySymbol")) {
                        preferencesRepository.setCurrencySymbol(settingsObj.getString("currencySymbol"))
                    }
                    if (settingsObj.has("themeMode")) {
                        preferencesRepository.setThemeMode(settingsObj.getString("themeMode"))
                    }
                    if (settingsObj.has("monthlyBudget")) {
                        preferencesRepository.setMonthlyBudget(settingsObj.getDouble("monthlyBudget"))
                    }
                }
                preferencesRepository.setLastBackupTimestamp(System.currentTimeMillis())
                RestoreResult.Success(categoriesToRestore.size, expensesToRestore.size, splitsToRestore.size, "Replaced All")
            } else {
                // Merge mode: Skip duplicates using unique IDs
                val existingCategories = categoryDao.getAllCategoriesSync().associateBy { it.id }
                val newCategories = categoriesToRestore.filter { !existingCategories.containsKey(it.id) }
                if (newCategories.isNotEmpty()) {
                    categoryDao.insertCategories(newCategories)
                }

                val existingExpenses = expenseDao.getAllRawExpensesSync().associateBy { it.id }
                val newExpenses = expensesToRestore.filter { !existingExpenses.containsKey(it.id) }
                if (newExpenses.isNotEmpty()) {
                    expenseDao.insertExpenses(newExpenses)
                }

                if (splitsToRestore.isNotEmpty() && expenseSplitDao != null) {
                    val existingSplits = expenseSplitDao.getAllSplitsSync().associateBy { it.id }
                    val newSplits = splitsToRestore.filter { !existingSplits.containsKey(it.id) }
                    if (newSplits.isNotEmpty()) {
                        expenseSplitDao.insertSplits(newSplits)
                    }
                }

                RestoreResult.Success(newCategories.size, newExpenses.size, splitsToRestore.size, "Merged")
            }
        } catch (e: Exception) {
            RestoreResult.Error(e.localizedMessage ?: "Unknown restore error")
        }
    }
}
