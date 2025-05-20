package com.example.quickrates.presentation.view

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.quickrates.App
import com.example.quickrates.R
import com.example.quickrates.presentation.ui.theme.PurpleDarkTrans
import com.example.quickrates.presentation.ui.theme.QuickRatesTheme
import com.example.quickrates.presentation.viewModel.RateChange
import com.example.quickrates.presentation.viewModel.RatesViewModel
import com.example.quickrates.utils.timeUtils.TimeUtils
import com.example.quickrates.utils.worker.QuickRatesWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: RatesViewModel by viewModels()

    lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permiso para notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        workManager = WorkManager.getInstance(App.instance)

        setupWorker("QuickRatesWorkerBCV", 17, 0, workManager)
        setupWorker("QuickRatesWorkerMonitor", 12, 30, workManager)

        setContent {
            QuickRatesTheme {
                Box(modifier = Modifier.fillMaxWidth()){
                    MainDesign(viewModel = viewModel)

                    //Dialog de Carga
                    LoadingDialog(show = viewModel.isLoading)

                    //Dialog de Error
                    ErrorDialog(
                        errorMessage = viewModel.error,
                        onDismiss = {viewModel.clearError()},
                        onRetry = {viewModel.refreshRates()}
                    )
                }
            }
        }
    }

    fun setupWorker(uniqueWorkName: String, hour: Int, minute: Int, workManager: WorkManager){
        workManager.getWorkInfosForUniqueWorkLiveData(uniqueWorkName).observeForever{ workInfo ->
            if ((workInfo == null) or (workInfo.isEmpty())){
                val initialDelay = TimeUtils.calculateInitialDelay(hour, minute)

                val dailyWorkRequest = PeriodicWorkRequestBuilder<QuickRatesWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .build()

                workManager.enqueueUniquePeriodicWork(uniqueWorkName, ExistingPeriodicWorkPolicy.KEEP, dailyWorkRequest)
            }
        }
    }
}

