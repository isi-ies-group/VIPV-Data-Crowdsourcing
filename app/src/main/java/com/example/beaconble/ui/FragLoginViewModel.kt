package com.example.beaconble.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beaconble.ApiUserSessionState
import com.example.beaconble.BeaconReferenceApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException

class FragLoginViewModel : ViewModel() {
    private val beaconReferenceApplication = BeaconReferenceApplication.instance

    // variables for the login form persistence between destroy and create
    var email: MutableLiveData<String> = MutableLiveData("")
    var password: MutableLiveData<String> = MutableLiveData("")

    // mutable status for the login process, to report errors to the user
    val loginStatus = MutableLiveData<ApiUserSessionState>()
    // mutable status for whether login button should be enabled
    val loginButtonEnabled = MutableLiveData<Boolean>()

    init {
        // observe the email and password fields for changes
        email.observeForever { onCredentialsChanged() }
        password.observeForever { onCredentialsChanged() }
    }

    fun onCredentialsChanged() {
        // enable the login button if both email and password are not empty
        val validEmail = email.value!!.isNotEmpty()
        val validPassword = password.value!!.isNotEmpty()
        loginButtonEnabled.postValue(validEmail && validPassword)
    }

    fun doLogin() = viewModelScope.launch {
        // call login method from the application and return the result
        // if successful, the user will be redirected to the main activity
        // else, update the loginStatus variable with the error message
        val result = beaconReferenceApplication.apiUserSession.login(email.value!!, password.value!!)
        loginStatus.postValue(result)
    }
}