package com.karyar.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Formatters {

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /**
     * Converts any Latin digits (0-9) to Persian digits (۰-۹)
     */
    fun toPersianDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(PERSIAN_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Int): String = toPersianDigits(number.toString())
    fun toPersianDigits(number: Long): String = toPersianDigits(number.toString())
    fun toPersianDigits(number: Double): String = toPersianDigits(if (number % 1.0 == 0.0) number.toLong().toString() else "%.1f".format(Locale.US, number))
    fun toPersianDigits(number: Number): String = toPersianDigits(number.toString())

    /**
     * Converts Persian digits to English digits
     */
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

    /**
     * Formats number with 3-digit commas in Persian digits (e.g. ۱,۲۵۰,۰۰۰)
     */
    fun formatThousandsPersian(amount: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val df = DecimalFormat("#,###", symbols)
        val formatted = if (amount == 0L) "0" else df.format(amount)
        return toPersianDigits(formatted)
    }

    fun formatNumber(number: Long): String = formatThousandsPersian(number)
    fun formatNumber(number: Int): String = formatThousandsPersian(number.toLong())

    /**
     * Formats currency with commas and currency unit in Persian (e.g. ۱,۲۵۰,۰۰۰ تومان)
     */
    fun formatCurrency(amount: Long, unit: String = "تومان"): String {
        return "${formatThousandsPersian(amount)} $unit"
    }

    /**
     * Converts "08:30" string to total minutes from midnight
     */
    fun timeToMinutes(timeStr: String): Int {
        val engTime = toEnglishDigits(timeStr).trim()
        val parts = engTime.split(":")
        if (parts.size != 2) return 0
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return h * 60 + m
    }

    /**
     * Converts minutes to "HH:mm" formatted string in Persian digits
     */
    fun minutesToTime(totalMinutes: Int): String {
        val normalized = if (totalMinutes < 0) 0 else totalMinutes % (24 * 60)
        val h = normalized / 60
        val m = normalized % 60
        return toPersianDigits(String.format("%02d:%02d", h, m))
    }

    /**
     * Formats decimal hours e.g. 8.5 to "۸ ساعت و ۳۰ دقیقه" or "۸ ساعت"
     */
    fun formatHours(hours: Double): String {
        val wholeHours = hours.toInt()
        val minutes = ((hours - wholeHours) * 60).toInt()
        val res = if (minutes > 0) {
            "$wholeHours ساعت و $minutes دقیقه"
        } else {
            "$wholeHours ساعت"
        }
        return toPersianDigits(res)
    }

    /**
     * Formats price / amount while typing in input fields with 3-digit comma separators.
     * Starts empty if input has no digits.
     */
    fun formatPriceInput(input: String): String {
        val rawEnglish = toEnglishDigits(input)
        val digitsOnly = rawEnglish.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""
        val num = digitsOnly.toLongOrNull() ?: return ""
        return formatThousandsPersian(num)
    }

    /**
     * Parses numeric amount from a formatted price string (handles Persian digits and commas)
     */
    fun parsePrice(input: String): Long {
        val rawEnglish = toEnglishDigits(input)
        val digitsOnly = rawEnglish.filter { it.isDigit() }
        return digitsOnly.toLongOrNull() ?: 0L
    }

    /**
     * Parses Double from string with Persian or English digits
     */
    fun parseDouble(input: String): Double {
        val raw = toEnglishDigits(input).trim()
            .replace(",", ".")
            .replace("/", ".")
            .replace("٫", ".")
        return raw.toDoubleOrNull() ?: 0.0
    }

    /**
     * Parses Int from string with Persian or English digits
     */
    fun parseInt(input: String): Int {
        val raw = toEnglishDigits(input).filter { it.isDigit() || it == '-' }
        return raw.toIntOrNull() ?: 0
    }
}
