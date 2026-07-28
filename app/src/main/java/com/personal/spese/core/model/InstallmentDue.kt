package com.personal.spese.core.model

import java.time.LocalDate

/**
 * Proiezione Room: rata dovuta nel mese + metadati del piano (join `installment_entry`↔`installment_plan`).
 * Solo primitivi (invariante 9): `dueDate` è epochDay, la conversione a [LocalDate] avviene nel repository.
 * I nomi dei campi coincidono con gli alias della query.
 */
data class InstallmentDueRow(
    val entryId: Long,
    val planId: Long,
    val number: Int,
    val installmentsCount: Int,
    val title: String,
    val categoryId: Long,
    val amountCents: Long,
    val dueDate: Long,
    val isPaid: Boolean
)

/** Dominio: rata dovuta nel mese, con data come [LocalDate], per la lista spese. */
data class InstallmentDue(
    val entryId: Long,
    val planId: Long,
    val number: Int,
    val installmentsCount: Int,
    val title: String,
    val categoryId: Long,
    val amountCents: Long,
    val dueDate: LocalDate,
    val isPaid: Boolean
)
