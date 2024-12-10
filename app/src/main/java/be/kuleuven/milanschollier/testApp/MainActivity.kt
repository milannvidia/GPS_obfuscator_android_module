package be.kuleuven.milanschollier.testApp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import be.kuleuven.milanschollier.safegps.LatLonTs
import be.kuleuven.milanschollier.safegps.LocationManager
import be.kuleuven.milanschollier.safegps.LocationObfuscatorV1
import be.kuleuven.milanschollier.testApp.ui.theme.TestAppTheme
import com.google.android.gms.location.Priority
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Date
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {
    private val locationManager= LocationManager.getInstance(this)
    private val priorityOptions= hashMapOf(
        "High Accuracy" to Priority.PRIORITY_HIGH_ACCURACY,
        "Balanced Power Accuracy" to Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        "Low Power" to Priority.PRIORITY_LOW_POWER,
        "Passive" to Priority.PRIORITY_PASSIVE
    )
    init {
        locationManager.locationObfuscator= LocationObfuscatorV1.getInstance()
        locationManager.debug=true
        locationManager.debugCallback={original,obfuscated-> sendLocationToServer(original,obfuscated)}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestAppTheme  {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)){
                        Text(
                            text = "Dit is een app voor mijn thesis het stuurt continue locatie gegevens door. Bij privacy concerns stuur me zeker een vraag",
                            modifier = Modifier.padding(10.dp)
                        )
                        var expanded by remember { mutableStateOf(false) }
                        var prioritySelect by remember { mutableStateOf(priorityOptions.entries.first{it.value==locationManager.priority}.key) }
                        Box {
                            Column {
                                OutlinedTextField(
                                    value = prioritySelect,
                                    onValueChange = {},
                                    trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = "Priority") }
                                )
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    priorityOptions.keys.forEach  { priority ->
                                        DropdownMenuItem(
                                            text = { Text(text = priority) },
                                            onClick = {
                                                expanded = false
                                                locationManager.priority=priorityOptions.getValue(priority)
                                                prioritySelect=priority
                                            })
                                    }
                                }
                            }
                            Spacer(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Transparent)
                                    .padding(10.dp)
                                    .clickable(onClick = {
                                        expanded = !expanded
                                    })
                            )
                        }
                        val finePermission =locationManager.finePermission.observeAsState()
                        val coarsePermission =locationManager.coarsePermission.observeAsState()
                        var intervalValue by remember { mutableStateOf(locationManager.interval.toString()) }
                        OutlinedTextField(
                            value = intervalValue,

                            onValueChange = {
                                val x: Long
                                try {
                                    x=it.toLong()
                                }catch (e: NumberFormatException){
                                    intervalValue=0L.toString()
                                    locationManager.interval=0
                                    return@OutlinedTextField
                                }
                                locationManager.interval=x
                                intervalValue=locationManager.interval.toString()
                            },
                            modifier= Modifier.fillMaxWidth(),
                            label = { Text(text = "interval ms") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        var maxRuntimeValue by remember { mutableStateOf((locationManager.maxRuntime/(1000*60)).toString()) }
                        OutlinedTextField(
                            value = maxRuntimeValue,
                            onValueChange = {
                                val x: Long
                                try {
                                    x=it.toLong()*1000*60
                                }catch (e: NumberFormatException){
                                    maxRuntimeValue=0L.toString()
                                    locationManager.maxRuntime=0
                                    return@OutlinedTextField
                                }
                                locationManager.maxRuntime=x
                                maxRuntimeValue=(locationManager.maxRuntime/(1000*60)).toString()
                            },
                            modifier= Modifier.fillMaxWidth(),
                            label = { Text(text = "maxRuntime minutes (background)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        var foreground by remember { mutableStateOf(locationManager.foregroundService) }
                        Box{
                            OutlinedTextField(
                                value = foreground.toString(),
                                readOnly = true,
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(text = "foreground") }
                            )
                            Spacer(modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .padding(10.dp)
                                .clickable(onClick = {
                                    foreground = !foreground
                                    locationManager.foregroundService = foreground
                                }))
                        }
                        OutlinedTextField(
                            value = "finePermission: ${finePermission.value} coarsePermission: ${coarsePermission.value}",
                            onValueChange = {},
                            readOnly = true,
                            modifier= Modifier.fillMaxWidth(),
                            label = { Text(text = "Permissionstate") },
                        )

                        Row {
                            Button(onClick ={
                                locationManager.getCoarsePermission(true)
                            },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(64.dp, 64.dp),
                                enabled = coarsePermission.value==false
                            ) {
                                Text(text = "Ask Coarse Permission")
                            }
                            Button(onClick ={
                                locationManager.getFinePermission(true)
                            },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(64.dp, 64.dp),
                                enabled = finePermission.value==false
                            ) {
                                Text(text = "Ask Fine Permission")
                            }
                        }
                        var results by remember { mutableStateOf("") }
                        Row{
                            val running=locationManager.running.observeAsState()
                            Button(
                                onClick = {
                                    if(running.value==true) locationManager.stopTracking()
                                    else {
                                        locationManager.startTracking()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(64.dp, 64.dp)
                            ){
                                Text(text = if(running.value==true) "Stop tracking" else "Start tracking")
                            }
                            Button(
                                onClick = {
                                        locationManager.getLocation { x->
                                            results += "${x.first}, ${x.second}, ${Date(x.third)}\n"
                                        }
                                          },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(64.dp, 64.dp)
                            ){
                                Text(text = "Get location")
                            }
                        }

                        LaunchedEffect(LocationManager) {
                            locationManager.callback={ location ->
                                results += "${location.first}, ${location.second}, ${Date(location.third)} \n"
                            }
                        }
                        OutlinedTextField(
                            value = results,
                            onValueChange = {},
                            readOnly = true,
                            modifier= Modifier.fillMaxWidth(),
                            label = { Text(text = "Results") },
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        sendBlobsToServer()
        super.onStop()
    }
    @SuppressLint("HardwareIds")
    fun sendLocationToServer(realLocation: LatLonTs, obfuscatedLocation:LatLonTs){
         val jsonBody= JSONObject()
            .put("finePermission",this.locationManager.getFinePermission(false))
            .put("foreGround",this.locationManager.foregroundService)
            .put("priority",this.locationManager.priority)
            .put("user", Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
            .put("realLocation",realLocation)
            .put("obfuscatedLocation",obfuscatedLocation)

        println(jsonBody.toString())
        Thread{
            try {
                val client= OkHttpClient()
                val request= Request.Builder()
                    .url("https://masterproefmilanschollier.azurewebsites.net/location")
                    .post(jsonBody.toString().toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull()))
                    .build()
                val response=client.newCall(request).execute()
                if (response.isSuccessful) {
                    // Handle successful response
                    retryPendingLocation(this)
                } else {
                    savePendingLocation(this,jsonBody)
                    // Handle error response
                    println("Error: ${response.code} - ${response.message}")
                    println("response fails error")
                }
            }catch (e: IOException){
                savePendingLocation(this,jsonBody)
                println("try error")
                println("Error: ${e.message}")
            }

        }.start()
    }

    @SuppressLint("HardwareIds")
    fun sendBlobsToServer(){
        val jsonBody= JSONObject()
            .put("user", Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
            .put("blobs",(locationManager.locationObfuscator as LocationObfuscatorV1).getHistoryBlobs())

        println(jsonBody.toString())
        Thread{
            try {
                val client= OkHttpClient()
                val request= Request.Builder()
                    .url("https://masterproefmilanschollier.azurewebsites.net/blobs")
                    .post(jsonBody.toString().toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull()))
                    .build()
                val response=client.newCall(request).execute()
                if (response.isSuccessful) {
                    // Handle successful response
                    println("Response: ${response.body?.string()}")
                    retryPendingBlob(this)
                } else {
                    // Handle error response
                    println("Error: ${response.code} - ${response.message}")
                    savePendingBlob(this,jsonBody)
                }
            }catch (e: IOException){
                savePendingBlob(this,jsonBody)
                println("Error: ${e.message}")
            }
        }.start()
    }

    private fun savePendingLocation(context: Context, jsonBody: JSONObject){
        val sharedPreferences =context.getSharedPreferences("pendingLocation", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val pendingLocation = sharedPreferences.getString("pendingLocation", "[]")
        val locationArray = JSONArray(pendingLocation)
        locationArray.put(jsonBody)
        editor.putString("pendingLocation", locationArray.toString())
        editor.apply()
    }

    private fun retryPendingLocation(context: Context) {
        val sharedPreferences =context.getSharedPreferences("pendingLocation", Context.MODE_PRIVATE)
        val pendingLocation = sharedPreferences.getString("pendingLocation", "[]")
        val locationArray = JSONArray(pendingLocation)
        val failedArray= sendArray(locationArray,"https://masterproefmilanschollier.azurewebsites.net/location")
        val editor = sharedPreferences.edit()
        editor.putString("pendingLocation", failedArray.toString())
        editor.apply()
    }

    private fun savePendingBlob(context: Context, jsonBody: JSONObject) {
        val sharedPreferences =context.getSharedPreferences("pendingBlob", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val pendingBlob = sharedPreferences.getString("pendingBlob", "[]")
        val blobArray = JSONArray(pendingBlob)
        blobArray.put(jsonBody)
        editor.putString("pendingBlob", blobArray.toString())
        editor.apply()
    }

    private fun retryPendingBlob(context: Context) {
        val sharedPreferences =context.getSharedPreferences("pendingBlob", Context.MODE_PRIVATE)
        val pendingBlob = sharedPreferences.getString("pendingBlob", "[]")
        val blobArray = JSONArray(pendingBlob)
        val failedArray=sendArray(blobArray,"https://masterproefmilanschollier.azurewebsites.net/blobs")
        val editor = sharedPreferences.edit()
        editor.putString("pendingBlob", failedArray.toString())
        editor.apply()
    }

    private fun sendArray(array: JSONArray,url: String): JSONArray {
        val failedArray=JSONArray()
        val latch = CountDownLatch(array.length())
        for (i in 0 until array.length()) {
            val jsonObject = array.getJSONObject(i)
            thread {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url(url)
                        .post(
                            jsonObject.toString()
                                .toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull())
                        )
                        .build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        failedArray.put(jsonObject)
                    }
                }catch (e: IOException){
                    failedArray.put(jsonObject)
                }finally {
                    latch.countDown()
                }
            }.start()
        }
        latch.await()
        return failedArray
    }
}
