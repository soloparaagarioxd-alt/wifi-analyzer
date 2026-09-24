package com.example.wifianalyzer

/**
 * Representa el resultado del análisis y transformación del algoritmo.
 */
sealed interface AnalysisResult {
    data class Success(
        val originalSsid: String,
        val originalMac: String,
        val normalizedMac: String,
        val macWithoutFirstTwo: String,
        val extractedSuffix: String,
        val preservedPrefix: String,
        val substitutionExplanation: String,
        val derivedString: String
    ) : AnalysisResult

    data class Error(val message: String) : AnalysisResult

    object Empty : AnalysisResult
}

/**
 * Objeto utilitario con la función pura de transformación lógica de SSID y MAC.
 */
object AlgorithmHelper {

    /**
     * Aplica las reglas de transformación:
     * 1. Normalizar MAC (quitar ':' y '-', pasar a mayúsculas, verificar 12 chars hex).
     * 2. Omitir los 2 primeros dígitos (quedan 10 caracteres).
     * 3. Extraer el sufijo del SSID (lo que va después del último '-').
     * 4. Sustitución posicional (reemplazar los últimos N caracteres de la MAC recortada con el sufijo).
     * 5. Retornar desglose completo o error descriptivo.
     */
    fun process(ssid: String, mac: String): AnalysisResult {
        val trimmedSsid = ssid.trim()
        val trimmedMac = mac.trim()

        if (trimmedSsid.isEmpty() && trimmedMac.isEmpty()) {
            return AnalysisResult.Empty
        }

        // 1. Normalizar la MAC: quitar caracteres ':' y '-', convertir a mayúsculas
        val cleanMac = trimmedMac.replace(":", "").replace("-", "").uppercase()
        val hexRegex = Regex("^[0-9A-F]{12}$")
        if (!cleanMac.matches(hexRegex)) {
            return AnalysisResult.Error(
                "La dirección MAC/BSSID ingresada no es válida. Debe contener exactamente 12 caracteres hexadecimales (0-9, A-F). Valor actual: '$cleanMac' (${cleanMac.length} caracteres)."
            )
        }

        // 2. Omitir los 2 primeros dígitos (quedan 10 caracteres)
        val macWithoutFirstTwo = cleanMac.substring(2)

        // 3. Extraer Sufijo del SSID (después del último guión '-')
        val lastDashIndex = trimmedSsid.lastIndexOf('-')
        if (lastDashIndex == -1 || lastDashIndex == trimmedSsid.length - 1) {
            return AnalysisResult.Error(
                "El SSID no contiene un guión '-' válido o no tiene texto después del guión (ejemplo esperado: 'Personal-E60')."
            )
        }

        val suffix = trimmedSsid.substring(lastDashIndex + 1).uppercase()
        if (suffix.length > macWithoutFirstTwo.length) {
            return AnalysisResult.Error(
                "La longitud del sufijo ('$suffix', ${suffix.length} caracteres) no puede exceder el tamaño de la MAC procesada (${macWithoutFirstTwo.length} caracteres)."
            )
        }

        // 4. Sustitución Posicional:
        // Tomar la MAC recortada (10 caracteres) y reemplazar sus últimos N caracteres por el sufijo extraído.
        val n = suffix.length
        val preservedPrefix = macWithoutFirstTwo.substring(0, macWithoutFirstTwo.length - n)
        val derivedString = preservedPrefix + suffix

        val explanation = "Se conservan los primeros ${preservedPrefix.length} caracteres ('$preservedPrefix') de la MAC recortada y se sustituyen los últimos $n con el sufijo ('$suffix')."

        return AnalysisResult.Success(
            originalSsid = trimmedSsid,
            originalMac = trimmedMac,
            normalizedMac = cleanMac,
            macWithoutFirstTwo = macWithoutFirstTwo,
            extractedSuffix = suffix,
            preservedPrefix = preservedPrefix,
            substitutionExplanation = explanation,
            derivedString = derivedString
        )
    }
}
