package com.example.motioncomparer.gles

import android.opengl.GLES32
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class GLObject(
    val renderer : GLRenderer2D,
    val vertexShaderResID : Int,
    val fragmentShaderResID : Int,
    val objectDrawMode : Int
)
{
    private lateinit var verticesFloatArray : FloatArray
    lateinit var vertexBuffer : FloatBuffer
    private lateinit var colorFloatArray : FloatArray
    lateinit var colorBuffer : FloatBuffer

    val buffers = IntArray(2)

    var numOfVertices : Int? = null;

    var program = GLHelper.createProgram(vertexShaderResID, fragmentShaderResID)

    init
    {
        GLES32.glGenBuffers(buffers.size, buffers, 0)
        renderer.glObjects.add(this)
    }

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

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[0])
        GLES32.glBufferData(
            GLES32.GL_ARRAY_BUFFER,
            vertexBuffer.capacity() * 4,
            vertexBuffer,
            GLES32.GL_STATIC_DRAW
        )
    }

    public fun setVertexColor(newVertexColor : FloatArray)
    {
        colorFloatArray = newVertexColor

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[1])
        colorBuffer = ByteBuffer
            .allocateDirect(colorFloatArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(newVertexColor)
                position(0)
            }

        GLES32.glBufferData(
            GLES32.GL_ARRAY_BUFFER,
            colorBuffer.capacity() * 4,
            colorBuffer,
            GLES32.GL_STATIC_DRAW
        )
    }

    open fun drawObject()
    {
        TODO("Implement drawObject().")
    }

    open fun drawObjectAtFrame(frame : Int)
    {
        TODO("Implement drawObjectAtFrame().")
    }
}