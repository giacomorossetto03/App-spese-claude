package com.personal.spese.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Spesa singola oppure istanza materializzata di una ricorrente.
 * Le rate NON passano da qui: vivono in installment_entry.
 * Denaro in centesimi (Long). Date come epochDay (Long).
 * type: "SINGLE" | "RECURRING_INSTANCE".
 * recurringId: solo riferimento logico (nessuna FK, per non vincolare le cancellazioni).
 */
@Entity(
    tableName = "expense",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("date"), Index("categoryId"), Index("recurringId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val date: Long,
    val categoryId: Long,
    val note: String?,
    val paymentMethod: String?,
    val type: String,
    val recurringId: Long?,
    val createdAt: Long
)
