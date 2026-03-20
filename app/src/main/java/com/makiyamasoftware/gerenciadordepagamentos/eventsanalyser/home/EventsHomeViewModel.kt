package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network.EventAnalyserApi
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network.EventAnalyserApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.IOException

private const val TAG = "EventsHomeViewModel"

sealed interface EventsHomeUiState {
    data class Success(
        val events: List<EventAnalyserApiService.MarsPhoto>,
        val eventID: Int = 0,
        val occurrencesNumber: Int = 1
    ) : EventsHomeUiState

    object Loading : EventsHomeUiState
    object Error : EventsHomeUiState
}

class EventsHomeViewModel : ViewModel() {
    var uiState: EventsHomeUiState by mutableStateOf(EventsHomeUiState.Loading)
        private set // Makes the setter private works like the _uiState pattern

    init {
        getPhotos()
    }

    fun updateUIState(newState: EventsHomeUiState) {
        uiState = newState
    }

    fun getPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            uiState = try {
                val listResult = EventAnalyserApi.retrofitService.getPhotos()
                EventsHomeUiState.Success(listResult)
            } catch (e: IOException) {
                EventsHomeUiState.Error
            }
        }
    }

    fun submit() {
        Log.i(TAG, "Trying to submit data...")
        TODO()
    }

    fun retry() {
        Log.i(TAG, "Trying to reconnect to API...")
        uiState = EventsHomeUiState.Loading
        getPhotos()
    }
}
