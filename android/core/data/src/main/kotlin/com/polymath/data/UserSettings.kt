package com.polymath.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore by preferencesDataStore("folio_settings")
data class Settings(val onboarded: Boolean = false, val liveNews: Boolean = false,
    val aiEnabled: Boolean = false, val aiEndpoint: String = "",
    val lastRefresh: Long = 0, val refreshMessage: String = "News fetching is off. Starter lessons work offline.",
    val localAi: Boolean = true)
class UserSettings(context: Context) {
    private val store = context.applicationContext.settingsStore
    val values = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        Settings(it[ONBOARDED] ?: false, it[LIVE] ?: false, it[AI_ENABLED] ?: false, it[AI_ENDPOINT] ?: "", it[REFRESH] ?: 0,
            it[MESSAGE] ?: "News fetching is off. Starter lessons work offline.", it[LOCAL_AI] ?: true)
    }
    suspend fun aiConnection(endpoint: String, enabled: Boolean) { store.edit { it[AI_ENDPOINT] = endpoint; it[AI_ENABLED] = enabled } }
    suspend fun localAi(enabled: Boolean) { store.edit { it[LOCAL_AI] = enabled } }
    suspend fun finishOnboarding() { store.edit { it[ONBOARDED] = true } }
    suspend fun liveNews(enabled: Boolean) { store.edit { it[LIVE] = enabled } }
    suspend fun refreshed(result: RefreshResult) { store.edit {
        it[REFRESH] = result.refreshedAt
        it[MESSAGE] = if (result.failedSources.isEmpty()) "Checked ${result.fetched} feed entries."
            else "Checked ${result.fetched} entries. Unavailable: ${result.failedSources.joinToString()}. Cached content is still available."
    } }
    private companion object {
        val LOCAL_AI = booleanPreferencesKey("local_ai")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_ENDPOINT = stringPreferencesKey("ai_endpoint")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val LIVE = booleanPreferencesKey("live_news")
        val REFRESH = longPreferencesKey("last_refresh")
        val MESSAGE = stringPreferencesKey("refresh_message")
    }
}
