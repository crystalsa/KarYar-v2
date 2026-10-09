package com.karyar.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.karyar.app.R
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.domain.model.DashboardAnalytics
import com.karyar.app.domain.model.WorkerPerformance
import java.io.File
import java.io.FileOutputStream

object PdfExportUtil {

    // A4 Landscape Dimensions (افقی)
    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595
    private const val MARGIN_LEFT = 35f
    private const val MARGIN_RIGHT = 807f
    private const val CONTENT_WIDTH = 772
    private const val MAX_CONTENT_Y = 515f

    /**
     * Generates a multi-page, professional Landscape (افقی) PDF report with embedded Persian font,
     * proper RTL text shaping via StaticLayout, no abbreviations, full words (روز، ساعت، تومان),
     * and clean work breakdown formatting.
     */
    fun exportToPdf(
        context: Context,
        projectName: String,
        employerName: String,
        foremanName: String,
        analytics: DashboardAnalytics,
        performances: List<WorkerPerformance>,
        expenses: List<ExpenseEntity> = emptyList()
    ): File {
        val vazirTypeface: Typeface = try {
            val tf = ResourcesCompat.getFont(context, R.font.vazirmatn)
            tf ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        val pdfDoc = PdfDocument()
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        // 1. First Page Header
        drawFirstPageHeader(
            canvas = canvas,
            typeface = vazirTypeface,
            projectName = projectName,
            employerName = employerName,
            foremanName = foremanName,
            analytics = analytics
        )

        // 2. Financial Summary Cards on First Page
        drawFinancialSummaryCards(canvas, vazirTypeface, analytics)

        // 3. Workers Table Title
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = vazirTypeface
            textSize = 12f
            isFakeBoldText = true
            color = Color.parseColor("#0F172A")
        }
        drawRtlText(canvas, "صورت‌جلسه کارکرد و تسویه حساب هر کارگر (تفکیک به ازای شناسه کارگر):", MARGIN_LEFT, 192f, textPaint, CONTENT_WIDTH)

        // Table Header
        var currentY = 212f
        drawTableHeader(canvas, vazirTypeface, currentY)
        currentY += 22f

        // Draw Workers Rows (All workers, separate row per worker ID)
        val rowHeight = 22f
        for ((index, p) in performances.withIndex()) {
            if (currentY + rowHeight > MAX_CONTENT_Y) {
                // Finish current page
                drawPageFooter(canvas, vazirTypeface, pageNumber)
                pdfDoc.finishPage(page)

                // Start next page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas

                // Compact Header for continuation page
                drawContinuationHeader(canvas, vazirTypeface, projectName)
                currentY = 48f
                drawTableHeader(canvas, vazirTypeface, currentY)
                currentY += 22f
            }

            drawWorkerRow(canvas, vazirTypeface, index, p, currentY)
            currentY += rowHeight
        }

        // Summary and Signatures: if not enough room on current page, create a final page
        val summaryNeededHeight = 135f
        if (currentY + summaryNeededHeight > MAX_CONTENT_Y) {
            drawPageFooter(canvas, vazirTypeface, pageNumber)
            pdfDoc.finishPage(page)

            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDoc.startPage(pageInfo)
            canvas = page.canvas

            drawContinuationHeader(canvas, vazirTypeface, projectName)
            currentY = 48f
        }

        currentY += 10f
        drawSummaryAndExpensesBox(canvas, vazirTypeface, currentY, analytics, expenses)
        currentY += 72f

        drawSignatures(canvas, vazirTypeface, currentY, foremanName, employerName)

        drawPageFooter(canvas, vazirTypeface, pageNumber)
        pdfDoc.finishPage(page)

        // Save to cache/exports
        val fileName = "گزارش_کارگاه_${System.currentTimeMillis()}.pdf"
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        return file
    }

    private fun drawFirstPageHeader(
        canvas: Canvas,
        typeface: Typeface,
        projectName: String,
        employerName: String,
        foremanName: String,
        analytics: DashboardAnalytics
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Banner background (Landscape wide)
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 72f, paint)

