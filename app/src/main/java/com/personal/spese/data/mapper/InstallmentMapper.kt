package com.personal.spese.data.mapper

import com.personal.spese.core.db.entity.InstallmentEntryEntity
import com.personal.spese.core.db.entity.InstallmentPlanEntity
import com.personal.spese.core.model.InstallmentEntry
import com.personal.spese.core.model.InstallmentPlan
import com.personal.spese.core.util.Dates

fun InstallmentPlanEntity.toDomain(): InstallmentPlan = InstallmentPlan(
    id = id,
    title = title,
    totalAmountCents = totalAmountCents,
    installmentsCount = installmentsCount,
    installmentAmountCents = installmentAmountCents,
    firstDueDate = Dates.fromEpochDay(firstDueDate),
    categoryId = categoryId,
    note = note,
    createdAt = createdAt
)

fun InstallmentPlan.toEntity(): InstallmentPlanEntity = InstallmentPlanEntity(
    id = id,
    title = title,
    totalAmountCents = totalAmountCents,
    installmentsCount = installmentsCount,
    installmentAmountCents = installmentAmountCents,
    firstDueDate = Dates.toEpochDay(firstDueDate),
    categoryId = categoryId,
    note = note,
    createdAt = createdAt
)

fun InstallmentEntryEntity.toDomain(): InstallmentEntry = InstallmentEntry(
    id = id,
    planId = planId,
    number = number,
    dueDate = Dates.fromEpochDay(dueDate),
    amountCents = amountCents,
    isPaid = isPaid,
    paidDate = paidDate?.let { Dates.fromEpochDay(it) }
)

fun InstallmentEntry.toEntity(): InstallmentEntryEntity = InstallmentEntryEntity(
    id = id,
    planId = planId,
    number = number,
    dueDate = Dates.toEpochDay(dueDate),
    amountCents = amountCents,
    isPaid = isPaid,
    paidDate = paidDate?.let { Dates.toEpochDay(it) }
)