@Composable
fun MainDesign(viewModel: RatesViewModel) {

    var leftTextFieldValue by remember { mutableStateOf("1") }
    var rightTextFieldValue by remember { mutableStateOf("1") }
    var isLeftTextFieldFocused by remember { mutableStateOf(true) }

    //actualizar conversiones cuando cambia la tasa
    LaunchedEffect(viewModel.currentRate) {
        if (isLeftTextFieldFocused){
            rightTextFieldValue = viewModel.convertToBs(leftTextFieldValue)
        } else {
            leftTextFieldValue = viewModel.convertFromBs(rightTextFieldValue)
        }
    }

    //Funcion para menejar cambios en los Textfields
    fun updateConversion(){
        if (isLeftTextFieldFocused){
            rightTextFieldValue = viewModel.convertToBs(leftTextFieldValue)
        } else {
            leftTextFieldValue = viewModel.convertFromBs(rightTextFieldValue)
        }
    }

    //Observar cambios en las tasas
    val ratesChange by remember { derivedStateOf { viewModel.getRateChangeIndicator() } }
    val formattedRate by remember { derivedStateOf { viewModel.getFormattedRate() } }

    //funcion para borrar el ultimo caracter
    val onDelete = {
        if (isLeftTextFieldFocused){
            if (leftTextFieldValue.isNotEmpty()){
                leftTextFieldValue = leftTextFieldValue.dropLast(1)
                if (leftTextFieldValue.isEmpty()) leftTextFieldValue = "0"
            }
        } else {
            if (rightTextFieldValue.isNotEmpty()){
                rightTextFieldValue = rightTextFieldValue.dropLast(1)
                if (rightTextFieldValue.isEmpty()) rightTextFieldValue = "0"
            }
        }
        updateConversion()
    }

    //Funcion para acumular los Numeros
    val onNumberButtonClick: (String) -> Unit = { number ->
        if (isLeftTextFieldFocused) {
            leftTextFieldValue = if (leftTextFieldValue == "0") number else leftTextFieldValue + number
        } else {
            rightTextFieldValue = if (rightTextFieldValue == "0") number else rightTextFieldValue + number
        }
        updateConversion()
    }

    //variable para controlar dimensiones de la primera caja
    var firstBoxSize by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current.density

    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Color.White)
        ) {
        //NameApp y Boton de Refrescar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(start = 22.dp, top = 18.dp),
                text = "QUICK RATES",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
            )
            IconButton(onClick = {
                viewModel.refreshRates()
            },
                modifier = Modifier
                    .padding(end = 22.dp, top = 18.dp)
                    .height(36.dp)
                    .width(35.dp)
                    .clip(CircleShape)
                    .background(color = colorResource(R.color.gray_main)))
            {
                Image(painter = painterResource(R.drawable.ic_refres),
                    contentDescription = "Boton Para refrescar")
            }
        }

        //Texto de Bienvenida
        Text(
            modifier = Modifier.padding(start = 22.dp, top = 18.dp),
            text = stringResource(R.string.welcome_message),
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
                ),
            )

        //Ultima Actualizacion de Precios
        Text(
            modifier = Modifier.padding(start = 22.dp, top = 4.dp),
            text = stringResource(R.string.Last_Update, viewModel.lastUpdateDate, viewModel.lastUpdateTime),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
                ),
            )

        //Cajas

        //Caja Larga Superior
        Box(modifier = Modifier
            .padding(horizontal = 22.dp)
            .padding(top = 18.dp)
            .fillMaxWidth()
            //.height(64.dp)
            .background(
                color = colorResource(R.color.purple_main),
                shape = RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 32.dp
                )
            )
        ){
            //Aqui se ubican los DropMenu y la Tasa Actual
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 10.dp)) {

                //Caja para DropDownMenu de Monedas
                Box(modifier = Modifier
                    .background(
                        color = colorResource(R.color.purple_main)
                    )
                ){
                    //DropDownMenu de Monedas
                    DropDownMenuCurrency(
                        selectedCurrency = viewModel.selectCurrency,
                        onCurrencySelected = {viewModel.selectCurrency(it)}
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier
                        .height(24.dp))

                    //Text Tasa Actual
                    Text(
                        text = formattedRate,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    )

                    Box(modifier = Modifier
                        .size(24.dp)

                    ){
                        when(ratesChange){
                            RateChange.INCREASE -> Image(
                                painter = painterResource(R.drawable.ic_arrow_up_double_line_verde),
                                contentDescription = "Precio Subio"
                            )
                            RateChange.DECREASE -> Image(
                                painter = painterResource(R.drawable.ic_arrow_up_double_line_rojo),
                                contentDescription = "Precio Bajo"
                            )
                            RateChange.NO_CHANGE -> Image(
                                painter = painterResource(R.drawable.ic_equal_24dp),
                                contentDescription = "Precio Igual"
                            )
                        }
                    }
                }

                //Caja para DropDownMenu de Plataformas
                Box(modifier = Modifier
                    .background(
                        color = colorResource(R.color.purple_main)
                    )
                ){
                    //DropDownMenu de Plataformas
                    DropDownMenuPlatform(
                        selectedPlatform = viewModel.selectedPlatform,
                        availablePlatforms = viewModel.getAvailablePlatform(),
                        onPlatformSelected = {platform ->
                            viewModel.selectPlatform(platform)
                        }
                    )
                }
            }
        }

        // Box inferior
        Box(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .background(
                    color = colorResource(R.color.white),
                    shape = RoundedCornerShape(
                        bottomEnd = 32.dp,
                        bottomStart = 32.dp
                    )
                )
        ){
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                //Primera Caja
                Box(
                    modifier = Modifier
                        .weight(0.40f)
                        .background(
                            color = colorResource(R.color.purple_main),
                            shape = RoundedCornerShape(
                                bottomStart = 32.dp,
                                bottomEnd = 32.dp
                            )
                        )
                        .onGloballyPositioned {
                            firstBoxSize = (it.size.height / density).dp
                        },
                    contentAlignment = Alignment.Center
                ){
                    //Textfield para monto Dolares/euro
                    Card (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        backgroundColor = if (isLeftTextFieldFocused){
                            PurpleDarkTrans
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape,
                        elevation = 0.dp
                    ){
                        BasicTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            value = "$leftTextFieldValue ${
                                if (viewModel.selectCurrency == "USD"){
                                    "$"
                                } else {
                                    "€"
                                }
                            }",
                            onValueChange = { newValue ->
                                leftTextFieldValue = newValue
                                updateConversion()
                            },
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            readOnly = true,
                            maxLines = 1,
                            singleLine = true
                        )
                    }
                }

                //Segunda Caja, donde esta el Boton de Intercambiar
                Box(modifier = Modifier
                    .weight(0.20f)
                    .height(firstBoxSize)
                    .background(color = colorResource(R.color.purple_main))
                ){
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(
                                topStart = 128.dp,
                                topEnd = 128.dp
                            )
                        ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isLeftTextFieldFocused = !isLeftTextFieldFocused
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .width(50.dp)
                                .clip(CircleShape)
                                .background(
                                    color = colorResource(R.color.gray_main)
                                )
                        ) {
                            Image(painter = painterResource(R.drawable.ic_two_arrow),
                                contentDescription = "Boton Para Intercambiar")
                        }
                    }
                }

                //Tercera caja
                Box(
                    modifier = Modifier
                        .weight(0.40f)
                        .background(
                            color = colorResource(R.color.purple_main),
                            shape = RoundedCornerShape(
                                bottomStart = 32.dp,
                                bottomEnd = 32.dp
                            )
                        )
                ){
                    //Textfield para monto Bolivares
                    Card (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        backgroundColor = if (!isLeftTextFieldFocused){
                            PurpleDarkTrans
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape,
                        elevation = 0.dp
                    ){
                        BasicTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            value = "${rightTextFieldValue} Bs",
                            onValueChange = {newValue ->
                                rightTextFieldValue = newValue
                                updateConversion()
                            },
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            readOnly = true,
                            maxLines = 1,
                            singleLine = true
                        )
                    }
                }
            }
        }

        //Teclado
        //fila del 1, 2 y 3
        Row(horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp)
                .padding(top = 36.dp)
        ) {
            NumberButton("1") { onNumberButtonClick("1") }
            NumberButton("2") { onNumberButtonClick("2") }
            NumberButton("3") { onNumberButtonClick("3") }
        }

        //fila del 4, 5 y 6
        Row(horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp)
                .padding(top = 36.dp)
        ) {
            NumberButton("4") { onNumberButtonClick("4") }
            NumberButton("5") { onNumberButtonClick("5") }
            NumberButton("6") { onNumberButtonClick("6") }
        }

        //fila del 7, 8 y 9
        Row(horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp)
                .padding(top = 36.dp)
        ) {
            NumberButton("7") { onNumberButtonClick("7") }
            NumberButton("8") { onNumberButtonClick("8") }
            NumberButton("9") { onNumberButtonClick("9") }
        }

        //fila del 0, delete y decimal
        Row(horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp)
                .padding(top = 36.dp)
        ) {
            DeleteButtonWithIcon(onDelete = onDelete)
            NumberButton("0") { onNumberButtonClick("0") }
            NumberButton(".") {
                if (isLeftTextFieldFocused){
                    if (!leftTextFieldValue.contains(".")){
                        leftTextFieldValue += "."
                    }
                } else {
                    if (!rightTextFieldValue.contains(".")){
                        rightTextFieldValue += "."
                    }
                }
            }
        }
    }
}

