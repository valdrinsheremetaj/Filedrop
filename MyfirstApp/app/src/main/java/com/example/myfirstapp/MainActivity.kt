package com.example.myfirstapp
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.content.BroadcastReceiver
import android.content.IntentFilter


class MainActivity : ComponentActivity(), View.OnClickListener{
    // declaring objects of Button class
    private var start: Button? = null
    private var stop: Button? = null
    private var wifiButton: Button? = null

    private var bleScanner : BluetoothLeScanner? = null
    // 10 second scan
    private val SCANPERIOD: Long = 6000
    private val handler = Handler(Looper.getMainLooper())
    private var scanning = false
    private lateinit var deviceListAdapter: LeDeviceListAdapter
    private lateinit var listView: android.widget.ListView
    private val scannedDevices = ArrayList<BluetoothDevice>()
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: Channel
    private lateinit var wifiDirectButton: Button
    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter



    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        // assigning ID of startButton
        // to the object start
        start = findViewById<View>(R.id.startButton) as Button

        // assigning ID of stopButton
        // to the object stop
        stop = findViewById<View>(R.id.stopButton) as Button

        wifiButton = findViewById<View>(R.id.showWIFI) as Button

        wifiDirectButton = findViewById<View>(R.id.wifiDirectButton) as Button

        // declaring listeners for the
        // buttons to make them respond
        // correctly according to the process
        start!!.setOnClickListener(this)
        stop!!.setOnClickListener(this)
        wifiButton!!.setOnClickListener(this)
        wifiDirectButton.setOnClickListener(this)


        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, mainLooper, null)

        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = WifiDirectBroadcastReceiver(wifiP2pManager, channel, this)


        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter?.bluetoothLeScanner

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, getPermissionsForThisDevice(), 1)
            return
        }




    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    override fun onClick(view: View) {

        // process to be performed
        // if start button is clicked
        if (view === start) {
            Log.d("BLE_SCAN", "BLE Scanner null? ${bleScanner == null}")
            Log.d("BLE_SCAN", "Has permission? ${hasPermissions()}")
            Log.d("BLE_SCAN", "Starting scan now...")

            Log.d("BLE_SCAN", "scanLeDevice() started")
            if (hasPermissions()) {
                scanLeDevice()
            }
        }

        // process to be performed
        // if stop button is clicked
        else if (view === stop) {

            // stopping the service

            startActivity(Intent(this@MainActivity, peripheralView::class.java))

        }
        // process to be performed
        // if stop button is clicked
        else if (view === wifiButton) {


            // stopping the service
            startActivity(Intent(this@MainActivity, WIFINetListActivity::class.java))
            //stopService(Intent(this, HelloService::class.java))

            // just used for faster Wifi Direct testing
        } else if (view === wifiDirectButton) {
            if (!hasPermissions()) {
                Log.e("WIFI_DIRECT", "Missing Wi-Fi Direct permissions")
                return
            }

            val manager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
            val channel = manager.initialize(this, mainLooper, null)
            WifiDirectManager.initialize(manager, channel, this)
            WifiDirectManager.registerReceiver(this)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            WifiDirectManager.discoverPeers(
                context = this,
                onSuccess = {
                    Log.d("WIFI_DIRECT", "Peers discovered. Connecting to first available peer...")

                    val peer = WifiDirectManager.peerList.firstOrNull()
                    if (peer != null) {
                        WifiDirectManager.connectToPeer(
                            peer,
                            onSuccess = {
                                Log.d("WIFI_DIRECT", "Connected to ${peer.deviceName}, opening file picker")

                                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                }
                                startActivityForResult(Intent.createChooser(intent, "Select File"), 1234)
                            },
                            onFailure = { reason ->
                                Log.e("WIFI_DIRECT", "Connection failed: $reason")
                            }
                        )
                    } else {
                        Log.w("WIFI_DIRECT", "No peers found.")
                    }
                },
                onFailure = { reason ->
                    Log.e("WIFI_DIRECT", "Peer discovery failed: $reason")
                }
            )
        }

    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    private fun scanLeDevice() {
        Log.d("BLE_SCAN", "scanLeDevice() started")

        if (!scanning) {
            scannedDevices.clear()
            handler.postDelayed({
                scanning = false
                bleScanner?.stopScan(leScanCallback)
                Log.d("BLE_SCAN", "Stopped scan after $SCANPERIOD ms")
                // change to devicelistactivty
                val intent = Intent(this, DeviceListActivity::class.java)
                intent.putParcelableArrayListExtra("devices", scannedDevices)
                startActivity(intent)
            }, SCANPERIOD)
            scanning = true
            bleScanner?.startScan(leScanCallback)
            Log.d("BLE_SCAN", "Started scanning")
        } else {
            scanning = false
            bleScanner?.stopScan(leScanCallback)
        }
    }
    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val name = device.name ?: "Unknown"
            val address = device.address

            Log.d("BLE_SCAN", "Device found: Name=$name, Address=$address")
            // filter or no?
            if (!scannedDevices.contains(device) /*&& device.name == "Calculator"*/) {
                scannedDevices.add(device)
            }
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
                Manifest.permission.ACCESS_FINE_LOCATION
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


    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    @Deprecated("Use ActivityResult API instead if needed.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1234 && resultCode == RESULT_OK) {
            val fileUri = data?.data
            if (fileUri == null) {
                Log.e("FILE_PICKER", "No file selected")
                return
            }

            val info = WifiDirectManager.connectionInfo
            Log.d("FILE_SENDER", "connectionInfo = $info")
            if (info == null || !info.groupFormed) {
                Log.e("FILE_SENDER", "Wi-Fi Direct not connected or no group owner")
                return
            }

            val hostAddress = info.groupOwnerAddress.hostAddress
            val port = 8888

            val serviceIntent = Intent(this, FileTransferService::class.java).apply {
                action = FileTransferService.ACTION_SEND_FILE
                putExtra(FileTransferService.EXTRAS_FILE_PATH, fileUri.toString())
                putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, hostAddress)
                putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, port)
            }

            Log.d("FILE_SENDER", "Sending file to $hostAddress:$port → $fileUri")
            startService(serviceIntent)
        }
    }




}



