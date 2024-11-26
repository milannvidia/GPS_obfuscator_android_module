//package be.kuleuven.milanschollier.testApp
//
//import android.app.PendingIntent
//import android.app.Service
//import android.content.Intent
//import android.location.Location
//import android.os.IBinder
//import android.provider.Settings
//import androidx.core.app.NotificationCompat
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationCallback
//import com.google.android.gms.location.LocationRequest
//import com.google.android.gms.location.LocationResult
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.location.Priority
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//import java.io.IOException
//
//
//class LocationTrackingService:Service() {
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private lateinit var locationRequest: LocationRequest
//    private var finePermission=false
//    private var priority:Int?=null
//    private var foreground=true
//
//    override fun onCreate() {
//
//        println("OnCreate")
//        super.onCreate()
//
//        val notificationIntent=Intent(this, MainActivity::class.java)
//        val pendingIntent=PendingIntent.getActivity(this,0,notificationIntent,PendingIntent.FLAG_IMMUTABLE)
//        val notification = NotificationCompat.Builder(this, "locationTrackingChannel")
//            .setContentTitle("Location Tracking")
//            .setContentText("Tracking your location in the background")
//            .setSmallIcon(R.drawable.ic_launcher_background)
//            .setContentIntent(pendingIntent)
//            .build()
//        startForeground(1,notification)
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        println("onStartCommand")
//
//        val interval = intent?.getLongExtra("interval",120L)
//        priority = intent?.getIntExtra("Priority", Priority.PRIORITY_BALANCED_POWER_ACCURACY)
//        finePermission= intent?.getBooleanExtra("FinePermission",false) == true
//
//        locationRequest = LocationRequest.Builder(priority!!, interval!!*1000).build()
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        startLocationUpdates()
//        return super.onStartCommand(intent, flags, startId)
//    }
//    override fun onDestroy() {
//        println("onDestroy")
//        super.onDestroy()
//
//        fusedLocationClient.removeLocationUpdates(locationCallback)
//        stopForeground(STOP_FOREGROUND_REMOVE)
//    }
//    private fun startLocationUpdates() {
//        println("startLocationUpdates")
//
//        try {
//            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
//        } catch (e: SecurityException) {
//            e.printStackTrace()
//        }
//    }
//
//    private val locationCallback = object : LocationCallback() {
//        override fun onLocationResult(locationResult: LocationResult) {
//            println("onLocationResult")
//            val location: Location? = locationResult.lastLocation
//            println(location)
//            if (location != null) {
//                sendLocationToServer(location)
//            }
//        }
//    }
//
//    private fun sendLocationToServer(location: Location) {
//        val JSONBody=JSONObject()
//            .put("finePermission",finePermission)
//            .put("foreGround",foreground)
//            .put("priority",priority)
//            .put("user", Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
//            .put("latitude",location.latitude)
//            .put("longitude",location.longitude)
//            .put("time",location.time)
//            .put("accuracy",location.accuracy)
//
//
//        println(JSONBody.toString())
//
//        Thread{
//            try {
//                val client=OkHttpClient()
//                val request= Request.Builder()
//                    .url("https://masterproefmilanschollier.azurewebsites.net/location")
//                    .post(JSONBody.toString().toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull()))
//                    .build()
//                val response=client.newCall(request).execute()
//                if (response.isSuccessful) {
//                    // Handle successful response
//                    println("Response: ${response.body?.string()}")
//                } else {
//                    // Handle error response
//                    println("Error: ${response.code} - ${response.message}")
//                }
//            }catch (e: IOException){
//                println("Error: ${e.message}")
//            }
//
//        }.start()
//
//
//    }
//
//    override fun onBind(p0: Intent?): IBinder? {
//        return null
//    }
//}