package com.example.beaconble

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.beaconble.io.SessionWriter
import org.altbeacon.beacon.Identifier
import java.io.File
import java.time.Instant
import kotlin.text.clear

/**
 * Singleton object to hold the logging session and some other metadata.
 *
 * Handles the addition of SensorEntries to the beacons. Creates new instances of Beacon if the
 * identifier is not found in the list.
 */
object LoggingSession {
    /**
     * Prefix for the session files.
     */
    private const val SESSION_FILE_PREFIX = "VIPV_"

    /**
     * Extension for the session files.
     */
    private const val SESSION_FILE_EXTENSION = "txt"

    /**
     * The beacons detected during the session.
     */
    private val beaconsInternal: MutableLiveData<ArrayList<BeaconSimplified>> =
        MutableLiveData<ArrayList<BeaconSimplified>>(ArrayList<BeaconSimplified>())
    val beacons: LiveData<ArrayList<BeaconSimplified>> = beaconsInternal

    /**
     * Start and stop instants of the session.
     */
    var startInstant: Instant? = null
    var stopInstant: Instant? = null

    /**
     * File to store data temporarily.
     */
    var bodyFile: File? = null

    /**
     * Cache directory to store the session files.
     */
    var cacheDir: File? = null

    /**
     * Initialize the singleton with the cache directory.
     */
    fun init(cacheDir: File) {
        this.cacheDir = cacheDir
    }

    /**
     * Adds a SensorEntry to the beacon with the given identifier. If the beacon is not found, it
     * creates a new instance of Beacon and adds it to the list.
     * @param id The identifier of the beacon.
     * @param data The data to be added to the beacon.
     * @param latitude The latitude of the beacon.
     * @param longitude The longitude of the beacon.
     * @param timestamp The timestamp of the data.
     */
    fun addSensorEntry(
        id: Identifier,
        data: Short,
        latitude: Float,
        longitude: Float,
        timestamp: Instant
    ) {
        val beacon = beaconsInternal.value?.find { it.id == id }
        if (beacon != null) {
            beacon.sensorData.value?.add(SensorEntry(data, latitude, longitude, timestamp))
            beacon.sensorData.notifyObservers()
            beaconsInternal.notifyObservers()
        } else {
            val newBeacon = BeaconSimplified(id)
            newBeacon.sensorData.value?.add(SensorEntry(data, latitude, longitude, timestamp))
            beaconsInternal.value!!.add(newBeacon)
            beaconsInternal.notifyObservers()
        }
    }

    /**
     * Returns the list of beacons.
     * @return The list of beacons.
     */
    fun getBeacons(): ArrayList<BeaconSimplified> {
        return beaconsInternal.value!!
    }

    /**
     * Returns the beacon with the given identifier.
     * @param id The identifier of the beacon.
     * @return The beacon with the given identifier.
     */
    fun getBeacon(id: Identifier): BeaconSimplified? {
        return beaconsInternal.value?.find { it.id == id }
    }

    /**
     * Removes all the beacons from the list.
     */
    fun emptyAll() {
        beaconsInternal.value?.clear()
        beaconsInternal.notifyObservers()
    }

    /**
     * Removes the beacon with the given identifier from the list.
     * @param id The identifier of the beacon.
     */
    fun removeBeacon(id: Identifier) {
        beaconsInternal.value?.removeIf { it.id == id }
        beaconsInternal.notifyObservers()
    }

    fun startFreshSession() {
        clear()
        startInstant = Instant.now()
    }

    fun clear() {
        beacons.value?.clear()
        startInstant = null
        stopInstant = null
    }

    fun clearBeaconsData() {
        beacons.value?.forEach { it.sensorData.value?.clear() }
    }

    /**
     * Save the data to temporary files.
     * Allows updating and merging them later. This is expected to be called when too much data is
     * in memory.
     */
    fun freeDataTemporarily() {
        if (bodyFile == null) {
            bodyFile = File.createTempFile("VIPV_body_", ".txt", cacheDir)
        }
        // append the latest data to the temporary file
        bodyFile!!.outputStream().writer(Charsets.UTF_8).use {
            SessionWriter.V1.appendCsvBody(it, beacons.value!!)
            it.close()
        }
        // clear the data from the beacons to free memory
        beacons.value?.forEach { it.sensorData.value?.clear() }
    }

    /**
     * Conclude the session and save it to a file.
     * Gets the data from temporary dumps and the current readings.
     * @return The file with the session data.
     */
    fun saveSession(): File {
        stopInstant = Instant.now()
        var outFile = File(cacheDir, "${SESSION_FILE_PREFIX}${startInstant}-${stopInstant}.${SESSION_FILE_EXTENSION}")

        outFile.outputStream().writer(Charsets.UTF_8).use {
            // write the header
            SessionWriter.V1.createJSONHeader(it, beacons.value!!, startInstant!!, stopInstant!!)
            it.write("\n\n")  // separate the header from the body
            // write the header
            it.write("beacon_index,timestamp,data,latitude,longitude\n")
            // write the body
            if (bodyFile != null) {
                // concat body file if it was saved previously
                bodyFile!!.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.lines().forEach { line -> it.write(line + "\n") }
                    reader.close()
                }
            }
            // append the latest data to the file
            SessionWriter.V1.appendCsvBody(it, beacons.value!!)
            it.close()
        }

        // return the file to the caller
        return outFile
    }

    /**
     * Ends the current session by saving it to the cache dir and clearing the beacons data.
     */
    fun concludeSession() {
        saveSession()
        clearBeaconsData()
    }
}
