package com.personal.spese.di

import android.content.Context
import com.personal.spese.core.datastore.SettingsDataStore
import com.personal.spese.core.db.AppDatabase
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.DashboardRepository
import com.personal.spese.data.repository.ExpenseRepository
import com.personal.spese.data.repository.InstallmentRepository
import com.personal.spese.data.repository.RecurringGenerator
import com.personal.spese.data.repository.RecurringRepository
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

    private val database = AppDatabase.getInstance(context)

    val settings = SettingsDataStore(context)

    val categoryRepository = CategoryRepository(database.categoryDao())

    val expenseRepository = ExpenseRepository(database.expenseDao())

    val installmentRepository = InstallmentRepository(database)

    val dashboardRepository = DashboardRepository(database.expenseDao(), database.installmentEntryDao())

    val recurringRepository = RecurringRepository(database.recurringExpenseDao())

    val recurringGenerator = RecurringGenerator(database.recurringExpenseDao(), database.expenseDao())

    init {
        appScope.launch {
            categoryRepository.seedDefaultsIfEmpty()
            // Materializza le istanze ricorrenti mancanti fino al mese corrente (idempotente).
            recurringGenerator.generateUpTo()
        }
    }
}
