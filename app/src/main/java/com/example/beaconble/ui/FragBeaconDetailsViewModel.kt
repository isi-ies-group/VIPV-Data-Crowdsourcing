package com.example.beaconble.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.beaconble.BeaconCollectionDispatcher
import com.example.beaconble.BeaconReferenceApplication
import com.example.beaconble.BeaconSimplified
import com.example.beaconble.SensorEntry
import org.altbeacon.beacon.Identifier
import java.util.ArrayList

class FragBeaconDetailsViewModel : ViewModel() {
    private var beaconId: Identifier? = null

    private val beaconReferenceApplication = BeaconReferenceApplication.instance

    private var _beacon = MutableLiveData<BeaconSimplified>()
    val beacon: MutableLiveData<BeaconSimplified> get() = _beacon

    var sensorEntries: MutableLiveData<ArrayList<SensorEntry>> =
        MutableLiveData<ArrayList<SensorEntry>>()

    fun getBeaconId(): Identifier? {
        return beaconId
    }

    /**
     * Sets the beacon with the given identifier.
     * @param id The identifier of the beacon.
     */
    fun setBeaconId(id: Identifier) {
        beaconId = id
        _beacon.value = beaconReferenceApplication.beaconManagementCollection.getBeacon(id)
        sensorEntries = _beacon.value?.sensorData!!
    }

    /**
     * Loads the beacon with the given identifier.
     */
    fun loadBeacon(id: String?) {
        if (id != null) {
            setBeaconId(Identifier.parse(id))
        }
    }

    /**
     * Updates the beacon fields with the given values.
     */
    fun updateBeaconFields(description: String, tilt: Float?, direction: Float?) {
        if (_beacon.value != null) {
            _beacon.value?.description = description
            _beacon.value?.tilt = tilt
            _beacon.value?.direction = direction
        }
    }
}
