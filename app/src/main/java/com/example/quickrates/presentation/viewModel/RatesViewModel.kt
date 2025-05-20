package com.example.quickrates.presentation.viewModel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickrates.App
import com.example.quickrates.R
import com.example.quickrates.data.model.DollarResponse
import com.example.quickrates.data.model.EuroResponse
import com.example.quickrates.data.remote.api.KtorClient
import com.example.quickrates.utils.notifications.NotificationProvider
import com.example.quickrates.utils.notifications.NotificationUtils
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.network.sockets.SocketTimeoutException
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class RatesViewModel(application: Application): AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext

    //States para los Datos
    val dollarRates = mutableStateOf<DollarResponse?>(null)

    var euroRate by mutableStateOf<EuroResponse?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    //States para las selecciones del Usuario
    var selectCurrency by mutableStateOf("USD")
        private set

    var selectedPlatform by mutableStateOf("BCV")
        private set

    //Precio basado en la seleccion
    var currentRate by mutableStateOf(0.0)
        private set

    var previousRate by mutableStateOf(0.0)
        private set

    var lastUpdate by mutableStateOf("")
        private set

    //Variables para Fecha y Hora
    var lastUpdateDate by mutableStateOf("")
    var lastUpdateTime by mutableStateOf("")

    init {
        fetchAllRates()
    }

    private fun updateDataTime(datatime: DollarResponse.DatetimeModel?){
        lastUpdateDate = datatime?.date ?: "N/D"
        lastUpdateTime = datatime?.time ?: "N/D"
        lastUpdate = "${lastUpdateDate} ${lastUpdateTime}"
    }

    private fun updateCurrentRate() {
        when(selectCurrency){
            "USD" -> {
                val monitor = when(selectedPlatform){
                    "BCV" -> dollarRates.value?.monitorDollars?.BCV
                    "MNT" -> dollarRates.value?.monitorDollars?.enParalelo
                    else -> null
                }
                currentRate = monitor?.price ?: 0.0
                previousRate = monitor?.priceOld ?: 0.0
                lastUpdate = monitor?.lastUpdate ?: ""
            }
            "EUR" -> {
                val monitor = when(selectedPlatform){
                    "BCV" -> euroRate?.monitorEuros?.BCV
                    "MNT" -> euroRate?.monitorEuros?.enParalelo
                    else -> null
                }
                currentRate = monitor?.price ?: 0.0
                previousRate = monitor?.priceOld ?: 0.0
                lastUpdate = monitor?.lastUpdate ?: ""
            }
        }
    }

    private fun fetchAllRates() {
        viewModelScope.launch {

            isLoading = true
            error = null
            try {
                dollarRates.value = KtorClient.getDollarRates()
                euroRate = KtorClient.getEuroRates()

                updateDataTime(dollarRates.value?.datetime)
                updateCurrentRate()
            } catch (e: Exception){
                error = when(e){
                    is UnknownHostException -> context.getString(R.string.error_no_internet)
                    is SocketTimeoutException -> context.getString(R.string.error_timeout)

                    else -> context.getString(R.string.error_generic, e.localizedMessage ?: context.getString(R.string.unknown_error))
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun clearError(){
        error = null
    }

    fun refreshRates(){
        if (!isNetworkAvailable(context)){
            error = context.getString(R.string.error_no_internet)
            return
        }
        error = null
        fetchAllRates()
    }

    //Funcion para determinar si hay o no conexion a internet
    fun isNetworkAvailable(context: Context): Boolean{
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network)?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun selectCurrency(currency: String){
        selectCurrency = currency

        selectedPlatform = when(currency){
            "USD" -> "BCV"
            "EUR" -> "BCV"
            else -> "BCV"
        }
        updateCurrentRate()
    }

    fun selectPlatform(platform:String){
        selectedPlatform = platform
        updateCurrentRate()
    }

    fun getAvailablePlatform(): List<String> {
        return when(selectCurrency){
            "USD" -> listOf("BCV", "MNT") // BCV y Monitor/Paralelo para USD
            "EUR" -> listOf("BCV", "MNT") // BCV y Monitor/Paralelo para EUR
            else -> listOf("BCV")
        }
    }

    fun getFormattedRate(): String {
        return "%.2f Bs".format(currentRate)
    }

    fun getRateChangeIndicator(): RateChange {
        return when{
            currentRate > previousRate -> RateChange.INCREASE
            currentRate < previousRate -> RateChange.DECREASE
            else -> RateChange.NO_CHANGE
        }
    }

    fun convertToBs(amount:String): String {
        return try {
            val amountValue = amount.toDouble()
            "%.2f".format(amountValue * currentRate)
        } catch (e: NumberFormatException){
            "0.00"
        }
    }

    fun convertFromBs(amount: String): String {
        return try {
            val amountValue = amount.toDouble()
            if (currentRate != 0.0) {
                "%.2f".format(amountValue / currentRate)
            } else {
                "0.00"
            }
        } catch (e: NumberFormatException) {
            "0.00"
        }
    }
}
