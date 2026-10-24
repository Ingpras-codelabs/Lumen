package com.offlineviewer.app.render

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for [computeInSampleSize], the bounds-first downsampling helper
 * that keeps large images from OOMing on low-RAM devices. The BitmapFactory
 * decode itself is Android-bound and not unit-testable here, but the sample-size
 * math is pure and is the part that determines whether a large image renders.
 */
class ImageRendererTest {

    @Test
    fun noDownsamplingWhenImageFitsTarget() {
        assertEquals(1, computeInSampleSize(1024, 768, 2048))
        assertEquals(1, computeInSampleSize(2048, 2048, 2048))
    }

    @Test
    fun downsamplesByPowersOfTwoForLargeImages() {
        // Standard Android sampling: largest power-of-two such that doubling it
        // would drop a dimension below the target (so result stays near target).
        // 4096: at /2 -> 2048 (doubling to /4 -> 1024 < 2048), so 2.
        assertEquals(2, computeInSampleSize(4096, 4096, 2048))
        // 8192: at /4 -> 2048, so 4.
        assertEquals(4, computeInSampleSize(8192, 8192, 2048))
        // 20000: at /8 -> 2500 (doubling to /16 -> 1250 < 2048), so 8.
        assertEquals(8, computeInSampleSize(20000, 20000, 2048))
    }

    @Test
    fun usesLargerDimensionToDecideSampling() {
        // Wide-but-short image: width drives the sampling.
        // 9000 at /4 -> 2250 (doubling to /8 -> 1125 < 2048), so 4.
        assertEquals(4, computeInSampleSize(9000, 500, 2048))
    }

    @Test
    fun returnsOneForInvalidBounds() {
        // A failed bounds pass reports -1 dimensions; must not loop or crash.
        assertEquals(1, computeInSampleSize(-1, -1, 2048))
        assertEquals(1, computeInSampleSize(0, 0, 2048))
        assertEquals(1, computeInSampleSize(4096, 4096, 0))
    }
}
