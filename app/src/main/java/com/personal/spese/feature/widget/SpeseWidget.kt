package com.personal.spese.feature.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Canvas as NativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.personal.spese.MainActivity
import com.personal.spese.core.db.AppDatabase
import com.personal.spese.core.util.Dates
import com.personal.spese.core.util.Money
import com.personal.spese.ui.theme.CategoryColors
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.util.Locale
import java.time.format.TextStyle as MonthTextStyle

/** Widget home: totale, barra ripartizione categorie e n° movimenti del mese. Tap → apre l'app. */
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

        // Ripartizione per categoria (spese + rate dovute) per la barra.
        val byCat = HashMap<Long, Long>()
        (db.expenseDao().sumByCategoryInMonth(start, end).first() +
            db.installmentEntryDao().sumDueByCategoryInMonth(start, end).first())
            .forEach { byCat[it.categoryId] = (byCat[it.categoryId] ?: 0L) + it.total }
        val catTotal = byCat.values.sum()
        val bar: Bitmap? = if (catTotal > 0L) {
            val slices = byCat.entries.sortedByDescending { it.value }
                .map { CategoryColors.forId(it.key).toArgb() to (it.value.toFloat() / catTotal.toFloat()) }
            proportionBar(slices, widthPx = 480, heightPx = 28)
        } else null

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
                    if (bar != null) {
                        Image(
                            provider = ImageProvider(bar),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = GlanceModifier.fillMaxWidth().height(12.dp).padding(top = 6.dp)
                        )
                    }
                    Text(
                        "$count movimenti",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                        modifier = GlanceModifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }

    /** Barra orizzontale a segmenti proporzionali (argb, frazione). */
    private fun proportionBar(slices: List<Pair<Int, Float>>, widthPx: Int, heightPx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = NativeCanvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var x = 0f
        slices.forEach { (argb, fraction) ->
            val w = widthPx * fraction
            paint.color = argb
            canvas.drawRect(x, 0f, x + w, heightPx.toFloat(), paint)
            x += w
        }
        return bmp
    }
}
