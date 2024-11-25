package com.example.beaconble

import android.os.Bundle
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class FragSettings : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Get the API URI setting
        val editTextPreference = findPreference<EditTextPreference>("api_uri")
        if (editTextPreference == null) {
            Log.w("FragSettings", "API URI setting not found")
        }
        // Callback to update the application API service when the value changes
        editTextPreference?.setOnBindEditTextListener { editText ->
            editText.setOnEditorActionListener { _, _, _ ->
                // Update the endpoint in the API service
                BeaconReferenceApplication.instance.setService(editText.text.toString())
                true
            }
        }
        editTextPreference?.setDefaultValue(BuildConfig.SERVER_URL)
    }

}