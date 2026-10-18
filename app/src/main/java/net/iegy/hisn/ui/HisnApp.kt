package net.iegy.hisn.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import net.iegy.hisn.R
import net.iegy.hisn.PrivateBrowserActivity
import net.iegy.hisn.browser.BrowserSession
import net.iegy.hisn.data.BrowserRepository
import net.iegy.hisn.data.BrowserSettings
import net.iegy.hisn.data.SettingsRepository
import net.iegy.hisn.screens.AboutScreen
import net.iegy.hisn.screens.BrowserScreen
import net.iegy.hisn.screens.LibraryScreen
import net.iegy.hisn.screens.SettingsScreen
import net.iegy.hisn.screens.TabsScreen
import android.content.Intent

 enum class AppPage { BROWSER, TABS, BOOKMARKS, HISTORY, DOWNLOADS, SETTINGS, ABOUT, PRIVACY }

@Composable
fun HisnApp(
    session: BrowserSession,
    repository: BrowserRepository,
    settingsRepository: SettingsRepository,
    privateMode: Boolean,
    onExitPrivate: () -> Unit,
    onClearCache: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(BrowserSettings()) }
    var page by remember { mutableStateOf(AppPage.BROWSER) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(settingsRepository) {
        settingsRepository.settings.collect { settings = it }
    }
    LaunchedEffect(settings.language) {
        val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',')
        if (currentLocale != settings.language) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(settings.language))
        }
    }
    SideEffect { session.updateSettings(settings) }

    val darkTheme = settings.theme == "dark" || (settings.theme == "system" && androidx.compose.foundation.isSystemInDarkTheme())
    HisnTheme(darkTheme = darkTheme) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalLayoutDirection provides if (settings.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            BackHandler(enabled = true) {
                when {
                    page != AppPage.BROWSER -> page = AppPage.BROWSER
                    session.navigateBack() -> Unit
                    privateMode -> onExitPrivate()
                    session.selectedTab?.url?.isNotBlank() == true -> session.returnHome()
                    else -> (context as? android.app.Activity)?.finish()
                }
            }
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (page) {
                        AppPage.BROWSER -> BrowserScreen(
                            session = session,
                            repository = repository,
                            settingsRepository = settingsRepository,
                            settings = settings,
                            privateMode = privateMode,
                            onNavigate = { page = it },
                            onOpenPrivate = {
                                context.startActivity(Intent(context, PrivateBrowserActivity::class.java))
                            },
                            onExitPrivate = onExitPrivate,
                            onToast = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                        )
                        AppPage.TABS -> TabsScreen(
                            session = session,
                            privateMode = privateMode,
                            onBack = { page = AppPage.BROWSER },
                            onOpenTab = { session.selectTab(it); page = AppPage.BROWSER },
                            onNewTab = { session.newTab(); page = AppPage.BROWSER },
                            onCloseAll = { session.closeAllTabs(); page = AppPage.BROWSER }
                        )
                        AppPage.BOOKMARKS, AppPage.HISTORY, AppPage.DOWNLOADS -> LibraryScreen(
                            page = page,
                            repository = repository,
                            privateMode = privateMode,
                            onBack = { page = AppPage.BROWSER },
                            onOpenUrl = { url, newTab -> session.openWebsite(url, newTab); page = AppPage.BROWSER },
                            onToast = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                        )
                        AppPage.SETTINGS -> SettingsScreen(
                            settings = settings,
                            settingsRepository = settingsRepository,
                            onBack = { page = AppPage.BROWSER },
                            onLanguageChanged = { language ->
                                scope.launch { settingsRepository.setLanguage(language) }
                            },
                            onOpenPrivacy = { page = AppPage.PRIVACY },
                            onClearHistory = {
                                scope.launch {
                                    repository.clearHistory()
                                    snackbarHostState.showSnackbar(context.getString(R.string.history_cleared))
                                }
                            },
                            onClearCache = {
                                onClearCache()
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.cache_cleared)) }
                            },
                            onToast = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                        )
                        AppPage.ABOUT -> AboutScreen(onBack = { page = AppPage.BROWSER })
                        AppPage.PRIVACY -> AboutScreen(onBack = { page = AppPage.SETTINGS }, showPrivacy = true)
                    }
                }
            }
        }
    }
}
