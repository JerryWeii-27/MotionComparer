package com.example.googlemediapipetest

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.googlemediapipetest.fragment.AppSettingsFragment
import com.example.googlemediapipetest.fragment.CompareMotionsFragment
import com.example.googlemediapipetest.fragment.VideoAnalysisFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity()
{
    //    lateinit var tvTitle : TextView
    companion object
    {
        var modelName : String = "pose_landmarker_heavy.task"
        var sampleIntervalFrames : Int = 30
        var useGPU : Boolean = false
    }

    val exemplarVideoAnalysisFragment : VideoAnalysisFragment = VideoAnalysisFragment()
    val yourVideoAnalysisFragment : VideoAnalysisFragment = VideoAnalysisFragment()
    val compareMotionsFragment : CompareMotionsFragment = CompareMotionsFragment()
    val appSettingsFragment : AppSettingsFragment = AppSettingsFragment()

    private val handler = Handler(Looper.getMainLooper())
    private val logRunnable = object : Runnable {
        override fun run() {
            // Log all fragments' visibility status
            Log.i("FragmentDebug", "exemplarVideoAnalysisFragment visibility: ${exemplarVideoAnalysisFragment.isVisible}." +
                    "\nyourVideoAnalysisFragment visibility: ${yourVideoAnalysisFragment.isVisible}.")

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
                .add(R.id.fcFragmentContainer, compareMotionsFragment)
                .add(R.id.fcFragmentContainer, appSettingsFragment)
                .hide(yourVideoAnalysisFragment)
                .hide(compareMotionsFragment)
                .hide(appSettingsFragment)
                .commit()

//            tvTitle.text = bottomNavigation.menu.findItem(bottomNavigation.selectedItemId).title
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId)
            {
                R.id.navExemplar -> switchFragment(exemplarVideoAnalysisFragment)
                R.id.navYou -> switchFragment(yourVideoAnalysisFragment)
                R.id.navCompare -> switchFragment(compareMotionsFragment)
                R.id.navSettings -> switchFragment(appSettingsFragment)
            }

//            tvTitle.text = item.title
            true
        }
    }

    private fun switchFragment(fragment : Fragment)
    {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        supportFragmentManager.fragments.forEach {
            fragmentTransaction.hide(it)
        }

        fragmentTransaction.show(fragment)

        fragmentTransaction.commit()
    }
}