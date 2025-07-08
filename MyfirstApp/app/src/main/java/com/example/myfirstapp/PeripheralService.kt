package com.example.myfirstapp

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.UUID


class PeripheralService : Service() {


    private var bleAdvertiser : BluetoothLeAdvertiser? = null
    private lateinit var deviceListAdapter: LeDeviceListAdapter
    private var currentAdvertisingSet : android.bluetooth.le.AdvertisingSet? = null
    private lateinit var callbackVar : AdvertisingSetCallback


    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        val parameters = (AdvertisingSetParameters.Builder())
            .setLegacyMode(true) // True by default, but set here as a reminder.
            .setConnectable(true)
            .setScannable(true)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .build()

        val data = (AdvertiseData.Builder()).setIncludeDeviceName(true).build()

        val callback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet?,
                txPower: Int,
                status: Int
            ) {
                Log.i(
                    "LOG_TAG", ("onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                            + status)
                )
                currentAdvertisingSet = advertisingSet
            }

            override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet?, status: Int) {
                Log.i("LOG_TAG", "onAdvertisingDataSet() :status:" + status)
            }

            override fun onScanResponseDataSet(advertisingSet: AdvertisingSet?, status: Int) {
                Log.i("LOG_TAG", "onScanResponseDataSet(): status:" + status)
            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                Log.i("LOG_TAG", "onAdvertisingSetStopped():")
            }
        }

        callbackVar = callback;

        bleAdvertiser?.startAdvertisingSet(parameters, data, null, null, null, callback);


        // After onAdvertisingSetStarted callback is called, you can modify the
        // advertising data and scan response data:
        currentAdvertisingSet?.setAdvertisingData(
            AdvertiseData.Builder().setIncludeDeviceName
                (true).setIncludeTxPowerLevel(true).build()
        )


        // Wait for onAdvertisingDataSet callback...
        currentAdvertisingSet?.setScanResponseData(
            AdvertiseData.Builder().addServiceUuid(ParcelUuid(UUID.randomUUID())).build()
        )




        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onDestroy() {
        super.onDestroy()

        // stopping the process
        bleAdvertiser?.stopAdvertisingSet(callbackVar);
    }

    override fun onBind(intent: Intent): IBinder? {
        TODO("Return the communication channel to the service.")
        return null
    }


}