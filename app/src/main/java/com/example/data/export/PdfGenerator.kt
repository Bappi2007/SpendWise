package com.example.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.ExpenseWithCategory
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class PdfCategoryRow(
    val categoryName: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Double
)

internal data class PdfReportSummary(
    val totalAmount: Double,
    val transactionCount: Int,
    val averageAmount: Double,
    val earliestTimestamp: Long?,
    val latestTimestamp: Long?,
    val categories: List<PdfCategoryRow>
)

/**
 * Generates clean, multi-page A4 PDF expense reports using Android's built-in [PdfDocument].
 */
object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 42f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2f)

    private const val COLOR_NAVY = 0xFF121660.toInt()
    private const val COLOR_BLUE = 0xFF0171DF.toInt()
    private const val COLOR_GREEN = 0xFF1F7A5C.toInt()
    private const val COLOR_SURFACE_TINT = 0xFFF4F8FC.toInt()
    private const val COLOR_CARD_BORDER = 0xFFCAD8E8.toInt()
    private const val COLOR_TEXT_PRIMARY = 0xFF121660.toInt()
    private const val COLOR_TEXT_SECONDARY = 0xFF4A5580.toInt()
    private const val COLOR_ROW_ALT = 0xFFF8FAFC.toInt()
    private const val COLOR_DIVIDER = 0xFFE2EBF4.toInt()

    internal fun buildReportSummary(expenses: List<ExpenseWithCategory>): PdfReportSummary {
        val total = expenses.sumOf { it.expense.amount }
        val count = expenses.size
        val average = if (count > 0) total / count else 0.0
        val earliest = expenses.minOfOrNull { it.expense.timestamp }
        val latest = expenses.maxOfOrNull { it.expense.timestamp }

        val categories = expenses
            .groupBy { it.categoryName }
            .map { (name, items) ->
                val catTotal = items.sumOf { it.expense.amount }
                val pct = if (total > 0.0) (catTotal / total) * 100.0 else 0.0
                PdfCategoryRow(
                    categoryName = name,
                    totalAmount = catTotal,
                    count = items.size,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalAmount }

        return PdfReportSummary(
            totalAmount = total,
            transactionCount = count,
            averageAmount = average,
            earliestTimestamp = earliest,
            latestTimestamp = latest,
            categories = categories
        )
    }

    internal fun fitTextToWidth(
        text: String,
        maxWidth: Float,
        measureWidth: (String) -> Float
    ): String {
        if (text.isEmpty() || measureWidth(text) <= maxWidth) return text
        val ellipsis = "…"
        var low = 0
        var high = text.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            val candidate = text.substring(0, mid) + ellipsis
            if (measureWidth(candidate) <= maxWidth) {
                low = mid
            } else {
                high = mid - 1
            }
        }
        return if (low > 0) text.substring(0, low) + ellipsis else ellipsis
    }

    fun generateReport(
        expenses: List<ExpenseWithCategory>,
        currencySymbol: String,
        scopeName: String,
        outputStream: OutputStream
    ) {
        val summary = buildReportSummary(expenses)
        val sortedExpenses = expenses.sortedByDescending { it.expense.timestamp }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val generatedTimestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val document = PdfDocument()
        try {
            val paints = PdfPaints()
            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            var canvas = page.canvas
            var y = MARGIN_TOP

            fun finishAndStartNextPage(continuationSection: String? = null) {
                drawFooter(canvas, pageNumber, paints)
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN_TOP

                // Compact continuation bar
                canvas.drawText("SpendWise Expense Report ($scopeName)", MARGIN_H, y + 10f, paints.continuationTitle)
                canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN_H, y + 10f, paints.continuationRight)
                y += 16f
                canvas.drawLine(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y, paints.divider)
                y += 14f

                if (continuationSection != null) {
                    canvas.drawText(continuationSection, MARGIN_H, y + 11f, paints.sectionTitle)
                    y += 18f
                }
            }

            fun ensureSpace(neededHeight: Float, onNewPage: () -> Unit = {}) {
                if (y + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    finishAndStartNextPage()
                    onNewPage()
                }
            }

            // 1. Header Banner
            val bannerRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 76f)
            canvas.drawRoundRect(bannerRect, 10f, 10f, paints.headerBg)
            // Left accent bar
            val accentRect = RectF(MARGIN_H, y, MARGIN_H + 6f, y + 76f)
            canvas.drawRect(accentRect, paints.accentFill)

            canvas.drawText("SpendWise Expense Report", MARGIN_H + 18f, y + 28f, paints.headerTitle)
            val dateSpanText = if (summary.earliestTimestamp != null && summary.latestTimestamp != null) {
                "${displayDateFormat.format(Date(summary.earliestTimestamp))} – ${displayDateFormat.format(Date(summary.latestTimestamp))}"
            } else {
                "No transactions"
            }
            canvas.drawText("Scope: $scopeName  •  $dateSpanText", MARGIN_H + 18f, y + 48f, paints.headerSubtitle)
            canvas.drawText("Generated: $generatedTimestamp", MARGIN_H + 18f, y + 64f, paints.headerMeta)
            y += 90f

            // 2. Executive Summary Cards
            val cardGap = 10f
            val cardWidth = (CONTENT_WIDTH - (cardGap * 2f)) / 3f
            val cardTextMaxWidth = cardWidth - 20f
            val totalValue = fitTextToWidth(
                "$currencySymbol${String.format(Locale.US, "%,.2f", summary.totalAmount)}",
                cardTextMaxWidth
            ) { paints.cardValue.measureText(it) }
            val countValue = fitTextToWidth(
                "${summary.transactionCount} (Avg $currencySymbol${String.format(Locale.US, "%,.0f", summary.averageAmount)})",
                cardTextMaxWidth
            ) { paints.cardValue.measureText(it) }
            val topCategoryLabel = fitTextToWidth(
                summary.categories.firstOrNull()?.categoryName ?: "—",
                cardTextMaxWidth
            ) { paints.cardValue.measureText(it) }

            drawSummaryCard(
                canvas = canvas,
                left = MARGIN_H,
                top = y,
                width = cardWidth,
                height = 52f,
                label = "TOTAL SPENT",
                value = totalValue,
                valueColor = COLOR_BLUE,
                paints = paints
            )
            drawSummaryCard(
                canvas = canvas,
                left = MARGIN_H + cardWidth + cardGap,
                top = y,
                width = cardWidth,
                height = 52f,
                label = "TRANSACTIONS",
                value = countValue,
                valueColor = COLOR_TEXT_PRIMARY,
                paints = paints
            )
            drawSummaryCard(
                canvas = canvas,
                left = MARGIN_H + (cardWidth + cardGap) * 2f,
                top = y,
                width = cardWidth,
                height = 52f,
                label = "TOP CATEGORY",
                value = topCategoryLabel,
                valueColor = COLOR_GREEN,
                paints = paints
            )
            y += 68f

            // 3. Category Summary Table
            canvas.drawText("Category Breakdown", MARGIN_H, y + 12f, paints.sectionTitle)
            y += 20f
            drawCategoryTableHeader(canvas, y, paints)
            y += 24f

            summary.categories.forEachIndexed { index, cat ->
                ensureSpace(22f) {
                    canvas.drawText("Category Breakdown (continued)", MARGIN_H, y + 12f, paints.sectionTitle)
                    y += 20f
                    drawCategoryTableHeader(canvas, y, paints)
                    y += 24f
                }

                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 20f, paints.altRowFill)
                }

                val catName = fitTextToWidth(cat.categoryName, 210f) { paints.body.measureText(it) }
                canvas.drawText(catName, MARGIN_H + 8f, y + 14f, paints.body)
                canvas.drawText("${cat.count}", MARGIN_H + 280f, y + 14f, paints.bodyRight)
                canvas.drawText(String.format(Locale.US, "%.1f%%", cat.percentage), MARGIN_H + 360f, y + 14f, paints.bodyRight)
                canvas.drawText(
                    "$currencySymbol${String.format(Locale.US, "%,.2f", cat.totalAmount)}",
                    PAGE_WIDTH - MARGIN_H - 8f,
                    y + 14f,
                    paints.bodyBoldRight
                )
                canvas.drawLine(MARGIN_H, y + 20f, PAGE_WIDTH - MARGIN_H, y + 20f, paints.divider)
                y += 20f
            }

            // Category Grand Total Row
            ensureSpace(24f)
            canvas.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 22f, paints.totalRowFill)
            canvas.drawText("Grand Total", MARGIN_H + 8f, y + 15f, paints.bodyBold)
            canvas.drawText("${summary.transactionCount}", MARGIN_H + 280f, y + 15f, paints.bodyBoldRight)
            canvas.drawText(if (summary.transactionCount > 0) "100.0%" else "0.0%", MARGIN_H + 360f, y + 15f, paints.bodyBoldRight)
            canvas.drawText(
                "$currencySymbol${String.format(Locale.US, "%,.2f", summary.totalAmount)}",
                PAGE_WIDTH - MARGIN_H - 8f,
                y + 15f,
                paints.bodyBoldRight
            )
            y += 38f

            // 4. Transactions Table
            ensureSpace(54f)
            canvas.drawText("All Transactions (${sortedExpenses.size})", MARGIN_H, y + 12f, paints.sectionTitle)
            y += 20f
            drawTransactionTableHeader(canvas, y, paints)
            y += 24f

            sortedExpenses.forEachIndexed { index, item ->
                val cleanNotes = item.expense.notes?.trim()?.replace('\n', ' ')?.takeIf { it.isNotEmpty() }
                val rowHeight = if (cleanNotes != null) 31f else 20f

                ensureSpace(rowHeight) {
                    canvas.drawText("All Transactions (continued)", MARGIN_H, y + 12f, paints.sectionTitle)
                    y += 20f
                    drawTransactionTableHeader(canvas, y, paints)
                    y += 24f
                }

                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + rowHeight, paints.altRowFill)
                }

                val dateStr = dateFormat.format(Date(item.expense.timestamp))
                val timeStr = timeFormat.format(Date(item.expense.timestamp))
                val titleStr = fitTextToWidth(item.expense.title, 146f) { paints.body.measureText(it) }
                val catStr = fitTextToWidth(item.categoryName, 76f) { paints.bodyMuted.measureText(it) }
                val methodStr = fitTextToWidth(item.expense.paymentMethod, 48f) { paints.bodyMuted.measureText(it) }
                val amountStr = "$currencySymbol${String.format(Locale.US, "%,.2f", item.expense.amount)}"

                canvas.drawText(dateStr, MARGIN_H + 6f, y + 14f, paints.bodySmall)
                canvas.drawText(timeStr, MARGIN_H + 68f, y + 14f, paints.bodyMuted)
                canvas.drawText(titleStr, MARGIN_H + 108f, y + 14f, paints.body)
                canvas.drawText(catStr, MARGIN_H + 262f, y + 14f, paints.bodyMuted)
                canvas.drawText(methodStr, MARGIN_H + 346f, y + 14f, paints.bodyMuted)
                canvas.drawText(amountStr, PAGE_WIDTH - MARGIN_H - 6f, y + 14f, paints.bodyBoldRight)

                if (cleanNotes != null) {
                    val fittedNotes = fitTextToWidth("Note: $cleanNotes", CONTENT_WIDTH - 120f) { paints.notes.measureText(it) }
                    canvas.drawText(fittedNotes, MARGIN_H + 108f, y + 26f, paints.notes)
                }

                canvas.drawLine(MARGIN_H, y + rowHeight, PAGE_WIDTH - MARGIN_H, y + rowHeight, paints.divider)
                y += rowHeight
            }

            drawFooter(canvas, pageNumber, paints)
            document.finishPage(page)

            document.writeTo(outputStream)
            outputStream.flush()
        } finally {
            document.close()
        }
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        valueColor: Int,
        paints: PdfPaints
    ) {
        val rect = RectF(left, top, left + width, top + height)
        canvas.drawRoundRect(rect, 8f, 8f, paints.cardBg)
        canvas.drawRoundRect(rect, 8f, 8f, paints.cardStroke)
        canvas.drawText(label, left + 10f, top + 18f, paints.cardLabel)
        paints.cardValue.color = valueColor
        canvas.drawText(value, left + 10f, top + 38f, paints.cardValue)
    }

    private fun drawCategoryTableHeader(canvas: Canvas, top: Float, paints: PdfPaints) {
        canvas.drawRect(MARGIN_H, top, PAGE_WIDTH - MARGIN_H, top + 22f, paints.tableHeaderBg)
        canvas.drawText("Category", MARGIN_H + 8f, top + 15f, paints.tableHeaderText)
        canvas.drawText("Transactions", MARGIN_H + 280f, top + 15f, paints.tableHeaderTextRight)
        canvas.drawText("% Share", MARGIN_H + 360f, top + 15f, paints.tableHeaderTextRight)
        canvas.drawText("Total Amount", PAGE_WIDTH - MARGIN_H - 8f, top + 15f, paints.tableHeaderTextRight)
    }

    private fun drawTransactionTableHeader(canvas: Canvas, top: Float, paints: PdfPaints) {
        canvas.drawRect(MARGIN_H, top, PAGE_WIDTH - MARGIN_H, top + 22f, paints.tableHeaderBg)
        canvas.drawText("Date", MARGIN_H + 6f, top + 15f, paints.tableHeaderText)
        canvas.drawText("Time", MARGIN_H + 68f, top + 15f, paints.tableHeaderText)
        canvas.drawText("Title", MARGIN_H + 108f, top + 15f, paints.tableHeaderText)
        canvas.drawText("Category", MARGIN_H + 262f, top + 15f, paints.tableHeaderText)
        canvas.drawText("Method", MARGIN_H + 346f, top + 15f, paints.tableHeaderText)
        canvas.drawText("Amount", PAGE_WIDTH - MARGIN_H - 6f, top + 15f, paints.tableHeaderTextRight)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, paints: PdfPaints) {
        val footerY = PAGE_HEIGHT - 22f
        canvas.drawLine(MARGIN_H, footerY - 10f, PAGE_WIDTH - MARGIN_H, footerY - 10f, paints.divider)
        canvas.drawText("SpendWise • Privacy-First Offline Expense Manager", MARGIN_H, footerY, paints.footer)
        canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN_H, footerY, paints.footerRight)
    }

    private class PdfPaints {
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            style = Paint.Style.FILL
        }
        val accentFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BLUE
            style = Paint.Style.FILL
        }
        val headerTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSubtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD8EAFF.toInt()
            textSize = 10f
        }
        val headerMeta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFAAB4D8.toInt()
            textSize = 8.5f
        }
        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SURFACE_TINT
            style = Paint.Style.FILL
        }
        val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CARD_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val cardLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cardValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BLUE
            style = Paint.Style.FILL
        }
        val tableHeaderText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableHeaderTextRight = Paint(tableHeaderText).apply {
            textAlign = Paint.Align.RIGHT
        }
        val altRowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROW_ALT
            style = Paint.Style.FILL
        }
        val totalRowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE8F0F8.toInt()
            style = Paint.Style.FILL
        }
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER
            strokeWidth = 0.75f
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 9.5f
        }
        val bodySmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 9f
        }
        val bodyMuted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 9f
        }
        val bodyRight = Paint(body).apply {
            textAlign = Paint.Align.RIGHT
        }
        val bodyBold = Paint(body).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyBoldRight = Paint(bodyBold).apply {
            textAlign = Paint.Align.RIGHT
        }
        val notes = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val continuationTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val continuationRight = Paint(continuationTitle).apply {
            textAlign = Paint.Align.RIGHT
        }
        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 8f
        }
        val footerRight = Paint(footer).apply {
            textAlign = Paint.Align.RIGHT
        }
    }
}
