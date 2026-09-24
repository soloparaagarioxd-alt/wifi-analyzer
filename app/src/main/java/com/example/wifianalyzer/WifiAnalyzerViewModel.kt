package com.example.wifianalyzer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Modelo que representa una red Wi-Fi detectada.
 */
data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val signalLevel: Int,
    val capabilities: String
)

/**
 * Estados del escáner de redes Wi-Fi.
 */
sealed interface WifiScanState {
    object Idle : WifiScanState
    object Scanning : WifiScanState
    data class Success(val networks: List<WifiNetwork>) : WifiScanState
    data class Error(val message: String) : WifiScanState
}

class WifiAnalyzerViewModel : ViewModel() {

    private val _ssidInput = MutableStateFlow("")
    val ssidInput: StateFlow<String> = _ssidInput.asStateFlow()

    private val _macInput = MutableStateFlow("")
    val macInput: StateFlow<String> = _macInput.asStateFlow()

    private val _scanState = MutableStateFlow<WifiScanState>(WifiScanState.Idle)
    val scanState: StateFlow<WifiScanState> = _scanState.asStateFlow()

    // El resultado del algoritmo se actualiza reactivamente en tiempo real
    val analysisResult: StateFlow<AnalysisResult> = combine(_ssidInput, _macInput) { ssid, mac ->
        AlgorithmHelper.process(ssid, mac)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalysisResult.Empty
    )

    fun onSsidChanged(newSsid: String) {
        _ssidInput.value = newSsid
    }

    fun onMacChanged(newMac: String) {
        _macInput.value = newMac
    }

    fun onNetworkSelected(network: WifiNetwork) {
        _ssidInput.value = network.ssid
        _macInput.value = network.bssid
    }

    fun clearInputs() {
        _ssidInput.value = ""
        _macInput.value = ""
    }

    /**
     * Inicia el escaneo de redes Wi-Fi mediante WifiManager y BroadcastReceiver.
     */
    @SuppressLint("MissingPermission")
    fun startWifiScan(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            _scanState.value = WifiScanState.Error("WifiManager no está disponible en este dispositivo.")
            return
        }

        if (!wifiManager.isWifiEnabled) {
            _scanState.value = WifiScanState.Error("El Wi-Fi está desactivado. Por favor, actívalo para escanear.")
            return
        }

        _scanState.value = WifiScanState.Scanning

        viewModelScope.launch {
            try {
                // Registrar un BroadcastReceiver temporal para escuchar los resultados del escaneo
                val wifiScanReceiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context, intent: Intent) {
                        val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                        try {
                            context.unregisterReceiver(this)
                        } catch (ignored: Exception) {}

                        processScanResults(wifiManager, success)
                    }
                }

                val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(wifiScanReceiver, intentFilter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(wifiScanReceiver, intentFilter)
                }

                // Iniciar escaneo
                val started = wifiManager.startScan()
                if (!started) {
                    // Si startScan() fue bloqueado por throttling de Android (máximo 4 escaneos cada 2 minutos en primer plano),
                    // leemos los últimos resultados ya almacenados en caché.
                    processScanResults(wifiManager, isThrottled = true)
                }
            } catch (e: SecurityException) {
                _scanState.value = WifiScanState.Error("Permisos insuficientes: ${e.localizedMessage}")
            } catch (e: Exception) {
                _scanState.value = WifiScanState.Error("Error al escanear: ${e.localizedMessage}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun processScanResults(wifiManager: WifiManager, isThrottled: Boolean = false) {
        try {
            val results = wifiManager.scanResults
            val networkList = results
                .filter { !it.SSID.isNullOrBlank() }
                .distinctBy { it.BSSID }
                .map { scanResult ->
                    WifiNetwork(
                        ssid = scanResult.SSID.orEmpty(),
                        bssid = scanResult.BSSID.orEmpty(),
                        signalLevel = scanResult.level,
                        capabilities = scanResult.capabilities.orEmpty()
                    )
                }
                .sortedByDescending { it.signalLevel }

            if (networkList.isEmpty()) {
                _scanState.value = WifiScanState.Error(
                    if (isThrottled) "No se encontraron redes (posible limitación de escaneo del sistema). Asegúrate de tener la Ubicación (GPS) activada."
                    else "No se detectaron redes Wi-Fi cercanas con SSID visible."
                )
            } else {
                _scanState.value = WifiScanState.Success(networkList)
            }
        } catch (e: Exception) {
            _scanState.value = WifiScanState.Error("Error al leer resultados de escaneo: ${e.localizedMessage}")
        }
    }
}
