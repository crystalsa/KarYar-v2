package com.karyar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "date_folders",
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
        Index(value = ["folderId", "date"]),
        Index(value = ["epochDay"])
    ]
)
data class DateFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long,             // شناسه پوشه اصلی کارگاه
    val date: String,               // تاریخ شمسی مثلا 1405/01/15
    val dayOfWeek: String,          // روز هفته مثلا شنبه، یکشنبه، ...
    val title: String = "",         // عنوان اختیاری مثلا: بتن‌ریزی سقف اول
    val notes: String = "",         // توضیحات روز
    val createdAt: Long = System.currentTimeMillis(),
    val epochDay: Long = 0L         // تاریخ به صورت epochDay برای فیلتر و مرتب‌سازی دقیق
)
