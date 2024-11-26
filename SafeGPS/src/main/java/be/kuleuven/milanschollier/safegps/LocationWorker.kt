package be.kuleuven.milanschollier.safegps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import com.google.android.gms.location.Priority

class LocationWorker(context: Context, workerParams: WorkerParameters,

) :
    Worker(context,workerParams) {
    private val locationManager=LocationManager.getInstance(context as ComponentActivity)
    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    override fun doWork(): Result {
        try{
            if (locationManager.foregroundService) {
                val notification =
                    NotificationCompat.Builder(applicationContext, "locationTrackingChannel")
                        .setContentTitle("Location Tracking")
                        .setContentText("Tracking your location in the background")
                        .setSmallIcon(com.google.android.material.R.drawable.abc_ic_star_black_16dp)
                        .build()

                val foregroundInfo = ForegroundInfo(1, notification)
                this.setForegroundAsync(foregroundInfo)
            }

            if (true) {
                startLocationUpdates()
                Thread.sleep(locationManager.maxRuntime)
            }
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
            return
        }
        locationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    private fun getLocation(): Location? {
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
            return null
        }
        var res:Location?=null
        locationClient.getCurrentLocation(locationManager.priority, null)
            .addOnSuccessListener { location -> res= location }
            .addOnFailureListener { e -> println(e) }
        return res
    }
    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location? = locationResult.lastLocation
            if (location != null) {
                locationManager.callback?.invoke(location)
            }
        }
    }
}