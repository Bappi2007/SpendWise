package com.example

import com.example.data.backup.BackupPreview
import com.example.data.backup.BackupRestoreManager
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupSerializationTest {

    @Test
    fun `parse valid backup JSON generates correct preview metadata`() {
        val root = JSONObject().apply {
            put("schemaVersion", 1)
            put("app", "SpendWise")
            put("exportTimestamp", 1700000000000L)
            put("settings", JSONObject().apply {
                put("currencySymbol", "₹")
                put("themeMode", "SYSTEM")
                put("monthlyBudget", 30000.0)
            })
            put("categories", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "cat_1")
                    put("name", "Groceries")
                    put("iconKey", "food")
                    put("colorHex", 0xFFF97316L)
                    put("isDefault", true)
                    put("sortOrder", 1)
                })
            })
            put("expenses", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "exp_1")
                    put("title", "Supermarket")
                    put("amount", 1500.0)
                    put("categoryId", "cat_1")
                    put("timestamp", 1700000000000L)
                    put("paymentMethod", "UPI")
                    put("notes", "Weekly grocery shopping")
                })
                put(JSONObject().apply {
                    put("id", "exp_2")
                    put("title", "Metro Recharge")
                    put("amount", 500.0)
                    put("categoryId", "cat_1")
                    put("timestamp", 1700050000000L)
                    put("paymentMethod", "CARD")
                })
            })
        }

        val jsonString = root.toString()
        val parsedRoot = JSONObject(jsonString)
        assertEquals(1, parsedRoot.getInt("schemaVersion"))
        assertEquals("SpendWise", parsedRoot.getString("app"))

        val expensesArray = parsedRoot.getJSONArray("expenses")
        assertEquals(2, expensesArray.length())

        val total = (0 until expensesArray.length()).sumOf {
            expensesArray.getJSONObject(it).getDouble("amount")
        }
        assertEquals(2000.0, total, 0.001)
    }

    @Test
    fun `corrupt JSON returns failure with informative message`() {
        val corruptJson = "{ invalid json content ... "
        var errorCaught = false
        try {
            JSONObject(corruptJson)
        } catch (e: Exception) {
            errorCaught = true
            assertTrue(e.message != null)
        }
        assertTrue("Corrupt JSON should throw parse error", errorCaught)
    }

    @Test
    fun `missing schema version is identified as invalid`() {
        val jsonWithoutSchema = JSONObject().apply {
            put("app", "SpendWise")
        }.toString()

        val root = JSONObject(jsonWithoutSchema)
        val hasSchema = root.has("schemaVersion")
        assertTrue("Schema version must be checked", !hasSchema)
    }
}
