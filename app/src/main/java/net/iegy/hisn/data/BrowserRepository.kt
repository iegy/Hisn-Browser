package net.iegy.hisn.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class BrowserRepository(context: Context) {
    private val database = HisnDatabase.get(context)
    private val bookmarks = database.bookmarkDao()
    private val history = database.historyDao()

    fun observeBookmarks(): Flow<List<BookmarkEntity>> = bookmarks.observeAll()
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>> = bookmarks.search(query)
    fun observeHistory(): Flow<List<HistoryEntity>> = history.observeRecent()
    fun searchHistory(query: String): Flow<List<HistoryEntity>> = history.search(query)

    suspend fun saveBookmark(title: String, url: String, folder: String = "General") {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isEmpty()) return
        val existing = bookmarks.findByUrl(normalizedUrl)
        if (existing == null) {
            bookmarks.insert(BookmarkEntity(title = title.ifBlank { normalizedUrl }, url = normalizedUrl, folder = folder))
        } else {
            bookmarks.update(existing.id, title.ifBlank { normalizedUrl }, normalizedUrl, folder)
        }
    }

    suspend fun editBookmark(id: Long, title: String, url: String, folder: String = "General") {
        if (url.isNotBlank()) bookmarks.update(id, title.ifBlank { url }, url.trim(), folder)
    }

    suspend fun removeBookmark(id: Long) = bookmarks.delete(id)
    suspend fun removeBookmarkByUrl(url: String) = bookmarks.deleteByUrl(url)
    suspend fun isBookmarked(url: String): Boolean = bookmarks.exists(url)

    suspend fun addHistory(title: String, url: String) {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) return
        history.insert(HistoryEntity(title = title.ifBlank { cleanUrl }, url = cleanUrl))
    }

    suspend fun removeHistory(id: Long) = history.delete(id)
    suspend fun clearHistory() = history.clear()
    suspend fun clearHistoryOlderThan(cutoff: Long) = history.deleteBefore(cutoff)
}
