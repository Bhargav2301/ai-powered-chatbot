package com.polymath.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore by preferencesDataStore("folio_settings")
data class Settings(val onboarded: Boolean = false, val liveNews: Boolean = false,
    val lastRefresh: Long = 0, val refreshMessage: String = "News fetching is off. Starter lessons work offline.")
class UserSettings(context: Context) {
    private val store = context.applicationContext.settingsStore
    val values = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        Settings(it[ONBOARDED] ?: false, it[LIVE] ?: false, it[REFRESH] ?: 0,
            it[MESSAGE] ?: "News fetching is off. Starter lessons work offline.")
    }
    suspend fun finishOnboarding() { store.edit { it[ONBOARDED] = true } }
    suspend fun liveNews(enabled: Boolean) { store.edit { it[LIVE] = enabled } }
    suspend fun refreshed(result: RefreshResult) { store.edit {
        it[REFRESH] = result.refreshedAt
        it[MESSAGE] = if (result.failedSources.isEmpty()) "Checked ${result.fetched} feed entries."
            else "Checked ${result.fetched} entries. Unavailable: ${result.failedSources.joinToString()}. Cached content is still available."
    } }
    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val LIVE = booleanPreferencesKey("live_news")
        val REFRESH = longPreferencesKey("last_refresh")
        val MESSAGE = stringPreferencesKey("refresh_message")
    }
}
