// Project Update: Reporting Visual System Integration
package com.example.sqlaris

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ApiService {
    @POST("verify")
    suspend fun verifyConnection(@Body request: ConnectionRequest): Response<ConnectionResponse>

    @POST("tables")
    suspend fun getTables(@Header("Authorization") token: String): Response<TablesResponse>

    @POST("describe")
    suspend fun describeTables(
        @Header("Authorization") token: String,
        @Body request: DescribeRequest
    ): Response<DescribeResponse>

    @POST("query")
    suspend fun executeQuery(
        @Header("Authorization") token: String,
        @Body request: QueryRequest
    ): Response<QueryJsonResponse>

    @Streaming
    @POST("query")
    suspend fun downloadExcel(
        @Header("Authorization") token: String,
        @Body request: QueryRequest
    ): Response<ResponseBody>

    companion object {
        private const val BASE_URL = "https://back-consultas-gwa8duqp9-oscardelaluzgalicia-artys-projects.vercel.app/"

        fun create(): ApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
