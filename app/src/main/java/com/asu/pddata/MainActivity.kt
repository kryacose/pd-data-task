package com.asu.pddata

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import com.asu.pddata.constants.Constants
import com.asu.pddata.databinding.ActivityMainBinding
import com.asu.pddata.service.ForegroundService

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var button: Button
    private lateinit var foregroundService: ForegroundService
    private lateinit var serviceConnection : ServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createNotificationChannel()

        serviceConnection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName) {
//                mBounded = false
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
//                mBounded = true
                val mLocalBinder = service as ForegroundService.LocalBinder
                foregroundService = mLocalBinder.getServerInstance()
            }
        }

        val serviceIntent = Intent(this, ForegroundService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)


        button = findViewById(R.id.button)
        button.setOnClickListener {
            foregroundService.setTookMedication()
        }
    }


    private fun createNotificationChannel() {

        val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = Constants.NOTIFICATION_CHANNEL_DESCRIPTION
        }

        val notificationManager: NotificationManager =
            getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

}