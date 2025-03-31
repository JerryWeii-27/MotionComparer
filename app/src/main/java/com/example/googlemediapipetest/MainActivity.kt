package com.example.googlemediapipetest

import android.os.Bundle
import android.view.Menu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.googlemediapipetest.fragment.AppSettings
import com.example.googlemediapipetest.fragment.CompareMotions
import com.example.googlemediapipetest.fragment.ExemplarVideoAnalysis
import com.example.googlemediapipetest.fragment.YourVideoAnalysis
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity()
{
//    lateinit var tvTitle : TextView

    override fun onCreate(
        savedInstanceState : Bundle?
    )
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        tvTitle = findViewById(R.id.tvTitle)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bnvNavigation)

        if (savedInstanceState == null)
        {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fcFragmentContainer,
                    ExemplarVideoAnalysis()
                ) // Replace with your fragment class
                .commit()

//            tvTitle.text = bottomNavigation.menu.findItem(bottomNavigation.selectedItemId).title
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId)
            {
                R.id.navExemplar -> switchFragment(ExemplarVideoAnalysis())
                R.id.navYou -> switchFragment(YourVideoAnalysis())
                R.id.navCompare -> switchFragment(CompareMotions())
                R.id.navSettings -> switchFragment(AppSettings())
            }

//            tvTitle.text = item.title

            true
        }
    }

    private fun switchFragment(fragment : Fragment)
    {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcFragmentContainer, fragment)
            .addToBackStack(null)  // Allows back navigation
            .commit()
    }
}