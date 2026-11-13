package com.rf.recon

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporters {
    fun csvWifi(rows: List<WifiObservation>): String = buildString {
        appendLine("ssid,bssid,rssi_dbm,frequency_mhz,channel,security,capabilities,timestamp")
        rows.forEach {
            appendLine(listOf(it.ssid, it.bssid, it.rssi, it.frequencyMhz, it.channel,
                it.security, it.capabilities, it.timestamp).joinToString(",") { v ->
                "\"${v.toString().replace("\"", "\"\"")}\""
            })
        }
    }

    fun htmlReport(wifi: List<WifiObservation>, ble: List<BleObservation>): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
        <title>RF//RECON Assessment</title><style>
        body{background:#090d12;color:#d7e2e8;font:14px monospace;margin:2rem}
        h1,h2{color:#39ff88}table{border-collapse:collapse;width:100%;margin-bottom:2rem}
        td,th{border:1px solid #26343d;padding:8px;text-align:left}th{color:#39ff88}
        .muted{color:#82939d}</style></head><body><h1>RF//RECON</h1>
        <p class="muted">PASSIVE RF OBSERVATION REPORT · $date</p><h2>Wi-Fi observations (${wifi.size})</h2>
        <table><tr><th>SSID</th><th>BSSID</th><th>RSSI</th><th>Frequency</th><th>Security</th></tr>
        ${wifi.joinToString("") { "<tr><td>${esc(it.ssid)}</td><td>${esc(it.bssid)}</td><td>${it.rssi}</td><td>${it.frequencyMhz}</td><td>${it.security}</td></tr>" }}
        </table><h2>BLE observations (${ble.size})</h2><table><tr><th>Address</th><th>Name</th><th>RSSI</th><th>UUIDs</th></tr>
        ${ble.joinToString("") { "<tr><td>${esc(it.address)}</td><td>${esc(it.name.orEmpty())}</td><td>${it.rssi}</td><td>${esc(it.serviceUuids)}</td></tr>" }}
        </table><p class="muted">Observations are not proof of compromise. Validate findings independently.</p></body></html>"""
    }

    private fun esc(s: String) = s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")

    fun writePdf(context: Context, uri: Uri, lines: List<String>) {
        val doc = PdfDocument()
        val paint = Paint().apply { textSize = 11f; color = android.graphics.Color.DKGRAY }
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
        var y = 48f
        lines.forEach { line ->
            if (y > 800f) {
                doc.finishPage(page); pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
                y = 48f
            }
            page.canvas.drawText(line.take(95), 36f, y, paint); y += 18f
        }
        doc.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
        doc.close()
    }
}
