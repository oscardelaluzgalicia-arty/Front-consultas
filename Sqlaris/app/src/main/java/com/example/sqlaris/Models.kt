package com.example.sqlaris

import com.google.gson.annotations.SerializedName

data class ConnectionRequest(
    val dbHost: String,
    val dbUser: String,
    val dbPass: String,
    val dbName: String
)

data class ConnectionResponse(
    @SerializedName("CONECCION")
    val connection: String,
    @SerializedName("TOKEN")
    val token: String
)
