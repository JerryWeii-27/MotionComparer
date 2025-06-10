package com.example.motioncomparer.gles

import android.content.Context
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.motioncomparer.fragment.VideoAnalysisFragment
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SingleSkeletonRenderer(private val context : Context, val fragment : VideoAnalysisFragment) :
    GLSurfaceView.Renderer
{
    // Objects.
    lateinit var flatSkeleton : FlatSkeleton
    var currentFrame : Int = 0
    var sampleInterval = 0

    // Window info.
    var windowWidth : Float = 1.0f
    var windowHeight : Float = 1.0f
    var aspect = windowWidth / windowHeight

    init
    {
        GLHelper.init(context)
    }

    override fun onSurfaceCreated(
        gl : GL10?,
        config : EGLConfig?
    )
    {
        Log.d("OpenGLThread", "OpenGL running on thread: ${Thread.currentThread().id}")
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        flatSkeleton = FlatSkeleton(this)
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

        windowWidth = width.toFloat()
        windowHeight = height.toFloat()
        aspect = windowWidth / windowHeight  // Screen aspect ratio
    }

    override fun onDrawFrame(gl : GL10?)
    {
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT)

        if (!fragment.isVisible)
        {
            return
        }

        drawFlatSkeleton()
    }

    fun drawFlatSkeleton()
    {
        // Update flat skeleton according to user's actions.
//        Log.i("OpenGL", "onDrawFrame: $currentFrame. \nDrawing is ${flatSkeleton.allFramesAdded}.")
        flatSkeleton.drawObjectAtFrame(currentFrame)

    }
}