package com.example.motioncomparer.fragment

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.motioncomparer.MainActivity
import com.example.motioncomparer.R
import com.example.motioncomparer.VideoAnalysis
import com.example.motioncomparer.gles.FlatSkeleton
import com.example.motioncomparer.gles.SkeletonRenderer


class VideoAnalysisFragment(val mainActivity : MainActivity, val videoType : Int) :
    Fragment(R.layout.fragment_video_analysis)
{
    lateinit var videoAnalysis : VideoAnalysis
    var compareMotionsFragment : CompareMotionsFragment? = null

    lateinit var exemplarVideoButton : SwitchVideoButton
    lateinit var yourVideoButton : SwitchVideoButton

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
            videoType
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

        videoAnalysis.skeletonRenderer = SkeletonRenderer(requireContext(), this)

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


        glSurfaceView.setRenderer(videoAnalysis.skeletonRenderer)

//        @SuppressLint("ClickableViewAccessibility")
//        glSurfaceView.setOnTouchListener { _, event ->
//            videoAnalysis.glRenderer2D.onTouchEvent(event)
//        }

        exemplarVideoButton =
            SwitchVideoButton(
                view.findViewById(R.id.buttonPickFirstVideo),
                ContextCompat.getColor(requireContext(), R.color.green),
                ContextCompat.getColor(requireContext(), R.color.gray),
            )

        yourVideoButton =
            SwitchVideoButton(
                view.findViewById(R.id.buttonPickSecondVideo),
                ContextCompat.getColor(requireContext(), R.color.orange),
                ContextCompat.getColor(requireContext(), R.color.gray),
            )
    }

    class SwitchVideoButton(
        val pickVideoButton : Button,
        val enableColor : Int,
        val disableColor : Int,
    )
    {
        var flatSkeleton : FlatSkeleton? = null
        var enabled = false
        var centerX = 0
        var centerY = 0

        init
        {
            pickVideoButton.setBackgroundColor(disableColor)
            pickVideoButton.setOnClickListener { onClickPickButton() }
        }

        fun onClickPickButton()
        {
            if (enabled)
            {
                startButtonColorAnimation(enableColor, disableColor)
            }
            else
            {
                startButtonColorAnimation(disableColor, enableColor)
            }
            enabled = !enabled
        }

        fun startButtonColorAnimation(startColor : Int, endColor : Int)
        {
            val colorAnimation : ValueAnimator =
                ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)
            colorAnimation.setDuration(350);
            colorAnimation.addUpdateListener { updatedAnimation ->
                pickVideoButton.setBackgroundColor(updatedAnimation.animatedValue as Int)
            }
            colorAnimation.start()
        }
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
