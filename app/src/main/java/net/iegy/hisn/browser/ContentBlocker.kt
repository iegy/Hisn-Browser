package net.iegy.hisn.browser

/**
 * Domain matcher. Looks the host up by walking its parent domains
 * (a.b.example.com -> b.example.com -> example.com), so lookups stay O(labels)
 * even with a list of 70k+ domains.
 */
class ContentBlocker(private val blockedDomains: Set<String>) {
    fun shouldBlock(host: String?, allowlist: Set<String>): Boolean {
        val normalized = host?.trim()?.trimEnd('.')?.lowercase().orEmpty()
        if (normalized.isBlank() || matches(normalized, allowlist)) return false
        return matches(normalized, blockedDomains)
    }

    private fun matches(host: String, rules: Set<String>): Boolean {
        if (rules.isEmpty()) return false
        var current = host
        while (true) {
            if (current in rules) return true
            val dot = current.indexOf('.')
            if (dot < 0) return false
            current = current.substring(dot + 1)
        }
    }

    companion object {
        fun fromLines(lines: Sequence<String>): ContentBlocker = ContentBlocker(
            lines.map { it.substringBefore('#').trim().lowercase().trim('.') }
                .filter { it.isNotBlank() && !it.contains('/') && !it.contains(' ') }
                .toHashSet()
        )

        val DEFAULT = fromLines(
            sequenceOf(
                "doubleclick.net", "googlesyndication.com", "adservice.google.com",
                "adservice.google.com.eg", "googletagservices.com", "adnxs.com",
                "adsrvr.org", "scorecardresearch.com", "quantserve.com", "outbrain.com",
                "taboola.com", "criteo.com", "connect.facebook.net", "facebook.net",
                "analytics.google.com", "google-analytics.com", "hotjar.com",
                "mixpanel.com", "segment.io", "segment.com"
            )
        )
    }
}
