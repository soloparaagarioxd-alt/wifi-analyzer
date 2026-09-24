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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

// ─── Paleta de colores del tema terminal ───────────────────────────────────
private val BgPrimary     = Color(0xFF0D0D0D)   // fondo general
private val BgCard        = Color(0xFF161616)   // fondo de tarjetas
private val BgInput       = Color(0xFF1A1A1A)   // fondo de inputs
private val BorderColor   = Color(0xFF2A2A2A)   // bordes sutiles
private val GreenNeon     = Color(0xFF00FF41)   // verde neón principal
private val GreenDim      = Color(0xFF2ECC71)   // verde suave para labels
private val TextPrimary   = Color(0xFFE0E0E0)   // texto principal
private val TextMuted     = Color(0xFF666666)   // texto apagado
private val TextLabel     = Color(0xFF888888)   // etiquetas secundarias
private val AccentWhite   = Color(0xFFFFFFFF)   // blanco para destacar
private val WarnYellow    = Color(0xFFFFD700)   // amarillo advertencia
private val ErrorRed      = Color(0xFFFF3B30)   // error

class MainActivity : ComponentActivity() {
    private val viewModel: WifiAnalyzerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BgPrimary,
                    surface = BgCard,
                    primary = GreenNeon,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgPrimary
                ) {
                    WifiAnalyzerApp(viewModel = viewModel)
                }
            }
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
}

// ─── Componente de sección de título con prefijo // ────────────────────────
@Composable
fun SectionHeader(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "// $title",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextLabel,
            letterSpacing = 1.5.sp
        )
        trailing?.invoke()
    }
}

// ─── Caja estilo terminal ──────────────────────────────────────────────────
@Composable
fun TerminalBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
            .background(BgCard, RoundedCornerShape(4.dp))
            .padding(14.dp),
        content = content
    )
}

