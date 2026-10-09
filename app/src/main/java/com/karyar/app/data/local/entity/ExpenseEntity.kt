package com.karyar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = WorkplaceFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["workerId"]),
        Index(value = ["date"]),
        Index(value = ["epochDay"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long = 0L,        // شناسه پوشه محل کار
    val title: String,
    val category: String,           // TRANSIT (ایاب و ذهاب), ACCOMMODATION (اسکان), FOOD (خوراک), MEDICAL (درمان), OTHER (سایر)
    val scope: String,              // INDIVIDUAL (تکی), GROUP (جمعی)
    val impactType: String = "DEDUCTION", // ALLOWANCE (افزایشی / اضافه به حقوق یا کمک‌هزینه), DEDUCTION (کاهشی / کسر از حقوق)
    val workerId: Long? = null,     // در صورت تکی بودن، شناسه کارگر
    val workerName: String? = null, // نام کارگر برای دسترسی سریع
    val amount: Long,               // مبلغ هزینه (تومان)
    val accommodationDays: Int = 0, // مدت زمان اسکان به روز
    val date: String,               // تاریخ شمسی
    val epochDay: Long = 0L,        // تاریخ به صورت epochDay
    val timestamp: Long = System.currentTimeMillis(),
    val workplaceName: String = "", // نام محل کار / پروژه
    val employerName: String = "",  // نام کارفرما
    val foremanName: String = "",   // نام سرکارگر
    val notes: String = ""          // توضیحات
)
