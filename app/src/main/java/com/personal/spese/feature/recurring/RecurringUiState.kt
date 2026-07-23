package com.personal.spese.feature.recurring

data class RecurringRow(
    val id: Long,
    val title: String,
    val amountCents: Long,
    val categoryName: String,
    val dayOfMonth: Int,
    val isActive: Boolean
)

data class RecurringUiState(
    val items: List<RecurringRow> = emptyList()
)
