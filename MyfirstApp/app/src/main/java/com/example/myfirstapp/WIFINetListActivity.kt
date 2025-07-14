package com.example.myfirstapp
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import java.lang.Boolean
import java.lang.reflect.Method
import kotlin.Exception


class WIFINetListActivity: ComponentActivity(), View.OnClickListener{
    public final lateinit var wifiManager : WifiManager
    private lateinit var wifiList: WifiNetworkList
    lateinit var wifiConfiguration: WifiConfiguration
    private var enableHotSpotButton: Button? = null

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wifi_list)
        // set up the ui
        var listView = findViewById<View>(R.id.wifiList) as ListView
        wifiList = WifiNetworkList(this)

        listView.adapter = wifiList
        // set up the wifiManager
        wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = "MyDummySSID"


        val wifiScanReceiver = object : BroadcastReceiver() {
            // so what are we doing here  no idea, but if the success is true we we will scan
            @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        this.registerReceiver(wifiScanReceiver, intentFilter)

        val success = wifiManager.startScan()
        if (!success) {
            // scan failure handling
            scanFailure()
        }

        enableHotSpotButton = findViewById<View>(R.id.enableHotSpot) as Button
        enableHotSpotButton!!.setOnClickListener(this)
    }



    // this is supposed to be turning the hotspot on but it does not 
    private fun changeStateWifiAp(activated: kotlin.Boolean) {
        val method: Method?
        try {
            method = wifiManager.javaClass.getDeclaredMethod(
                "setWifiApEnabled",
                WifiConfiguration::class.java,
                Boolean.TYPE
            )
            method.invoke(wifiManager, wifiConfiguration, activated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun scanSuccess() {
        val results = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
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
        } else {

        }
        var scanResults = wifiManager.scanResults

        for( i in scanResults.indices)
        {
            wifiList.addDevice(scanResults[i])
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        val results = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
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
        } else {

        }
        wifiManager.scanResults

    }

    override fun onClick(view: View?) {
        if (view === enableHotSpotButton) {
            changeStateWifiAp(true)
        }
    }
}




