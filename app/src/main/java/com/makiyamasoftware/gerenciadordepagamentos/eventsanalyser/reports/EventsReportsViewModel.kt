package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.reports

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network.EventAnalyserApiService
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network.SubjectResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException

private const val TAG = "EventsReportsViewModel"

sealed class EventsReportsUIState {
    data class Success(
        val reportTypes: List<String>,
        val subjectIDs: List<String>,
        val reportsData: EventsReportsData? = null
    ): EventsReportsUIState()

    data class Error(
        val connectionError: Boolean
    ): EventsReportsUIState()

    object Loading : EventsReportsUIState()
}

class EventsReportsViewModel(val retrofitService: EventAnalyserApiService) : ViewModel() {
    var uiState: EventsReportsUIState by mutableStateOf(EventsReportsUIState.Loading)
        private set

    var selectedReportType: String by mutableStateOf(EventsReportType.BASIC.name)
    var selectedSubjectID: Int by mutableIntStateOf(0)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getReportsTypesAndSubjectIDs()
        }
    }

    suspend fun getReportsTypesAndSubjectIDs() {
        uiState = try {
            val reportsTypes = retrofitService.getReportTypes()
            val subjects = retrofitService.getAllSubjects()

            selectedReportType = reportsTypes.first()
            selectedSubjectID = subjects.first().id

            EventsReportsUIState.Success(
                reportTypes = reportsTypes,
                subjectIDs = getSubjectIDsFromSubjects(subjects)
            )
        } catch (e: HttpException) {
            Log.e(TAG, "Error getting Report types or subject IDs: ${e.message}. $e")
            EventsReportsUIState.Error(connectionError = true)
        }
    }

    private fun getSubjectIDsFromSubjects(subjects: List<SubjectResponse>): List<String> {
        return subjects.map { subjects -> subjects.id.toString() }
    }

    fun getReportData(reportType: String, subjectID: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            uiState = try {
                val result = retrofitService.getReportData(reportType, subjectID)
                val success = uiState as EventsReportsUIState.Success

                Log.e(TAG, "Got this report from server: $result")
                when(reportType) {
                    EventsReportType.BASIC.name -> {
                        EventsReportsUIState.Success(
                            reportTypes = success.reportTypes,
                            subjectIDs = success.subjectIDs,
                            reportsData = (result as EventsReportsData.BasicReportData)
                        )
                    }
                    EventsReportType.CHART.name -> {
                        EventsReportsUIState.Success(
                            reportTypes = success.reportTypes,
                            subjectIDs = success.subjectIDs,
                            reportsData = (result as EventsReportsData.ChartReportData)
                        )
                    }
                    else -> {
                        Log.e(TAG, "Report type unknown: $reportType and data: $result")
                        EventsReportsUIState.Error(
                            connectionError = false
                        )
                    }
                }
            } catch (e: HttpException) {
                Log.e(TAG, "Error getting report data: ${e.message}. $e")
                EventsReportsUIState.Error(connectionError = true)
            }
        }
    }

    fun onChangeReportType(newType: String) {
        selectedReportType = newType
//        getReportData(selectedReportType, selectedSubjectID) TODO: once the endpoint is implemented
    }

    fun onChangeSubjectID(newID: String) {
        selectedSubjectID = newID.toInt()
//        getReportData(selectedReportType, selectedSubjectID) TODO: once the endpoint is implemented
    }

    fun pingEventsAnalyserServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pong = retrofitService.ping()
                Log.d(TAG, pong)
                getReportsTypesAndSubjectIDs()
            } catch (e: IOException) {
                Log.e(TAG, "Error: ${e.message}. $e")
                uiState = EventsReportsUIState.Error(connectionError = true)
            } catch (e: HttpException) {
                Log.e(TAG, "Error: ${e.message}. $e")
                var errorMessage = e.message
                if (e.code() == 502) errorMessage = "$errorMessage - server offline"
                uiState = EventsReportsUIState.Error(connectionError = true)
            }
        }
    }
}