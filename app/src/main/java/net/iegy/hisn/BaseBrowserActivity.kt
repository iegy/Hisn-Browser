package net.iegy.hisn

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import net.iegy.hisn.browser.BrowserSession
import net.iegy.hisn.data.BrowserRepository
import net.iegy.hisn.data.SettingsRepository
import net.iegy.hisn.ui.HisnApp

open class BaseBrowserActivity : ComponentActivity() {
    protected open val privateMode: Boolean = false
    lateinit var browserSession: BrowserSession
        private set
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        val callback = filePathCallback
        filePathCallback = null
        if (callback != null) {
            val uris = if (result.resultCode == RESULT_OK) {
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            } else null
            callback.onReceiveValue(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val repository = BrowserRepository(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext, privateMode = privateMode)
        val restoredUrls = if (!privateMode) savedInstanceState?.getStringArrayList(KEY_TAB_URLS).orEmpty() else emptyList()
        val restoredIndex = if (!privateMode) savedInstanceState?.getInt(KEY_SELECTED_TAB, 0) ?: 0 else 0

        browserSession = BrowserSession(
            activity = this,
            isPrivate = privateMode,
            repository = repository,
            fileChooser = ::showFileChooser,
            initialUrls = restoredUrls,
            initialSelectedIndex = restoredIndex
        )

        setContent {
            HisnApp(
                session = browserSession,
                repository = repository,
                settingsRepository = settingsRepository,
                privateMode = privateMode,
                onExitPrivate = { finish() },
                onClearCache = { browserSession.tabs.forEach { it.webView?.clearCache(true) } }
            )
        }
    }

    private fun showFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams
    ): Boolean {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        return try {
            fileChooserLauncher.launch(params.createIntent())
            true
        } catch (_: Exception) {
            filePathCallback = null
            callback.onReceiveValue(null)
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (!privateMode) {
            outState.putStringArrayList(KEY_TAB_URLS, ArrayList(browserSession.tabs.map { it.url }))
            outState.putInt(KEY_SELECTED_TAB, browserSession.tabs.indexOfFirst { it.id == browserSession.selectedTabId }.coerceAtLeast(0))
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (privateMode && isFinishing) {
            browserSession.clearPrivateBrowserData()
        }
        if (isFinishing) filePathCallback?.onReceiveValue(null)
        browserSession.dispose()
        super.onDestroy()
    }

    companion object {
        private const val KEY_TAB_URLS = "hisn_tab_urls"
        private const val KEY_SELECTED_TAB = "hisn_selected_tab"
    }
}
