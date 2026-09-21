package com.example.accel_jellybean

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var txtResult: TextView

    private val accelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val accelX = intent?.getFloatExtra("accelX", 0f) ?: 0f
            val accelY = intent?.getFloatExtra("accelY", 0f) ?: 0f
            val accelZ = intent?.getFloatExtra("accelZ", 0f) ?: 0f
            txtResult.text = String.format(Locale.getDefault(),
                "X: %.2f\nY: %.2f\nZ: %.2f", accelX, accelY, accelZ)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        txtResult = findViewById(R.id.txtResult)

        checkPermissionsAndStartService()
    }

    private fun checkPermissionsAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            } else {
                startAccelService()
            }
        } else {
            startAccelService()
        }
    }

    private fun startAccelService() {
        val serviceIntent = Intent(this, AccelService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(AccelService.ACTION_READ_ACCEL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(accelReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(accelReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(accelReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAccelService()
        }
    }
}