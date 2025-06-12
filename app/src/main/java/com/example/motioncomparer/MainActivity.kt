package com.example.motioncomparer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.motioncomparer.fragment.AppSettingsFragment
import com.example.motioncomparer.fragment.CompareMotionsFragment
import com.example.motioncomparer.fragment.VideoAnalysisFragment
import com.example.motioncomparer.gles.FlatSkeleton
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity()
{
    //    lateinit var tvTitle : TextView
    companion object
    {
        var modelName : String = "pose_landmarker_heavy.task"
        var sampleIntervalFrames : Int = 30
        var useGPU : Boolean = false

        var glSurfaceViewWidth = 0
        var glSurfaceViewHeight = 0

        var forceSameAspectRatio : Boolean = true

        var exemplarSkeleton : FlatSkeleton? = null
        var yourSkeleton : FlatSkeleton? = null

        var colorSeed : Int = 0

        var renderOrder : String = "Default"

        var sameColor : Boolean = true

        var currentVideoType : Int = 0
    }

    // Video Type 0: exemplar.
    // Video Type 1: your.
    val exemplarVideoAnalysisFragment : VideoAnalysisFragment = VideoAnalysisFragment(this, 0)
    val yourVideoAnalysisFragment : VideoAnalysisFragment = VideoAnalysisFragment(this, 1)
    val appSettingsFragment : AppSettingsFragment = AppSettingsFragment()

    private val handler = Handler(Looper.getMainLooper())
    private val logRunnable = object : Runnable
    {
        override fun run()
        {
            // Log all fragments' visibility status
            Log.i(
                "FragmentDebug",
                "exemplarVideoAnalysisFragment visibility: ${exemplarVideoAnalysisFragment.isVisible}." +
                        "\nyourVideoAnalysisFragment visibility: ${yourVideoAnalysisFragment.isVisible}."
            )

            // Repeat the task after a delay (e.g., 5 seconds)
            handler.postDelayed(this, 10000)
        }
    }


    override fun onCreate(
        savedInstanceState : Bundle?
    )
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handler.post(logRunnable)

//        tvTitle = findViewById(R.id.tvTitle)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bnvNavigation)

        if (savedInstanceState == null)
        {
            supportFragmentManager.beginTransaction()
                .add(R.id.fcFragmentContainer, exemplarVideoAnalysisFragment)
                .add(R.id.fcFragmentContainer, yourVideoAnalysisFragment)
                .add(R.id.fcFragmentContainer, appSettingsFragment)
                .hide(yourVideoAnalysisFragment)
                .hide(appSettingsFragment)
                .commit()

//            tvTitle.text = bottomNavigation.menu.findItem(bottomNavigation.selectedItemId).title
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId)
            {
                R.id.navExemplar -> switchFragment(exemplarVideoAnalysisFragment, 0)
                R.id.navYou -> switchFragment(yourVideoAnalysisFragment, 1)
                R.id.navSettings -> switchFragment(appSettingsFragment, 3)
            }

//            tvTitle.text = item.title
            true
        }
    }

    private fun switchFragment(fragment : Fragment, fragmentID : Int)
    {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        currentVideoType = fragmentID

        if(fragmentID == 0)
        {
            yourSkeleton?.currentAlpha = 0.6f
            exemplarSkeleton?.currentAlpha = 0.6f
        }
        if(fragmentID == 1)
        {
            exemplarSkeleton?.currentAlpha = 0.6f
            yourSkeleton?.currentAlpha = 0.6f
        }


        supportFragmentManager.fragments.forEach {
            fragmentTransaction.hide(it)
        }

        fragmentTransaction.show(fragment)

        fragmentTransaction.commit()
    }
}