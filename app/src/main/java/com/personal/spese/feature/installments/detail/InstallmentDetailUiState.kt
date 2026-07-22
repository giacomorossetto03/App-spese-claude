package com.personal.spese.feature.installments.detail

import java.time.LocalDate

data class EntryRow(
    val id: Long,
    val number: Int,
    val dueDate: LocalDate,
    val amountCents: Long,
    val isPaid: Boolean
)

data class InstallmentDetailUiState(
    val loaded: Boolean = false,
    val title: String = "",
    val categoryName: String = "",
    val totalCents: Long = 0L,
    val installmentsCount: Int = 0,
    val paidCount: Int = 0,
    val residualCents: Long = 0L,
    val note: String? = null,
    val entries: List<EntryRow> = emptyList()
)
