package com.example.myfirstapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.annotation.RequiresPermission

class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val context: Context
) : BroadcastReceiver() {

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                Log.d("WIFI_DIRECT", "Wi-Fi P2P state: $state")
            }
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                WifiDirectManager.manager.requestPeers(WifiDirectManager.channel) { peers ->
                    WifiDirectManager.peerList.clear()
                    WifiDirectManager.peerList.addAll(peers.deviceList)
                    Log.d("WIFI_DIRECT", "Peers changed!")
                    peers.deviceList.forEach {
                        Log.d("WIFI_DIRECT", "Found peer: ${it.deviceName} - ${it.deviceAddress}")
                    }
                }
            }
        }
    }
}

