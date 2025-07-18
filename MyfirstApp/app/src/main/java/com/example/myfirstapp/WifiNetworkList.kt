package com.example.myfirstapp

import android.Manifest
import android.annotation.SuppressLint
import com.example.myfirstapp.R
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.RequiresPermission
import android.view.LayoutInflater
import android.widget.Button
import android.net.wifi.ScanResult
import android.os.Build
import androidx.annotation.RequiresApi


class WifiNetworkList(private val context: Context) : BaseAdapter() {
    private val results = mutableListOf<ScanResult>()

    fun addDevice(res: ScanResult) {
        if (!results.contains(res)) {
            results.add(res)
            notifyDataSetChanged()
        }
    }

    fun clearDevices()
    {
        results.clear()
        notifyDataSetChanged()
    }

    override fun getCount(): Int = results.size

    override fun getItem(position: Int): ScanResult = results[position]
    override fun getItemId(position: Int): Long {
        TODO("Not yet implemented")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ViewHolder")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val device = results[position]
        val rowView = LayoutInflater.from(context).inflate(R.layout.wifi_list_item, parent, false)


        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            device.wifiSsid?.toString() ?: "Unknown"
        } else {
            device.SSID ?: "Unknown"
        }

        rowView.findViewById<TextView>(R.id.wifiName).text = "$name"

        return rowView
    }
}


