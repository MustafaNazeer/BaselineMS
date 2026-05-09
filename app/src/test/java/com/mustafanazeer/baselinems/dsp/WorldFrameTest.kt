package com.mustafanazeer.baselinems.dsp

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WorldFrameTest {
    @Test
    fun `device frame vertical of plus z transforms to world frame z when device is upright`() {
        val deviceVerticalLinear = Vector3(0.0, 0.0, 1.0)
        val upright = Quaternion.IDENTITY
        val world = WorldFrame.toWorld(upright, deviceVerticalLinear)
        assertTrue(abs(world.z - 1.0) < 1e-9)
        assertTrue(abs(world.x) < 1e-9)
        assertTrue(abs(world.y) < 1e-9)
    }

    @Test
    fun `gravity estimate from a static window approximates 9_8 mps2 magnitude`() {
        val staticWindow = (0 until 100).map { Vector3(0.0, 0.0, 9.80665) }
        val g = WorldFrame.estimateGravity(staticWindow)
        assertTrue(abs(g.magnitude() - 9.80665) < 0.05)
    }
}
