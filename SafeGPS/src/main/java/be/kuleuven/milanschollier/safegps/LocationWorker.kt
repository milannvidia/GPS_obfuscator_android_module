package be.kuleuven.milanschollier.safegps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class LocationWorker(context: Context, workerParams: WorkerParameters,

) :
    Worker(context,workerParams) {
    private val locationManager=LocationManager.getInstance()
    private lateinit var locationClient: FusedLocationProviderClient
    override fun doWork(): Result {
        return try{
            println("doWork")
            locationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            if (locationManager.foregroundService) {

                val notification =
                    NotificationCompat.Builder(applicationContext, "locationTrackingChannel")
                        .setContentTitle("Location Tracking")
                        .setTicker("Location Tracking")
                        .setContentText("Tracking your location in the background")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setOngoing(true)
                        .build()

                val foregroundInfo = ForegroundInfo(1001,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                setForegroundAsync(foregroundInfo)
            }
            startLocationUpdates()
            if(locationManager.foregroundService) Thread.sleep(Long.MAX_VALUE)
            else Thread.sleep(locationManager.maxRuntime)
            Result.success()
        }catch (e:Exception){
            e.printStackTrace()
            Result.failure()
        }finally {
            locationClient.removeLocationUpdates(locationCallback)
        }
    }
    private fun startLocationUpdates() {
        println("startLocationUpdates priority: ${locationManager.priority} interval: ${locationManager.interval}")
        val locationRequest = LocationRequest.Builder(locationManager.priority, locationManager.interval)
            .build()

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("no permission")
            return
        }
        try {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                if(LocationManager.getInstance().foregroundService) Looper.getMainLooper()
                else Looper.myLooper()
            )
            println("Location updates requested successfully")
        }catch (e:Exception){
            println("Location updates failed: ${e.message}")
            e.printStackTrace()
        }

    }
    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                locationManager.useCallback(location)
            }
        }
    }
}