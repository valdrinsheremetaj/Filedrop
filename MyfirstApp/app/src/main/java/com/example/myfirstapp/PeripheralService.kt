package com.example.myfirstapp

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import java.util.UUID


class PeripheralService : Service() {


    private var bleAdvertiser : BluetoothLeAdvertiser? = null
    private lateinit var deviceListAdapter: LeDeviceListAdapter
    private var currentAdvertisingSet : android.bluetooth.le.AdvertisingSet? = null
    private lateinit var callbackVar : AdvertisingSetCallback
    private lateinit var settings:AdvertiseSettings
    private lateinit var data: AdvertiseData




    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return Service.START_STICKY_COMPATIBILITY
        }
        //var originalName = bluetoothAdapter.getName();
        bluetoothAdapter.setName("OnePlus 10 Pro 5G");

        settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .build()

        val data = AdvertiseData.Builder()
            //.addServiceUuid(ParcelUuid(UUID.fromString(getString(R.string.ble_uuid))))
            .setIncludeDeviceName(true)
            .build()

        bleAdvertiser = bluetoothLeAdvertiser;



        bleAdvertiser?.startAdvertising(settings, data, object : AdvertiseCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.d("TAG", "Advertising started successfully")
                Log.d("TAG", bluetoothAdapter.name)
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.e("TAG", "Advertising failed with error code $errorCode")
            }


        })



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