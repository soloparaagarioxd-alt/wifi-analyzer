package com.example.wifianalyzer

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val viewModel: WifiAnalyzerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1E88E5),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFD1E4FF),
                    onPrimaryContainer = Color(0xFF001D36),
                    secondary = Color(0xFF00897B),
                    surface = Color(0xFFFBFDFD),
                    error = Color(0xFFBA1A1A),
                    errorContainer = Color(0xFFFFDAD6)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WifiAnalyzerApp(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Función auxiliar para copiar texto al portapapeles del sistema y emitir un Toast.
 */
fun copyToClipboard(context: Context, label: String, text: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiAnalyzerApp(viewModel: WifiAnalyzerViewModel) {
    val context = LocalContext.current

    val ssidInput by viewModel.ssidInput.collectAsState()
    val macInput by viewModel.macInput.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()

    // Lista de permisos requeridos según la versión de Android
    val requiredPermissions = remember {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        permissions.toTypedArray()
    }

    // Launcher nativo de Compose para solicitar múltiples permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val fineLocationGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val nearbyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsMap[Manifest.permission.NEARBY_WIFI_DEVICES] ?: false
        } else true

        if (fineLocationGranted || nearbyGranted) {
            viewModel.startWifiScan(context)
        } else {
            Toast.makeText(
                context,
                "Se requieren permisos de ubicación y dispositivos Wi-Fi cercanos para listar redes",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestScan() {
        val allGranted = requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            viewModel.startWifiScan(context)
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Wifi Analyzer",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // SECCIÓN 1: ESCÁNER DE REDES WI-FI
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Escáner de Redes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Detecta puntos de acceso cercanos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { requestScan() },
                                enabled = scanState !is WifiScanState.Scanning
                            ) {
                                if (scanState is WifiScanState.Scanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Escaneando...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Escanear"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Escanear")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Feedback de estado de escaneo
                        when (val state = scanState) {
                            is WifiScanState.Idle -> {
                                Text(
                                    text = "Presiona 'Escanear' para buscar redes Wi-Fi cercanas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is WifiScanState.Scanning -> {
                                Text(
                                    text = "Buscando señales Wi-Fi (asegúrate de tener el GPS activado)...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            is WifiScanState.Error -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            is WifiScanState.Success -> {
                                Text(
                                    text = "${state.networks.size} redes detectadas. Toca una para transferir datos al formulario:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // LISTA DE REDES DETECTADAS
            if (scanState is WifiScanState.Success) {
                val networks = (scanState as WifiScanState.Success).networks
                items(networks) { network ->
                    WifiNetworkCard(
                        network = network,
                        onSelect = { viewModel.onNetworkSelected(network) },
                        onCopySsid = { copyToClipboard(context, "SSID", network.ssid) },
                        onCopyMac = { copyToClipboard(context, "BSSID (MAC)", network.bssid) }
                    )
                }
            }

            // SECCIÓN 2: FORMULARIO DE ANÁLISIS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Formulario de Análisis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row {
                                TextButton(
                                    onClick = {
                                        // Rellenar con los datos del ejemplo para validación rápida
                                        viewModel.onSsidChanged("Personal-E60")
                                        viewModel.onMacChanged("20:35:43:2F:4E:65")
                                    }
                                ) {
                                    Text("Ejemplo", fontSize = 12.sp)
                                }

                                IconButton(onClick = { viewModel.clearInputs() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar campos")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Campo SSID
                        OutlinedTextField(
                            value = ssidInput,
                            onValueChange = { viewModel.onSsidChanged(it) },
                            label = { Text("SSID de la Red") },
                            placeholder = { Text("Ej: Personal-E60") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { copyToClipboard(context, "SSID", ssidInput) }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar SSID",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Campo MAC
                        OutlinedTextField(
                            value = macInput,
                            onValueChange = { viewModel.onMacChanged(it) },
                            label = { Text("Dirección MAC / BSSID") },
                            placeholder = { Text("Ej: 20:35:43:2F:4E:65 o 2035432F4E65") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { copyToClipboard(context, "MAC", macInput) }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar MAC",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // SECCIÓN 3: TARJETA DE DESGLOSE DEL ALGORITMO
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Desglose del Algoritmo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        when (val result = analysisResult) {
                            is AnalysisResult.Empty -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Ingresa los datos o toca una red para calcular la cadena derivada en tiempo real.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is AnalysisResult.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = result.message,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            is AnalysisResult.Success -> {
                                AlgorithmBreakdownContent(
                                    result = result,
                                    onCopyDerivedString = {
                                        copyToClipboard(context, "Cadena Derivada", result.derivedString)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Componente que renderiza cada red Wi-Fi detectada con botones de copiado y selección.
 */
@Composable
fun WifiNetworkCard(
    network: WifiNetwork,
    onSelect: () -> Unit,
    onCopySsid: () -> Unit,
    onCopyMac: () -> Unit
) {
    val isPersonal = network.ssid.startsWith("personal", ignoreCase = true)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        border = if (isPersonal) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        colors = if (isPersonal) CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ) else CardDefaults.elevatedCardColors()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (isPersonal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    if (isPersonal) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(bottom = 3.dp)
                        ) {
                            Text(
                                text = "PERSONAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = network.ssid,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isPersonal) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isPersonal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = network.bssid,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Señal: ${network.signalLevel} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row {
                IconButton(onClick = onCopySsid) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar SSID",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onCopyMac) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar MAC",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

/**
 * Componente que muestra el desglose del algoritmo paso a paso y la cadena final resaltada.
 */
@Composable
fun AlgorithmBreakdownContent(
    result: AnalysisResult.Success,
    onCopyDerivedString: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // PASO 1
        BreakdownStepItem(
            stepNumber = "1",
            title = "MAC normalizada (sin los 2 primeros caracteres):",
            value = result.macWithoutFirstTwo,
            detail = "Original: '${result.normalizedMac}' -> Recortada: '${result.macWithoutFirstTwo}' (${result.macWithoutFirstTwo.length} caracteres)"
        )

        // PASO 2
        BreakdownStepItem(
            stepNumber = "2",
            title = "Sufijo extraído del SSID:",
            value = result.extractedSuffix,
            detail = "Texto posterior al último guión '-' en '${result.originalSsid}' (longitud N = ${result.extractedSuffix.length})"
        )

        // PASO 3
        BreakdownStepItem(
            stepNumber = "3",
            title = "Operación de sustitución posicional:",
            value = "${result.preservedPrefix} + [${result.extractedSuffix}]",
            detail = result.substitutionExplanation
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

        // RESULTADO FINAL
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CADENA DERIVADA FINAL",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = result.derivedString,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onCopyDerivedString,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar resultado"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copiar Cadena Derivada",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BreakdownStepItem(
    stepNumber: String,
    title: String,
    value: String,
    detail: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(text = stepNumber, modifier = Modifier.padding(2.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp)
        )

        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 2.dp)
        )
    }
}
