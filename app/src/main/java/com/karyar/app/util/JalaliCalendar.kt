package com.karyar.app.util

import java.util.Calendar
import java.util.Date

/**
 * Utility for Persian / Jalali (شمسی) date calculations and formatting.
 */
object JalaliCalendar {

    private val PERSIAN_MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    data class JalaliDate(
        val year: Int,
        val month: Int, // 1..12
        val day: Int    // 1..31
    ) {
        fun format(): String {
            return String.format("%04d/%02d/%02d", year, month, day)
        }

        fun formatWithMonthName(): String {
            val monthName = if (month in 1..12) PERSIAN_MONTH_NAMES[month - 1] else "$month"
            return "$day $monthName $year"
        }
    }

    /**
     * Converts Gregorian timestamp (millis) to Jalali Date
     */
    fun fromTimestamp(millis: Long): JalaliDate {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Returns today's Jalali date
     */
    fun today(): JalaliDate {
        return fromTimestamp(System.currentTimeMillis())
    }

    fun todayString(): String = today().format()

    /**
     * Converts Gregorian Year, Month (1-12), Day (1-31) to JalaliDate
     */
    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        val gy = gYear - 1600
        val gm = gMonth - 1
        val gd = gDay - 1

        var gDayNo = 365 * gy + ((gy + 3) / 4) - ((gy + 99) / 100) + ((gy + 399) / 400)
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += ((jDayNo - 1) / 365)
            jDayNo = (jDayNo - 1) % 365
        }

        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, if (isLeapYear(jy)) 30 else 29)

        var jm = 0
        var jd = 0
        for (i in 0..11) {
            val days = jDaysInMonth[i]
            if (jDayNo < days) {
                jm = i + 1
                jd = jDayNo + 1
                break
            }
            jDayNo -= days
        }
        if (jm == 0) {
            jm = 12
            jd = jDayNo + 1
        }

        return JalaliDate(jy, jm, jd)
    }

    /**
     * Converts Persian digits to English and vice versa if needed
     */
    fun toPersianDigits(text: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '۰' -> sb.append('0')
                '۱' -> sb.append('1')
                '۲' -> sb.append('2')
                '۳' -> sb.append('3')
                '۴' -> sb.append('4')
                '۵' -> sb.append('5')
                '۶' -> sb.append('6')
                '۷' -> sb.append('7')
                '۸' -> sb.append('8')
                '۹' -> sb.append('9')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun getDayOfWeekPersian(calendarDayOfWeek: Int): String {
        return when (calendarDayOfWeek) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> "شنبه"
        }
    }

    fun getDayOfWeek(millis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return getDayOfWeekPersian(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun jalaliToGregorian(jYear: Int, jMonth: Int, jDay: Int): Triple<Int, Int, Int> {
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        val jy = jYear - 979
        val jm = jMonth - 1
        val jd = jDay - 1

        var jDayNo = 365 * jy + (jy / 33) * 8 + ((jy % 33 + 3) / 4)
        for (i in 0 until jm) {
            jDayNo += jDaysInMonth[i]
        }
        jDayNo += jd

        var gDayNo = jDayNo + 79
        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524

            if (gDayNo >= 365) {
                gDayNo++
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        val isLeap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        val gDaysInMonth = intArrayOf(31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 12 && gDayNo >= gDaysInMonth[gm]) {
            gDayNo -= gDaysInMonth[gm]
            gm++
        }
        val gd = gDayNo + 1
        return Triple(gy, gm + 1, gd)
    }

    /**
     * Converts a Jalali date string (e.g. 1405/01/15) to epochDay (days since 1970-01-01 UTC).
     */
    fun toEpochDay(dateStr: String): Long {
        return try {
            val (jy, jm, jd) = parseDate(dateStr)
            val (gy, gm, gd) = jalaliToGregorian(jy, jm, jd)
            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            cal.clear()
            cal.set(gy, gm - 1, gd)
            cal.timeInMillis / 86400000L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Converts epochDay to JalaliDate.
     */
    fun fromEpochDay(epochDay: Long): JalaliDate {
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.timeInMillis = epochDay * 86400000L
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getDayOfWeek(dateStr: String): String {
        return try {
            val eng = toEnglishDigits(dateStr)
            val parts = eng.replace('-', '/').split('/')
            if (parts.size == 3) {
                val jy = parts[0].trim().toInt()
                val jm = parts[1].trim().toInt()
                val jd = parts[2].trim().toInt()
                val (gy, gm, gd) = jalaliToGregorian(jy, jm, jd)
                val cal = Calendar.getInstance()
                cal.set(gy, gm - 1, gd)
                getDayOfWeekPersian(cal.get(Calendar.DAY_OF_WEEK))
            } else {
                todayDayOfWeek()
            }
        } catch (e: Exception) {
            todayDayOfWeek()
        }
    }

    val DAYS_OF_WEEK = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    fun todayDayOfWeek(): String = getDayOfWeek()

    /**
     * Checks if a Jalali year is a leap year (کبیسه).
     */
    fun isLeapYear(year: Int): Boolean {
        val a = year - 474
        val b = (a % 2820 + 2820) % 2820 + 474
        return ((b + 38) * 682) % 2816 < 682
    }

    /**
     * Returns exact days count for any Jalali month:
     * 1..6 (فروردین تا شهریور): 31 days
     * 7..11 (مهر تا بهمن): 30 days
     * 12 (اسفند): 29 days (30 in leap years)
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    fun getMonthName(month: Int): String {
        return if (month in 1..12) PERSIAN_MONTH_NAMES[month - 1] else "$month"
    }

    fun parseDate(dateStr: String): Triple<Int, Int, Int> {
        val eng = toEnglishDigits(dateStr)
        val parts = eng.replace('-', '/').split('/')
        val jy = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: today().year
        val jm = (parts.getOrNull(1)?.trim()?.toIntOrNull() ?: today().month).coerceIn(1, 12)
        val maxDays = getDaysInMonth(jy, jm)
        val jd = (parts.getOrNull(2)?.trim()?.toIntOrNull() ?: today().day).coerceIn(1, maxDays)
        return Triple(jy, jm, jd)
    }

    fun formatDate(year: Int, month: Int, day: Int): String {
        return String.format("%04d/%02d/%02d", year, month, day)
    }

    /**
     * Advances to the next day strictly following Persian calendar:
     * 31 Shahrivar -> 1 Mehr (never 32 Shahrivar)
     * 29/30 Esfand -> 1 Farvardin of next year
     */
    fun nextDay(dateStr: String): Pair<String, String> {
        val (y, m, d) = parseDate(dateStr)
        val daysInM = getDaysInMonth(y, m)
        val (nextY, nextM, nextD) = when {
            d < daysInM -> Triple(y, m, d + 1)
            m < 12 -> Triple(y, m + 1, 1)
            else -> Triple(y + 1, 1, 1)
        }
        val nextDateStr = formatDate(nextY, nextM, nextD)
        val nextDow = getDayOfWeek(nextDateStr)
        return Pair(nextDateStr, nextDow)
    }

    /**
     * Moves to the previous day strictly following Persian calendar:
     * 1 Mehr -> 31 Shahrivar
     * 1 Farvardin -> 29/30 Esfand of previous year
     */
    fun previousDay(dateStr: String): Pair<String, String> {
        val (y, m, d) = parseDate(dateStr)
        val (prevY, prevM, prevD) = when {
            d > 1 -> Triple(y, m, d - 1)
            m > 1 -> Triple(y, m - 1, getDaysInMonth(y, m - 1))
            else -> Triple(y - 1, 12, getDaysInMonth(y - 1, 12))
        }
        val prevDateStr = formatDate(prevY, prevM, prevD)
        val prevDow = getDayOfWeek(prevDateStr)
        return Pair(prevDateStr, prevDow)
    }

    fun addDays(dateStr: String, daysToAdd: Int): Pair<String, String> {
        var current = dateStr
        if (daysToAdd > 0) {
            repeat(daysToAdd) {
                current = nextDay(current).first
            }
        } else if (daysToAdd < 0) {
            repeat(-daysToAdd) {
                current = previousDay(current).first
            }
        }
        return Pair(current, getDayOfWeek(current))
    }
}
