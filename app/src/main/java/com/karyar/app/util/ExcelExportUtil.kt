package com.karyar.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.karyar.app.data.local.entity.AttendanceEntity
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.domain.model.DashboardAnalytics
import com.karyar.app.domain.model.WorkerPerformance
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs

data class SavedExportResult(
    val fileName: String,
    val displayPath: String,
    val uri: Uri? = null,
    val localFile: File? = null
)

object ExcelExportUtil {

    /**
     * سرستون‌های جدول افقی کارگران (ردیف اول اکسل: A1 تا AK1).
     * هر ستون بیانگر یکی از متغیرهای ثبت‌شده یا محاسبه‌شده برای کارگر است
     * و از ردیف دوم به بعد (A2, B2, ...) اطلاعات هر کارگر در یک ردیف قرار می‌گیرد.
     */
    val WORKER_EXPORT_HEADERS: List<String> = listOf(
        "نام کارگر",                        // A (0)
        "شغل / تخصص",                      // B (1)
        "تعداد روزهای کار",                 // C (2)
        "تعداد روز تمام‌روز",               // D (3)
        "تعداد روز نصف‌روز",                // E (4)
        "تعداد روز ساعتی",                  // F (5)
        "تعداد روز غیبت",                   // G (6)
        "تفکیک وضعیت کارکرد",               // H (7)
        "ساعات عادی (ساعت)",                // I (8)
        "ساعت کار ساعتی (ساعت)",            // J (9)
        "اضافه کاری (ساعت)",                // K (10)
        "نرخ روزمزد پایه (تومان)",          // L (11)
        "نرخ ساعتی پایه (تومان)",           // M (12)
        "نرخ اضافه کاری (تومان)",           // N (13)
        "دستمزد پایه (تومان)",              // O (14)
        "دستمزد ساعتی (تومان)",             // P (15)
        "اضافه کاری (تومان)",               // Q (16)
        "کمک‌هزینه ایاب و ذهاب (تومان)",    // R (17)
        "کمک‌هزینه غذا (تومان)",            // S (18)
        "کمک‌هزینه اسکان (تومان)",          // T (19)
        "کمک‌هزینه درمان (تومان)",          // U (20)
        "کمک‌هزینه‌ها (تومان)",             // V (21)
        "سایر مزایا (تومان)",               // W (22)
        "کسورات (تومان)",                   // X (23)
        "سایر کسورات (تومان)",              // Y (24)
        "کسورات ایاب و ذهاب (تومان)",       // Z (25)
        "کسورات غذا (تومان)",               // AA (26)
        "کسورات اسکان (تومان)",             // AB (27)
        "کسورات درمان (تومان)",             // AC (28)
        "سهم هزینه‌های گروهی (تومان)",      // AD (29)
        "خالص دریافتی نهایی (تومان)",       // AE (30)
        "وضعیت بدهی (تومان)",               // AF (31)
        "شماره تلفن",                       // AG (32)
        "کد ملی",                           // AH (33)
        "وضعیت فعالیت",                     // AI (34)
        "پروژه / کارگاه",                   // AJ (35)
        "توضیحات"                           // AK (36)
    )

    /**
     * فرمت عددی ساعت با ارقام لاتین، ممیز نقطه و حداکثر ۲ رقم اعشار با [Locale.US] ثابت،
     * تا در Excel / LibreOffice / Google Sheets به‌عنوان عدد قابل جمع (`SUM` / `AVERAGE`) شناخته شود.
     */
    fun formatCsvHours(hours: Double): String {
        if (!hours.isFinite() || abs(hours) < 1e-9) return "0"
        val formatted = String.format(Locale.US, "%.2f", hours)
        return formatted.trimEnd('0').trimEnd('.').ifEmpty { "0" }
    }

