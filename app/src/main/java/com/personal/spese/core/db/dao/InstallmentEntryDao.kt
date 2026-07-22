package com.personal.spese.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.personal.spese.core.db.entity.InstallmentEntryEntity
import com.personal.spese.core.model.CategorySum
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentEntryDao {

    @Insert
    suspend fun insertAll(entries: List<InstallmentEntryEntity>)

    @Query("SELECT * FROM installment_entry WHERE planId = :planId ORDER BY number")
    fun observeByPlan(planId: Long): Flow<List<InstallmentEntryEntity>>

    @Query("SELECT COUNT(*) FROM installment_entry WHERE isPaid = 0")
    fun countOpen(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountCents),0) FROM installment_entry WHERE isPaid = 0")
    fun residualTotal(): Flow<Long>

    @Query("SELECT * FROM installment_entry WHERE dueDate >= :start AND dueDate < :end ORDER BY dueDate")
    fun dueInMonth(start: Long, end: Long): Flow<List<InstallmentEntryEntity>>

    @Query("SELECT COALESCE(SUM(amountCents),0) FROM installment_entry WHERE dueDate >= :start AND dueDate < :end")
    fun sumDueInMonth(start: Long, end: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM installment_entry WHERE dueDate >= :start AND dueDate < :end")
    fun countDueInMonth(start: Long, end: Long): Flow<Int>

    @Query(
        "SELECT p.categoryId AS categoryId, COALESCE(SUM(e.amountCents),0) AS total " +
            "FROM installment_entry e JOIN installment_plan p ON p.id = e.planId " +
            "WHERE e.dueDate >= :start AND e.dueDate < :end GROUP BY p.categoryId"
    )
    fun sumDueByCategoryInMonth(start: Long, end: Long): Flow<List<CategorySum>>

    @Query("SELECT * FROM installment_entry WHERE isPaid = 0 AND dueDate >= :today ORDER BY dueDate LIMIT 1")
    fun nextUpcoming(today: Long): Flow<InstallmentEntryEntity?>

    @Query("UPDATE installment_entry SET isPaid = :paid, paidDate = :paidDate WHERE id = :id")
    suspend fun setPaid(id: Long, paid: Boolean, paidDate: Long?)
}
