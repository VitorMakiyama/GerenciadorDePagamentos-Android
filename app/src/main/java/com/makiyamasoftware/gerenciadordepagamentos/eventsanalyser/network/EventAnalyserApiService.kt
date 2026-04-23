package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.makiyamasoftware.gerenciadordepagamentos.BuildConfig
import com.makiyamasoftware.gerenciadordepagamentos.settings.SettingsRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import kotlin.time.Instant

private const val BASE_URL = BuildConfig.EventsServiceBaseURL // Comes from local.properties

@Serializable
data class EventRequest(
    @SerialName("subject_id")
    val subjectID: Int,
    @SerialName("occurrences")
    val occurrences: Int,
    @SerialName("insert_ts") // Should be in RFC3339,such as '2011-12-03T10:15:30+01:00'
    val insertTS: String,
)

@Serializable
data class EventResponse(
    val id: Int,
    @SerialName("subject_id")
    val subjectID: Int,
    val occurrences: Int,
    @SerialName("insert_ts") // Comes in RFC3339, on backend's local time
    val insertTS: Instant,
    @SerialName("last_update")
    val lastUpdate: Instant,        // Comes in RFC3339, on backend's local time
)


interface EventAnalyserApiService {
    @GET("ping")
    suspend fun ping(): String

    @POST("events")
    suspend fun postEvent(@Body eventRequest: EventRequest): EventResponse

    @PUT("events")
    suspend fun putEvent(@Query("id") id: Int, @Body eventRequest: EventRequest): EventResponse
}

object EventAnalyserApi {
    fun getService(settingsRepository: SettingsRepository): EventAnalyserApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseURLInterceptor(settingsRepository))
            .build()

        val retrofit = Retrofit.Builder()
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(BASE_URL)
            .client(client)
            .build()
        val retrofitService: EventAnalyserApiService by lazy {
            retrofit.create(EventAnalyserApiService::class.java)
        }
        return retrofitService
    }
    fun getBaseURLConst(): String = BASE_URL
}