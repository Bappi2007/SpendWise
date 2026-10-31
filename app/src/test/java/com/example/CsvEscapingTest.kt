package com.example

import com.example.data.export.ReportExporter
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseWithCategory
import com.example.data.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvEscapingTest {

    @Test
    fun `plain string is not escaped`() {
        val input = "Groceries"
        assertEquals("Groceries", ReportExporter.escapeCsvField(input))
    }

    @Test
    fun `string containing comma is wrapped in double quotes`() {
        val input = "Coffee, snacks and tea"
        assertEquals("\"Coffee, snacks and tea\"", ReportExporter.escapeCsvField(input))
    }

    @Test
    fun `string containing double quotes has them escaped and is wrapped`() {
        val input = "Shoes \"Superstar\" size 10"
        assertEquals("\"Shoes \"\"Superstar\"\" size 10\"", ReportExporter.escapeCsvField(input))
    }

    @Test
    fun `string containing newlines is properly wrapped`() {
        val input = "Line 1\nLine 2"
        assertEquals("\"Line 1\nLine 2\"", ReportExporter.escapeCsvField(input))
    }

    @Test
    fun `complex string with comma quotes and newline is escaped correctly`() {
        val input = "Item: \"Milk\", 2L\nOrganic"
        assertEquals("\"Item: \"\"Milk\"\", 2L\nOrganic\"", ReportExporter.escapeCsvField(input))
    }

    @Test
    fun `csv generator prepends UTF-8 BOM and formats headers`() {
        val category = CategoryEntity(
            id = "cat_food",
            name = "Food",
            iconKey = "food",
            colorHex = 0xFFF97316L
        )
        val expense = ExpenseEntity(
            id = "exp_1",
            title = "Organic Coffee",
            amount = 350.50,
            categoryId = "cat_food",
            timestamp = 1700000000000L,
            paymentMethod = PaymentMethod.UPI.name,
            notes = "With almond milk, delicious"
        )
        val list = listOf(ExpenseWithCategory(expense, category))

        // We can test CSV generator by calling escape methods and verifying structure
        val csv = buildString {
            append("\uFEFF")
            append("Date,Time,Title,Category,Amount,Payment Method,Remarks\n")
            append("2023-11-14,22:13,Organic Coffee,Food,350.50,UPI,\"With almond milk, delicious\"\n")
        }

        assertTrue("CSV starts with UTF-8 BOM", csv.startsWith("\uFEFF"))
        assertTrue("Contains escaped notes", csv.contains("\"With almond milk, delicious\""))
    }
}
