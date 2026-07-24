package com.personal.spese.ui.theme

import androidx.compose.ui.graphics.Color

/** Colore stabile per categoria (derivato dall'id): non richiede modifiche al DB. */
object CategoryColors {
    private val palette = listOf(
        Color(0xFF2E7D64), // verde
        Color(0xFF4E79A7), // blu
        Color(0xFFE1575A), // rosso
        Color(0xFFF28E2B), // arancio
        Color(0xFF76B041), // lime
        Color(0xFF9C6ADE), // viola
        Color(0xFF57A6A1), // teal
        Color(0xFFD4A017), // oro
        Color(0xFFB07AA1), // magenta
        Color(0xFF8C7B6B)  // bruno
    )

    fun forId(id: Long): Color {
        val size = palette.size
        val idx = ((id % size).toInt() + size) % size
        return palette[idx]
    }
}
