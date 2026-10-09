package com.karyar.app

import com.karyar.app.util.JalaliCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliCalendarTest {

    @Test
    fun testLeapYearCalculation() {
        // Known Persian leap years in Birashk 33-year cycle
        assertTrue("1391 must be a leap year (کبیسه)", JalaliCalendar.isLeapYear(1391))
        assertTrue("1395 must be a leap year (کبیسه)", JalaliCalendar.isLeapYear(1395))
        assertTrue("1399 must be a leap year (کبیسه)", JalaliCalendar.isLeapYear(1399))
        assertTrue("1404 must be a leap year (کبیسه)", JalaliCalendar.isLeapYear(1404))
        assertTrue("1408 must be a leap year (کبیسه)", JalaliCalendar.isLeapYear(1408))

        // Known regular years
        assertFalse("1400 must NOT be a leap year", JalaliCalendar.isLeapYear(1400))
        assertFalse("1401 must NOT be a leap year", JalaliCalendar.isLeapYear(1401))
        assertFalse("1402 must NOT be a leap year", JalaliCalendar.isLeapYear(1402))
        assertFalse("1403 must NOT be a leap year", JalaliCalendar.isLeapYear(1403))
    }

    @Test
    fun testDaysInMonth() {
        // First 6 months: 31 days
        for (m in 1..6) {
            assertEquals("Month $m should have 31 days", 31, JalaliCalendar.getDaysInMonth(1403, m))
        }

        // Months 7 to 11: 30 days
        for (m in 7..11) {
            assertEquals("Month $m should have 30 days", 30, JalaliCalendar.getDaysInMonth(1403, m))
        }

        // Month 12 (Esfand): 30 in leap year (1399, 1404), 29 in non-leap year (1402, 1403)
        assertEquals("1399 Esfand must have 30 days", 30, JalaliCalendar.getDaysInMonth(1399, 12))
        assertEquals("1404 Esfand must have 30 days", 30, JalaliCalendar.getDaysInMonth(1404, 12))
        assertEquals("1402 Esfand must have 29 days", 29, JalaliCalendar.getDaysInMonth(1402, 12))
        assertEquals("1403 Esfand must have 29 days", 29, JalaliCalendar.getDaysInMonth(1403, 12))
    }

    @Test
    fun testMonthBoundaryTransition() {
        // 31 Shahrivar -> 1 Mehr (never 32 Shahrivar)
        val (nextAfterShahrivar, _) = JalaliCalendar.nextDay("1403/06/31")
        assertEquals("1403/07/01", nextAfterShahrivar)

        // 1 Mehr previous day -> 31 Shahrivar
        val (prevFromMehr, _) = JalaliCalendar.previousDay("1403/07/01")
        assertEquals("1403/06/31", prevFromMehr)

        // 30 Bahman -> 1 Esfand
        val (nextAfterBahman, _) = JalaliCalendar.nextDay("1403/11/30")
        assertEquals("1403/12/01", nextAfterBahman)

        // 1 Esfand previous day -> 30 Bahman
        val (prevFromEsfand, _) = JalaliCalendar.previousDay("1403/12/01")
        assertEquals("1403/11/30", prevFromEsfand)
    }

    @Test
    fun testYearBoundaryTransition_NonLeapYear() {
        // 1402 is non-leap (Esfand has 29 days)
        val (dayAfter29Esfand, _) = JalaliCalendar.nextDay("1402/12/29")
        assertEquals("1403/01/01", dayAfter29Esfand)

        val (dayBeforeNowruz, _) = JalaliCalendar.previousDay("1403/01/01")
        assertEquals("1402/12/29", dayBeforeNowruz)
    }

    @Test
    fun testYearBoundaryTransition_LeapYear() {
        // 1399 is a leap year (Esfand has 30 days)
        val (dayAfter29Esfand1399, _) = JalaliCalendar.nextDay("1399/12/29")
        assertEquals("1399/12/30", dayAfter29Esfand1399)

        val (dayAfter30Esfand1399, _) = JalaliCalendar.nextDay("1399/12/30")
        assertEquals("1400/01/01", dayAfter30Esfand1399)

        val (dayBeforeNowruz1400, _) = JalaliCalendar.previousDay("1400/01/01")
        assertEquals("1399/12/30", dayBeforeNowruz1400)
    }

    @Test
    fun testGregorianJalaliRoundtrip() {
        val testDates = listOf(
            Triple(1403, 1, 1),   // Nowruz 1403
            Triple(1403, 6, 31),  // End of summer
            Triple(1403, 7, 1),   // Start of autumn
            Triple(1399, 12, 30), // Leap day 1399
            Triple(1402, 12, 29), // Normal last day
            Triple(1399, 1, 1),   // Nowruz 1399
            Triple(1405, 5, 15)   // Random date
        )

        for ((jy, jm, jd) in testDates) {
            val (gy, gm, gd) = JalaliCalendar.jalaliToGregorian(jy, jm, jd)
            val convertedBack = JalaliCalendar.gregorianToJalali(gy, gm, gd)
            assertEquals("Year roundtrip mismatch for $jy/$jm/$jd", jy, convertedBack.year)
            assertEquals("Month roundtrip mismatch for $jy/$jm/$jd", jm, convertedBack.month)
            assertEquals("Day roundtrip mismatch for $jy/$jm/$jd", jd, convertedBack.day)
        }
    }

    @Test
    fun testEpochDayRoundtrip() {
        val testDateStrings = listOf(
            "1403/01/01",
            "1403/06/31",
            "1403/07/01",
            "1399/12/30",
            "1402/12/29",
            "1400/10/12"
        )

        for (dateStr in testDateStrings) {
            val epochDay = JalaliCalendar.toEpochDay(dateStr)
            val recoveredJalali = JalaliCalendar.fromEpochDay(epochDay)
            assertEquals("Epoch roundtrip mismatch for $dateStr", dateStr, recoveredJalali.format())
        }
    }

    @Test
    fun testDigitConversion() {
        assertEquals("1403/05/12", JalaliCalendar.toEnglishDigits("۱۴۰۳/۰۵/۱۲"))
        assertEquals("۰۱۲۳۴۵۶۷۸۹", JalaliCalendar.toPersianDigits("0123456789"))
    }
}
