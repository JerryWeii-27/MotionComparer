package com.example.googlemediapipetest

import android.opengl.GLES32
import android.util.Log
import com.example.googlemediapipetest.gles.GLHelper
import com.example.googlemediapipetest.gles.GLObject
import com.example.googlemediapipetest.gles.GLRenderer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HumanModel(renderer : GLRenderer) : GLObject(
    renderer,
    R.raw.simple_vertex_shader,
    R.raw.white_fragment_shader,
    GLES32.GL_TRIANGLES
)
{
    public lateinit var landmarksPos : FloatArray

    public fun updateJoints(newLandmarksPos : FloatArray)
    {
        landmarksPos = newLandmarksPos
    }

    init
    {

    }
}