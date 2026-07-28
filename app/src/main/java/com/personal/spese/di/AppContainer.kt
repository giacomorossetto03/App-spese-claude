package com.personal.spese.di

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.InvalidationTracker
import com.personal.spese.core.datastore.SettingsDataStore
import com.personal.spese.core.db.AppDatabase
import com.personal.spese.core.update.UpdateService
import com.personal.spese.data.backup.BackupManager
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.DashboardRepository
import com.personal.spese.data.repository.ExpenseRepository
import com.personal.spese.data.repository.InstallmentRepository
import com.personal.spese.data.repository.RecurringGenerator
import com.personal.spese.data.repository.RecurringRepository
import com.personal.spese.feature.widget.SpeseWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * DI manuale (service locator). Un'app personale non richiede Hilt.
 * I repository delle milestone successive (Expense, Installment, Recurring,
 * Dashboard, Backup) verranno aggiunti qui.
 */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val appContext = context.applicationContext

    private val database = AppDatabase.getInstance(context)

    val settings = SettingsDataStore(context)

    val categoryRepository = CategoryRepository(database.categoryDao())

    val expenseRepository = ExpenseRepository(database.expenseDao())

    val installmentRepository = InstallmentRepository(database)

    val dashboardRepository = DashboardRepository(database.expenseDao(), database.installmentEntryDao())

    val recurringRepository = RecurringRepository(database.recurringExpenseDao())

    val recurringGenerator = RecurringGenerator(database.recurringExpenseDao(), database.expenseDao())

    val backupManager = BackupManager(database)

    val updateService = UpdateService()

    init {
        appScope.launch {
            categoryRepository.seedDefaultsIfEmpty()
            // Materializza le istanze ricorrenti mancanti (mese corrente + previsioni), idempotente.
            recurringGenerator.generateUpTo()
            // Primo refresh del widget dopo la generazione iniziale.
            runCatching { SpeseWidget().updateAll(appContext) }
        }
        observeDataForWidget()
    }

    /**
     * Tiene il widget allineato ai dati: a ogni modifica di spese/rate/ricorrenti Room notifica
     * l'observer, che richiede a Glance il redraw. Senza questo il widget restava "congelato".
     */
    private fun observeDataForWidget() {
        val tables = arrayOf("expense", "installment_entry", "installment_plan", "recurring_expense")
        database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(tables) {
                override fun onInvalidated(tables: Set<String>) {
                    appScope.launch { runCatching { SpeseWidget().updateAll(appContext) } }
                }
            }
        )
    }
}
