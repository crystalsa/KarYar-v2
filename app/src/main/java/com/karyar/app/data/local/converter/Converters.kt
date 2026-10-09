package com.karyar.app.data.local.converter

import androidx.room.TypeConverter
import com.karyar.app.data.local.entity.AttendanceStatus

class Converters {
    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus?): String {
        return status?.name ?: AttendanceStatus.FULL_DAY.name
    }

    @TypeConverter
    fun toAttendanceStatus(value: String?): AttendanceStatus {
        return value?.let {
            try {
                AttendanceStatus.valueOf(it)
            } catch (e: Exception) {
                when (it) {
                    "تمام روز" -> AttendanceStatus.FULL_DAY
                    "نصف روز" -> AttendanceStatus.HALF_DAY
                    "ساعتی" -> AttendanceStatus.HOURLY
                    "غیبت" -> AttendanceStatus.ABSENT
                    else -> AttendanceStatus.FULL_DAY
                }
            }
        } ?: AttendanceStatus.FULL_DAY
    }
}
