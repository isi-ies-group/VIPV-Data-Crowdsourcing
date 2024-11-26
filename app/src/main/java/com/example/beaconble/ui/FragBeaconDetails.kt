package com.example.beaconble.ui

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.beaconble.R

class FragBeaconDetails : Fragment() {

    companion object {
        fun newInstance() = FragBeaconDetails()
    }

    private val viewModel: FragBeaconDetailsViewModel by viewModels()

    lateinit var textViewBeaconId: TextView
    lateinit var editTextBeaconDescription: EditText
    lateinit var editTextBeaconTilt: EditText
    lateinit var editTextBeaconDirection: EditText
    lateinit var listViewBeaconMeasurements: ListView

    lateinit var adapter: ListAdapterSensorEntries

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            val beaconId = arguments?.getString("beaconId")
            viewModel.loadBeacon(beaconId)
        }
        activity?.title = "Beacon Details"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment and find the IDs of the UI elements.
        val view = inflater.inflate(R.layout.fragment_beacon_details, container, false)

        textViewBeaconId = view.findViewById<TextView>(R.id.tvBeaconIdentifier)
        editTextBeaconDescription = view.findViewById<EditText>(R.id.editTextDescription)
        editTextBeaconTilt = view.findViewById<EditText>(R.id.editTextTilt)
        editTextBeaconDirection = view.findViewById<EditText>(R.id.editTextDirection)
        listViewBeaconMeasurements = view.findViewById<ListView>(R.id.listViewSensorEntries)

        adapter = ListAdapterSensorEntries(requireContext(), ArrayList())
        listViewBeaconMeasurements.adapter = adapter

        // Assign observers and callbacks to the ViewModel's LiveData objects.
        viewModel.beacon.observe(viewLifecycleOwner) { beacon ->
            textViewBeaconId.text = beacon.id.toString()
            editTextBeaconDescription.setText(beacon.description)
            editTextBeaconTilt.setText(beacon.tilt.toString())
            editTextBeaconDirection.setText(beacon.direction.toString())
        }

        viewModel.sensorEntries.observe(viewLifecycleOwner) { sensorEntries ->
            adapter.updateData(sensorEntries.asReversed())
        }

        // set callbacks for modified text fields
        editTextBeaconDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // do nothing
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                // call the updateBeaconFields method
                viewModel.updateBeacon4Fields(
                    editTextBeaconDescription.text.toString(),
                    editTextBeaconTilt.text.toString().toFloatOrNull(),
                    editTextBeaconDirection.text.toString().toFloatOrNull()
                )
            }
        })


        // Return the view.
        return view
    }
}