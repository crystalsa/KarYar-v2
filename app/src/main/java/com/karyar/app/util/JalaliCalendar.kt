package com.karyar.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.util.Calendar
import android.icu.util.GregorianCalendar
import android.icu.util.ULocale
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for Persian / Jalali (شمسی) date calculations and formatting
 * backed by ICU's Persian calendar (`android.icu.util.PersianCalendar` via [Calendar.getInstance])
 * and the device's clock/timezone ([TimeZone.getDefault]).
 */
object JalaliCalendar {

    /**
     * Julian day number corresponding to 1970-01-01 (Unix epoch day 0).
     */
    private const val JULIAN_DAY_EPOCH_1970 = 2440588L

    private val PERSIAN_LOCALE = ULocale("fa_IR@calendar=persian")

    private val PERSIAN_MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val DAYS_OF_WEEK = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    data class JalaliDate(
        val year: Int,
        val month: Int, // 1..12
        val day: Int    // 1..31
    ) {
        fun format(): String {
            return String.format(Locale.US, "%04d/%02d/%02d", year, month, day)
        }

        fun formatWithMonthName(): String {
            val monthName = if (month in 1..12) PERSIAN_MONTH_NAMES[month - 1] else "$month"
            return "$day $monthName $year"
        }
    }

    private fun toIcuTimeZone(timeZone: TimeZone = TimeZone.getDefault()): android.icu.util.TimeZone {
        return android.icu.util.TimeZone.getTimeZone(timeZone.id)
    }

    private fun createPersianCalendar(timeZone: TimeZone = TimeZone.getDefault()): Calendar {
        return Calendar.getInstance(toIcuTimeZone(timeZone), PERSIAN_LOCALE)
    }

    private fun createGregorianCalendar(timeZone: TimeZone = TimeZone.getDefault()): GregorianCalendar {
        return GregorianCalendar(toIcuTimeZone(timeZone), ULocale.US)
    }

    /**
     * Reactive [StateFlow] of today's Jalali date string (`YYYY/MM/DD`), updated when:
     * - [Intent.ACTION_DATE_CHANGED], [Intent.ACTION_TIME_CHANGED], or [Intent.ACTION_TIMEZONE_CHANGED] broadcasts arrive
     * - The activity/screen resumes (`ON_RESUME`) via [refreshToday]
     * - [todayString] or [refreshToday] is invoked
     */
    private val _todayFlow = MutableStateFlow(computeTodayString())
    val todayFlow: StateFlow<String> = _todayFlow.asStateFlow()

    private fun computeTodayString(timeZone: TimeZone = TimeZone.getDefault()): String {
        return fromTimestamp(System.currentTimeMillis(), timeZone).format()
    }

    /**
     * Re-reads the current device clock & timezone, updates [todayFlow], and returns the current Jalali date string.
     */
    fun refreshToday(): String {
        val current = computeTodayString(TimeZone.getDefault())
        if (_todayFlow.value != current) {
            _todayFlow.value = current
        }
        return current
    }

