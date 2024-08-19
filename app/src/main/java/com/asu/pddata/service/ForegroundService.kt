package com.asu.pddata.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.asu.pddata.CSVRow
import com.asu.pddata.SensorData
import com.asu.pddata.constants.Constants
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Locale

class ForegroundService : Service(), SensorEventListener {

    private var isServiceRunning = false
    private var mSensorManager: SensorManager? = null
    private var mAccSensor: Sensor? = null
    private var mGyroSensor: Sensor? = null
    private var mHeartRateSensor: Sensor? = null


    private var sensorData: SensorData = SensorData()
    private var csvData: ArrayList<CSVRow> = arrayListOf()
    private var tookMedication: Boolean = false


    private val dataCollectionHandler = Handler()
    private val cloudSyncHandler = Handler()
    private var lastSynced = System.currentTimeMillis()

    private var mBinder: Binder = LocalBinder()


    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        fun getServerInstance(): ForegroundService = this@ForegroundService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (!isServiceRunning) {
            isServiceRunning = true
            startForeground()
        }
        // Service will be restarted if killed by the system
        return START_STICKY
    }

    private fun startForeground() {
        val notification = Notification.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Service")
            .setContentText("Collecting data")
            .build()

        startForeground(1, notification)
    }

    override fun onCreate() {
        super.onCreate()

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyroSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mHeartRateSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        mSensorManager?.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager?.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager?.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)

        startDataCollection()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stopDataCollection()
        mSensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return
        }
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            sensorData.accXValue = event.values[0]
            sensorData.accYValue = event.values[1]
            sensorData.accZValue = event.values[2]
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            sensorData.angularSpeedX = event.values[0]
            sensorData.angularSpeedY = event.values[1]
            sensorData.angularSpeedZ = event.values[2]
        } else if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            sensorData.heartRate = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //do something
    }


    private fun saveDataToCSV(fileName: String): Boolean {

        if(!isExternalStorageWritable())
            return false

        if(csvData.isEmpty()) {
            Log.i("data", "List is empty")
            return false
        }

        val csvFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            Log.v("Cloud", "Saving file to $fileName")
            val fileWriter = FileWriter(csvFile)

            fileWriter.append(CSVRow.getHeaders().joinToString(","))
            fileWriter.append("\n")

            for (i in 0 until csvData.size) {
                val row: MutableList<String> = mutableListOf()
                row.add(csvData[i].timestamp.toString())
                row.add(String.format(Locale.US, "%.2f", csvData[i].sensorData.accXValue))
                row.add(String.format(Locale.US, "%.2f", csvData[i].sensorData.accYValue))
                row.add(String.format(Locale.US, "%.2f", csvData[i].sensorData.accZValue))
                row.add(String.format(Locale.US, "%.2f", csvData[i].sensorData.angularSpeedX))
                row.add(String.format(Locale.US, "%.2f", csvData[i].sensorData.angularSpeedY))
                row.add(String.format(Locale.US, "%.2f", csvData[i].sensorData.angularSpeedZ))
                row.add(String.format(Locale.US, "%.2f", csvData[i].sensorData.heartRate))
                row.add(String.format(Locale.US, "%d", if(csvData[i].medication) 1 else 0))

                fileWriter.append(row.joinToString(","))
                fileWriter.append("\n")
            }


            fileWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    private val dataCollectionRunnable = object : Runnable {
        override fun run() {
            collectData()

            dataCollectionHandler.postDelayed(this, Constants.DATA_COLLECTION_INTERVAL.toLong())
        }
    }

    private val cloudSyncRunnable = object : Runnable {
        override fun run() {
            val currentSync = System.currentTimeMillis()
            if (saveDataToCSV("data-${Constants.USER_ID}-$lastSynced-$currentSync")) {
                lastSynced = currentSync
            }
            cloudSyncHandler.postDelayed(this, Constants.CLOUD_SYNC_INTERVAL.toLong())
        }
    }

    private fun startDataCollection() {
        dataCollectionHandler.post(dataCollectionRunnable)
        cloudSyncHandler.post(cloudSyncRunnable)
    }

    private fun stopDataCollection() {
        dataCollectionHandler.removeCallbacks(dataCollectionRunnable)
        cloudSyncHandler.removeCallbacks(cloudSyncRunnable)
    }

    fun collectData() {
        csvData.add(CSVRow(System.currentTimeMillis(), sensorData, getTookMedication()))
        Log.v("Collect", "Collecting data: ${csvData.last()}")
        //clear collected sensor data
        sensorData = SensorData()
    }

    fun setTookMedication() {

        tookMedication = true
    }
    private fun getTookMedication(): Boolean {
        if(tookMedication) {
            tookMedication = false
            return true
        }
        return false
    }


}

