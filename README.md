# Wifi Analyzer (Android + Jetpack Compose)

Aplicación nativa de Android desarrollada en **Kotlin** con **Jetpack Compose (Material Design 3)**, **ViewModels** y **Coroutines / StateFlow**, que permite escanear redes Wi-Fi cercanas y calcular derivaciones de cadenas lógicas a partir del SSID y la MAC (BSSID).

## Estructura del Proyecto

* **[MainActivity.kt](file:///C:/Users/PC/.gemini/antigravity/scratch/wifi-analyzer/app/src/main/java/com/example/wifianalyzer/MainActivity.kt)**: UI reactiva en Compose (Escáner de redes, lista interactiva, formulario y desglose del algoritmo).
* **[WifiAnalyzerViewModel.kt](file:///C:/Users/PC/.gemini/antigravity/scratch/wifi-analyzer/app/src/main/java/com/example/wifianalyzer/WifiAnalyzerViewModel.kt)**: Manejo del ciclo de vida, llamadas a `WifiManager` y `BroadcastReceiver`, y publicación de flujos (`StateFlow`).
* **[AlgorithmHelper.kt](file:///C:/Users/PC/.gemini/antigravity/scratch/wifi-analyzer/app/src/main/java/com/example/wifianalyzer/AlgorithmHelper.kt)**: Función pura que implementa la transformación de texto y validaciones paso a paso.
* **[AndroidManifest.xml](file:///C:/Users/PC/.gemini/antigravity/scratch/wifi-analyzer/app/src/main/AndroidManifest.xml)**: Declaración de permisos de ubicación, Wi-Fi y dispositivos cercanos.
* **[AlgorithmHelperTest.kt](file:///C:/Users/PC/.gemini/antigravity/scratch/wifi-analyzer/app/src/test/java/com/example/wifianalyzer/AlgorithmHelperTest.kt)**: Pruebas unitarias para validar las transformaciones lógicas.

---

## Permisos y Consideraciones de Android

Para poder escanear redes Wi-Fi y obtener el SSID y BSSID real (y no valores anónimos como `02:00:00:00:00:00` o `<unknown ssid>`), Android impone restricciones de privacidad:

1. **Permiso de Ubicación Precisa (`ACCESS_FINE_LOCATION`)**: Requerido desde Android 6.0 hasta Android 12, ya que los BSSID permiten inferir la ubicación geográfica física.
2. **Permiso de Dispositivos Cercanos (`NEARBY_WIFI_DEVICES`)**: Requerido en Android 13+ (API 33).
3. **Servicios de Ubicación (GPS)**: El dispositivo debe tener el interruptor de Ubicación encendido al momento de escanear.
4. **Limitación de Frecuencia (Scan Throttling)**: A partir de Android 9 (Pie), el sistema operativo limita las llamadas a `startScan()` en aplicaciones en primer plano a **4 veces cada 2 minutos**. Si se excede, el ViewModel detecta la limitación y retorna los resultados almacenados en el caché del sistema.
