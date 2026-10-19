package net.iegy.hisn.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import net.iegy.hisn.R
import net.iegy.hisn.browser.BrowserSession
import net.iegy.hisn.browser.BrowserTab
import net.iegy.hisn.browser.MediaItem
import net.iegy.hisn.data.BrowserRepository
import net.iegy.hisn.data.BrowserSettings
import net.iegy.hisn.data.SettingsRepository
import net.iegy.hisn.data.Shortcut
import net.iegy.hisn.ui.AppPage
import kotlinx.coroutines.launch

@Composable
fun BrowserScreen(
    session: BrowserSession,
    repository: BrowserRepository,
    settingsRepository: SettingsRepository,
    settings: BrowserSettings,
    privateMode: Boolean,
    onNavigate: (AppPage) -> Unit,
    onOpenPrivate: () -> Unit,
    onExitPrivate: () -> Unit,
    onToast: (String) -> Unit
) {
    val tab = session.selectedTab
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboardManager.current
    val history by repository.observeHistory().collectAsState(initial = emptyList())
    val bookmarks by repository.observeBookmarks().collectAsState(initial = emptyList())
    var address by remember(tab?.id) { mutableStateOf(tab?.url.orEmpty()) }
    var editingAddress by remember(tab?.id) { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkTitle by remember { mutableStateOf("") }
    var bookmarkUrl by remember { mutableStateOf("") }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var shortcutTitle by remember { mutableStateOf("") }
    var shortcutUrl by remember { mutableStateOf("") }
    var showMediaDialog by remember { mutableStateOf(false) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    LaunchedEffect(tab?.id, tab?.url) {
        if (!editingAddress) address = tab?.url.orEmpty()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_hisn_logo),
                    contentDescription = "HISN",
                    tint = Color.Unspecified,
                    modifier = Modifier.padding(4.dp)
                )
            }
            OutlinedTextField(
                value = address,
                onValueChange = { address = it; editingAddress = true },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.address_hint), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = if (tab?.url?.startsWith("https://", true) == true) Icons.Default.Lock else Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (tab?.url?.startsWith("https://", true) == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (address.isNotBlank()) {
                        IconButton(onClick = { address = ""; editingAddress = true }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    session.openInput(address)
                    editingAddress = false
                    keyboard?.hide()
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                )
            )
            IconButton(onClick = {
                val current = session.selectedTab
                if (current?.url?.isNotBlank() == true) {
                    bookmarkTitle = current.title
                    bookmarkUrl = current.url
                    showBookmarkDialog = true
                } else {
                    onToast(context.getString(R.string.invalid_address))
                }
            }) { Icon(Icons.Default.BookmarkAdd, contentDescription = stringResource(R.string.add_bookmark)) }
            IconButton(onClick = {
                session.openInput(address)
                editingAddress = false
                keyboard?.hide()
            }) { Icon(Icons.Default.Public, contentDescription = stringResource(R.string.go)) }
        }

        if (tab != null && tab.progress in 1..99) {
            LinearProgressIndicator(progress = { tab.progress / 100f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary)
        }
        if (tab?.url?.startsWith("http://", true) == true) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.insecure_connection),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (settings.blockingEnabled && (tab?.blockedCount ?: 0) > 0) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.blocked_count, tab?.blockedCount ?: 0),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        val detectedMedia = tab?.media?.size ?: 0
        if (detectedMedia > 0) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth().clickable {
                    session.scanMedia { items -> mediaItems = items; showMediaDialog = true }
                }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(stringResource(R.string.video_detected, detectedMedia), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (privateMode) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(stringResource(R.string.private_notice), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onExitPrivate, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.private_exit), modifier = Modifier.size(17.dp))
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (tab == null || tab.url.isBlank()) {
                HomePanel(
                    settings = settings,
                    history = if (privateMode) emptyList() else history.take(5),
                    onOpen = { session.openWebsite(it) },
                    onNavigate = onNavigate,
                    onAddShortcut = { shortcutTitle = ""; shortcutUrl = ""; showShortcutDialog = true },
                    onRemoveShortcut = { url -> scope.launch { settingsRepository.removeShortcut(url) } },
                    onRemoveHistory = { id -> scope.launch { repository.removeHistory(id) } },
                    privateMode = privateMode
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { FrameLayout(it) },
                    update = { container ->
                        val webView = tab.webView
                        if (webView != null && webView.parent !== container) {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            container.removeAllViews()
                            container.addView(webView, FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            ))
                            webView.onResume()
                        }
                    }
                )
                if (tab.error) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(10.dp))
                            Text(stringResource(R.string.page_error), fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { tab.error = false; tab.webView?.reload() }) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
            }
        }

        if (session.message.isNotBlank()) {
            val visibleMessage = when {
                session.message.contains("Invalid address", ignoreCase = true) || session.message.contains("unsupported URL", ignoreCase = true) -> stringResource(R.string.invalid_address)
                session.message.contains("could not be opened", ignoreCase = true) -> stringResource(R.string.external_link_failed)
                session.message.contains("Download queued", ignoreCase = true) -> stringResource(R.string.downloaded)
                session.message.contains("Download could not", ignoreCase = true) -> stringResource(R.string.download_failed)
                session.message.contains("Maximum of", ignoreCase = true) -> stringResource(R.string.close_all_tabs)
                session.message.contains("Only HTTP", ignoreCase = true) -> stringResource(R.string.invalid_address)
                session.message.contains("Popup blocked", ignoreCase = true) -> stringResource(R.string.popup_blocked)
                session.message.contains("Redirect blocked", ignoreCase = true) -> stringResource(R.string.redirect_blocked)
                session.message.contains("Video download started", ignoreCase = true) -> stringResource(R.string.video_started)
                session.message.startsWith("Downloading video") ->
                    stringResource(R.string.video_progress, session.message.filter { it.isDigit() })
                session.message.startsWith("Video saved") -> stringResource(R.string.video_saved)
                session.message.startsWith("Video unsupported") -> stringResource(R.string.video_unsupported)
                session.message.contains("Video download failed", ignoreCase = true) -> stringResource(R.string.video_failed)
                else -> session.message
            }
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth().clickable { session.clearMessage() }) {
                Text(visibleMessage, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { session.navigateBack() }, enabled = tab?.canGoBack == true) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                IconButton(onClick = { session.navigateForward() }, enabled = tab?.canGoForward == true) {
                    Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.forward))
                }
                IconButton(onClick = { session.returnHome(); address = ""; editingAddress = false }) {
                    Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home))
                }
                IconButton(onClick = { onNavigate(AppPage.TABS) }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tab, contentDescription = stringResource(R.string.tabs))
                        Text(session.tabs.size.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        MenuItem(R.string.new_tab, Icons.Default.Add) { session.newTab(); menuExpanded = false }
                        MenuItem(R.string.bookmarks, Icons.Default.Star) { onNavigate(AppPage.BOOKMARKS); menuExpanded = false }
                        MenuItem(R.string.history, Icons.Default.History) { onNavigate(AppPage.HISTORY); menuExpanded = false }
                        MenuItem(R.string.downloads, Icons.Default.Download) { onNavigate(AppPage.DOWNLOADS); menuExpanded = false }
                        MenuItem(R.string.download_video, Icons.Default.Download) {
                            session.scanMedia { items -> mediaItems = items; showMediaDialog = true }
                            menuExpanded = false
                        }
                        MenuItem(R.string.copy_url, Icons.Default.ContentCopy) {
                            session.selectedTab?.url?.takeIf { it.isNotBlank() }?.let { clipboard.setText(AnnotatedString(it)); onToast(it) }
                            menuExpanded = false
                        }
                        MenuItem(R.string.share_url, Icons.Default.Share) {
                            session.selectedTab?.url?.takeIf { it.isNotBlank() }?.let { url ->
                                val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_url)))
                            }
                            menuExpanded = false
                        }
                        if (session.selectedTab?.url?.isNotBlank() == true) {
                            MenuItem(R.string.allowlist_add, Icons.Default.Security) {
                                val host = Uri.parse(session.selectedTab?.url).host
                                if (!host.isNullOrBlank()) {
                                    scope.launch {
                                        settingsRepository.setAllowlisted(host, true)
                                        onToast(context.getString(R.string.site_allowlisted))
                                    }
                                }
                                menuExpanded = false
                            }
                        }
                        MenuItem(R.string.settings, Icons.Default.Settings) { onNavigate(AppPage.SETTINGS); menuExpanded = false }
                        MenuItem(R.string.about, Icons.Default.Security) { onNavigate(AppPage.ABOUT); menuExpanded = false }
                        if (privateMode) {
                            MenuItem(R.string.private_exit, Icons.Default.Close) { onExitPrivate(); menuExpanded = false }
                        } else {
                            MenuItem(R.string.private_mode, Icons.Default.Lock) { onOpenPrivate(); menuExpanded = false }
                        }
                    }
                }
                IconButton(onClick = { session.reloadOrStop() }, enabled = tab?.url?.isNotBlank() == true) {
                    Icon(if (tab?.loading == true) Icons.Default.Stop else Icons.Default.Refresh,
                        contentDescription = stringResource(if (tab?.loading == true) R.string.stop_loading else R.string.reload))
                }
            }
        }
    }

    if (showMediaDialog) {
        AlertDialog(
            onDismissRequest = { showMediaDialog = false },
            title = { Text(stringResource(R.string.download_video)) },
            text = {
                if (mediaItems.isEmpty()) {
                    Text(stringResource(R.string.no_video_found))
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        mediaItems.forEach { item ->
                            TextButton(
                                onClick = { session.downloadMedia(item); showMediaDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(item.label(), maxLines = 2) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMediaDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = { Text(stringResource(R.string.add_bookmark)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = bookmarkTitle, onValueChange = { bookmarkTitle = it }, label = { Text(stringResource(R.string.shortcut_title)) }, singleLine = true)
                    OutlinedTextField(value = bookmarkUrl, onValueChange = { bookmarkUrl = it }, label = { Text(stringResource(R.string.shortcut_url)) }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.saveBookmark(bookmarkTitle, bookmarkUrl)
                        onToast(context.getString(R.string.bookmark_saved))
                    }
                    showBookmarkDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showBookmarkDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showShortcutDialog) {
        AlertDialog(
            onDismissRequest = { showShortcutDialog = false },
            title = { Text(stringResource(R.string.add_shortcut)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = shortcutTitle, onValueChange = { shortcutTitle = it }, label = { Text(stringResource(R.string.shortcut_title)) }, singleLine = true)
                    OutlinedTextField(value = shortcutUrl, onValueChange = { shortcutUrl = it }, label = { Text(stringResource(R.string.shortcut_url)) }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val normalized = if (shortcutUrl.startsWith("http://") || shortcutUrl.startsWith("https://")) shortcutUrl else "https://$shortcutUrl"
                    scope.launch { settingsRepository.addShortcut(Shortcut(shortcutTitle, normalized)) }
                    showShortcutDialog = false
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = { TextButton(onClick = { showShortcutDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun MenuItem(label: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick
    )
}

@Composable
private fun HomePanel(
    settings: BrowserSettings,
    history: List<net.iegy.hisn.data.HistoryEntity>,
    onOpen: (String) -> Unit,
    onNavigate: (AppPage) -> Unit,
    onAddShortcut: () -> Unit,
    onRemoveShortcut: (String) -> Unit,
    onRemoveHistory: (Long) -> Unit,
    privateMode: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(painter = painterResource(R.drawable.ic_hisn_logo), contentDescription = "HISN", tint = Color.Unspecified, modifier = Modifier.size(74.dp))
        Spacer(Modifier.height(10.dp))
        Text("حصن  |  HISN", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.shortcuts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onAddShortcut) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.add_shortcut)) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            settings.shortcuts.forEach { shortcut ->
                Card(
                    modifier = Modifier.width(116.dp).height(106.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                ) {
                    Column(Modifier.fillMaxSize().padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(Modifier.size(40.dp).clip(CircleShape).clickable { onOpen(shortcut.url) }, contentAlignment = Alignment.Center) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(shortcut.title, maxLines = 1, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                        TextButton(onClick = { onRemoveShortcut(shortcut.url) }, modifier = Modifier.height(24.dp)) { Text("×", fontSize = 16.sp) }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.recent_sites), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { onNavigate(AppPage.HISTORY) }) { Text(stringResource(R.string.history)) }
        }
        if (history.isEmpty()) {
            Text(stringResource(R.string.empty_history), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            history.distinctBy { it.url }.take(5).forEach { entry ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpen(entry.url) }.padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, maxLines = 1, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(entry.url, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onRemoveHistory(entry.id) }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(16.dp)) }
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.settings_credit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (privateMode) Text(stringResource(R.string.privacy_short), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
