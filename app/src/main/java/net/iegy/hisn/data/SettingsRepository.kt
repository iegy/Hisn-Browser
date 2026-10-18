package net.iegy.hisn.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.hisnDataStore by preferencesDataStore(name = "hisn_settings")
private val Context.hisnPrivateDataStore by preferencesDataStore(name = "hisn_settings_private")

data class Shortcut(val title: String, val url: String)

data class BrowserSettings(
    val theme: String = "system",
    val language: String = "ar",
    val searchEngine: String = "google",
    val blockingEnabled: Boolean = true,
    val desktopSiteDefault: Boolean = false,
    val allowlist: Set<String> = emptySet(),
    val shortcuts: List<Shortcut> = defaultShortcuts()
)

fun defaultShortcuts() = listOf(
    Shortcut("Google", "https://www.google.com"),
    Shortcut("YouTube", "https://www.youtube.com"),
    Shortcut("Wikipedia", "https://www.wikipedia.org"),
    Shortcut("DuckDuckGo", "https://duckduckgo.com")
)

class SettingsRepository(private val context: Context, privateMode: Boolean = false) {
    private val dataStore = if (privateMode) context.hisnPrivateDataStore else context.hisnDataStore
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
        val searchEngine = stringPreferencesKey("search_engine")
        val blocking = booleanPreferencesKey("blocking_enabled")
        val desktopSite = booleanPreferencesKey("desktop_site_default")
        val allowlist = stringPreferencesKey("allowlist")
        val shortcuts = stringPreferencesKey("shortcuts")
    }

    val settings: Flow<BrowserSettings> = dataStore.data.map { prefs ->
        BrowserSettings(
            theme = prefs[Keys.theme] ?: "system",
            language = prefs[Keys.language] ?: "ar",
            searchEngine = prefs[Keys.searchEngine] ?: "google",
            blockingEnabled = prefs[Keys.blocking] ?: true,
            desktopSiteDefault = prefs[Keys.desktopSite] ?: false,
            allowlist = (prefs[Keys.allowlist] ?: "").split('\n').map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet(),
            shortcuts = decodeShortcuts(prefs[Keys.shortcuts])
        )
    }

    suspend fun setTheme(value: String) = dataStore.edit { it[Keys.theme] = value }
    suspend fun setLanguage(value: String) = dataStore.edit { it[Keys.language] = value }
    suspend fun setSearchEngine(value: String) = dataStore.edit { it[Keys.searchEngine] = value }
    suspend fun setBlockingEnabled(value: Boolean) = dataStore.edit { it[Keys.blocking] = value }
    suspend fun setDesktopSiteDefault(value: Boolean) = dataStore.edit { it[Keys.desktopSite] = value }

    suspend fun setAllowlisted(host: String, allowed: Boolean) {
        val normalized = normalizeHost(host)
        if (normalized.isBlank()) return
        dataStore.edit { prefs ->
            val items = (prefs[Keys.allowlist] ?: "").split('\n').map { it.trim().lowercase() }.filter { it.isNotBlank() }.toMutableSet()
            if (allowed) items.add(normalized) else items.remove(normalized)
            prefs[Keys.allowlist] = items.sorted().joinToString("\n")
        }
    }

    suspend fun addShortcut(shortcut: Shortcut) {
        val cleanUrl = shortcut.url.trim()
        if (cleanUrl.isBlank() || shortcut.title.isBlank()) return
        dataStore.edit { prefs ->
            val current = decodeShortcuts(prefs[Keys.shortcuts]).toMutableList()
            current.removeAll { it.url.equals(cleanUrl, ignoreCase = true) }
            current.add(Shortcut(shortcut.title.trim(), cleanUrl))
            prefs[Keys.shortcuts] = encodeShortcuts(current)
        }
    }

    suspend fun removeShortcut(url: String) {
        dataStore.edit { prefs ->
            prefs[Keys.shortcuts] = encodeShortcuts(decodeShortcuts(prefs[Keys.shortcuts]).filterNot { it.url == url })
        }
    }

    private fun normalizeHost(value: String): String = value.trim().removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore(':').lowercase()

    private fun decodeShortcuts(raw: String?): List<Shortcut> {
        if (raw.isNullOrBlank()) return defaultShortcuts()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val objectValue = array.optJSONObject(index) ?: return@mapNotNull null
                val title = objectValue.optString("title").trim()
                val url = objectValue.optString("url").trim()
                if (title.isNotBlank() && url.startsWith("http")) Shortcut(title, url) else null
            }.ifEmpty { defaultShortcuts() }
        } catch (_: Exception) {
            defaultShortcuts()
        }
    }

    private fun encodeShortcuts(shortcuts: List<Shortcut>): String {
        val array = JSONArray()
        shortcuts.forEach { shortcut ->
            array.put(JSONObject().put("title", shortcut.title).put("url", shortcut.url))
        }
        return array.toString()
    }
}
