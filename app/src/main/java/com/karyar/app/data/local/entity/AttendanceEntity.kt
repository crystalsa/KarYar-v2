package com.karyar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.karyar.app.util.JalaliCalendar

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = WorkplaceFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["workerId"]),
        Index(value = ["dateFolderId"]),
        Index(value = ["date"]),
        Index(value = ["epochDay"]),
        Index(value = ["workerId", "date"], unique = true)
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long = 0L,            // شناسه پوشه محل کار
    val workerId: Long,
    val dateFolderId: Long = 0L,        // شناسه پوشه تاریخ
    val date: String,                   // تاریخ شمسی مثلا 1405/01/15
    val epochDay: Long = JalaliCalendar.toEpochDay(date), // تاریخ به صورت epochDay
    val status: AttendanceStatus = AttendanceStatus.FULL_DAY, // وضعیت حضور: FULL_DAY, HALF_DAY, HOURLY, ABSENT
    val timestamp: Long = System.currentTimeMillis(),
    val entryTime: String = "08:00",    // ساعت ورود
    val exitTime: String = "17:00",     // ساعت خروج
    val regularHours: Double = 8.0,     // ساعت کار عادی
    val overtimeHours: Double = 0.0,    // اضافه کاری به ساعت
    val overtimeRate: Long = 0L,        // نرخ هر ساعت اضافه کاری (تومان)
    val hourlyWageRate: Long = 0L,      // نرخ هر ساعت کارکرد ساعتی (تومان)
    val hourlyHours: Double = 0.0,      // تعداد ساعت کارکرد ساعتی
    val dailyWage: Long = 0L,           // دستمزد روزانه منظور شده (تومان)
    val hourlyWage: Long = 0L,          // دستمزد ساعتی منظور شده (تومان)
    val workplaceName: String = "",     // نام محل کار / پروژه
    val employerName: String = "",      // نام کارفرما
    val foremanName: String = "",       // نام سرکارگر
    val notes: String = "",             // توضیحات آزاد کاربر

    // تنظیمات اختصاصی هزینه‌ها و مزایای این روز کاری (مستقل از روزهای دیگر)
    val transitAllowance: Long = 0L,
    val transitImpact: String = "ALLOWANCE",
    val foodAllowance: Long = 0L,
    val foodImpact: String = "ALLOWANCE",
    val accommodationAllowance: Long = 0L,
    val accommodationImpact: String = "ALLOWANCE",
    val medicalAllowance: Long = 0L,
    val medicalImpact: String = "ALLOWANCE",
    val hasCustomAllowances: Boolean = false
)
