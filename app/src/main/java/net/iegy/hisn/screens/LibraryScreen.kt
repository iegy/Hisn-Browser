package net.iegy.hisn.screens

import android.app.DownloadManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.iegy.hisn.R
import net.iegy.hisn.data.BookmarkEntity
import net.iegy.hisn.data.BrowserRepository
import net.iegy.hisn.data.HistoryEntity
import net.iegy.hisn.download.DownloadHelper
import net.iegy.hisn.download.DownloadItem
import net.iegy.hisn.ui.AppPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    page: AppPage,
    repository: BrowserRepository,
    privateMode: Boolean,
    onBack: () -> Unit,
    onOpenUrl: (String, Boolean) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bookmarks by repository.observeBookmarks().collectAsState(initial = emptyList())
    val history by repository.observeHistory().collectAsState(initial = emptyList())
    var downloads by remember { mutableStateOf(emptyList<DownloadItem>()) }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var clearHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(page) {
        if (page == AppPage.DOWNLOADS) {
            while (true) {
                downloads = DownloadHelper.query(context)
                delay(1500)
            }
        }
    }

    val titleRes = when (page) {
        AppPage.BOOKMARKS -> R.string.bookmarks
        AppPage.HISTORY -> R.string.history
        else -> R.string.downloads
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(titleRes)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.back)) } },
            actions = {
                if (page == AppPage.HISTORY && !privateMode) IconButton(onClick = { clearHistoryDialog = true }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_history)) }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (privateMode && page == AppPage.HISTORY) Text(stringResource(R.string.private_notice), style = MaterialTheme.typography.bodySmall)
            when (page) {
                AppPage.BOOKMARKS -> BookmarkList(
                    bookmarks = bookmarks,
                    onOpen = { onOpenUrl(it, false) },
                    onOpenNew = { onOpenUrl(it, true) },
                    onDelete = { id -> scope.launch { repository.removeBookmark(id) } },
                    onEdit = { editingBookmark = it }
                )
                AppPage.HISTORY -> HistoryList(
                    history = if (privateMode) emptyList() else history,
                    onOpen = { onOpenUrl(it, false) },
                    onOpenNew = { onOpenUrl(it, true) },
                    onDelete = { id -> scope.launch { repository.removeHistory(id) } }
                )
                AppPage.DOWNLOADS -> DownloadsList(context, downloads)
                else -> Unit
            }
        }
    }

    editingBookmark?.let { bookmark ->
        var title by remember(bookmark.id) { mutableStateOf(bookmark.title) }
        var url by remember(bookmark.id) { mutableStateOf(bookmark.url) }
        AlertDialog(
            onDismissRequest = { editingBookmark = null },
            title = { Text(stringResource(R.string.edit_shortcut)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.shortcut_title)) }, singleLine = true)
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text(stringResource(R.string.shortcut_url)) }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.editBookmark(bookmark.id, title, url) }
                    editingBookmark = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editingBookmark = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (clearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { clearHistoryDialog = false },
            title = { Text(stringResource(R.string.clear_history)) },
            text = { Text(stringResource(R.string.clear_history_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.clearHistory()
                        onToast(context.getString(R.string.history_cleared))
                    }
                    clearHistoryDialog = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { clearHistoryDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun BookmarkList(
    bookmarks: List<BookmarkEntity>,
    onOpen: (String) -> Unit,
    onOpenNew: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (BookmarkEntity) -> Unit
) {
    if (bookmarks.isEmpty()) {
        EmptyLibrary(Icons.Default.Bookmark, stringResource(R.string.empty_bookmarks))
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(bookmarks, key = { it.id }) { item ->
            LibraryRow(Icons.Default.Bookmark, item.title, item.url, onOpen = { onOpen(item.url) }, onDelete = { onDelete(item.id) }, onEdit = { onEdit(item) }, onOpenNew = { onOpenNew(item.url) })
        }
    }
}

@Composable
private fun HistoryList(
    history: List<HistoryEntity>,
    onOpen: (String) -> Unit,
    onOpenNew: (String) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (history.isEmpty()) {
        EmptyLibrary(Icons.Default.History, stringResource(R.string.empty_history))
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(history, key = { it.id }) { item ->
            LibraryRow(Icons.Default.History, item.title, item.url, onOpen = { onOpen(item.url) }, onDelete = { onDelete(item.id) }, onOpenNew = { onOpenNew(item.url) })
        }
    }
}

@Composable
private fun LibraryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onOpenNew: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title.ifBlank { stringResource(R.string.no_title) }, maxLines = 1, fontWeight = FontWeight.SemiBold)
                Text(subtitle, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onEdit != null) IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_shortcut)) }
            IconButton(onClick = onOpenNew) { Icon(Icons.Default.OpenInNew, contentDescription = stringResource(R.string.open_new_tab)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete)) }
        }
    }
}

@Composable
private fun DownloadsList(context: Context, downloads: List<DownloadItem>) {
    if (downloads.isEmpty()) {
        EmptyLibrary(Icons.Default.Download, stringResource(R.string.empty_downloads))
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(downloads, key = { it.id }) { item ->
            val status = when (item.status) {
                DownloadManager.STATUS_PENDING -> stringResource(R.string.download_status_pending)
                DownloadManager.STATUS_RUNNING -> stringResource(R.string.download_status_running)
                DownloadManager.STATUS_PAUSED -> stringResource(R.string.download_status_paused)
                DownloadManager.STATUS_SUCCESSFUL -> stringResource(R.string.download_status_success)
                DownloadManager.STATUS_FAILED -> stringResource(R.string.download_status_failed)
                else -> stringResource(R.string.download_status_pending)
            }
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, maxLines = 1, fontWeight = FontWeight.SemiBold)
                        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                        IconButton(onClick = { DownloadHelper.openUri(context, item.localUri) }) { Icon(Icons.Default.Public, contentDescription = stringResource(R.string.download_open)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
        Text(text, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
