package com.example.googlemediapipetest.gles

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class GLObject(
    val renderer : GLRenderer,
    val vertexShaderResID : Int,
    val fragmentShaderResID : Int,
    val objectDrawMode : Int
)
{
    private lateinit var verticesFloatArray : FloatArray
    lateinit var vertexBuffer : FloatBuffer

    var numOfVertices : Int? = null;

    var program = GLHelper.createProgram(vertexShaderResID, fragmentShaderResID)

    public fun setVertices(newVertices : FloatArray)
    {
        verticesFloatArray = newVertices

//        val arrSize = verticesFloatArray.size
//        Log.i("OpenGL", "verticesFloatArray size: $arrSize.")

        vertexBuffer = ByteBuffer
            .allocateDirect(verticesFloatArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(verticesFloatArray)
                position(0)
            }

        numOfVertices = verticesFloatArray.size / 3
    }

    open fun drawObject()
    {
        TODO("Implement drawObject().")
//        GLES32.glUseProgram(program)
    }
}