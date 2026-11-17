package vn.kana.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import vn.kana.app.MainActivity
import vn.kana.app.R
import vn.kana.app.domain.DailyTipsCatalog
import java.time.LocalDate

class KanaDailyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val tip = DailyTipsCatalog.getTipForEpochDay(LocalDate.now().toEpochDay())
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_kana_daily).apply {
                setTextViewText(R.id.widget_badge, "KANA N5 • ${tip.category.uppercase()}")
                setTextViewText(R.id.widget_title, tip.titleVi)
                setTextViewText(R.id.widget_example, "${tip.highlightJp} (${tip.highlightRomaji})")
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
