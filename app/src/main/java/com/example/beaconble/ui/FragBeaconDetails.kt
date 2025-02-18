package com.example.beaconble.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.beaconble.AppMain
import com.example.beaconble.BeaconSimplifiedStatus
import com.example.beaconble.R
import com.example.beaconble.createPositionMap
import com.example.beaconble.databinding.FragmentBeaconDetailsBinding

class FragBeaconDetails : Fragment() {
    private val viewModel: FragBeaconDetailsViewModel by viewModels()
    private var _binding: FragmentBeaconDetailsBinding? = null
    private val binding get() = _binding!!

    private val descriptionTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { viewModel.beacon.value?.setDescription(s.toString()) }
    }
    private val tiltTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { viewModel.beacon.value?.setTilt(s.toString().toFloatOrNull()) }
    }
    private val directionTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { viewModel.beacon.value?.setDirection(s.toString().toFloatOrNull()) }
    }
    private val positionSpinnerOnItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val localizedItemText = parent.getItemAtPosition(position).toString()
            val unlocalizedItemValue = positionMap[localizedItemText]
            viewModel.beacon.value?.setPosition(unlocalizedItemValue ?: "")
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("beaconId")?.let { viewModel.loadBeacon(it) }
        activity?.title = getString(R.string.beacon_details)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using view binding
        _binding = FragmentBeaconDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
        setupSpinner()
        setupListView()
    }

    private fun setupObservers() {
        viewModel.beacon.observe(viewLifecycleOwner) { beacon ->
            if (beacon != null) {
                updateTextFields()
            } else {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }

        viewModel.sensorEntries.observe(viewLifecycleOwner) { sensorEntries ->
            (binding.listViewSensorEntries.adapter as? ListAdapterSensorEntries)?.updateData(
                sensorEntries.reversed()
            )
        }

        viewModel.status.observe(viewLifecycleOwner) { status ->
            val (drawableResource, tintColor, contentDescription) = when (status) {
                BeaconSimplifiedStatus.OFFLINE -> Triple(
                    R.drawable.bluetooth_off,
                    R.color.warning_red,
                    getString(R.string.beacon_detail_out_of_range_error)
                )

                BeaconSimplifiedStatus.INFO_MISSING -> Triple(
                    R.drawable.warning,
                    R.color.warning_orange,
                    getString(R.string.beacon_detail_info_error)
                )

                else /* OK */ -> Triple(
                    R.drawable.check, R.color.green_ok, getString(R.string.beacon_detail_ok)
                )
            }
            binding.tvBeaconStatusMessage.setCompoundDrawablesWithIntrinsicBounds(
                drawableResource, 0, 0, 0
            )
            TextViewCompat.setCompoundDrawableTintList(
                binding.tvBeaconStatusMessage,
                ContextCompat.getColorStateList(requireContext(), tintColor)
            )
            binding.tvBeaconStatusMessage.text = contentDescription
        }
    }

    private fun setupListeners() {
        binding.imBtnDeleteBeacon.setOnClickListener {
            AlertDialog.Builder(requireContext()).setTitle(getString(R.string.empty_all_data))
                .setMessage(getString(R.string.empty_all_data_confirmation))
                .setPositiveButton(getString(R.string.yes)) { _, _ -> viewModel.deleteBeacon() }
                .setNegativeButton(getString(R.string.no), null).show()
        }

        /** note listeners for editTexts and spinner are added in updateTextFields() */
    }

    private fun setupSpinner() {
        val positions = positionMap.keys.toList()
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, positions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPosition.adapter = adapter
        binding.spinnerPosition.setSelection(0) // Set default item as selected
    }

    private fun setupListView() {
        val adapter = ListAdapterSensorEntries(requireContext(), ArrayList())
        binding.listViewSensorEntries.adapter = adapter
    }

    private fun updateTextFields() {
        val beacon = viewModel.beacon.value
        if (beacon != null) {
            // Remove listeners to avoid triggering the updateBeaconInfo method
            binding.editTextDescription.removeTextChangedListener(descriptionTextWatcher)
            binding.editTextTilt.removeTextChangedListener(tiltTextWatcher)
            binding.editTextDirection.removeTextChangedListener(directionTextWatcher)
            binding.spinnerPosition.onItemSelectedListener = null

            binding.tvBeaconIdentifier.text = beacon.id.toString()
            binding.editTextDescription.setText(beacon.descriptionValue)
            val tilt = beacon.tiltValue?.toInt()?.toString()
            binding.editTextTilt.setText(tilt ?: "")
            val direction = beacon.directionValue?.toInt()?.toString()
            binding.editTextDirection.setText(direction ?: "")

            val positionKey = positionMap.entries.find { it.value == beacon.positionValue }?.key
            val positionIndex = positionMap.keys.indexOf(positionKey)
            if (positionIndex >= 0) {
                binding.spinnerPosition.setSelection(positionIndex)
            }

            // Add listeners back
            // Reattach TextWatchers and OnItemSelectedListener
            binding.editTextDescription.addTextChangedListener(descriptionTextWatcher)
            binding.editTextTilt.addTextChangedListener(tiltTextWatcher)
            binding.editTextDirection.addTextChangedListener(directionTextWatcher)
            binding.spinnerPosition.onItemSelectedListener = positionSpinnerOnItemSelectedListener
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val positionMap: Map<String, String> = createPositionMap(AppMain.instance)
    }
}
