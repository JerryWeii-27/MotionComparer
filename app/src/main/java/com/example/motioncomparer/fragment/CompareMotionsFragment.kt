package com.example.motioncomparer.fragment

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
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
import com.example.motioncomparer.R

class CompareMotionsFragment : Fragment(R.layout.fragment_compare_motions)
{
    private lateinit var ibPrevFrame : ImageButton
    private lateinit var ibNextFrame : ImageButton
    private lateinit var glSurfaceView : GLSurfaceView
    private lateinit var etFrameStep : EditText

    lateinit var analyzedExemplarVideo : AnalyzedVideo
    lateinit var analyzedYourVideo : AnalyzedVideo

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

        analyzedExemplarVideo = AnalyzedVideo(
            view.findViewById(R.id.buttonPickFirstVideo),
            ContextCompat.getColor(requireContext(), R.color.green),
            ContextCompat.getColor(requireContext(), R.color.gray)
        )

        analyzedYourVideo = AnalyzedVideo(
            view.findViewById(R.id.buttonPickSecondVideo),
            ContextCompat.getColor(requireContext(), R.color.orange),
            ContextCompat.getColor(requireContext(), R.color.gray)
        )

        ibPrevFrame.setOnClickListener {

        }

        ibNextFrame.setOnClickListener {

        }
    }

    fun updateFrame()
    {
        frameStep = etFrameStep.text.toString().toIntOrNull() ?: frameStep
    }

    class AnalyzedVideo(
        val pickVideoButton : Button,
        val enableColor : Int,
        val disableColor : Int
    )
    {
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
