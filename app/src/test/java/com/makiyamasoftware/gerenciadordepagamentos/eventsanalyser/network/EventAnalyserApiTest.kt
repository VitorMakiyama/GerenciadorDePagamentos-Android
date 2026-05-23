package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network

import com.google.common.truth.Truth.assertThat
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.reports.EventsReportType
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.reports.EventsReportsData
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// So Android is well simulated on my JVM
@RunWith(RobolectricTestRunner::class)
class EventAnalyserApiTest {
    lateinit var server: MockWebServer

    @Before
    fun setupMockServer() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun shutdownMockServer() {
        server.shutdown()
    }

    fun getRetrofitService(url: HttpUrl): EventAnalyserApiService {
        val moshi: Moshi = Moshi.Builder()
            .add(
                // 1. Define base class
                PolymorphicJsonAdapterFactory.of(
                    EventsReportsData::class.java,
                    "type"                          // "type" is the key inside JSON
                )
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

        val retrofit = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(url)
            .build()
        val retrofitService: EventAnalyserApiService by lazy {
            retrofit.create(EventAnalyserApiService::class.java)
        }
        return retrofitService
    }

    @Test
    fun testPing() = runBlocking {
        server.enqueue(MockResponse().setBody("\"pong!\""))

        val retrofit = getRetrofitService(server.url("/"))
        val result = retrofit.ping()

        val request = server.takeRequest()

        assertThat(request.path).isEqualTo("/ping")
        assertThat(result).isEqualTo("pong!")
    }

    @Test
    fun test_getReportTypes() = runBlocking {
        server.enqueue(MockResponse().setBody("""["BASIC", "CHART"]"""))

        val retrofit = getRetrofitService(server.url("/"))
        val result = retrofit.getReportTypes()

        val request = server.takeRequest()

        assertThat(request.path).isEqualTo("/reports/types")
        assertThat(result).isEqualTo(arrayOf("BASIC", "CHART"))
    }

    @Test
    fun test_getReportData_BASIC_REPORT() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"type": "BASIC", "weekly":"1,0","monthly":"4,0","sigma":"0,5","start_date":"2026-05-16","total_occurrences":"10"}"""))

        val retrofit = getRetrofitService(server.url("/"))
        val result = retrofit.getReportData(EventsReportType.BASIC.name, 1)

        val request = server.takeRequest()

        assertThat(request.path).isEqualTo("/reports?type=BASIC&subject_id=1")
        assertThat(result).isEqualTo(
            EventsReportsData.BasicReportData(
                "1,0",
                "4,0",
                "0,5",
                "2026-05-16",
                "10"
            )
        )
    }

    @Test
    fun test_getReportData_CHART_REPORT() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"type": "CHART", "data":"TODO: REMAKE THIS DATA STRUCTURE"}"""))

        val retrofit = getRetrofitService(server.url("/"))
        val result = retrofit.getReportData(EventsReportType.CHART.name, 1)

        val request = server.takeRequest()

        assertThat(request.path).isEqualTo("/reports?type=CHART&subject_id=1")
        assertThat(result).isEqualTo(EventsReportsData.ChartReportData("TODO: REMAKE THIS DATA STRUCTURE"))
    }
}

