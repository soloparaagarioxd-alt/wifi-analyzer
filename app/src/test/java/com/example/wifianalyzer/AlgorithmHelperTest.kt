package com.example.wifianalyzer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgorithmHelperTest {

    @Test
    fun testUserProvidedExample() {
        val ssid = "Personal-E60"
        val mac = "20:35:43:2F:4E:65"

        val result = AlgorithmHelper.process(ssid, mac)

        assertTrue(result is AnalysisResult.Success)
        val success = result as AnalysisResult.Success

        assertEquals("2035432F4E65", success.normalizedMac)
        assertEquals("35432F4E65", success.macWithoutFirstTwo)
        assertEquals("E60", success.extractedSuffix)
        assertEquals("35432F4", success.preservedPrefix)
        assertEquals("35432F4E60", success.derivedString)
    }

    @Test
    fun testMacWithoutSeparatorsAndLowerCase() {
        val ssid = "Fibertel-Wi-Fi-9A2"
        val mac = "aa:bb:cc:dd:ee:ff"

        val result = AlgorithmHelper.process(ssid, mac)

        assertTrue(result is AnalysisResult.Success)
        val success = result as AnalysisResult.Success

        assertEquals("AABBCCDDEEFF", success.normalizedMac)
        assertEquals("BBCCDDEEFF", success.macWithoutFirstTwo)
        assertEquals("9A2", success.extractedSuffix) // Sufijo después del último guión
        assertEquals("BBCCDDE9A2", success.derivedString)
    }

    @Test
    fun testInvalidMac() {
        val result = AlgorithmHelper.process("MiRed-123", "20:35:43:2F:4E") // solo 10 chars
        assertTrue(result is AnalysisResult.Error)
    }

    @Test
    fun testMissingDashInSsid() {
        val result = AlgorithmHelper.process("MiRedSinGuion", "20:35:43:2F:4E:65")
        assertTrue(result is AnalysisResult.Error)
    }

    @Test
    fun testEmptyInput() {
        val result = AlgorithmHelper.process("", "")
        assertTrue(result is AnalysisResult.Empty)
    }
}
