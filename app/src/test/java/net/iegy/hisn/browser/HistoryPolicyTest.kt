package net.iegy.hisn.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPolicyTest {
    @Test fun neverRecordsPrivateUrls() {
        assertFalse(HistoryPolicy.shouldRecord(true, "https://example.com"))
    }

    @Test fun recordsOnlyHttpAndHttpsInRegularMode() {
        assertTrue(HistoryPolicy.shouldRecord(false, "https://example.com"))
        assertTrue(HistoryPolicy.shouldRecord(false, "http://example.com"))
        assertFalse(HistoryPolicy.shouldRecord(false, "file:///private"))
    }
}
