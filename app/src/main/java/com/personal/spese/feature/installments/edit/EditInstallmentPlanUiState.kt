package com.personal.spese.feature.installments.edit

import com.personal.spese.core.model.Category

/** Modifica dei soli metadati di un piano di rate: titolo, categoria, nota. */
data class EditInstallmentPlanUiState(
    val loaded: Boolean = false,
    val title: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val titleError: Boolean = false,
    val categoryError: Boolean = false,
    val saved: Boolean = false
)
