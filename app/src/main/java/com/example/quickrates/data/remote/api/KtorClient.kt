package com.example.quickrates.data.remote.api

import com.example.quickrates.data.model.DollarResponse
import com.example.quickrates.data.model.EuroResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object KtorClient {

    // Ktor HttpClient
    private fun client(): HttpClient {
        return HttpClient(CIO){
            install(HttpTimeout){
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 30000
                socketTimeoutMillis = 30000
            }
            install(ContentNegotiation){
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
        }
    }

    suspend fun getDollarRates(): DollarResponse {
        return client().get("https://pydolarve.org/api/v1/dollar").body()
    }

    suspend fun getEuroRates(): EuroResponse {
        return client().get("https://pydolarve.org/api/v1/euro").body()
    }
}

