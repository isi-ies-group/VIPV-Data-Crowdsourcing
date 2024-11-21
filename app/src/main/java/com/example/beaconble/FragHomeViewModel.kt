package com.example.beaconble

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.beaconble.BeaconReferenceApplication  // This is a singleton class, access it with BeaconReferenceApplication.instance

class FragHomeViewModel() : ViewModel() {
    private val _exampleData = MutableLiveData<Array<String>>(arrayOf("Beacon 1", "Beacon 2", "Beacon 3"))
    val exampleData: LiveData<Array<String>> get() = _exampleData
    private val _counter = MutableLiveData<Int>(0)
    val counter: LiveData<Int> get() = _counter

    private val beaconReferenceApplication = BeaconReferenceApplication.instance

    fun updateData(newData: Array<String>) {
        _exampleData.value = newData
    }

    fun incrementCounter() {
        _counter.value = (_counter.value ?: 0) + 1
        _exampleData.value = _exampleData.value?.plus("Beacon ${_counter.value}")
    }
}
