package com.example.googlemediapipetest.gles

import android.content.Context
import android.opengl.GLES32
import android.util.Log

class GLHelper
{
    companion object
    {
        private var appContext : Context? = null

        fun init(context : Context)
        {
            appContext = context.applicationContext
        }

        public fun createProgram(vertexShaderResID : Int, fragmentShaderResID : Int) : Int
        {
            val vertexShaderCode = loadShaderFromRawResource(vertexShaderResID)
            val fragmentShaderCode = loadShaderFromRawResource(fragmentShaderResID)

            val vertexShader = compileShaderCode(GLES32.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = compileShaderCode(GLES32.GL_FRAGMENT_SHADER, fragmentShaderCode)

            val program = GLES32.glCreateProgram()
            GLES32.glAttachShader(program, vertexShader)
            GLES32.glAttachShader(program, fragmentShader)
            GLES32.glLinkProgram(program)

            val linked = IntArray(1)
            GLES32.glGetProgramiv(program, GLES32.GL_LINK_STATUS, linked, 0)
            if (linked[0] == 0)
            {
                Log.e("OpenGL", "Program linking failed: " + GLES32.glGetProgramInfoLog(program))
                throw RuntimeException(
                    "Program linking failed: " + GLES32.glGetProgramInfoLog(
                        program
                    )
                )
            }

            return program
        }

        public fun loadShaderFromRawResource(resId : Int) : String
        {
            val stringBuilder = StringBuilder()
            try
            {
                val inputStream = appContext?.resources?.openRawResource(resId)
                val reader = inputStream?.bufferedReader()
                reader.use {
                    it?.forEachLine { line ->
                        stringBuilder.append(line).append("\n")
                    }
                }
            } catch (e : Exception)
            {
                Log.e("LoadingShader", "Error loading shader from raw resources.", e)
            }
            return stringBuilder.toString()
        }

        public fun compileShaderCode(type : Int, shaderCode : String) : Int
        {
            val error = GLES32.glGetError()
            if (error != GLES32.GL_NO_ERROR)
            {
                Log.e("OpenGL", "OpenGL Error before shader creation: $error")
            }

            val shader = GLES32.glCreateShader(type)

            if (shader == 0)
            {
                throw RuntimeException("Error creating shader of type: $type  " + GLES32.glGetString(GLES32.GL_VERSION))
            }

            GLES32.glShaderSource(shader, shaderCode)
            GLES32.glCompileShader(shader)

            val compiled = IntArray(1)
            GLES32.glGetShaderiv(shader, GLES32.GL_COMPILE_STATUS, compiled, 0)

            if (compiled[0] == 0)
            {
                val errorLog = GLES32.glGetShaderInfoLog(shader)
                Log.e("OpenGL", "Shader compilation failed:\n$errorLog\nCode:\n$shaderCode")

                // Clean up failed shader
                GLES32.glDeleteShader(shader)

                throw RuntimeException("Shader compilation failed:\n$errorLog\nCode:\n$shaderCode")
            }

            return shader
        }

    }
}