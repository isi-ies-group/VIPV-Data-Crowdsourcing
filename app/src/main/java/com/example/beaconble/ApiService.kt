package com.example.beaconble

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.GET
import retrofit2.http.POST


const val APIv1_base = "api/v1/"

interface APIService {
    @GET(APIv1_base + "salt")
    @Headers("Content-type: application/json")
    suspend fun getUserSalt(
        @Header("username") username: String,
    ): ApiUserSession.SaltResponse

    @POST(APIv1_base + "register")
    @Headers("Content-type: application/json")
    suspend fun registerUser(
        @Body request: ApiUserSession.RegisterRequest,
    ): ResponseBody

    @POST(APIv1_base + "login")
    @Headers("Content-type: application/json")
    suspend fun loginUser(
        @Body request: ApiUserSession.LoginRequest,
    ): ApiUserSession.LoginResponse
}


