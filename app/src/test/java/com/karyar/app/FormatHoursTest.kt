package com.karyar.app

import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.domain.model.WorkerPerformance
import com.karyar.app.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatHoursTest {

    @Test
    fun testFormatHoursRoundingForRequestedValues() {
        assertEquals("۰ ساعت و ۶ دقیقه", Formatters.formatHours(0.1))
        assertEquals("۱ ساعت و ۶ دقیقه", Formatters.formatHours(1.1))
        assertEquals("۲ ساعت و ۱۸ دقیقه", Formatters.formatHours(2.3))
        assertEquals("۳ ساعت و ۱۸ دقیقه", Formatters.formatHours(3.3))
        assertEquals("۴ ساعت و ۳۶ دقیقه", Formatters.formatHours(4.6))
        assertEquals("۷ ساعت و ۵۴ دقیقه", Formatters.formatHours(7.9))
        assertEquals("۸ ساعت و ۱۸ دقیقه", Formatters.formatHours(8.3))
        assertEquals("۱ ساعت", Formatters.formatHours(0.999))
        assertEquals("۸ ساعت", Formatters.formatHours(8.0))
    }

    @Test
    fun testFormatHoursRolloverWhenMinutesReachSixty() {
        // 2.999 * 60 = 179.94 -> rounds to 180 minutes -> 3 hours 0 minutes
        assertEquals("۳ ساعت", Formatters.formatHours(2.999))
        // 0.0 -> 0 hours
        assertEquals("۰ ساعت", Formatters.formatHours(0.0))
    }

    @Test
    fun testFormatHoursNegativeAndNonFiniteSafety() {
        assertEquals("۰ ساعت", Formatters.formatHours(-1.0))
        assertEquals("۰ ساعت", Formatters.formatHours(-3.3))
        assertEquals("۰ ساعت", Formatters.formatHours(Double.NaN))
        assertEquals("۰ ساعت", Formatters.formatHours(Double.NEGATIVE_INFINITY))
        assertEquals("۰ ساعت", Formatters.formatHours(Double.POSITIVE_INFINITY))
    }

    @Test
    fun testWorkerPerformanceFormatWorkSummaryUsesFormatHours() {
        val worker = WorkerEntity(
            id = 1L,
            folderId = 1L,
            name = "علی محمدی",
            role = "جوشکار"
        )
        val perfWhole = WorkerPerformance(
            worker = worker,
            totalShifts = 2,
            regularHours = 8.0,
            hourlyHours = 16.0,
            overtimeHours = 0.0,
            baseWageTotal = 1_000_000L,
            hourlyPayTotal = 1_600_000L,
            overtimePayTotal = 0L,
            fullDaysCount = 1,
            hourlyDaysCount = 1,
            netPayout = 2_600_000L
        )
        assertEquals("(۱ روز: تمام‌روز) (۱۶ ساعت: ساعتی)", perfWhole.formatWorkSummary())

        val perfFractional = perfWhole.copy(hourlyHours = 3.3)
        assertEquals("(۱ روز: تمام‌روز) (۳ ساعت و ۱۸ دقیقه: ساعتی)", perfFractional.formatWorkSummary())
    }
}
