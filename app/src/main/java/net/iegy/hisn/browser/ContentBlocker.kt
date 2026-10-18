package net.iegy.hisn.browser

/** Conservative domain matcher. It intentionally supports host rules only, not cosmetic filtering. */
class ContentBlocker(private val blockedDomains: Set<String>) {
    fun shouldBlock(host: String?, allowlist: Set<String>): Boolean {
        val normalized = host?.trim()?.trimEnd('.')?.lowercase().orEmpty()
        if (normalized.isBlank() || isAllowlisted(normalized, allowlist)) return false
        return blockedDomains.any { rule -> normalized == rule || normalized.endsWith(".$rule") }
    }

    private fun isAllowlisted(host: String, allowlist: Set<String>): Boolean =
        allowlist.any { rule -> host == rule || host.endsWith(".$rule") }

    companion object {
        fun fromLines(lines: Sequence<String>): ContentBlocker = ContentBlocker(
            lines.map { it.substringBefore('#').trim().lowercase().trim('.') }
                .filter { it.isNotBlank() && !it.contains('/') && !it.contains(' ') }
                .toSet()
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
