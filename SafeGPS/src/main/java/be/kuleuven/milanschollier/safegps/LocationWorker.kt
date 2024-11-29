package be.kuleuven.milanschollier.safegps

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import androidx.activity.ComponentActivity
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
    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    override fun doWork(): Result {
        try{
            if (locationManager.foregroundService) {

                val notification =
                    NotificationCompat.Builder(applicationContext, "locationTrackingChannel")
                        .setContentTitle("Location Tracking")
                        .setContentText("Tracking your location in the background")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .build()

                val foregroundInfo = ForegroundInfo(1, notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                setForegroundAsync(foregroundInfo)
            }
            startLocationUpdates()
            Thread.sleep(locationManager.maxRuntime)
            return Result.success()
        }catch (e:Exception){
            return Result.failure()
        }finally {
            locationClient.removeLocationUpdates(locationCallback)
        }
    }
    private fun startLocationUpdates() {
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
        locationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }
    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location? = locationResult.lastLocation
            if (location != null) {
                locationManager.useCallback(location)
            }
        }
    }
}