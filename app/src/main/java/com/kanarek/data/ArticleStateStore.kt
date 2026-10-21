package com.kanarek.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val Context.articleStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "article_state")

class ArticleStateStore(
    private val context: Context,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val offlineStore = OfflineArticleStore(context.applicationContext)

    val state: Flow<ArticleState> =
        flow {
            migrateEmbeddedOfflineArticles()
            emitAll(
                combine(context.articleStateDataStore.data, SAVED_REVISION) { prefs, _ ->
                    val now = nowMillis()
                    val savedRecords =
                        SAVED_MUTEX.withLock {
                            val latest = context.articleStateDataStore.data.first()
                            loadSavedRecords(latest[KEY_SAVED].orEmpty())
                        }
                    ArticleState(
                        readIds =
                            ArticleIdHistory.ids(
                                ArticleIdHistory.prune(
                                    records = prefs[KEY_READ].orEmpty(),
                                    nowMillis = now,
                                    maxAgeMillis = READ_MAX_AGE_MILLIS,
                                    maxCount = MAX_HISTORY_ITEMS,
                                ),
                            ),
                        savedArticles = savedRecords.map(SavedArticleRecord::item),
                        savedFeedByArticle = savedRecords.mapNotNull { record ->
                            record.feedUrl?.let { ArticleStates.id(record.item) to it }
                        }.toMap(),
                        hiddenIds =
                            ArticleIdHistory.ids(
                                ArticleIdHistory.prune(
                                    records = prefs[KEY_HIDDEN].orEmpty(),
                                    nowMillis = now,
                                    maxAgeMillis = HIDDEN_MAX_AGE_MILLIS,
                                    maxCount = MAX_HISTORY_ITEMS,
                                ),
                            ),
                        offlineArticles =
                            savedRecords.mapNotNull { record ->
                                record.offline?.let { ArticleStates.id(record.item) to it }
                            }.toMap(),
                    )
                },
            )
        }

    suspend fun markRead(item: NewsItem) {
        val id = ArticleStates.id(item)
        if (id.isBlank()) return
        context.articleStateDataStore.edit { prefs ->
            val now = nowMillis()
            pruneHistories(prefs, now)
            prefs[KEY_READ] =
                ArticleIdHistory.touch(
                    records = prefs[KEY_READ].orEmpty(),
                    id = id,
                    nowMillis = now,
                    maxAgeMillis = READ_MAX_AGE_MILLIS,
                    maxCount = MAX_HISTORY_ITEMS,
                )
        }
    }

    suspend fun toggleSaved(item: NewsItem, feedUrl: String? = null) {
        val id = ArticleStates.id(item)
        if (id.isBlank()) return
        SAVED_MUTEX.withLock {
            val now = nowMillis()
            val records = currentSavedRecords().toMutableList()
            val matchingIndex = records.indexOfFirst { ArticleStates.id(it.item) == id }
            if (matchingIndex < 0) {
                records += SavedArticleRecord(item = item, savedAtMillis = now, feedUrl = feedUrl)
            } else {
                records.removeAt(matchingIndex)
            }
            persistSavedRecords(records) { prefs -> pruneHistories(prefs, now) }
        }
    }

    /** Adds full reader text only while the bookmark still exists; a late fetch cannot restore it. */
    suspend fun saveOffline(
        item: NewsItem,
        article: CleanArticle,
    ) {
        val id = ArticleStates.id(item)
        if (id.isBlank()) return
        val offline = OfflineArticles.fromCleanArticle(article, nowMillis()) ?: return
        SAVED_MUTEX.withLock {
            val records = currentSavedRecords().toMutableList()
            val matchingIndex = records.indexOfFirst { ArticleStates.id(it.item) == id }
            if (matchingIndex < 0) return@withLock
            records[matchingIndex] = records[matchingIndex].copy(offline = offline)
            persistSavedRecords(records)
        }
    }

    suspend fun hide(item: NewsItem) {
        val id = ArticleStates.id(item)
        if (id.isBlank()) return
        SAVED_MUTEX.withLock {
            val now = nowMillis()
            val savedRecords = currentSavedRecords().filterNot { ArticleStates.id(it.item) == id }
            persistSavedRecords(savedRecords) { prefs ->
                pruneHistories(prefs, now)
                prefs[KEY_HIDDEN] =
                    ArticleIdHistory.touch(
                        records = prefs[KEY_HIDDEN].orEmpty(),
                        id = id,
                        nowMillis = now,
                        maxAgeMillis = HIDDEN_MAX_AGE_MILLIS,
                        maxCount = MAX_HISTORY_ITEMS,
                    )
            }
        }
    }

    suspend fun clearReadAndHidden() {
        context.articleStateDataStore.edit { prefs ->
            prefs.remove(KEY_READ)
            prefs.remove(KEY_HIDDEN)
        }
    }

    suspend fun clearSavedArticles() {
        SAVED_MUTEX.withLock { persistSavedRecords(emptyList()) }
    }

    internal suspend fun portableSavedRecordsNow(): Set<String> =
        SAVED_MUTEX.withLock {
            migrateEmbeddedOfflineArticlesLocked()
            currentSavedRecords().mapTo(linkedSetOf(), SavedArticleCodec::encodeRecord)
        }

    internal suspend fun replacePortableSavedRecords(records: Set<String>) {
        val decoded = records.mapNotNull(SavedArticleCodec::decodeRecord)
        if (decoded.size != records.size) throw BackupFormatException("Invalid saved article")
        SAVED_MUTEX.withLock { persistSavedRecords(decoded) }
    }

    private suspend fun currentSavedRecords(): List<SavedArticleRecord> =
        loadSavedRecords(context.articleStateDataStore.data.first()[KEY_SAVED].orEmpty())

    private suspend fun loadSavedRecords(rawRecords: Set<String>): List<SavedArticleRecord> {
        val records = SavedArticleCodec.decodeRecords(rawRecords)
        return withContext(Dispatchers.IO) {
            records.map { record ->
                if (record.offline != null) {
                    record
                } else {
                    record.copy(offline = offlineStore.read(record))
                }
            }
        }
    }

    private suspend fun persistSavedRecords(
        records: List<SavedArticleRecord>,
        updatePreferences: (MutablePreferences) -> Unit = {},
    ) {
        val normalized = SavedArticleCodec.normalizeRecords(records)
        val bounded = OfflineArticles.enforceLimit(normalized, OFFLINE_CONTENT_LIMIT_BYTES)
        val compact = compactRecords(bounded)
        val staged = withContext(Dispatchers.IO) { offlineStore.stage(bounded) }
        var published = false
        try {
            context.articleStateDataStore.edit { prefs ->
                updatePreferences(prefs)
                writeCompactRecords(prefs, compact)
            }
            withContext(Dispatchers.IO) { offlineStore.publish(staged) }
            published = true
        } finally {
            if (!published) {
                withContext(NonCancellable + Dispatchers.IO) { offlineStore.discard(staged) }
            }
        }
        withContext(Dispatchers.IO) { runCatching { offlineStore.pruneTo(bounded) } }
        bumpSavedRevision()
    }

    private suspend fun migrateEmbeddedOfflineArticles() {
        SAVED_MUTEX.withLock { migrateEmbeddedOfflineArticlesLocked() }
    }

    private suspend fun migrateEmbeddedOfflineArticlesLocked() {
        val raw = context.articleStateDataStore.data.first()[KEY_SAVED].orEmpty()
        val records = SavedArticleCodec.decodeRecords(raw)
        if (records.none { it.offline != null }) {
            withContext(Dispatchers.IO) { runCatching { offlineStore.reconcile(records) } }
            return
        }
        val bounded = OfflineArticles.enforceLimit(records, OFFLINE_CONTENT_LIMIT_BYTES)
        val stored =
            runCatching {
                withContext(Dispatchers.IO) { offlineStore.writeMigration(bounded) }
            }.isSuccess
        if (!stored) return

        val compact = compactRecords(bounded)
        var migrated = false
        context.articleStateDataStore.edit { prefs ->
            if (prefs[KEY_SAVED].orEmpty() != raw) return@edit
            writeCompactRecords(prefs, compact)
            migrated = true
        }
        if (migrated) {
            withContext(Dispatchers.IO) { runCatching { offlineStore.reconcile(compactRecordsAsRecords(compact)) } }
            bumpSavedRevision()
        }
    }

    private fun compactRecordsAsRecords(records: Set<String>): List<SavedArticleRecord> =
        SavedArticleCodec.decodeRecords(records)

    private fun compactRecords(records: List<SavedArticleRecord>): Set<String> =
        records.mapTo(linkedSetOf()) { record ->
            SavedArticleCodec.encodeRecord(record.copy(offline = null))
        }

    private fun writeCompactRecords(
        prefs: MutablePreferences,
        records: Set<String>,
    ) {
        if (records.isEmpty()) {
            prefs.remove(KEY_SAVED)
        } else {
            prefs[KEY_SAVED] = records
        }
    }

    private fun pruneHistories(
        prefs: MutablePreferences,
        now: Long,
    ) {
        prefs[KEY_READ] =
            ArticleIdHistory.prune(
                records = prefs[KEY_READ].orEmpty(),
                nowMillis = now,
                maxAgeMillis = READ_MAX_AGE_MILLIS,
                maxCount = MAX_HISTORY_ITEMS,
            )
        prefs[KEY_HIDDEN] =
            ArticleIdHistory.prune(
                records = prefs[KEY_HIDDEN].orEmpty(),
                nowMillis = now,
                maxAgeMillis = HIDDEN_MAX_AGE_MILLIS,
                maxCount = MAX_HISTORY_ITEMS,
            )
    }

    private fun bumpSavedRevision() {
        SAVED_REVISION.value += 1L
    }

    companion object {
        const val OFFLINE_CONTENT_LIMIT_BYTES = 2L * 1024L * 1024L

        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        private const val READ_MAX_AGE_MILLIS = 90L * DAY_MILLIS
        private const val HIDDEN_MAX_AGE_MILLIS = 180L * DAY_MILLIS
        private const val MAX_HISTORY_ITEMS = 2_000

        private val SAVED_MUTEX = Mutex()
        private val SAVED_REVISION = MutableStateFlow(0L)
        private val KEY_READ = stringSetPreferencesKey("read_article_ids")
        private val KEY_SAVED = stringSetPreferencesKey("saved_articles")
        private val KEY_HIDDEN = stringSetPreferencesKey("hidden_article_ids")
    }
}
