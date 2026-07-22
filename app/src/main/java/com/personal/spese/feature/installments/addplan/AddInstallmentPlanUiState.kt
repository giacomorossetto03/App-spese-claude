package com.personal.spese.feature.installments.addplan

import com.personal.spese.core.model.Category
import java.time.LocalDate

data class AddInstallmentPlanUiState(
    val title: String = "",
    val amountInput: String = "",
    val countInput: String = "",
    val firstDueDate: LocalDate = LocalDate.now(),
    val categoryId: Long? = null,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val titleError: Boolean = false,
    val amountError: Boolean = false,
    val countError: Boolean = false,
    val categoryError: Boolean = false,
    val saved: Boolean = false
)
