package com.example.quickrates.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DollarResponse(
    @SerialName("datetime") var datetime: DatetimeModel,
    @SerialName("monitors") var monitorDollars: MonitorUsdModel
) {
    @Serializable
    data class DatetimeModel(
        @SerialName("date") var date: String,
        @SerialName("time") var time: String
    )

    @Serializable
    data class MonitorUsdModel(
        @SerialName("bcv") val BCV: CurrencyItemUsd,
        @SerialName("enparalelovzla") val enParalelo: CurrencyItemUsd
    ){
        @Serializable
        data class CurrencyItemUsd(
            @SerialName("last_update") val lastUpdate:String,
            @SerialName("price") val price:Double,
            @SerialName("price_old") val priceOld:Double
        )
    }
}
