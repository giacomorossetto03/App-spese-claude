package com.personal.spese.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.personal.spese.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val budgetKey = longPreferencesKey("monthly_budget_cents")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[themeKey] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    /** Budget mensile in centesimi; 0 = nessun budget impostato. */
    val monthlyBudgetCents: Flow<Long> = context.dataStore.data.map { it[budgetKey] ?: 0L }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setMonthlyBudget(cents: Long) {
        context.dataStore.edit { it[budgetKey] = cents }
    }
}
