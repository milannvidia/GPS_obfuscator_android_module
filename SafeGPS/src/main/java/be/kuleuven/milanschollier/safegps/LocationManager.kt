package be.kuleuven.milanschollier.safegps

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import java.io.File


class LocationManager private constructor() {
    companion object {
        @Volatile
        private var instance: LocationManager? = null
        private var fileDirectory: File? = null
        fun getInstance(context: Context) =
            instance ?: synchronized(this){
                instance ?: LocationManager().also {
                    instance = it
                    fileDirectory=context.filesDir
                }
            }
    }

    private var finePermission: Boolean = false
    private var coarsePermission: Boolean = false
    private var foregroundPermission: Boolean = false
    private var priority: Int = 0
    private var interval: Long = 0
    private var locationObfuscator: LocationObfuscator? = null

    fun setFinePermission(context: Context, activity: ComponentActivity, permission: Boolean):Boolean {
        this.finePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (this.finePermission) return true

        if (permission) {
            activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                //TODO: Check if permissions are granted
                println(permissions)
            }.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        return this.finePermission
    }

    fun setCoarsePermission(context: Context, activity: ComponentActivity, permission: Boolean):Boolean {
        this.coarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (this.coarsePermission) return true

        if (permission) {
            activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                res -> this.coarsePermission=res
            }.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return this.coarsePermission
    }

    fun setForegroundPermission(permission: Boolean) {
        this.foregroundPermission = permission
    }

    fun setPriority(priority: Int) {
        this.priority = priority
    }

    fun setInterval(interval: Long) {
        this.interval = interval
    }

    fun setLocationObfuscator(locationObfuscator: LocationObfuscator) {
        this.locationObfuscator = locationObfuscator
    }


}


class LocationService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

}