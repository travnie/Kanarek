package com.kanarek.screenshots

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.kanarek.HomeActivity
import com.kanarek.R
import com.kanarek.data.Station
import com.kanarek.data.StationKind
import com.kanarek.widget.KanarekWidgetProvider
import com.kanarek.widget.NewsWidgetConfig
import com.kanarek.widget.PlayerWidgetProvider
import com.kanarek.widget.PlayerWidgetState
import com.kanarek.widget.WidgetSizeClass
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
                // Navigate like a user; EXTRA_PAGE on a cold start doesn't move the pager here.
                // The bottom-bar tab; the closed drawer holds an off-screen twin.
                val tabs = compose.onAllNodes(hasText(app.getString(R.string.player_title)) and hasClickAction())
                val onScreen = tabs.fetchSemanticsNodes().indexOfFirst { it.boundsInRoot.left >= 0f }
                tabs[onScreen].performClick()
                idle()
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

    private fun capture(
        views: android.widget.RemoteViews,
        name: String,
        widthDp: Int,
        heightDp: Int,
    ) {
        val host = FrameLayout(app)
        views.apply(app, host).let(host::addView)
        val d = app.resources.displayMetrics.density
        host.measure(
            View.MeasureSpec.makeMeasureSpec((widthDp * d).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((heightDp * d).toInt(), View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, host.measuredWidth, host.measuredHeight)
        val bitmap = Bitmap.createBitmap(host.measuredWidth, host.measuredHeight, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(WALLPAPER) // widgets are translucent; show them over a launcher-ish backdrop
            host.draw(this)
        }
        bitmap.captureRoboImage("$OUT/$name.png")
    }

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
        WidgetSizeClass.entries.forEach { sizeClass ->
            val (w, h) = size(sizeClass)
            val tag = sizeClass.name.lowercase()
            capture(
                PlayerWidgetProvider.buildViews(
                    context = app,
                    appWidgetId = ID,
                    state = PlayerWidgetState(station = station, isPlaying = true, nowPlaying = "Artist — Track title"),
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
            )
        }
    }

    private companion object {
        const val OUT = "build/outputs/roborazzi"
        const val ID = 42
        const val WALLPAPER = 0xFF3A5A78.toInt()
    }
}
