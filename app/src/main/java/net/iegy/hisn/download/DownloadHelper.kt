package net.iegy.hisn.download

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import java.util.Locale

 data class DownloadItem(
    val id: Long,
    val title: String,
    val status: Int,
    val reason: Int,
    val localUri: String?
)

object DownloadHelper {
    fun enqueue(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ): Long? {
        if (!url.startsWith("https://", true) && !url.startsWith("http://", true)) return null
        return try {
            val safeName = sanitizeFilename(URLUtil.guessFileName(url, contentDisposition, mimeType))
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(safeName)
                .setDescription("HISN Browser download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
            if (!mimeType.isNullOrBlank()) request.setMimeType(mimeType)
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }?.let {
                request.addRequestHeader("Cookie", it)
            }
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        } catch (_: Exception) {
            null
        }
    }

    fun query(context: Context): List<DownloadItem> {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor: Cursor = manager.query(DownloadManager.Query().setFilterByStatus(
            DownloadManager.STATUS_PENDING or DownloadManager.STATUS_RUNNING or
                DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_SUCCESSFUL or DownloadManager.STATUS_FAILED
        )) ?: return emptyList()
        cursor.use { c ->
            val idCol = c.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleCol = c.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val statusCol = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val reasonCol = c.getColumnIndex(DownloadManager.COLUMN_REASON)
            val uriCol = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val items = mutableListOf<DownloadItem>()
            while (c.moveToNext()) {
                items += DownloadItem(
                    id = c.getLong(idCol),
                    title = c.getString(titleCol).orEmpty(),
                    status = c.getInt(statusCol),
                    reason = c.getInt(reasonCol),
                    localUri = if (uriCol >= 0) c.getString(uriCol) else null
                )
            }
            return items.reversed()
        }
    }

    fun sanitizeFilename(input: String): String = FilenameSanitizer.sanitize(input)

    fun statusLabel(status: Int): String = when (status) {
        DownloadManager.STATUS_PENDING -> "pending"
        DownloadManager.STATUS_RUNNING -> "running"
        DownloadManager.STATUS_PAUSED -> "paused"
        DownloadManager.STATUS_SUCCESSFUL -> "success"
        DownloadManager.STATUS_FAILED -> "failed"
        else -> "pending"
    }

    fun openUri(context: Context, rawUri: String?, mimeType: String? = null) {
        if (rawUri.isNullOrBlank()) return
        try {
            val uri = Uri.parse(rawUri)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType ?: "*/*")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Open download"))
        } catch (_: Exception) { }
    }

    fun filenameFromUrl(url: String): String = URLUtil.guessFileName(url, null, null).lowercase(Locale.ROOT)
}
