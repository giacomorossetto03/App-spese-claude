package com.personal.spese.data.repository

import com.personal.spese.core.db.dao.RecurringExpenseDao
import com.personal.spese.core.model.RecurringExpense
import com.personal.spese.data.mapper.toDomain
import com.personal.spese.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Milestone 8: CRUD delle regole ricorrenti. La materializzazione è in [RecurringGenerator]. */
class RecurringRepository(private val dao: RecurringExpenseDao) {

    fun observeAll(): Flow<List<RecurringExpense>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): RecurringExpense? = dao.getById(id)?.toDomain()

    suspend fun upsert(recurring: RecurringExpense): Long = dao.upsert(recurring.toEntity())

    suspend fun delete(id: Long) = dao.deleteById(id)
}
