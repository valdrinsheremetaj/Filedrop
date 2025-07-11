package com.example.myfirstapp
import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import android.content.Intent
import android.renderscript.ScriptGroup
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat


class peripheralView : AppCompatActivity(), View.OnClickListener {

    // declaring objects of Button class
    private var start: Button? = null
    private var stop: Button? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peripheral_view)

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

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter


        //var originalName = bluetoothAdapter.getName();
        //bluetoothAdapter.setName("FileDrop3000_"+originalName);
        
        var nameEditField =  findViewById<View>(R.id.editTextName) as EditText
        nameEditField.setText(bluetoothAdapter.name)

        var macEditField =  findViewById<View>(R.id.editTextMac) as EditText


        val neededPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )

        ActivityCompat.requestPermissions(this, neededPermissions, 1)
    }



    override fun onClick(view: View) {

        // process to be performed
        // if start button is clicked
        if (view === start) {

            // starting the service
            var intent = Intent(this, PeripheralService::class.java)

            startService(Intent(this, PeripheralService::class.java))
        }

        // process to be performed
        // if stop button is clicked
        else if (view === stop) {

            // stopping the service
            stopService(Intent(this, PeripheralService::class.java))
        }
    }
}