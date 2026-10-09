package com.karyar.app.data.local.entity

/**
 * Represents the official attendance status of a worker for a specific day.
 * Separated from free-form user notes.
 */
enum class AttendanceStatus {
    FULL_DAY,
    HALF_DAY,
    HOURLY,
    ABSENT
}
