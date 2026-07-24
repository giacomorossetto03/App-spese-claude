package com.personal.spese.data.backup

import androidx.room.withTransaction
import com.personal.spese.core.backup.BackupData
import com.personal.spese.core.db.AppDatabase
import com.personal.spese.data.repository.DefaultCategories
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Milestone 10: backup/ripristino/reset. JSON via kotlinx.serialization.
 * Import e reset rispettano l'ordine delle FK (categoria = RESTRICT, entry = CASCADE).
 */
class BackupManager(private val db: AppDatabase) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportJson(): String {
        val data = BackupData(
            categories = db.categoryDao().getAll().map { it.toBackup() },
            expenses = db.expenseDao().getAll().map { it.toBackup() },
            plans = db.installmentPlanDao().getAll().map { it.toBackup() },
            entries = db.installmentEntryDao().getAll().map { it.toBackup() },
            recurring = db.recurringExpenseDao().getAll().map { it.toBackup() }
        )
        return json.encodeToString(data)
    }

    suspend fun importJson(text: String) {
        val data = json.decodeFromString<BackupData>(text)
        db.withTransaction {
            deleteAllInternal()
            db.categoryDao().insertAll(data.categories.map { it.toEntity() })
            db.recurringExpenseDao().insertAll(data.recurring.map { it.toEntity() })
            db.expenseDao().insertAll(data.expenses.map { it.toEntity() })
            db.installmentPlanDao().insertAll(data.plans.map { it.toEntity() })
            db.installmentEntryDao().insertAll(data.entries.map { it.toEntity() })
        }
    }

    suspend fun resetToDefaults() {
        db.withTransaction {
            deleteAllInternal()
            db.categoryDao().insertAll(DefaultCategories.list())
        }
    }

    /** Cancella tutto in ordine figlio→padre (rispetta le FK). */
    private suspend fun deleteAllInternal() {
        db.installmentEntryDao().deleteAll()
        db.installmentPlanDao().deleteAll()
        db.expenseDao().deleteAll()
        db.recurringExpenseDao().deleteAll()
        db.categoryDao().deleteAll()
    }
}
