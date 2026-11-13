package com.rf.recon

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class RfDatabase(context: Context) : SQLiteOpenHelper(context, "rf_recon.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE wifi (
            id INTEGER PRIMARY KEY AUTOINCREMENT, ssid TEXT, bssid TEXT, rssi INTEGER,
            frequency INTEGER, capabilities TEXT, observed_at INTEGER, lat REAL, lon REAL)""")
        db.execSQL("""CREATE TABLE ble (
            id INTEGER PRIMARY KEY AUTOINCREMENT, address TEXT, name TEXT, rssi INTEGER,
            tx_power INTEGER, manufacturer TEXT, uuids TEXT, observed_at INTEGER, lat REAL, lon REAL)""")
        db.execSQL("""CREATE INDEX idx_wifi_bssid_time ON wifi(bssid, observed_at)""")
        db.execSQL("""CREATE INDEX idx_ble_addr_time ON ble(address, observed_at)""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun save(o: WifiObservation) {
        val v = ContentValues().apply {
            put("ssid", o.ssid); put("bssid", o.bssid); put("rssi", o.rssi)
            put("frequency", o.frequencyMhz); put("capabilities", o.capabilities)
            put("observed_at", o.timestamp); if (o.latitude != null) put("lat", o.latitude)
            if (o.longitude != null) put("lon", o.longitude)
        }
        writableDatabase.insert("wifi", null, v)
    }
    fun save(o: BleObservation) {
        val v = ContentValues().apply {
            put("address", o.address); put("name", o.name); put("rssi", o.rssi)
            put("tx_power", o.txPower); put("manufacturer", o.manufacturerData)
            put("uuids", o.serviceUuids); put("observed_at", o.timestamp)
            if (o.latitude != null) put("lat", o.latitude)
            if (o.longitude != null) put("lon", o.longitude)
        }
        writableDatabase.insert("ble", null, v)
    }
}
