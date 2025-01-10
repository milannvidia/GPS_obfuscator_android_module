package be.kuleuven.milanschollier.safegps

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks.await
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.max

typealias LatLonTs = Triple<Double, Double, Long> // lon, lat, timestamp

class LocationManager private constructor(private var activity: ComponentActivity) :
    DefaultLifecycleObserver {
    companion object {
        @Volatile
        private var instance: LocationManager? = null
        fun getInstance(activity: ComponentActivity) =
            instance ?: synchronized(this) {
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
    private val askCoarse = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { res ->
        this._coarsePermission.value = res
        println("coarsePermission: $_coarsePermission")
    }
    private val askFine = activity.registerForActivityResult(
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
    private var _foregroundService: Boolean = true
    private var _priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    private var _maxRuntime: Long = 10 * 60 * 1000
    private var _interval: Long = 1000
    private var _locationObfuscator: LocationObfuscator? = null
    private var _callback: ((LatLonTs) -> Unit)? = null
    private var _debugCallback: ((LatLonTs, LatLonTs) -> Unit)? = null
    private var _logLevel: LogLevel = LogLevel.DEBUG

    var foregroundService: Boolean
        get() = _foregroundService
        set(value) {
            this.logDebug("foregroundService set: $value",LogLevel.VERBOSE)
            _foregroundService = value
        }
    var logLevel: LogLevel
        get() = _logLevel
        set(value) {
            this.logDebug("logLevel set: ${value.asString()}",LogLevel.VERBOSE)
            _logLevel = value
        }
    var priority: Int
        get() = _priority
        set(value) {
            if (Priority.PRIORITY_HIGH_ACCURACY != value &&
                Priority.PRIORITY_LOW_POWER != value &&
                Priority.PRIORITY_BALANCED_POWER_ACCURACY != value &&
                Priority.PRIORITY_PASSIVE != value
            ) return
            this.logDebug("priority set: $value",LogLevel.VERBOSE)
            _priority = value
        }
    var maxRuntime: Long
        get() = _maxRuntime
        set(value) {
            if (value < 0) return
            var res = value
            if (res > 10 * 60 * 1000) res = 10 * 60 * 1000
            this.logDebug("maxRuntime set: $res",LogLevel.VERBOSE)
            _maxRuntime = res
        }
    var interval: Long
        get() = _interval
        set(value) {
            if (value < 0) return
            var res = value
            if (res < 120000 && _finePermission.value == false) res = 120000

            this.logDebug("interval set: $res",LogLevel.VERBOSE)
            _interval = res
        }
    var locationObfuscator: LocationObfuscator?
        get() = _locationObfuscator
        set(value) {
            this.logDebug("locationObfuscator set",LogLevel.VERBOSE)
            _locationObfuscator = value
        }
    var callback: ((LatLonTs) -> Unit)?
        get() = _callback
        set(value) {
            this.logDebug("callback set",LogLevel.VERBOSE)
            _callback = value
        }
    var debugCallback: ((LatLonTs, LatLonTs) -> Unit)?
        get() {
            return null
        }
        set(value) {
            this.logDebug("debugCallback set",LogLevel.VERBOSE)
            _debugCallback = value
        }

    override fun onCreate(owner: LifecycleOwner) {
        this.logDebug("onCreate",LogLevel.VERBOSE)
        getFinePermission(false)
        getCoarsePermission(false)
    }

    override fun onStart(owner: LifecycleOwner) {
        this.logDebug("onStart",LogLevel.VERBOSE)
    }

    override fun onResume(owner: LifecycleOwner) {
        this.logDebug("onResume",LogLevel.VERBOSE)
    }

    override fun onPause(owner: LifecycleOwner) {
        this.logDebug("onPause",LogLevel.VERBOSE)
    }

    override fun onStop(owner: LifecycleOwner) {
        this.logDebug("onStop",LogLevel.VERBOSE)
        locationObfuscator?.store(activity.filesDir)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        this.logDebug("onDestroy",LogLevel.VERBOSE)
    }

    fun getFinePermission(askIfNecessary: Boolean = false): Boolean {
        this.logDebug("getFinePermission",LogLevel.VERBOSE)
        this._finePermission.value = activity.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (this._finePermission.value == true) {
            this.logDebug("already granted",LogLevel.VERBOSE)
            return true
        }

        if (askIfNecessary) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${activity.packageName}")
                startActivity(activity, intent, null)
            } else askFine.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        return this._finePermission.value == true
    }

    fun getCoarsePermission(askIfNecessary: Boolean = false): Boolean {
        this.logDebug("getCoarsePermission",LogLevel.VERBOSE)
        this._coarsePermission.value = activity.checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (this._coarsePermission.value == true) {
            this.logDebug("already granted",LogLevel.VERBOSE)
            return true
        }

        if (askIfNecessary) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${activity.packageName}")
                startActivity(activity, intent, null)
            } else askCoarse.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return this._coarsePermission.value == true
    }

    fun useCallback(location: Location) {
        this.logDebug("useCallback",LogLevel.VERBOSE)
        if (_locationObfuscator == null) {
            _callback?.invoke(Triple(location.latitude, location.longitude, location.time))
        } else {
            val obfuscatedLocation = this._locationObfuscator!!.obfuscateLocation(location)
            _debugCallback?.invoke(
                Triple(
                    location.latitude,
                    location.longitude,
                    location.time
                ), obfuscatedLocation
            )
            _callback?.invoke(obfuscatedLocation)
        }
    }

    fun startTracking() {
        this.logDebug("startTracking",LogLevel.VERBOSE)
        if (_running.value == true) {
            this.logDebug("already running, so stopping",LogLevel.INFO)
            stopTracking()
        }
        if (_callback == null && _debugCallback == null) return
        _locationObfuscator?.load(activity.filesDir)
        if (_foregroundService) {
            setupNotificationChannel()
            val worker = OneTimeWorkRequestBuilder<LocationWorker>()
                .build()
            WorkManager.getInstance(activity).enqueueUniqueWork(
                "location",
                ExistingWorkPolicy.REPLACE,
                worker
            )
            this.logDebug("started foreground service",LogLevel.INFO)
        } else {
            val worker = PeriodicWorkRequestBuilder<LocationWorker>(
                max(_interval, MIN_PERIODIC_INTERVAL_MILLIS), TimeUnit.MILLISECONDS
            )
                .build()
            WorkManager.getInstance(activity).enqueueUniquePeriodicWork(
                "location",
                ExistingPeriodicWorkPolicy.UPDATE,
                worker
            )
            this.logDebug("started background service",LogLevel.INFO)
        }
        _running.value = true
    }

    private fun setupNotificationChannel() {
        val channelId = "locationTrackingChannel"
        val channelName = "Location Tracking"
        val channelDescription = "Location tracking is active"
        val notificationManager =
            activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel if not already created
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW).apply {
                description = channelDescription
            }
        notificationManager.createNotificationChannel(channel)
    }

    fun stopTracking() {
        this.logDebug("stopTracking",LogLevel.INFO)
        WorkManager.getInstance(activity).cancelUniqueWork("location")
        _running.value = false
    }

    fun getLocation(param: (LatLonTs) -> Unit) {
        this.logDebug("getLocation",LogLevel.INFO)
        thread {
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
                this.logDebug("no permission",LogLevel.ERROR)
                return@thread
            }
            val locationClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(activity)
            val request = locationClient.getCurrentLocation(this.priority, null)
                .addOnFailureListener { e ->
                    println(e)
                }
            await(request)
            if (request.result==null){
                this.logDebug("no location",LogLevel.ERROR)
                return@thread
            }
            if (_locationObfuscator == null) {
                param(Triple(request.result.latitude, request.result.longitude, request.result.time))
            } else {
                _locationObfuscator?.load(activity.filesDir)
                param(this._locationObfuscator!!.obfuscateLocation(request.result))
            }
        }
    }

    fun logDebug(message: String, level: LogLevel) {


        if (level.asPriority() >= this.logLevel.asPriority()) Log.d(level.asString(), message)
        if (level.asPriority() <= LogLevel.DEBUG.asPriority()) return
        val timestamp =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logMessage = "$timestamp:${level.asString()}: $message"

        try {
            val logFile = File(this.activity.getExternalFilesDir(null), "app_debug.log")
            FileWriter(logFile, true).buffered().use { fw ->
                PrintWriter(fw).use { pw ->
                    pw.println(logMessage)
                }
            }
        } catch (e: Exception) {
            Log.e("LocationManager", "Failed to write log: ${e.message}")
        }
    }
}
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    ASSERT;
    fun asString(): String {
        return when (this) {
            VERBOSE -> "VERBOSE"
            DEBUG -> "DEBUG"
            INFO -> "INFO"
            WARNING -> "WARNING"
            ERROR -> "ERROR"
            ASSERT -> "ASSERT"
        }
    }

    fun asPriority(): Int {
        return when (this) {
            VERBOSE -> Log.VERBOSE
            DEBUG -> Log.DEBUG
            INFO -> Log.INFO
            WARNING -> Log.WARN
            ERROR -> Log.ERROR
            ASSERT -> Log.ASSERT
        }
    }
}

