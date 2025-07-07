package com.example.myfirstapp
import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.util.Log
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myfirstapp.ui.theme.MyfirstAppTheme
import android.content.Intent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.ComponentActivity
import kotlin.Int.Companion.MAX_VALUE
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity(), View.OnClickListener{
    // declaring objects of Button class
    private var start: Button? = null
    private var stop: Button? = null

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

        // declaring listeners for the
        // buttons to make them respond
        // correctly according to the process
        start!!.setOnClickListener(this)
        stop!!.setOnClickListener(this)


        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter?.bluetoothLeScanner

        listView = findViewById(R.id.deviceList)
        deviceListAdapter = LeDeviceListAdapter(this)
        listView.adapter = deviceListAdapter

        val neededPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        ActivityCompat.requestPermissions(this, neededPermissions, 1)


    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    override fun onClick(view: View) {

        // process to be performed
        // if start button is clicked
        if (view === start) {
            if (hasPermissions()) {
                scanLeDevice()
            }
        }

        // process to be performed
        // if stop button is clicked
        else if (view === stop) {

            // stopping the service

            stopService(Intent(this, HelloService::class.java))

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



