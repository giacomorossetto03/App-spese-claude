package com.personal.spese.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Definizione di ricorrenza mensile semplice. Genera ExpenseEntity(RECURRING_INSTANCE). */
@Entity(
    tableName = "recurring_expense",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId"), Index("isActive")]
)
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val categoryId: Long,
    val dayOfMonth: Int,
    val startDate: Long,
    val endDate: Long?,
    val isActive: Boolean,
    val lastGeneratedPeriod: Int?
)
