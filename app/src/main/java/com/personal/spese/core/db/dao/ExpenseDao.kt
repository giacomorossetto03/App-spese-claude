package com.personal.spese.core.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.personal.spese.core.db.entity.ExpenseEntity
import com.personal.spese.core.model.CategorySum
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expense WHERE date >= :start AND date < :end ORDER BY date DESC, id DESC")
    fun observeInMonth(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amountCents),0) FROM expense WHERE date >= :start AND date < :end")
    fun sumInMonth(start: Long, end: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM expense WHERE date >= :start AND date < :end")
    fun countInMonth(start: Long, end: Long): Flow<Int>

    @Query(
        "SELECT categoryId AS categoryId, COALESCE(SUM(amountCents),0) AS total " +
            "FROM expense WHERE date >= :start AND date < :end GROUP BY categoryId"
    )
    fun sumByCategoryInMonth(start: Long, end: Long): Flow<List<CategorySum>>

    @Query("SELECT * FROM expense ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT * FROM expense " +
            "WHERE date >= :start AND date < :end " +
            "AND (:categoryId IS NULL OR categoryId = :categoryId) " +
            "AND (:type IS NULL OR type = :type) " +
            "ORDER BY date DESC, id DESC"
    )
    fun observeFiltered(start: Long, end: Long, categoryId: Long?, type: String?): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expense WHERE recurringId = :recurringId AND date >= :start AND date < :end LIMIT 1")
    suspend fun findRecurringInstance(recurringId: Long, start: Long, end: Long): ExpenseEntity?

    @Upsert
    suspend fun upsert(expense: ExpenseEntity): Long

    @Insert
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expense WHERE id = :id")
    suspend fun deleteById(id: Long)
}
