package com.asu.pddata

class SensorData {
    var accXValue: Float = 0F
    var accYValue: Float = 0F
    var accZValue: Float = 0F
    var angularSpeedX: Float = 0F
    var angularSpeedY: Float = 0F
    var angularSpeedZ: Float = 0F
    var heartRate: Float = 0F
    override fun toString(): String {
        return "SensorData(accXValue=$accXValue, accYValue=$accYValue, accZValue=$accZValue, angularSpeedX=$angularSpeedX, angularSpeedY=$angularSpeedY, angularSpeedZ=$angularSpeedZ, heartRate=$heartRate)"
    }

}

class CSVRow(var timestamp: Long, var sensorData: SensorData, var medication: Boolean) {
    companion object {
        fun getHeaders(): List<String> {
            return listOf(
                "Timestamp",
                "Acc X",
                "Acc Y",
                "Acc Z",
                "Angular X",
                "Angular Y",
                "Angular Z",
                "Heart Rate",
                "Medication"
            )
        }
    }

    override fun toString(): String {
        return "CSVRow(timestamp=$timestamp, sensorData=$sensorData, medication=$medication)"
    }

}