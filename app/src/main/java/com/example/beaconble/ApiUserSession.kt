package com.example.beaconble

import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import com.lambdapioneer.argon2kt.Argon2Mode
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileInputStream
import java.time.Instant
import kotlin.random.Random

enum class ApiUserSessionState {
    LOGGED_IN,  // user has logged in successfully
    NOT_LOGGED_IN,  // user logged out
    NEVER_LOGGED_IN,  // user has never logged in

    // errors
    ERROR_BAD_IDENTITY,
    ERROR_BAD_PASSWORD,
    ERROR_BAD_TOKEN,
    CONNECTION_ERROR,
}

class ApiUserSession {
    // members
    var username: String? = null
    var email: String? = null
    var passHash: String? = null
    var passSalt: String? = null
    var lastKnownState = MutableLiveData<ApiUserSessionState>(ApiUserSessionState.NOT_LOGGED_IN)
    var apiService: APIService
    var sharedPrefs: SharedPreferences

    var accessToken: String? = null
    var accessTokenRx: Instant? = null
    var accessTokenValidity: Int? = null

    // constructors
    // default
    constructor(
        username: String,
        email: String,
        passHash: String,
        passSalt: String,
        apiService: APIService,
        sharedPrefs: SharedPreferences
    ) {
        this.username = username
        this.email = email
        this.passHash = passHash
        this.passSalt = passSalt
        this.apiService = apiService
        this.sharedPrefs = sharedPrefs
    }

    // copy
    constructor(user: ApiUserSession) {
        this.username = user.username
        this.email = user.email
        this.passHash = user.passHash
        this.passSalt = user.passSalt
        this.apiService = user.apiService
        this.sharedPrefs = user.sharedPrefs
    }

    // constructor from shared preferences
    constructor(sharedPrefs: SharedPreferences, apiService: APIService) {
        this.username = sharedPrefs.getString("username", null)
        this.email = sharedPrefs.getString("email", null)
        this.passHash = sharedPrefs.getString("passHash", null)
        this.apiService = apiService
        this.lastKnownState.value = sharedPrefs.getString("lastKnownState", "NEVER_LOGGED_IN")
            ?.let { ApiUserSessionState.valueOf(it) } ?: ApiUserSessionState.NEVER_LOGGED_IN
        this.sharedPrefs = sharedPrefs
    }

    // methods
    fun saveToSharedPreferences() {
        with(this.sharedPrefs.edit()) {
            putString("username", username)
            putString("email", email)
            putString("passHash", passHash)
            putString("lastKnownState", lastKnownState.value.toString())
            apply()
        }
    }

    fun logout() {
        with(this.sharedPrefs.edit()) {
            remove("username")
            remove("email")
            remove("passHash")
            remove("lastKnownState")
            apply()
        }
        clear()
        lastKnownState.value = ApiUserSessionState.NOT_LOGGED_IN
        saveToSharedPreferences()
    }

    fun clear() {
        this.username = null
        this.email = null
        this.passHash = null
        this.passSalt = null
        this.accessToken = null
        this.accessTokenRx = null
        this.accessTokenValidity = null
        this.lastKnownState.value = ApiUserSessionState.NOT_LOGGED_IN
    }

    /**
     * Log in a user with the server
     * @param username the username of the user
     * @param passWord the password of the user
     * @return the state of the user session after the login
     *
     * This function will set the username, passHash, and passSalt fields of the user session object,
     * then request the salt for the user from the server, then hashes the password with the salt
     * using Argon2, and sends the login request to the server.
     *
     * If the server responds with a successful login, the function will return LOGGED_IN.
     * If the server responds with an error, the function will return ERROR_BAD_PASSWORD or CONNECTION_ERROR.
     */
    suspend fun login(email: String, passWord: String): ApiUserSessionState {
        this.email = email

        // get salt from server
        try {
            val saltResponse = apiService.getUserSalt(email)
            this.passSalt = saltResponse.passSalt
        } catch (e: HttpException) {
            Log.e("ApiUserSession", "Error getting salt: ${e.message}")
            return ApiUserSessionState.ERROR_BAD_IDENTITY
        } catch (e: Exception) {
            Log.e("ApiUserSession", "Error getting salt: ${e.message}")
            return ApiUserSessionState.CONNECTION_ERROR
        }

        val passWordByteArray = passWord.toByteArray()
        val saltByteArray = this.passSalt!!.toByteArray()

        // hash password with salt, store in .passHash as plaintext
        val argon2Kt = Argon2Kt()
        val hashResult: Argon2KtResult = argon2Kt.hash(
            mode = Argon2Mode.ARGON2_I,
            password = passWordByteArray,
            salt = saltByteArray,
            tCostInIterations = 6,
            mCostInKibibyte = 65536,
        )
        this.passHash = hashResult.encodedOutputAsString()

        // send login request to server
        val loginRequest = LoginRequest(this.email!!, this.passHash!!)
        try {
            val loginResponse = apiService.loginUser(loginRequest)
            this.username = loginResponse.username
            this.accessToken = "Bearer ${loginResponse.access_token}"
            this.accessTokenRx = Instant.now()
            this.accessTokenValidity = loginResponse.validity
            this.lastKnownState.value = ApiUserSessionState.LOGGED_IN
        } catch (e: HttpException) {
            Log.e("ApiUserSession", "HttpException logging in user: ${e.message}")
            this.lastKnownState.value = ApiUserSessionState.ERROR_BAD_PASSWORD
        } catch (e: Exception) {
            Log.e("ApiUserSession", "Exception logging in user: ${e.message}")
            this.lastKnownState.value = ApiUserSessionState.CONNECTION_ERROR
        }
        saveToSharedPreferences()
        return this.lastKnownState.value!!
    }

