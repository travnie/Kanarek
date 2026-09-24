package com.kanarek.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.kanarek.R

class QuoteRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        QuoteRemoteViewsFactory(
            context = applicationContext,
            appWidgetId =
                intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                ),
        )
}

private class QuoteRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {
    private var quote: QuoteItem? = null
    private var typography = QuoteWidgetTypography(quoteSp = 17f, authorSp = 12f)

    override fun onCreate() {}

    override fun onDataSetChanged() {
        quote = QuoteRepository.today(context)
        val options =
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                android.os.Bundle()
            } else {
                AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
            }
        val orientation = context.resources.configuration.orientation
        typography =
            quoteWidgetTypography(
                widthDp = options.widgetWidthDp(orientation),
                heightDp = options.widgetHeightDp(orientation),
                quoteLength = quote?.quote?.length ?: 0,
            )
    }

    override fun onDestroy() {
        quote = null
    }

    override fun getCount(): Int = if (quote == null) 0 else 1

    override fun getViewAt(position: Int): RemoteViews {
        val item = quote ?: return RemoteViews(context.packageName, R.layout.quote_widget_item)
        return RemoteViews(context.packageName, R.layout.quote_widget_item).apply {
            setTextViewText(R.id.quote_text, item.quote)
            setTextViewText(R.id.quote_author, if (item.author.isBlank()) "" else "— ${item.author}")
            setViewVisibility(R.id.quote_author, if (item.author.isBlank()) View.GONE else View.VISIBLE)
            setTextViewTextSize(R.id.quote_text, TypedValue.COMPLEX_UNIT_SP, typography.quoteSp)
            setTextViewTextSize(R.id.quote_author, TypedValue.COMPLEX_UNIT_SP, typography.authorSp)
            setOnClickFillInIntent(
                R.id.quote_item_root,
                Intent().apply { data = Uri.parse(quoteWikiquoteUrl(item.author)) },
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = quote?.hashCode()?.toLong() ?: 0L
    override fun hasStableIds(): Boolean = true
}
