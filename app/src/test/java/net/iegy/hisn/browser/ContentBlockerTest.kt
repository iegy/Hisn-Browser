package net.iegy.hisn.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentBlockerTest {
    private val blocker = ContentBlocker.fromLines(sequenceOf("# comment", "ads.example", "tracker.example"))

    @Test fun blocksExactHostsAndSubdomains() {
        assertTrue(blocker.shouldBlock("ads.example", emptySet()))
        assertTrue(blocker.shouldBlock("cdn.ads.example", emptySet()))
    }

    @Test fun doesNotBlockUnrelatedOrAllowlistedHosts() {
        assertFalse(blocker.shouldBlock("example.com", emptySet()))
        assertFalse(blocker.shouldBlock("cdn.ads.example", setOf("ads.example")))
    }
}
