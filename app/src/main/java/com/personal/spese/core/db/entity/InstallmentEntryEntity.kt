package com.personal.spese.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Figlia: la rata reale, scadenza mensile autonoma. Base di tutti i calcoli residuo/scadenze. */
@Entity(
    tableName = "installment_entry",
    foreignKeys = [
        ForeignKey(
            entity = InstallmentPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId"), Index("dueDate"), Index("isPaid")]
)
data class InstallmentEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val number: Int,
    val dueDate: Long,
    val amountCents: Long,
    val isPaid: Boolean,
    val paidDate: Long?
)
