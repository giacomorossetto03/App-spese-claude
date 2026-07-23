package com.personal.spese.core.model

import java.time.LocalDate

/** Regola di spesa ricorrente mensile. Genera Expense(type=RECURRING_INSTANCE). */
data class RecurringExpense(
    val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val categoryId: Long,
    val dayOfMonth: Int,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isActive: Boolean = true,
    val lastGeneratedPeriod: Int? = null
)
