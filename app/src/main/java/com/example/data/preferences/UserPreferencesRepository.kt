package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spendwise_settings")

data class UserPreferences(
    val currencySymbol: String = "₹",
    val themeMode: String = "SYSTEM",
    val monthlyBudget: Double = 0.0,
    val lastBackupTimestamp: Long = 0L,
    val isSampleDataSeeded: Boolean = false,
    val categoryBudgets: Map<String, Double> = emptyMap(),
    val monthBudgets: Map<String, Double> = emptyMap(),
    val isMonthlyLimitEnabled: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val IS_SAMPLE_DATA_SEEDED = booleanPreferencesKey("is_sample_data_seeded")
        val CATEGORY_BUDGETS_JSON = stringPreferencesKey("category_budgets_json")
        val MONTH_BUDGETS_JSON = stringPreferencesKey("month_budgets_json")
        val IS_MONTHLY_LIMIT_ENABLED = booleanPreferencesKey("is_monthly_limit_enabled")
    }

    @Volatile
    private var cachedPreferences: UserPreferences? = null

    private fun mapPreferences(preferences: Preferences): UserPreferences {
        val catJson = preferences[PreferencesKeys.CATEGORY_BUDGETS_JSON] ?: "{}"
        val catMap = parseJsonToDoubleMap(catJson)

        val monthJson = preferences[PreferencesKeys.MONTH_BUDGETS_JSON] ?: "{}"
        val monthMap = parseJsonToDoubleMap(monthJson)

        return UserPreferences(
            currencySymbol = preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: "₹",
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            monthlyBudget = preferences[PreferencesKeys.MONTHLY_BUDGET] ?: 0.0,
            lastBackupTimestamp = preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] ?: 0L,
            isSampleDataSeeded = preferences[PreferencesKeys.IS_SAMPLE_DATA_SEEDED] ?: false,
            categoryBudgets = catMap,
            monthBudgets = monthMap,
            isMonthlyLimitEnabled = preferences[PreferencesKeys.IS_MONTHLY_LIMIT_ENABLED] ?: false
        )
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val mapped = mapPreferences(preferences)
        cachedPreferences = mapped
        mapped
    }

    suspend fun getPreferencesSync(): UserPreferences {
        return cachedPreferences ?: userPreferencesFlow.first().also { cachedPreferences = it }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        val updated = context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = symbol
        }
        cachedPreferences = mapPreferences(updated)
    }

    suspend fun setThemeMode(mode: String) {
        val updated = context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
        cachedPreferences = mapPreferences(updated)
    }

    suspend fun setMonthlyBudget(budget: Double) {
        val updated = context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONTHLY_BUDGET] = budget
        }
        cachedPreferences = mapPreferences(updated)
    }

    suspend fun setMonthBudget(year: Int, month: Int, budget: Double) {
        val key = formatYearMonthKey(year, month)
        val updated = context.dataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.MONTH_BUDGETS_JSON] ?: "{}"
            val map = parseJsonToDoubleMap(currentJson).toMutableMap()
            map[key] = budget
            preferences[PreferencesKeys.MONTH_BUDGETS_JSON] = mapToJson(map)
        }
        cachedPreferences = mapPreferences(updated)
    }

    suspend fun setCategoryBudget(categoryId: String, budget: Double?) {
        val updated = context.dataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.CATEGORY_BUDGETS_JSON] ?: "{}"
            val map = parseJsonToDoubleMap(currentJson).toMutableMap()
            if (budget == null || budget <= 0) {
                map.remove(categoryId)
            } else {
                map[categoryId] = budget
            }
            preferences[PreferencesKeys.CATEGORY_BUDGETS_JSON] = mapToJson(map)
        }
        cachedPreferences = mapPreferences(updated)
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        val updated = context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
        cachedPreferences = mapPreferences(updated)
    }

    suspend fun setSampleDataSeeded(seeded: Boolean) {
        val updated = context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SAMPLE_DATA_SEEDED] = seeded
        }
        cachedPreferences = mapPreferences(updated)
    }

    suspend fun setMonthlyLimitEnabled(enabled: Boolean) {
        val updated = context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_MONTHLY_LIMIT_ENABLED] = enabled
        }
        cachedPreferences = mapPreferences(updated)
    }

    suspend fun clearSettings() {
        val updated = context.dataStore.edit { it.clear() }
        cachedPreferences = mapPreferences(updated)
    }

    companion object {
        fun formatYearMonthKey(year: Int, month: Int): String {
            return String.format(java.util.Locale.US, "%04d-%02d", year, month)
        }

        private fun parseJsonToDoubleMap(jsonString: String): Map<String, Double> {
            return try {
                val json = JSONObject(jsonString)
                val map = mutableMapOf<String, Double>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = json.optDouble(k, 0.0)
                }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        }

        private fun mapToJson(map: Map<String, Double>): String {
            val json = JSONObject()
            for ((k, v) in map) {
                json.put(k, v)
            }
            return json.toString()
        }
    }
}
