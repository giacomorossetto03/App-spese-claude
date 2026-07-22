package com.personal.spese.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Padre: l'acquisto/finanziamento. Nessun contatore aggregato: tutto derivato dalle entry. */
@Entity(
    tableName = "installment_plan",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId")]
)
data class InstallmentPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmountCents: Long,
    val installmentsCount: Int,
    val installmentAmountCents: Long,
    val firstDueDate: Long,
    val categoryId: Long,
    val note: String?,
    val createdAt: Long
)
