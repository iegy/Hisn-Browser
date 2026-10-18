package net.iegy.hisn.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlResolverTest {
    @Test fun addsHttpsToHostname() {
        assertEquals("https://example.com", UrlResolver.resolve("example.com"))
    }

    @Test fun ordinaryTextUsesSelectedSearchEngine() {
        val result = UrlResolver.resolve("حصن browser", "duckduckgo")
        assertTrue(result!!.startsWith("https://duckduckgo.com/?q="))
        assertTrue(result.contains("%D8%AD"))
    }

    @Test fun rejectsDangerousAddressBarSchemes() {
        assertNull(UrlResolver.resolve("javascript:alert(1)"))
        assertNull(UrlResolver.resolve("file:///etc/passwd"))
        assertNull(UrlResolver.resolve("content://com.example.provider/item"))
    }

    @Test fun supportsSearchEngineOptions() {
        assertTrue(UrlResolver.searchUrl("hello world", "bing").startsWith("https://www.bing.com/search?q=hello%20world"))
        assertTrue(UrlResolver.searchUrl("hello world", "google").startsWith("https://www.google.com/search?q=hello%20world"))
    }
}
