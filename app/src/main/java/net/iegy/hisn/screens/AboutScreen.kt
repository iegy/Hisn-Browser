package net.iegy.hisn.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.iegy.hisn.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, showPrivacy: Boolean = false) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (showPrivacy) stringResource(R.string.privacy_policy) else stringResource(R.string.about)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = if (showPrivacy) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (!showPrivacy) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.padding(top = 18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("حصن | HISN Browser", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.app_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
            }
            Text(stringResource(R.string.settings_credit), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.creator_credit_ar))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.copyright), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.browser_engine), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.limitations), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            if (showPrivacy) {
                Text("Local browser data", fontWeight = FontWeight.Bold)
                Text("Regular bookmarks, browsing history and preferences are stored locally on this device. HISN does not operate a browsing-history server and does not include analytics SDKs. The app does not intentionally upload browsing history, bookmarks or search terms to the developer.")
                Spacer(Modifier.height(12.dp))
                Text("Private browsing", fontWeight = FontWeight.Bold)
                Text("Private browsing uses a separate Android process and WebView data directory. HISN requests deletion of session cookies, website storage and cache when the private activity ends. Android system downloads that you explicitly start remain in the public Downloads folder. No private mode can hide activity from websites you visit, the network owner or your internet provider.")
                Spacer(Modifier.height(12.dp))
                Text("Connections and blocking", fontWeight = FontWeight.Bold)
                Text("HTTPS certificate errors are not bypassed. HTTP pages are supported but are not encrypted. The built-in tracker blocker uses a small local domain list and cannot block every ad or tracker. Websites may still receive the information necessary to deliver pages you request.")
                Spacer(Modifier.height(12.dp))
                Text("Website permissions", fontWeight = FontWeight.Bold)
                Text("Website camera and microphone permission requests are denied by default until a user-mediated Android permission flow is available. Downloads are initiated by explicit browser download callbacks.")
                Spacer(Modifier.height(12.dp))
                Text("Data controls", fontWeight = FontWeight.Bold)
                Text("You can remove individual bookmarks and history entries, clear regular history, and clear cached WebView content in Settings. Clearing cache does not clear cookies or saved website logins.")
                Spacer(Modifier.height(12.dp))
                Text("Contact", fontWeight = FontWeight.Bold)
                Text("https://iegy.net/")
            }
        }
    }
}
