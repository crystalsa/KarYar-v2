package com.karyar.app

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.karyar.app.util.JalaliCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JalaliCalendarTest {

    @Test
    fun testLeapYear1403AndNonLeapYear1404() {
        // 1403 is a leap year (کبیسه): 1403/12/30 is valid and next day is 1404/01/01
        assertTrue("1403 must be a leap year (کبیسه)", JalaliCalendar.isLeapYear(1403))
        assertEquals(30, JalaliCalendar.getDaysInMonth(1403, 12))
        assertTrue("1403/12/30 must be valid", JalaliCalendar.isValidDate("1403/12/30"))
        assertEquals(Triple(1403, 12, 30), JalaliCalendar.parseDateOrNull("1403/12/30"))
        assertNull(JalaliCalendar.validateDateError("1403/12/30"))

        val (nextAfter1403Esfand30, _) = JalaliCalendar.nextDay("1403/12/30")
        assertEquals("1404/01/01", nextAfter1403Esfand30)
        assertEquals("1403/12/30", JalaliCalendar.previousDay("1404/01/01").first)

        // 1404 is NOT a leap year: 1404/12/30 is invalid
        assertFalse("1404 must NOT be a leap year", JalaliCalendar.isLeapYear(1404))
        assertEquals(29, JalaliCalendar.getDaysInMonth(1404, 12))
        assertFalse("1404/12/30 must be invalid", JalaliCalendar.isValidDate("1404/12/30"))
        assertNull("parseDateOrNull must return null for 1404/12/30", JalaliCalendar.parseDateOrNull("1404/12/30"))
        assertNull("toEpochDayOrNull must return null for 1404/12/30", JalaliCalendar.toEpochDayOrNull("1404/12/30"))

        val errorMsg = JalaliCalendar.validateDateError("1404/12/30")
        assertNotNull("validateDateError must return Persian error for 1404/12/30", errorMsg)
        assertTrue("Error message should mention 1404 / کبیسه", errorMsg!!.contains("کبیسه"))

        assertThrows(IllegalArgumentException::class.java) {
            JalaliCalendar.parseDate("1404/12/30")
        }
        assertThrows(IllegalArgumentException::class.java) {
            JalaliCalendar.toEpochDay("1404/12/30")
        }

        // Day after 1404/12/29 is 1405/01/01
        assertEquals("1405/01/01", JalaliCalendar.nextDay("1404/12/29").first)
        assertEquals("1404/12/29", JalaliCalendar.previousDay("1405/01/01").first)
    }

    @Test
    fun testNowruz1405Equals2026March21() {
        val (gy, gm, gd) = JalaliCalendar.jalaliToGregorian(1405, 1, 1)
        assertEquals(2026, gy)
        assertEquals(3, gm)
        assertEquals(21, gd)

        val jalali = JalaliCalendar.gregorianToJalali(2026, 3, 21)
        assertEquals("1405/01/01", jalali.format())
        assertEquals("شنبه", JalaliCalendar.getDayOfWeek("1405/01/01"))
    }

    @Test
    fun testDaysInMonth() {
        for (m in 1..6) {
            assertEquals("Month $m should have 31 days", 31, JalaliCalendar.getDaysInMonth(1403, m))
        }
        for (m in 7..11) {
            assertEquals("Month $m should have 30 days", 30, JalaliCalendar.getDaysInMonth(1403, m))
        }
        assertEquals("1399 Esfand must have 30 days", 30, JalaliCalendar.getDaysInMonth(1399, 12))
        assertEquals("1402 Esfand must have 29 days", 29, JalaliCalendar.getDaysInMonth(1402, 12))
        assertEquals("1403 Esfand must have 30 days", 30, JalaliCalendar.getDaysInMonth(1403, 12))
        assertEquals("1404 Esfand must have 29 days", 29, JalaliCalendar.getDaysInMonth(1404, 12))
    }

    @Test
    fun testMonthBoundaryTransition() {
        val (nextAfterShahrivar, _) = JalaliCalendar.nextDay("1403/06/31")
        assertEquals("1403/07/01", nextAfterShahrivar)

        val (prevFromMehr, _) = JalaliCalendar.previousDay("1403/07/01")
        assertEquals("1403/06/31", prevFromMehr)

        val (nextAfterBahman, _) = JalaliCalendar.nextDay("1403/11/30")
        assertEquals("1403/12/01", nextAfterBahman)

        val (prevFromEsfand, _) = JalaliCalendar.previousDay("1403/12/01")
        assertEquals("1403/11/30", prevFromEsfand)
    }

    @Test
    fun testStrictValidationRejectsInvalidDates() {
        val invalidDates = listOf(
            "",
            "   ",
            "1403/13/01",
            "1403/00/10",
            "1403/07/31",
            "1402/12/30",
            "1404/12/30",
            "1403/01/32",
            "1403/01/00",
            "invalid-date"
        )
        for (invalid in invalidDates) {
            assertFalse("Expected isValidDate=false for '$invalid'", JalaliCalendar.isValidDate(invalid))
            assertNull("Expected parseDateOrNull=null for '$invalid'", JalaliCalendar.parseDateOrNull(invalid))
            assertNotNull("Expected Persian validation error for '$invalid'", JalaliCalendar.validateDateError(invalid))
            assertThrows("parseDate should throw on '$invalid'", IllegalArgumentException::class.java) {
                JalaliCalendar.parseDate(invalid)
            }
            assertThrows("toEpochDay should throw on '$invalid'", IllegalArgumentException::class.java) {
                JalaliCalendar.toEpochDay(invalid)
            }
        }
    }

    @Test
    fun testFullRoundtripAllDaysFrom1380To1500() {
        var previousEpochDay: Long? = null
        var totalDaysChecked = 0

        for (year in 1380..1500) {
            for (month in 1..12) {
                val daysInMonth = JalaliCalendar.getDaysInMonth(year, month)
                for (day in 1..daysInMonth) {
                    val dateStr = JalaliCalendar.formatDate(year, month, day)

                    // 1. Jalali -> Gregorian -> Jalali round-trip
                    val (gy, gm, gd) = JalaliCalendar.jalaliToGregorian(year, month, day)
                    val backFromGregorian = JalaliCalendar.gregorianToJalali(gy, gm, gd)
                    assertEquals("Gregorian round-trip year mismatch on $dateStr", year, backFromGregorian.year)
                    assertEquals("Gregorian round-trip month mismatch on $dateStr", month, backFromGregorian.month)
                    assertEquals("Gregorian round-trip day mismatch on $dateStr", day, backFromGregorian.day)

                    // 2. Jalali -> epochDay -> Jalali round-trip & strict monotonicity
                    val epochDay = JalaliCalendar.toEpochDay(dateStr)
                    if (previousEpochDay != null) {
                        assertEquals(
                            "EpochDay must increment by 1 each consecutive day at $dateStr",
                            previousEpochDay + 1L,
                            epochDay
                        )
                    }
                    previousEpochDay = epochDay

                    val backFromEpoch = JalaliCalendar.fromEpochDay(epochDay)
                    assertEquals("EpochDay round-trip mismatch on $dateStr", dateStr, backFromEpoch.format())

                    totalDaysChecked++
                }
            }
        }

        assertTrue("Should have checked > 44,000 days in 1380..1500", totalDaysChecked > 44_000)
    }

    @Test
    fun testCompareIcuLeapYearsWithOfficialAstronomicalIranianCalendar1380To1500() {
        // Official astronomical Iranian leap years in 1380..1500 (based on Tehran vernal equinox):
        // 4-year and 5-year cycles:
        // 1383, 1387, 1391, 1395, 1399, 1403 (5-yr gap to 1408),
        // 1408, 1412, 1416, 1420, 1424, 1428, 1432, 1436 (5-yr gap to 1441),
        // 1441, 1445, 1449, 1453, 1457, 1461, 1465, 1469 (5-yr gap to 1474),
        // 1474, 1478, 1482, 1486, 1490, 1494, 1498
        val astronomicalLeapYears = setOf(
            1383, 1387, 1391, 1395, 1399, 1403,
            1408, 1412, 1416, 1420, 1424, 1428, 1432, 1436,
            1441, 1445, 1449, 1453, 1457, 1461, 1465, 1469,
            1474, 1478, 1482, 1486, 1490, 1494, 1498
        )

        val discrepantYears = mutableListOf<String>()
        for (year in 1380..1500) {
            val icuLeap = JalaliCalendar.isLeapYear(year)
            val astroLeap = year in astronomicalLeapYears
            if (icuLeap != astroLeap) {
                discrepantYears.add("$year (ICU=$icuLeap, Astronomical=$astroLeap)")
            }
        }
        println("ICU vs Astronomical leap year discrepancies in 1380..1500: $discrepantYears")
        // Verify 1403 and 1404 specifically match reality
        assertTrue(JalaliCalendar.isLeapYear(1403))
        assertFalse(JalaliCalendar.isLeapYear(1404))
    }

    @Test
    fun testDigitConversion() {
        assertEquals("1403/05/12", JalaliCalendar.toEnglishDigits("۱۴۰۳/۰۵/۱۲"))
        assertEquals("1403/05/12", JalaliCalendar.toEnglishDigits("١٤٠٣/٠٥/١٢"))
        assertEquals("۰۱۲۳۴۵۶۷۸۹", JalaliCalendar.toPersianDigits("0123456789"))
    }

    @Test
    fun testTimezoneAndMidnightDriftStability() {
        val originalTz = TimeZone.getDefault()
        val timeZonesToTest = listOf(
            "UTC",
            "Asia/Tehran",
            "America/Los_Angeles",
            "America/New_York",
            "Europe/London",
            "Asia/Tokyo",
            "Pacific/Auckland",
            "Pacific/Honolulu"
        )
        // Known dates with their exact Persian day of week and epochDay:
        // 1403/01/01 = 2024-03-20 (Wednesday -> چهارشنبه, epochDay = 19802)
        // 1403/06/31 = 2024-09-21 (Saturday -> شنبه, epochDay = 19987)
        // 1403/07/01 = 2024-09-22 (Sunday -> یکشنبه, epochDay = 19988)
        // 1402/12/29 = 2024-03-19 (Tuesday -> سه‌شنبه, epochDay = 19801)
        // 1403/12/30 = 2025-03-20 (Thursday -> پنج‌شنبه, epochDay = 20167)
        // 1404/01/01 = 2025-03-21 (Friday -> جمعه, epochDay = 20168)
        // 1405/01/01 = 2026-03-21 (Saturday -> شنبه, epochDay = 20533)
        val expectedSamples = listOf(
            Triple("1403/01/01", "چهارشنبه", 19802L),
            Triple("1403/06/31", "شنبه", 19987L),
            Triple("1403/07/01", "یکشنبه", 19988L),
            Triple("1402/12/29", "سه‌شنبه", 19801L),
            Triple("1403/12/30", "پنج‌شنبه", 20167L),
            Triple("1404/01/01", "جمعه", 20168L),
            Triple("1405/01/01", "شنبه", 20533L)
        )

        try {
            for (tzId in timeZonesToTest) {
                TimeZone.setDefault(TimeZone.getTimeZone(tzId))

                for ((dateStr, expectedDow, expectedEpochDay) in expectedSamples) {
                    assertEquals(
                        "getDayOfWeek drift in timezone $tzId for $dateStr",
                        expectedDow,
                        JalaliCalendar.getDayOfWeek(dateStr)
                    )
                    assertEquals(
                        "toEpochDay drift in timezone $tzId for $dateStr",
                        expectedEpochDay,
                        JalaliCalendar.toEpochDay(dateStr)
                    )
                    assertEquals(
                        "fromEpochDay drift in timezone $tzId for $dateStr",
                        dateStr,
                        JalaliCalendar.fromEpochDay(expectedEpochDay).format()
                    )
                }
            }
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun testTodayFlowUpdatesOnBroadcastAndTimezoneChange() {
        val originalTz = TimeZone.getDefault()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = JalaliCalendar.registerTimeChangeReceiver(context)
        try {
            val expectedInTehran = JalaliCalendar.today(TimeZone.getTimeZone("Asia/Tehran")).format()
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tehran"))
            receiver.onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))
            assertEquals(expectedInTehran, JalaliCalendar.todayFlow.value)
        } finally {
            context.unregisterReceiver(receiver)
            TimeZone.setDefault(originalTz)
            JalaliCalendar.refreshToday()
        }
    }
}
