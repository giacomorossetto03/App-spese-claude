package com.personal.spese.data.mapper

import com.personal.spese.core.db.entity.CategoryEntity
import com.personal.spese.core.model.Category

fun CategoryEntity.toDomain(): Category =
    Category(id, name, colorArgb, isDefault, isArchived, sortOrder)

fun Category.toEntity(): CategoryEntity =
    CategoryEntity(id, name, colorArgb, isDefault, isArchived, sortOrder)
