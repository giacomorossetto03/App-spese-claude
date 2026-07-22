package com.personal.spese.data.repository

import com.personal.spese.core.db.dao.ExpenseDao
import com.personal.spese.core.model.ExpenseType
import com.personal.spese.core.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import com.personal.spese.core.model.Expense
import com.personal.spese.data.mapper.toDomain
import com.personal.spese.data.mapper.toEntity

/**
 * Milestone 4: gestione della singola spesa (create/edit/get/delete).
 * Le query di elenco/aggregazione verranno esposte nelle milestone 5-6.
 */
class ExpenseRepository(private val dao: ExpenseDao) {

    /** Spese del mese, con filtro opzionale per categoria e tipo. */
    fun observeMonth(ym: YearMonth, categoryId: Long?, type: ExpenseType?): Flow<List<Expense>> {
        val (start, end) = Dates.monthBounds(ym)
        return dao.observeFiltered(start, end, categoryId, type?.name)
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun getById(id: Long): Expense? = dao.getById(id)?.toDomain()

    suspend fun save(expense: Expense): Long = dao.upsert(expense.toEntity())

    suspend fun delete(id: Long) = dao.deleteById(id)
}
