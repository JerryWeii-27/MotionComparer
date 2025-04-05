package com.example.googlemediapipetest.gles

import android.opengl.GLES32
import android.util.Log
import com.example.googlemediapipetest.Vector3
import kotlin.contracts.Returns
import kotlin.random.Random

class GLTriangles(
    renderer : GLRenderer,
    vertexShaderResID : Int,
    fragmentShaderResID : Int,
    verticesList : FloatArray,
) : GLObject(renderer, vertexShaderResID, fragmentShaderResID, GLES32.GL_TRIANGLES)
{
    init
    {
        setVertices(verticesList)


        val randomColors : FloatArray = FloatArray(numOfVertices!! * 4)
        Random.nextFloat()
        var curColor = Vector3(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
        for (i in 0 until numOfVertices!!)
        {
            if (i % 3 == 0)
            {
                curColor = Vector3(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
            }

            randomColors[4 * i] = curColor.x
            randomColors[4 * i + 1] = curColor.y
            randomColors[4 * i + 2] = curColor.z
            randomColors[4 * i + 3] = 1f
        }
        setVertexColor(randomColors)

        Log.i("OpenGL", "randomColors: ${randomColors.toList()}, $numOfVertices.")
    }

    override fun drawObject()
    {
        GLES32.glUseProgram(program)

        // Matrix
        val mvpMatrixHandle = GLES32.glGetUniformLocation(program, "uMVPMatrix")
        GLES32.glUniformMatrix4fv(mvpMatrixHandle, 1, false, renderer.mvpMatrix, 0)

        val positionHandle = GLES32.glGetAttribLocation(program, "vPosition")
        GLES32.glEnableVertexAttribArray(positionHandle)
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[0])
        GLES32.glVertexAttribPointer(
            positionHandle,
            3,
            GLES32.GL_FLOAT,
            false,
            0,
            0
        )

        // Color
        val colorHandle = GLES32.glGetAttribLocation(program, "vColor")

        if (colorHandle == -1)
        {
            Log.e("OpenGL", "GLTriangle drawObject: No 'vColor' attribute.")
        }

        GLES32.glEnableVertexAttribArray(colorHandle)
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[1])
        GLES32.glVertexAttribPointer(colorHandle, 4, GLES32.GL_FLOAT, false, 0, 0)

        GLES32.glDrawArrays(objectDrawMode, 0, numOfVertices ?: 0)

        GLES32.glDisableVertexAttribArray(positionHandle)
        GLES32.glDisableVertexAttribArray(colorHandle)
    }
}
