package dev.groceryledger.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun comparesSemanticVersionsNumerically() {
        assertTrue(VersionComparator.isNewer("1.0.0", "v1.0.1"))
        assertTrue(VersionComparator.isNewer("1.0.0", "1.0.10"))
        assertTrue(VersionComparator.isNewer("1.2.0", "1.10.0"))
        assertTrue(VersionComparator.isNewer("1.0.0", "2"))
        assertFalse(VersionComparator.isNewer("1.0.1", "v1.0.1"))
        assertFalse(VersionComparator.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun ignoresBuildAndPreReleaseSuffixes() {
        assertFalse(VersionComparator.isNewer("1.0.0-debug", "v1.0.0"))
        assertFalse(VersionComparator.isNewer("1.0.0+local", "v1.0.0"))
        assertTrue(VersionComparator.isNewer("1.0.0-debug", "v1.0.1"))
    }

    @Test
    fun nonNumericTagsAreTreatedAsDifferent() {
        assertTrue(VersionComparator.isNewer("1.0.0", "latest"))
        assertFalse(VersionComparator.isNewer("latest", "latest"))
        assertEquals("1.2.3", VersionComparator.normalize(" v1.2.3 "))
    }
}
