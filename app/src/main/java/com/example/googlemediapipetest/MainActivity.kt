package com.example.googlemediapipetest

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.googlemediapipetest.fragment.AppSettings
import com.example.googlemediapipetest.fragment.CompareMotions
import com.example.googlemediapipetest.VideoAnalysis
import com.example.googlemediapipetest.fragment.VideoAnalysisFragment
import com.example.googlemediapipetest.fragment.YourVideoAnalysis
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity()
{
    //    lateinit var tvTitle : TextView
    val exemplarVideoAnalysisFragment : VideoAnalysisFragment = VideoAnalysisFragment()
    val yourVideoAnalysisFragment : VideoAnalysisFragment = VideoAnalysisFragment()

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
                .hide(yourVideoAnalysisFragment)
                .commit()

//            tvTitle.text = bottomNavigation.menu.findItem(bottomNavigation.selectedItemId).title
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId)
            {
                R.id.navExemplar -> switchFragment(exemplarVideoAnalysisFragment)
                R.id.navYou -> switchFragment(yourVideoAnalysisFragment)
                R.id.navCompare -> switchFragment(CompareMotions())
                R.id.navSettings -> switchFragment(AppSettings())
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