@Composable
fun NumberButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(R.color.gray_main)
        ),
        modifier = Modifier
            .size(64.dp),
        shape = CircleShape,
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        Text(text = label, fontSize = 24.sp, color = Color.Black)
    }
    
}

@Composable
fun DeleteButtonWithIcon(onDelete: () -> Unit) {
    Button(
        onClick = onDelete,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(R.color.gray_main)
        ),// Color de fondo,
        modifier = Modifier
            .size(64.dp),
        shape = CircleShape,
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = "Borrar caracter"
        )
    }
}

@Composable
fun DropDownMenuCurrency(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    // Estado para controlar si el menú está abierto o cerrado
    var isExpanded by remember { mutableStateOf(false) }

    Box(){
        // Botón que activar el menú
        Button(onClick = { isExpanded = true },
            colors = ButtonDefaults.buttonColors(PurpleDarkTrans),
            contentPadding = PaddingValues(8.dp),
            shape = CircleShape,
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
        ) {

            //icono para la Moneda
            Box(modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    color = colorResource(R.color.gray_main)
                )
            ){
                Image(
                    painter = painterResource(
                        if (selectedCurrency == "USD") R.drawable.ic_dollar_circle
                        else R.drawable.ic_euro_circle
                    ),
                    contentDescription = "Icono de Moneda",
                    modifier = Modifier.padding(top = 5.dp, start = 6.dp)
                )
            }

            Spacer(modifier = Modifier
                .width(4.dp))

            Text(text = selectedCurrency,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier
                .width(2.dp))

            //icono de flecha
            Box(modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color = Color.Transparent)
            ){
                Image(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = "Desplegar Menu"
                )
            }
        }

        //Menu Desplegable
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = {isExpanded = false}, // Cierra el menú al tocar fuera
            modifier = Modifier.width(220.dp)
        ) {
            //Opciones del Menu
            DropdownMenuItem(
                onClick = {
                    onCurrencySelected("USD")
                    isExpanded = false
                }
            ) {
                Text("Dólares Americanos (USD)")
            }
            DropdownMenuItem(
                onClick = {
                    onCurrencySelected("EUR")
                    isExpanded = false
                }
            ) {
                Text("Euros (EUR)")
            }
        }
    }
}

