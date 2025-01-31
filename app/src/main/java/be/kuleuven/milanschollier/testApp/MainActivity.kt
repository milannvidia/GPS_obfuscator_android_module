package be.kuleuven.milanschollier.testApp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import be.kuleuven.milanschollier.safegps.LatLonTs
import be.kuleuven.milanschollier.safegps.LocationManager
import be.kuleuven.milanschollier.safegps.LocationObfuscatorV1
import be.kuleuven.milanschollier.safegps.LogLevel
import be.kuleuven.milanschollier.testApp.ui.theme.TestAppTheme
import com.google.android.gms.location.Priority
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    private val locationManager = LocationManager.getInstance(this)

    private val url = "https://masterproefmilanschollier.azurewebsites.net"
    private val priorityOptions = hashMapOf(
        "High Accuracy" to Priority.PRIORITY_HIGH_ACCURACY,
        "Balanced Power Accuracy" to Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        "Low Power" to Priority.PRIORITY_LOW_POWER,
        "Passive" to Priority.PRIORITY_PASSIVE
    )
    private val client = OkHttpClient()
        .newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    init {
        locationManager.locationObfuscator = LocationObfuscatorV1.getInstance()
        locationManager.logLevel = LogLevel.VERBOSE
        locationManager.debugCallback =
            { original, obfuscated -> sendLocationToServer(original, obfuscated) }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Dit is een app voor mijn thesis het stuurt continue locatie gegevens door. Bij privacy concerns stuur me zeker een vraag",
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            var prioritySelect by remember { mutableStateOf(priorityOptions.entries.first { it.value == locationManager.priority }.key) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            ) {
                                Column {
                                    OutlinedTextField(
                                        value = prioritySelect,
                                        onValueChange = {},
                                        trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(),
                                        label = { Text(text = "Priority") },
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        priorityOptions.keys.forEach { priority ->
                                            DropdownMenuItem(
                                                text = { Text(text = priority) },
                                                onClick = {
                                                    expanded = false
                                                    locationManager.priority =
                                                        priorityOptions.getValue(priority)
                                                    prioritySelect = priority
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
                            var foreground by remember { mutableStateOf(locationManager.foregroundService) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedTextField(
                                    value = foreground.toString(),
                                    readOnly = true,
                                    onValueChange = {},
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    label = { Text(text = "foreground") },

                                    )
                                Spacer(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Transparent)
                                        .padding(10.dp)
                                        .clickable(onClick = {
                                            foreground = !foreground
                                            locationManager.foregroundService = foreground
                                        })
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            var intervalValue by remember { mutableStateOf(locationManager.interval.toString()) }
                            OutlinedTextField(
                                value = intervalValue,
                                onValueChange = { intervalValue = it },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            try {
                                                locationManager.interval = intervalValue.toLong()
                                            } catch (_: Exception) {
                                            }
                                            intervalValue = locationManager.interval.toString()
                                        }
                                    },
                                label = { Text(text = "interval ms") },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        try {
                                            locationManager.interval = intervalValue.toLong()
                                        } catch (_: Exception) {
                                        }
                                        intervalValue = locationManager.interval.toString()
                                    })
                            )
                            var maxRuntimeValue by remember { mutableStateOf((locationManager.maxRuntime / (1000 * 60)).toString()) }
                            OutlinedTextField(
                                value = maxRuntimeValue,
                                onValueChange = { maxRuntimeValue = it },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            try {
                                                locationManager.maxRuntime =
                                                    maxRuntimeValue.toLong() * 1000 * 60
                                            } catch (_: Exception) {
                                            }
                                            maxRuntimeValue =
                                                (locationManager.maxRuntime / (1000 * 60)).toString()
                                        }
                                    },
                                label = { Text(text = "maxRuntime minutes (background)") },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        try {
                                            locationManager.maxRuntime =
                                                maxRuntimeValue.toLong() * 1000 * 60
                                        } catch (_: Exception) {
                                        }
                                        maxRuntimeValue =
                                            (locationManager.maxRuntime / (1000 * 60)).toString()
                                    }

                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            var blobRadius by remember { mutableStateOf((locationManager.locationObfuscator as LocationObfuscatorV1).blobRadius.toString()) }
                            OutlinedTextField(
                                value = blobRadius,
                                onValueChange = { blobRadius = it },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            try {
                                                (locationManager.locationObfuscator as LocationObfuscatorV1).blobRadius =
                                                    blobRadius.toDouble()
                                            } catch (_: Exception) {
                                            }
                                            blobRadius =
                                                (locationManager.locationObfuscator as LocationObfuscatorV1).blobRadius.toString()
                                        }
                                    },
                                label = { Text(text = "Blob radius") },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        try {
                                            (locationManager.locationObfuscator as LocationObfuscatorV1).blobRadius =
                                                blobRadius.toDouble()
                                        } catch (_: Exception) {
                                        }
                                        blobRadius =
                                            (locationManager.locationObfuscator as LocationObfuscatorV1).blobRadius.toString()
                                    })
                            )
                            var deltaTime by remember { mutableStateOf((locationManager.locationObfuscator as LocationObfuscatorV1).deltaTime.toString()) }
                            OutlinedTextField(
                                value = deltaTime,
                                onValueChange = { deltaTime = it },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            try {
                                                (locationManager.locationObfuscator as LocationObfuscatorV1).deltaTime =
                                                    deltaTime.toLong()
                                            } catch (_: Exception) {
                                            }
                                            deltaTime =
                                                (locationManager.locationObfuscator as LocationObfuscatorV1).deltaTime.toString()
                                        }
                                    },
                                label = { Text(text = "Time between privacy") },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        try {
                                            (locationManager.locationObfuscator as LocationObfuscatorV1).deltaTime =
                                                deltaTime.toLong()
                                        } catch (_: Exception) {
                                        }
                                        deltaTime =
                                            (locationManager.locationObfuscator as LocationObfuscatorV1).deltaTime.toString()
                                    })

                            )
                        }

                        val finePermission = locationManager.finePermission.observeAsState()
                        val coarsePermission = locationManager.coarsePermission.observeAsState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    locationManager.getCoarsePermission(true)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                enabled = coarsePermission.value == false
                            ) {
                                Text(text = if (coarsePermission.value == true) "Coarse permission granted" else "Ask Coarse Permission")
                            }
                            Button(
                                onClick = {
                                    locationManager.getFinePermission(true)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                enabled = finePermission.value == false
                            ) {
                                Text(text = if (finePermission.value == true) "Fine permission granted" else "Ask Fine Permission")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {

                            Button(
                                onClick = {
                                    locationManager.locationObfuscator?.clearStorage(this@MainActivity.filesDir)
                                    Toast.makeText(this@MainActivity, "Storage cleared", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            ) {
                                Text(text = "clear obfuscator storage")
                            }
                            Button(
                                onClick = {
                                    this@MainActivity.newUUID()

                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            ) {
                                Text(text = "Get new UUID")
                            }
                        }
                        var results by remember { mutableStateOf("") }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val running = locationManager.running.observeAsState()
                            Button(
                                onClick = {
                                    if (running.value == true) {
                                        locationManager.stopTracking()
                                        sendBlobsToServer()
                                    }
                                    else {
                                        locationManager.startTracking()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            ) {
                                Text(text = if (running.value == true) "Stop tracking" else "Start tracking")
                            }
                            Button(
                                onClick = {
                                    locationManager.getLocation { res ->
                                        results += locationToString(res)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            ) {
                                Text(text = "Get location")
                            }
                        }

                        LaunchedEffect(LocationManager) {
                            locationManager.callback = { location ->
                                results += locationToString(location)
                            }
                        }
                        OutlinedTextField(
                            value = results,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
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

    @SuppressLint("DefaultLocale")
    private fun locationToString(location: LatLonTs): String {
        val xDegrees = location.first.toLong()
        val xMinutes = (location.first - xDegrees) * 60
        val xMinutesDegrees = xMinutes.toLong()
        val xSeconds = (xMinutes - xMinutesDegrees) * 60

        val yDegrees = location.second.toLong()
        val yMinutes = (location.second - yDegrees) * 60
        val yMinutesDegrees = yMinutes.toLong()
        val ySeconds = (yMinutes - yMinutesDegrees) * 60

        var row = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date(location.third))
        row += " ${xDegrees}° ${xMinutesDegrees}' ${String.format("%.2f", xSeconds)}\""
        row += " ${yDegrees}° ${yMinutesDegrees}' ${String.format("%.2f", ySeconds)}\""
        return row
    }
    //=================================================================================================================================================================
    // IO
    //=================================================================================================================================================================

    private fun sendLocationToServer(realLocation: LatLonTs, obfuscatedLocation: LatLonTs?) {
        println("sendLocationToServer")
        val jsonBody = JSONObject()
            .put("finePermission", this.locationManager.getFinePermission(false))
            .put("foreGround", this.locationManager.foregroundService)
            .put("priority", this.locationManager.priority)
            .put("uuid", this.getUUID().toString())
            .put("timestamp", Date().time)
            .put("realLocation", realLocation)
            .put("obfuscatedLocation", obfuscatedLocation)
        println(jsonBody)
        val href = "location"
        val key = "pendingLocation"
        Thread {
            if (sendToServer(jsonBody, href)) {
                retryPending(key, href)
                locationManager.logDebug("retryPending", LogLevel.VERBOSE)
            } else {
                savePending(key, jsonBody)
                locationManager.logDebug("savePending", LogLevel.VERBOSE)
            }
        }.start()
    }

    private fun sendBlobsToServer() {
        println("sendBlobsToServer")
        val jsonBody = JSONObject()
            .put("uuid", this.getUUID().toString())
            .put("timestamp", Date().time)
            .put(
                "blobs",
                (locationManager.locationObfuscator as LocationObfuscatorV1).getHistoryBlobs()
            )
        val href = "blobs"
        val key = "pendingBlobs"
        Thread {
            if (sendToServer(jsonBody, href)) {
                retryPending(key, href)
                locationManager.logDebug("retryPendingBlobs", LogLevel.VERBOSE)
            } else {
                savePending(key, jsonBody)
                locationManager.logDebug("savePendingBlobs", LogLevel.VERBOSE)
            }
        }.start()
    }

    //=================================================================================================================================================================
    // uuid
    //=================================================================================================================================================================

    private fun getUUID(): UUID {
        val sharedPreferences = this.getSharedPreferences("uuid", Context.MODE_PRIVATE)
        val pending = sharedPreferences.getString("uuid", null)
        return if (pending != null) UUID.fromString(pending) else newUUID()
    }

    private fun newUUID():UUID {
        val uuid = UUID.randomUUID()
        val sharedPreferences = this.getSharedPreferences("uuid", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("uuid", uuid.toString())
        editor.apply()
        Toast.makeText(this@MainActivity, "new UUID saved $uuid", Toast.LENGTH_SHORT).show()
        return uuid
    }

    //=================================================================================================================================================================
    // permanent storage sending to server
    //=================================================================================================================================================================
    private fun savePending(key: String, jsonBody: JSONObject) {
        val sharedPreferences = this.getSharedPreferences(key, Context.MODE_PRIVATE)
        val pending = sharedPreferences.getString(key, "[]")
        val array = JSONArray(pending)
        val editor = sharedPreferences.edit()
        array.put(jsonBody)
        locationManager.logDebug("saved: ${array.length()} in file", LogLevel.DEBUG)
        editor.putString(key, array.toString())
        editor.apply()
    }

    private fun retryPending(key: String, href: String) {
        val sharedPreferences = this.getSharedPreferences(key, Context.MODE_PRIVATE)
        val pending = sharedPreferences.getString(key, "[]")
        val array = JSONArray(pending)
        if (array.length() == 0) return
        val failedArray = sendArray(array, href)
        locationManager.logDebug(
            "retried with ${array.length()}, ${failedArray.length()} failed",
            LogLevel.DEBUG
        )
        val editor = sharedPreferences.edit()
        editor.putString(key, failedArray.toString())
        editor.apply()
    }

    private fun sendArray(array: JSONArray, href: String): JSONArray {
        val failedArray = JSONArray()
        for (i in 0 until array.length()) {
            val jsonObject = array.getJSONObject(i)
            if (!sendToServer(jsonObject, href)) {
                failedArray.put(jsonObject)
            }
        }
        return failedArray
    }

    private fun sendToServer(jsonBody: JSONObject, href: String): Boolean {

        val request = Request.Builder()
            .url("$url/$href")
            .post(
                jsonBody.toString()
                    .toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull())
            )
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: IOException) {
            false
        }
    }
}
