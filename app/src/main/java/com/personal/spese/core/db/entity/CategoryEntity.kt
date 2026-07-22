package com.personal.spese.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int?,
    val isDefault: Boolean,
    val isArchived: Boolean,
    val sortOrder: Int
)
