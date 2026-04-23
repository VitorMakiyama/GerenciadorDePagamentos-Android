package com.makiyamasoftware.gerenciadordepagamentos.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network.EventAnalyserApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SettingsUIState(
    val baseURL: String = "",
    val editBaseURL: Boolean = false
)

class SettingsViewModel(private val settingsRepo: SettingsRepository) : ViewModel() {
    var uiState: SettingsUIState by mutableStateOf(SettingsUIState())
        private set // Makes the setter private works like the _uiState pattern

    init {
        //  Observes Flow continuously
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.baseUrlFlow.collect { url ->
                // Every time the DataStore changes, it triggers automatically
                val finalUrl = url.ifBlank { EventAnalyserApi.getBaseURLConst() }

                // Updates the state on the Main Thread (Compose demands that to reflect on UI)
                launch(Dispatchers.Main) {
                    uiState = uiState.copy(baseURL = finalUrl)
                }
            }
        }
    }

    /**
     * Sets the User typed URL as the new Base URL for the EventAnalyserApi
     */
    fun setNewBaseURL(newURL: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.updateBaseUrl(newURL)
        }
        viewModelScope.launch(Dispatchers.Main) {
            dismissEditBaseURLDialog()
        }
    }

    fun showEditBaseURLDialog() {
        uiState = uiState.copy(
            editBaseURL = true
        )
    }

    fun dismissEditBaseURLDialog() {
        uiState = uiState.copy(
            editBaseURL = false
        )
    }
}