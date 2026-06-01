package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network.EventAnalyserApiService
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network.EventRequest
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network.EventResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant

private const val TAG = "EventsHomeViewModel"

data class Event(
    val id: Int,
    val subjectID: Int,
    val occurrences: Int,
    val insertTS: ZonedDateTime,
    val lastUpdate: ZonedDateTime,
) {
    override fun toString(): String {
        return "Event(id=$id, subjectID=$subjectID, occurrences=$occurrences,\ninsertTS=${
            insertTS.format(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
            )
        },\nlastUpdate=${
            lastUpdate.format(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
            )
        })"
    }
}

sealed interface EventsHomeUiState {
    data class Success(
        val createdEvent: Event? = null,
        val subjectID: Int = 0,
        val occurrencesNumber: Int = 1,
        val eventLocalDateTime: ZonedDateTime = ZonedDateTime.now(),
        val showCreatedEventSnackbar: Boolean = false,
        val eventAlreadyExists: Boolean = false,
        val subjectIDNotFound: Boolean = false,
        val eventUpdated: Boolean = false
    ) : EventsHomeUiState

    object Loading : EventsHomeUiState
    data class Error(
        val errorMessage: String,
        val connectionError: Boolean,
        val eventLocalDateTime: ZonedDateTime = ZonedDateTime.now(),
    ) : EventsHomeUiState
}

class EventsHomeViewModel(val retrofitService: EventAnalyserApiService) : ViewModel() {
    var uiState: EventsHomeUiState by mutableStateOf(EventsHomeUiState.Loading)
        private set // Makes the setter private works like the _uiState pattern

    init {
        pingEventsAnalyserServer()
    }

    fun updateUIState(newState: EventsHomeUiState) {
        uiState = newState
    }

    fun updateSuccessUIState(
        createdEvent: Event? = null,
        subjectID: Int? = null,
        occurrencesNumber: Int? = null,
        eventLocalDateTime: ZonedDateTime? = null,
        showCreatedEventSnackbar: Boolean? = null,
        eventAlreadyExists: Boolean? = null,
        subjectIDNotFound: Boolean? = null,
        eventUpdated: Boolean? = null
    ) {
        val currentState = (uiState as EventsHomeUiState.Success)
        val updatedState = currentState.copy(
            createdEvent = createdEvent ?: currentState.createdEvent,
            subjectID = subjectID ?: currentState.subjectID,
            occurrencesNumber = occurrencesNumber ?: currentState.occurrencesNumber,
            eventLocalDateTime = eventLocalDateTime ?: currentState.eventLocalDateTime,
            showCreatedEventSnackbar = showCreatedEventSnackbar
                ?: currentState.showCreatedEventSnackbar,
            eventAlreadyExists = eventAlreadyExists ?: currentState.eventAlreadyExists,
            subjectIDNotFound = subjectIDNotFound ?: currentState.subjectIDNotFound,
            eventUpdated = eventUpdated ?: currentState.eventUpdated
        )
        uiState = updatedState
    }

    fun pingEventsAnalyserServer() {
        viewModelScope.launch(Dispatchers.IO) {
            uiState = try {
                val pong = retrofitService.ping()
                Log.d(TAG, pong)
                EventsHomeUiState.Success()
            } catch (e: IOException) {
                Log.e(TAG, "IO Error: ${e.message}. $e")
                EventsHomeUiState.Error(
                    e.message ?: "",
                    connectionError = true,
                )
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP Error: ${e.message}. $e")
                var errorMessage = e.message
                if (e.code() == 502) errorMessage = "$errorMessage - server offline"
                EventsHomeUiState.Error(errorMessage ?: "", connectionError = true)
            }
        }
    }

