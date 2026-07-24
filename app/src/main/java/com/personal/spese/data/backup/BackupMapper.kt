package com.personal.spese.data.backup

import com.personal.spese.core.backup.BackupCategory
import com.personal.spese.core.backup.BackupEntry
import com.personal.spese.core.backup.BackupExpense
import com.personal.spese.core.backup.BackupPlan
import com.personal.spese.core.backup.BackupRecurring
import com.personal.spese.core.db.entity.CategoryEntity
import com.personal.spese.core.db.entity.ExpenseEntity
import com.personal.spese.core.db.entity.InstallmentEntryEntity
import com.personal.spese.core.db.entity.InstallmentPlanEntity
import com.personal.spese.core.db.entity.RecurringExpenseEntity

fun CategoryEntity.toBackup() = BackupCategory(id, name, colorArgb, isDefault, isArchived, sortOrder)
fun BackupCategory.toEntity() = CategoryEntity(id, name, colorArgb, isDefault, isArchived, sortOrder)

fun ExpenseEntity.toBackup() =
    BackupExpense(id, amountCents, date, categoryId, note, paymentMethod, type, recurringId, createdAt)
fun BackupExpense.toEntity() =
    ExpenseEntity(id, amountCents, date, categoryId, note, paymentMethod, type, recurringId, createdAt)

fun InstallmentPlanEntity.toBackup() =
    BackupPlan(id, title, totalAmountCents, installmentsCount, installmentAmountCents, firstDueDate, categoryId, note, createdAt)
fun BackupPlan.toEntity() =
    InstallmentPlanEntity(id, title, totalAmountCents, installmentsCount, installmentAmountCents, firstDueDate, categoryId, note, createdAt)

fun InstallmentEntryEntity.toBackup() =
    BackupEntry(id, planId, number, dueDate, amountCents, isPaid, paidDate)
fun BackupEntry.toEntity() =
    InstallmentEntryEntity(id, planId, number, dueDate, amountCents, isPaid, paidDate)

fun RecurringExpenseEntity.toBackup() =
    BackupRecurring(id, title, amountCents, categoryId, dayOfMonth, startDate, endDate, isActive, lastGeneratedPeriod)
fun BackupRecurring.toEntity() =
    RecurringExpenseEntity(id, title, amountCents, categoryId, dayOfMonth, startDate, endDate, isActive, lastGeneratedPeriod)
