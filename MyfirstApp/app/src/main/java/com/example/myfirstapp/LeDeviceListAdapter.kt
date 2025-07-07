package com.example.myfirstapp

import android.Manifest
import android.R
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.RequiresPermission

class LeDeviceListAdapter(context: Context) : ArrayAdapter<BluetoothDevice>(context, android.R.layout.simple_list_item_2) {
    private val devices = mutableListOf<BluetoothDevice>()

    fun addDevice(device: BluetoothDevice) {
        if (!devices.contains(device)) {
            devices.add(device)
            notifyDataSetChanged()
        }
    }

    fun clearDevices() {
        devices.clear()
        notifyDataSetChanged()
    }

    override fun getCount(): Int = devices.size

    override fun getItem(position: Int): BluetoothDevice? = devices[position]

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: View.inflate(context, R.layout.simple_list_item_2, null)
        val device = devices[position]

        val nameView = view.findViewById<TextView>(R.id.text1)
        val addressView = view.findViewById<TextView>(R.id.text2)

        nameView.text = device.name ?: "Unknown Device"
        addressView.text = device.address

        return view
    }
}