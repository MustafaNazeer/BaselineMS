package com.mustafan4x.baselinems.dsp

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MadgwickTest {
    @Test
    fun `static gravity sample converges to upright orientation`() {
        val m = Madgwick(beta = 0.5)
        repeat(2000) {
            m.update(
                gyro = Vector3.ZERO,
                accel = Vector3(0.0, 0.0, 9.80665),
                dtSeconds = 0.01
            )
        }
        val q = m.orientation()
        val zUp = q.rotate(Vector3(0.0, 0.0, 1.0))
        assertTrue(abs(zUp.x) < 0.05)
        assertTrue(abs(zUp.y) < 0.05)
        assertTrue(abs(zUp.z - 1.0) < 0.05)
    }

    @Test
    fun `pure rotation about world Z is reflected in the orientation`() {
        val m = Madgwick(beta = 0.0)
        val omega = 1.0
        val dt = 0.01
        repeat(((2.0 * kotlin.math.PI / omega) / dt).toInt()) {
            m.update(
                gyro = Vector3(0.0, 0.0, omega),
                accel = Vector3(0.0, 0.0, 9.80665),
                dtSeconds = dt
            )
        }
        val q = m.orientation()
        val toleranceDegrees = 5.0
        val angleDegrees = 2.0 * kotlin.math.acos(abs(q.w)) * 180.0 / kotlin.math.PI
        assertTrue("residual angle $angleDegrees", angleDegrees < toleranceDegrees)
    }
}
