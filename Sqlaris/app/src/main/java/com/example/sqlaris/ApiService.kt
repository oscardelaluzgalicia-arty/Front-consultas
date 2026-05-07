package com.example.sqlaris

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("verify")
    suspend fun verifyConnection(@Body request: ConnectionRequest): Response<ConnectionResponse>

    companion object {
        private const val BASE_URL = "https://back-consultas-4qyssf05d-oscardelaluzgalicia-artys-projects.vercel.app/"

        fun create(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
