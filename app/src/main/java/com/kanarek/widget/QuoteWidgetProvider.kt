package com.kanarek.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.kanarek.R

/** Resizable home-screen quote widget with a deliberately inexact daily refresh. */
class QuoteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        update(context, appWidgetManager, appWidgetId)
    }

    private fun update(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val views =
            RemoteViews(context.packageName, R.layout.quote_widget).apply {
                val serviceIntent =
                    Intent(context, QuoteRemoteViewsService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        data = Uri.parse("kanarek://quote/$appWidgetId")
                    }
                setRemoteAdapter(R.id.quote_list, serviceIntent)
                setEmptyView(R.id.quote_list, R.id.quote_empty)
                val openTemplate =
                    PendingIntent.getActivity(
                        context,
                        QUOTE_REQUEST_BASE + appWidgetId,
                        Intent(context, ArticleRedirectActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    )
                setPendingIntentTemplate(R.id.quote_list, openTemplate)
            }
        manager.updateAppWidget(appWidgetId, views)
        manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.quote_list)
    }

    private companion object {
        const val QUOTE_REQUEST_BASE = 70_000
    }
}
