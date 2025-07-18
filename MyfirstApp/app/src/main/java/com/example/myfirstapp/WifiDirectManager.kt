package com.example.myfirstapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission

object WifiDirectManager {

    private const val TAG = "WifiDirectManager"

    // entry point for Wi-Fi P2P functionality
    lateinit var manager: WifiP2pManager
    //communication channel between app and wifi framework
    lateinit var channel: WifiP2pManager.Channel
    // listens for events
    lateinit var receiver: BroadcastReceiver
    // what events should we listen to?
    lateinit var intentFilter: IntentFilter

    var peerList: MutableList<WifiP2pDevice> = mutableListOf()
    // might be used for future purposes like group owner etc
    var connectionInfo: WifiP2pInfo? = null
    //val bleName = BLEConnectionManager.connectedBLEName



    // Initialize the Wi-Fi P2P manager
    fun initialize(manager: WifiP2pManager, channel: WifiP2pManager.Channel, context: Context) {
        this.manager = manager
        this.channel = channel

        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = WifiDirectBroadcastReceiver(manager, channel, context)
    }



    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun discoverPeers(context: Context, onSuccess: () -> Unit, onFailure: (reason: Int) -> Unit) {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Peer discovery started.")


                Handler(Looper.getMainLooper()).postDelayed({
                    // use ble name from handshake to match with wifi peer
                    val bleName = BLEConnectionManager.connectedDevice?.name
                    if (bleName == null) {
                        Log.e(TAG, "BLE device MAC adress is null. Cannot match Wi-Fi peer.")
                        onFailure(-2)
                    }
                    val matchedPeer = peerList.find { it.deviceName.equals(bleName, ignoreCase = true) }

                    // if matched peer found, connect to it
                    if (matchedPeer != null) {
                        Log.d(TAG, "Matched peer found: ${matchedPeer.deviceName}")
                        checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        connectToPeer(matchedPeer,
                            onSuccess = {
                                Log.d(TAG, "Connected to matched peer via Wi-Fi Direct.")
                                onSuccess()
                            },
                            onFailure = { reason ->
                                Log.e(TAG, "Failed to connect to matched peer: $reason")
                                onFailure(reason)
                            }
                        )
                    } else {
                        Log.w(TAG, "No matching peer found for BLE name: $bleName")
                        onFailure(-1)
                    }
                }, 10000)

            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Peer discovery failed: $reason")
                onFailure(reason)
            }
        })
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun connectToPeer(device: WifiP2pDevice, onSuccess: () -> Unit, onFailure: (reason: Int) -> Unit) {
        // connect via mac address by using WifiP2pConfig
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connecting to device: ${device.deviceName}")
                onSuccess()
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Connection failed: $reason")
                onFailure(reason)
            }
        })
    }

    // listen for events
    fun registerReceiver(context: Context) {
        ContextCompat.registerReceiver(
            context,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // stop listening
    fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(receiver)
    }
}
