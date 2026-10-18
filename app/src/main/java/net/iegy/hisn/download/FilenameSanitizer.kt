package net.iegy.hisn.download

object FilenameSanitizer {
    fun sanitize(input: String): String {
        val sanitized = input
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "")
            .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .trim().trim('.')
            .take(150)
        val safe = sanitized.ifBlank { "download-${System.currentTimeMillis()}" }
        return if (safe.equals(".", true) || safe.equals("..", true)) "download-${System.currentTimeMillis()}" else safe
    }
}
