package com.hanyz.stopme

import com.hanyz.stopme.util.LocationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun testHaversineDistance_monasToBundaranHi() {
        // Monas (-6.1754, 106.8272) ke Bundaran HI (-6.1925, 106.8228) ~ 1.9 - 2.0 km
        val dist = LocationUtils.haversineDistanceMeters(-6.1754, 106.8272, -6.1925, 106.8228)
        assertTrue(dist in 1800.0..2200.0)
    }

    @Test
    fun testFormatDistance_underThousand() {
        val formatted = LocationUtils.formatDistance(450.0)
        assertEquals("450 m", formatted)
    }

    @Test
    fun testFormatDistance_aboveThousand() {
        val formatted = LocationUtils.formatDistance(1500.0)
        assertTrue(formatted.contains("1.5") || formatted.contains("1,5"))
    }

    @Test
    fun testFormatEta() {
        val etaMinutes = LocationUtils.formatEta(180)
        assertEquals("3 mnt", etaMinutes)
    }
}
