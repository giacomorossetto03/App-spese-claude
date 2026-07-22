package com.personal.spese.data.repository

import com.personal.spese.core.db.dao.ExpenseDao
import com.personal.spese.core.model.CategorySum
import com.personal.spese.core.model.Expense
import com.personal.spese.core.util.Dates
import com.personal.spese.data.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth

/**
 * Milestone 6: aggregati per la dashboard.
 * In M6 l'unica sorgente è `expense`; le rate (installment) entrano in M7.
 * Denaro sempre in centesimi (Long); range mese half-open via [Dates.monthBounds].
 */
class DashboardRepository(private val expenseDao: ExpenseDao) {

    /** Totale (centesimi) delle spese del mese. */
    fun totalInMonth(ym: YearMonth): Flow<Long> {
        val (start, end) = Dates.monthBounds(ym)
        return expenseDao.sumInMonth(start, end)
    }

    /** Numero di spese del mese. */
    fun countInMonth(ym: YearMonth): Flow<Int> {
        val (start, end) = Dates.monthBounds(ym)
        return expenseDao.countInMonth(start, end)
    }

    /** Somma per categoria nel mese (categoryId -> totale centesimi). */
    fun categoryBreakdown(ym: YearMonth): Flow<List<CategorySum>> {
        val (start, end) = Dates.monthBounds(ym)
        return expenseDao.sumByCategoryInMonth(start, end)
    }

    /** Ultime spese inserite (tutte le date), per l'anteprima in dashboard. */
    fun recentExpenses(limit: Int = 5): Flow<List<Expense>> =
        expenseDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }
}
