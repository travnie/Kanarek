package com.kanarek.widget

import com.kanarek.R
import com.kanarek.data.NewsItem
import com.kanarek.data.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewsWidgetStateTest {
    @Test
    fun migrationCopiesGlobalDefaultsOnlyWhenWidgetHasNoConfig() {
        val global =
            NewsWidgetConfig(
                feeds = listOf("https://example.com/global.xml"),
                headlines = true,
                intervalSeconds = 15,
            )
        val stored =
            NewsWidgetConfig(
                feeds = listOf("https://example.com/widget.xml"),
                headlines = false,
                intervalSeconds = 60,
            )

        assertEquals(global, NewsWidgetConfigs.migrate(null, global))
        assertEquals(stored, NewsWidgetConfigs.migrate(stored, global))
    }

    @Test
    fun configNormalizationTrimsDeduplicatesAndBoundsValues() {
        val normalized =
            NewsWidgetConfigs.normalize(
                config =
                    NewsWidgetConfig(
                        feeds = listOf(" https://example.com/feed ", "https://example.com/feed"),
                        headlines = false,
                        intervalSeconds = 999,
                    ),
                fallbackFeeds = listOf("https://example.com/fallback"),
            )

        assertEquals(listOf("https://example.com/feed"), normalized.feeds)
        assertEquals(300, normalized.intervalSeconds)
    }

    @Test
    fun configNormalizationPreservesDisabledSlideshow() {
        val normalized =
            NewsWidgetConfigs.normalize(
                config =
                    NewsWidgetConfig(
                        feeds = listOf("https://example.com/feed"),
                        headlines = false,
                        intervalSeconds = SettingsStore.INTERVAL_OFF,
                    ),
                fallbackFeeds = listOf("https://example.com/fallback"),
            )

        assertEquals(SettingsStore.INTERVAL_OFF, normalized.intervalSeconds)
    }

    @Test
    fun configNormalizationRestoresFallbackFeeds() {
        val normalized =
            NewsWidgetConfigs.normalize(
                config =
                    NewsWidgetConfig(
                        feeds = listOf(" ", ""),
                        headlines = false,
                        intervalSeconds = 1,
                    ),
                fallbackFeeds = listOf(" https://example.com/fallback "),
            )

        assertEquals(listOf("https://example.com/fallback"), normalized.feeds)
        assertEquals(15, normalized.intervalSeconds)
    }

    @Test
    fun slideshowUsesPrecompiledRemoteViewsLayouts() {
        assertEquals(R.layout.widget_static, newsWidgetLayoutId(SettingsStore.INTERVAL_OFF))
        assertEquals(R.layout.widget, newsWidgetLayoutId(15))
        assertEquals(R.layout.widget_interval_60s, newsWidgetLayoutId(60))
        assertEquals(R.layout.widget_interval_300s, newsWidgetLayoutId(300))
    }

    @Test
    fun everyIntervalOptionHasOwnLayoutAndLegacyValuesSnap() {
        val layouts = SettingsStore.INTERVAL_OPTIONS.map(::newsWidgetLayoutId)
        assertEquals(layouts.size, layouts.toSet().size)
        assertEquals(15, SettingsStore.normalizeInterval(5))
        assertEquals(15, SettingsStore.normalizeInterval(7))
        assertEquals(15, SettingsStore.normalizeInterval(30))
        assertEquals(60, SettingsStore.normalizeInterval(45))
        assertEquals(60, SettingsStore.normalizeInterval(120))
        assertEquals(SettingsStore.DEFAULT_INTERVAL, SettingsStore.normalizeInterval(SettingsStore.DEFAULT_INTERVAL))
    }

    @Test
    fun snapshotCodecRoundTripsLastGoodItems() {
        val snapshot =
            NewsWidgetSnapshot(
                items = listOf(item("Zażółć | test")),
                lastUpdatedMillis = 456L,
            )

        assertEquals(snapshot, NewsWidgetSnapshotCodec.decode(NewsWidgetSnapshotCodec.encode(snapshot)))
    }

    @Test
    fun sharedSnapshotCodecPreservesFeedBucketsIncludingEmptyOnes() {
        val snapshot =
            SharedNewsWidgetSnapshot(
                itemsByFeed =
                    linkedMapOf(
                        "https://example.com/a.xml" to listOf(item("A")),
                        "https://example.com/empty.xml" to emptyList(),
                    ),
                lastUpdatedMillis = 789L,
            )

        assertEquals(
            snapshot,
            SharedNewsWidgetSnapshotCodec.decode(SharedNewsWidgetSnapshotCodec.encode(snapshot)),
        )
    }

    @Test
    fun corruptSnapshotIsIgnored() {
        assertNull(NewsWidgetSnapshotCodec.decode("broken"))
        assertNull(SharedNewsWidgetSnapshotCodec.decode("broken"))
    }

    private fun item(title: String): NewsItem =
        NewsItem(
            title = title,
            link = "https://example.com/${title.hashCode()}",
            summary = "Line one\nLine two",
            imageUrl = "https://example.com/image.jpg",
            source = "Źródło",
            publishedAtMillis = 123L,
        )
}
