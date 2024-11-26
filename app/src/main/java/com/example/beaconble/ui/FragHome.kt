package com.example.beaconble.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.beaconble.BeaconReferenceApplication
import com.example.beaconble.BeaconSimplified
import com.example.beaconble.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FragHome : Fragment() {
    lateinit var viewModel: Lazy<FragHomeViewModel>

    lateinit var postButton: FloatingActionButton

    lateinit var beaconListView: ListView
    lateinit var beaconCountTextView: TextView
    lateinit var beaconReferenceApplication: BeaconReferenceApplication

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Create a ViewModel the first time the system creates this Class.
        // Re-created fragments receive the same ViewModel instance created by the first one.
        viewModel = viewModels<FragHomeViewModel>()

        // Get the application instance
        beaconReferenceApplication = BeaconReferenceApplication.Companion.instance

        // Inflate the layout for this fragment and find the IDs of the UI elements.
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        postButton = view.findViewById<FloatingActionButton>(R.id.uploadButton)
        beaconListView = view.findViewById<ListView>(R.id.beaconListView)
        beaconCountTextView = view.findViewById<TextView>(R.id.beaconCountTextView)

        // Assign observers and callbacks to the ViewModel's LiveData objects.
        viewModel.value.rangedBeacons.observe(viewLifecycleOwner) { beacons ->
            val adapter = ListAdapterBeacons(requireContext(), R.layout.row_item_beacon, beacons)
            beaconListView.adapter = adapter
        }

        beaconListView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val beacon = parent.getItemAtPosition(position) as BeaconSimplified
                val beaconId = beacon.id
                Log.d("FragHome", "Beacon clicked: $beaconId")
            }

        viewModel.value.nRangedBeacons.observe(viewLifecycleOwner) { n ->
            // Update the top message textview to show the number of beacons detected
            if (n == 0) {
                beaconCountTextView.text = getString(R.string.beacons_detected_zero)
            } else {
                beaconCountTextView.text = getString(R.string.beacons_detected_nonzero, n)
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postButton.setOnClickListener {
            // findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
            // TODO("Implement post button")
            viewModel.value.sendTestData()
        }

        beaconCountTextView.text = getString(R.string.beacons_detected_zero)
    }
}
