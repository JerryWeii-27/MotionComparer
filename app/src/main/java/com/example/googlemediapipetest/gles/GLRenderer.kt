package com.example.googlemediapipetest.gles

import android.content.Context
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.MotionEvent
import com.example.googlemediapipetest.R
import com.example.googlemediapipetest.fragment.VideoAnalysisFragment
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class GLRenderer(private val context : Context, val fragment : VideoAnalysisFragment) :
    GLSurfaceView.Renderer
{
    var mvpMatrix = FloatArray(16)

    // Objects.
    lateinit var floorGrid : GLGrid
    lateinit var triangle : GLTriangles
    lateinit var flatSkeleton : FlatSkeleton
    lateinit var humanModel : HumanModel
    var currentFrame : Int = 0
    var totalDeltaTimeFromLastFrameMS = 0
    var sampleInterval = 0;

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
    var minRadius : Float = 0.1f
    var maxRadius : Float = 10f
    var horAngle : Float = PI.toFloat() / 4.0f
    var vertAngle : Float = PI.toFloat() / 4.0f

    var maxVertAngle : Float = PI.toFloat() / 2.1f // In radians.

    // Touch info.
    private var lastX = 0.0f
    private var lastY = 0.0f
    private var initialPinchDistance = 0f
    private var isPinching = false

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
        Log.d("OpenGLThread", "OpenGL running on thread: ${Thread.currentThread().id}")
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)


        setCamLocation(horAngle, vertAngle, radius)

        val triangleCoords = floatArrayOf(
            // Bottom face (y=0)
            0f, 0f, 0f,
            1f, 0f, 0f,
            1f, 0f, 1f,
            0f, 0f, 1f,
            1f, 0f, 1f,
            0f, 0f, 0f,

            // Front face (z=0)
            1f, 1f, 0f,
            0f, 0f, 0f,
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 0f,
            1f, 1f, 0f,

            // Left face (x=0)
            0f, 1f, 0f,
            0f, 0f, 0f,
            0f, 0f, 1f,
            0f, 1f, 0f,
            0f, 0f, 1f,
            0f, 1f, 1f,

            // Right face (x=1)
            1f, 1f, 0f,
            1f, 0f, 0f,
            1f, 1f, 1f,
            1f, 1f, 1f,
            1f, 0f, 0f,
            1f, 0f, 1f,

            // Back face (z=1)
            0f, 1f, 1f,
            0f, 0f, 1f,
            1f, 0f, 1f,
            0f, 1f, 1f,
            1f, 0f, 1f,
            1f, 1f, 1f,

            // Top face (y=1)
            1f, 1f, 0f,
            0f, 1f, 0f,
            0f, 1f, 1f,
            1f, 1f, 1f,
            1f, 1f, 0f,
            0f, 1f, 1f
        )
        triangle =
            GLTriangles(
                this,
                R.raw.colored_vertex_shader,
                R.raw.colored_fragment_shader,
                triangleCoords
            )

        floorGrid = GLGrid(this, 10.0f, 0.5f)

        humanModel = HumanModel(this)
        flatSkeleton = FlatSkeleton(this)
    }

    public fun newHumanModel(totalFrames : Int)
    {
        humanModel = HumanModel(this)
        humanModel.totalFrames = totalFrames
    }

    public fun newFlatSkeleton(totalFrames : Int)
    {
        flatSkeleton = FlatSkeleton(this)
        flatSkeleton.totalFrames = totalFrames
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
        if (!fragment.isVisible)
        {
            return
        }

        // Update deltaTime.

        // Clear buffers at start of each frame.
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT)
        setCamLocation(horAngle, vertAngle, radius)
//        setCamLocation(horAngle + 0.1f * PI.toFloat() * deltaTime, vertAngle, radius)

//        floorGrid.drawObject()
//        triangle.drawObject()

        drawFlatSkeleton()
    }

    fun drawFlatSkeleton()
    {
        // Update flat skeleton according to user's actions.
//        Log.i("OpenGL", "onDrawFrame: $currentFrame. \nDrawing is ${flatSkeleton.allFramesAdded}.")
        if (flatSkeleton.allFramesAdded)
        {
            flatSkeleton.drawObjectAtFrame(currentFrame)
        }
    }

    fun drawHumanModel()
    {
        // Update human model on its onw.
        if (humanModel.allFramesAdded)
        {
            updateDeltaTime()
            Log.i("HumanModel", "Total frames: ${humanModel.totalFrames}")
            totalDeltaTimeFromLastFrameMS += (deltaTime * 1000f).toInt()

            currentFrame += totalDeltaTimeFromLastFrameMS / sampleInterval
            currentFrame %= humanModel.totalFrames
            totalDeltaTimeFromLastFrameMS %= sampleInterval

            humanModel.drawObjectAtFrame(currentFrame)
            Log.i("OpenGL", "onDrawFrame: $currentFrame")
        }
    }

    fun updateDeltaTime()
    {
        // Calculate deltaTime (in seconds).
        deltaTime = (System.nanoTime() - lastTime) / 1_000_000_000.0f

        lastTime = System.nanoTime()
    }

    public fun onTouchEvent(event : MotionEvent) : Boolean
    {
        when (event.actionMasked)
        {
            MotionEvent.ACTION_DOWN ->
            {
                lastX = event.x
                lastY = event.y
                isPinching = false
            }

            MotionEvent.ACTION_POINTER_DOWN ->
            {
                if (event.pointerCount == 2)
                {
                    initialPinchDistance = getDistance(event)
                    isPinching = true
                }
            }

            MotionEvent.ACTION_MOVE ->
            {
                if (isPinching && event.pointerCount == 2)
                {
                    val newDistance = getDistance(event)
                    val scale = newDistance / initialPinchDistance
                    handlePinch(scale)
                    initialPinchDistance = newDistance
                }
                else if (!isPinching)
                {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    handleSwipe(dx, dy)
                    lastX = event.x
                    lastY = event.y
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP ->
            {
                if (event.pointerCount <= 2)
                {
                    isPinching = false
                }
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

    private fun handlePinch(scale : Float)
    {
        // scale > 1 → zoom in, scale < 1 → zoom out
        radius /= scale
        radius = radius.coerceIn(minRadius, maxRadius)
    }

    private fun getDistance(event : MotionEvent) : Float
    {
        if (event.pointerCount >= 2)
        {
            val dx = event.getX(0) - event.getX(1)
            val dy = event.getY(0) - event.getY(1)
            return kotlin.math.sqrt(dx * dx + dy * dy)
        }
        return 0f
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