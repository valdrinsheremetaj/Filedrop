package com.example.myfirstapp

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.widget.ListView
import android.widget.Toast
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
        devices?.forEach { deviceListAdapter.addDevice(it) }

        BLEConnectionManager.onConnected = {
            Handler(Looper.getMainLooper()).post {
                try {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                                checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == android.content.pm.PackageManager.PERMISSION_GRANTED)) {

                        AlertDialog.Builder(this)
                            .setTitle("Start Wi-Fi Direct?")
                            .setMessage("Connected to $connectedBLEName via BLE. Start Wi-Fi Direct file transfer?")
                            .setPositiveButton("Yes") { _, _ -> startClientMode() }
                            .setNegativeButton("No", null)
                            .show()
                    } else {
                        Log.e("PERMISSION", "Missing required permissions for Wi-Fi Direct")
                    }
                } catch (e: SecurityException) {
                    Log.e("PERMISSION", "SecurityException while checking permissions: ${e.message}")
                }
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private fun startClientMode() {
        val manager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)

        WifiDirectManager.initialize(manager, channel, this)
        WifiDirectManager.registerReceiver(this)

        manager.requestGroupInfo(channel) { group ->
            if (group != null) {
                manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                    @RequiresPermission(allOf = ["android.permission.ACCESS_FINE_LOCATION", "android.permission.NEARBY_WIFI_DEVICES"])
                    override fun onSuccess() {
                        Log.d("WIFI_DIRECT", "Removed an existing group.")
                        Handler(Looper.getMainLooper()).postDelayed({
                            proceedToPeerDiscovery(manager, channel)
                        }, 1500)
                    }
                    override fun onFailure(reason: Int) {
                        Log.e("WIFI_DIRECT", "Failed to remove existing group: $reason.")
                    }
                })
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    proceedToPeerDiscovery(manager, channel)
                }, 1500)
            }
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            val fileUri = data?.data ?: run {
                Log.e("FILE_PICKER", "No file selected")
                return
            }

            val info = WifiDirectManager.connectionInfo ?: run {
                Log.e("FILE_SENDER", "connectionInfo is null")
                return
            }

            if (!info.isGroupOwner) {
                val hostAddress = info.groupOwnerAddress?.hostAddress ?: run {
                    Log.e("FILE_SENDER", "Group owner address is null")
                    return
                }

                val port = 8888

                // Get file name and file type
                var fileName = "unknown"
                val mimeType: String? = contentResolver.getType(fileUri) ?: "application/octet-stream"

                contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                        val fileSize = cursor.getLong(sizeIndex)
                        Log.d("FILE_META", "Selected file: $fileName (${fileSize} bytes), type: $mimeType")
                    }
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    val serviceIntent = Intent(this, FileTransferService::class.java).apply {
                        action = FileTransferService.ACTION_SEND_FILE
                        putExtra(FileTransferService.EXTRAS_FILE_PATH, fileUri.toString())
                        putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, hostAddress)
                        putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, port)
                        putExtra(FileTransferService.EXTRAS_FILE_NAME, fileName)
                        putExtra(FileTransferService.EXTRAS_MIME_TYPE, mimeType)
                    }

                    Log.d("FILE_TRANSFER", "Starting sending: $fileName to $hostAddress:$port")
                    startService(serviceIntent)
                }, 3000)
            }
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private fun proceedToPeerDiscovery(manager: WifiP2pManager, channel: WifiP2pManager.Channel) {
        manager.requestConnectionInfo(channel) { info ->
            if (info.groupFormed) {
                WifiDirectManager.connectionInfo = info
            }
        }

        WifiDirectManager.discoverPeers(
            context = this,
            onSuccess = {
                Log.d("WIFI_DIRECT", "Peer discovery started.")
                val bleName = connectedBLEName
                val device = WifiDirectManager.peerList.find {
                    Log.d("WIFI_DIRECT", "Peer: ${it.deviceName} (${it.deviceAddress})")
                    it.deviceName.equals(bleName, ignoreCase = true)
                }

                if (device != null) {
                    Log.d("WIFI_DIRECT", "Target device found: ${device.deviceName}")
                    connectWithRetry(device)
                } else {
                    Log.w("WIFI_DIRECT", "Device $bleName not found among peers")
                }
            },
            onFailure = { reason ->
                Log.e("WIFI_DIRECT", "Peer discovery failed: $reason")
            }
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private fun connectWithRetry(device: WifiP2pDevice, attempt: Int = 1) {
        WifiDirectManager.connectToPeer(
            device,
            onSuccess = {
                Log.d("WIFI_DIRECT", "Connected to ${device.deviceName}.")

                openFilePicker()
            },
            onFailure = { reason ->
                Log.e("WIFI_DIRECT", "Connection attempt $attempt failed: $reason")

                if (reason == WifiP2pManager.BUSY && attempt <= 3) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        connectWithRetry(device, attempt + 1)
                    }, 2000)
                } else {
                    Toast.makeText(this, "Failed to connect to ${device.deviceName}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}