    /**
     * Function that tries to send Event to API and updates the UI state accordingly
     */
    fun submit() {
        viewModelScope.launch(Dispatchers.IO) {
            uiState = try {
                Log.i(TAG, "Trying to submit data... $uiState")
                val response = retrofitService.postEvent(
                    EventRequest(
                        subjectID = (uiState as EventsHomeUiState.Success).subjectID,
                        occurrences = (uiState as EventsHomeUiState.Success).occurrencesNumber,
                        insertTS = (uiState as EventsHomeUiState.Success).eventLocalDateTime.format(
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        ) // Format equivalent to RFC3339
                    )
                )
                Log.i(TAG, "Event created successfully: $response")
                EventsHomeUiState.Success(
                    createdEvent = Event(
                        response.id,
                        response.subjectID,
                        response.occurrences,
                        fromInstantUTCToCalendarLocal(response.insertTS),
                        fromInstantUTCToCalendarLocal(response.lastUpdate)
                    ),
                    subjectID = response.subjectID,
                    occurrencesNumber = response.occurrences,
                    eventLocalDateTime = ZonedDateTime.ofInstant(
                        response.insertTS.toJavaInstant(),
                        ZoneId.systemDefault()
                    ),
                    showCreatedEventSnackbar = true,
                    eventAlreadyExists = false,
                    subjectIDNotFound = false,
                    eventUpdated = false,
                )
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    val receivedJSON = e.response()?.errorBody()?.string() ?: ""
                    val responseErr = Json.decodeFromString<EventResponse>(receivedJSON)
                    Log.d(TAG, "Expected error 409(Conflict), respErr: $responseErr")
                    (uiState as EventsHomeUiState.Success).copy(
                        createdEvent = Event(
                            responseErr.id,
                            responseErr.subjectID,
                            responseErr.occurrences,
                            fromInstantUTCToCalendarLocal(responseErr.insertTS),
                            fromInstantUTCToCalendarLocal(responseErr.lastUpdate)
                        ),
                        showCreatedEventSnackbar = false,
                        eventAlreadyExists = true,
                        subjectIDNotFound = false,
                        eventUpdated = false,
                    )
                } else if (e.code() == 404) {
                    Log.e(TAG, "Error: ${e.message}. $e")
                    val currentState = (uiState as EventsHomeUiState.Success)
                    currentState.copy(
                        createdEvent = Event(
                            id = currentState.subjectID,
                            subjectID = currentState.subjectID,
                            occurrences = currentState.occurrencesNumber,
                            insertTS = ZonedDateTime.now(),
                            lastUpdate = ZonedDateTime.now()
                        ),
                        showCreatedEventSnackbar = false,
                        eventAlreadyExists = false,
                        subjectIDNotFound = true,
                        eventUpdated = false,
                    )
                } else {
                    // e.code() == 400 || e.code() == 500
                    Log.e(TAG, "Unexpected error: ${e.message}. $e")
                    val errorMessage =
                        "Unexpected error: ${e.message}. Contact server administrator"
                    EventsHomeUiState.Error(errorMessage, connectionError = true)
                }
            }
        }
    }

    fun retry() {
        Log.i(TAG, "Trying to reconnect to API...")
        uiState = EventsHomeUiState.Loading
        pingEventsAnalyserServer()
    }

    fun onSubjectIDChanged(subjectID: String) {
        val newSubjectID = subjectID.toIntOrNull()
        uiState = (uiState as EventsHomeUiState.Success).copy(
            subjectID = newSubjectID ?: 0,
        )
    }

    fun onOccurrencesNumberChanged(occurrencesNumber: String) {
        val newOccurrencesNumber = occurrencesNumber.toIntOrNull()
        uiState = (uiState as EventsHomeUiState.Success).copy(
            occurrencesNumber = newOccurrencesNumber ?: 0,
        )
    }

    fun onEventDateChanged(eventDateMillis: Long?) {
        eventDateMillis?.let {
            // 'eventDateMillis' é um Long representando a data em milissegundos UTC
            val dateTime = ZonedDateTime.ofInstant(
                Instant.fromEpochMilliseconds(eventDateMillis).toJavaInstant(),
                ZoneOffset.UTC // Must be interpreted as UTC because the DatePicker returns in UTC YYYY-MM-DDT00:00:00Z format, meaning negative timezones (e.g.: GMT-3) would change the day
            )

            var currentUIState = (uiState as EventsHomeUiState.Success)

            if (dateTime.format(DateTimeFormatter.ISO_DATE) == currentUIState.eventLocalDateTime.format(
                    DateTimeFormatter.ISO_DATE
                )
            ) {
                // If new date is the same as old date...
                Log.d(
                    TAG,
                    "onEventDateChanged: Changed date, so updating UIState.eventAlreadyExists to false"
                )
                currentUIState = currentUIState.copy(eventAlreadyExists = false)
            }

            val updatedDateTime = currentUIState.eventLocalDateTime
                .withYear(dateTime.year)
                .withMonth(dateTime.monthValue)
                .withDayOfMonth(dateTime.dayOfMonth)

            uiState = currentUIState.copy(
                eventLocalDateTime = updatedDateTime,
                createdEvent = null
            )
        }
    }

    fun onEventTimeChanged(hour: Int, minute: Int, is24Hour: Boolean, isPm: Boolean) {
        val updatedDateTime =
            (uiState as EventsHomeUiState.Success).eventLocalDateTime.withHour(hour)
                .withMinute(minute)
        uiState = (uiState as EventsHomeUiState.Success).copy(
            eventLocalDateTime = updatedDateTime,
            createdEvent = null
        )
    }

    fun onPutEventNewOccurrenceNumber() {
        viewModelScope.launch(Dispatchers.IO) {
            val createdEvent = (uiState as EventsHomeUiState.Success).createdEvent
            Log.i(TAG, "Trying to update Event ${createdEvent?.id}. ... $uiState")
            createdEvent?.let {
                uiState = try {
                    val response = retrofitService.putEvent(
                        id = createdEvent.id,
                        eventRequest = EventRequest(
                            subjectID = (uiState as EventsHomeUiState.Success).subjectID,
                            occurrences = (uiState as EventsHomeUiState.Success).occurrencesNumber,
                            insertTS = (uiState as EventsHomeUiState.Success).eventLocalDateTime.format(
                                DateTimeFormatter.ISO_OFFSET_DATE_TIME
                            ) // Format equivalent to RFC3339
                        )
                    )
                    Log.i(TAG, "Event updated successfully: $response")
                    EventsHomeUiState.Success(
                        createdEvent = Event(
                            response.id,
                            response.subjectID,
                            response.occurrences,
                            fromInstantUTCToCalendarLocal(response.insertTS),
                            fromInstantUTCToCalendarLocal(response.lastUpdate)
                        ),
                        subjectID = response.subjectID,
                        occurrencesNumber = response.occurrences,
                        eventLocalDateTime = ZonedDateTime.ofInstant(
                            response.insertTS.toJavaInstant(),
                            ZoneId.systemDefault()
                        ),
                        showCreatedEventSnackbar = false,
                        eventAlreadyExists = false,
                        subjectIDNotFound = false,
                        eventUpdated = true,
                    )
                } catch (e: HttpException) {
                    val errorMessage = "Could not update event ${createdEvent}. Error: ${e.message}"
                    Log.e(TAG, errorMessage)
                    EventsHomeUiState.Error(
                        errorMessage,
                        connectionError = false,
                        eventLocalDateTime = (uiState as EventsHomeUiState.Success).eventLocalDateTime
                    )
                }
            }
        }
    }

    fun fromInstantUTCToCalendarLocal(instant: Instant): ZonedDateTime {
        return ZonedDateTime.ofInstant(instant.toJavaInstant(), ZoneId.systemDefault())
    }
}
