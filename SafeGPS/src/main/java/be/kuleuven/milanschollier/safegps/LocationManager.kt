package be.kuleuven.milanschollier.safegps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.Priority
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest


class LocationManager private constructor(private var activity: ComponentActivity):DefaultLifecycleObserver{
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
    init {
        activity.lifecycle.addObserver(this)
    }
    private var finePermission: Boolean = false
    private var coarsePermission: Boolean = false
    private val askCoarse=activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {res ->
        coarsePermission=res
        println("coarsePermission: $coarsePermission")
    }
    private val askFine=activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        finePermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        coarsePermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        println("finePermission: $finePermission coarsePermission: $coarsePermission")
    }
//  To define by user
    private var _foregroundService: Boolean = false
    private var _priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    private var _maxRuntime: Long = Long.MAX_VALUE
    private var _interval: Long = Long.MAX_VALUE
    private var _locationObfuscator: LocationObfuscator? = null
    private var _callback: ((Location) -> Unit)? = null

    var foregroundService: Boolean
        get() = _foregroundService
        set(value) {
            _foregroundService = value
        }
    var priority: Int
        get() = _priority
        set(value) {
            _priority = value
        }
    var maxRuntime: Long
        get() = _maxRuntime
        set(value) {
            _maxRuntime = value
        }
    var interval: Long
        get() = _interval
        set(value) {
            _interval = value
        }
    var locationObfuscator: LocationObfuscator?
        get() = _locationObfuscator
        set(value) {
            _locationObfuscator = value
        }
    var callback: ((Location) -> Unit)?
        get() = _callback
        set(value) {
            _callback = value
        }
    override fun onCreate(owner: LifecycleOwner) {
        println("onCreate")
    }
    override fun onStart(owner: LifecycleOwner) {
        println("onStart")
    }
    override fun onResume(owner: LifecycleOwner) {
        println("onResume")
    }
    override fun onPause(owner: LifecycleOwner) {
        println("onPause")
    }
    override fun onStop(owner: LifecycleOwner) {
        println("onStop")
        locationObfuscator?.store(activity.filesDir)
    }
    override fun onDestroy(owner: LifecycleOwner) {
        println("onDestroy")
    }

    fun getFinePermission(activity: ComponentActivity, askIfNecessary: Boolean=false):Boolean {
        println("getFinePermission")
        this.finePermission = activity.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (this.finePermission) {
            println("already granted")
            return true
        }

        if (askIfNecessary) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.ACCESS_FINE_LOCATION)){
                val intent=Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data= Uri.parse("package:${activity.packageName}")
                startActivity(activity,intent,null)
            }else askFine.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        return this.finePermission
    }

    fun getCoarsePermission(activity: ComponentActivity, askIfNecessary: Boolean=false):Boolean {
        println("getCoarsePermission")
        this.coarsePermission = activity.checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (this.coarsePermission) {
            println("already granted")
            return true
        }

        if (askIfNecessary) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.ACCESS_COARSE_LOCATION)){
                val intent=Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data= Uri.parse("package:${activity.packageName}")
                startActivity(activity,intent,null)
            }else askCoarse.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return this.coarsePermission
    }

    fun startTracking(){
        println("startTracking")
    }


}


