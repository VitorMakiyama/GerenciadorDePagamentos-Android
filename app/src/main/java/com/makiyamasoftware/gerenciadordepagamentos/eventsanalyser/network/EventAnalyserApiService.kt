package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET

private const val BASE_URL =
    "https://android-kotlin-fun-mars-server.appspot.com" // Change to my domain URL

private val retrofit = Retrofit.Builder()
    .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
    .baseUrl(BASE_URL)
    .build()

interface EventAnalyserApiService {
    @GET("photos")
    suspend fun getPhotos(): List<MarsPhoto>

    @Serializable
    data class MarsPhoto(
        val id: String,
        @SerialName(value = "img_src") val imgSrcUrl: String
    )
}

object EventAnalyserApi {
    val retrofitService: EventAnalyserApiService by lazy {
        retrofit.create(EventAnalyserApiService::class.java)
    }
}