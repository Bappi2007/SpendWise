package com.example.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.ExpenseWithCategory
import com.example.data.repository.ExpenseRepository
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportScope(val displayName: String) {
    CURRENT_MONTH("Current Month"),
    CUSTOM_RANGE("Custom Date Range"),
    ALL_TIME("All Time")
}

enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    CSV("csv", "text/csv", "CSV Spreadsheet (.csv)"),
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel Workbook (.xlsx)")
}

class ReportExporter(
    private val context: Context,
    private val expenseRepository: ExpenseRepository
) {

    companion object {
        fun escapeCsvField(value: String): String {
            return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        }
    }

    suspend fun fetchExpensesForScope(
        scope: ExportScope,
        year: Int,
        month: Int,
        customStartMillis: Long,
        customEndMillis: Long
    ): List<ExpenseWithCategory> {
        return when (scope) {
            ExportScope.CURRENT_MONTH -> {
                val (start, end) = ExpenseRepository.getMonthBounds(year, month)
                expenseRepository.getExpensesForDateRangeSync(start, end)
            }
            ExportScope.CUSTOM_RANGE -> {
                expenseRepository.getExpensesForDateRangeSync(customStartMillis, customEndMillis)
            }
            ExportScope.ALL_TIME -> {
                expenseRepository.getAllExpensesSync()
            }
        }
    }

    fun generateCsvString(expenses: List<ExpenseWithCategory>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        // Prepend UTF-8 BOM so Excel opens it properly with special characters
        sb.append("\uFEFF")

        // Header
        sb.append("Date,Time,Title,Category,Amount,Payment Method,Remarks\n")

        for (item in expenses) {
            val dateStr = escapeCsvField(dateFormat.format(Date(item.expense.timestamp)))
            val timeStr = escapeCsvField(timeFormat.format(Date(item.expense.timestamp)))
            val title = escapeCsvField(item.expense.title)
            val category = escapeCsvField(item.categoryName)
            val amount = String.format(Locale.US, "%.2f", item.expense.amount)
            val payment = escapeCsvField(item.expense.paymentMethod)
            val remarks = escapeCsvField(item.expense.notes ?: "")

            sb.append("$dateStr,$timeStr,$title,$category,$amount,$payment,$remarks\n")
        }

        return sb.toString()
    }

    fun exportToStream(
        format: ExportFormat,
        expenses: List<ExpenseWithCategory>,
        currencySymbol: String,
        outputStream: OutputStream
    ) {
        when (format) {
            ExportFormat.CSV -> {
                val csv = generateCsvString(expenses)
                outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(csv)
                    writer.flush()
                }
            }
            ExportFormat.EXCEL -> {
                XlsxGenerator.generateWorkbook(expenses, currencySymbol, outputStream)
            }
        }
    }

    fun createShareIntent(
        format: ExportFormat,
        expenses: List<ExpenseWithCategory>,
        currencySymbol: String,
        scopeName: String
    ): Intent {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedScope = scopeName.lowercase(Locale.getDefault()).replace(" ", "_")
        val fileName = "spendwise_${sanitizedScope}_$timestamp.${format.extension}"
        val file = File(reportsDir, fileName)

        file.outputStream().use { os ->
            exportToStream(format, expenses, currencySymbol, os)
        }

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "SpendWise Expense Report ($scopeName)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
