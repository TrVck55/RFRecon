package com.rf.recon

data class WifiObservation(
    val ssid: String, val bssid: String, val rssi: Int, val frequencyMhz: Int,
    val capabilities: String, val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null, val longitude: Double? = null
) {
    val channel: Int get() = when {
        frequencyMhz == 2484 -> 14
        frequencyMhz in 2412..2472 -> (frequencyMhz - 2407) / 5
        frequencyMhz in 5000..5895 -> (frequencyMhz - 5000) / 5
        frequencyMhz in 5955..7115 -> (frequencyMhz - 5950) / 5
        else -> -1
    }
    val security: String get() = when {
        capabilities.contains("WPA3", true) || capabilities.contains("SAE", true) -> "WPA3"
        capabilities.contains("WPA2", true) -> "WPA2"
        capabilities.contains("WPA", true) -> "WPA"
        capabilities.contains("WEP", true) -> "WEP"
        capabilities.isBlank() || capabilities == "[ESS]" -> "OPEN"
        else -> "UNKNOWN"
    }
}

data class BleObservation(
    val address: String, val name: String?, val rssi: Int, val txPower: Int?,
    val manufacturerData: String, val serviceUuids: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null, val longitude: Double? = null
)

data class GpsPoint(val latitude: Double, val longitude: Double, val accuracy: Float, val timestamp: Long)
