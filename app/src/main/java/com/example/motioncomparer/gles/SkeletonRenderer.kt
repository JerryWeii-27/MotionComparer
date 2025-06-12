package com.example.motioncomparer.gles

import android.content.Context
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.motioncomparer.MainActivity
import com.example.motioncomparer.fragment.CompareMotionsFragment
import com.example.motioncomparer.fragment.VideoAnalysisFragment
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SkeletonRenderer(private val context : Context, val fragment : VideoAnalysisFragment) :
    GLSurfaceView.Renderer
{
    // Objects.
    lateinit var flatSkeleton : FlatSkeleton
    val videoType = fragment.videoType
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

        if (videoType == 0)
        {
            MainActivity.exemplarSkeleton = FlatSkeleton()
            flatSkeleton = MainActivity.exemplarSkeleton!!
        }
        else if (videoType == 1)
        {
            MainActivity.yourSkeleton = FlatSkeleton()
            flatSkeleton = MainActivity.yourSkeleton!!
        }
    }

    public fun newFlatSkeleton(totalFrames : Int)
    {
        if (videoType == 0)
        {
            MainActivity.exemplarSkeleton = FlatSkeleton()
            MainActivity.exemplarSkeleton?.videoType = 0
            flatSkeleton = MainActivity.exemplarSkeleton!!
        }
        else if (videoType == 1)
        {
            MainActivity.yourSkeleton = FlatSkeleton()
            MainActivity.yourSkeleton?.videoType = 1
            flatSkeleton = MainActivity.yourSkeleton!!
        }
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
        Log.i(
            "SkeletonRenderer",
            "---------------------------------\ndrawFlatSkeleton: Drawing skeleton.\n"
        )

        if (MainActivity.exemplarSkeleton == null)
        {
            Log.e("SecondSkeleton", "drawFlatSkeleton: MainActivity.exemplarSkeleton == null")
        }

        if (MainActivity.yourSkeleton == null)
        {
            Log.e("SecondSkeleton", "MainActivity.yourSkeleton == null")
        }
        flatSkeleton.currentFrame = currentFrame

        if (MainActivity.renderOrder == "Default")
        {
            if (MainActivity.currentVideoType == 1)
            {
                drawExemplarSkeleton()
                drawYourSkeleton()
            }
            else
            {
                drawYourSkeleton()
                drawExemplarSkeleton()
            }
        }
        else if (MainActivity.renderOrder == "Vid 1 in front")
        {
            drawYourSkeleton()
            drawExemplarSkeleton()
        }
        else
        {
            drawExemplarSkeleton()
            drawYourSkeleton()
        }
    }

    private fun drawYourSkeleton()
    {
        if (fragment.yourVideoButton.enabled)
        {
            MainActivity.yourSkeleton?.drawObjectAtFrame(MainActivity.yourSkeleton!!.currentFrame)
        }
    }

    private fun drawExemplarSkeleton()
    {
        if (fragment.exemplarVideoButton.enabled)
        {
            MainActivity.exemplarSkeleton?.drawObjectAtFrame(MainActivity.exemplarSkeleton!!.currentFrame)
        }
    }
}