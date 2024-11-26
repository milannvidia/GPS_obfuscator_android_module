package be.kuleuven.milanschollier.safegps

import android.content.Context
import android.location.Location
import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.GeodesicMask
import java.io.File
import java.io.IOException
import kotlin.math.PI
import kotlin.random.Random.Default.nextDouble
typealias PrivacyBlob = Triple<Double, Double, Double> // (x, y, radius)
typealias Settings = Pair<Double, Timestamp>         // radius, time interval TODO: ook een tijdsvariantie erop steken niet enkel cooldown
typealias Timestamp = Long
typealias LatLonTs = Triple<Double, Double, Timestamp> // lon, lat, timestamp

interface LocationObfuscator {
    fun perturbLocation(location: Location): LatLonTs
    fun load(filesDir: File)
    fun store(filesDir: File)
}

class LocationObfuscatorV1 private constructor() : LocationObfuscator {

    companion object {
        @Volatile
        private var instance: LocationObfuscatorV1? = null
        fun getInstance(filesDir: File): LocationObfuscatorV1 {
            return instance ?: synchronized(this) {
                instance ?: LocationObfuscatorV1().also {
                    instance = it
                    instance!!.load(filesDir)
                }
            }
        }

    }
    private val historyBlobs= mutableListOf<PrivacyBlob>()
    private val settings:Settings = Pair(100.0, 3_600_000)
    private val perturbedLocationCache = mutableListOf<LatLonTs>()
    val angleSampler = { nextDouble(0.0, 2 * PI) }
    val radiusSampler = { nextDouble(0.0, settings.first) }

    override fun perturbLocation(location: Location): LatLonTs {
        val lat = location.latitude
        val lon = location.longitude
        val ts:Timestamp = location.time

        val lastLocation = perturbedLocationCache.lastOrNull()?:LatLonTs(0.0,0.0,0)
        if (ts - lastLocation.third < settings.second) {
            return lastLocation
        }

        //Voor jitter tegen te gaan als in in de buurt van vorige locatie die privacy blob doorsturen ongeacht andere dichter is
        if(Geodesic.WGS84.Inverse(lastLocation.first,lastLocation.second,lat,lon, GeodesicMask.DISTANCE).s12 > settings.first){
            return LatLonTs(lastLocation.first,lastLocation.second,ts)
        }
        
        val matchedReports = historyBlobs.filter { report ->
            Geodesic.WGS84.Inverse(
                report.first,
                report.second,
                lat,
                lon,
                GeodesicMask.DISTANCE
            ).s12 < report.third
        }
        if (matchedReports.isNotEmpty()) {
            val closestReport = matchedReports.minByOrNull { report ->
                Geodesic.WGS84.Inverse(report.first,report.second,lat,lon, GeodesicMask.DISTANCE).s12
            }!!
            perturbedLocationCache.add(LatLonTs(closestReport.first, closestReport.second, ts))
            if (perturbedLocationCache.size>5 ) perturbedLocationCache.removeAt(0)
            return LatLonTs(closestReport.first, closestReport.second, ts)
        }

        val sampledRadius = radiusSampler()
        val sampledAngle = Math.toDegrees(angleSampler())
        val res= Geodesic.WGS84.Direct(lat,lon,sampledAngle,sampledRadius)
        val newHistoryBlob=PrivacyBlob(res.lat2,res.lon2,settings.first)
        historyBlobs.add(newHistoryBlob)
        perturbedLocationCache.add(LatLonTs(res.lat2, res.lon2, ts))
        if (perturbedLocationCache.size>5 ) perturbedLocationCache.removeAt(0)
        return LatLonTs(res.lat2, res.lon2, ts)
    }

    override fun load(filesDir:File) {
        loadHistoryBlobs(File(filesDir,"LocationObfuscatorV1_historyBlobs.txt"))
        loadSettings(File(filesDir,"LocationObfuscatorV1_settings.txt"))
    }

    override fun store(filesDir:File) {
        saveHistoryBlobs(File(filesDir,"LocationObfuscatorV1_historyBlobs.txt"))
        saveSettings(File(filesDir,"LocationObfuscatorV1_settings.txt"))
    }

    private fun loadHistoryBlobs(file: File): List<PrivacyBlob> {
        return if (file.exists()) {
            file.readLines().map { line ->
                val (x, y, radius) = line.split(",").map { it.toDouble() }
                PrivacyBlob(x, y, radius)
            }
        } else {
            return emptyList()
        }
    }
    private fun saveHistoryBlobs(file: File) {
        try {
            file.writeText(historyBlobs.joinToString("\n") { "${it.first},${it.second},${it.third}" })

        }catch (e: IOException){
            println("Error: ${e.message}")
        }
    }
    private fun getHistoryBlobs(): List<PrivacyBlob> {
        return historyBlobs.map{it.copy()}
    }
    private fun loadSettings(file: File): Settings {
        return if (file.exists()) {
            val (radius, interval) = file.readText().split(",")
            Settings(radius.toDouble(), interval.toLong())
        } else {
            return Settings(100.0, 3_600_000)
        }
    }
    private fun saveSettings(file: File) {
        try {
            file.writeText("${settings.first},${settings.second}")
        }catch (e: IOException){
            println("Error: ${e.message}")
        }
    }


}