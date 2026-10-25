package com.karyar.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Formatters {

    const val MAX_WAGE_AMOUNT: Long = 1_000_000_000L
    const val MAX_DAILY_WAGE: Long = MAX_WAGE_AMOUNT
    const val MAX_HOURLY_RATE: Long = MAX_WAGE_AMOUNT
    const val MAX_OVERTIME_RATE: Long = MAX_WAGE_AMOUNT
    const val MAX_ALLOWANCE_AMOUNT: Long = MAX_WAGE_AMOUNT

    const val MAX_REGULAR_HOURS: Double = 24.0
    const val MAX_HOURLY_HOURS: Double = 24.0
    const val MAX_OVERTIME_HOURS: Double = 24.0
    const val MAX_WORK_HOURS: Double = 24.0

    const val MAX_NAME_LENGTH: Int = 60
    const val MAX_PRICE_INPUT_DIGITS: Int = 13

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
        val safeHours = if (!hours.isFinite() || hours <= 0.0) 0.0 else hours
        val totalMinutes = Math.round(safeHours * 60.0).coerceAtLeast(0L)
        val wholeHours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        val res = if (minutes > 0L) {
            "$wholeHours ساعت و $minutes دقیقه"
        } else {
            "$wholeHours ساعت"
        }
        return toPersianDigits(res)
    }

    /**
     * Formats price / amount while typing in input fields with 3-digit comma separators.
     * Starts empty if input has no digits.
     * Caps raw input length to [MAX_PRICE_INPUT_DIGITS] so Long never overflows while still allowing
     * values above [MAX_WAGE_AMOUNT] to be entered and flagged by validation.
     */
    fun formatPriceInput(input: String): String {
        val rawEnglish = toEnglishDigits(input)
        val digitsOnly = rawEnglish.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""
        val normalizedDigits = digitsOnly.trimStart('0').ifEmpty { "0" }.take(MAX_PRICE_INPUT_DIGITS)
        val num = normalizedDigits.toLongOrNull() ?: return ""
        return formatThousandsPersian(num)
    }

    /**
     * Parses numeric amount from a formatted price string (handles Persian digits and commas).
     * Preserves negative sign if present, and returns Long.MAX_VALUE / Long.MIN_VALUE on >64-bit
     * digit sequences so upper/lower bound validators catch the invalid input instead of returning 0.
     */
    fun parsePrice(input: String): Long {
        val rawEnglish = toEnglishDigits(input).trim()
        val isNegative = rawEnglish.startsWith("-")
        val digitsOnly = rawEnglish.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return 0L
        val normalizedDigits = digitsOnly.trimStart('0').ifEmpty { "0" }
        val parsed = normalizedDigits.toLongOrNull()
        return when {
            parsed == null -> if (isNegative) Long.MIN_VALUE else Long.MAX_VALUE
            isNegative -> -parsed
            else -> parsed
        }
    }

    /**
     * Parses Double from string with Persian or English digits.
     * Guards against NaN and Infinity.
     */
    fun parseDouble(input: String): Double {
        val raw = toEnglishDigits(input).trim()
            .replace(",", ".")
            .replace("/", ".")
            .replace("٫", ".")
        val value = raw.toDoubleOrNull() ?: return 0.0
        return if (value.isFinite()) value else 0.0
    }

    /**
     * Parses Int from string with Persian or English digits
     */
    fun parseInt(input: String): Int {
        val raw = toEnglishDigits(input).filter { it.isDigit() || it == '-' }
        return raw.toIntOrNull() ?: 0
    }

    /**
     * اعتبارسنجی نام یا عنوان اجباری (غیرخالی و حداکثر [MAX_NAME_LENGTH] کاراکتر).
     */
    fun validateRequiredName(
        name: String,
        fieldLabel: String = "نام",
        maxLength: Int = MAX_NAME_LENGTH
    ): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return "$fieldLabel نمی‌تواند خالی باشد"
        }
        if (trimmed.length > maxLength || name.length > maxLength) {
            return "طول $fieldLabel نباید بیشتر از ${toPersianDigits(maxLength)} کاراکتر باشد"
        }
        return null
    }

    /**
     * اعتبارسنجی نام یا عنوان اختیاری (حداکثر [MAX_NAME_LENGTH] کاراکتر).
     */
    fun validateOptionalName(
        text: String,
        fieldLabel: String = "عنوان",
        maxLength: Int = MAX_NAME_LENGTH
    ): String? {
        if (text.trim().length > maxLength || text.length > maxLength) {
            return "طول $fieldLabel نباید بیشتر از ${toPersianDigits(maxLength)} کاراکتر باشد"
        }
        return null
    }

    /**
     * اعتبارسنجی مبلغ عددی (دستمزد روزانه، نرخ ساعتی، نرخ اضافه‌کاری، کمک‌هزینه و کسورات).
     */
    fun validateWageAmount(
        amount: Long,
        fieldLabel: String = "مبلغ",
        maxAmount: Long = MAX_WAGE_AMOUNT,
        allowZero: Boolean = true
    ): String? {
        if (amount < 0L) {
            return "$fieldLabel نمی‌تواند منفی باشد"
        }
        if (!allowZero && amount == 0L) {
            return "$fieldLabel باید بیشتر از صفر باشد"
        }
        if (amount > maxAmount) {
            return "$fieldLabel نباید بیشتر از ${formatCurrency(maxAmount)} باشد"
        }
        return null
    }

    /**
     * اعتبارسنجی رشته ورودی مبلغ در فرم‌ها.
     */
    fun validateWageInput(
        input: String,
        fieldLabel: String = "مبلغ",
        maxAmount: Long = MAX_WAGE_AMOUNT,
        allowZero: Boolean = true,
        allowEmpty: Boolean = true
    ): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return if (allowEmpty) null else "$fieldLabel نمی‌تواند خالی باشد"
        }
        val rawEnglish = toEnglishDigits(trimmed)
        if (rawEnglish.startsWith("-")) {
            return "$fieldLabel نمی‌تواند منفی باشد"
        }
        val digitsOnly = rawEnglish.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) {
            return "فرمت $fieldLabel نامعتبر است"
        }
        val amount = parsePrice(trimmed)
        return validateWageAmount(
            amount = amount,
            fieldLabel = fieldLabel,
            maxAmount = maxAmount,
            allowZero = allowZero
        )
    }

    /**
     * اعتبارسنجی مقدار ساعت (عادی، ساعتی، اضافه‌کاری) بین ۰ تا [MAX_WORK_HOURS] (۲۴ ساعت).
     */
    fun validateHours(
        hours: Double,
        fieldLabel: String = "ساعت",
        maxHours: Double = MAX_WORK_HOURS
    ): String? {
        if (!hours.isFinite()) {
            return "مقدار $fieldLabel نامعتبر است"
        }
        if (hours < 0.0) {
            return "$fieldLabel نمی‌تواند منفی باشد"
        }
        if (hours > maxHours) {
            return "$fieldLabel نباید بیشتر از ${toPersianDigits(maxHours)} ساعت باشد"
        }
        return null
    }

    /**
     * اعتبارسنجی رشته ورودی ساعت در فرم‌ها.
     */
    fun validateHoursInput(
        input: String,
        fieldLabel: String = "ساعت",
        maxHours: Double = MAX_WORK_HOURS,
        allowEmpty: Boolean = true
    ): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return if (allowEmpty) null else "$fieldLabel نمی‌تواند خالی باشد"
        }
        val normalized = toEnglishDigits(trimmed)
            .replace(",", ".")
            .replace("/", ".")
            .replace("٫", ".")
        val value = normalized.toDoubleOrNull()
            ?: return "فرمت $fieldLabel نامعتبر است"
        return validateHours(value, fieldLabel, maxHours)
    }

    /**
     * اعتبارسنجی کد ملی ۱۰ رقمی ایران بر اساس الگوریتم رسمی رقم کنترل (Check Digit):
     * ۱. دقیقاً ۱۰ رقم عددی باشد (پشتیبانی از ارقام فارسی و انگلیسی).
     * ۲. تمام ارقام یکسان نباشند (مانند 0000000000 یا 1111111111).
     * ۳. مجموع وزن‌دار ۹ رقم اول (ضرایب ۱۰ تا ۲) تقسیم بر ۱۱:
     *    - اگر باقیمانده < ۲ باشد، رقم کنترل (رقم دهم) باید برابر باقیمانده باشد.
     *    - اگر باقیمانده >= ۲ باشد، رقم کنترل باید برابر (۱۱ منهای باقیمانده) باشد.
     */
    fun isValidIranianNationalId(input: String): Boolean {
        val normalized = toEnglishDigits(input).trim()
        if (normalized.length != 10 || !normalized.all { it.isDigit() }) {
            return false
        }
        if (normalized.all { it == normalized[0] }) {
            return false
        }
        var sum = 0
        for (i in 0 until 9) {
            val digit = normalized[i] - '0'
            val weight = 10 - i
            sum += digit * weight
        }
        val remainder = sum % 11
        val checkDigit = normalized[9] - '0'
        return if (remainder < 2) {
            checkDigit == remainder
        } else {
            checkDigit == (11 - remainder)
        }
    }

    /**
     * برگرداندن پیام خطای فارسی برای کد ملی ایران، یا null در صورت معتبر بودن.
     */
    fun validateIranianNationalId(
        input: String,
        allowEmpty: Boolean = true
    ): String? {
        val normalized = toEnglishDigits(input).trim()
        if (normalized.isEmpty()) {
            return if (allowEmpty) null else "کد ملی نمی‌تواند خالی باشد"
        }
        if (!normalized.all { it.isDigit() }) {
            return "کد ملی باید فقط شامل ارقام باشد"
        }
        if (normalized.length != 10) {
            return "کد ملی ایران باید دقیقاً ۱۰ رقم باشد"
        }
        if (normalized.all { it == normalized[0] }) {
            return "کد ملی واردشده نامعتبر است (ارقام تکراری)"
        }
        if (!isValidIranianNationalId(normalized)) {
            return "کد ملی واردشده معتبر نیست (رقم کنترل نادرست است)"
        }
        return null
    }
}
