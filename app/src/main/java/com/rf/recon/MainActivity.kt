package com.rf.recon

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Bg = Color(0xFF080D12)
private val Panel = Color(0xFF111B22)
private val Green = Color(0xFF39FF88)
private val Cyan = Color(0xFF32D9FF)
private val Muted = Color(0xFF82939D)

class MainActivity : ComponentActivity() {
    private lateinit var wifi: WifiCollector
    private lateinit var ble: BleCollector
    private lateinit var db: RfDatabase
    private val wifiRows = mutableStateListOf<WifiObservation>()
    private val bleRows = mutableStateListOf<BleObservation>()
    private val gpsRows = mutableStateListOf<GpsPoint>()
    private var status by mutableStateOf("READY")
    private var scanning by mutableStateOf(false)
    private var selected by mutableStateOf("OVERVIEW")
    private var lastLocation: Location? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { status = if (it.values.all { v -> v }) "PERMISSIONS GRANTED" else "PERMISSIONS PARTIAL" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifi = WifiCollector(this); ble = BleCollector(this); db = RfDatabase(this)
        setContent { RfReconApp() }
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val wanted = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (wanted.isNotEmpty()) permissionLauncher.launch(wanted.toTypedArray())
    }

    private fun startObservation() {
        if (scanning) return
        scanning = true; status = "PASSIVE OBSERVATION ACTIVE"
        wifi.requestScan()
        wifiRows.clear(); wifiRows.addAll(wifi.snapshot())
        wifiRows.forEach { db.save(it.copy(latitude = lastLocation?.latitude, longitude = lastLocation?.longitude)) }
        val started = ble.start { item ->
            runOnUiThread {
                val tagged = item.copy(latitude = lastLocation?.latitude, longitude = lastLocation?.longitude)
                bleRows.removeAll { it.address == tagged.address }
                bleRows.add(0, tagged)
                db.save(tagged)
            }
        }
        if (!started) status = "BLE UNAVAILABLE / PERMISSION REQUIRED"
        startGps()
    }

    private fun stopObservation() {
        ble.stop(); scanning = false; status = "OBSERVATION PAUSED"
    }

    @Suppress("MissingPermission")
    private fun startGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 2f, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lastLocation = location
                    gpsRows.add(GpsPoint(location.latitude, location.longitude, location.accuracy, location.time))
                }
                @Deprecated("Deprecated in Java") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            })
        } catch (_: Exception) { status = "GPS UNAVAILABLE" }
    }

    @Composable
    private fun RfReconApp() {
        MaterialTheme(colorScheme = darkColorScheme(
            primary = Green, secondary = Cyan, background = Bg, surface = Panel,
            onBackground = Color(0xFFE5EDF1), onSurface = Color(0xFFE5EDF1)
        )) {
            Column(Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("RF//RECON", color = Green, fontSize = 25.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Text("PASSIVE RF INTELLIGENCE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    Text("● ${if (scanning) "LIVE" else "IDLE"}", color = if (scanning) Green else Muted, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric("WI-FI", wifiRows.size.toString())
                    Metric("BLE", bleRows.size.toString())
                    Metric("GPS", gpsRows.size.toString())
                }
                Spacer(Modifier.height(12.dp))
                Text(status, color = Cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (scanning) stopObservation() else startObservation() }) {
                        Text(if (scanning) "STOP OBSERVATION" else "START OBSERVATION")
                    }
                    OutlinedButton(onClick = { selected = "RADAR" }) { Text("RF RADAR") }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("OVERVIEW", "WI-FI", "BLE", "GPS", "RADAR").forEach { tab ->
                        TextButton(onClick = { selected = tab }) {
                            Text(tab, color = if (selected == tab) Green else Muted, fontSize = 10.sp)
                        }
                    }
                }
                Divider(color = Color(0xFF26343D))
                Spacer(Modifier.height(8.dp))
                when (selected) {
                    "RADAR" -> RadarView(wifiRows.map { it.rssi } + bleRows.map { it.rssi })
                    "WI-FI" -> ObservationList(wifiRows.map { "${it.ssid}  ·  ${it.bssid}  ·  ${it.rssi} dBm  ·  ${it.security}  ·  CH ${it.channel}" })
                    "BLE" -> ObservationList(bleRows.map { "${it.name ?: "Unknown BLE"}  ·  ${it.address}  ·  ${it.rssi} dBm" })
                    "GPS" -> ObservationList(gpsRows.reversed().map { "${it.latitude}, ${it.longitude}  ·  ±${it.accuracy}m" })
                    else -> {
                        Text("OBSERVATION SUMMARY", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Wi-Fi observations are sourced from Android scan results. BLE observations are advertisement-only; no GATT connections are initiated.", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        RadarView(wifiRows.map { it.rssi } + bleRows.map { it.rssi })
                        Spacer(Modifier.height(10.dp))
                        Text("LATEST EVENTS", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        ObservationList((wifiRows.take(4).map { "WIFI  ${it.ssid}  ${it.rssi} dBm" } + bleRows.take(4).map { "BLE  ${it.name ?: it.address}  ${it.rssi} dBm" }))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("RF//RECON  •  PASSIVE COLLECTION ONLY  •  ANDROID API LIMITS APPLY", color = Muted, fontSize = 9.sp)
            }
        }
    }

    @Composable private fun Metric(label: String, value: String) {
        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(label, color = Muted, fontSize = 10.sp)
                Text(value, color = Green, fontSize = 23.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }

    @Composable private fun ObservationList(rows: List<String>) {
        if (rows.isEmpty()) Text("NO OBSERVATIONS IN SESSION", color = Muted, fontSize = 12.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rows) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(6.dp)) {
                    Text(row, Modifier.fillMaxWidth().padding(10.dp), color = Color(0xFFD7E2E8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }

    @Composable private fun RadarView(levels: List<Int>) {
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(230.dp)) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                val maxR = minOf(size.width, size.height) * .43f
                for (i in 1..4) drawCircle(Green.copy(alpha = .25f), maxR * i / 4f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                drawLine(Green.copy(alpha = .5f), Offset(c.x - maxR, c.y), Offset(c.x + maxR, c.y), 1f)
                drawLine(Green.copy(alpha = .5f), Offset(c.x, c.y - maxR), Offset(c.x, c.y + maxR), 1f)
                levels.take(32).forEachIndexed { i, rssi ->
                    val angle = (i * 137.5) * Math.PI / 180.0
                    val radius = maxR * (1.0 - ((rssi.coerceIn(-100, -20) + 100) / 80.0))
                    val p = Offset(c.x + (kotlin.math.cos(angle) * radius).toFloat(), c.y + (kotlin.math.sin(angle) * radius).toFloat())
                    drawCircle(if (rssi > -60) Green else Cyan, 5f, p)
                }
                drawCircle(Green, 4f, c)
            }
        }
    }
}
