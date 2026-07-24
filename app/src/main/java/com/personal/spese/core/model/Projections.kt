package com.personal.spese.core.model

/** Risultati di query aggregate (POJO Room, non entità). */

data class CategorySum(
    val categoryId: Long,
    val total: Long
)

/** Totale per periodo (Int YYYYMM), per i grafici di andamento mensile. */
data class PeriodSum(
    val period: Int,
    val total: Long
)

data class PlanProgress(
    val planId: Long,
    val title: String,
    val categoryId: Long,
    val totalCount: Int,
    val paidCount: Int,
    val residualCents: Long
)
