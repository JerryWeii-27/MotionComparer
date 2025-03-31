package com.example.googlemediapipetest.gles

import android.opengl.GLES32

class GLTriangles(
    renderer : GLRenderer,
    vertexShaderResID : Int,
    fragmentShaderResID : Int,
    verticesList : FloatArray
) : GLObject(renderer, vertexShaderResID, fragmentShaderResID, GLES32.GL_TRIANGLES)
{
    init
    {
        setVertices(verticesList)
    }

    override fun drawObject()
    {
        // Use the program for rendering
        GLES32.glUseProgram(program)

        // Get attribute and uniform locations from the shader program
        val positionHandle = GLES32.glGetAttribLocation(program, "vPosition")
        val mvpMatrixHandle = GLES32.glGetUniformLocation(program, "uMVPMatrix")
        GLES32.glUniformMatrix4fv(mvpMatrixHandle, 1, false, renderer.mvpMatrix, 0)

        // Enable position attribute
        GLES32.glEnableVertexAttribArray(positionHandle)
        GLES32.glVertexAttribPointer(
            positionHandle,
            3,
            GLES32.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        // Draw the object
        GLES32.glDrawArrays(objectDrawMode, 0, numOfVertices ?: 0)

        // Disable the position attribute
        GLES32.glDisableVertexAttribArray(positionHandle)
    }
}
