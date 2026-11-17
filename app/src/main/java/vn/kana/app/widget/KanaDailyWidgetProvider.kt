package vn.kana.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import vn.kana.app.MainActivity
import vn.kana.app.R
import vn.kana.app.domain.DailyTipsCatalog
import java.time.LocalDate

class KanaDailyWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NEXT_TIP) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentOffset = prefs.getInt(KEY_TIP_OFFSET, 0)
            prefs.edit().putInt(KEY_TIP_OFFSET, currentOffset + 1).apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, KanaDailyWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                onUpdate(context, manager, ids)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val offset = prefs.getInt(KEY_TIP_OFFSET, 0)
        val streakDays = prefs.getInt(KEY_STREAK_DAYS, 0)

        val articles = DailyTipsCatalog.articles
        val baseIndex = ((LocalDate.now().toEpochDay() + offset) % articles.size).toInt()
            .let { if (it < 0) it + articles.size else it }
        val tip = articles[baseIndex]

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val launchPendingIntent = PendingIntent.getActivity(
            context,
            3001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextTipIntent = Intent(context, KanaDailyWidgetProvider::class.java).apply {
            action = ACTION_NEXT_TIP
        }
        val nextTipPendingIntent = PendingIntent.getBroadcast(
            context,
            3002,
            nextTipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_kana_daily).apply {
                setTextViewText(R.id.widget_badge, "${tip.issueLabel} • ${tip.category.uppercase()}")
                setTextViewText(R.id.widget_streak, "$streakDays ngày")
                setTextViewText(R.id.widget_highlight_jp, "${tip.highlightJp}  (${tip.highlightRomaji})")
                setTextViewText(R.id.widget_title, tip.titleVi)
                setTextViewText(R.id.widget_example, tip.funFactVi)
                setOnClickPendingIntent(R.id.widget_btn_study, launchPendingIntent)
                setOnClickPendingIntent(R.id.widget_root, launchPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_next, nextTipPendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        private const val ACTION_NEXT_TIP = "vn.kana.app.widget.ACTION_NEXT_TIP"
        private const val PREFS_NAME = "kana_widget_prefs"
        private const val KEY_TIP_OFFSET = "tip_offset"
        private const val KEY_STREAK_DAYS = "streak_days"

        fun notifyWidgetDataChanged(context: Context, streakDays: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_STREAK_DAYS, streakDays)
                .apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, KanaDailyWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val updateIntent = Intent(context, KanaDailyWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(updateIntent)
            }
        }
    }
}
