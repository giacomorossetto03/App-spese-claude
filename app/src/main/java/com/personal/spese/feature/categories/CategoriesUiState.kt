package com.personal.spese.feature.categories

import com.personal.spese.core.model.Category

data class CategoriesUiState(
    val categories: List<Category> = emptyList()
)
