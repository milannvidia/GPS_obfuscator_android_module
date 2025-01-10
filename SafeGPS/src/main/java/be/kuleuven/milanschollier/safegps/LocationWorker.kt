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

class LocationWorker(
    context: Context, workerParams: WorkerParameters,

    ) :
    Worker(context, workerParams) {
    private val locationManager = LocationManager.getInstance()
    private lateinit var locationClient: FusedLocationProviderClient
    override fun doWork(): Result {
        return try {
            locationManager.logDebug("doWork", LogLevel.VERBOSE)
            val startTimestamp = System.currentTimeMillis()
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

                val foregroundInfo =
                    ForegroundInfo(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                setForegroundAsync(foregroundInfo)
            }
            startLocationUpdates()
            while (locationManager.running.value == true) {
                Thread.sleep(1000)
                if (!locationManager.foregroundService && System.currentTimeMillis() - startTimestamp > locationManager.maxRuntime) break
            }
            locationManager.logDebug("doWork done", LogLevel.VERBOSE)
            locationClient.removeLocationUpdates(locationCallback)
            Result.success()
        } catch (e: Exception) {
            locationManager.logDebug("doWork failed: ${e.message}", LogLevel.ERROR)
            e.printStackTrace()
            locationClient.removeLocationUpdates(locationCallback)
            Result.retry()
        }
    }
    override fun onStopped() {
        super.onStopped()
        locationManager.logDebug("do work stopped", LogLevel.VERBOSE)
        locationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        locationManager.logDebug("startLocationUpdates priority: ${locationManager.priority} interval: ${locationManager.interval}", LogLevel.VERBOSE)
        val locationRequest =
            LocationRequest.Builder(locationManager.priority, locationManager.interval)
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
            locationManager.logDebug("no permission", LogLevel.ERROR)
            return
        }
        try {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                if (LocationManager.getInstance().foregroundService) Looper.getMainLooper()
                else Looper.myLooper()
            )
            locationManager.logDebug("Location updates requested successfully", LogLevel.VERBOSE)
        } catch (e: Exception) {
            locationManager.logDebug("Location updates failed: ${e.message}", LogLevel.ERROR)
            e.printStackTrace()
        }

    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                locationManager.useCallback(location)
            }
        }
    }
}