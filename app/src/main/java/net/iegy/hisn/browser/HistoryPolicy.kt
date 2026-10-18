package net.iegy.hisn.browser

object HistoryPolicy {
    fun shouldRecord(isPrivateSession: Boolean, url: String): Boolean =
        !isPrivateSession && (url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true))
}
