package com.personal.spese.data.repository

import com.personal.spese.core.db.dao.ExpenseDao
import com.personal.spese.core.db.dao.RecurringExpenseDao
import com.personal.spese.core.db.entity.ExpenseEntity
import com.personal.spese.core.model.ExpenseType
import com.personal.spese.core.util.Dates
import java.time.LocalDate
import java.time.YearMonth

/**
 * Materializza le regole ricorrenti come `Expense(type=RECURRING_INSTANCE)`, così il totale
 * resta `sum(expense) + sumDue(installment)` senza doppi conteggi (invariante 5).
 *
 * **Idempotente**: per ogni mese usa [ExpenseDao.findRecurringInstance] per non duplicare, e
 * avanza `lastGeneratedPeriod` fino al mese corrente. Genera l'istanza dell'intero mese
 * (anche se il giorno non è ancora arrivato): "istanze ricorrenti del mese".
 */
class RecurringGenerator(
    private val recurringDao: RecurringExpenseDao,
    private val expenseDao: ExpenseDao
) {

    /**
     * Allinea le istanze già materializzate ai nuovi valori della regola (categoria/importo/titolo).
     * Da chiamare dopo la modifica di una ricorrente: la generazione è idempotente e non
     * toccherebbe i mesi già creati, quindi senza questo passaggio la modifica non si vedrebbe.
     */
    suspend fun syncExistingInstances(ruleId: Long, categoryId: Long, amountCents: Long, title: String) {
        expenseDao.updateRecurringInstances(ruleId, categoryId, amountCents, title)
    }

    suspend fun generateUpTo(today: LocalDate = Dates.today()) {
        val currentYm = YearMonth.from(today)
        val currentPeriod = Dates.period(currentYm)

        for (rule in recurringDao.toGenerate(currentPeriod)) {
            val start = Dates.fromEpochDay(rule.startDate)
            val end = rule.endDate?.let { Dates.fromEpochDay(it) }

            // Primo mese da valutare: mese di inizio, oppure il mese dopo l'ultimo generato.
            var period = YearMonth.from(start)
            rule.lastGeneratedPeriod?.let { last ->
                val nextAfterLast = Dates.fromPeriod(last).plusMonths(1)
                if (nextAfterLast.isAfter(period)) period = nextAfterLast
            }

            var lastProcessed: Int? = rule.lastGeneratedPeriod
            while (!period.isAfter(currentYm)) {
                val day = minOf(rule.dayOfMonth, period.lengthOfMonth())
                val instanceDate = period.atDay(day)
                val withinBounds = !instanceDate.isBefore(start) &&
                    (end == null || !instanceDate.isAfter(end))

                if (withinBounds) {
                    val (monthStart, monthEnd) = Dates.monthBounds(period)
                    val existing = expenseDao.findRecurringInstance(rule.id, monthStart, monthEnd)
                    if (existing == null) {
                        expenseDao.upsert(
                            ExpenseEntity(
                                amountCents = rule.amountCents,
                                date = Dates.toEpochDay(instanceDate),
                                categoryId = rule.categoryId,
                                note = rule.title,
                                paymentMethod = null,
                                type = ExpenseType.RECURRING_INSTANCE.name,
                                recurringId = rule.id,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                lastProcessed = Dates.period(period)
                period = period.plusMonths(1)
            }

            if (lastProcessed != null && lastProcessed != rule.lastGeneratedPeriod) {
                recurringDao.updateLastGenerated(rule.id, lastProcessed)
            }
        }
    }
}
