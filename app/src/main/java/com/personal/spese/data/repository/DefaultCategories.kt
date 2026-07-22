package com.personal.spese.data.repository

import com.personal.spese.core.db.entity.CategoryEntity

/** Categorie predefinite create al primo avvio. Modificabili/archiviabili dall'utente. */
object DefaultCategories {
    private val names = listOf(
        "Alimentari", "Casa", "Trasporti", "Bollette", "Salute",
        "Svago", "Ristoranti", "Abbigliamento", "Istruzione", "Altro"
    )

    fun list(): List<CategoryEntity> = names.mapIndexed { i, n ->
        CategoryEntity(
            name = n,
            colorArgb = null,
            isDefault = true,
            isArchived = false,
            sortOrder = i
        )
    }
}
