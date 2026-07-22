package com.personal.spese.data.repository

import androidx.room.withTransaction
import com.personal.spese.core.db.AppDatabase
import com.personal.spese.core.db.entity.InstallmentEntryEntity
import com.personal.spese.core.db.entity.InstallmentPlanEntity
import com.personal.spese.core.model.InstallmentEntry
import com.personal.spese.core.model.InstallmentPlan
import com.personal.spese.core.model.PlanProgress
import com.personal.spese.core.util.Dates
import com.personal.spese.data.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Milestone 7: piani di rate. Le rate NON sono [com.personal.spese.core.model.Expense]:
 * vivono in `installment_entry`. Denaro sempre in centesimi (Long).
 */
class InstallmentRepository(private val db: AppDatabase) {

    private val planDao = db.installmentPlanDao()
    private val entryDao = db.installmentEntryDao()

    fun observePlansProgress(): Flow<List<PlanProgress>> = planDao.observePlansProgress()

    suspend fun getPlan(id: Long): InstallmentPlan? = planDao.getById(id)?.toDomain()

    fun observeEntries(planId: Long): Flow<List<InstallmentEntry>> =
        entryDao.observeByPlan(planId).map { list -> list.map { it.toDomain() } }

    suspend fun setPaid(entryId: Long, paid: Boolean) =
        entryDao.setPaid(entryId, paid, if (paid) Dates.today().toEpochDay() else null)

    suspend fun deletePlan(id: Long) = planDao.deleteById(id)

    /**
     * Crea piano + rate in un'unica transazione (invariante: nessun piano senza rate).
     * Arrotondamento: `rata = floor(totale/n)`, l'**ultima** rata assorbe il resto.
     * Le scadenze sono mensili a partire da [firstDueDate].
     */
    suspend fun createPlan(
        title: String,
        totalAmountCents: Long,
        installmentsCount: Int,
        firstDueDate: LocalDate,
        categoryId: Long,
        note: String?
    ): Long {
        require(installmentsCount >= 1) { "Numero rate deve essere >= 1" }
        require(totalAmountCents >= 0) { "Importo non valido" }

        val base = totalAmountCents / installmentsCount            // floor (valori non negativi)
        val remainder = totalAmountCents - base * installmentsCount // 0..n-1
        val createdAt = System.currentTimeMillis()

        return db.withTransaction {
            val planId = planDao.insert(
                InstallmentPlanEntity(
                    title = title.trim(),
                    totalAmountCents = totalAmountCents,
                    installmentsCount = installmentsCount,
                    installmentAmountCents = base,
                    firstDueDate = Dates.toEpochDay(firstDueDate),
                    categoryId = categoryId,
                    note = note?.trim()?.ifBlank { null },
                    createdAt = createdAt
                )
            )
            val entries = (0 until installmentsCount).map { i ->
                val amount = if (i == installmentsCount - 1) base + remainder else base
                InstallmentEntryEntity(
                    planId = planId,
                    number = i + 1,
                    dueDate = Dates.toEpochDay(firstDueDate.plusMonths(i.toLong())),
                    amountCents = amount,
                    isPaid = false,
                    paidDate = null
                )
            }
            entryDao.insertAll(entries)
            planId
        }
    }
}
