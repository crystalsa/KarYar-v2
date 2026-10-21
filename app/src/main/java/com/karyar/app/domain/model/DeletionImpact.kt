package com.karyar.app.domain.model

import com.karyar.app.util.Formatters

/**
 * خلاصه سوابق حضور و مبلغ خالص تسویه‌ای که در صورت حذف کارگر، روز کاری یا پوشه کارگاه از بین می‌رود.
 *
 * @property attendanceDaysCount تعداد روزهای حضور (نفر-روز کاری غیرغایب)
 * @property netSettlementAmount مبلغ خالص تسویه (به تومان، مقدار خام؛ در صورت منفی بودن نشان‌دهنده بدهی است)
 * @property totalAttendanceRecordsCount تعداد کل رکوردهای تردد ثبت‌شده (شامل روزهای غیبت)
 */
data class DeletionImpact(
    val attendanceDaysCount: Int = 0,
    val netSettlementAmount: Long = 0L,
    val totalAttendanceRecordsCount: Int = attendanceDaysCount
) {
    val hasHistory: Boolean
        get() = totalAttendanceRecordsCount > 0 || attendanceDaysCount > 0 || netSettlementAmount != 0L

    fun formatSettlementText(): String {
        return if (netSettlementAmount < 0L) {
            "${Formatters.formatCurrency(-netSettlementAmount)} (بدهکار)"
        } else {
            Formatters.formatCurrency(netSettlementAmount)
        }
    }
}
