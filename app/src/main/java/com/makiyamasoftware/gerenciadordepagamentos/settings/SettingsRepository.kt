package com.makiyamasoftware.gerenciadordepagamentos.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val BASE_URL = stringPreferencesKey("base_url")
    }

    // Safe reading with error treatment
    val baseUrlFlow: Flow<String> = dataStore.data.catch { exception ->
        if (exception is IOException) emit(emptyPreferences()) else throw exception
    }
        .map { preferences ->
            preferences[PreferencesKeys.BASE_URL] ?: ""
        }

    // Write
    suspend fun updateBaseUrl(newUrl: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BASE_URL] = newUrl
        }
    }
}