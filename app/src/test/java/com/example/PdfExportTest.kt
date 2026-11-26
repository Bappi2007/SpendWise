package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.SpendWiseDatabase
import com.example.data.export.ExportFormat
import com.example.data.export.PdfGenerator
import com.example.data.export.ReportExporter
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseWithCategory
import com.example.data.model.PaymentMethod
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ExpenseRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfExportTest {

    private lateinit var context: Context
    private lateinit var database: SpendWiseDatabase
    private lateinit var reportExporter: ReportExporter

    private val foodCategory = CategoryEntity(
        id = "cat_food",
        name = "Food",
        iconKey = "food",
        colorHex = 0xFFF97316L,
        isDefault = true,
        sortOrder = 1
    )
    private val transportCategory = CategoryEntity(
        id = "cat_transport",
        name = "Transport",
        iconKey = "transport",
        colorHex = 0xFF0284C7L,
        isDefault = true,
        sortOrder = 2
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SpendWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = ExpenseRepository(
            expenseDao = database.expenseDao(),
            categoryDao = database.categoryDao(),
            preferencesRepository = UserPreferencesRepository(context),
            expenseSplitDao = database.expenseSplitDao(),
            friendDao = database.friendDao(),
            fundDao = database.fundDao(),
            database = database
        )
        reportExporter = ReportExporter(context, repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `export format PDF has expected extension and mime type`() {
        assertEquals("pdf", ExportFormat.PDF.extension)
        assertEquals("application/pdf", ExportFormat.PDF.mimeType)
    }

    @Test
    fun `buildReportSummary aggregates totals categories and date range`() {
        val expenses = listOf(
            ExpenseWithCategory(
                ExpenseEntity("e1", "Lunch", 300.0, foodCategory.id, 1700000000000L, PaymentMethod.UPI.name),
                foodCategory
            ),
            ExpenseWithCategory(
                ExpenseEntity("e2", "Dinner", 700.0, foodCategory.id, 1700100000000L, PaymentMethod.CARD.name),
                foodCategory
            ),
            ExpenseWithCategory(
                ExpenseEntity("e3", "Metro", 200.0, transportCategory.id, 1700050000000L, PaymentMethod.CASH.name),
                transportCategory
            )
        )

        val summary = PdfGenerator.buildReportSummary(expenses)
        assertEquals(1200.0, summary.totalAmount, 0.001)
        assertEquals(3, summary.transactionCount)
        assertEquals(400.0, summary.averageAmount, 0.001)
        assertEquals(1700000000000L, summary.earliestTimestamp)
        assertEquals(1700100000000L, summary.latestTimestamp)
        assertEquals(2, summary.categories.size)
        assertEquals("Food", summary.categories[0].categoryName)
        assertEquals(1000.0, summary.categories[0].totalAmount, 0.001)
        assertEquals("Transport", summary.categories[1].categoryName)
        assertEquals(200.0, summary.categories[1].totalAmount, 0.001)
    }

    @Test
    fun `buildReportSummary handles empty expense list safely`() {
        val summary = PdfGenerator.buildReportSummary(emptyList())
        assertEquals(0.0, summary.totalAmount, 0.001)
        assertEquals(0, summary.transactionCount)
        assertEquals(0.0, summary.averageAmount, 0.001)
        assertNull(summary.earliestTimestamp)
        assertNull(summary.latestTimestamp)
        assertTrue(summary.categories.isEmpty())
    }

    @Test
    fun `fitTextToWidth truncates long labels with ellipsis`() {
        val fitted = PdfGenerator.fitTextToWidth("Very Long Merchant Title Name", 60f) { s ->
            s.length * 6f
        }
        assertTrue(fitted.endsWith("…"))
        assertTrue(fitted.length * 6f <= 60f)
    }

    @Test
    fun `exportToStream generates valid multi-page PDF document`() {
        val expenses = (1..60).map { i ->
            val cat = if (i % 2 == 0) foodCategory else transportCategory
            ExpenseWithCategory(
                expense = ExpenseEntity(
                    id = "exp_$i",
                    title = "Expense Item Number $i",
                    amount = 100.0 + i,
                    categoryId = cat.id,
                    timestamp = 1700000000000L + (i * 3_600_000L),
                    paymentMethod = if (i % 2 == 0) PaymentMethod.UPI.name else PaymentMethod.CASH.name,
                    notes = if (i % 5 == 0) "Note for item $i" else null
                ),
                category = cat
            )
        }

        val output = ByteArrayOutputStream()
        reportExporter.exportToStream(
            format = ExportFormat.PDF,
            expenses = expenses,
            currencySymbol = "₹",
            outputStream = output,
            scopeName = "Current Month"
        )

        val bytes = output.toByteArray()
        assertTrue("PDF output should be non-empty", bytes.isNotEmpty())
        val header = String(bytes.take(5).toByteArray(), Charsets.US_ASCII)
        assertEquals("%PDF-", header)
    }

    @Test
    fun `createShareIntent produces PDF intent with FileProvider URI`() {
        val expenses = listOf(
            ExpenseWithCategory(
                ExpenseEntity("e1", "Coffee", 150.0, foodCategory.id, 1700000000000L, PaymentMethod.UPI.name),
                foodCategory
            )
        )

        val intent = reportExporter.createShareIntent(
            format = ExportFormat.PDF,
            expenses = expenses,
            currencySymbol = "₹",
            scopeName = "Current Month"
        )

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/pdf", intent.type)
        assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        assertTrue(uri != null && uri.toString().endsWith(".pdf"))
    }
}
