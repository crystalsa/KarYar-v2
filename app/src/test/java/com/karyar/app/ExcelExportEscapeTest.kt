package com.karyar.app

import com.karyar.app.util.ExcelExportUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
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
        org.junit.Assert.assertTrue(
            csvText.contains("\"کمک‌هزینه‌ها (تومان)\",\"سایر مزایا (تومان)\",\"کسورات (تومان)\",\"سایر کسورات (تومان)\"")
        )
        org.junit.Assert.assertTrue(
            csvText.contains(",\"170000\",\"120000\",\"90000\",\"90000\",")
        )
        tempDir.deleteRecursively()
    }

    @Test
    fun testExportCsvNoCellStartsWithFormulaCharExceptPureNumbersAndNegativeNumbersPreserved() {
        val tempDir = java.nio.file.Files.createTempDirectory("karyar_csv_formula_test").toFile()
        val testContext = object : android.content.ContextWrapper(null) {
            override fun getCacheDir(): java.io.File = tempDir
        }

        val worker = com.karyar.app.data.local.entity.WorkerEntity(
            id = 10L,
            folderId = 1L,
            name = "=HYPERLINK(\"http://evil\")",
            role = "+استادکار",
            baseDailyWage = 1_000_000L
        )
        val perf = com.karyar.app.domain.model.WorkerPerformance(
            worker = worker,
            totalShifts = 1,
            regularHours = 8.0,
            overtimeHours = 2.0,
            baseWageTotal = 1_000_000L,
            overtimePayTotal = 200_000L,
            totalAllowances = 50_000L,
            totalDeductions = 150_000L,
            netPayout = -150000L,
            debtAmount = 0L
        )
        val attendance = com.karyar.app.data.local.entity.AttendanceEntity(
            id = 1L,
            workerId = 10L,
            date = "1403/12/30",
            epochDay = 19000L,
            status = com.karyar.app.data.local.entity.AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 2.0,
            dailyWage = 1_000_000L,
            workplaceName = "@کارگاه مرکزی",
            employerName = "-کارفرما",
            foremanName = "=سرکارگر",
            notes = "-150000"
        )
        val expense = com.karyar.app.data.local.entity.ExpenseEntity(
            id = 1L,
            date = "1403/12/30",
            epochDay = 19000L,
            title = "=SUM(A1:A10)",
            amount = -150000L,
            category = "+حمل و نقل",
            scope = "INDIVIDUAL",
            workerId = null,
            workerName = null,
            notes = "@یادداشت"
        )
        val analytics = com.karyar.app.domain.model.DashboardAnalytics(
            totalWorkersCount = 1,
            activeWorkersCount = 1,
            todayAttendanceCount = 1,
            totalWorkDaysCount = 1,
            totalPersonDays = 1,
            totalWorkHours = 8.0,
            totalOvertimeHours = 2.0,
            totalWagesPaid = 1_000_000L,
            totalOvertimePaid = 200_000L,
            totalHourlyPaid = 0L,
            grandTotalExpenses = -150000L,
            grandTotalProjectCost = 1_050_000L,
            totalWorkerDebt = 0L
        )

        val csvFile = ExcelExportUtil.exportToExcelCsv(
            context = testContext,
            workers = listOf(worker),
            attendanceList = listOf(attendance),
            performances = listOf(perf),
            projectName = "=پروژه تستی",
            expenses = listOf(expense),
            analytics = analytics
        )

        val rawText = csvFile.readText(Charsets.UTF_8).removePrefix("\uFEFF")
        val rows = parseCsvRows(rawText)
        val allCells = rows.flatten().filter { it.isNotEmpty() }

        val forbiddenPrefixes = setOf('=', '+', '-', '@')
        for (cell in allCells) {
            val isPureNumber = cell.trim().let {
                it.isNotEmpty() && it != "-" && it != "+" && it.toDoubleOrNull() != null
            }
            if (!isPureNumber) {
                val firstChar = cell.first()
                org.junit.Assert.assertFalse(
                    "Non-numeric cell '$cell' must not start with =, +, -, or @",
                    firstChar in forbiddenPrefixes
                )
            }
        }

        // Verify negative numeric values like -150000 remain intact (not prefixed with ')
        org.junit.Assert.assertTrue(
            "Expected pure negative number -150000 to remain intact in exported cells",
            allCells.contains("-150000")
        )
        org.junit.Assert.assertFalse(
            "Negative number -150000 must not be escaped with leading single quote",
            allCells.contains("'-150000")
        )

        tempDir.deleteRecursively()
    }

    /**
     * Minimal RFC 4180 CSV parser returning unquoted cell contents for verification.
     */
    private fun parseCsvRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < csv.length) {
            val c = csv[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length && csv[i + 1] == '"') {
                        currentCell.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentCell.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentCell.toString())
                        currentCell.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentCell.toString())
                        currentCell.clear()
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    '\r' -> { /* ignore CR */ }
                    else -> currentCell.append(c)
                }
            }
            i++
        }
        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentCell.toString())
            rows.add(currentRow)
        }
        return rows
    }
}
