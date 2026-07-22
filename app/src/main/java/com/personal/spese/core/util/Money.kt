package com.personal.spese.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Denaro sempre in centesimi (Long). Formattazione/parsing in locale IT. */
object Money {

    private val currency: NumberFormat = NumberFormat.getCurrencyInstance(Locale.ITALY)

    fun format(cents: Long): String = currency.format(BigDecimal.valueOf(cents, 2))

    /** Converte un input testuale ("12,34" o "12.34") in centesimi. Null se non valido. */
    fun parseToCents(input: String): Long? {
        val cleaned = input.trim()
            .replace("€", "")
            .replace(" ", "")
            .replace(".", "")   // separatore migliaia IT
            .replace(',', '.')  // separatore decimale
        if (cleaned.isEmpty()) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}
