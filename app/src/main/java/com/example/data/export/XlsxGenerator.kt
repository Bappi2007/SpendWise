package com.example.data.export

import com.example.data.model.ExpenseWithCategory
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Lightweight, zero-dependency OpenXML SpreadsheetML (.xlsx) generator.
 * Produces native multi-sheet Excel workbooks with formatted headers and column widths.
 */
object XlsxGenerator {

    fun generateWorkbook(
        expenses: List<ExpenseWithCategory>,
        currencySymbol: String,
        outputStream: OutputStream
    ) {
        val zip = ZipOutputStream(outputStream)

        // 1. [Content_Types].xml
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        zip.write(getContentTypesXml().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 2. _rels/.rels
        zip.putNextEntry(ZipEntry("_rels/.rels"))
        zip.write(getRootRelsXml().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zip.write(getWorkbookRelsXml().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 4. xl/styles.xml
        zip.putNextEntry(ZipEntry("xl/styles.xml"))
        zip.write(getStylesXml().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 5. xl/workbook.xml
        zip.putNextEntry(ZipEntry("xl/workbook.xml"))
        zip.write(getWorkbookXml().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 6. xl/worksheets/sheet1.xml (All Expenses)
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        zip.write(getSheet1Xml(expenses).toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 7. xl/worksheets/sheet2.xml (Category Summary)
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
        zip.write(getSheet2Xml(expenses, currencySymbol).toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        zip.finish()
        zip.flush()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun getRootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun getWorkbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun getWorkbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="All Expenses" sheetId="1" r:id="rId1"/>
    <sheet name="Category Summary" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""

    private fun getStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="#,##0.00"/>
  </numFmts>
  <fonts count="2">
    <font><sz val="11"/><color theme="1"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0F5132"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
  </cellXfs>
</styleSheet>"""

    private fun getSheet1Xml(expenses: List<ExpenseWithCategory>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="14" customWidth="1"/>
    <col min="2" max="2" width="10" customWidth="1"/>
    <col min="3" max="3" width="28" customWidth="1"/>
    <col min="4" max="4" width="18" customWidth="1"/>
    <col min="5" max="5" width="16" customWidth="1"/>
    <col min="6" max="6" width="16" customWidth="1"/>
    <col min="7" max="7" width="30" customWidth="1"/>
  </cols>
  <sheetData>
""")

        // Header Row (Row 1, style 1 = bold white text with green fill)
        sb.append("""    <row r="1">
      <c r="A1" t="inlineStr" s="1"><is><t>Date</t></is></c>
      <c r="B1" t="inlineStr" s="1"><is><t>Time</t></is></c>
      <c r="C1" t="inlineStr" s="1"><is><t>Title</t></is></c>
      <c r="D1" t="inlineStr" s="1"><is><t>Category</t></is></c>
      <c r="E1" t="inlineStr" s="1"><is><t>Amount</t></is></c>
      <c r="F1" t="inlineStr" s="1"><is><t>Payment Method</t></is></c>
      <c r="G1" t="inlineStr" s="1"><is><t>Remarks</t></is></c>
    </row>
""")

        var rowIndex = 2
        for (item in expenses) {
            val dateStr = dateFormat.format(Date(item.expense.timestamp))
            val timeStr = timeFormat.format(Date(item.expense.timestamp))
            val title = escapeXml(item.expense.title)
            val category = escapeXml(item.categoryName)
            val amount = item.expense.amount
            val payment = escapeXml(item.expense.paymentMethod)
            val remarks = escapeXml(item.expense.notes ?: "")

            sb.append("""    <row r="$rowIndex">
      <c r="A$rowIndex" t="inlineStr"><is><t>$dateStr</t></is></c>
      <c r="B$rowIndex" t="inlineStr"><is><t>$timeStr</t></is></c>
      <c r="C$rowIndex" t="inlineStr"><is><t>$title</t></is></c>
      <c r="D$rowIndex" t="inlineStr"><is><t>$category</t></is></c>
      <c r="E$rowIndex" s="2"><v>$amount</v></c>
      <c r="F$rowIndex" t="inlineStr"><is><t>$payment</t></is></c>
      <c r="G$rowIndex" t="inlineStr"><is><t>$remarks</t></is></c>
    </row>
""")
            rowIndex++
        }

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun getSheet2Xml(expenses: List<ExpenseWithCategory>, currencySymbol: String): String {
        val totalSpent = expenses.sumOf { it.expense.amount }
        val categoryMap = expenses.groupBy { it.categoryName }

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="22" customWidth="1"/>
    <col min="2" max="2" width="16" customWidth="1"/>
    <col min="3" max="3" width="18" customWidth="1"/>
    <col min="4" max="4" width="16" customWidth="1"/>
  </cols>
  <sheetData>
""")

        // Header
        sb.append("""    <row r="1">
      <c r="A1" t="inlineStr" s="1"><is><t>Category</t></is></c>
      <c r="B1" t="inlineStr" s="1"><is><t>Total Amount ($currencySymbol)</t></is></c>
      <c r="C1" t="inlineStr" s="1"><is><t>Transactions</t></is></c>
      <c r="D1" t="inlineStr" s="1"><is><t>% of Total</t></is></c>
    </row>
""")

        var rowIndex = 2
        for ((catName, items) in categoryMap.entries.sortedByDescending { it.value.sumOf { e -> e.expense.amount } }) {
            val catTotal = items.sumOf { it.expense.amount }
            val count = items.size
            val pct = if (totalSpent > 0) String.format(Locale.US, "%.1f%%", (catTotal / totalSpent) * 100) else "0%"

            sb.append("""    <row r="$rowIndex">
      <c r="A$rowIndex" t="inlineStr"><is><t>${escapeXml(catName)}</t></is></c>
      <c r="B$rowIndex" s="2"><v>$catTotal</v></c>
      <c r="C$rowIndex"><v>$count</v></c>
      <c r="D$rowIndex" t="inlineStr"><is><t>$pct</t></is></c>
    </row>
""")
            rowIndex++
        }

        // Summary row
        sb.append("""    <row r="$rowIndex">
      <c r="A$rowIndex" t="inlineStr" s="1"><is><t>Grand Total</t></is></c>
      <c r="B$rowIndex" s="1"><v>$totalSpent</v></c>
      <c r="C$rowIndex" s="1"><v>${expenses.size}</v></c>
      <c r="D$rowIndex" t="inlineStr" s="1"><is><t>100%</t></is></c>
    </row>
""")

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }
}
