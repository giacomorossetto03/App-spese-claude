package com.personal.spese.data.repository

import com.personal.spese.core.db.dao.CategoryDao
import com.personal.spese.core.db.entity.CategoryEntity
import com.personal.spese.core.model.Category
import com.personal.spese.data.mapper.toDomain
import com.personal.spese.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val dao: CategoryDao) {

    fun observeActive(): Flow<List<Category>> =
        dao.observeActive().map { list -> list.map(CategoryEntity::toDomain) }

    fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map(CategoryEntity::toDomain) }

    suspend fun getById(id: Long): Category? = dao.getById(id)?.toDomain()

    suspend fun upsert(category: Category): Long = dao.upsert(category.toEntity())

    suspend fun archive(id: Long) = dao.archive(id)

    suspend fun unarchive(id: Long) = dao.unarchive(id)

    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() > 0) return
        dao.insertAll(DefaultCategories.list())
    }
}