    /**
     * Creates an [IntentFilter] for system date, time, and timezone changes.
     */
    fun createTimeChangeIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
    }

    /**
     * Dynamically registers a [BroadcastReceiver] on [context] to keep [todayFlow] updated
     * whenever the date, time, or timezone changes on the device.
     */
    fun registerTimeChangeReceiver(
        context: Context,
        onTodayUpdated: (String) -> Unit = {}
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val updated = refreshToday()
                onTodayUpdated(updated)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            createTimeChangeIntentFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        return receiver
    }

    /**
     * Converts Gregorian timestamp (millis) to Jalali Date using the device's current [TimeZone.getDefault] by default.
     */
    fun fromTimestamp(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): JalaliDate {
        val cal = createPersianCalendar(timeZone)
        cal.timeInMillis = millis
        return JalaliDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Returns today's Jalali date from the device's current clock and [TimeZone.getDefault].
     */
    fun today(timeZone: TimeZone = TimeZone.getDefault()): JalaliDate {
        return fromTimestamp(System.currentTimeMillis(), timeZone)
    }

    /**
     * Returns today's formatted Jalali date string (`YYYY/MM/DD`) from the device's current clock
     * and synchronizes [todayFlow].
     */
    fun todayString(): String {
        return refreshToday()
    }

    /**
     * Converts Gregorian Year, Month (1-12), Day (1-31) to [JalaliDate] using [PersianCalendar].
     */
    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
        val gCal = createGregorianCalendar()
        gCal.isLenient = false
        gCal.clear()
        gCal.set(gYear, gMonth - 1, gDay, 12, 0, 0)
        val julianDay = gCal.get(Calendar.JULIAN_DAY)

        val pCal = createPersianCalendar()
        pCal.clear()
        pCal.set(Calendar.JULIAN_DAY, julianDay)
        return JalaliDate(
            year = pCal.get(Calendar.YEAR),
            month = pCal.get(Calendar.MONTH) + 1,
            day = pCal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Converts Jalali Year, Month (1-12), Day (1-31) to Gregorian (Year, Month, Day) using [PersianCalendar].
     * Throws [IllegalArgumentException] if the Jalali date is invalid.
     */
    fun jalaliToGregorian(jYear: Int, jMonth: Int, jDay: Int): Triple<Int, Int, Int> {
        require(isValidDate(jYear, jMonth, jDay)) {
            "تاریخ شمسی نامعتبر است: ${formatDate(jYear, jMonth, jDay)}"
        }
        val pCal = createPersianCalendar()
        pCal.clear()
        pCal.set(jYear, jMonth - 1, jDay, 12, 0, 0)
        val julianDay = pCal.get(Calendar.JULIAN_DAY)

        val gCal = createGregorianCalendar()
        gCal.clear()
        gCal.set(Calendar.JULIAN_DAY, julianDay)
        return Triple(
            gCal.get(Calendar.YEAR),
            gCal.get(Calendar.MONTH) + 1,
            gCal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Converts Persian digits to English and vice versa if needed.
     */
    fun toPersianDigits(text: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder(text.length)
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
                '۰', '٠' -> sb.append('0')
                '۱', '١' -> sb.append('1')
                '۲', '٢' -> sb.append('2')
                '۳', '٣' -> sb.append('3')
                '۴', '٤' -> sb.append('4')
                '۵', '٥' -> sb.append('5')
                '۶', '٦' -> sb.append('6')
                '۷', '٧' -> sb.append('7')
                '۸', '٨' -> sb.append('8')
                '۹', '٩' -> sb.append('9')
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

    fun getDayOfWeek(millis: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = createPersianCalendar(timeZone)
        cal.timeInMillis = millis
        return getDayOfWeekPersian(cal.get(Calendar.DAY_OF_WEEK))
    }

    /**
     * Returns the Persian day-of-week name for a valid Jalali date string, or `null` if invalid.
     */
    fun getDayOfWeekOrNull(dateStr: String): String? {
        val (jy, jm, jd) = parseDateOrNull(dateStr) ?: return null
        val cal = createPersianCalendar()
        cal.clear()
        cal.set(jy, jm - 1, jd, 12, 0, 0)
        return getDayOfWeekPersian(cal.get(Calendar.DAY_OF_WEEK))
    }

    /**
     * Returns the Persian day-of-week name for a Jalali date string.
     */
    fun getDayOfWeek(dateStr: String): String {
        return getDayOfWeekOrNull(dateStr) ?: todayDayOfWeek()
    }

    fun todayDayOfWeek(): String {
        val (jy, jm, jd) = today().let { Triple(it.year, it.month, it.day) }
        val cal = createPersianCalendar()
        cal.clear()
        cal.set(jy, jm - 1, jd, 12, 0, 0)
        return getDayOfWeekPersian(cal.get(Calendar.DAY_OF_WEEK))
    }

    /**
     * Checks if a Jalali year is a leap year (کبیسه) according to [PersianCalendar].
     */
    fun isLeapYear(year: Int): Boolean {
        return getDaysInMonth(year, 12) == 30
    }

    /**
     * Returns the exact number of days in a Jalali month (1..12) of the given [year]
     * using [PersianCalendar].
     * Throws [IllegalArgumentException] if [month] is not in 1..12.
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        require(month in 1..12) { "ماه شمسی باید بین ۱ تا ۱۲ باشد: $month" }
        val cal = createPersianCalendar()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getMonthName(month: Int): String {
        return if (month in 1..12) PERSIAN_MONTH_NAMES[month - 1] else "$month"
    }

    /**
     * Checks whether the given Jalali (year, month, day) components form a valid date.
     */
    fun isValidDate(year: Int, month: Int, day: Int): Boolean {
        if (year !in 1..9999) return false
        if (month !in 1..12) return false
        val maxDays = getDaysInMonth(year, month)
        return day in 1..maxDays
    }

    /**
     * Checks whether [dateStr] is a valid Jalali date (`YYYY/MM/DD` or `YYYY-MM-DD`).
     */
    fun isValidDate(dateStr: String): Boolean {
        return parseDateOrNull(dateStr) != null
    }

    /**
     * Parses a Jalali date string into `Triple(year, month, day)` if and only if it is valid;
     * returns `null` without silent coercion if the format or calendar date is invalid.
     */
    fun parseDateOrNull(dateStr: String): Triple<Int, Int, Int>? {
        if (dateStr.isBlank()) return null
        val eng = toEnglishDigits(dateStr.trim())
        val parts = eng.replace('-', '/').split('/')
        if (parts.size != 3) return null
        val jy = parts[0].trim().toIntOrNull() ?: return null
        val jm = parts[1].trim().toIntOrNull() ?: return null
        val jd = parts[2].trim().toIntOrNull() ?: return null
        if (!isValidDate(jy, jm, jd)) return null
        return Triple(jy, jm, jd)
    }

    /**
     * Validates [dateStr] and returns a user-facing Persian error message if invalid,
     * or `null` if [dateStr] is a valid Jalali date.
     */
    fun validateDateError(dateStr: String): String? {
        if (dateStr.isBlank()) {
            return "لطفاً تاریخ را وارد کنید"
        }
        val eng = toEnglishDigits(dateStr.trim())
        val parts = eng.replace('-', '/').split('/')
        if (parts.size != 3) {
            return "فرمت تاریخ نامعتبر است (مثال صحیح: ۱۴۰۳/۱۲/۲۹)"
        }
        val jy = parts[0].trim().toIntOrNull()
        val jm = parts[1].trim().toIntOrNull()
        val jd = parts[2].trim().toIntOrNull()
        if (jy == null || jm == null || jd == null) {
            return "فرمت تاریخ نامعتبر است (مثال صحیح: ۱۴۰۳/۱۲/۲۹)"
        }
        if (jy !in 1..9999) {
            return "سال واردشده نامعتبر است"
        }
        if (jm !in 1..12) {
            return "ماه باید بین ۱ تا ۱۲ باشد"
        }
        val maxDays = getDaysInMonth(jy, jm)
        if (jd < 1 || jd > maxDays) {
            val persianYear = toPersianDigits(jy.toString())
            return if (jm == 12 && jd == 30 && !isLeapYear(jy)) {
                "سال $persianYear کبیسه نیست و اسفند آن ۲۹ روزه است"
            } else {
                val persianMax = toPersianDigits(maxDays.toString())
                "ماه ${getMonthName(jm)} در سال $persianYear، $persianMax روزه است (روز باید بین ۱ تا $persianMax باشد)"
            }
        }
        return null
    }

    /**
     * Strictly parses a Jalali date string (`YYYY/MM/DD` or `YYYY-MM-DD`) into `Triple(year, month, day)`.
     * Never silently coerces invalid values; throws [IllegalArgumentException] if the date is invalid.
     */
    fun parseDate(dateStr: String): Triple<Int, Int, Int> {
        return parseDateOrNull(dateStr)
            ?: throw IllegalArgumentException(validateDateError(dateStr) ?: "تاریخ شمسی نامعتبر است: $dateStr")
    }

    /**
     * Converts a valid Jalali date string (e.g. `1403/12/30`) to epochDay (days since 1970-01-01)
     * using [PersianCalendar].
     * Throws [IllegalArgumentException] if [dateStr] is invalid (never returns 0 on error).
     */
    fun toEpochDay(dateStr: String): Long {
        val (jy, jm, jd) = parseDate(dateStr)
        val cal = createPersianCalendar()
        cal.clear()
        cal.set(jy, jm - 1, jd, 12, 0, 0)
        return cal.get(Calendar.JULIAN_DAY).toLong() - JULIAN_DAY_EPOCH_1970
    }

    /**
     * Safe variant of [toEpochDay] that returns `null` if [dateStr] is not a valid Jalali date.
     */
    fun toEpochDayOrNull(dateStr: String): Long? {
        val (jy, jm, jd) = parseDateOrNull(dateStr) ?: return null
        val cal = createPersianCalendar()
        cal.clear()
        cal.set(jy, jm - 1, jd, 12, 0, 0)
        return cal.get(Calendar.JULIAN_DAY).toLong() - JULIAN_DAY_EPOCH_1970
    }

    /**
     * Safe epochDay resolver that never throws on legacy invalid dates:
     * returns [toEpochDayOrNull] if valid, otherwise [recalculateLegacyEpochDayFallback], or `0L`.
     */
    fun safeEpochDay(dateStr: String): Long {
        return toEpochDayOrNull(dateStr) ?: recalculateLegacyEpochDayFallback(dateStr) ?: 0L
    }

    /**
     * Repairs a legacy invalid Jalali date string (such as `1404/12/30` created by the old
     * leap-year algorithm) into a valid canonical `YYYY/MM/DD` string.
     *
     * Rules:
     * - Returns `null` if [dateStr] is already valid (`parseDateOrNull(dateStr) != null`) or unparseable.
     * - If `month == 12 && day == 30 && !isLeapYear(year)`: moves to `(year + 1)/01/01` because
     *   in the old calendar `12/30` of a non-leap year was reached by "Next Day" after `12/29`
     *   and represented the Gregorian day corresponding to `01/01` of the next year.
     * - For any other invalid month/day: clamps month to `1..12` and day to `1..getDaysInMonth(year, month)`.
     */
    fun fixLegacyInvalidDate(dateStr: String): String? {
        if (parseDateOrNull(dateStr) != null) return null
        val eng = toEnglishDigits(dateStr.trim())
        val parts = eng.replace('-', '/').split('/')
        if (parts.size != 3) return null
        val jy = parts[0].trim().toIntOrNull() ?: return null
        val rawMonth = parts[1].trim().toIntOrNull() ?: return null
        val rawDay = parts[2].trim().toIntOrNull() ?: return null
        if (jy !in 1..9998) return null

        if (rawMonth == 12 && rawDay == 30 && !isLeapYear(jy)) {
            return formatDate(jy + 1, 1, 1)
        }

        val safeMonth = rawMonth.coerceIn(1, 12)
        val maxDays = getDaysInMonth(jy, safeMonth)
        val safeDay = rawDay.coerceIn(1, maxDays)
        return formatDate(jy, safeMonth, safeDay)
    }

    /**
     * Used only during database migration (e.g. 7->8) if a legacy row stored a date like `1404/12/30`
     * that was accepted under the old broken leap-year algorithm. Clamps to the valid month end so
     * its epochDay can still be recalculated without crashing migration.
     */
    internal fun recalculateLegacyEpochDayFallback(dateStr: String): Long? {
        val eng = toEnglishDigits(dateStr.trim())
        val parts = eng.replace('-', '/').split('/')
        if (parts.size != 3) return null
        val jy = parts[0].trim().toIntOrNull() ?: return null
        val jm = parts[1].trim().toIntOrNull()?.coerceIn(1, 12) ?: return null
        if (jy !in 1..9999) return null
        val maxDays = getDaysInMonth(jy, jm)
        val jd = parts[2].trim().toIntOrNull()?.coerceIn(1, maxDays) ?: return null
        val cal = createPersianCalendar()
        cal.clear()
        cal.set(jy, jm - 1, jd, 12, 0, 0)
        return cal.get(Calendar.JULIAN_DAY).toLong() - JULIAN_DAY_EPOCH_1970
    }

    /**
     * Converts epochDay (days since 1970-01-01) to [JalaliDate] using [PersianCalendar].
     */
    fun fromEpochDay(epochDay: Long): JalaliDate {
        val cal = createPersianCalendar()
        cal.clear()
        cal.set(Calendar.JULIAN_DAY, (epochDay + JULIAN_DAY_EPOCH_1970).toInt())
        return JalaliDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun formatDate(year: Int, month: Int, day: Int): String {
        return String.format(Locale.US, "%04d/%02d/%02d", year, month, day)
    }

    /**
     * Advances to the next day strictly following [PersianCalendar]:
     * 31 Shahrivar -> 1 Mehr
     * 29/30 Esfand -> 1 Farvardin of next year
     */
    fun nextDay(dateStr: String): Pair<String, String> {
        return addDays(dateStr, 1)
    }

    /**
     * Safe variant of [nextDay] that returns `null` instead of throwing if [dateStr] is invalid.
     */
    fun nextDayOrNull(dateStr: String): Pair<String, String>? {
        return addDaysOrNull(dateStr, 1)
    }

    /**
     * Moves to the previous day strictly following [PersianCalendar]:
     * 1 Mehr -> 31 Shahrivar
     * 1 Farvardin -> 29/30 Esfand of previous year
     */
    fun previousDay(dateStr: String): Pair<String, String> {
        return addDays(dateStr, -1)
    }

    /**
     * Safe variant of [previousDay] that returns `null` instead of throwing if [dateStr] is invalid.
     */
    fun previousDayOrNull(dateStr: String): Pair<String, String>? {
        return addDaysOrNull(dateStr, -1)
    }

    /**
     * Safe variant of [addDays] that returns `null` instead of throwing if [dateStr] is invalid.
     */
    fun addDaysOrNull(dateStr: String, daysToAdd: Int): Pair<String, String>? {
        val (y, m, d) = parseDateOrNull(dateStr) ?: return null
        val cal = createPersianCalendar()
        cal.clear()
        cal.set(y, m - 1, d, 12, 0, 0)
        if (daysToAdd != 0) {
            cal.add(Calendar.DAY_OF_MONTH, daysToAdd)
        }
        val targetDateStr = formatDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val targetDow = getDayOfWeekPersian(cal.get(Calendar.DAY_OF_WEEK))
        return Pair(targetDateStr, targetDow)
    }

    /**
     * Adds [daysToAdd] days (positive or negative) to [dateStr] using [PersianCalendar].
     * Throws [IllegalArgumentException] if [dateStr] is invalid.
     */
    fun addDays(dateStr: String, daysToAdd: Int): Pair<String, String> {
        val (y, m, d) = parseDate(dateStr)
        val cal = createPersianCalendar()
        cal.clear()
        cal.set(y, m - 1, d, 12, 0, 0)
        if (daysToAdd != 0) {
            cal.add(Calendar.DAY_OF_MONTH, daysToAdd)
        }
        val targetDateStr = formatDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val targetDow = getDayOfWeekPersian(cal.get(Calendar.DAY_OF_WEEK))
        return Pair(targetDateStr, targetDow)
    }
}
