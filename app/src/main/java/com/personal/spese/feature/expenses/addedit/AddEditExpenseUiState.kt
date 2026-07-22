package com.personal.spese.feature.expenses.addedit

import com.personal.spese.core.model.Category
import com.personal.spese.core.model.ExpenseType
import java.time.LocalDate

data class AddEditExpenseUiState(
    val isEdit: Boolean = false,
    val type: ExpenseType = ExpenseType.SINGLE,
    val amountInput: String = "",
    val categoryId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val paymentMethod: String = "",
    val categories: List<Category> = emptyList(),
    val amountError: Boolean = false,
    val categoryError: Boolean = false,
    val saved: Boolean = false
) {
    val selectedCategoryName: String?
        get() = categories.firstOrNull { it.id == categoryId }?.name
}
