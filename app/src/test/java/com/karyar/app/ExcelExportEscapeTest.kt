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
}
