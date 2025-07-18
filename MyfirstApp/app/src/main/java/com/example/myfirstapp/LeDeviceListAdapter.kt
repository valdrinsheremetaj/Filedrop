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


class LeDeviceListAdapter(private val context: Context) : BaseAdapter() {
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

    override fun getItem(position: Int): BluetoothDevice = devices[position]
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val device = devices[position]
        val rowView = LayoutInflater.from(context).inflate(R.layout.device_list_item, parent, false)

        val nameView = rowView.findViewById<TextView>(R.id.deviceName)
        val connectButton = rowView.findViewById<Button>(R.id.connectButton)

        val name = device.name ?: "Unknown"
        val address = device.address
        nameView.text = "$name\n$address"
        connectButton.setOnClickListener {
            BLEConnectionManager.connectToDevice(context, device, showDialog = true)
        }

        return rowView
    }
}