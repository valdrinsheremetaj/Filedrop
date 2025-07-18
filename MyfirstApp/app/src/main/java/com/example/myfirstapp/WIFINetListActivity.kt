package com.example.myfirstapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat


class WIFINetListActivity: ComponentActivity(), View.OnClickListener{
    public final lateinit var wifiManager : WifiManager
    private lateinit var wifiList: WifiNetworkList
    lateinit var wifiConfiguration: WifiConfiguration
    private var enableHotSpotButton: Button? = null
    private var hotspotReservation: LocalOnlyHotspotReservation? = null

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
    // this is supposed to be turning the hotspot on but it does not
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    @RequiresApi(api = Build.VERSION_CODES.O)
    public fun turnOnHotspot(): Void? {
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
            return null
        }
        val localhostCallback: WifiManager.LocalOnlyHotspotCallback = object : WifiManager.LocalOnlyHotspotCallback()
        {
            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                super.onStarted(reservation)
                hotspotReservation = reservation;
                var currentConfig = hotspotReservation?.getWifiConfiguration();

                printCurrentConfig(currentConfig);
                Log.v("TAG", "Local Hotspot Started");
            }

            override fun onStopped() {
                super.onStopped()
                Log.v("TAG", "Local Hotspot Stopped");
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                Log.v("TAG", "Local Hotspot failed to start");
            }


        }
        var handler:android.os.Handler = android.os.Handler()
        wifiManager.startLocalOnlyHotspot(localhostCallback, handler);
        return null
    }


    private fun printCurrentConfig(wifiConfiguration: WifiConfiguration?) {
        Log.v(
            "TAG", ("THE PASSWORD IS: "
                    + wifiConfiguration?.preSharedKey
                    + " \n SSID is : "
                    + wifiConfiguration?.SSID)
        )
    }


    //Workaround to turn off hotspot for Oreo versions
    @RequiresApi(api = Build.VERSION_CODES.O)
    fun turnOffHotspot() {
        if (hotspotReservation != null) {
            hotspotReservation!!.close()
            hotspotReservation = null
            Log.v("TAG", "Turned off hotspot")
        }
    }

    //This method checks the state of the hostpot for devices>=Oreo
    @RequiresApi(api = Build.VERSION_CODES.O)
    fun isHotspotStarted(): Boolean {
        return hotspotReservation != null
    }



    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(view: View?) {
        if (view === enableHotSpotButton) {
            turnOnHotspot()
        }
    }
}




