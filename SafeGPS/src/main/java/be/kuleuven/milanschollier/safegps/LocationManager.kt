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
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest.Companion.MIN_PERIODIC_INTERVAL_MILLIS
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks.await
import java.util.concurrent.TimeUnit
import kotlin.math.max


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
        fun getInstance() = instance!!
    }
    init {
        activity.lifecycle.addObserver(this)
    }
    private var _finePermission: MutableLiveData<Boolean> = MutableLiveData(false)
    private var _coarsePermission: MutableLiveData<Boolean> = MutableLiveData(false)
    private val askCoarse=activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {res ->
        this._coarsePermission.value=res
        println("coarsePermission: $_coarsePermission")
    }
    private val askFine=activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        _finePermission.value = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        _coarsePermission.value = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        println("finePermission: $_finePermission coarsePermission: $_coarsePermission")
    }
    private var _running: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val running: LiveData<Boolean>
        get() = _running
    val finePermission: LiveData<Boolean>
        get() = _finePermission
    val coarsePermission: LiveData<Boolean>
        get() = _coarsePermission

    //  To define by user
    private var _foregroundService: Boolean = false
    private var _priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    private var _maxRuntime: Long = 10*60*1000
    private var _interval: Long = 2*60*1000
    private var _locationObfuscator: LocationObfuscator? = null
    private var _callback: ((LatLonTs) -> Unit)? = null
    var foregroundService: Boolean
        get() = _foregroundService
        set(value) {
            println("foregroundService: $value")
            _foregroundService = value
        }
    var priority: Int
        get() = _priority
        set(value) {
            println("priority: $value")
            _priority = value
        }
    var maxRuntime: Long
        get() = _maxRuntime
        set(value) {
            println("maxRuntime: $value")
            _maxRuntime = value
        }
    var interval: Long
        get() = _interval
        set(value) {
            println("interval: $value")
            _interval = value
        }
    var locationObfuscator: LocationObfuscator?
        get() = _locationObfuscator
        set(value) {
            println("locationObfuscator set")
            _locationObfuscator = value
        }
    var callback: ((LatLonTs) -> Unit)?
        get() = _callback
        set(value) {
            println("callback set")
            _callback = value
        }
    override fun onCreate(owner: LifecycleOwner) {
        println("onCreate")
        getFinePermission(activity,false)
        getCoarsePermission(activity,false)
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
        this._finePermission.value = activity.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (this._finePermission.value==true) {
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
        return this._finePermission.value==true
    }

    fun getCoarsePermission(activity: ComponentActivity, askIfNecessary: Boolean=false):Boolean {
        println("getCoarsePermission")
        this._coarsePermission.value = activity.checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (this._coarsePermission.value==true) {
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
        return this._coarsePermission.value==true
    }

    fun useCallback(location: Location) {
        if (_locationObfuscator==null) {
            _callback?.invoke(Triple(location.latitude,location.longitude,location.time))
        }else{
            _callback?.invoke(_locationObfuscator!!.obfuscateLocation(location))
        }
    }
    fun startTracking(){
        println("startTracking")
        if(_running.value==true){
            println("already running, so stopping")
            stopTracking()
        }
        if (_callback == null) return
        _locationObfuscator?.load(activity.filesDir)
        if (_foregroundService){
            val worker=OneTimeWorkRequestBuilder<LocationWorker>()
                .build()
            WorkManager.getInstance(activity).enqueueUniqueWork(
                "location",
                ExistingWorkPolicy.REPLACE,
                worker)
        }else {
            val worker= PeriodicWorkRequestBuilder<LocationWorker>(
                max(_interval,MIN_PERIODIC_INTERVAL_MILLIS), TimeUnit.MILLISECONDS
            ).build()
            WorkManager.getInstance(activity).enqueueUniquePeriodicWork(
                "location",
                ExistingPeriodicWorkPolicy.UPDATE,
                worker)
        }
        _running.value=true
    }


    fun stopTracking(){
        println("stopTracking")
        WorkManager.getInstance(activity).cancelUniqueWork("location")
        _running.value=false
    }
    fun getLocation(): LatLonTs? {
        println("getLocation")
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("no permission")
            return null
        }

        val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
        val request=locationClient.getCurrentLocation(this.priority, null)
            .addOnFailureListener {
                e -> println(e)
            }
        await(request)
        val res= request.result ?: return null
        _locationObfuscator?.load(activity.filesDir)
        return this._locationObfuscator?.obfuscateLocation(res)
    }

}


