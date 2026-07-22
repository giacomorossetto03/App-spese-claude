package com.personal.spese.di

import android.content.Context
import com.personal.spese.core.datastore.SettingsDataStore
import com.personal.spese.core.db.AppDatabase
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.DashboardRepository
import com.personal.spese.data.repository.ExpenseRepository
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

    val dashboardRepository = DashboardRepository(database.expenseDao())

    init {
        appScope.launch { categoryRepository.seedDefaultsIfEmpty() }
    }
}