@Composable
fun DropDownMenuPlatform(
    selectedPlatform: String,
    availablePlatforms: List<String>,
    onPlatformSelected: (String) -> Unit
) {
    // Estado para controlar si el menú está abierto o cerrado
    var isExpanded by remember { mutableStateOf(false) }

    Box(){
        // Botón que activar el menú
        Button(onClick = { isExpanded = true },
            colors = ButtonDefaults.buttonColors(PurpleDarkTrans),
            contentPadding = PaddingValues(8.dp),
            shape = CircleShape,
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
        ) {

            //icono de Banco
            Box(modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    color = colorResource(R.color.gray_main)
                )
            ){
                Image(
                    painter = painterResource(R.drawable.ic_bank),
                    contentDescription = "Icono de Banco",
                    modifier = Modifier.padding(top = 5.dp, start = 6.dp)
                )
            }

            Spacer(modifier = Modifier
                .width(4.dp))

            Text(text = selectedPlatform, style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = Color.White
                )
            )

            Spacer(modifier = Modifier
                .width(2.dp))

            //icono de flecha
            Box(modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    color = Color.Transparent
                )
            ){
                Image(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = "Desplegar Menu"
                )
            }
        }

        //Menu Desplegable
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = {isExpanded = false}, // Cierra el menú al tocar fuera
            modifier = Modifier.width(220.dp)
        ) {
            availablePlatforms.forEach { platform ->
                DropdownMenuItem(
                    content = {
                        Text(
                            when(platform){
                                "BCV" -> "Banco Central de Venezuela"
                                "MNT" -> "Monitor/Paralelo"
                                else -> platform
                            }
                        )
                    },
                    onClick = {
                        onPlatformSelected(platform)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingDialog(
    show: Boolean
) {
    if (show) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colors.surface,
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .width(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    //ProgressBar
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp),
                        color = colorResource(R.color.purple_main),
                        strokeWidth = 4.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Texto "Cargando..."
                    Text(
                        text = stringResource(R.string.loading_rates),
                        style = MaterialTheme.typography.h6,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    if (!errorMessage.isNullOrEmpty()){
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.error_title),
                    style = MaterialTheme.typography.h6,
                    color = Color.Red
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.h6
                )
            },
            confirmButton = {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(R.color.purple_main)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.retry_button),
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = stringResource(R.string.close_button)
                    )
                }
            },
            shape = RoundedCornerShape(8.dp),
            backgroundColor = MaterialTheme.colors.surface
        )
    }
}