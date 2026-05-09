package com.mustafanazeer.baselinems.dsp

import kotlin.math.sqrt

data class Vector3(val x: Double, val y: Double, val z: Double) {
    fun magnitude(): Double = sqrt(x * x + y * y + z * z)

    operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)

    companion object {
        val ZERO = Vector3(0.0, 0.0, 0.0)
    }
}
