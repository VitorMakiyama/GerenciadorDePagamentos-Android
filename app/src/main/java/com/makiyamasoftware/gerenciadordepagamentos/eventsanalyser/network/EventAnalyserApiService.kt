package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.makiyamasoftware.gerenciadordepagamentos.BuildConfig
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.reports.EventsReportType
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.reports.EventsReportsData
import com.makiyamasoftware.gerenciadordepagamentos.settings.SettingsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
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

@Serializable
data class SubjectResponse(
    val id: Int,
    val name: String,
    val description: String
)

interface EventAnalyserApiService {
    @GET("ping")
    suspend fun ping(): String

    @POST("events")
    suspend fun postEvent(@Body eventRequest: EventRequest): EventResponse

    @PUT("events")
    suspend fun putEvent(@Query("id") id: Int, @Body eventRequest: EventRequest): EventResponse

    @GET("subjects")
    suspend fun getAllSubjects(): List<SubjectResponse>
}

interface EventsReportsApiService {
    // Reports
    @GET("reports/types")
    suspend fun getReportTypes(): List<String>

    @GET("reports")
    suspend fun getReportData(
        @Query("type") type: String,
        @Query("subject_id") subjectID: Int
    ): EventsReportsData
}

object EventAnalyserApi {
    // Moshi helps us parse JSON into custom Kotlin classes based on a key inside the JSON
    val moshi: Moshi = Moshi.Builder()
        .add(
            // 1. Define base class
            PolymorphicJsonAdapterFactory.of(
                EventsReportsData::class.java,
                "type"
            ) // "type" is the key inside JSON
                // 2. Maps JSON value for each corresponding Kotlin class
                .withSubtype(
                    EventsReportsData.BasicReportData::class.java,
                    EventsReportType.BASIC.name
                )
                .withSubtype(
                    EventsReportsData.ChartReportData::class.java,
                    EventsReportType.CHART.name
                )
        )
        .addLast(KotlinJsonAdapterFactory())
        .build()
    lateinit var client: OkHttpClient
    lateinit var eventAnalyserAPI: EventAnalyserApiService
    lateinit var eventsReportsAPI: EventsReportsApiService

    fun getEventAnalyserService(settingsRepository: SettingsRepository): EventAnalyserApiService {
        if (!this::client.isInitialized) {
            // if is not initialized, initialize it!
            client = OkHttpClient.Builder()
                .addInterceptor(BaseURLInterceptor(settingsRepository))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
        }

        if (!this::eventAnalyserAPI.isInitialized) {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                .baseUrl(BASE_URL)
                .client(client)
                .build()
            val retrofitService: EventAnalyserApiService by lazy {
                retrofit.create(EventAnalyserApiService::class.java)
            }
            eventAnalyserAPI = retrofitService
            return eventAnalyserAPI
        }
        return eventAnalyserAPI
    }

    fun getEventsReportsService(settingsRepository: SettingsRepository): EventsReportsApiService {
        if (!this::client.isInitialized) {
            // if is not initialized, initialize it!
            client = OkHttpClient.Builder()
                .addInterceptor(BaseURLInterceptor(settingsRepository))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
        }

        if (!this::eventsReportsAPI.isInitialized) {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl(BASE_URL)
                .client(client)
                .build()
            val retrofitService: EventsReportsApiService by lazy {
                retrofit.create(EventsReportsApiService::class.java)
            }
            eventsReportsAPI = retrofitService
            return eventsReportsAPI
        }
        return eventsReportsAPI
    }

    fun getBaseURLConst(): String = BASE_URL
}