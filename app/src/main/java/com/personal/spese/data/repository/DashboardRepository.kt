package com.personal.spese.data.repository

import com.personal.spese.core.db.dao.ExpenseDao
import com.personal.spese.core.db.dao.InstallmentEntryDao
import com.personal.spese.core.model.CategorySum
import com.personal.spese.core.model.Expense
import com.personal.spese.core.util.Dates
import com.personal.spese.data.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Riepilogo globale delle rate ancora aperte (non del solo mese). */
data class InstallmentSummary(val openCount: Int, val residualCents: Long)

/** Totale di un mese per il grafico andamento (etichetta breve + centesimi). */
data class MonthTotal(val label: String, val totalCents: Long)

/**
 * Milestone 6-7: aggregati per la dashboard.
 * Il mese unisce due sorgenti (invarianti 4-6): `expense` + rate **dovute** nel mese
 * (pagate o no). Totale = sum(expense) + sumDue(installment); ripartizione e conteggio
 * coerenti col totale. Denaro sempre in centesimi (Long).
 */
class DashboardRepository(
    private val expenseDao: ExpenseDao,
    private val installmentEntryDao: InstallmentEntryDao
) {

    /** Totale mese (centesimi): spese singole + rate dovute nel mese. */
    fun totalInMonth(ym: YearMonth): Flow<Long> {
        val (start, end) = Dates.monthBounds(ym)
        return combine(
            expenseDao.sumInMonth(start, end),
            installmentEntryDao.sumDueInMonth(start, end)
        ) { expenses, installments -> expenses + installments }
    }

    /** N° movimenti del mese: spese + rate dovute. */
    fun countInMonth(ym: YearMonth): Flow<Int> {
        val (start, end) = Dates.monthBounds(ym)
        return combine(
            expenseDao.countInMonth(start, end),
            installmentEntryDao.countDueInMonth(start, end)
        ) { expenses, installments -> expenses + installments }
    }

    /** Ripartizione per categoria nel mese: spese + rate dovute, fuse per categoryId. */
    fun categoryBreakdown(ym: YearMonth): Flow<List<CategorySum>> {
        val (start, end) = Dates.monthBounds(ym)
        return combine(
            expenseDao.sumByCategoryInMonth(start, end),
            installmentEntryDao.sumDueByCategoryInMonth(start, end)
        ) { expenses, installments ->
            val byCategory = HashMap<Long, Long>()
            (expenses + installments).forEach { cs ->
                byCategory[cs.categoryId] = (byCategory[cs.categoryId] ?: 0L) + cs.total
            }
            byCategory.map { (id, total) -> CategorySum(id, total) }
        }
    }

    /** Rate aperte + residuo totale (non del solo mese), per le card riepilogo. */
    fun installmentSummary(): Flow<InstallmentSummary> =
        combine(
            installmentEntryDao.countOpen(),
            installmentEntryDao.residualTotal()
        ) { open, residual -> InstallmentSummary(open, residual) }

    /** Ultime spese inserite (solo `expense`), per l'anteprima in dashboard. */
    fun recentExpenses(limit: Int = 5): Flow<List<Expense>> =
        expenseDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    /** Andamento degli ultimi [months] mesi fino a [ym] incluso: spese + rate dovute per mese. */
    fun monthlyTrend(ym: YearMonth, months: Int): Flow<List<MonthTotal>> {
        val first = ym.minusMonths((months - 1).toLong())
        val start = first.atDay(1).toEpochDay()
        val end = ym.plusMonths(1).atDay(1).toEpochDay()
        return combine(
            expenseDao.sumByMonth(start, end),
            installmentEntryDao.sumDueByMonth(start, end)
        ) { expenses, installments ->
            val byPeriod = HashMap<Int, Long>()
            (expenses + installments).forEach { ps ->
                byPeriod[ps.period] = (byPeriod[ps.period] ?: 0L) + ps.total
            }
            (0 until months).map { i ->
                val m = first.plusMonths(i.toLong())
                val label = m.month.getDisplayName(TextStyle.SHORT, Locale.ITALY)
                    .replaceFirstChar { it.uppercase(Locale.ITALY) }
                MonthTotal(label, byPeriod[Dates.period(m)] ?: 0L)
            }
        }
    }
}
