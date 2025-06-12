package com.example.motioncomparer.fragment

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.motioncomparer.MainActivity
import com.example.motioncomparer.R
import com.example.motioncomparer.gles.DualSkeletonRenderer
import com.example.motioncomparer.gles.FlatSkeleton

class CompareMotionsFragment(
    val mainActivity : MainActivity
) : Fragment(R.layout.fragment_compare_motions)
{
    private lateinit var ibPrevFrame : ImageButton
    private lateinit var ibNextFrame : ImageButton
    private lateinit var glSurfaceView : GLSurfaceView
    private lateinit var etFrameStep : EditText
    private lateinit var buttonRefresh : Button

    val videoType = 2

    lateinit var analyzedExemplarVideo : SwitchVideoButton
    lateinit var analyzedYourVideo : SwitchVideoButton

    lateinit var skeletonRenderer : DualSkeletonRenderer

    private var frameStep : Int = 0

    override fun onCreateView(
        inflater : LayoutInflater, container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View?
    {
        return inflater.inflate(R.layout.fragment_compare_motions, container, false)
    }

    override fun onViewCreated(view : View, savedInstanceState : Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        ibPrevFrame = view.findViewById(R.id.ibPrevFrame)
        ibNextFrame = view.findViewById(R.id.ibNextFrame)
        glSurfaceView = view.findViewById(R.id.glSurfaceView)
        etFrameStep = view.findViewById(R.id.etFrameStep)

        buttonRefresh = view.findViewById(R.id.buttonRefresh)

        analyzedExemplarVideo = SwitchVideoButton(
            view.findViewById(R.id.buttonPickFirstVideo),
            ContextCompat.getColor(requireContext(), R.color.green),
            ContextCompat.getColor(requireContext(), R.color.gray),
        )

        analyzedYourVideo = SwitchVideoButton(
            view.findViewById(R.id.buttonPickSecondVideo),
            ContextCompat.getColor(requireContext(), R.color.orange),
            ContextCompat.getColor(requireContext(), R.color.gray),
        )

        buttonRefresh.setOnClickListener {
        }

        ibPrevFrame.setOnClickListener {

        }

        ibNextFrame.setOnClickListener {

        }

        skeletonRenderer = DualSkeletonRenderer(requireContext(), this)
        glSurfaceView.setRenderer(skeletonRenderer)


        Log.i("CompareMotionsFragment", "onViewCreated: Created.")
    }

    fun updateFrame()
    {
        frameStep = etFrameStep.text.toString().toIntOrNull() ?: frameStep
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
}
