package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.makiyamasoftware.gerenciadordepagamentos.BuildConfig
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.reports.EventsReports
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.reports.EventsReportsData
import com.makiyamasoftware.gerenciadordepagamentos.settings.SettingsRepository
import com.squareup.moshi.JsonClass
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

@JsonClass(generateAdapter = true)
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

    // Reports
    @GET("reports/types")
    suspend fun getReportTypes(): Array<String>

    @GET("reports")
    suspend fun getReportData(@Query("type") type: String): EventsReportsData
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
                .withSubtype(EventsReportsData.BasicReportData::class.java, EventsReports.BASIC.name)
//                .withSubtype(EventsReportsData.ChartReportData::class.java, EventsReports.CHART.name)
        )
        .addLast(KotlinJsonAdapterFactory())
        .build()


    fun getService(settingsRepository: SettingsRepository): EventAnalyserApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseURLInterceptor(settingsRepository))
            .build()

        val retrofit = Retrofit.Builder()
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
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