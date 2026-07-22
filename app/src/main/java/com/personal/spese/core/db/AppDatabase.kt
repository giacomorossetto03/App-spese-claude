package com.personal.spese.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.personal.spese.core.db.dao.CategoryDao
import com.personal.spese.core.db.dao.ExpenseDao
import com.personal.spese.core.db.dao.InstallmentEntryDao
import com.personal.spese.core.db.dao.InstallmentPlanDao
import com.personal.spese.core.db.dao.RecurringExpenseDao
import com.personal.spese.core.db.entity.CategoryEntity
import com.personal.spese.core.db.entity.ExpenseEntity
import com.personal.spese.core.db.entity.InstallmentEntryEntity
import com.personal.spese.core.db.entity.InstallmentPlanEntity
import com.personal.spese.core.db.entity.MonthlyBudgetEntity
import com.personal.spese.core.db.entity.RecurringExpenseEntity

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        InstallmentPlanEntity::class,
        InstallmentEntryEntity::class,
        RecurringExpenseEntity::class,
        MonthlyBudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun installmentPlanDao(): InstallmentPlanDao
    abstract fun installmentEntryDao(): InstallmentEntryDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spese.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
