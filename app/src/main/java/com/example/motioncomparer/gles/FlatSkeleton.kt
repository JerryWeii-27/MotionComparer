package com.example.motioncomparer.gles

import android.opengl.EGL14
import android.opengl.GLES32
import android.util.Log
import com.example.motioncomparer.MainActivity
import com.example.motioncomparer.R
import com.example.motioncomparer.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FlatSkeleton() : GLObject(
    R.raw.flat_vertex_shader,
    R.raw.flat_fragment_shader,
    GLES32.GL_TRIANGLES
)
{
    public lateinit var packedPositionData : FloatArray
    public lateinit var packedColorData : FloatArray

    public var currentAlpha : Float = 0.6f

    public var totalFrames : Int = 0
    var lastFrame : Int = -1
    var vertexCount = 0

    public var allFramesAdded : Boolean = false
    private lateinit var landmarksPos : Array<Vector3>

    // Vertex positions and normals
    private val trianglesList = mutableListOf<Vector3>()
    private val colorsList = mutableListOf<Vector3>()

    // 2D flat skeleton settings.
    var limbsKiteWidthOverLength : Float = 0.2f

    var videoType : Int = -1;

    public fun bindVBO()
    {
        // Vertices
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[0])

        val frameSizeBytes = vertexCount * 3 * 4 // (x,y,z) × 4 bytes per float.
        GLES32.glBufferData(
            GLES32.GL_ARRAY_BUFFER,
            frameSizeBytes,
            null, // Allocate space but don't upload data yet.
            GLES32.GL_DYNAMIC_DRAW
        )

        // Normals
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[1])

        GLES32.glBufferData(
            GLES32.GL_ARRAY_BUFFER,
            frameSizeBytes,
            null, // Allocate space but don't upload data yet.
            GLES32.GL_DYNAMIC_DRAW
        )
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0)
    }

    override fun drawObjectAtFrame(frame : Int)
    {
        val currentContext = EGL14.eglGetCurrentContext()
        Log.d("GL_CONTEXT", "Context handle: ${currentContext.hashCode()}")
        Log.i("OpenGLDrawFrame", "drawObjectAtFrame: Drawing humanModel at frame $frame.")
        if (!allFramesAdded)
        {
            Log.i("OpenGLDrawFrame", "drawObjectAtFrame: Not all frames added.")
            return
        }
        bindVBO()

        GLES32.glUseProgram(program)
        Log.i("OpenGLDrawFrame", "drawObjectAtFrame: Using program: $program.")
        currentFrame = frame

        if (frame != lastFrame)
        {
            updateVBOs(frame)
        }

        // Pos buffer.
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[0])

        val positionHandle = GLES32.glGetAttribLocation(program, "vPosition")
        GLES32.glEnableVertexAttribArray(positionHandle)
        GLES32.glVertexAttribPointer(
            positionHandle,
            3,
            GLES32.GL_FLOAT,
            false,
            0,
            0
        )

        // Color buffer.
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[1])
        val colorHandle = GLES32.glGetAttribLocation(program, "vColor")
        GLES32.glEnableVertexAttribArray(colorHandle)
        GLES32.glVertexAttribPointer(
            colorHandle,
            3,
            GLES32.GL_FLOAT,
            false,
            0,
            0
        )

        val alphaHandle = GLES32.glGetUniformLocation(program, "uAlpha")
        GLES32.glUniform1f(alphaHandle, currentAlpha)

        val secondColorHandle = GLES32.glGetUniformLocation(program, "useSecondColor")
        if (MainActivity.sameColor)
        {
            GLES32.glUniform1i(secondColorHandle, 0)
        }
        else
        {
            GLES32.glUniform1i(secondColorHandle, videoType)
        }

        val zPosHandle = GLES32.glGetUniformLocation(program, "zPosition")

        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, vertexCount)

        GLES32.glDisableVertexAttribArray(positionHandle)
        GLES32.glDisableVertexAttribArray(colorHandle)
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0)
    }

    fun updateVBOs(frameInAnimationData : Int)
    {
        Log.i(
            "OpenGL",
            "FlatSkeleton updateVBOs: ${frameInAnimationData + 1} out of $totalFrames frames."
        )
        require(frameInAnimationData in 0 until totalFrames)

        // Calculate offset in packedAnimationData.
        val frameOffset = frameInAnimationData * vertexCount * 3

        // Bind VBO.
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[0])

        // Copy only the current frame's data to GPU.
        val buffer = ByteBuffer
            .allocateDirect(vertexCount * 3 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(packedPositionData, frameOffset, vertexCount * 3)
                position(0)
            }

        GLES32.glBufferSubData(
            GLES32.GL_ARRAY_BUFFER,
            0,
            vertexCount * 3 * 4,
            buffer
        )

        // Bind VBO.
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[1])

        // Copy only the current frame's data to GPU.
        buffer.clear()
        buffer.put(packedColorData, frameOffset, vertexCount * 3).position(0)
        GLES32.glBufferSubData(
            GLES32.GL_ARRAY_BUFFER,
            0,
            vertexCount * 3 * 4,
            buffer
        )
    }

    public fun updateJointsForSingleFrame(frame : Int, newLandmarksPos : Array<Vector3>)
    {
        landmarksPos = newLandmarksPos

        // MakeBox(landmarkA, landmarkB, width)
        // normal of the box =
        // Torso1: 11 24 12
        quickAddTriangle(11, 24, 12)

        // Torso2: 11 24 23
        quickAddTriangle(11, 24, 23)

        // Right upper arm: 12 14
        quickAddLimb(12, 14)

        // Right lower arm: 14 16
        quickAddLimb(14, 16)
        // Right hand: 16 18 20
        quickAddTriangle(16, 18, 20)

        // Left upper arm: 11 13
        quickAddLimb(11, 13)
        // Left lower arm: 13 15
        quickAddLimb(13, 15)
        // Left hand: 15 17 19
        quickAddTriangle(15, 17, 19)

        // Right upper leg: 24 26
        quickAddLimb(24, 26)
        // Right lower leg: 26 28
        quickAddLimb(26, 28)
        // Right foot: 28 30 32
        quickAddTriangle(28, 30, 32)

        // Left upper leg: 23 25
        quickAddLimb(23, 25)
        // Left lower leg: 25 27
        quickAddLimb(25, 27)
        // Left foot: 27 29 31
        quickAddTriangle(27, 29, 31)

        // Face: 0
//        val earMid = (landmarksPos[7] + landmarksPos[8]) / 2.0f
//        val noseToEarMid = (earMid - landmarksPos[0]).normalize()
//        quickAddSphere(earMid + noseToEarMid * 0.15f)
        if (frame == 0)
        {
            Log.i("OpenGL", "Triangles list: ${landmarksPos.toList()}")
        }

        if (!::packedPositionData.isInitialized)
        {
            vertexCount = trianglesList.size
            packedPositionData = FloatArray(vertexCount * 3 * totalFrames)
            packedColorData = FloatArray(vertexCount * 3 * totalFrames)
            val s = vertexCount * 3 * totalFrames
            Log.i("OpenGL", "Packed animation data size: $s. \nVertex count: $vertexCount.")
        }

        flattenToAnimationData(frame, trianglesList)

        trianglesList.clear()
        colorsList.clear()
    }

    fun notVisible(x : Int) : Boolean
    {
        return landmarksPos[x].x == 114.514f && landmarksPos[x].y == 114.514f && landmarksPos[x].z == 114.514f
    }

    fun quickAddTriangle(a : Int, b : Int, c : Int)
    {
        if (notVisible(a) || notVisible(b) || notVisible(c))
        {
            for (i in 0..2)
            {
                trianglesList.add(Vector3(10000f, 10000f, 10000f))
                colorsList.add(Vector3(0f, 0f, 0f))
            }
            return
        }

        trianglesList.add(landmarksPos[a])
        trianglesList.add(landmarksPos[b])
        trianglesList.add(landmarksPos[c])

        val color = getSeededColor(a + b + c, -a + b - c)
        for (i in 0..2)
        {
            colorsList.add(color)
        }
    }

    fun quickAddLimb(start : Int, end : Int)
    {
        if (notVisible(start) || notVisible(end))
        {
            for (i in 0..5)
            {
                trianglesList.add(Vector3(10000f, 10000f, 10000f))
                colorsList.add(Vector3(0f, 0f, 0f))
            }
            return
        }

        val p1 = landmarksPos[start]
        val p2 = landmarksPos[end]

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y

        val dir = Vector3(dx, dy, 0f)
        val limbLength = dir.magnitude()
        val center = p1 + dir * 0.3f

        val perp = Vector3(-dy, dx, 0f).normalized()

        val p3 = center + perp * limbsKiteWidthOverLength * limbLength
        val p4 = center - perp * limbsKiteWidthOverLength * limbLength

        trianglesList.add(p1)
        trianglesList.add(p2)
        trianglesList.add(p3)

        trianglesList.add(p1)
        trianglesList.add(p4)
        trianglesList.add(p2)

        val color = getSeededColor(start * end, end + start)
        for (i in 0..5)
        {
            colorsList.add(color)
        }
    }

    fun getSeededColor(i : Int, j : Int) : Vector3
    {
        val seed = i * 73856093 xor j * 199663 + MainActivity.colorSeed
        val random = java.util.Random(seed.toLong())

        // Generate hue (0-1), fixed high saturation & brightness
        val h = random.nextFloat()
        val s = 0.9f  // High saturation
        val v = 0.9f  // High brightness

        // Convert HSV to RGB (hue in [0,1], s/v in [0,1])
        val rgb = hsvToRgb(h, s, v)
        return Vector3(rgb[0], rgb[1], rgb[2])
    }

    // Helper: HSV to RGB conversion
    fun hsvToRgb(h : Float, s : Float, v : Float) : FloatArray
    {
        val i = (h * 6).toInt()
        val f = h * 6 - i
        val p = v * (1 - s)
        val q = v * (1 - f * s)
        val t = v * (1 - (1 - f) * s)

        return when (i % 6)
        {
            0 -> floatArrayOf(v, t, p)
            1 -> floatArrayOf(q, v, p)
            2 -> floatArrayOf(p, v, t)
            3 -> floatArrayOf(p, q, v)
            4 -> floatArrayOf(t, p, v)
            5 -> floatArrayOf(v, p, q)
            else -> floatArrayOf(0f, 0f, 0f)
        }
    }

    fun flattenToAnimationData(frame : Int, vertices : List<Vector3>)
    {
        val offset = frame * vertexCount * 3

        try
        {
            for (i in vertices.indices)
            {
                packedPositionData[offset + i * 3 + 0] = vertices[i].x
                packedPositionData[offset + i * 3 + 1] = vertices[i].y
                packedPositionData[offset + i * 3 + 2] = vertices[i].z

                packedColorData[offset + i * 3 + 0] = colorsList[i].x
                packedColorData[offset + i * 3 + 1] = colorsList[i].y
                packedColorData[offset + i * 3 + 2] = colorsList[i].z
            }
        }
        catch (e : RuntimeException)
        {
            Log.e("OpenGL", "flattenToAnimationData: $offset")
            throw e
        }
    }
}