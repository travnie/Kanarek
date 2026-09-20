package com.kanarek

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.kanarek.data.NewsNotificationStore
import com.kanarek.data.SettingsStore
import com.kanarek.notifications.NewsNotificationWorker
import com.kanarek.reader.ReaderRefreshWorker
import com.kanarek.widget.WidgetRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class KanarekProcessInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val applicationContext = context.applicationContext
        // Coil decodes no SVG unless the decoder is registered. Some logos in the
        // bundled and imported playlists are SVG, and without this AsyncImage
        // fails them and falls through Favicons.logoChain to a generic favicon.
        // The factory is lazy, so this costs nothing until the first image loads.
        // Note this covers Compose only: PlayerService decodes widget and
        // notification artwork itself with BitmapFactory, which still cannot
        // read SVG.
        // Configure the process-wide singleton here before Compose asks for it;
        // a second factory registered later would be too late.
        SingletonImageLoader.setSafe(
            SingletonImageLoader.Factory {
                ImageLoader
                    .Builder(applicationContext)
                    .components {
                        add(SvgDecoder.Factory())
                        add(
                            OkHttpNetworkFetcherFactory(
                                callFactory = {
                                    OkHttpClient
                                        .Builder()
                                        .addNetworkInterceptor { chain ->
                                            val request =
                                                chain
                                                    .request()
                                                    .newBuilder()
                                                    .header(
                                                        "User-Agent",
                                                        "Kanarek Android (+https://github.com/travnie/kanarek)",
                                                    ).build()
                                            chain.proceed(request)
                                        }.build()
                                },
                            ),
                        )
                    }.build()
            },
        )
        WidgetRefreshWorker.reconcile(applicationContext)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val settings = SettingsStore(applicationContext)
            val notifications = NewsNotificationStore(applicationContext)
            reconcilePersistedSchedules(
                state =
                    ProcessScheduleState(
                        readerRefreshMinutes = settings.backgroundRefreshMinutesNow(),
                        notificationsEnabled = notifications.configNow().enabled,
                    ),
                syncReader = { minutes ->
                    ReaderRefreshWorker.syncSchedule(applicationContext, minutes)
                },
                syncNotifications = { enabled ->
                    NewsNotificationWorker.syncSchedule(applicationContext, enabled)
                },
            )
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(WorkManagerInitializer::class.java)
}

internal data class ProcessScheduleState(
    val readerRefreshMinutes: Int,
    val notificationsEnabled: Boolean,
)

internal fun reconcilePersistedSchedules(
    state: ProcessScheduleState,
    syncReader: (Int) -> Unit,
    syncNotifications: (Boolean) -> Unit,
) {
    syncReader(state.readerRefreshMinutes)
    syncNotifications(state.notificationsEnabled)
}
