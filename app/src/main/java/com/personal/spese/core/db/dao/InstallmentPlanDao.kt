package com.personal.spese.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.personal.spese.core.db.entity.InstallmentPlanEntity
import com.personal.spese.core.model.PlanProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentPlanDao {

    @Insert
    suspend fun insert(plan: InstallmentPlanEntity): Long

    @Insert
    suspend fun insertAll(plans: List<InstallmentPlanEntity>)

    @Query("SELECT * FROM installment_plan WHERE id = :id")
    suspend fun getById(id: Long): InstallmentPlanEntity?

    @Query("SELECT * FROM installment_plan ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InstallmentPlanEntity>>

    /** Progressione per piano derivata dalle entry (pagate/totali e residuo). */
    @Transaction
    @Query(
        "SELECT p.id AS planId, p.title AS title, p.categoryId AS categoryId, " +
            "p.installmentsCount AS totalCount, " +
            "COALESCE(SUM(CASE WHEN e.isPaid = 1 THEN 1 ELSE 0 END),0) AS paidCount, " +
            "COALESCE(SUM(CASE WHEN e.isPaid = 0 THEN e.amountCents ELSE 0 END),0) AS residualCents " +
            "FROM installment_plan p " +
            "LEFT JOIN installment_entry e ON e.planId = p.id " +
            "GROUP BY p.id ORDER BY p.createdAt DESC"
    )
    fun observePlansProgress(): Flow<List<PlanProgress>>

    @Query("DELETE FROM installment_plan WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM installment_plan")
    suspend fun getAll(): List<InstallmentPlanEntity>

    @Query("DELETE FROM installment_plan")
    suspend fun deleteAll()
}
