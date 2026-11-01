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

    @Test
    fun testExportCsvAllHourColumnsAreParseableWithDoubleParseDouble() {
        val tempDir = java.nio.file.Files.createTempDirectory("karyar_csv_hours_test").toFile()
        val testContext = object : android.content.ContextWrapper(null) {
            override fun getCacheDir(): java.io.File = tempDir
        }

        val worker = com.karyar.app.data.local.entity.WorkerEntity(
            id = 77L,
            folderId = 1L,
            name = "مهدی کریمی",
            role = "جوشکار",
            baseDailyWage = 1_000_000L
        )
        val perf = com.karyar.app.domain.model.WorkerPerformance(
            worker = worker,
            totalShifts = 2,
            regularHours = 8.0,
            hourlyHours = 6.5,
            overtimeHours = 2.25,
            baseWageTotal = 1_000_000L,
            hourlyPayTotal = 650_000L,
            overtimePayTotal = 337_500L,
            netPayout = 1_987_500L
        )
        val att1 = com.karyar.app.data.local.entity.AttendanceEntity(
            id = 1L,
            workerId = 77L,
            date = "1405/01/15",
            epochDay = 20000L,
            status = com.karyar.app.data.local.entity.AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            hourlyHours = 0.0,
            overtimeHours = 1.5,
            dailyWage = 1_000_000L
        )
        val att2 = com.karyar.app.data.local.entity.AttendanceEntity(
            id = 2L,
            workerId = 77L,
            date = "1405/01/16",
            epochDay = 20001L,
            status = com.karyar.app.data.local.entity.AttendanceStatus.HOURLY,
            regularHours = 0.0,
            hourlyHours = 6.5,
            overtimeHours = 0.75,
            hourlyWage = 100_000L,
            hourlyWageRate = 100_000L
        )
        val analytics = com.karyar.app.domain.model.DashboardAnalytics(
            totalWorkersCount = 1,
            activeWorkersCount = 1,
            todayAttendanceCount = 1,
            totalWorkDaysCount = 2,
            totalPersonDays = 2,
            totalWorkHours = 16.75,
            totalOvertimeHours = 2.25,
            totalWagesPaid = 1_000_000L,
            totalOvertimePaid = 337_500L,
            totalHourlyPaid = 650_000L,
            grandTotalProjectCost = 1_987_500L
        )

        val csvFile = ExcelExportUtil.exportToExcelCsv(
            context = testContext,
            workers = listOf(worker),
            attendanceList = listOf(att1, att2),
            performances = listOf(perf),
            projectName = "پروژه تست ساعت عددی",
            expenses = emptyList(),
            analytics = analytics
        )

        val rawText = csvFile.readText(Charsets.UTF_8).removePrefix("\uFEFF")
        val rows = parseCsvRows(rawText)

        // Row 1 (index 0) is the horizontal header row; Row 2 (index 1) is the worker's row
        val headerRow = rows[0]
        val perfDataRow = rows[1]

        val regularHoursIdx = headerRow.indexOf("ساعات عادی (ساعت)")
        val hourlyHoursIdx = headerRow.indexOf("ساعت کار ساعتی (ساعت)")
        val overtimeHoursIdx = headerRow.indexOf("اضافه کاری (ساعت)")

        org.junit.Assert.assertTrue("Regular hours column must exist in Row 1", regularHoursIdx >= 0)
        org.junit.Assert.assertTrue("Hourly hours column must exist in Row 1", hourlyHoursIdx >= 0)
        org.junit.Assert.assertTrue("Overtime hours column must exist in Row 1", overtimeHoursIdx >= 0)

        assertEquals(8.0, java.lang.Double.parseDouble(perfDataRow[regularHoursIdx]), 0.001)
        assertEquals(6.5, java.lang.Double.parseDouble(perfDataRow[hourlyHoursIdx]), 0.001)
        assertEquals(2.25, java.lang.Double.parseDouble(perfDataRow[overtimeHoursIdx]), 0.001)

        tempDir.deleteRecursively()
    }

    @Test
    fun testHorizontalRtlExcelXlsxAndCsvWorkerPerRowVariablesInColumns() {
        val tempDir = java.nio.file.Files.createTempDirectory("karyar_xlsx_rtl_test").toFile()
        val testContext = object : android.content.ContextWrapper(null) {
            override fun getCacheDir(): java.io.File = tempDir
        }

        val worker1 = com.karyar.app.data.local.entity.WorkerEntity(
            id = 1L,
            folderId = 1L,
            name = "علی رضایی",
            role = "بنا",
            phone = "09121112233",
            nationalId = "0012345678",
            baseDailyWage = 1_200_000L,
            baseHourlyWage = 150_000L,
            overtimeRate = 200_000L,
            notes = "استادکار دیوارچینی"
        )
        val worker2 = com.karyar.app.data.local.entity.WorkerEntity(
            id = 2L,
            folderId = 1L,
            name = "حسن محمدی",
            role = "کارگر ساده",
            phone = "09351112233",
            nationalId = "0087654321",
            baseDailyWage = 800_000L,
            overtimeRate = 120_000L
        )
        val perf1 = com.karyar.app.domain.model.WorkerPerformance(
            worker = worker1,
            totalShifts = 5,
            fullDaysCount = 4,
            halfDaysCount = 1,
            hourlyDaysCount = 0,
            absentDaysCount = 1,
            regularHours = 36.0,
            hourlyHours = 0.0,
            overtimeHours = 3.5,
            baseWageTotal = 5_400_000L,
            hourlyPayTotal = 0L,
            overtimePayTotal = 700_000L,
            totalAllowances = 300_000L,
            transitAllowanceTotal = 200_000L,
            foodAllowanceTotal = 100_000L,
            netPayout = 6_400_000L
        )
        val perf2 = com.karyar.app.domain.model.WorkerPerformance(
            worker = worker2,
            totalShifts = 3,
            fullDaysCount = 3,
            regularHours = 24.0,
            overtimeHours = 1.0,
            baseWageTotal = 2_400_000L,
            overtimePayTotal = 120_000L,
            netPayout = 2_520_000L
        )

        // 1. Check CSV structure: Row 1 = headers, Row 2 = worker1, Row 3 = worker2
        val csvFile = ExcelExportUtil.exportToExcelCsv(
            context = testContext,
            workers = listOf(worker1, worker2),
            attendanceList = emptyList(),
            performances = listOf(perf1, perf2),
            projectName = "پروژه افقی"
        )
        val rows = parseCsvRows(csvFile.readText(Charsets.UTF_8).removePrefix("\uFEFF"))
        assertEquals("Should have exactly 1 header row + 2 worker rows", 3, rows.size)
        assertEquals(ExcelExportUtil.WORKER_EXPORT_HEADERS, rows[0])
        assertEquals("علی رضایی", rows[1][0])
        assertEquals("بنا", rows[1][1])
        assertEquals("5", rows[1][2]) // C2: تعداد روزهای کار
        assertEquals("3.5", rows[1][10]) // K2: اضافه کاری (ساعت)
        assertEquals("حسن محمدی", rows[2][0])
        assertEquals("3", rows[2][2]) // C3: تعداد روزهای کار

        // 2. Check XLSX structure: valid ZIP, rightToLeft="1", A1..AK1 headers, A2/C2/K2 values
        val xlsxFile = ExcelExportUtil.exportToExcelXlsx(
            context = testContext,
            workers = listOf(worker1, worker2),
            attendanceList = emptyList(),
            performances = listOf(perf1, perf2),
            projectName = "پروژه افقی"
        )
        org.junit.Assert.assertTrue(xlsxFile.exists())
        java.util.zip.ZipFile(xlsxFile).use { zip ->
            val sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml")
            org.junit.Assert.assertNotNull("sheet1.xml must exist in .xlsx", sheetEntry)
            val sheetXml = zip.getInputStream(sheetEntry!!).bufferedReader(Charsets.UTF_8).readText()
            org.junit.Assert.assertTrue(
                "Sheet must be Right-to-Left (rightToLeft=\"1\")",
                sheetXml.contains("rightToLeft=\"1\"")
            )
            org.junit.Assert.assertTrue(
                "A1 must be نام کارگر header",
                sheetXml.contains("<c r=\"A1\" s=\"1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">نام کارگر</t></is></c>")
            )
            org.junit.Assert.assertTrue(
                "C1 must be تعداد روزهای کار header",
                sheetXml.contains("<c r=\"C1\" s=\"1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">تعداد روزهای کار</t></is></c>")
            )
            org.junit.Assert.assertTrue(
                "A2 must be first worker name",
                sheetXml.contains("<c r=\"A2\" s=\"2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">علی رضایی</t></is></c>")
            )
            org.junit.Assert.assertTrue(
                "C2 must be first worker total work days (5)",
                sheetXml.contains("<c r=\"C2\" s=\"3\" t=\"n\"><v>5</v></c>")
            )
            org.junit.Assert.assertTrue(
                "K2 must be first worker overtime hours (3.5)",
                sheetXml.contains("<c r=\"K2\" s=\"3\" t=\"n\"><v>3.5</v></c>")
            )
            org.junit.Assert.assertTrue(
                "A3 must be second worker name",
                sheetXml.contains("<c r=\"A3\" s=\"2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">حسن محمدی</t></is></c>")
            )
        }

        tempDir.deleteRecursively()
    }

    @Test
    fun testSaveExcelAndPdfToInternalStorage() {
        val tempDir = java.nio.file.Files.createTempDirectory("karyar_internal_save_test").toFile()
        val testContext = object : android.content.ContextWrapper(null) {
            override fun getCacheDir(): java.io.File = tempDir
            override fun getFilesDir(): java.io.File = tempDir
            override fun getExternalFilesDir(type: String?): java.io.File = java.io.File(tempDir, type ?: "external")
            override fun getContentResolver(): android.content.ContentResolver? = null
        }

        val worker = com.karyar.app.data.local.entity.WorkerEntity(
            id = 1L,
            folderId = 1L,
            name = "رضا صادقی",
            role = "آرماتوربند",
            baseDailyWage = 1_500_000L
        )
        val perf = com.karyar.app.domain.model.WorkerPerformance(
            worker = worker,
            totalShifts = 4,
            fullDaysCount = 4,
            regularHours = 32.0,
            overtimeHours = 0.0,
            baseWageTotal = 6_000_000L,
            overtimePayTotal = 0L,
            netPayout = 6_000_000L
        )

        val xlsxSource = ExcelExportUtil.exportToExcelXlsx(
            context = testContext,
            workers = listOf(worker),
            attendanceList = emptyList(),
            performances = listOf(perf),
            projectName = "کارگاه برج میلاد"
        )
        val excelFileName = ExcelExportUtil.buildExportFileName("گزارش_اکسل", "کارگاه برج میلاد", "xlsx")
        val savedExcel = ExcelExportUtil.saveFileToInternalStorage(
            context = testContext,
            sourceFile = xlsxSource,
            desiredFileName = excelFileName,
            mimeType = ExcelExportUtil.MIME_TYPE_XLSX
        )

        org.junit.Assert.assertTrue(savedExcel.fileName.endsWith(".xlsx"))
        org.junit.Assert.assertTrue(savedExcel.displayPath.startsWith("Download/KarYar/"))
        org.junit.Assert.assertNotNull(savedExcel.localFile)
        org.junit.Assert.assertTrue(savedExcel.localFile!!.exists() && savedExcel.localFile!!.length() > 0L)

        // Simulate PDF file and verify direct internal storage saving
        val dummyPdfFile = java.io.File(tempDir, "dummy_report.pdf").apply {
            writeBytes("%PDF-1.4\n%KarYar Test PDF Content\n%%EOF".toByteArray(Charsets.UTF_8))
        }
        val pdfFileName = ExcelExportUtil.buildExportFileName("گزارش_PDF", "کارگاه برج میلاد", "pdf")
        val savedPdf = ExcelExportUtil.saveFileToInternalStorage(
            context = testContext,
            sourceFile = dummyPdfFile,
            desiredFileName = pdfFileName,
            mimeType = ExcelExportUtil.MIME_TYPE_PDF
        )

        org.junit.Assert.assertTrue(savedPdf.fileName.endsWith(".pdf"))
        org.junit.Assert.assertTrue(savedPdf.displayPath.startsWith("Download/KarYar/"))
        org.junit.Assert.assertNotNull(savedPdf.localFile)
        org.junit.Assert.assertTrue(savedPdf.localFile!!.exists() && savedPdf.localFile!!.length() > 0L)

        tempDir.deleteRecursively()
    }
}

