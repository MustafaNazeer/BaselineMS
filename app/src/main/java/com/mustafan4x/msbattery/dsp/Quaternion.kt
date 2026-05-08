package com.mustafan4x.msbattery.dsp

import kotlin.math.sqrt

data class Quaternion(val w: Double, val x: Double, val y: Double, val z: Double) {
    fun normalized(): Quaternion {
        val n = sqrt(w * w + x * x + y * y + z * z)
        if (n == 0.0) return IDENTITY
        return Quaternion(w / n, x / n, y / n, z / n)
    }

    /**
     * Returns the rotation that undoes this one. For unit quaternions this is the conjugate
     * `Quaternion(w, -x, -y, -z)`; the result is normalized so the API stays well behaved on
     * inputs that are slightly off the unit sphere. The intended round trip property is
     * `q.inverse().rotate(q.rotate(v))` is approximately `v` for unit `q`. The Phase 3 DSP code
     * does not call this on the per sample loop, so the allocation cost of constructing a new
     * Quaternion is acceptable; consumers that need an in place conjugate should inline the
     * sign flip themselves.
     */
    fun inverse(): Quaternion = Quaternion(w, -x, -y, -z).normalized()

    fun rotate(v: Vector3): Vector3 {
        val ww = w * w; val xx = x * x; val yy = y * y; val zz = z * z
        val wx = w * x; val wy = w * y; val wz = w * z
        val xy = x * y; val xz = x * z; val yz = y * z
        return Vector3(
            x = (ww + xx - yy - zz) * v.x + 2.0 * (xy - wz) * v.y + 2.0 * (xz + wy) * v.z,
            y = 2.0 * (xy + wz) * v.x + (ww - xx + yy - zz) * v.y + 2.0 * (yz - wx) * v.z,
            z = 2.0 * (xz - wy) * v.x + 2.0 * (yz + wx) * v.y + (ww - xx - yy + zz) * v.z
        )
    }

    companion object {
        val IDENTITY = Quaternion(1.0, 0.0, 0.0, 0.0)
    }
}
