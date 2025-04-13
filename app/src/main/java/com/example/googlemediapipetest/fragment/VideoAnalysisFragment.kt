package com.example.googlemediapipetest.fragment

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.googlemediapipetest.R
import com.example.googlemediapipetest.VideoAnalysis
import com.example.googlemediapipetest.gles.GLRenderer2D


class VideoAnalysisFragment : Fragment(R.layout.fragment_video_analysis)
{
    lateinit var videoAnalysis : VideoAnalysis

    override fun onCreateView(
        inflater : LayoutInflater,
        container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View?
    {
        return inflater.inflate(R.layout.fragment_video_analysis, container, false)
    }

    override fun onViewCreated(view : View, savedInstanceState : Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

//        Log.i("VideoAnalysisFragment", "onViewCreated() is called. \nDetected savedInstanceState: ${savedInstanceState != null}.")

        videoAnalysis = VideoAnalysis(
            fragmentContext = requireContext(),
            fragmentActivity = requireActivity(),
            ivCurrentFrame = ivCurrentFrame,
            etFrameStep = etFrameStep,
            pbDetectionProgress = pbDetectionProgress,
            glSurfaceView = glSurfaceView,
        )

        setUpUI(view)
    }

    fun setUpUI(view : View)
    {
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        ivCurrentFrame.isEnabled = true
        ivCurrentFrame.visibility = View.VISIBLE

        videoAnalysis.glRenderer2D = GLRenderer2D(requireContext(), this)

        buttonNextFrame.setOnClickListener {
            videoAnalysis.updateFrame(1)
        }
        buttonPrevFrame.setOnClickListener {
            videoAnalysis.updateFrame(-1)
        }

        buttonPickVideo.setOnClickListener {
            videoAnalysis.openVideoPicker()
        }

        pbDetectionProgress.setProgress(0, true)

        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
        glSurfaceView.setZOrderOnTop(true)


        glSurfaceView.setRenderer(videoAnalysis.glRenderer2D)

//        @SuppressLint("ClickableViewAccessibility")
//        glSurfaceView.setOnTouchListener { _, event ->
//            videoAnalysis.glRenderer2D.onTouchEvent(event)
//        }
    }

    // All view bindings here.
    val buttonPickVideo : Button by lazy { requireView().findViewById(R.id.buttonExemplarPickVideo) }
    val ivCurrentFrame : ImageView by lazy { requireView().findViewById(R.id.ivCurrentFrame) }

    val buttonNextFrame : ImageButton by lazy { requireView().findViewById(R.id.buttonNextFrame) }
    val buttonPrevFrame : ImageButton by lazy { requireView().findViewById(R.id.buttonPrevFrame) }
    val etFrameStep : EditText by lazy { requireView().findViewById(R.id.etFrameStep) }

    val pbDetectionProgress : ProgressBar by lazy { requireView().findViewById(R.id.pbExemplarDetectionProgress) }

    val glSurfaceView : GLSurfaceView by lazy { requireView().findViewById(R.id.glSurfaceView) }
}
