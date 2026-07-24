package com.personal.spese.feature.installments.addplan

import com.personal.spese.core.model.Category
import java.time.LocalDate

data class AddInstallmentPlanUiState(
    val title: String = "",
    // false = importo diretto (totale da pagare); true = prezzo base + interessi %.
    val withInterest: Boolean = false,
    val amountInput: String = "",    // prezzo (con interessi) oppure totale (senza)
    val interestInput: String = "",  // percentuale, solo con interessi (vuota = 0%)
    val countInput: String = "",
    val firstDueDate: LocalDate = LocalDate.now(),
    val categoryId: Long? = null,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val titleError: Boolean = false,
    val amountError: Boolean = false,
    val countError: Boolean = false,
    val categoryError: Boolean = false,
    val saved: Boolean = false
)
