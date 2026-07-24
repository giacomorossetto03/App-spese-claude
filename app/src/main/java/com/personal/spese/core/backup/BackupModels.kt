package com.personal.spese.core.backup

import kotlinx.serialization.Serializable

/** Formato di backup (JSON). Campi = primitivi delle entità, per un round-trip fedele. */
@Serializable
data class BackupData(
    val version: Int = 1,
    val categories: List<BackupCategory> = emptyList(),
    val expenses: List<BackupExpense> = emptyList(),
    val plans: List<BackupPlan> = emptyList(),
    val entries: List<BackupEntry> = emptyList(),
    val recurring: List<BackupRecurring> = emptyList()
)

@Serializable
data class BackupCategory(
    val id: Long,
    val name: String,
    val colorArgb: Int? = null,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)

@Serializable
data class BackupExpense(
    val id: Long,
    val amountCents: Long,
    val date: Long,
    val categoryId: Long,
    val note: String? = null,
    val paymentMethod: String? = null,
    val type: String,
    val recurringId: Long? = null,
    val createdAt: Long
)

@Serializable
data class BackupPlan(
    val id: Long,
    val title: String,
    val totalAmountCents: Long,
    val installmentsCount: Int,
    val installmentAmountCents: Long,
    val firstDueDate: Long,
    val categoryId: Long,
    val note: String? = null,
    val createdAt: Long
)

@Serializable
data class BackupEntry(
    val id: Long,
    val planId: Long,
    val number: Int,
    val dueDate: Long,
    val amountCents: Long,
    val isPaid: Boolean,
    val paidDate: Long? = null
)

@Serializable
data class BackupRecurring(
    val id: Long,
    val title: String,
    val amountCents: Long,
    val categoryId: Long,
    val dayOfMonth: Int,
    val startDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val lastGeneratedPeriod: Int? = null
)
