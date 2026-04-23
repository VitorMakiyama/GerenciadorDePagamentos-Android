package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.network

import com.makiyamasoftware.gerenciadordepagamentos.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class BaseURLInterceptor(private val repository: SettingsRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Gets the URL from DataStore synchronously inside the network thread
        val savedUrl = runBlocking { repository.baseUrlFlow.first() }

        // If DataStore is empty, uses the original URL from Retrofit (fallback from local.properties)
        val newUrl = savedUrl.toHttpUrlOrNull() ?: return chain.proceed(originalRequest)

        // Rebuilds the URL keeping the path (ex: /ping, /events, /subjects)
        val updatedUrl = originalRequest.url.newBuilder()
            .scheme(newUrl.scheme)
            .host(newUrl.host)
            .port(newUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(updatedUrl)
            .build()

        return chain.proceed(newRequest)
    }
}