    /**
     * Helper to safely escape CSV fields according to RFC 4180 and prevent CSV injection.
     * Preserves exact user value (no trim).
     * Guards formula injection (=, +, -, @, \t, \r) while preserving purely numeric values (like -150000, -5) and "-" placeholder.
     */
    fun escapeCsv(value: Any?): String {
        if (value == null) return "\"\""
        val str = sanitizeFormulaInjection(value.toString())
        val escaped = str.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    internal fun sanitizeFormulaInjection(raw: String): String {
        val isNumeric = isNumericString(raw)
        val isPlaceholder = raw == "-"
        if (!isNumeric && !isPlaceholder && raw.isNotEmpty()) {
            val firstChar = raw[0]
            if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
                return "'$raw"
            }
        }
        return raw
    }

    private fun isNumericString(str: String): Boolean {
        if (str.isEmpty() || str == "-" || str == "+") return false
        val trimmed = str.trim()
        if (trimmed.isEmpty() || trimmed == "-" || trimmed == "+") return false
        val parsed = trimmed.toDoubleOrNull() ?: return false
        return parsed.isFinite()
    }

    /**
     * Formats a single CSV row by passing every cell through [escapeCsv] so no cell
     * (whether static title/header or dynamic value) can bypass escaping.
     */
    fun formatRow(vararg cells: Any?): String {
        return cells.joinToString(",") { escapeCsv(it) } + "\n"
    }

    private fun Appendable.writeRow(vararg cells: Any?) {
        append(formatRow(*cells))
    }

    private fun resolveWorkerPerformances(
        workers: List<WorkerEntity>,
        performances: List<WorkerPerformance>
    ): List<WorkerPerformance> {
        if (performances.isEmpty() && workers.isEmpty()) return emptyList()
        val existingIds = performances.map { it.worker.id }.toSet()
        val missingPerformances = workers
            .filter { it.id !in existingIds }
            .map { worker ->
                WorkerPerformance(
                    worker = worker,
                    totalShifts = 0,
                    regularHours = 0.0,
                    overtimeHours = 0.0,
                    baseWageTotal = 0L,
                    overtimePayTotal = 0L,
                    netPayout = 0L
                )
            }
        return performances + missingPerformances
    }

    private fun buildWorkerRowValues(
        p: WorkerPerformance,
        workerAttendances: List<AttendanceEntity>,
        projectName: String
    ): List<Any> {
        val worker = p.worker
        val hourlyRate = if (worker.hourlyWageRate > 0L) worker.hourlyWageRate else worker.baseHourlyWage
        val notesParts = mutableListOf<String>()
        if (worker.notes.isNotBlank()) {
            notesParts.add(worker.notes)
        }
        for (att in workerAttendances) {
            if (att.notes.isNotBlank() && att.notes !in notesParts) {
                notesParts.add(att.notes)
            }
        }
        val combinedNotes = if (notesParts.isEmpty()) "" else notesParts.joinToString(" | ")

        return listOf(
            worker.name,                                // A: نام کارگر
            worker.role,                                // B: شغل / تخصص
            p.totalShifts,                              // C: تعداد روزهای کار
            p.fullDaysCount,                            // D: تعداد روز تمام‌روز
            p.halfDaysCount,                            // E: تعداد روز نصف‌روز
            p.hourlyDaysCount,                          // F: تعداد روز ساعتی
            p.absentDaysCount,                          // G: تعداد روز غیبت
            p.formatWorkSummary(),                      // H: تفکیک وضعیت کارکرد
            formatCsvHours(p.regularHours),             // I: ساعات عادی (ساعت)
            formatCsvHours(p.hourlyHours),              // J: ساعت کار ساعتی (ساعت)
            formatCsvHours(p.overtimeHours),            // K: اضافه کاری (ساعت)
            worker.baseDailyWage,                       // L: نرخ روزمزد پایه (تومان)
            hourlyRate,                                 // M: نرخ ساعتی پایه (تومان)
            worker.overtimeRate,                        // N: نرخ اضافه کاری (تومان)
            p.baseWageTotal,                            // O: دستمزد پایه (تومان)
            p.hourlyPayTotal,                           // P: دستمزد ساعتی (تومان)
            p.overtimePayTotal,                         // Q: اضافه کاری (تومان)
            p.transitAllowanceTotal,                    // R: کمک‌هزینه ایاب و ذهاب (تومان)
            p.foodAllowanceTotal,                       // S: کمک‌هزینه غذا (تومان)
            p.accommodationAllowanceTotal,              // T: کمک‌هزینه اسکان (تومان)
            p.medicalAllowanceTotal,                    // U: کمک‌هزینه درمان (تومان)
            p.totalAllowances,                          // V: کمک‌هزینه‌ها (تومان)
            p.otherAllowanceTotal,                      // W: سایر مزایا (تومان)
            p.totalDeductions,                          // X: کسورات (تومان)
            p.otherDeductionTotal,                      // Y: سایر کسورات (تومان)
            p.transitDeductionTotal,                    // Z: کسورات ایاب و ذهاب (تومان)
            p.foodDeductionTotal,                       // AA: کسورات غذا (تومان)
            p.accommodationDeductionTotal,              // AB: کسورات اسکان (تومان)
            p.medicalDeductionTotal,                    // AC: کسورات درمان (تومان)
            p.groupExpenseShare,                        // AD: سهم هزینه‌های گروهی (تومان)
            p.netPayout,                                // AE: خالص دریافتی نهایی (تومان)
            p.debtAmount,                               // AF: وضعیت بدهی (تومان)
            worker.phone.ifBlank { "—" },               // AG: شماره تلفن
            worker.nationalId.ifBlank { "—" },          // AH: کد ملی
            if (worker.isActive) "فعال" else "غیرفعال", // AI: وضعیت فعالیت
            projectName,                                // AJ: پروژه / کارگاه
            combinedNotes                               // AK: توضیحات
        )
    }

