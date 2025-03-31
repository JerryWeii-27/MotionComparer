package com.example.googlemediapipetest.gles

import android.content.Context
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import com.example.googlemediapipetest.HumanModel
import com.example.googlemediapipetest.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class GLRenderer(private val context : Context) : GLSurfaceView.Renderer
{
    var mvpMatrix = FloatArray(16)

    // Objects
    lateinit var floorGrid : GLGrid
    lateinit var triangle : GLTriangles

    // Window info.
    var windowWidth : Float = 1.0f
    var windowHeight : Float = 1.0f
    var aspect = windowWidth / windowHeight


    // Camera info.
    val zNear = 0.1f
    val zFar = 100f
    val fovY = 75f

    var eyeX : Float = 3.0f
    var eyeY : Float = 0.0f
    var eyeZ : Float = 0.0f

    // Look-at target
    val centerX = 0f
    val centerY = 0f
    val centerZ = 0f

    // Up direction
    val upX = 0f
    val upY = 1f
    val upZ = 0f

    var radius : Float = 5.0f
    var horAngle : Float = PI.toFloat() / 4.0f
    var vertAngle : Float = PI.toFloat() / 4.0f

    var maxVertAngle : Float = PI.toFloat() / 2.1f // In radians.

    // Touch info.
    private var lastX = 0.0f
    private var lastY = 0.0f

    public var sensitivity = 0.01f;

    // Frame info.
    private var lastTime : Long = 0 // In nanoseconds
    private var deltaTime : Float = 0.0f // In seconds.

    init
    {
        lastTime = System.nanoTime()
        GLHelper.init(context)
    }

    override fun onSurfaceCreated(
        gl : GL10?,
        config : EGLConfig?
    )
    {
        GLES32.glClearColor(0.2f, 0.2f, 0.3f, 1.0f)
        GLES32.glEnable(GLES32.GL_DEPTH_TEST)

        setCamLocation(horAngle, vertAngle, radius)

        val triangleCoords = floatArrayOf(
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f
        )
        triangle =
            GLTriangles(this, R.raw.simple_vertex_shader, R.raw.red_fragment_shader, triangleCoords)
        floorGrid = GLGrid(this, 10.0f, 0.5f)

        var humanModel = HumanModel(this)
        humanModel.testComputeBuffer()
    }

    override fun onSurfaceChanged(
        gl : GL10?,
        width : Int,
        height : Int
    )
    {
        GLES32.glViewport(0, 0, width, height)

        val viewMatrix = FloatArray(16)

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)

        val projectionMatrix = FloatArray(16)
        windowWidth = width.toFloat()
        windowHeight = height.toFloat()
        aspect = windowWidth / windowHeight  // Screen aspect ratio

        Matrix.perspectiveM(projectionMatrix, 0, fovY, aspect, zNear, zFar)

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    override fun onDrawFrame(gl : GL10?)
    {
        // Update deltaTime.
        updateDeltaTime()

        // Clear buffers at start of each frame.
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT)
        setCamLocation(horAngle, vertAngle, radius)
//        setCamLocation(horAngle + 0.1f * PI.toFloat() * deltaTime, vertAngle, radius)

//        drawTriangle()
        floorGrid.drawObject()
        triangle.drawObject()
    }

    fun updateDeltaTime()
    {
        // Calculate deltaTime (in seconds).
        deltaTime = (System.nanoTime() - lastTime) / 1_000_000_000.0f

        lastTime = System.nanoTime()
    }

    public fun onTouchEvent(event : MotionEvent) : Boolean
    {
        when (event.action)
        {
            MotionEvent.ACTION_DOWN ->
            {
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE ->
            {
                val dx = event.x - lastX
                val dy = event.y - lastY
                handleSwipe(dx, dy)
                lastX = event.x
                lastY = event.y
            }
        }
        return true
    }

    private fun handleSwipe(dx : Float, dy : Float)
    {
        horAngle += dx * sensitivity
        vertAngle += dy * sensitivity

        vertAngle = vertAngle.coerceIn(-maxVertAngle, maxVertAngle)
    }

    private fun setCamLocation(newHorAngle : Float, newVertAngle : Float, newRadius : Float)
    {

        val viewMatrix = FloatArray(16)

        horAngle = newHorAngle
        vertAngle = newVertAngle
        radius = newRadius

        val eyeDirX : Float = cos(horAngle)
        val eyeDirY : Float = tan(vertAngle)
        val eyeDirZ : Float = sin(horAngle)

        val l : Float = sqrt(eyeDirX.pow(2) + eyeDirY.pow(2) + eyeDirZ.pow(2))

        eyeX = eyeDirX / l * radius
        eyeY = eyeDirY / l * radius
        eyeZ = eyeDirZ / l * radius

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)

        val projectionMatrix = FloatArray(16)
        Matrix.perspectiveM(projectionMatrix, 0, fovY, aspect, zNear, zFar)

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }
}