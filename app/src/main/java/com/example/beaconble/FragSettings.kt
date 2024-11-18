package com.example.beaconble

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import android.widget.Toast

class FragSettings : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        Toast.makeText(context, "These are your settings", Toast.LENGTH_SHORT).show()
    }

}