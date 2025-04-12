package com.example.googlemediapipetest.fragment

import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.googlemediapipetest.MainActivity
import com.example.googlemediapipetest.R
import android.content.SharedPreferences

class AppSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Set initial values to match current preferences.
        updateRequireRestartCompanionValues(prefs)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.i("Preferences", "$key -> ${sharedPreferences.all[key]}.")
//        updateRequireRestartCompanionValues(sharedPreferences)
    }

    private fun updateRequireRestartCompanionValues(sharedPreferences: SharedPreferences) {
        val model = sharedPreferences.getString("model", MainActivity.modelName)
        val interval = sharedPreferences.getInt("interval", MainActivity.sampleIntervalFrames)

        if (model != null) {
            MainActivity.modelName = model
        }

        MainActivity.sampleIntervalFrames = interval

        Log.i("UpdatedCompanion", "modelName = ${MainActivity.modelName}, sampleIntervalFrames = ${MainActivity.sampleIntervalFrames}.")
    }

    override fun onResume() {
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}
