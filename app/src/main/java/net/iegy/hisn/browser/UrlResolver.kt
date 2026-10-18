package net.iegy.hisn.browser

import java.net.IDN
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UrlResolver {
    private val hostPattern = Regex("^(?=.{1,253}$)(localhost|(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3})(?::\\d{1,5})?(?:[/?#].*)?$")
    private val blockedSchemes = setOf("javascript", "file", "content", "data", "intent", "blob", "vbscript")

    fun resolve(input: String, searchEngine: String = "google"): String? {
        val value = input.trim()
        if (value.isEmpty()) return null
        val scheme = value.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme in blockedSchemes) return null
        if (value.any { it == '\n' || it == '\r' }) return null

        val candidate = when {
            value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true) -> value
            hostPattern.matches(value) -> "https://$value"
            else -> return searchUrl(value, searchEngine)
        }

        return try {
            val uri = URI(candidate).normalize()
            val host = uri.host ?: return null
            if (host.isBlank() || host.contains(' ')) return null
            val asciiHost = IDN.toASCII(host)
            if (uri.scheme !in setOf("http", "https")) return null
            URI(uri.scheme, uri.userInfo, asciiHost, uri.port, uri.path, uri.query, uri.fragment).toASCIIString()
        } catch (_: Exception) {
            null
        }
    }

    fun searchUrl(query: String, engine: String): String {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20")
        return when (engine.lowercase()) {
            "bing" -> "https://www.bing.com/search?q=$encoded"
            "duckduckgo", "duck duck go" -> "https://duckduckgo.com/?q=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }
}