// ─── Input de terminal ─────────────────────────────────────────────────────
@Composable
fun TerminalInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TextLabel,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                .background(BgInput, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(GreenNeon),
                singleLine = true,
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                    inner()
                }
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar",
                    tint = TextLabel,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Pantalla principal ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiAnalyzerApp(viewModel: WifiAnalyzerViewModel) {
    val context = LocalContext.current

    val ssidInput    by viewModel.ssidInput.collectAsState()
    val macInput     by viewModel.macInput.collectAsState()
    val scanState    by viewModel.scanState.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()

    val requiredPermissions = remember {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        perms.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        val granted = map[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        map[Manifest.permission.NEARBY_WIFI_DEVICES] == true)
        if (granted) viewModel.startWifiScan(context)
        else Toast.makeText(context, "Se requieren permisos de ubicación", Toast.LENGTH_LONG).show()
    }

    fun requestScan() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) viewModel.startWifiScan(context) else permissionLauncher.launch(requiredPermissions)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .systemBarsPadding()
    ) {
        // ── TOP BAR ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(1.dp, TextLabel, RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▣", fontSize = 12.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "WIFI SCANNER",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AccentWhite,
                    letterSpacing = 2.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { requestScan() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar",
                        tint = if (scanState is WifiScanState.Scanning) GreenNeon else TextLabel,
                        modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, TextLabel, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null,
                        tint = TextLabel, modifier = Modifier.size(18.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {

            // ── STATUS BLOCK ─────────────────────────────────────────────
            item {
                TerminalBox {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "// STATUS:",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextLabel,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        // Indicador de estado en vivo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(GreenNeon, RoundedCornerShape(50))
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (scanState) {
                                is WifiScanState.Scanning -> "SCANNING..."
                                is WifiScanState.Success  -> "SCAN_COMPLETE"
                                is WifiScanState.Error    -> "SCAN_ERROR"
                                else                       -> "IDLE_MONITOR"
                            },
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "LIVE_FEED",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GreenNeon,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Botón escanear
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TextPrimary, RoundedCornerShape(4.dp))
                            .background(if (scanState is WifiScanState.Scanning) Color(0xFF1A1A1A) else Color.Transparent, RoundedCornerShape(4.dp))
                            .clickable(enabled = scanState !is WifiScanState.Scanning) { requestScan() }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (scanState is WifiScanState.Scanning) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = GreenNeon,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "SCANNING_NETWORKS...",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenNeon,
                                    letterSpacing = 1.sp
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("◎  ", fontSize = 14.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "ESCANEAR REDES WI-FI",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    }

                    // Error de escaneo
                    if (scanState is WifiScanState.Error) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "> ERROR: ${(scanState as WifiScanState.Error).message}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ErrorRed
                        )
                    }
                }
            }

            // ── NEARBY APS ───────────────────────────────────────────────
            if (scanState is WifiScanState.Success || scanState is WifiScanState.Idle || scanState is WifiScanState.Error) {
                item {
                    val count = if (scanState is WifiScanState.Success)
                        (scanState as WifiScanState.Success).networks.size else 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "// NEARBY_APS ($count)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextLabel,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "CH: AUTO",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            if (scanState is WifiScanState.Success) {
                val networks = (scanState as WifiScanState.Success).networks
                items(networks) { network ->
                    TerminalNetworkCard(
                        network = network,
                        onSelect = { viewModel.onNetworkSelected(network) },
                        onCopySsid = { copyToClipboard(context, "SSID", network.ssid) },
                        onCopyMac = { copyToClipboard(context, "MAC", network.bssid) }
                    )
                }
            }

            // Separador entre secciones
            if (scanState is WifiScanState.Success) {
                item { Spacer(Modifier.height(4.dp)) }
            }

            // ── ANALYSIS MODULE ──────────────────────────────────────────
            item {
                TerminalBox {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "// ANALYSIS_MODULE",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextLabel,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.dp, TextLabel, RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", fontSize = 10.sp, color = TextLabel, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    TerminalInput(
                        value = ssidInput,
                        onValueChange = { viewModel.onSsidChanged(it) },
                        label = "TARGET SSID",
                        placeholder = "Ingresa el SSID...",
                        onCopy = { copyToClipboard(context, "SSID", ssidInput) }
                    )

                    Spacer(Modifier.height(12.dp))

                    TerminalInput(
                        value = macInput,
                        onValueChange = { viewModel.onMacChanged(it) },
                        label = "TARGET MAC ADDRESS",
                        placeholder = "XX:XX:XX:XX:XX:XX",
                        onCopy = { copyToClipboard(context, "MAC", macInput) }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Botón ejecutar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentWhite, RoundedCornerShape(4.dp))
                            .background(Color(0xFF1C1C1C), RoundedCornerShape(4.dp))
                            .clickable {
                                // Pre-cargar ejemplo si está vacío
                                if (ssidInput.isBlank() && macInput.isBlank()) {
                                    viewModel.onSsidChanged("Personal-E60")
                                    viewModel.onMacChanged("20:35:43:2F:4E:65")
                                }
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡  ", fontSize = 13.sp, color = AccentWhite, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "EJECUTAR ALGORITMO",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = AccentWhite,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ── ALGORITHM OUTPUT
                    Text(
                        text = "// ALGORITHM_OUTPUT",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextLabel,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    when (val result = analysisResult) {
                        is AnalysisResult.Empty -> {
                            TerminalOutputBox {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "-- WAITING_EXECUTION --",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted,
                                        letterSpacing = 1.sp
                                    )
                                    Icon(Icons.Default.ContentCopy, contentDescription = null,
                                        tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        is AnalysisResult.Error -> {
                            TerminalOutputBox {
                                Text(
                                    text = "> ERR: ${result.message}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ErrorRed
                                )
                            }
                        }

                        is AnalysisResult.Success -> {
                            // Desglose compacto estilo terminal
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                    .background(BgInput, RoundedCornerShape(4.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TerminalOutputLine("> MAC_CLEAN:", result.normalizedMac)
                                TerminalOutputLine("> MAC_TRIM: ", result.macWithoutFirstTwo)
                                TerminalOutputLine("> SUFFIX:   ", result.extractedSuffix)
                                TerminalOutputLine("> PREFIX:   ", result.preservedPrefix)

                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(color = BorderColor)
                                Spacer(Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row {
                                        Text(
                                            text = "> DERIVED:  ",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = result.derivedString,
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GreenNeon,
                                            letterSpacing = 1.5.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { copyToClipboard(context, "Cadena Derivada", result.derivedString) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar resultado",
                                            tint = GreenNeon, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Botón de copiar resultado grande
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GreenNeon, RoundedCornerShape(4.dp))
                                    .background(Color(0xFF001A0A), RoundedCornerShape(4.dp))
                                    .clickable { copyToClipboard(context, "Cadena Derivada", result.derivedString) }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null,
                                        tint = GreenNeon, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "COPIAR_RESULTADO",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenNeon,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─── Caja de output de terminal ────────────────────────────────────────────
@Composable
fun TerminalOutputBox(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
            .background(BgInput, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content
    )
}

// ─── Línea de output del algoritmo ─────────────────────────────────────────
@Composable
fun TerminalOutputLine(label: String, value: String) {
    Row {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Tarjeta de red Wi-Fi estilo terminal ──────────────────────────────────
@Composable
fun TerminalNetworkCard(
    network: WifiNetwork,
    onSelect: () -> Unit,
    onCopySsid: () -> Unit,
    onCopyMac: () -> Unit
) {
    val isPersonal = network.ssid.startsWith("personal", ignoreCase = true)
    val signalColor = when {
        network.signalLevel >= -55 -> GreenNeon
        network.signalLevel >= -70 -> GreenDim
        else                       -> WarnYellow
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPersonal) 1.5.dp else 1.dp,
                color = if (isPersonal) GreenNeon else BorderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .background(
                if (isPersonal) Color(0xFF041A0A) else BgCard,
                RoundedCornerShape(4.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        // Fila superior: SSID + señal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (isPersonal) GreenNeon else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = network.ssid,
                    fontSize = if (isPersonal) 14.sp else 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isPersonal) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isPersonal) GreenNeon else TextPrimary,
                    letterSpacing = if (isPersonal) 0.5.sp else 0.sp
                )
            }
            Text(
                text = "${network.signalLevel} dBm",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = signalColor
            )
        }

        Spacer(Modifier.height(5.dp))

        // Fila inferior: MAC + seguridad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MAC: ",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Text(
                    text = network.bssid.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextLabel,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tipo de seguridad
                val security = when {
                    network.capabilities.contains("WPA3") -> "WPA3"
                    network.capabilities.contains("WPA2") -> "WPA2"
                    network.capabilities.contains("WPA")  -> "WPA"
                    network.capabilities.contains("WEP")  -> "WEP"
                    else -> "OPEN"
                }
                Text(
                    text = security,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onCopyMac, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar MAC",
                        tint = TextMuted, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}
