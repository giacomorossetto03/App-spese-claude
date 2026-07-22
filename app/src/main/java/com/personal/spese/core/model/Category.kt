package com.personal.spese.core.model

data class Category(
    val id: Long = 0,
    val name: String,
    val colorArgb: Int? = null,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)
