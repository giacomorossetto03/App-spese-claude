package com.personal.spese.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.personal.spese.core.db.entity.InstallmentEntryEntity
import com.personal.spese.core.model.CategorySum
import com.personal.spese.core.model.InstallmentDueRow
import com.personal.spese.core.model.PeriodSum
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

    /**
     * Rate dovute nel mese con i metadati del piano (titolo, categoria, n° totale rate), filtrabili
     * per categoria. Serve alla lista spese per mostrare le rate accanto alle spese vere.
     */
    @Query(
        "SELECT e.id AS entryId, e.planId AS planId, e.number AS number, " +
            "p.installmentsCount AS installmentsCount, p.title AS title, p.categoryId AS categoryId, " +
            "e.amountCents AS amountCents, e.dueDate AS dueDate, e.isPaid AS isPaid " +
            "FROM installment_entry e JOIN installment_plan p ON p.id = e.planId " +
            "WHERE e.dueDate >= :start AND e.dueDate < :end " +
            "AND (:categoryId IS NULL OR p.categoryId = :categoryId) " +
            "ORDER BY e.dueDate DESC, e.id DESC"
    )
    fun dueInMonthDetailed(start: Long, end: Long, categoryId: Long?): Flow<List<InstallmentDueRow>>

    @Query("SELECT COALESCE(SUM(amountCents),0) FROM installment_entry WHERE dueDate >= :start AND dueDate < :end")
    fun sumDueInMonth(start: Long, end: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM installment_entry WHERE dueDate >= :start AND dueDate < :end")
    fun countDueInMonth(start: Long, end: Long): Flow<Int>

    @Query(
        "SELECT CAST(strftime('%Y%m', dueDate * 86400, 'unixepoch') AS INTEGER) AS period, " +
            "COALESCE(SUM(amountCents),0) AS total " +
            "FROM installment_entry WHERE dueDate >= :start AND dueDate < :end GROUP BY period"
    )
    fun sumDueByMonth(start: Long, end: Long): Flow<List<PeriodSum>>

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

    @Query("SELECT * FROM installment_entry")
    suspend fun getAll(): List<InstallmentEntryEntity>

    @Query("DELETE FROM installment_entry")
    suspend fun deleteAll()
}
