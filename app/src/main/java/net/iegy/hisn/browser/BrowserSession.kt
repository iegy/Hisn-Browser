package net.iegy.hisn.browser

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.iegy.hisn.data.BrowserRepository
import net.iegy.hisn.data.BrowserSettings
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import net.iegy.hisn.download.DownloadHelper
import net.iegy.hisn.download.FilenameSanitizer
import net.iegy.hisn.download.HlsDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.io.File

class BrowserTab(
    val id: Long,
    val isPrivate: Boolean,
    initialUrl: String = ""
) {
    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf("New tab")
    var progress by mutableIntStateOf(0)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var error by mutableStateOf(false)
    var blockedCount by mutableIntStateOf(0)
    var popupsBlocked by mutableIntStateOf(0)
    var fromPopup: Boolean = false
    val media = mutableStateListOf<MediaItem>()
    var webView: WebView? = null
}

/** A downloadable media URL detected on a page. kind is "video", "audio" or "hls". */
data class MediaItem(val url: String, val kind: String, val referer: String) {
    fun label(): String {
        val name = try { Uri.parse(url).lastPathSegment } catch (_: Exception) { null } ?: url
        return kind.uppercase() + " · " + name.take(60)
    }
}

class BrowserSession(
    private val activity: Activity,
    val isPrivate: Boolean,
    private val repository: BrowserRepository,
    private val fileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean,
    initialUrls: List<String> = emptyList(),
    initialSelectedIndex: Int = 0
) {
    val tabs = mutableStateListOf<BrowserTab>()
    var selectedTabId by mutableLongStateOf(-1L)
        private set
    var message by mutableStateOf("")
        private set

    @Volatile
    private var settingsSnapshot: BrowserSettings = BrowserSettings()
    private val blocker = try {
        activity.assets.open("filters/blocked_domains.txt").bufferedReader().use { reader ->
            ContentBlocker.fromLines(reader.lineSequence())
        }
    } catch (_: Exception) {
        ContentBlocker.DEFAULT
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var nextId = 1L
    private var disposed = false

    init {
        if (initialUrls.isNotEmpty()) {
            initialUrls.take(8).forEach { url ->
                val tab = createTabInternal(url)
                if (url.isNotBlank()) ensureWebView(tab).loadUrl(url)
            }
            tabs.getOrNull(initialSelectedIndex.coerceIn(0, tabs.lastIndex))?.let { selectedTabId = it.id }
        } else {
            newTab()
        }
    }

    val selectedTab: BrowserTab?
        get() = tabs.firstOrNull { it.id == selectedTabId } ?: tabs.firstOrNull()

    fun updateSettings(settings: BrowserSettings) {
        settingsSnapshot = settings
        tabs.forEach { tab ->
            tab.webView?.settings?.userAgentString = if (settings.desktopSiteDefault) DESKTOP_USER_AGENT else null
            tab.webView?.settings?.useWideViewPort = settings.desktopSiteDefault
            tab.webView?.settings?.loadWithOverviewMode = settings.desktopSiteDefault
        }
    }

    fun newTab(url: String = ""): BrowserTab {
        if (tabs.size >= MAX_TABS) {
            message = "Maximum of $MAX_TABS open tabs reached"
            return selectedTab ?: createTabInternal("")
        }
        val previous = selectedTab
        previous?.webView?.onPause()
        val tab = createTabInternal("")
        if (url.isNotBlank()) navigateResolved(url, tab)
        return tab
    }

    fun openInput(input: String, inNewTab: Boolean = false) {
        val resolved = UrlResolver.resolve(input, settingsSnapshot.searchEngine)
        if (resolved == null) {
            message = "Invalid address or unsupported URL scheme"
            return
        }
        val target = if (inNewTab) newTab() else selectedTab ?: newTab()
        navigateResolved(resolved, target)
    }

    fun openWebsite(url: String, inNewTab: Boolean = false) {
        val resolved = UrlResolver.resolve(url, settingsSnapshot.searchEngine) ?: run {
            message = "This address could not be opened"
            return
        }
        val target = if (inNewTab) newTab() else selectedTab ?: newTab()
        navigateResolved(resolved, target)
    }

    fun clearMessage() { message = "" }

    private fun navigateResolved(url: String, tab: BrowserTab) {
        if (!url.startsWith("https://", true) && !url.startsWith("http://", true)) {
            message = "Only HTTP and HTTPS website navigation is supported in the address field"
            return
        }
        tab.error = false
        tab.url = url
        tab.title = hostLabel(url)
        tab.progress = 0
        tab.loading = true
        ensureWebView(tab).loadUrl(url)
        selectedTabId = tab.id
        message = ""
    }

    fun selectTab(id: Long) {
        if (id == selectedTabId) return
        selectedTab?.webView?.onPause()
        selectedTabId = id
        selectedTab?.webView?.onResume()
    }

    fun closeTab(id: Long) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val closingSelected = selectedTabId == id
        destroyTab(tabs[index])
        tabs.removeAt(index)
        if (tabs.isEmpty()) {
            val new = createTabInternal("")
            selectedTabId = new.id
        } else if (closingSelected) {
            selectedTabId = tabs[(index - 1).coerceAtLeast(0)].id
            selectedTab?.webView?.onResume()
        }
    }

    fun closeAllTabs() {
        tabs.toList().forEach(::destroyTab)
        tabs.clear()
        val homeTab = createTabInternal("")
        selectedTabId = homeTab.id
    }

    fun navigateBack(): Boolean {
        val view = selectedTab?.webView ?: return false
        return if (view.canGoBack()) {
            view.goBack()
            true
        } else false
    }

    fun navigateForward() {
        selectedTab?.webView?.takeIf { it.canGoForward() }?.goForward()
    }

    fun reloadOrStop() {
        val tab = selectedTab ?: return
        val view = tab.webView ?: return
        if (tab.loading) view.stopLoading() else view.reload()
        tab.loading = false
    }

    fun returnHome() {
        val tab = selectedTab ?: return
        tab.webView?.onPause()
        tab.webView?.stopLoading()
        tab.url = ""
        tab.title = "New tab"
        tab.error = false
        tab.progress = 0
        tab.loading = false
    }

    fun allowCurrentSite() {
        val tab = selectedTab ?: return
        val host = Uri.parse(tab.url).host ?: return
        sessionScope.launch {
            // Persist allowlist through the settings repository in the UI layer.
            mainHandler.post { message = "Add $host to the allowlist in Settings" }
        }
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        tabs.toList().forEach(::destroyTab)
        tabs.clear()
        sessionScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    fun clearPrivateBrowserData(onComplete: (() -> Unit)? = null) {
        tabs.forEach { it.webView?.clearCache(true) }
        try {
            CookieManager.getInstance().removeAllCookies {
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
                onComplete?.invoke()
            }
        } catch (_: Exception) {
            onComplete?.invoke()
        }
    }

    private fun isAllowlisted(url: String?): Boolean {
        val host = try { Uri.parse(url.orEmpty()).host } catch (_: Exception) { null }?.lowercase() ?: return false
        return settingsSnapshot.allowlist.any { host == it || host.endsWith(".$it") }
    }

    private fun baseDomain(host: String): String {
        val parts = host.lowercase().split('.')
        return if (parts.size <= 2) host.lowercase() else parts.takeLast(2).joinToString(".")
    }

    private fun isGenuineLinkTap(view: WebView): Boolean {
        val type = view.hitTestResult.type
        return type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
            type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
    }

    private fun mediaKindFromPath(path: String): String? = when {
        path.endsWith(".m3u8") -> "hls"
        path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mkv") ||
            path.endsWith(".mov") || path.endsWith(".m4v") || path.endsWith(".3gp") -> "video"
        path.endsWith(".mp3") || path.endsWith(".m4a") || path.endsWith(".aac") ||
            path.endsWith(".ogg") || path.endsWith(".wav") -> "audio"
        else -> null
    }

    private fun sniffMedia(tab: BrowserTab, request: WebResourceRequest) {
        val url = request.url.toString()
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        val kind = mediaKindFromPath(request.url.path?.lowercase().orEmpty()) ?: return
        mainHandler.post {
            if (tab.media.size < 30 && tab.media.none { it.url == url }) {
                tab.media.add(MediaItem(url, kind, tab.url))
            }
        }
    }

    /** Merges sniffed network URLs with <video>/<audio> sources currently in the page. */
    fun scanMedia(onResult: (List<MediaItem>) -> Unit) {
        val tab = selectedTab
        if (tab == null) {
            onResult(emptyList())
            return
        }
        val known = tab.media.toList()
        val view = tab.webView
        if (view == null) {
            onResult(known)
            return
        }
        view.evaluateJavascript(MEDIA_SCAN_JS) { raw ->
            val found = parseMediaScan(raw, tab.url)
            onResult((known + found).distinctBy { it.url })
        }
    }

    private fun parseMediaScan(raw: String?, referer: String): List<MediaItem> {
        if (raw.isNullOrBlank() || raw == "null") return emptyList()
        return try {
            val text = JSONTokener(raw).nextValue() as? String ?: return emptyList()
            val array = JSONArray(text)
            val items = ArrayList<MediaItem>()
            for (i in 0 until array.length()) {
                val u = array.optString(i)
                if (u.startsWith("http://") || u.startsWith("https://")) {
                    val kind = mediaKindFromPath(Uri.parse(u).path?.lowercase().orEmpty()) ?: "video"
                    items.add(MediaItem(u, kind, referer))
                }
            }
            items
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun downloadMedia(item: MediaItem) {
        val userAgent = selectedTab?.webView?.settings?.userAgentString
        if (item.kind == "hls") {
            startHlsDownload(item, userAgent)
            return
        }
        val path = (Uri.parse(item.url).path ?: "").lowercase()
        val mime = when {
            path.endsWith(".webm") -> "video/webm"
            path.endsWith(".mkv") -> "video/x-matroska"
            path.endsWith(".mp3") -> "audio/mpeg"
            path.endsWith(".m4a") || path.endsWith(".aac") -> "audio/mp4"
            path.endsWith(".ogg") -> "audio/ogg"
            path.endsWith(".wav") -> "audio/wav"
            item.kind == "audio" -> "audio/mpeg"
            else -> "video/mp4"
        }
        val id = DownloadHelper.enqueue(activity, item.url, userAgent, null, mime, item.referer)
        message = if (id != null) "Download queued" else "Download could not be started"
    }

    private fun startHlsDownload(item: MediaItem, userAgent: String?) {
        val title = selectedTab?.title.orEmpty()
        message = "Video download started"
        sessionScope.launch {
            val cache = File(activity.cacheDir, "hls_" + System.currentTimeMillis() + ".part")
            try {
                var lastBucket = -1
                val ext = HlsDownloader.download(item.url, item.referer, userAgent, cache) { pct ->
                    val bucket = pct / 25
                    if (bucket != lastBucket) {
                        lastBucket = bucket
                        mainHandler.post { message = "Downloading video $pct%" }
                    }
                }
                val name = FilenameSanitizer.sanitize(title.ifBlank { "video" } + "." + ext)
                val mime = if (ext == "mp4") "video/mp4" else "video/mp2t"
                val saved = HlsDownloader.publish(activity, cache, name, mime)
                mainHandler.post { message = if (saved != null) "Video saved: $saved" else "Video download failed" }
            } catch (e: HlsDownloader.UnsupportedStreamException) {
                cache.delete()
                mainHandler.post { message = "Video unsupported: " + (e.message ?: "") }
            } catch (_: Exception) {
                cache.delete()
                mainHandler.post { message = "Video download failed" }
            }
        }
    }

    private fun buildProtectionScript(): String {
        val allow = JSONArray(settingsSnapshot.allowlist.toList()).toString()
        return PROTECTION_JS.replace("__ALLOW__", allow)
    }

    private fun createTabInternal(url: String): BrowserTab {
        val tab = BrowserTab(id = nextId++, isPrivate = isPrivate, initialUrl = url)
        tabs.add(tab)
        selectedTabId = tab.id
        return tab
    }

    private fun ensureWebView(tab: BrowserTab): WebView {
        tab.webView?.let { return it }
        val webView = WebView(activity)
        tab.webView = webView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(true)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = settingsSnapshot.desktopSiteDefault
            loadWithOverviewMode = settingsSnapshot.desktopSiteDefault
            if (settingsSnapshot.desktopSiteDefault) userAgentString = DESKTOP_USER_AGENT
        }
        if (settingsSnapshot.blockingEnabled &&
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        ) {
            try {
                WebViewCompat.addDocumentStartJavaScript(webView, buildProtectionScript(), setOf("*"))
            } catch (_: Exception) { }
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            val id = DownloadHelper.enqueue(activity, url, userAgent, contentDisposition, mimeType)
            mainHandler.post { message = if (id != null) "Download queued" else "Download could not be started" }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase().orEmpty()
                if (scheme == "https" || scheme == "http") {
                    val snapshot = settingsSnapshot
                    if (snapshot.blockingEnabled && request.isForMainFrame && !request.hasGesture() &&
                        !request.isRedirect && !tab.loading && tab.url.isNotBlank() && !isAllowlisted(tab.url)
                    ) {
                        val fromHost = try { Uri.parse(tab.url).host } catch (_: Exception) { null }
                        val toHost = uri.host
                        if (fromHost != null && toHost != null && baseDomain(fromHost) != baseDomain(toHost)) {
                            mainHandler.post {
                                tab.popupsBlocked += 1
                                message = "Redirect blocked"
                            }
                            return true
                        }
                    }
                    return false
                }
                if (scheme in setOf("mailto", "tel", "sms", "geo")) {
                    return openExternalUri(uri)
                }
                // Never allow web content to navigate directly to file/content/javascript/data schemes.
                return true
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val snapshot = settingsSnapshot
                val host = request.url.host
                sniffMedia(tab, request)
                val blockThis = snapshot.blockingEnabled &&
                    (!request.isForMainFrame || tab.fromPopup) &&
                    blocker.shouldBlock(host, snapshot.allowlist)
                if (blockThis) {
                    if (request.isForMainFrame && tab.fromPopup) {
                        mainHandler.post {
                            message = "Popup blocked"
                            closeTab(tab.id)
                        }
                    } else {
                        mainHandler.post { tab.blockedCount += 1 }
                    }
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        204,
                        "Blocked by HISN",
                        mapOf("Cache-Control" to "no-store"),
                        ByteArrayInputStream(ByteArray(0))
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (!url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://"))) tab.url = url
                tab.loading = true
                tab.error = false
                tab.progress = 0
                tab.media.clear()
                refreshNavigationState(tab, view)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                tab.fromPopup = false
                tab.loading = false
                tab.progress = 100
                tab.url = view.url ?: url ?: tab.url
                tab.title = view.title?.takeIf { it.isNotBlank() } ?: hostLabel(tab.url)
                tab.error = false
                refreshNavigationState(tab, view)
                if (HistoryPolicy.shouldRecord(isPrivate, tab.url)) {
                    val finalUrl = tab.url
                    val finalTitle = tab.title
                    sessionScope.launch { repository.addHistory(finalTitle, finalUrl) }
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    tab.error = true
                    tab.loading = false
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                // Fail closed. HISN never bypasses TLS certificate errors.
                handler.cancel()
                tab.loading = false
                tab.error = true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                tab.progress = newProgress
                tab.loading = newProgress in 1..99
                refreshNavigationState(tab, view)
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                title?.takeIf { it.isNotBlank() }?.let { tab.title = it }
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean = fileChooser(filePathCallback, fileChooserParams)

            override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
                // Granting website camera/microphone permissions requires explicit runtime mediation.
                // Until that flow is implemented, deny rather than grant silently.
                request.deny()
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message
            ): Boolean {
                if (!isUserGesture || tabs.size >= MAX_TABS) return false
                val protect = settingsSnapshot.blockingEnabled && !isAllowlisted(view.url)
                if (protect && !isGenuineLinkTap(view)) {
                    tab.popupsBlocked += 1
                    mainHandler.post { message = "Popup blocked" }
                    return false
                }
                val newTab = newTab()
                newTab.fromPopup = protect
                val popupWebView = ensureWebView(newTab)
                val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                transport.webView = popupWebView
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView) {
                val tab = tabs.firstOrNull { it.webView === window }
                if (tab != null) closeTab(tab.id)
            }
        }
        return webView
    }

    private fun refreshNavigationState(tab: BrowserTab, webView: WebView) {
        mainHandler.post {
            tab.canGoBack = webView.canGoBack()
            tab.canGoForward = webView.canGoForward()
        }
    }

    private fun openExternalUri(uri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            } else {
                mainHandler.post { message = "This link cannot be opened on this device" }
            }
            true
        } catch (_: ActivityNotFoundException) {
            mainHandler.post { message = "This link cannot be opened on this device" }
            true
        } catch (_: Exception) {
            true
        }
    }

    private fun destroyTab(tab: BrowserTab) {
        val view = tab.webView ?: return
        try {
            (view.parent as? ViewGroup)?.removeView(view)
            view.stopLoading()
            view.webChromeClient = null
            view.webViewClient = WebViewClient()
            view.removeAllViews()
            view.destroy()
        } catch (_: Exception) { }
        tab.webView = null
    }

    private fun hostLabel(url: String): String = try {
        Uri.parse(url).host ?: url
    } catch (_: Exception) {
        url
    }

    companion object {
        const val MAX_TABS = 12
        private const val MEDIA_SCAN_JS = "(function(){var o=[];try{document.querySelectorAll('video,audio').forEach(function(v){if(v.currentSrc)o.push(v.currentSrc);if(v.src)o.push(v.src);v.querySelectorAll('source').forEach(function(s){if(s.src)o.push(s.src);});});}catch(e){}return JSON.stringify(o);})()"

        /** Injected at document start. __ALLOW__ is replaced with the allowlist JSON array. */
        private val PROTECTION_JS = """
(function () {
  try {
    var allow = __ALLOW__;
    var host = (location.hostname || '').toLowerCase();
    for (var i = 0; i < allow.length; i++) {
      if (host === allow[i] || host.endsWith('.' + allow[i])) return;
    }
    var deny = function () { return Promise.resolve('denied'); };
    try {
      if (window.Notification) {
        Notification.requestPermission = deny;
        Object.defineProperty(Notification, 'permission', { get: function () { return 'denied'; } });
      }
    } catch (e) {}
    try {
      if (window.PushManager) {
        PushManager.prototype.subscribe = function () {
          return Promise.reject(new DOMException('denied', 'NotAllowedError'));
        };
      }
    } catch (e) {}
    try {
      var nativeOpen = window.open;
      window.open = function () {
        if (navigator.userActivation && !navigator.userActivation.isActive) return null;
        return nativeOpen.apply(window, arguments);
      };
    } catch (e) {}
    var css = 'ins.adsbygoogle,.adsbygoogle,[id^="google_ads_"],[id*="div-gpt-ad"],' +
      'iframe[src*="doubleclick.net"],iframe[src*="googlesyndication.com"],iframe[id^="aswift_"],' +
      '[data-ad-slot],[data-google-query-id],.ad-banner,.ad-wrapper,.advert,.advertisement,' +
      '.sponsored-ad,#taboola-below-article,.OUTBRAIN,.taboola,[id^="ad-container"],[class*="ad-slot"]' +
      '{display:none!important;visibility:hidden!important;height:0!important;min-height:0!important}';
    var style = document.createElement('style');
    style.textContent = css;
    (document.head || document.documentElement).appendChild(style);
    var re = /(allow|enable|turn on|subscribe)[^\n]{0,40}(notification|push|alert)|(notification|push)[^\n]{0,40}(allow|enable)|(السماح|تفعيل)[^\n]{0,30}(الإشعارات|الاشعارات|إشعارات)|(الإشعارات|الاشعارات)[^\n]{0,30}(السماح|تفعيل)/i;
    function sweep() {
      try {
        if (!document.body) return;
        var vw = innerWidth, vh = innerHeight;
        var kids = document.body.children;
        for (var i = kids.length - 1; i >= 0; i--) {
          var e = kids[i];
          var cs = getComputedStyle(e);
          if (cs.position !== 'fixed') continue;
          var r = e.getBoundingClientRect();
          var z = parseInt(cs.zIndex) || 0;
          var txt = (e.innerText || '').slice(0, 300);
          var promptBanner = txt && re.test(txt) && r.width > vw * 0.4;
          var coverIframe = e.tagName === 'IFRAME' && z > 999 && r.width >= vw * 0.6 && r.height >= vh * 0.4;
          if (promptBanner || coverIframe) {
            e.remove();
            document.documentElement.style.overflow = '';
            document.body.style.overflow = '';
          }
        }
      } catch (e) {}
    }
    var runs = 0;
    var timer = setInterval(function () { sweep(); if (++runs > 40) clearInterval(timer); }, 1500);
  } catch (e) {}
})();
""".trimIndent()
        private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}
