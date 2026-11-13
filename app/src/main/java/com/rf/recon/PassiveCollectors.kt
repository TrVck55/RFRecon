package com.rf.recon

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.UUID

class WifiCollector(private val context: Context) {
    @SuppressLint("MissingPermission")
    fun snapshot(): List<WifiObservation> {
        val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return emptyList()
        return try {
            manager.scanResults.map {
                WifiObservation(
                    ssid = it.SSID.orEmpty().ifBlank { "<hidden>" },
                    bssid = it.BSSID.orEmpty(),
                    rssi = it.level,
                    frequencyMhz = it.frequency,
                    capabilities = it.capabilities.orEmpty()
                )
            }
        } catch (_: SecurityException) { emptyList() }
    }

    @SuppressLint("MissingPermission")
    fun requestScan(): Boolean {
        val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try { manager.startScan() } catch (_: SecurityException) { false }
    }
}

class BleCollector(private val context: Context) {
    private val adapter: BluetoothAdapter? get() = BluetoothAdapter.getDefaultAdapter()
    private var scanner: BluetoothLeScanner? = null
    private var callback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    fun start(onObservation: (BleObservation) -> Unit): Boolean {
        val a = adapter ?: return false
        if (!a.isEnabled) return false
        scanner = a.bluetoothLeScanner ?: return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 31) return false

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord
                val manufacturer = record?.manufacturerSpecificData
                val manufacturerText = if (manufacturer == null) "" else buildString {
                    for (i in 0 until manufacturer.size()) {
                        append("%04X:".format(manufacturer.keyAt(i)))
                        append(manufacturer.valueAt(i).joinToString("") { "%02X".format(it) })
                        append(";")
                    }
                }
                val uuids = record?.serviceUuids?.joinToString(",") { it.uuid.toString() }.orEmpty()
                onObservation(
                    BleObservation(
                        address = result.device.address,
                        name = record?.deviceName ?: result.device.name,
                        rssi = result.rssi,
                        txPower = record?.txPowerLevel,
                        manufacturerData = manufacturerText,
                        serviceUuids = uuids
                    )
                )
            }
        }
        callback = cb
        return try {
            scanner?.startScan(null, ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build(), cb)
            true
        } catch (_: SecurityException) { false }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        try { callback?.let { scanner?.stopScan(it) } } catch (_: SecurityException) {}
        callback = null
    }
}
