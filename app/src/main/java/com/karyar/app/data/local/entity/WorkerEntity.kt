package com.karyar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workers",
    foreignKeys = [
        ForeignKey(
            entity = WorkplaceFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["folderId"])
    ]
)
data class WorkerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long = 0L,         // شناسه پوشه محل کار
    val name: String,
    val role: String,                // مثلا بنا، آرماتوربند، کارگر ساده، گچ‌کار، برق‌کار
    val phone: String = "",          // حداکثر ۱۱ رقم فقط عددی
    val nationalId: String = "",     // حداکثر ۱۰ رقم فقط عددی
    val baseDailyWage: Long = 0L,    // دستمزد روزانه پایه (تومان)
    val baseHourlyWage: Long = 0L,   // دستمزد ساعتی پایه (تومان)

    // دستمزد ساعتی با تیک‌باکس
    val isHourlyEnabled: Boolean = false,
    val hourlyWageRate: Long = 0L,   // مبلغ هر ساعت کار (تومان)
    val hourlyHours: Double = 0.0,    // تعداد ساعت کار ساعتی

    // اضافه کاری با تیک‌باکس
    val isOvertimeEnabled: Boolean = false,
    val overtimeRate: Long = 0L,     // مبلغ هر ساعت اضافه کار (تومان)
    val overtimeHours: Double = 0.0,  // تعداد ساعت اضافه کار

    val isActive: Boolean = true,
    val notes: String = "",          // توضیحات هر شخص
    val colorTag: Long = 0xFFD97706L,

    // تنظیمات هزینه‌های فردی با قابلیت انتخاب افزایشی/کاهشی
    val transitAllowance: Long = 0L,
    val transitImpact: String = "ALLOWANCE", // "ALLOWANCE" (افزایشی) یا "DEDUCTION" (کاهشی)

    val foodAllowance: Long = 0L,
    val foodImpact: String = "ALLOWANCE",

    val accommodationAllowance: Long = 0L,
    val accommodationImpact: String = "ALLOWANCE",

    val medicalAllowance: Long = 0L,
    val medicalImpact: String = "ALLOWANCE",

    val createdAt: Long = System.currentTimeMillis()
)
