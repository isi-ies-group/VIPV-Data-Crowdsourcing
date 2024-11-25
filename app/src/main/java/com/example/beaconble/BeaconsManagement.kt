package com.example.beaconble

import android.provider.ContactsContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.altbeacon.beacon.Identifier
import java.time.Instant

/**
 * In-beacon data entry with the data that changes through a measurement session.
 * Holds the read data (16-bit 2's complement) from the analog channel of the
 * beacon, location and timestamp.
 */
data class SensorEntry(
    val data: Short,
    val latitude: Float,
    val longitude: Float,
    val timestamp: Instant,
)

/**
 * Represents a beacon with its identifier and its data. This one is different from the one in the
 * AltBeacon library: this one holds exclusively the data expected to be used in conjunction with
 * the configured beacons and the API service.
 * @param id The identifier of the beacon.
 * @property sensorData The data received from the beacon.
 */
class Beacon(val id: Identifier) {
    /**
     * The data received from the analog channel of the beacon. From the NanoBeacon Config Tool User Guide EN.pdf:
     * "The ADC data is of 16-bit in 2’s complement format."
     * Set initial capacity of 360.
     */
    var sensorData: ArrayList<SensorEntry> = ArrayList<SensorEntry>(360)
}


/**
 * Handles the addition of SensorEntries to the beacons. Creates new instances of Beacon if the
 * identifier is not found in the list.
 */
class BeaconCollectionDispatcher {
    private val _beacons: MutableLiveData<ArrayList<Beacon>> = MutableLiveData<ArrayList<Beacon>>()
    val beacons: LiveData<ArrayList<Beacon>> = _beacons
    /**
     * Adds a SensorEntry to the beacon with the given identifier. If the beacon is not found, it
     * creates a new instance of Beacon and adds it to the list.
     * @param id The identifier of the beacon.
     * @param data The data to be added to the beacon.
     * @param latitude The latitude of the beacon.
     * @param longitude The longitude of the beacon.
     * @param timestamp The timestamp of the data.
     */
    fun addSensorEntry(id: Identifier, data: Short, latitude: Float, longitude: Float, timestamp: Instant) {
        val beacon = _beacons.value!!.find { it.id == id }
        if (beacon != null) {
            beacon.sensorData.add(SensorEntry(data, latitude, longitude, timestamp))
        } else {
            val newBeacon = Beacon(id)
            newBeacon.sensorData.add(SensorEntry(data, latitude, longitude, timestamp))
            _beacons.value!!.add(newBeacon)
        }
    }

    /**
     * Returns the list of beacons.
     * @return The list of beacons.
     */
    fun getBeacons(): ArrayList<Beacon> {
        return _beacons.value!!
    }
}