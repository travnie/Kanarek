package com.kanarek.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.kanarek.R
import com.kanarek.data.Headlines
import com.kanarek.data.NewsItem
import com.kanarek.data.NewsRepository
import com.kanarek.data.SettingsStore
import com.kanarek.data.readBytesCapped
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class NewsRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        WidgetRefreshWorker.reconcile(applicationContext)
        return NewsRemoteViewsFactory(
            context = applicationContext,
            appWidgetId =
                intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                ),
        )
    }
}

private class NewsRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {
    private val settings = SettingsStore(context)
    private val widgetStore = NewsWidgetStore(context)
    private var items: List<NewsItem> = emptyList()
    private var sizeClass = WidgetSizeClass.REGULAR
    private var widthDp = DEFAULT_WIDGET_WIDTH_DP

    override fun onCreate() {}

    override fun onDataSetChanged() {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            items = emptyList()
            return
        }
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val orientation = context.resources.configuration.orientation
        sizeClass = newsWidgetSizeClass(options = options, orientation = orientation)
        widthDp = options.widgetWidthDp(orientation)
        val global =
            NewsWidgetConfig(
                feeds =
                    runCatching { settings.feedsBlocking() }
                        .getOrDefault(NewsRepository.DEFAULT_FEEDS),
                headlines =
                    runCatching { settings.headlinesModeBlocking() }
                        .getOrDefault(false),
                intervalSeconds =
                    runCatching { settings.intervalSecondsBlocking() }
                        .getOrDefault(SettingsStore.DEFAULT_INTERVAL),
            )
        val config = widgetStore.configOrMigrate(appWidgetId, global)
        val base =
            itemsForWidget(
                shared = widgetStore.sharedSnapshot(),
                config = config,
                legacy = widgetStore.snapshot(appWidgetId),
                perSourceCap = runCatching { settings.perSourceCapBlocking() }.getOrDefault(0),
                limit = ITEM_CAP,
            )
        val nextItems =
            if (config.headlines && base.isNotEmpty()) {
                val top = runCatching { settings.topSourcesBlocking() }.getOrDefault(emptySet())
                Headlines.headlines(base, topSources = top, limit = HEADLINES_CAP)
            } else {
                base
            }
        widgetStore.runIfCurrent(appWidgetId, config) {
            items = nextItems
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewTypeCount(): Int = WidgetSizeClass.entries.size

    override fun getItemId(position: Int): Long =
        items
            .getOrNull(position)
            ?.link
            ?.hashCode()
            ?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewAt(position: Int): RemoteViews {
        val layoutId = newsItemLayout(sizeClass)
        val item =
            items.getOrNull(position)
                ?: return RemoteViews(context.packageName, layoutId)
        return RemoteViews(context.packageName, layoutId).apply {
            setTextViewText(R.id.item_title, item.title)
            setTextViewText(R.id.item_summary, item.summary)
            setTextViewText(R.id.item_source, item.source)
            val showSummary = sizeClass != WidgetSizeClass.COMPACT && item.summary.isNotBlank()
            setViewVisibility(R.id.item_summary, if (showSummary) View.VISIBLE else View.GONE)
            clearButtonLane(showSummary, item.summary)

            val bitmap = item.imageUrl?.let { loadBitmap(it) }
            if (bitmap != null) {
                setImageViewBitmap(R.id.item_image, bitmap)
                setViewVisibility(R.id.item_image, View.VISIBLE)
                setViewVisibility(R.id.item_scrim, View.VISIBLE)
            } else {
                setViewVisibility(R.id.item_image, View.GONE)
                setViewVisibility(R.id.item_scrim, View.GONE)
            }

            val favicon = faviconUrl(item.link)?.let { loadBitmap(it) }
            if (favicon != null) {
                setImageViewBitmap(R.id.item_favicon, favicon)
            } else {
                setImageViewResource(R.id.item_favicon, R.drawable.ic_rss_fallback)
            }
            setViewVisibility(R.id.item_favicon, View.VISIBLE)

            val fillIn = Intent().apply { data = Uri.parse(item.link) }
            setOnClickFillInIntent(R.id.item_root, fillIn)
        }
    }

    /**
     * Regular/expanded chrome puts prev/next buttons in the bottom corners. Pad the text that can
     * reach their lane sideways instead of reserving height, which short widgets don't have:
     * the summary, plus the title when there is no summary or it fits on one line.
     */
    private fun RemoteViews.clearButtonLane(
        showSummary: Boolean,
        summary: String,
    ) {
        // Block padding and summary text size of widget_item.xml / widget_item_expanded.xml.
        val (blockPaddingDp, summarySp) =
            when (sizeClass) {
                WidgetSizeClass.COMPACT -> return
                WidgetSizeClass.REGULAR -> 14 to 12f
                WidgetSizeClass.EXPANDED -> 18 to 14f
            }
        val res = context.resources
        val density = res.displayMetrics.density
        val laneDp = res.getDimensionPixelSize(R.dimen.widget_button_lane) / density
        val sideDp = (laneDp - blockPaddingDp).coerceAtLeast(0f)
        val side = (sideDp * density).toInt()
        val summaryWidthPx = (widthDp - 2 * (blockPaddingDp + sideDp)) * density
        // Measure with the summary's real paint: a one-line summary leaves the title's last line
        // inside the lane too.
        val summaryPaint =
            TextPaint().apply {
                textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, summarySp, res.displayMetrics)
            }
        val oneLineSummary = summaryPaint.measureText(summary) <= summaryWidthPx
        if (showSummary) setViewPadding(R.id.item_summary, side, 0, side, 0)
        if (!showSummary || oneLineSummary) setViewPadding(R.id.item_title, side, 0, side, 0)
    }

    private fun faviconUrl(link: String): String? {
        val host = runCatching { URI(link).host?.removePrefix("www.") }.getOrNull()
        return if (host.isNullOrBlank()) null else "https://icons.duckduckgo.com/ip3/$host.ico"
    }

    private fun loadBitmap(url: String): Bitmap? {
        WidgetImageCache.get(context, url)?.let { return it }
        return runCatching {
            val connection =
                (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = IMG_TIMEOUT_MS
                    readTimeout = IMG_TIMEOUT_MS
                    instanceFollowRedirects = true
                }
            try {
                if (connection.responseCode !in 200..299) return null
                val bytes = connection.inputStream.use { it.readBytesCapped(MAX_IMAGE_BYTES) }
                decodeScaled(bytes, MAX_IMAGE_PX)
                    ?.also { WidgetImageCache.put(context, url, it) }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private fun decodeScaled(
        bytes: ByteArray,
        maxPx: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        var width = bounds.outWidth
        var height = bounds.outHeight
        while (width / 2 >= maxPx || height / 2 >= maxPx) {
            width /= 2
            height /= 2
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    companion object {
        private const val ITEM_CAP = 12
        private const val HEADLINES_CAP = 6
        private const val MAX_IMAGE_PX = 400
        private const val MAX_IMAGE_BYTES = 3 * 1024 * 1024
        private const val IMG_TIMEOUT_MS = 6_000
    }
}