        // Gold accent line
        paint.color = Color.parseColor("#F59E0B")
        canvas.drawRect(0f, 72f, PAGE_WIDTH.toFloat(), 75f, paint)

        // Header Title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 15f
            isFakeBoldText = true
            color = Color.WHITE
        }
        drawRtlText(canvas, "گزارش جامع عملکرد، حضور و غیاب و تسویه کارگران", MARGIN_LEFT, 15f, titlePaint, CONTENT_WIDTH)

        // Subtitle
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 9.5f
            color = Color.parseColor("#94A3B8")
        }
        val todayStr = JalaliCalendar.todayString()
        drawRtlText(canvas, "پروژه: $projectName   |   تاریخ صدور: $todayStr", MARGIN_LEFT, 44f, subPaint, CONTENT_WIDTH)

        // Metadata Box in Landscape
        paint.color = Color.parseColor("#F1F5F9")
        val metaRect = RectF(MARGIN_LEFT, 85f, MARGIN_RIGHT, 122f)
        canvas.drawRoundRect(metaRect, 6f, 6f, paint)

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 9.5f
            isFakeBoldText = true
            color = Color.parseColor("#1E293B")
        }
        // RTL ordering across 772 width: Employer on Right, Foreman in Center, Personnel on Left
        drawRtlText(canvas, "کارفرما: $employerName", 540f, 98f, metaPaint, 250)
        drawRtlText(canvas, "سرکارگر: $foremanName", 285f, 98f, metaPaint, 240)
        drawRtlText(canvas, "تعداد کل کارگران: ${Formatters.toPersianDigits(analytics.totalWorkersCount)} نفر (${Formatters.toPersianDigits(analytics.activeWorkersCount)} فعال)", 45f, 98f, metaPaint, 230)
    }

    private fun drawContinuationHeader(canvas: Canvas, typeface: Typeface, projectName: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 34f, paint)

        paint.color = Color.parseColor("#F59E0B")
        canvas.drawRect(0f, 34f, PAGE_WIDTH.toFloat(), 36f, paint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 10.5f
            isFakeBoldText = true
            color = Color.WHITE
        }
        drawRtlText(canvas, "پروژه: $projectName  |  ادامه صورت‌جلسه کارکرد و تسویه حساب پرسنل", MARGIN_LEFT, 11f, textPaint, CONTENT_WIDTH)
    }

    private fun drawFinancialSummaryCards(canvas: Canvas, typeface: Typeface, analytics: DashboardAnalytics) {
        val cardWidth = 180f
        val cardHeight = 48f
        val startY = 132f

        // 4 cards in Landscape, flowing RTL:
        // Card 1 (Rightmost): 627f
        // Card 2: 430f
        // Card 3: 233f
        // Card 4 (Leftmost): 35f
        drawStatCard(canvas, typeface, 627f, startY, cardWidth, cardHeight, "مجموع دستمزد پایه", Formatters.formatCurrency(analytics.totalWagesPaid), "#10B981")
        drawStatCard(canvas, typeface, 430f, startY, cardWidth, cardHeight, "اضافه کاری و ساعتی", Formatters.formatCurrency(analytics.totalOvertimePaid + analytics.totalHourlyPaid), "#F59E0B")
        drawStatCard(canvas, typeface, 233f, startY, cardWidth, cardHeight, "هزینه‌های کارگاه", Formatters.formatCurrency(analytics.grandTotalExpenses), "#EC4899")
        drawStatCard(canvas, typeface, 35f, startY, cardWidth, cardHeight, "کل مخارج پروژه", Formatters.formatCurrency(analytics.grandTotalProjectCost), "#6366F1")
    }

    private fun drawTableHeader(canvas: Canvas, typeface: Typeface, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 20f, paint)

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f
            isFakeBoldText = true
            color = Color.parseColor("#334155")
        }

        // Strict Right-to-Left Columns in Landscape (total width 772f, from 35f to 807f):
        // 1. Radif (Rightmost): 775f (width 28)
        // 2. Name & ID: 625f (width 145)
        // 3. Role / Specialty: 515f (width 105)
        // 4. Work Summary (Full, Half, Hourly without abbreviations): 325f (width 185)
        // 5. Overtime: 235f (width 85)
        // 6. Adjustments / Deductions / Share: 135f (width 95)
        // 7. Net Payout (Leftmost): 35f (width 95)
        drawRtlText(canvas, "ردیف", 775f, y + 4f, headerPaint, 28)
        drawRtlText(canvas, "نام کارگر (شناسه)", 625f, y + 4f, headerPaint, 145)
        drawRtlText(canvas, "تخصص / شغل", 515f, y + 4f, headerPaint, 105)
        drawRtlText(canvas, "کارکرد (روز / ساعت)", 325f, y + 4f, headerPaint, 185)
        drawRtlText(canvas, "اضافه کاری", 235f, y + 4f, headerPaint, 85)
        drawRtlText(canvas, "مزایا / کسورات / سهم", 135f, y + 4f, headerPaint, 95)
        drawRtlText(canvas, "خالص دریافتی", 35f, y + 4f, headerPaint, 95)
    }

    private fun drawWorkerRow(
        canvas: Canvas,
        typeface: Typeface,
        index: Int,
        p: WorkerPerformance,
        y: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (index % 2 == 0) {
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRect(MARGIN_LEFT, y - 2f, MARGIN_RIGHT, y + 19f, paint)
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f
            color = Color.parseColor("#1E293B")
        }

        // 1. Index (Rightmost)
        drawRtlText(canvas, Formatters.toPersianDigits(index + 1), 775f, y + 3f, textPaint, 28)

        // 2. Name with ID
        val nameWithId = "${p.worker.name} (#${p.worker.id})"
        drawRtlText(canvas, nameWithId, 625f, y + 3f, textPaint, 145)

        // 3. Role
        drawRtlText(canvas, p.worker.role, 515f, y + 3f, textPaint, 105)

        // 4. Work Breakdown (بدون کلمات مخفف، برای تمام‌روز فقط ۲ روز، برای مختلط تفکیک کامل)
        val workSummary = p.formatWorkSummary()
        drawRtlText(canvas, workSummary, 325f, y + 3f, textPaint, 185)

        // 5. Overtime (بدون مخفف «س»، نوشتن کامل «ساعت»)
        val otStr = if (p.overtimeHours > 0) {
            "${Formatters.toPersianDigits(p.overtimeHours.toString().removeSuffix(".0"))} ساعت"
        } else {
            "-"
        }
        drawRtlText(canvas, otStr, 235f, y + 3f, textPaint, 85)

        // 6. Net allowances / deductions / group share
        val netAdjust = p.totalAllowances - p.totalDeductions - p.groupExpenseShare
        val adjustStr = if (netAdjust > 0L) "+${Formatters.formatCurrency(netAdjust)}" else if (netAdjust < 0L) Formatters.formatCurrency(netAdjust) else "۰"
        val adjustPaint = TextPaint(textPaint).apply {
            color = if (netAdjust > 0L) Color.parseColor("#059669") else if (netAdjust < 0L) Color.parseColor("#E11D48") else Color.parseColor("#64748B")
        }
        drawRtlText(canvas, adjustStr, 135f, y + 3f, adjustPaint, 95)

        // 7. Net Payout (Leftmost)
        val payoutPaint = TextPaint(textPaint).apply {
            isFakeBoldText = true
            color = Color.parseColor("#047857")
        }
        drawRtlText(canvas, Formatters.formatCurrency(p.netPayout), 35f, y + 3f, payoutPaint, 95)
    }

    private fun drawSummaryAndExpensesBox(
        canvas: Canvas,
        typeface: Typeface,
        y: Float,
        analytics: DashboardAnalytics,
        expenses: List<ExpenseEntity>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#F8FAFC")
        val expBox = RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + 62f)
        canvas.drawRoundRect(expBox, 8f, 8f, paint)

        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(expBox, 8f, 8f, paint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 9.5f
            isFakeBoldText = true
            color = Color.parseColor("#0F172A")
        }
        drawRtlText(canvas, "خلاصه آمار عملکرد، تردد و هزینه‌های این کارگاه:", MARGIN_LEFT + 12f, y + 8f, titlePaint, CONTENT_WIDTH - 24)

        val itemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f
            color = Color.parseColor("#334155")
        }

        // Line 1 in Landscape (3 columns across 772f)
        drawRtlText(canvas, "• کل ساعات کارکرد عادی: ${Formatters.toPersianDigits(analytics.totalWorkHours)} ساعت", 540f, y + 26f, itemPaint, 250)
        drawRtlText(canvas, "• کل ساعات اضافه کاری: ${Formatters.toPersianDigits(analytics.totalOvertimeHours)} ساعت", 285f, y + 26f, itemPaint, 240)
        drawRtlText(canvas, "• دستمزد ساعتی پرداختی: ${Formatters.formatCurrency(analytics.totalHourlyPaid)}", 45f, y + 26f, itemPaint, 230)

        // Line 2
        drawRtlText(canvas, "• تعداد روزهای کاری کارگاه: ${Formatters.toPersianDigits(analytics.totalWorkDaysCount)} روز تقویمی", 540f, y + 44f, itemPaint, 250)
        drawRtlText(canvas, "• مجموع کارکرد پرسنل: ${Formatters.toPersianDigits(analytics.totalPersonDays)} نفر-روز (شیفت)", 285f, y + 44f, itemPaint, 240)
        val grandTotalPaint = TextPaint(itemPaint).apply {
            isFakeBoldText = true
            color = Color.parseColor("#4338CA")
        }
        drawRtlText(canvas, "• هزینه کل نهایی پروژه: ${Formatters.formatCurrency(analytics.grandTotalProjectCost)}", 45f, y + 44f, grandTotalPaint, 230)
    }

    private fun drawSignatures(canvas: Canvas, typeface: Typeface, y: Float, foremanName: String, employerName: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 1f
        // Right signature line: Foreman in Landscape
        canvas.drawLine(520f, y, 760f, y, paint)
        // Left signature line: Employer in Landscape
        canvas.drawLine(80f, y, 320f, y, paint)

        val sigPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 9f
            color = Color.parseColor("#475569")
        }
        drawRtlText(canvas, "امضاء و تأیید سرکارگر: $foremanName", 520f, y + 8f, sigPaint, 240)
        drawRtlText(canvas, "امضاء و تأیید کارفرما: $employerName", 80f, y + 8f, sigPaint, 240)
    }

    private fun drawPageFooter(canvas: Canvas, typeface: Typeface, pageNumber: Int) {
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f
            color = Color.parseColor("#94A3B8")
        }
        val footerText = "سامانه مدیریت کارگران   |   صفحه ${Formatters.toPersianDigits(pageNumber)}"
        drawRtlText(canvas, footerText, MARGIN_LEFT, 575f, footerPaint, CONTENT_WIDTH, Layout.Alignment.ALIGN_CENTER)
    }

    private fun drawStatCard(
        canvas: Canvas,
        typeface: Typeface,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        accentHex: String
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#F8FAFC")
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        // Accent indicator bar on the RIGHT side for RTL
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor(accentHex)
        val rightBar = RectF(x + width - 3.5f, y, x + width, y + height)
        canvas.drawRoundRect(rightBar, 2f, 2f, paint)

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 7.5f
            color = Color.parseColor("#64748B")
        }
        drawRtlText(canvas, label, x + 5f, y + 7f, labelPaint, (width - 12).toInt())

        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f
            isFakeBoldText = true
            color = Color.parseColor("#0F172A")
        }
        drawRtlText(canvas, value, x + 5f, y + 25f, valuePaint, (width - 12).toInt())
    }

    /**
     * Renders Persian / Arabic text with proper letter shaping (اتصال حروف) and RTL layout direction.
     */
    private fun drawRtlText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: TextPaint,
        width: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ) {
        if (text.isBlank()) return
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(align)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
