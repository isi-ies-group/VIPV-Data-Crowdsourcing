package com.example.beaconble.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FragLoginViewModel : ViewModel() {
    // variables for the login form
    var username: MutableLiveData<String> = MutableLiveData("")
    var password: MutableLiveData<String> = MutableLiveData("")

    fun doLogin() {

    }
}