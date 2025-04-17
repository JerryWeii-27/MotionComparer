package com.example.motioncomparer.fragment

import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.motioncomparer.MainActivity
import com.example.motioncomparer.R
import android.content.SharedPreferences

class AppSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Set initial values to match current preferences.
        updateRequireRestartSettings(prefs)
        updateSimpleSettings(prefs)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.i("Preferences", "$key -> ${sharedPreferences.all[key]}.")
//        updateRequireRestartCompanionValues(sharedPreferences)
        updateSimpleSettings(sharedPreferences)
    }

    private fun updateRequireRestartSettings(sharedPreferences: SharedPreferences) {
        val model = sharedPreferences.getString("model", MainActivity.modelName)
        val interval = sharedPreferences.getInt("interval", MainActivity.sampleIntervalFrames)
        val useGPU = sharedPreferences.getBoolean("useGPU", MainActivity.useGPU)

        if (model != null) {
            MainActivity.modelName = model
        }

        MainActivity.sampleIntervalFrames = interval

        MainActivity.useGPU = useGPU

        for (entry in sharedPreferences.all.entries) {
            Log.d("Preferences", "Key: ${entry.key}, Value: ${entry.value}.")
        }
    }

    private fun updateSimpleSettings(sharedPreferences: SharedPreferences)
    {
        val forceSameAspectRatio = sharedPreferences.getBoolean("forceSameAspect", MainActivity.forceSameAspectRatio)
        MainActivity.forceSameAspectRatio = forceSameAspectRatio
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
