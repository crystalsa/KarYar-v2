package com.karyar.app

import com.karyar.app.util.ExcelExportUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class ExcelExportEscapeTest {

    @Test
    fun testEscapeCsvQuotes() {
        // Double quotes must be doubled per RFC 4180
        val input = "علی \"استاد\" رضایی"
        val expected = "\"علی \"\"استاد\"\" رضایی\""
        assertEquals(expected, ExcelExportUtil.escapeCsv(input))
    }

    @Test
    fun testEscapeCsvComma() {
        // Commas must be enclosed in quotes
        val input = "بنا, گچ‌کار"
        val expected = "\"بنا, گچ‌کار\""
        assertEquals(expected, ExcelExportUtil.escapeCsv(input))
    }

    @Test
    fun testEscapeCsvNewline() {
        // Newlines must be enclosed in quotes
        val input = "سطر اول\nسطر دوم"
        val expected = "\"سطر اول\nسطر دوم\""
        assertEquals(expected, ExcelExportUtil.escapeCsv(input))
    }

    @Test
    fun testEscapeCsvFormulaInjection_Sum() {
        // Formula injection starting with '=' must be escaped with prepended single quote
        val input = "=SUM(A1:B10)"
        val expected = "\"'=SUM(A1:B10)\""
        assertEquals(expected, ExcelExportUtil.escapeCsv(input))
    }

    @Test
    fun testEscapeCsvNegativeNumberPreserved() {
        // Pure negative numbers like -5 and -150000 must NOT be treated as formulas
        assertEquals("\"-5\"", ExcelExportUtil.escapeCsv("-5"))
        assertEquals("\"-150000\"", ExcelExportUtil.escapeCsv("-150000"))
        assertEquals("\"-3.5\"", ExcelExportUtil.escapeCsv("-3.5"))
    }

    @Test
    fun testEscapeCsvDashPlaceholderPreserved() {
        // Dash placeholder '-' must remain '-' and NOT be prepended with single quote
        assertEquals("\"-\"", ExcelExportUtil.escapeCsv("-"))
    }

    @Test
    fun testEscapeCsvOtherFormulaInjections() {
        // @, +, \t, \r formulas must be escaped
        assertEquals("\"'@cmd\"", ExcelExportUtil.escapeCsv("@cmd"))
        assertEquals("\"'+cmd()\"", ExcelExportUtil.escapeCsv("+cmd()"))
        assertEquals("\"'\tcmd\"", ExcelExportUtil.escapeCsv("\tcmd"))
        assertEquals("\"'\rcmd\"", ExcelExportUtil.escapeCsv("\rcmd"))
    }

    @Test
    fun testEscapeCsvPreservesWhitespaceWithoutTrim() {
        // Trim must not be called, user spaces must be preserved
        val input = "  متن با فاصله  "
        val expected = "\"  متن با فاصله  \""
        assertEquals(expected, ExcelExportUtil.escapeCsv(input))
    }

    @Test
    fun testExportCsvIncludesSeparateOtherAllowanceAndDeductionColumns() {
        val tempDir = java.nio.file.Files.createTempDirectory("karyar_csv_test").toFile()
        val testContext = object : android.content.ContextWrapper(null) {
            override fun getCacheDir(): java.io.File = tempDir
        }

        val worker = com.karyar.app.data.local.entity.WorkerEntity(
            id = 55L,
            folderId = 1L,
            name = "رضا صادقی",
            role = "استادکار",
            baseDailyWage = 1_000_000L
        )
        val perf = com.karyar.app.domain.model.WorkerPerformance(
            worker = worker,
            totalShifts = 1,
            regularHours = 8.0,
            overtimeHours = 0.0,
            baseWageTotal = 1_000_000L,
            overtimePayTotal = 0L,
            totalAllowances = 170_000L,
            transitAllowanceTotal = 50_000L,
            otherAllowanceTotal = 120_000L,
            totalDeductions = 90_000L,
            otherDeductionTotal = 90_000L,
            netPayout = 1_080_000L
        )

        val csvFile = ExcelExportUtil.exportToExcelCsv(
            context = testContext,
            workers = listOf(worker),
            attendanceList = emptyList(),
            performances = listOf(perf),
            projectName = "کارگاه تست سایر"
        )

        val csvText = csvFile.readText(Charsets.UTF_8)
        org.junit.Assert.assertTrue(csvText.contains("کمک‌هزینه‌ها (تومان),سایر مزایا (تومان),کسورات (تومان),سایر کسورات (تومان)"))
        org.junit.Assert.assertTrue(csvText.contains(",170000,120000,90000,90000,"))
        tempDir.deleteRecursively()
    }
}
