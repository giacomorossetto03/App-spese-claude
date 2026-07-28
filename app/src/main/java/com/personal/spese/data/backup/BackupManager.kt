package com.personal.spese.data.backup

import androidx.room.withTransaction
import com.personal.spese.core.backup.BackupData
import com.personal.spese.core.db.AppDatabase
import com.personal.spese.core.util.Dates
import com.personal.spese.data.repository.DefaultCategories
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    /** Esporta le spese in CSV (delimitatore ';' per compatibilità Excel IT). */
    suspend fun exportExpensesCsv(): String {
        val names = db.categoryDao().getAll().associate { it.id to it.name }
        val expenses = db.expenseDao().getAll().sortedByDescending { it.date }
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY)
        val sb = StringBuilder()
        sb.append("Data;Categoria;Importo;Nota;Metodo;Tipo\n")
        expenses.forEach { e ->
            val row = listOf(
                Dates.fromEpochDay(e.date).format(fmt),
                names[e.categoryId] ?: "",
                "%.2f".format(Locale.ITALY, e.amountCents / 100.0),
                e.note ?: "",
                e.paymentMethod ?: "",
                if (e.type == "RECURRING_INSTANCE") "Ricorrente" else "Singola"
            )
            sb.append(row.joinToString(";") { csvEscape(it) }).append("\n")
        }
        return sb.toString()
    }

    private fun csvEscape(s: String): String =
        if (s.contains(';') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s

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
