package com.example.googlemediapipetest.gles

import android.opengl.GLES32
import android.util.Log
import com.example.googlemediapipetest.R
import com.example.googlemediapipetest.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class HumanModel(renderer : GLRenderer) : GLObject(
    renderer,
    R.raw.human_vertex_shader,
    R.raw.human_fragment_shader,
    GLES32.GL_TRIANGLES
)
{
    public lateinit var packedPositionData : FloatArray
    public lateinit var packedNormalData : FloatArray

    public var totalFrames = 0
    var vertexCount = 0

    public var haveLandmarks : Boolean = false
    public var allFramesAdded : Boolean = false
    private lateinit var landmarksPos : Array<Vector3>

    // Vertex positions and normals
    private val trianglesList = mutableListOf<Vector3>()
    private val normalsList = mutableListOf<Vector3>()

    val torsoThickness = 0.07f
    val limbsRadius = 0.055f
    val limbsRes = 6

    val headRadius = 0.12f
    val headRes = 4

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
        GLES32.glUseProgram(program)
        Log.i("OpenGLDrawFrame", "drawObjectAtFrame: Drawing humanModel")

        updateVBOs(frame)

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

        // Normal buffer.
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, buffers[1])
        val normalHandle = GLES32.glGetAttribLocation(program, "vNormal")
        GLES32.glEnableVertexAttribArray(normalHandle)
        GLES32.glVertexAttribPointer(
            normalHandle,
            3,
            GLES32.GL_FLOAT,
            false,
            0,
            0
        )

        // mvpMatrix.
        val mvpMatrixHandle = GLES32.glGetUniformLocation(program, "uMVPMatrix")
        GLES32.glUniformMatrix4fv(mvpMatrixHandle, 1, false, renderer.mvpMatrix, 0)
        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, vertexCount)

        GLES32.glDisableVertexAttribArray(positionHandle)
        GLES32.glDisableVertexAttribArray(normalHandle)
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0)
    }

    fun updateVBOs(frameInAnimationData : Int)
    {
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
        buffer.put(packedNormalData, frameOffset, vertexCount * 3).position(0)
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
        quickAddTwoTriangles(11, 24, 12, torsoThickness)

        // Torso2: 11 24 23
        quickAddTwoTriangles(11, 24, 23, torsoThickness)

        // Right upper arm: 12 14
        quickAddPillar(12, 14)

        // Right lower arm: 14 16
        quickAddPillar(14, 16)
        // Right hand: 16 18 20
        quickAddTwoTriangles(16, 18, 20, limbsRadius)

        // Left upper arm: 11 13
        quickAddPillar(11, 13)
        // Left lower arm: 13 15
        quickAddPillar(13, 15)
        // Left hand: 15 17 19
        quickAddTwoTriangles(15, 17, 19, limbsRadius)

        // Right upper leg: 24 26
        quickAddPillar(24, 26)
        // Right lower leg: 26 28
        quickAddPillar(26, 28)
        // Right foot: 28 30 32
        quickAddTwoTriangles(28, 30, 32, limbsRadius)

        // Left upper leg: 23 25
        quickAddPillar(23, 25)
        // Left lower leg: 25 27
        quickAddPillar(25, 27)
        // Left foot: 27 29 31
        quickAddTwoTriangles(27, 29, 31, limbsRadius)

        // Face: 0
//        val earMid = (landmarksPos[7] + landmarksPos[8]) / 2.0f
//        val noseToEarMid = (earMid - landmarksPos[0]).normalize()
//        quickAddSphere(earMid + noseToEarMid * 0.15f)

        if (!::packedPositionData.isInitialized)
        {
            vertexCount = trianglesList.size
            packedPositionData = FloatArray(vertexCount * 3 * totalFrames)
            packedNormalData = FloatArray(vertexCount * 3 * totalFrames)
            val s = vertexCount * 3 * totalFrames
            Log.i("OpenGL", "Packed animation data size: $s. \nVertex count: $vertexCount.")
        }

        flattenToAnimationData(frame, trianglesList)
        trianglesList.clear()
        normalsList.clear()
    }

    fun quickAddTwoTriangles(
        i : Int,
        j : Int,
        k : Int,
        thickness : Float
    )
    {
        val a = landmarksPos[i]
        val b = landmarksPos[j]
        val c = landmarksPos[k]
        val normal = Vector3.Companion.computeNormal(a, b, c)

        val d = a + normal * thickness
        val e = b + normal * thickness
        val f = c + normal * thickness
        val x = a - normal * thickness
        val y = b - normal * thickness
        val z = c - normal * thickness

        val startIndex = trianglesList.indices.last + 1
        // Front.
        trianglesList.add(d)
        trianglesList.add(e)
        trianglesList.add(f)

        // Back.
        trianglesList.add(x)
        trianglesList.add(y)
        trianglesList.add(z)

        // Side 1.
        trianglesList.add(d)
        trianglesList.add(x)
        trianglesList.add(y)
        trianglesList.add(d)
        trianglesList.add(e)
        trianglesList.add(y)

        // Side 2.
        trianglesList.add(e)
        trianglesList.add(y)
        trianglesList.add(f)
        trianglesList.add(z)
        trianglesList.add(y)
        trianglesList.add(f)

        // Side 3.
        trianglesList.add(z)
        trianglesList.add(d)
        trianglesList.add(x)
        trianglesList.add(z)
        trianglesList.add(d)
        trianglesList.add(f)

        val center = (a + b + c) / 3f

        for (i in 0 until 8)
        {
            addNormals(
                center,
                trianglesList[startIndex + 3 * i + 0],
                trianglesList[startIndex + 3 * i + 1],
                trianglesList[startIndex + 3 * i + 2]
            )
        }
    }

    fun quickAddPillar(i : Int, j : Int)
    {
        val normal = landmarksPos[i] - landmarksPos[j]
        val center = (landmarksPos[i] + landmarksPos[j]) / 2f

        val u : Vector3 = normal.cross(Vector3(0f, 114.514f, 0f)).normalize()
        val v = normal.cross(u).normalize()

        val pointsArr = Array(2 * limbsRes + 2) { Vector3() }
        pointsArr[0] = landmarksPos[i]  // Top center
        pointsArr[1] = landmarksPos[j]  // Bottom center

        // Generate points around the top and bottom circles.
        for (k in 0 until limbsRes)
        {
            val angle = k.toFloat() / limbsRes.toFloat() * 2f * PI.toFloat()
            val dir = u * limbsRadius * cos(angle) + v * limbsRadius * sin(angle)

            // Top circle points (indices 2..2+limbsRes-1)
            pointsArr[2 + k] = landmarksPos[i] + dir

            // Bottom circle points (indices 2+limbsRes..2+2*limbsRes-1)
            pointsArr[2 + limbsRes + k] = landmarksPos[j] + dir
        }

        val startIndex = trianglesList.indices.last + 1
        // 1. Side faces.
        for (k in 0 until limbsRes)
        {
            val nextK = (k + 1) % limbsRes

            // Top and bottom vertices for current and next segment.
            val topCurrent = 2 + k
            val topNext = 2 + nextK
            val bottomCurrent = 2 + limbsRes + k
            val bottomNext = 2 + limbsRes + nextK

            // Triangle 1 (topCurrent -> topNext -> bottomCurrent).
            trianglesList.add(pointsArr[topCurrent])
            trianglesList.add(pointsArr[topNext])
            trianglesList.add(pointsArr[bottomCurrent])

            // Triangle 2 (topNext -> bottomNext -> bottomCurrent).
            trianglesList.add(pointsArr[topNext])
            trianglesList.add(pointsArr[bottomNext])
            trianglesList.add(pointsArr[bottomCurrent])
        }

        // 2. Top cap (fan of triangles).
        for (k in 0 until limbsRes)
        {
            val nextK = (k + 1) % limbsRes
            trianglesList.add(pointsArr[0])  // Top center.
            trianglesList.add(pointsArr[2 + k])
            trianglesList.add(pointsArr[2 + nextK])
        }

        // 3. Bottom cap (fan of triangles).
        for (k in 0 until limbsRes)
        {
            val nextK = (k + 1) % limbsRes
            trianglesList.add(pointsArr[1])  // Bottom center.
            trianglesList.add(pointsArr[2 + limbsRes + nextK])
            trianglesList.add(pointsArr[2 + limbsRes + k])
        }

        for (i in 0 until 4 * limbsRes)
        {
            addNormals(
                center,
                trianglesList[startIndex + 3 * i + 0],
                trianglesList[startIndex + 3 * i + 1],
                trianglesList[startIndex + 3 * i + 2]
            )
        }
    }

    fun quickAddSphere(center : Vector3)
    {
        // Generate sphere vertices using latitude-longitude approach.
        val vertices = mutableListOf<Vector3>()
        val phiSteps = headRes
        val thetaSteps = headRes * 2

        // Generate vertices.
        for (phi in 0..phiSteps)
        {
            val phiAngle = PI.toFloat() * phi.toFloat() / phiSteps.toFloat()
            val sinPhi = sin(phiAngle)
            val cosPhi = cos(phiAngle)

            for (theta in 0..thetaSteps)
            {
                val thetaAngle = 2f * PI.toFloat() * theta.toFloat() / thetaSteps.toFloat()
                val sinTheta = sin(thetaAngle)
                val cosTheta = cos(thetaAngle)

                val x = cosTheta * sinPhi
                val y = cosPhi
                val z = sinTheta * sinPhi

                vertices.add(center + Vector3(x, y, z) * headRadius)
            }
        }

        // Generate triangles.
        for (phi in 0 until phiSteps)
        {
            for (theta in 0 until thetaSteps)
            {
                val first = (phi * (thetaSteps + 1)) + theta
                val second = first + thetaSteps + 1

                // First triangle
                trianglesList.add(vertices[first])
                trianglesList.add(vertices[second])
                trianglesList.add(vertices[first + 1])

                addNormals(
                    center,
                    vertices[first],
                    vertices[second],
                    vertices[first + 1]
                )

                // Second triangle
                trianglesList.add(vertices[first + 1])
                trianglesList.add(vertices[second])
                trianglesList.add(vertices[second + 1])

                addNormals(
                    center,
                    vertices[first + 1],
                    vertices[second],
                    vertices[second + 1]
                )
            }
        }
    }

    fun addNormals(center : Vector3, v1 : Vector3, v2 : Vector3, v3 : Vector3)
    {
        val triCenter = (v1 + v2 + v3) / 3.0f
        val normal = Vector3.Companion.computeNormal(v1, v2, v3)

        val toCenter = (center - triCenter).normalize()
        if (normal.dot(toCenter) > 0)
        {
            normalsList.add(-normal)
            normalsList.add(-normal)
            normalsList.add(-normal)
        }
        else
        {
            normalsList.add(normal)
            normalsList.add(normal)
            normalsList.add(normal)
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

                packedNormalData[offset + i * 3 + 0] = normalsList[i].x
                packedNormalData[offset + i * 3 + 1] = normalsList[i].y
                packedNormalData[offset + i * 3 + 2] = normalsList[i].z
            }
        } catch (e : RuntimeException)
        {
            Log.e("OpenGL", "flattenToAnimationData: $offset")
            throw e
        }
    }
}