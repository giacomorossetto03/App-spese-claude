package com.personal.spese.feature.installments

data class PlanRow(
    val planId: Long,
    val title: String,
    val categoryName: String,
    val paidCount: Int,
    val totalCount: Int,
    val residualCents: Long
)

data class InstallmentsUiState(
    val plans: List<PlanRow> = emptyList()
)
