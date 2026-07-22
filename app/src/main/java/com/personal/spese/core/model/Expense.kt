package com.personal.spese.core.model

import java.time.LocalDate

data class Expense(
    val id: Long = 0,
    val amountCents: Long,
    val date: LocalDate,
    val categoryId: Long,
    val note: String? = null,
    val paymentMethod: String? = null,
    val type: ExpenseType = ExpenseType.SINGLE,
    val recurringId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
