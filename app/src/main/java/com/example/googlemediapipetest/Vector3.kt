package com.example.googlemediapipetest

import kotlin.math.sqrt

data class Vector3(val x : Float = 0.0f, val y : Float = 0.0f, val z : Float = 0.0f)
{
    operator fun minus(other : Vector3) : Vector3 =
        Vector3(x - other.x, y - other.y, z - other.z)

    operator fun unaryMinus() : Vector3 = Vector3(-x, -y, -z)

    operator fun plus(other : Vector3) : Vector3 =
        Vector3(x + other.x, y + other.y, z + other.z)

    operator fun times(other : Float) : Vector3 =
        Vector3(x * other, y * other, z * other)

    operator fun div(other : Float) : Vector3 =
        Vector3(x / other, y / other, z / other)

    fun cross(other : Vector3) : Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun dot(other : Vector3) : Float = x * other.x + y * other.y + z * other.z

    fun magnitude() : Float = sqrt(x * x + y * y + z * z)

    fun normalized() : Vector3
    {
        val mag = magnitude()
        return if (mag == 0.0f) this else Vector3(x / mag, y / mag, z / mag)
    }

    override fun toString() : String
    {
        return "Vector3(x=$x, y=$y, z=$z)"
    }

    companion object
    {
        fun computeNormal(a : Vector3, b : Vector3, c : Vector3) : Vector3
        {
            val v1 = b - a
            val v2 = c - a
            return v1.cross(v2).normalized()
        }
    }
}