package dev.groceryledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.groceryledger.GroceryLedgerApplication
import dev.groceryledger.MainActivity
import dev.groceryledger.R
import dev.groceryledger.domain.date.DateRange
import dev.groceryledger.domain.money.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Home-screen widget: today's total plus a one-tap "Add item" button. The tap opens the
 * app with the add form visible; nothing is written without the user's confirmation.
 */
class GroceryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetIds.isEmpty()) return

        // Room cannot be queried on the main thread, so finish the broadcast off it.
        val pendingResult = goAsync()
        Thread {
            val total = runCatching {
                val container = (context.applicationContext as GroceryLedgerApplication).container
                val today = container.dateProvider.today()
                runBlocking {
                    container.expenseRepository.observeTotal(DateRange(today, today)).first()
                }
            }.getOrDefault(0L)

            val views = buildViews(context, total)
            appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            pendingResult.finish()
        }.apply { isDaemon = true }.start()
    }

    companion object {
        private const val REQUEST_ADD_ITEM = 1001

        /** Also used by tests; the widget never shows stale data for longer than a refresh. */
        fun buildViews(context: Context, todayTotalPaise: Long): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_grocery)
            views.setTextViewText(
                R.id.widget_total,
                context.getString(R.string.widget_today_total, Money.format(todayTotalPaise)),
            )

            val intent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_QUICK_ADD
                putExtra(MainActivity.EXTRA_QUICK_ADD, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_ADD_ITEM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setContentDescription(
                R.id.widget_root,
                context.getString(R.string.widget_description),
            )
            return views
        }

        /** Re-renders every placed widget, e.g. after the app is left and data has changed. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, GroceryWidgetProvider::class.java),
            )
            if (ids.isEmpty()) return

            val intent = Intent(context, GroceryWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
