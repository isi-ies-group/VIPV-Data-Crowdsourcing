package com.example.beaconble.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.example.beaconble.AppMain
import com.example.beaconble.BuildConfig
import com.example.beaconble.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FragSettings : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Ge the upload only on wifi setting
        val switchPreference = findPreference<SwitchPreference>("upload_only_wifi")
        // set callback to update the application service when the value changes
        switchPreference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                // TODO AppMain.instance.setUploadOnlyOnWifi(newValue as Boolean)
                true
            }

        // Get the API URI setting
        val editTextPreference = findPreference<EditTextPreference>("api_uri")
        if (editTextPreference == null) {
            Log.w("FragSettings", "API URI setting not found")
        }
        // set callback to update the application API service when the value changes
        editTextPreference?.setOnBindEditTextListener { editText ->
            editText.setOnEditorActionListener { _, _, _ ->
                // Update the endpoint in the API service
                var newUri = editText.text.toString()
                if (!newUri.endsWith("/")) {
                    newUri += "/"
                }
                if (!newUri.startsWith("http://") && !newUri.startsWith("https://")) {
                    newUri = "http://$newUri"
                }
                AppMain.Companion.instance.setService(newUri)
                true
            }
        }
        editTextPreference?.setDefaultValue(BuildConfig.SERVER_URL)

        // Get test api endpoint button preference
        val testApiEndpoint = findPreference<Preference>("test_api_endpoint")
        // set callback to update the application API service when the value changes
        testApiEndpoint?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            lifecycleScope.launch {
                val isUp = AppMain.instance.testApiEndpoint()
                // show a toast with the result
                Toast.makeText(
                    requireContext(),
                    if (isUp) getString(R.string.settings_api_valid) else getString(R.string.settings_api_invalid),
                    Toast.LENGTH_LONG
                ).show()
            }
            true
        }
    }
}