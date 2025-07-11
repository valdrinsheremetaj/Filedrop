package com.example.myfirstapp

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission

object BLEConnectionManager {

    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null

    private var appContext: Context? = null
    private var showTestDialog: Boolean = false

    private val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BLE", "Connected to ${gatt.device.address}")
                connectedDevice = gatt.device
                gatt.discoverServices()

                if (showTestDialog) {
                    appContext?.let { context ->
                        Handler(Looper.getMainLooper()).post {
                            AlertDialog.Builder(context)
                                .setTitle("BLE Connection")
                                .setMessage("Connected to ${gatt.device.name ?: gatt.device.address}")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("BLE", "Disconnected from ${gatt.device.address}")
                close()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(context: Context, device: BluetoothDevice, showDialog: Boolean = true) {
        Log.i("BLE", "Requested connection to ${device.address}")

        appContext = context
        showTestDialog = showDialog

        if (bluetoothGatt != null && connectedDevice?.address != device.address) {
            Log.i("BLE", "Disconnecting from previous device ${connectedDevice?.address}")
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        }

        bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }
}
