package com.example.myfirstapp
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


class MainActivity : ComponentActivity(), View.OnClickListener{
    // declaring objects of Button class
    private var start: Button? = null
    private var stop: Button? = null

    @Volatile var increment: Int? = 0
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
    }
    //fun clickButton(view: View?) {
        // Do something in response to button click
    //    increment = increment?.plus(1)
    //    val tvId = findViewById<View?>(R.id.textView) as TextView
    //    tvId.setText(increment.toString())

    //}

    fun isSeriviceRunning(context: Context, classs: Class<*>?): Boolean
    {
        val activityManager = this?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var services: List<ActivityManager.RunningServiceInfo>
        services = activityManager.getRunningServices( MAX_VALUE)
        for(s in services)
        {
            if(s.equals(classs))
            {
                return true
            }
        }
        return false
    }



    override fun onClick(view: View) {

        // process to be performed
        // if start button is clicked

        if (view === start) {

            // starting the service
            startService(Intent(this, HelloService::class.java))
            //Log.d("CREATION", isSeriviceRunning(this, HelloService::class.java).toString())
        }

        // process to be performed
        // if stop button is clicked
        else if (view === stop) {

            // stopping the service

            stopService(Intent(this, HelloService::class.java))

        }
    }
}



