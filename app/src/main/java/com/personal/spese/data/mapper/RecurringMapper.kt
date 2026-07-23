package com.personal.spese.data.mapper

import com.personal.spese.core.db.entity.RecurringExpenseEntity
import com.personal.spese.core.model.RecurringExpense
import com.personal.spese.core.util.Dates

fun RecurringExpenseEntity.toDomain(): RecurringExpense = RecurringExpense(
    id = id,
    title = title,
    amountCents = amountCents,
    categoryId = categoryId,
    dayOfMonth = dayOfMonth,
    startDate = Dates.fromEpochDay(startDate),
    endDate = endDate?.let { Dates.fromEpochDay(it) },
    isActive = isActive,
    lastGeneratedPeriod = lastGeneratedPeriod
)

fun RecurringExpense.toEntity(): RecurringExpenseEntity = RecurringExpenseEntity(
    id = id,
    title = title,
    amountCents = amountCents,
    categoryId = categoryId,
    dayOfMonth = dayOfMonth,
    startDate = Dates.toEpochDay(startDate),
    endDate = endDate?.let { Dates.toEpochDay(it) },
    isActive = isActive,
    lastGeneratedPeriod = lastGeneratedPeriod
)
