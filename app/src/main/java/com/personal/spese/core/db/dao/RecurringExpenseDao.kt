package com.personal.spese.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.personal.spese.core.db.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {

    @Query("SELECT * FROM recurring_expense ORDER BY title")
    fun observeAll(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expense WHERE isActive = 1")
    fun observeActive(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expense WHERE id = :id")
    suspend fun getById(id: Long): RecurringExpenseEntity?

    @Query(
        "SELECT * FROM recurring_expense " +
            "WHERE isActive = 1 AND (lastGeneratedPeriod IS NULL OR lastGeneratedPeriod < :currentPeriod)"
    )
    suspend fun toGenerate(currentPeriod: Int): List<RecurringExpenseEntity>

    @Query("UPDATE recurring_expense SET lastGeneratedPeriod = :period WHERE id = :id")
    suspend fun updateLastGenerated(id: Long, period: Int)

    @Upsert
    suspend fun upsert(recurring: RecurringExpenseEntity): Long

    @Query("DELETE FROM recurring_expense WHERE id = :id")
    suspend fun deleteById(id: Long)
}
