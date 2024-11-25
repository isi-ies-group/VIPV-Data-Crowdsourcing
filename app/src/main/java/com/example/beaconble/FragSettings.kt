package com.example.beaconble

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class FragSettings : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<EditTextPreference>("api_uri")?.setOnBindEditTextListener { editText ->
            editText.setOnEditorActionListener { _, _, _ ->
                // Save the new value
                val newValue = editText.text.toString()
                val sharedPreferences = preferenceManager.sharedPreferences
                sharedPreferences?.edit()?.putString("api_uri", newValue)?.apply()

                // Update the endpoint in the API service
                BeaconReferenceApplication.instance.updateService(newValue)
                true
            }
        }
    }

}