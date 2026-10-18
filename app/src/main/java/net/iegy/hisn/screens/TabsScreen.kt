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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.iegy.hisn.R
import net.iegy.hisn.browser.BrowserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(
    session: BrowserSession,
    privateMode: Boolean,
    onBack: () -> Unit,
    onOpenTab: (Long) -> Unit,
    onNewTab: () -> Unit,
    onCloseAll: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("${stringResource(R.string.tabs)} (${session.tabs.size})") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.back)) } },
            actions = {
                IconButton(onClick = onNewTab) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_tab)) }
                IconButton(onClick = onCloseAll) { Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.close_all_tabs)) }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (privateMode) Text(stringResource(R.string.private_notice), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(session.tabs, key = { it.id }) { tab ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenTab(tab.id) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (tab.id == session.selectedTabId) 3.dp else 1.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (tab.url.startsWith("https://")) Icons.Default.Lock else Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tab.title.ifBlank { stringResource(R.string.new_tab) }, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(tab.url.ifBlank { stringResource(R.string.home) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            if (tab.id == session.selectedTabId) Text("●", color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { session.closeTab(tab.id) }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_tab)) }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                Button(onClick = onNewTab, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, contentDescription = null); Text(stringResource(R.string.new_tab)) }
            }
        }
    }
}
