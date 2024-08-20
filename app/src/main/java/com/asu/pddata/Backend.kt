package com.asu.pddata

import android.os.Environment
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException


class Backend  {
    private val client = OkHttpClient()


    fun getUserDetails(userName: String) {
        val request = Request.Builder()
            .url("http://mayo.abdullah-mamun.com:9000/parkinson/get_user_details/?user_name=$userName")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.v("API ERROR", "getUserDetails: $e")
            }
            override fun onResponse(call: Call, response: Response) {
                Log.v("API OUTPUT", "getUserDetails: ${response.body()?.string()}")
            }
        })
    }

    fun uploadSensorData(fileName: String) {

        val nameParts = fileName.split("-")
        val startTimestamp = nameParts[2]
        val endTimestamp = nameParts[3]


        val inputStream = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName).inputStream()
        val fileContent = inputStream.bufferedReader().use { it.readText() }

        Log.v("FILE_CONTENT", fileContent)
        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(MultipartBody.Part.createFormData("user_name", "TestUser1"))
            .addPart(MultipartBody.Part.createFormData("study_name", "test1"))
            .addPart(MultipartBody.Part.createFormData("application_type", "android"))
            .addPart(MultipartBody.Part.createFormData("num_rows", "10"))
            .addPart(MultipartBody.Part.createFormData("start_timestamp", startTimestamp))
            .addPart(MultipartBody.Part.createFormData("end_timestamp", endTimestamp))
            .addPart(MultipartBody.Part.createFormData("file_name", fileName))
            .addPart(MultipartBody.Part.createFormData("file_content", fileContent))
            .build()

        val request = Request.Builder()
            .url("http://mayo.abdullah-mamun.com:9000/parkinson/upload_sensor_data/")
            .header("content-type", "multipart/form-data")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.v("API ERROR", "uploadSensorData: $e")
            }
            override fun onResponse(call: Call, response: Response) {
                Log.v("API OUTPUT", "uploadSensorData: ${response.body()?.string()}")
            }
        })
    }
}