package com.polymath.app

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.OkHttpClient
import androidx.room.Room
import androidx.work.*
import com.polymath.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class PolymathApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    override fun newImageLoader() = ImageLoader.Builder(this)
        .okHttpClient(OkHttpClient.Builder().followSslRedirects(false)
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build())
        .diskCache(null).build()
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.WARN).build()
    @Inject lateinit var repository: FolioRepository
    @Inject lateinit var settings: UserSettings
    @Inject lateinit var newsFetcher: NewsFetcher
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): FolioDatabase =
        Room.databaseBuilder(context, FolioDatabase::class.java, "polymath.db").addMigrations(MIGRATION_1_2).build()
    @Provides @Singleton fun repository(db: FolioDatabase) = FolioRepository(db)
    @Provides @Singleton fun settings(@ApplicationContext context: Context) = UserSettings(context)
    @Provides @Singleton fun secret(@ApplicationContext context: Context) = ServiceSecret(context)
    @Provides @Singleton fun rag() = RagClient(BuildConfig.DEBUG)
    @Provides @Singleton fun news(repository: FolioRepository) = NewsFetcher(repository)
}

class NewsRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as PolymathApplication
        if (!app.settings.values.first().liveNews) return Result.success()
        app.repository.initialize()
        val result = app.newsFetcher.refresh()
        app.settings.refreshed(result)
        return if (result.failedSources.size == NewsFetcher.sources.size && runAttemptCount < 2) Result.retry() else Result.success()
    }
    companion object {
        fun configure(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            if (!enabled) { manager.cancelUniqueWork("news-refresh"); return }
            val work = PeriodicWorkRequestBuilder<NewsRefreshWorker>(2, TimeUnit.HOURS, 30, TimeUnit.MINUTES)
                .setInitialDelay(2, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
                .build()
            manager.enqueueUniquePeriodicWork("news-refresh", ExistingPeriodicWorkPolicy.KEEP, work)
        }
    }
}
