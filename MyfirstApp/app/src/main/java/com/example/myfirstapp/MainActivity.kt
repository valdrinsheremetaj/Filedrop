package com.example.myfirstapp
import android.Manifest
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


class MainActivity : ComponentActivity(), View.OnClickListener{
    // declaring objects of Button class
    private var start: Button? = null
    private var stop: Button? = null
    private var wifiButton: Button? = null

    private var bleScanner : BluetoothLeScanner? = null
    // 10 second scan
    private val SCANPERIOD: Long = 60000
    private val handler = Handler(Looper.getMainLooper())
    private var scanning = false
    private lateinit var deviceListAdapter: LeDeviceListAdapter
    private lateinit var listView: android.widget.ListView





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

        // declaring listeners for the
        // buttons to make them respond
        // correctly according to the process
        start!!.setOnClickListener(this)
        stop!!.setOnClickListener(this)
        wifiButton!!.setOnClickListener(this)


        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter?.bluetoothLeScanner

        listView = findViewById(R.id.deviceList)
        deviceListAdapter = LeDeviceListAdapter(this)
        listView.adapter = deviceListAdapter

        val neededPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.CHANGE_WIFI_STATE,
        )

        ActivityCompat.requestPermissions(this, neededPermissions, 1)


    }

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

        }
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    private fun scanLeDevice() {
        Log.d("BLE_SCAN", "scanLeDevice() started")

        if (!scanning) {
            handler.postDelayed({
                scanning = false
                bleScanner?.stopScan(leScanCallback)
                Log.d("BLE_SCAN", "Stopped scan after $SCANPERIOD ms")
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
            deviceListAdapter.addDevice(result.device)
            deviceListAdapter.notifyDataSetChanged()
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

}



