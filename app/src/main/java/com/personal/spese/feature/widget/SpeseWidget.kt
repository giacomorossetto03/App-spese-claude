package com.personal.spese.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.personal.spese.MainActivity
import com.personal.spese.core.db.AppDatabase
import com.personal.spese.core.util.Dates
import com.personal.spese.core.util.Money
import com.personal.spese.ui.theme.CategoryColors
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.util.Locale
import java.time.format.TextStyle as MonthTextStyle

/** Fetta di ripartizione: colore categoria, frazione sul totale, nome e importo (centesimi). */
private data class WidgetSlice(val color: Color, val fraction: Float, val name: String, val amountCents: Long)

/** Widget home: totale, barra ripartizione categorie (nativa) + legenda e n° movimenti. Tap → apre l'app. */
class SpeseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val ym = YearMonth.now()
        val (start, end) = Dates.monthBounds(ym)

        val total = db.expenseDao().sumInMonth(start, end).first() +
            db.installmentEntryDao().sumDueInMonth(start, end).first()
        val count = db.expenseDao().countInMonth(start, end).first() +
            db.installmentEntryDao().countDueInMonth(start, end).first()
        val monthLabel = ym.month.getDisplayName(MonthTextStyle.FULL, Locale.ITALY)
            .replaceFirstChar { it.uppercase(Locale.ITALY) }

        // Ripartizione per categoria (spese + rate dovute), con nome categoria per la legenda.
        val nameById = db.categoryDao().observeAll().first().associate { it.id to it.name }
        val byCat = HashMap<Long, Long>()
        (db.expenseDao().sumByCategoryInMonth(start, end).first() +
            db.installmentEntryDao().sumDueByCategoryInMonth(start, end).first())
            .forEach { byCat[it.categoryId] = (byCat[it.categoryId] ?: 0L) + it.total }
        val catTotal = byCat.values.sum()
        val slices: List<WidgetSlice> = if (catTotal > 0L) {
            byCat.entries.sortedByDescending { it.value }.map { (id, amount) ->
                WidgetSlice(
                    color = CategoryColors.forId(id),
                    fraction = amount.toFloat() / catTotal.toFloat(),
                    name = nameById[id] ?: "—",
                    amountCents = amount
                )
            }
        } else emptyList()

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(16.dp)
                        .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
                ) {
                    Text(
                        "Spese · $monthLabel",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                    )
                    Text(
                        Money.format(total),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    if (slices.isNotEmpty()) {
                        Spacer(GlanceModifier.height(10.dp))
                        ProportionBar(slices)
                        Spacer(GlanceModifier.height(8.dp))
                        Legend(slices)
                    }

                    Text(
                        "$count movimenti",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                        modifier = GlanceModifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/** Barra proporzionale nativa: N celle a peso uguale, ciascuna colorata dalla categoria che la "copre". */
@Composable
private fun ProportionBar(slices: List<WidgetSlice>) {
    val cells = 24
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(12.dp)
            .cornerRadius(6.dp)
    ) {
        for (i in 0 until cells) {
            val pos = (i + 0.5f) / cells
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .background(ColorProvider(colorAtFraction(slices, pos)))
            ) {}
        }
    }
}

/** Legenda: fino a 3 categorie principali (quadratino colorato + nome · importo). */
@Composable
private fun Legend(slices: List<WidgetSlice>) {
    slices.take(3).forEach { slice ->
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(10.dp)
                    .cornerRadius(2.dp)
                    .background(ColorProvider(slice.color))
            ) {}
            Spacer(GlanceModifier.width(8.dp))
            Text(
                slice.name,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                Money.format(slice.amountCents),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            )
        }
    }
}

/** Colore della categoria la cui frazione cumulata contiene [pos] (0..1). */
private fun colorAtFraction(slices: List<WidgetSlice>, pos: Float): Color {
    var acc = 0f
    for (s in slices) {
        acc += s.fraction
        if (pos <= acc) return s.color
    }
    return slices.last().color
}
