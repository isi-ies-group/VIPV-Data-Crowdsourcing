package com.example.beaconble

import android.content.SharedPreferences
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import com.lambdapioneer.argon2kt.Argon2Mode
import java.time.Instant

enum class ApiUserSessionState {
    LOGGED_IN,
    NOT_LOGGED_IN,
    REGISTERED,
    ERROR_BAD_IDENTITY,
    ERROR_BAD_PASSWORD,
}

class ApiUserSession {
    // members
    var username: String? = null
    var email: String? = null
    var passHash: String? = null
    var passSalt: String? = null
    var lastKnownState: ApiUserSessionState = ApiUserSessionState.NOT_LOGGED_IN
    var apiService: APIService

    var access_token: String? = null
    var access_token_received: Instant? = null

    // constructors
    // default
    constructor(username: String, email: String, passHash: String, passSalt: String, apiService: APIService) {
        this.username = username
        this.email = email
        this.passHash = passHash
        this.passSalt = passSalt
        this.apiService = apiService
    }

    // copy
    constructor(user: ApiUserSession) {
        this.username = user.username
        this.email = user.email
        this.passHash = user.passHash
        this.passSalt = user.passSalt
        this.apiService = user.apiService
    }

    // constructor from shared preferences
    constructor(sharedPrefs: SharedPreferences, apiService: APIService) {
        this.username = sharedPrefs.getString("username", null)
        this.email = sharedPrefs.getString("email", null)
        this.passHash = sharedPrefs.getString("passHash", null)
        this.passSalt = sharedPrefs.getString("passSalt", null)
        this.apiService = apiService
        this.lastKnownState = sharedPrefs.getString("lastKnownState", "NOT_LOGGED_IN")?.let { ApiUserSessionState.valueOf(it) } ?: ApiUserSessionState.NOT_LOGGED_IN
    }

    // methods
    fun saveToSharedPreferences(sharedPrefs: SharedPreferences) {
        with(sharedPrefs.edit()) {
            putString("username", username)
            putString("email", email)
            putString("passHash", passHash)
            putString("passSalt", passSalt)
            apply()
        }
    }

    fun clearThisAndSharedPreferences(sharedPrefs: SharedPreferences) {
        with(sharedPrefs.edit()) {
            remove("username")
            remove("email")
            remove("passHash")
            remove("passSalt")
            apply()
        }
        clear()
    }

    fun clear() {
        this.username = null
        this.email = null
        this.passHash = null
        this.passSalt = null
    }

    suspend fun login(username: String, passWord: String): ApiUserSessionState {
        this.username = username

        // get salt from server
        try {
            val saltResponse = apiService.getUserSalt(username)
            this.passSalt = saltResponse.passSalt
        } catch (e: Exception) {
            return ApiUserSessionState.ERROR_BAD_IDENTITY
        }

        val passWordByteArray = passWord.toByteArray()
        val saltByteArray = this.passSalt!!.toByteArray()

        // hash password with salt, store in passHash
        val argon2Kt = Argon2Kt()
        val hashResult: Argon2KtResult = argon2Kt.hash(
            mode = Argon2Mode.ARGON2_I,
            password = passWordByteArray,
            salt = saltByteArray,
            tCostInIterations = 6,
            mCostInKibibyte = 65536,
        )
        this.passHash = hashResult.rawHashAsHexadecimal()

        // send login request to server
        try {
            val loginResponse = apiService.loginUser(this.loginRequest())
            access_token = loginResponse.access_token
            access_token_received = Instant.now()
        } catch (e: Exception) {
            return ApiUserSessionState.ERROR_BAD_PASSWORD
        }
        return ApiUserSessionState.LOGGED_IN
    }

    fun isProbablyValid(): Boolean {
        return username != null && email != null && passHash != null && passSalt != null
    }

    // sub classes and factories from root class
    class SaltResponse {
        var passSalt: String? = null
    }

    class LoginRequest {
        var email: String? = null
        var passHash: String? = null

        constructor(user: ApiUserSession) {
            this.email = user.email
            this.passHash = user.passHash
        }
    }
    fun loginRequest() = LoginRequest(this)

    class LoginResponse {
        var access_token: String? = null
    }

    class RegisterRequest {
        var username: String? = null
        var email: String? = null
        var passHash: String? = null
        var passSalt: String? = null

        constructor(user: ApiUserSession) {
            this.username = user.username
            this.email = user.email
            this.passHash = user.passHash
            this.passSalt = user.passSalt
        }
    }
    fun registerRequest() = RegisterRequest(this)
}
