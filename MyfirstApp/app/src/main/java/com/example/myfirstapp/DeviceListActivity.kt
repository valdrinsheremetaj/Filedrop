package com.example.myfirstapp

import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi

class DeviceListActivity: ComponentActivity() {
    private lateinit var listView: ListView
    private lateinit var deviceListAdapter: LeDeviceListAdapter

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        listView = findViewById(R.id.deviceList)
        deviceListAdapter = LeDeviceListAdapter(this)
        listView.adapter = deviceListAdapter

        val devices = intent.getParcelableArrayListExtra<BluetoothDevice>("devices")
        devices?.forEach {deviceListAdapter.addDevice(it)}
    }
}