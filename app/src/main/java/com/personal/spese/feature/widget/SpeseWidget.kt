package com.personal.spese.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.personal.spese.MainActivity
import com.personal.spese.core.db.AppDatabase
import com.personal.spese.core.util.Dates
import com.personal.spese.core.util.Money
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.util.Locale
import java.time.format.TextStyle as MonthTextStyle

/** Widget home: totale e n° movimenti del mese corrente. Tap → apre l'app. */
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
                    Text(
                        "$count movimenti",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                    )
                }
            }
        }
    }
}