    /**
     * Register a new user with the server
     * @param username the username of the new user
     * @param email the email of the new user
     * @param passWord the password of the new user
     * @return the state of the user session after the registration
     *
     * This function will set the username, email, passHash, and passSalt fields of the user session
     * object, then send a register request to the server.
     * If the server responds with a successful registration, the function will return REGISTERED.
     * If the server responds with an error, the function will return ERROR_BAD_PASSWORD or CONNECTION_ERROR.
     */
    suspend fun register(username: String, email: String, passWord: String): ApiUserSessionState {
        this.username = username
        this.email = email

        // create a random salt for the user
        var saltByteArray = ByteArray(16) { Random.nextInt().toByte() }

        val passWordByteArray = passWord.toByteArray()

        // hash password with salt, store in passHash
        val argon2Kt = Argon2Kt()
        val hashResult: Argon2KtResult = argon2Kt.hash(
            mode = Argon2Mode.ARGON2_I,
            password = passWordByteArray,
            salt = saltByteArray,
            tCostInIterations = 6,
            mCostInKibibyte = 65536,
        )
        this.passHash = hashResult.encodedOutputAsString()

        // send register request to server
        val registerRequest = RegisterRequest(
            this.username!!,
            this.email!!,
            this.passHash!!,
            Base64.encodeToString(saltByteArray, Base64.DEFAULT)
        )
        try {
            val registerResponse = apiService.registerUser(registerRequest)
            this.lastKnownState.value = ApiUserSessionState.LOGGED_IN
        } catch (e: HttpException) {
            Log.e("ApiUserSession", "HttpException registering user: ${e.message}")
            this.lastKnownState.value = ApiUserSessionState.ERROR_BAD_PASSWORD
        } catch (e: Exception) {
            Log.e("ApiUserSession", "Exception registering user: ${e.message}")
            this.lastKnownState.value = ApiUserSessionState.CONNECTION_ERROR
        }
        saveToSharedPreferences()
        return this.lastKnownState.value!!
    }

    /**
     * Upload the session file to the server.
     * Checks the access token and if it is expired, logs in again.
     * The uploaded file is an stream of bytes, so it is not necessary to store it in the device.
     * @param file The file to upload.
     * @return The state of the user session after the upload.
     * If the server responds with a successful upload, the function will return LOGGED_IN.
     * If the server responds with an error, the function will return CONNECTION_ERROR.
     * If the access token is expired, the function will return ERROR_BAD_TOKEN.
     */
    suspend fun upload(file: File): ApiUserSessionState {
        if (accessToken == null
            || accessTokenRx == null
            || accessTokenValidity == null
            || accessTokenRx!!.plusSeconds(accessTokenValidity!!.toLong()) < Instant.now()
        ) {
            // access token is expired, log in again
            // send login request to server
            val loginRequest = LoginRequest(this.email!!, this.passHash!!)
            try {
                val loginResponse = apiService.loginUser(loginRequest)
                this.username = loginResponse.username
                this.accessToken = "Bearer ${loginResponse.access_token}"
                this.accessTokenRx = Instant.now()
                this.accessTokenValidity = loginResponse.validity
                this.lastKnownState.postValue(ApiUserSessionState.LOGGED_IN)
            } catch (e: HttpException) {
                Log.e("ApiUserSession", "HttpException logging in user: ${e.message}")
                this.lastKnownState.postValue(ApiUserSessionState.ERROR_BAD_PASSWORD)
            } catch (e: Exception) {
                Log.e("ApiUserSession", "Exception logging in user: ${e.message}")
                this.lastKnownState.postValue(ApiUserSessionState.CONNECTION_ERROR)
            }
        }

        val filePart = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = file.asRequestBody("plain/text".toMediaTypeOrNull())
        )
        try {
            apiService.uploadBeacons(this.accessToken!!, filePart)
        } catch (e: HttpException) {
            Log.e("ApiUserSession", "HttpException uploading file: ${e.message}")
            Log.e("ApiUserSession", "Response content: ${e.response()?.errorBody()?.string()}")
            return ApiUserSessionState.CONNECTION_ERROR
        } catch (e: Exception) {
            Log.e("ApiUserSession", "Exception uploading file: ${e.message}")
            return ApiUserSessionState.CONNECTION_ERROR
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

    data class LoginRequest(val email: String, val passHash: String)

    fun loginRequest() = LoginRequest(this.email!!, this.passHash!!)

    class LoginResponse {
        var username: String? = null
        var access_token: String? = null
        var validity: Int? = null
    }

    data class RegisterRequest(
        val username: String, val email: String, val passHash: String, val passSalt: String
    )

    fun registerRequest() =
        RegisterRequest(this.username!!, this.email!!, this.passHash!!, this.passSalt!!)
}
