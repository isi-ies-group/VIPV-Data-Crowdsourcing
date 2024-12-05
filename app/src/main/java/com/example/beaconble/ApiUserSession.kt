package com.example.beaconble

class ApiUserSession: Cloneable {
    // members
    var username: String? = null
    var email: String? = null
    var passHash: String? = null
    var passSalt: String? = null

    // constructors
    // default
    constructor(username: String, email: String, passHash: String, passSalt: String) {
        this.username = username
        this.email = email
        this.passHash = passHash
        this.passSalt = passSalt
    }

    // copy
    constructor(user: ApiUserSession) {
        this.username = user.username
        this.email = user.email
        this.passHash = user.passHash
        this.passSalt = user.passSalt
    }

    // sub classes and factories from root class
    class SaltResponse {
        var passSalt: String? = null
    }

    class LoginRequest {
        var username: String? = null
        var passHash: String? = null

        constructor(user: ApiUserSession) {
            this.username = user.username
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
