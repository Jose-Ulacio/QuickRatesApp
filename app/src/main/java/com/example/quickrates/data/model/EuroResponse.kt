package com.example.quickrates.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EuroResponse(
    @SerialName("datetime") var datetime: DatetimeModel,
    @SerialName("monitors") var monitorEuros: MonitorEurModel
){
    @Serializable
    data class DatetimeModel(
        @SerialName("date") var date: String,
        @SerialName("time") var time: String
    )

    @Serializable
    data class MonitorEurModel(
        @SerialName("amazon_gift_card") var amazon: CurrencyItemEur,
        @SerialName("bcv") var BCV: CurrencyItemEur,
        @SerialName("binance") var binance: CurrencyItemEur,
        @SerialName("cripto_euro") var criptoEuro: CurrencyItemEur,
        @SerialName("enparalelovzla") var enParalelo: CurrencyItemEur,
        @SerialName("euro_today") var euroToday: CurrencyItemEur,
        @SerialName("paypal") var paypal: CurrencyItemEur,
        @SerialName("promedio") var promedio: CurrencyItemEur,
        @SerialName("skrill") var skrill: CurrencyItemEur,
        @SerialName("uphold") var uphold: CurrencyItemEur
    ){
        @Serializable
        data class CurrencyItemEur(
            @SerialName("last_update") val lastUpdate:String,
            @SerialName("price") val price:Double,
            @SerialName("price_old") val priceOld:Double

        )
    }
}
