package com.personal.spese.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Opzionale (post-MVP). categoryId null = budget totale del mese. period = YYYYMM. */
@Entity(
    tableName = "monthly_budget",
    indices = [Index(value = ["period", "categoryId"], unique = true)]
)
data class MonthlyBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: Int,
    val categoryId: Long?,
    val amountCents: Long
)
