package net.iegy.hisn

import android.os.Bundle
import android.util.Log
import android.webkit.WebView

class PrivateBrowserActivity : BaseBrowserActivity() {
    override val privateMode: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!privateProfileConfigured) {
            try {
                // This activity runs in :private. Set the WebView data directory before any WebView is created.
                WebView.setDataDirectorySuffix("hisn_private")
                privateProfileConfigured = true
            } catch (error: Exception) {
                Log.e("HISN", "Unable to initialize isolated private WebView data directory", error)
            }
        }
        super.onCreate(savedInstanceState)
    }

    companion object {
        @Volatile private var privateProfileConfigured = false
    }
}
