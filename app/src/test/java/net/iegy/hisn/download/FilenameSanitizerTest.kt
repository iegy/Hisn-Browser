package net.iegy.hisn.download

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilenameSanitizerTest {
    @Test fun removesPathTraversalAndForbiddenCharacters() {
        val safe = FilenameSanitizer.sanitize("../../secret\\file:name?.txt")
        assertFalse(safe.contains("/"))
        assertFalse(safe.contains("\\"))
        assertFalse(safe.contains(":"))
        assertFalse(safe.contains("?"))
    }

    @Test fun handlesEmptyAndLongNames() {
        assertTrue(FilenameSanitizer.sanitize("\u0000\n").isNotBlank())
        assertTrue(FilenameSanitizer.sanitize("a".repeat(240)).length <= 150)
    }
}
