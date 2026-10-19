package net.iegy.hisn.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Downloads a plain (VOD) HLS stream by fetching its segments and joining them into one file.
 * Supports master playlists (picks the highest bandwidth) and AES-128 encrypted segments whose
 * key is published in the playlist. DRM-protected streams (SAMPLE-AES etc.) and live streams
 * are refused; no protection is bypassed.
 */
object HlsDownloader {
    class UnsupportedStreamException(message: String) : Exception(message)

    private class Key(val uri: String, val iv: ByteArray?)
    private class Seg(val url: String, val key: Key?, val seq: Long)

    /** Returns the output extension ("ts" or "mp4"). */
    fun download(
        playlistUrl: String,
        referer: String?,
        userAgent: String?,
        out: File,
        onProgress: (Int) -> Unit
    ): String {
        var url = playlistUrl
        var text = fetchText(url, referer, userAgent)
        var depth = 0
        while (text.contains("#EXT-X-STREAM-INF") && depth < 3) {
            url = pickBestVariant(text, url) ?: throw UnsupportedStreamException("No playable stream found")
            text = fetchText(url, referer, userAgent)
            depth++
        }
        if (!text.contains("#EXT-X-ENDLIST")) {
            throw UnsupportedStreamException("Live streams are not supported")
        }

        var seq = 0L
        var key: Key? = null
        var mapUrl: String? = null
        val segments = ArrayList<Seg>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                seq = line.substringAfter(':').trim().toLongOrNull() ?: 0L
            } else if (line.startsWith("#EXT-X-KEY:")) {
                val attrs = parseAttrs(line.substringAfter(':'))
                key = when (attrs["METHOD"] ?: "NONE") {
                    "NONE" -> null
                    "AES-128" -> {
                        val keyUri = attrs["URI"] ?: throw UnsupportedStreamException("Missing key")
                        Key(resolve(url, keyUri), attrs["IV"]?.let { hexToBytes(it) })
                    }
                    else -> throw UnsupportedStreamException("Protected streams are not supported")
                }
            } else if (line.startsWith("#EXT-X-MAP:")) {
                val attrs = parseAttrs(line.substringAfter(':'))
                mapUrl = attrs["URI"]?.let { resolve(url, it) }
            } else if (!line.startsWith("#")) {
                segments.add(Seg(resolve(url, line), key, seq))
                seq++
            }
        }
        if (segments.isEmpty()) throw UnsupportedStreamException("No video segments found")

        val initUrl = mapUrl
        val total = segments.size + (if (initUrl != null) 1 else 0)
        var done = 0
        val keyCache = HashMap<String, ByteArray>()
        FileOutputStream(out).use { os ->
            if (initUrl != null) {
                os.write(fetchBytes(initUrl, referer, userAgent))
                done++
                onProgress(done * 100 / total)
            }
            for (segment in segments) {
                var data = fetchBytes(segment.url, referer, userAgent)
                val k = segment.key
                if (k != null) {
                    val keyBytes = keyCache.getOrPut(k.uri) { fetchBytes(k.uri, referer, userAgent) }
                    val iv = k.iv ?: ByteArray(16).also { buf ->
                        var v = segment.seq
                        for (i in 15 downTo 8) {
                            buf[i] = (v and 0xff).toByte()
                            v = v shr 8
                        }
                    }
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
                    data = cipher.doFinal(data)
                }
                os.write(data)
                done++
                onProgress(done * 100 / total)
            }
        }
        return if (initUrl != null) "mp4" else "ts"
    }

    /** Moves the finished file to the public Downloads folder. Returns a display path or null. */
    fun publish(context: Context, file: File, displayName: String, mime: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                }
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
                file.delete()
                "Downloads/$displayName"
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
                val dest = File(dir, displayName)
                file.copyTo(dest, overwrite = true)
                file.delete()
                dest.absolutePath
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun pickBestVariant(text: String, base: String): String? {
        var bestBandwidth = -1L
        var best: String? = null
        var pendingBandwidth = 0L
        var expectUri = false
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                pendingBandwidth = parseAttrs(line.substringAfter(':'))["BANDWIDTH"]?.toLongOrNull() ?: 0L
                expectUri = true
            } else if (expectUri && line.isNotEmpty() && !line.startsWith("#")) {
                if (pendingBandwidth > bestBandwidth) {
                    bestBandwidth = pendingBandwidth
                    best = resolve(base, line)
                }
                expectUri = false
            }
        }
        return best
    }

    private fun parseAttrs(source: String): Map<String, String> {
        val map = HashMap<String, String>()
        val regex = Regex("([A-Z0-9-]+)=(\"[^\"]*\"|[^,]*)")
        for (match in regex.findAll(source)) {
            map[match.groupValues[1]] = match.groupValues[2].trim('"')
        }
        return map
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.removePrefix("0x").removePrefix("0X").padStart(32, '0')
        return ByteArray(clean.length / 2) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }

    private fun resolve(base: String, ref: String): String = URL(URL(base), ref).toString()

    private fun fetchText(url: String, referer: String?, userAgent: String?): String =
        String(fetchBytes(url, referer, userAgent), Charsets.UTF_8)

    private fun fetchBytes(url: String, referer: String?, userAgent: String?): ByteArray {
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
            throw IOException("Unsupported URL")
        }
        var lastError: Exception? = null
        for (attempt in 0 until 3) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    connection.instanceFollowRedirects = true
                    if (!userAgent.isNullOrBlank()) connection.setRequestProperty("User-Agent", userAgent)
                    if (!referer.isNullOrBlank()) connection.setRequestProperty("Referer", referer)
                    CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }?.let {
                        connection.setRequestProperty("Cookie", it)
                    }
                    if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
                    return connection.inputStream.use { it.readBytes() }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Download failed")
    }
}
