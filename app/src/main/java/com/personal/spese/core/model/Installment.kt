package com.personal.spese.core.model

import java.time.LocalDate

/** Piano rate (padre). Nessun contatore aggregato: tutto derivato dalle entry. */
data class InstallmentPlan(
    val id: Long = 0,
    val title: String,
    val totalAmountCents: Long,
    val installmentsCount: Int,
    val installmentAmountCents: Long,
    val firstDueDate: LocalDate,
    val categoryId: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Rata singola (figlia), con scadenza mensile autonoma. */
data class InstallmentEntry(
    val id: Long = 0,
    val planId: Long,
    val number: Int,
    val dueDate: LocalDate,
    val amountCents: Long,
    val isPaid: Boolean = false,
    val paidDate: LocalDate? = null
)
