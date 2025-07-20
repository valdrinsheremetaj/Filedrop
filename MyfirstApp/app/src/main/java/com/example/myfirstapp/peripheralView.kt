package com.example.myfirstapp

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class peripheralView : AppCompatActivity(), View.OnClickListener {

    private var start: Button? = null
    private var stop: Button? = null
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var wifiChannel: WifiP2pManager.Channel
    private lateinit var wifiReceiver: WifiDirectBroadcastReceiver
    private lateinit var wifiIntentFilter: IntentFilter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peripheral_view)

        start = findViewById(R.id.startButton)
        stop = findViewById(R.id.stopButton)
        start!!.setOnClickListener(this)
        stop!!.setOnClickListener(this)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        val nameEditField = findViewById<EditText>(R.id.editTextName)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            nameEditField.setText(bluetoothAdapter.name)
        } else {
            nameEditField.setText("Unknown")
        }

        val macEditField = findViewById<EditText>(R.id.editTextMac)

        // Wifi-Direct setup, to make sure device is also advertising via Wifi-Direct
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wifiChannel = wifiP2pManager.initialize(this, mainLooper, null)
        WifiDirectManager.initialize(wifiP2pManager, wifiChannel, this)


        wifiIntentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        wifiReceiver = WifiDirectBroadcastReceiver(wifiP2pManager, wifiChannel, this)
        registerReceiver(wifiReceiver, wifiIntentFilter)

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, getPermissionsForThisDevice(), 1)
            return
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private fun startWifiDirectDiscovery() {
        WifiDirectManager.manager.requestGroupInfo(WifiDirectManager.channel) { group ->
            if (group != null) {
                Log.d("WIFI_DIRECT", "Group already exists, removing first...")
                WifiDirectManager.manager.removeGroup(WifiDirectManager.channel, object : WifiP2pManager.ActionListener {
                    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
                    override fun onSuccess() {
                        Log.d("WIFI_DIRECT", "Group removed, waiting before creating new group...")
                        Handler(Looper.getMainLooper()).postDelayed({
                            createWifiDirectGroup()
                        }, 1000)
                    }


                    override fun onFailure(reason: Int) {
                        Log.e("WIFI_DIRECT", "Failed to remove existing group: $reason")
                    }
                })
            } else {
                createWifiDirectGroup()
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private fun createWifiDirectGroup() {
        WifiDirectManager.manager.createGroup(WifiDirectManager.channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WIFI_DIRECT", "Group created successfully!")
            }

            override fun onFailure(reason: Int) {
                Log.e("WIFI_DIRECT", "Group creation failed: $reason")
            }
        })
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onClick(view: View) {
        if (view === start) {
            startService(Intent(this, PeripheralService::class.java))
            startWifiDirectDiscovery()
            val receiveIntent = Intent(this, FileTransferService::class.java).apply {
                action = FileTransferService.ACTION_RECEIVE_FILE
            }
            startService(receiveIntent)
        } else if (view === stop) {
            stopService(Intent(this, PeripheralService::class.java))
        }
    }
    private fun hasPermissions(): Boolean {
        return getPermissionsForThisDevice().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getPermissionsForThisDevice(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.BLUETOOTH_PRIVILEGED,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            // this could be used for older devices
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
}
