package com.mustafanazeer.baselinems.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class QuaternionTest {

    @Test
    fun `inverse of non identity quaternion produces approximately identity rotation behavior on a test vector`() {
        val angle = PI / 3.0
        val axisX = 0.0
        val axisY = 0.0
        val axisZ = 1.0
        val q = Quaternion(
            w = cos(angle / 2.0),
            x = axisX * sin(angle / 2.0),
            y = axisY * sin(angle / 2.0),
            z = axisZ * sin(angle / 2.0)
        )
        val v = Vector3(0.7, 0.4, 0.5)
        val roundTrip = q.inverse().rotate(q.rotate(v))
        val tolerance = 1e-9
        assertTrue("x component drift ${abs(roundTrip.x - v.x)}", abs(roundTrip.x - v.x) < tolerance)
        assertTrue("y component drift ${abs(roundTrip.y - v.y)}", abs(roundTrip.y - v.y) < tolerance)
        assertTrue("z component drift ${abs(roundTrip.z - v.z)}", abs(roundTrip.z - v.z) < tolerance)
    }

    @Test
    fun `identity inverse equals identity`() {
        val inv = Quaternion.IDENTITY.inverse()
        val tolerance = 1e-12
        assertEquals(1.0, inv.w, tolerance)
        assertEquals(0.0, inv.x, tolerance)
        assertEquals(0.0, inv.y, tolerance)
        assertEquals(0.0, inv.z, tolerance)
    }
}
