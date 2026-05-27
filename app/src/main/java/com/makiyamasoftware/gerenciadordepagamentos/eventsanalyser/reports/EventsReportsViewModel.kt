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
    abstract val showSnackbar: Boolean
    abstract val snackbarMessage: String

    data class Success(
        override val showSnackbar: Boolean,
        override val snackbarMessage: String,
        val reportTypes: List<String>,
        val subjectIDs: List<String>,
        val reportsData: EventsReportsData? = null
    ) : EventsReportsUIState()

    data class Error(
        override val showSnackbar: Boolean,
        override val snackbarMessage: String,
        val connectionError: Boolean,
        val reportDataRetrieveError: Boolean
    ) : EventsReportsUIState()

    data class Loading(
        override val showSnackbar: Boolean = false,
        override val snackbarMessage: String = "",
    ) : EventsReportsUIState()
}

class EventsReportsViewModel(val retrofitService: EventAnalyserApiService) : ViewModel() {
    var uiState: EventsReportsUIState by mutableStateOf(EventsReportsUIState.Loading())
        private set

    var selectedReportType: String by mutableStateOf(EventsReportType.BASIC.name)
    var selectedSubjectID: Int by mutableIntStateOf(0)
    var reportsTypes: List<String> by mutableStateOf(listOf())
    var subjectsIDs: List<String> by mutableStateOf(listOf())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getReportsTypesAndSubjectIDs()
        }
    }

    suspend fun getReportsTypesAndSubjectIDs() {
        uiState = try {
            val types = retrofitService.getReportTypes()
            val subjects = retrofitService.getAllSubjects()
            Log.i(TAG, "Got these types $types and subjects $subjects from server")
            selectedReportType = types.first()
            selectedSubjectID = subjects.first().id
            reportsTypes = types
            subjectsIDs = getSubjectIDsFromSubjects(subjects)

            EventsReportsUIState.Success(
                showSnackbar = false,
                snackbarMessage = "",
                reportTypes = types,
                subjectIDs = subjectsIDs
            )
        } catch (e: HttpException) {
            Log.e(TAG, "Error getting Report types or subject IDs: ${e.message}. $e")
            EventsReportsUIState.Error(
                showSnackbar = true,
                snackbarMessage = "Error getting types or subject IDs: ${e.message}. $e",
                connectionError = true,
                reportDataRetrieveError = false
            )
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

                Log.i(TAG, "Got this report from server: $result")
                when (reportType) {
                    EventsReportType.BASIC.name -> {
                        EventsReportsUIState.Success(
                            showSnackbar = false,
                            snackbarMessage = "",
                            reportTypes = success.reportTypes,
                            subjectIDs = success.subjectIDs,
                            reportsData = (result as EventsReportsData.BasicReportData)
                        )
                    }

                    EventsReportType.CHART.name -> {
                        EventsReportsUIState.Success(
                            showSnackbar = false,
                            snackbarMessage = "",
                            reportTypes = success.reportTypes,
                            subjectIDs = success.subjectIDs,
                            reportsData = (result as EventsReportsData.ChartReportData)
                        )
                    }

                    else -> {
                        Log.e(TAG, "Report type unknown: $reportType and data: $result")
                        EventsReportsUIState.Error(
                            showSnackbar = true,
                            snackbarMessage = "Report type not processable: $reportType",
                            connectionError = false,
                            reportDataRetrieveError = true
                        )
                    }
                }
            } catch (e: HttpException) {
                Log.e(TAG, "Error getting report data: ${e.message}. $e")
                val isConnectionError: Boolean = e.code() == 503
                EventsReportsUIState.Error(
                    showSnackbar = true,
                    snackbarMessage = "Error getting report data: ${e.message}.",
                    connectionError = isConnectionError,
                    reportDataRetrieveError = !isConnectionError
                )
            }
        }
    }

    fun onChangeReportType(newType: String) {
        selectedReportType = newType
        getReportData(selectedReportType, selectedSubjectID)
    }

    fun onChangeSubjectID(newID: String) {
        selectedSubjectID = newID.toInt()
        getReportData(selectedReportType, selectedSubjectID)
    }

    fun pingEventsAnalyserServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pong = retrofitService.ping()
                Log.d(TAG, pong)
                getReportsTypesAndSubjectIDs()
            } catch (e: IOException) {
                Log.e(TAG, "Error: ${e.message}. $e")
                uiState = EventsReportsUIState.Error(
                    showSnackbar = true,
                    snackbarMessage = "Error: ${e.message}.",
                    connectionError = true,
                    reportDataRetrieveError = false
                )
            } catch (e: HttpException) {
                Log.e(TAG, "Error: ${e.message}. $e")
                var errorMessage = e.message
                if (e.code() == 502) errorMessage = "$errorMessage - server offline"
                uiState = EventsReportsUIState.Error(
                    showSnackbar = true,
                    snackbarMessage = errorMessage ?: "Error",
                    connectionError = true,
                    reportDataRetrieveError = false
                )
            }
        }
    }

    fun finishedShowingSnackbar() {
        uiState = when (uiState) {
            is EventsReportsUIState.Success -> {
                (uiState as EventsReportsUIState.Success).copy(
                    showSnackbar = false
                )
            }

            is EventsReportsUIState.Loading -> {
                (uiState as EventsReportsUIState.Loading).copy(
                    showSnackbar = false
                )
            }

            is EventsReportsUIState.Error -> {
                (uiState as EventsReportsUIState.Error).copy(
                    showSnackbar = false
                )
            }
        }
    }
}