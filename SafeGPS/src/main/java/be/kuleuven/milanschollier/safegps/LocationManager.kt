package be.kuleuven.milanschollier.safegps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.Priority
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.io.File


class LocationManager private constructor(private var activity: ComponentActivity):
    LifecycleObserver {
    companion object {
        @Volatile
        private var instance: LocationManager? = null
        fun getInstance(activity: ComponentActivity) =
            instance ?: synchronized(this){
                instance ?: LocationManager(activity).also {
                    instance = it
                }
            }
    }
    private var x=activity.lifecycle.addObserver(this)
    private var finePermission: Boolean = false
    private var coarsePermission: Boolean = false
    private var foregroundService: Boolean = false
    private var priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    private var interval: Long = Long.MAX_VALUE
    private var locationObfuscator: LocationObfuscator? = null

    @OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        locationObfuscator?.store(activity.filesDir)
    }

    fun getFinePermission(activity: ComponentActivity, askIfNecessary: Boolean=false):Boolean {
        this.finePermission = activity.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (this.finePermission) return true
        if (askIfNecessary) {
            activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                finePermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                coarsePermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            }.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        return this.finePermission
    }

    fun getCoarsePermission(activity: ComponentActivity, askIfNecessary: Boolean=false):Boolean {
        this.coarsePermission = activity.checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (this.coarsePermission) return true

        if (askIfNecessary) {
            activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {res ->
                this.coarsePermission=res
            }.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return this.coarsePermission
    }

    fun setForegroundService(permission: Boolean) {
        this.foregroundService = permission
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
    fun removeLocationObfuscator(){
        this.locationObfuscator = null
    }



}


