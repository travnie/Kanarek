package com.kanarek.screenshots

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterViewAnimator
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.work.Configuration
import androidx.work.WorkManager
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.kanarek.HomeActivity
import com.kanarek.R
import com.kanarek.data.NewsItem
import com.kanarek.data.Station
import com.kanarek.data.StationKind
import com.kanarek.widget.KanarekWidgetProvider
import com.kanarek.widget.NewsRemoteViewsService
import com.kanarek.widget.NewsWidgetConfig
import com.kanarek.widget.NewsWidgetSnapshot
import com.kanarek.widget.NewsWidgetStore
import com.kanarek.widget.PlayerWidgetProvider
import com.kanarek.widget.PlayerWidgetState
import com.kanarek.widget.QuoteRemoteViewsService
import com.kanarek.widget.WidgetSizeClass
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM screenshots of the app pages and launcher widgets, for reviewing UI changes without an
 * emulator. Record: `./gradlew :app:recordRoborazziPlayDebug --tests '*ScreenshotTest*'`;
 * PNGs land in app/build/outputs/roborazzi. Feeds load live when the network allows, so these
 * are for review, not golden-image verification.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val app: Application get() = RuntimeEnvironment.getApplication()

    private fun page(
        index: Int,
        name: String,
    ) {
        // Initialise native graphics' JNI class cache on the test thread. If a background thread
        // (player/Coil) gets there first, the JNI lookup of java.nio buffers fails and aborts.
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).copyPixelsToBuffer(java.nio.IntBuffer.allocate(1))
        ActivityScenario.launch(HomeActivity::class.java).use {
            idle()
            if (index == HomeActivity.PAGE_PLAYER) {
                // Navigate like a user: under Robolectric a cold-start EXTRA_PAGE doesn't move the
                // pager (it does on a device; the emulator job checks that).
                // The bottom-bar tab; the closed drawer holds an off-screen twin.
                val tabs = compose.onAllNodes(hasText(app.getString(R.string.player_title)) and hasClickAction())
                val onScreen = tabs.fetchSemanticsNodes().indexOfFirst { it.boundsInRoot.left >= 0f }
                tabs[onScreen].performClick()
                idle()
                tabs[onScreen].assertIsSelected()
            }
            captureScreenRoboImage("$OUT/$name.png")
        }
    }

    private fun idle() {
        repeat(3) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1)) }
        compose.waitForIdle()
    }

    @Test fun readerLight() = page(0, "reader_light")

    @Test fun playerLight() = page(1, "player_light")

    @Test
    @Config(qualifiers = "+night")
    fun readerDark() = page(0, "reader_dark")

    @Test
    @Config(qualifiers = "+night")
    fun playerDark() = page(1, "player_dark")

    @Test
    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    fun readerTablet() = page(0, "reader_tablet")

    @Test
    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    fun playerTablet() = page(1, "player_tablet")

    private fun capture(
        views: RemoteViews,
        name: String,
        widthDp: Int,
        heightDp: Int,
        list: Triple<Int, Int, RemoteViewsService.RemoteViewsFactory>? = null,
    ) {
        val host = FrameLayout(app)
        views.apply(app, host).let(host::addView)
        // Robolectric binds no RemoteViewsService; feed the collection from its factory directly.
        list?.let { (id, emptyId, factory) ->
            host.findViewById<AdapterView<*>>(id).apply {
                emptyView = host.findViewById(emptyId)
                bind(factory)
                (this as? AdapterViewAnimator)?.displayedChild = 0 // flipper shows nothing until told
            }
        }
        val d = app.resources.displayMetrics.density
        // Two passes with the looper run in between: AdapterViewFlipper adds its child during
        // the first layout and fades it in from alpha 0.
        repeat(2) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
            host.measure(
                View.MeasureSpec.makeMeasureSpec((widthDp * d).toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((heightDp * d).toInt(), View.MeasureSpec.EXACTLY),
            )
            host.layout(0, 0, host.measuredWidth, host.measuredHeight)
        }
        val bitmap = Bitmap.createBitmap(host.measuredWidth, host.measuredHeight, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(WALLPAPER) // widgets are translucent; show them over a launcher-ish backdrop
            host.draw(this)
        }
        bitmap.captureRoboImage("$OUT/$name.png")
    }

    @Suppress("UNCHECKED_CAST")
    private fun AdapterView<*>.bind(factory: RemoteViewsService.RemoteViewsFactory) {
        factory.onCreate()
        factory.onDataSetChanged()
        (this as AdapterView<BaseAdapter>).adapter =
            object : BaseAdapter() {
                override fun getCount() = factory.count

                override fun getItem(position: Int) = position

                override fun getItemId(position: Int) = factory.getItemId(position)

                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup,
                ): View = factory.getViewAt(position).apply(app, parent)
            }
    }

    private fun factory(
        service: Class<out RemoteViewsService>,
    ): RemoteViewsService.RemoteViewsFactory =
        Robolectric
            .setupService(service)
            .onGetViewFactory(Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ID))

    private fun size(sizeClass: WidgetSizeClass): Pair<Int, Int> =
        when (sizeClass.ordinal) {
            0 -> 180 to 110
            1 -> 320 to 180
            else -> 320 to 360
        }

    @Test
    fun widgets() {
        val station =
            Station(
                id = "radio",
                name = "Radio Example",
                streamUrl = "https://example.com/live",
                groupTitle = "Music",
                kind = StationKind.RADIO,
            )
        val config =
            NewsWidgetConfig(
                feeds = listOf("https://example.com/feed.xml"),
                headlines = false,
                intervalSeconds = 7,
            )
        // NewsRemoteViewsService reconciles refresh work on bind; the app disables auto-init.
        WorkManager.initialize(app, Configuration.Builder().build())
        NewsWidgetStore(app).apply {
            saveConfig(ID, config)
            saveSnapshot(ID, NewsWidgetSnapshot(FIXTURE_NEWS, lastUpdatedMillis = 1_700_000_000_000L))
        }
        WidgetSizeClass.entries.forEach { sizeClass ->
            val (w, h) = size(sizeClass)
            val tag = sizeClass.name.lowercase()
            capture(
                PlayerWidgetProvider.buildViews(
                    context = app,
                    appWidgetId = ID,
                    state =
                        PlayerWidgetState(station = station, isPlaying = true, nowPlaying = "Artist — Track title"),
                    sizeClass = sizeClass,
                ),
                "widget_player_$tag",
                w,
                h,
            )
            capture(
                KanarekWidgetProvider().buildViews(
                    context = app,
                    appWidgetId = ID,
                    config = config,
                    lastUpdatedMillis = 1_700_000_000_000L,
                    sizeClass = sizeClass,
                ),
                "widget_news_$tag",
                w,
                h,
                Triple(R.id.news_flipper, R.id.widget_empty, factory(NewsRemoteViewsService::class.java)),
            )
            capture(
                RemoteViews(app.packageName, R.layout.quote_widget),
                "widget_quote_$tag",
                w,
                h,
                Triple(R.id.quote_list, R.id.quote_empty, factory(QuoteRemoteViewsService::class.java)),
            )
        }
    }

    private companion object {
        const val OUT = "build/outputs/roborazzi"
        const val ID = 42
        const val WALLPAPER = 0xFF3A5A78.toInt()
        val FIXTURE_NEWS =
            listOf(
                "Kanarek ships JVM screenshot tests" to "Roborazzi renders pages and widgets without an emulator.",
                "Second headline that is long enough to wrap onto another line" to "Short summary.",
                "Third item" to "Summary three.",
            ).mapIndexed { i, (title, summary) ->
                NewsItem(
                    title = title,
                    link = "https://example.com/$i",
                    summary = summary,
                    imageUrl = null,
                    source = "Example News",
                    publishedAtMillis = 1_700_000_000_000L - i * 600_000L,
                )
            }
    }
}
