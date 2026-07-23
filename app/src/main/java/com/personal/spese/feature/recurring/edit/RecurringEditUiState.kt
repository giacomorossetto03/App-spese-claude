package com.personal.spese.feature.recurring.edit

import com.personal.spese.core.model.Category
import java.time.LocalDate

data class RecurringEditUiState(
    val isEdit: Boolean = false,
    val title: String = "",
    val amountInput: String = "",
    val dayInput: String = "1",
    val categoryId: Long? = null,
    val startDate: LocalDate = LocalDate.now(),
    val hasEnd: Boolean = false,
    val endDate: LocalDate = LocalDate.now(),
    val isActive: Boolean = true,
    val categories: List<Category> = emptyList(),
    val titleError: Boolean = false,
    val amountError: Boolean = false,
    val dayError: Boolean = false,
    val categoryError: Boolean = false,
    val saved: Boolean = false
)
