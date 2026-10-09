package com.karyar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workplace_folders")
data class WorkplaceFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // نام محل کار / پروژه
    val foremanName: String = "",       // نام سرکارگر (زیر نام محل کار)
    val employerName: String = "",      // نام کارفرما
    val colorTag: Long = 0xFFD97706L,
    val createdAt: String = "",
    val notes: String = ""
)
