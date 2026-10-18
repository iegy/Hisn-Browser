package net.iegy.hisn.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.iegy.hisn.R
import net.iegy.hisn.data.BrowserSettings
import net.iegy.hisn.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    onOpenPrivacy: () -> Unit,
    onClearHistory: () -> Unit,
    onClearCache: () -> Unit,
    onToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var allowlistDraft by remember { mutableStateOf("") }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = { androidx.compose.material3.IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsSection(icon = Icons.Default.Web, title = stringResource(R.string.theme)) {
                RadioChoice(stringResource(R.string.theme_system), settings.theme == "system") { scope.launch { settingsRepository.setTheme("system") } }
                RadioChoice(stringResource(R.string.theme_light), settings.theme == "light") { scope.launch { settingsRepository.setTheme("light") } }
                RadioChoice(stringResource(R.string.theme_dark), settings.theme == "dark") { scope.launch { settingsRepository.setTheme("dark") } }
            }

            SettingsSection(icon = Icons.Default.Language, title = stringResource(R.string.language)) {
                RadioChoice(stringResource(R.string.arabic), settings.language == "ar") { onLanguageChanged("ar") }
                RadioChoice(stringResource(R.string.english), settings.language == "en") { onLanguageChanged("en") }
            }

            SettingsSection(icon = Icons.Default.Web, title = stringResource(R.string.search_engine)) {
                RadioChoice("Google", settings.searchEngine == "google") { scope.launch { settingsRepository.setSearchEngine("google") } }
                RadioChoice("Bing", settings.searchEngine == "bing") { scope.launch { settingsRepository.setSearchEngine("bing") } }
                RadioChoice("DuckDuckGo", settings.searchEngine == "duckduckgo") { scope.launch { settingsRepository.setSearchEngine("duckduckgo") } }
            }

            SettingsSection(icon = Icons.Default.Block, title = stringResource(R.string.blocking)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.blocking), fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.blocking_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.blockingEnabled, onCheckedChange = { value -> scope.launch { settingsRepository.setBlockingEnabled(value) } })
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                Text(stringResource(R.string.allowlist), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = allowlistDraft,
                        onValueChange = { allowlistDraft = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.allowlist_hint)) },
                        singleLine = true
                    )
                    Button(onClick = {
                        scope.launch {
                            settingsRepository.setAllowlisted(allowlistDraft, true)
                            allowlistDraft = ""
                        }
                    }, enabled = allowlistDraft.isNotBlank()) { Text(stringResource(R.string.add)) }
                }
                settings.allowlist.sorted().forEach { host ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(host, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.material3.IconButton(onClick = { scope.launch { settingsRepository.setAllowlisted(host, false) } }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            SettingsSection(icon = Icons.Default.Web, title = stringResource(R.string.desktop_site)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.desktop_site), fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.desktop_site_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.desktopSiteDefault, onCheckedChange = { value -> scope.launch { settingsRepository.setDesktopSiteDefault(value) } })
                }
            }

            SettingsSection(icon = Icons.Default.Storage, title = stringResource(R.string.history)) {
                SettingsAction(stringResource(R.string.clear_history), stringResource(R.string.clear_history_confirm), Icons.Default.Delete) { showClearHistoryDialog = true }
                SettingsAction(stringResource(R.string.clear_cache), stringResource(R.string.clear_cache_confirm), Icons.Default.Storage) { showClearCacheDialog = true }
            }

            SettingsSection(icon = Icons.Default.Lock, title = stringResource(R.string.private_mode)) {
                Text(stringResource(R.string.private_info), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SettingsSection(icon = Icons.Default.Security, title = stringResource(R.string.about)) {
                Text("حصن | HISN Browser", fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.settings_credit), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.version_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenPrivacy) { Text(stringResource(R.string.privacy_policy)) }
            }
            Text(stringResource(R.string.copyright), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.clear_history)) },
            text = { Text(stringResource(R.string.clear_history_confirm)) },
            confirmButton = { TextButton(onClick = { showClearHistoryDialog = false; onClearHistory() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { showClearHistoryDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache)) },
            text = { Text(stringResource(R.string.clear_cache_confirm)) },
            confirmButton = { TextButton(onClick = { showClearCacheDialog = false; onClearCache() }) { Text(stringResource(R.string.clear_cache)) } },
            dismissButton = { TextButton(onClick = { showClearCacheDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun SettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun RadioChoice(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun SettingsAction(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
