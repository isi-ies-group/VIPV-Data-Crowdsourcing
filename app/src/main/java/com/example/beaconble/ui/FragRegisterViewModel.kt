package com.example.beaconble.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beaconble.ApiUserSessionState
import com.example.beaconble.BeaconReferenceApplication
import kotlinx.coroutines.launch

class FragRegisterViewModel : ViewModel() {
    private val beaconReferenceApplication = BeaconReferenceApplication.instance

    // variables for the login form persistence between destroy and create
    var username: MutableLiveData<String> = MutableLiveData("")
    var email: MutableLiveData<String> = MutableLiveData("")
    var password: MutableLiveData<String> = MutableLiveData("")
    var password2: MutableLiveData<String> = MutableLiveData("")

    // mutable status for the login process, to report errors to the user
    val registerStatus = MutableLiveData<ApiUserSessionState>()
    // mutable status for whether login button should be enabled
    val registerButtonEnabled = MutableLiveData<Boolean>()

    init {
        // observe the email and password fields for changes
        username.observeForever { onCredentialsChanged() }
        email.observeForever { onCredentialsChanged() }
        password.observeForever { onCredentialsChanged() }
        password2.observeForever { onCredentialsChanged() }
    }

    fun onCredentialsChanged() {
        // enable the login button if both email and password are not empty
        val validUsername = username.value!!.isNotEmpty()
        val validEmail = email.value!!.isNotEmpty()
        val validPassword = password.value!!.isNotEmpty()
        val samePasswords = password.value == password2.value
        registerButtonEnabled.postValue(validUsername && validEmail && validPassword && samePasswords)
    }

    fun doRegister() = viewModelScope.launch {
        // call login method from the application and return the result
        // if successful, the user will be redirected to the main activity
        // else, update the loginStatus variable with the error message
        val result = beaconReferenceApplication.apiUserSession.register(
            username.value!!,
            email.value!!,
            password.value!!
        )
        registerStatus.postValue(result)
    }
}