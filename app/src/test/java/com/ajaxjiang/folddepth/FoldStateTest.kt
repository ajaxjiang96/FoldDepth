package com.ajaxjiang.folddepth

import com.ajaxjiang.folddepth.model.calculateStretchScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldStateTest {

    @Test
    fun testKeyframeValues() {
        // 180° -> 1.0x (normal width)
        assertEquals(1.0f, calculateStretchScale(180f), 0.001f)

        // 130° -> 1.5x
        assertEquals(1.5f, calculateStretchScale(130f), 0.001f)

        // 115° -> 2.0x
        assertEquals(2.0f, calculateStretchScale(115f), 0.001f)

        // 100° -> 3.0x
        assertEquals(3.0f, calculateStretchScale(100f), 0.001f)

        // 0° -> 3.6x
        assertEquals(3.6f, calculateStretchScale(0f), 0.001f)
    }

    @Test
    fun testStrictMonotonicity() {
        var prevScale = calculateStretchScale(180f)
        for (angleInt in 179 downTo 0) {
            val angle = angleInt.toFloat()
            val scale = calculateStretchScale(angle)
            assertTrue(
                "Scale at $angle° ($scale) should be >= scale at ${angle + 1}° ($prevScale)",
                scale >= prevScale
            )
            prevScale = scale
        }
    }
}
