package com.example.myfirstapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.example.myfirstapp.BLEConnectionManager.connectedBLEName

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

        // used flag --> BLE is connected to some device --> should we start Wifi direct popup
        BLEConnectionManager.onConnected = {
            Handler(Looper.getMainLooper()).post {
                AlertDialog.Builder(this)
                    .setTitle("Start Wi-Fi Direct?")
                    .setMessage("Connected to $connectedBLEName via BLE. Start Wi-Fi Direct file transfer?")
                    // if we press yes --> wifi direct connection triggered
                    .setPositiveButton("Yes") { _, _ ->
                        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        startWifiDirectTransfer()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
    }
    @SuppressLint("ServiceCast")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private fun startWifiDirectTransfer() {
        val manager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)

        WifiDirectManager.initialize(manager, channel, this)
        WifiDirectManager.registerReceiver(this)

        // discover peers, connect to them and start our file picker
        WifiDirectManager.discoverPeers(
            context = this,
            onSuccess = {
                val bleName = connectedBLEName
                val device = WifiDirectManager.peerList.find {
                    it.deviceName.equals(bleName, ignoreCase = true)
                }

                if (device != null) {
                    WifiDirectManager.connectToPeer(
                        device,
                        onSuccess = {
                            Log.d("WIFI_DIRECT", "Connected to $device, opening file picker")
                            openFilePicker()
                        },
                        onFailure = { reason ->
                            Log.e("WIFI_DIRECT", "Connection failed: $reason")
                        }
                    )
                } else {
                    Log.w("WIFI_DIRECT", "$device not found among peers")
                }
            },
            onFailure = { reason ->
                Log.e("WIFI_DIRECT", "Peer discovery failed: $reason")
            }
        )
    }


    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Select File"), FILE_PICKER_REQUEST_CODE)
    }

    companion object {
        const val FILE_PICKER_REQUEST_CODE = 1234
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            val fileUri = data?.data
            Log.d("FILE_PICKER", "Selected file: $fileUri")

        }
    }
}