    /**
     * خروجی جدول افقی کارگران در قالب CSV استاندارد (UTF-8 با BOM):
     * - ردیف اول (A1, B1, C1, ...) شامل نام متغیرهای هر ستون است.
     * - از ردیف دوم به بعد (A2, B2, C2, ...) هر ردیف اختصاص به یک کارگر دارد.
     */
    fun exportToExcelCsv(
        context: Context,
        workers: List<WorkerEntity>,
        attendanceList: List<AttendanceEntity>,
        performances: List<WorkerPerformance>,
        projectName: String = "پروژه کارگاهی",
        expenses: List<ExpenseEntity> = emptyList(),
        analytics: DashboardAnalytics? = null
    ): File {
        val fileName = "گزارش_کارگران_${System.currentTimeMillis()}.csv"
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, fileName)

        val resolvedPerformances = resolveWorkerPerformances(workers, performances)
        val attendanceByWorker = attendanceList.groupBy { it.workerId }

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM (0xEF, 0xBB, 0xBF) so Excel recognizes Persian characters
            fos.write(0xEF)
            fos.write(0xBB)
            fos.write(0xBF)

            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Row 1: Column Headers (every variable registered/calculated for a worker)
                writer.writeRow(*WORKER_EXPORT_HEADERS.toTypedArray())

                // Row 2+: One row per worker
                for (p in resolvedPerformances) {
                    val workerAtts = attendanceByWorker[p.worker.id].orEmpty()
                    val rowValues = buildWorkerRowValues(p, workerAtts, projectName)
                    writer.writeRow(*rowValues.toTypedArray())
                }

