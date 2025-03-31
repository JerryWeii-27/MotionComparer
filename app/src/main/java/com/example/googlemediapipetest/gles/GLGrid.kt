package com.example.googlemediapipetest.gles

import android.opengl.GLES32
import android.util.Log
import com.example.googlemediapipetest.R

class GLGrid(
    renderer : GLRenderer,
    val gridSideLength : Float,
    val step : Float
) : GLObject(
    renderer,
    R.raw.simple_vertex_shader,
    R.raw.white_fragment_shader,
    GLES32.GL_LINES
)
{
    init
    {
        val gridCount = (gridSideLength / step / 2.0f).toInt()
        val verticesList = mutableListOf<Float>()

        for (i in -gridCount..gridCount)
        {
            // Horizontal lines.
            verticesList.add(-gridSideLength / 2.0f)
            verticesList.add(0.0f)
            verticesList.add(i * step)

            verticesList.add(gridSideLength / 2.0f)
            verticesList.add(0.0f)
            verticesList.add(i * step)

            // Vertical lines.
            verticesList.add(i * step)
            verticesList.add(0.0f)
            verticesList.add(-gridSideLength / 2.0f)

            verticesList.add(i * step)
            verticesList.add(0.0f)
            verticesList.add(gridSideLength / 2.0f)
        }

        setVertices(verticesList.toFloatArray())
    }

    override fun drawObject()
    {
        try
        {
            GLES32.glUseProgram(program)
            val positionHandle = GLES32.glGetAttribLocation(program, "vPosition")
            val mvpMatrixHandle = GLES32.glGetUniformLocation(program, "uMVPMatrix")

            if (positionHandle == -1)
            {
                Log.e("OpenGL", "Could not find attribute a_Position")
                return
            }

            if (mvpMatrixHandle == -1)
            {
                Log.e("OpenGL", "Could not find uniform uMVPMatrix")
            }
            GLES32.glEnableVertexAttribArray(positionHandle)
            GLES32.glVertexAttribPointer(
                positionHandle,
                3,
                GLES32.GL_FLOAT,
                false,
                0,
                vertexBuffer
            )

            GLES32.glUniformMatrix4fv(mvpMatrixHandle, 1, false, renderer.mvpMatrix, 0)
            GLES32.glDrawArrays(GLES32.GL_LINES, 0, numOfVertices!!)
            GLES32.glDisableVertexAttribArray(positionHandle)
        } catch (e : Exception)
        {
            Log.e("OpenGL", "drawObject: $e")
        }
    }
}