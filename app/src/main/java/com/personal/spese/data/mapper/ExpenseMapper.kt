package com.personal.spese.data.mapper

import com.personal.spese.core.db.entity.ExpenseEntity
import com.personal.spese.core.model.Expense
import com.personal.spese.core.model.ExpenseType
import com.personal.spese.core.util.Dates

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    amountCents = amountCents,
    date = Dates.fromEpochDay(date),
    categoryId = categoryId,
    note = note,
    paymentMethod = paymentMethod,
    type = ExpenseType.valueOf(type),
    recurringId = recurringId,
    createdAt = createdAt
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amountCents = amountCents,
    date = Dates.toEpochDay(date),
    categoryId = categoryId,
    note = note,
    paymentMethod = paymentMethod,
    type = type.name,
    recurringId = recurringId,
    createdAt = createdAt
)