                writer.flush()
            }
        }
        return file
    }

    /**
     * خروجی استاندارد اکسل (`.xlsx` OpenXML) به صورت افقی و راست‌به‌چپ (`rightToLeft="1"`):
     * - شیت به صورت خودکار در Excel، Google Sheets و LibreOffice از راست به چپ باز می‌شود (ستون A در سمت راست).
     * - ردیف ۱ (`A1, B1, C1, ...`) سرستون متغیرهاست و فریز شده است.
     * - ردیف ۲ به بعد (`A2, B2, C2, ...`) هر ردیف مربوط به یک کارگر است.
     */
    fun exportToExcelXlsx(
        context: Context,
        workers: List<WorkerEntity>,
        attendanceList: List<AttendanceEntity>,
        performances: List<WorkerPerformance>,
        projectName: String = "پروژه کارگاهی",
        expenses: List<ExpenseEntity> = emptyList(),
        analytics: DashboardAnalytics? = null
    ): File {
        val fileName = "گزارش_کارگران_${System.currentTimeMillis()}.xlsx"
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, fileName)

        val resolvedPerformances = resolveWorkerPerformances(workers, performances)
        val attendanceByWorker = attendanceList.groupBy { it.workerId }

        val dataRows: List<List<Any>> = resolvedPerformances.map { p ->
            val workerAtts = attendanceByWorker[p.worker.id].orEmpty()
            buildWorkerRowValues(p, workerAtts, projectName)
        }

        FileOutputStream(file).use { fos ->
            ZipOutputStream(fos).use { zos ->
                writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml())
                writeZipEntry(zos, "_rels/.rels", buildRootRelsXml())
                writeZipEntry(zos, "xl/workbook.xml", buildWorkbookXml())
                writeZipEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())
                writeZipEntry(zos, "xl/styles.xml", buildStylesXml())
                writeZipEntry(
                    zos,
                    "xl/worksheets/sheet1.xml",
                    buildWorksheetXml(WORKER_EXPORT_HEADERS, dataRows)
                )
            }
        }
        return file
    }

    internal fun columnIndexToLetter(colIndexOneBased: Int): String {
        var n = colIndexOneBased
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(input: String): String {
        val sb = StringBuilder(input.length + 16)
        for (ch in input) {
            val code = ch.code
            val isValidXmlChar = code == 0x09 || code == 0x0A || code == 0x0D ||
                (code in 0x20..0xD7FF) || (code in 0xE000..0xFFFD)
            if (!isValidXmlChar) continue
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun buildContentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
    """.trimIndent()

    private fun buildRootRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun buildWorkbookXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <workbookViews>
            <workbookView xWindow="0" yWindow="0" windowWidth="28800" windowHeight="17400"/>
          </workbookViews>
          <sheets>
            <sheet name="گزارش کارگران" sheetId="1" r:id="rId1"/>
          </sheets>
        </workbook>
    """.trimIndent()

    private fun buildWorkbookRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun buildStylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <fonts count="2">
            <font>
              <sz val="11"/>
              <color rgb="FF0F172A"/>
              <name val="Tahoma"/>
            </font>
            <font>
              <b/>
              <sz val="11"/>
              <color rgb="FFFFFFFF"/>
              <name val="Tahoma"/>
            </font>
          </fonts>
          <fills count="3">
            <fill>
              <patternFill patternType="none"/>
            </fill>
            <fill>
              <patternFill patternType="gray125"/>
            </fill>
            <fill>
              <patternFill patternType="solid">
                <fgColor rgb="FF0F766E"/>
                <bgColor indexed="64"/>
              </patternFill>
            </fill>
          </fills>
          <borders count="2">
            <border>
              <left/><right/><top/><bottom/><diagonal/>
            </border>
            <border>
              <left style="thin"><color rgb="FFCBD5E1"/></left>
              <right style="thin"><color rgb="FFCBD5E1"/></right>
              <top style="thin"><color rgb="FFCBD5E1"/></top>
              <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
              <diagonal/>
            </border>
          </borders>
          <cellStyleXfs count="1">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
          </cellStyleXfs>
          <cellXfs count="4">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
              <alignment horizontal="center" vertical="center" readingOrder="2" wrapText="1"/>
            </xf>
            <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
              <alignment horizontal="center" vertical="center" readingOrder="2"/>
            </xf>
            <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
              <alignment horizontal="center" vertical="center" readingOrder="2"/>
            </xf>
          </cellXfs>
        </styleSheet>
    """.trimIndent()

    private fun buildWorksheetXml(
        headers: List<String>,
        rows: List<List<Any>>
    ): String {
        val sb = StringBuilder(4096)
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""").append('\n')
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""").append('\n')
        sb.append("""  <sheetViews>""").append('\n')
        sb.append("""    <sheetView rightToLeft="1" tabSelected="1" workbookViewId="0">""").append('\n')
        sb.append("""      <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""").append('\n')
        sb.append("""    </sheetView>""").append('\n')
        sb.append("""  </sheetViews>""").append('\n')
        sb.append("""  <sheetFormatPr defaultRowHeight="22" customHeight="1"/>""").append('\n')

        // Column widths
        sb.append("""  <cols>""").append('\n')
        for (colIdx in headers.indices) {
            val colNum = colIdx + 1
            val headerLen = headers[colIdx].length
            val maxDataLen = rows.maxOfOrNull { row ->
                row.getOrNull(colIdx)?.toString()?.length ?: 0
            } ?: 0
            val width = maxOf(headerLen + 6, maxDataLen + 6, 16).coerceAtMost(45)
            sb.append("""    <col min="$colNum" max="$colNum" width="$width" customWidth="1"/>""").append('\n')
        }
        sb.append("""  </cols>""").append('\n')

        sb.append("""  <sheetData>""").append('\n')

        // Row 1: Column Headers
        sb.append("""    <row r="1" ht="28" customHeight="1">""").append('\n')
        for ((colIdx, header) in headers.withIndex()) {
            val cellRef = "${columnIndexToLetter(colIdx + 1)}1"
            val safeText = escapeXml(sanitizeFormulaInjection(header))
            sb.append("""      <c r="$cellRef" s="1" t="inlineStr"><is><t xml:space="preserve">$safeText</t></is></c>""").append('\n')
        }
        sb.append("""    </row>""").append('\n')

        // Row 2+: Worker Data Rows
        for ((rowIdx, rowCells) in rows.withIndex()) {
            val excelRowNum = rowIdx + 2
            sb.append("""    <row r="$excelRowNum" ht="22" customHeight="1">""").append('\n')
            for ((colIdx, cellValue) in rowCells.withIndex()) {
                val cellRef = "${columnIndexToLetter(colIdx + 1)}$excelRowNum"
                // Keep text columns (name, role, workSummary, phone, nationalId, isActive, projectName, notes) as inlineStr
                val forceTextColumn = colIdx in setOf(0, 1, 7, 32, 33, 34, 35, 36)
                val numericStr = when (cellValue) {
                    is Int, is Long -> cellValue.toString()
                    is Double -> if (cellValue.isFinite()) formatCsvHours(cellValue) else null
                    is String -> if (!forceTextColumn && isNumericString(cellValue)) cellValue.trim() else null
                    else -> null
                }
                if (numericStr != null && !forceTextColumn) {
                    sb.append("""      <c r="$cellRef" s="3" t="n"><v>$numericStr</v></c>""").append('\n')
                } else {
                    val safeText = escapeXml(sanitizeFormulaInjection(cellValue.toString()))
                    sb.append("""      <c r="$cellRef" s="2" t="inlineStr"><is><t xml:space="preserve">$safeText</t></is></c>""").append('\n')
                }
            }
            sb.append("""    </row>""").append('\n')
        }

        sb.append("""  </sheetData>""").append('\n')
        sb.append("""</worksheet>""")
        return sb.toString()
    }

    const val MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val MIME_TYPE_PDF = "application/pdf"

    /**
     * ساخت نام فایل استاندارد و خوانا برای ذخیره در حافظه داخلی گوشی
     */
    fun buildExportFileName(
        prefix: String,
        projectName: String,
        extension: String
    ): String {
        val safeProject = projectName
            .trim()
            .ifBlank { "KarYar" }
            .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .trim('_')
            .ifBlank { "KarYar" }
        val jalaliDate = try {
            JalaliCalendar.refreshToday().replace("/", "-")
        } catch (_: Exception) {
            "1404"
        }
        val cleanExt = extension.trimStart('.')
        return "${prefix}_${safeProject}_${jalaliDate}.$cleanExt"
    }

    /**
     * ذخیره مستقیم فایل خروجی (اکسل یا PDF) در حافظه داخلی گوشی (پوشه Download/KarYar).
     * در اندروید ۱۰ به بالا از MediaStore.Downloads بدون نیاز به مجوز اضافه استفاده می‌کند
     * و در محیط‌های قدیمی‌تر یا تست، در پوشه Download/KarYar کپی می‌کند.
     */
    fun saveFileToInternalStorage(
        context: Context,
        sourceFile: File,
        desiredFileName: String = sourceFile.name,
        mimeType: String
    ): SavedExportResult {
        require(sourceFile.exists() && sourceFile.length() > 0L) {
            "فایل مبدأ برای ذخیره‌سازی یافت نشد یا خالی است"
        }

        val cleanFileName = desiredFileName.trim().ifBlank { sourceFile.name }
        val relativeSubPath = "${Environment.DIRECTORY_DOWNLOADS}/KarYar"

        // ۱. تلاش برای ثبت مستقیم در MediaStore.Downloads در اندروید 10 (Q) به بالا
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, cleanFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeSubPath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val insertedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (insertedUri != null) {
                    val written = resolver.openOutputStream(insertedUri)?.use { out ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                        out.flush()
                        true
                    } ?: false

                    if (written) {
                        val completeValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        resolver.update(insertedUri, completeValues, null, null)
                        val mirrorFile = runCatching {
                            copyToFallbackDownloadsDir(context, sourceFile, cleanFileName)
                        }.getOrNull()
                        return SavedExportResult(
                            fileName = cleanFileName,
                            displayPath = "Download/KarYar/$cleanFileName",
                            uri = insertedUri,
                            localFile = mirrorFile
                        )
                    } else {
                        runCatching { resolver.delete(insertedUri, null, null) }
                    }
                }
            } catch (_: Exception) {
                // در صورت خطا در MediaStore، به مسیر فایل سیستم fallback می‌رویم
            }
        }

        // ۲. مسیر جایگزین (اندروید ۹ و پایین‌تر یا محیط تست/فایل‌سیستم مستقیم)
        val targetFile = copyToFallbackDownloadsDir(context, sourceFile, cleanFileName)
        return SavedExportResult(
            fileName = targetFile.name,
            displayPath = "Download/KarYar/${targetFile.name}",
            uri = runCatching { Uri.fromFile(targetFile) }.getOrNull(),
            localFile = targetFile
        )
    }

    private fun copyToFallbackDownloadsDir(
        context: Context,
        sourceFile: File,
        cleanFileName: String
    ): File {
        val publicDownloads = runCatching {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KarYar").apply {
                if (!exists()) mkdirs()
            }
        }.getOrNull()?.takeIf { it.exists() && it.canWrite() }

        val targetDir = publicDownloads ?: File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, "Downloads"),
            "KarYar"
        ).apply {
            if (!exists()) mkdirs()
        }

        val targetFile = File(targetDir, cleanFileName)
        sourceFile.inputStream().use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
        return targetFile
    }

    /**
     * ذخیره فایل خروجی در مسیر انتخاب‌شده توسط کاربر از طریق فایل‌منیجر سیستم (SAF CreateDocument)
     */
    fun writeFileToUri(context: Context, sourceFile: File, targetUri: Uri) {
        val outputStream = context.contentResolver.openOutputStream(targetUri)
            ?: throw IOException("امکان باز کردن مسیر انتخابی در حافظه وجود ندارد")
        outputStream.use { out ->
            sourceFile.inputStream().use { input ->
                input.copyTo(out)
            }
            out.flush()
        }
    }

    /**
     * Share exported file via standard Android share sheet
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }
}
