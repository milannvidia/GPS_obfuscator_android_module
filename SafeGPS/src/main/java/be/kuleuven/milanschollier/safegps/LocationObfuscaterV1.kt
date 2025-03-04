package be.kuleuven.milanschollier.safegps

import android.location.Location
import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.GeodesicMask
import java.io.File
import java.io.IOException
import kotlin.math.PI
import kotlin.random.Random.Default.nextDouble

typealias PrivacyBlob = Triple<Double, Double, Double> // (x, y, radius)

class LocationObfuscatorV1 private constructor() : LocationObfuscator {

    companion object {
        @Volatile
        private var instance: LocationObfuscatorV1? = null
        fun getInstance(): LocationObfuscatorV1 {
            return instance ?: synchronized(this) {
                instance ?: LocationObfuscatorV1().also {
                    instance = it
                }
            }
        }

    }

    private var historyBlobs = mutableListOf<PrivacyBlob>()
    private var _blobRadius: Double = 100.0
    private var _deltaTime: Long = 600 //seconds

    var blobRadius: Double
        get() = _blobRadius
        set(value) {
            if (value < 0) throw IllegalArgumentException("Blob radius cannot be negative")
            _blobRadius = value
            println("Blob radius set to $value")
        }
    var deltaTime: Long
        get() = _deltaTime
        set(value) {
            if (value < 0) throw IllegalArgumentException("Delta time cannot be negative")
            _deltaTime = value
        }

    private val perturbedLocationCache = mutableListOf<LatLonTs>()
    val angleSampler = { nextDouble(0.0, 2 * PI) }
    val radiusSampler = { nextDouble(0.0, _blobRadius) }

    override fun obfuscateLocation(location: Location): LatLonTs? {
        val lat = location.latitude
        val lon = location.longitude
        val ts: Long = location.time

        val matchedReports = historyBlobs.filter { report ->
            Geodesic.WGS84.Inverse(
                report.first,
                report.second,
                lat,
                lon,
                GeodesicMask.DISTANCE
            ).s12 < report.third
        }

        val blob: PrivacyBlob
        if (matchedReports.isEmpty()) {
            val sampledRadius = radiusSampler()
            val sampledAngle = Math.toDegrees(angleSampler())
            val res = Geodesic.WGS84.Direct(lat, lon, sampledAngle, sampledRadius)
            blob = PrivacyBlob(res.lat2, res.lon2, _blobRadius)
            println("Generated new privacy blob: $blob")
            historyBlobs.add(blob)
        } else {
            blob = matchedReports.minBy { report ->
                Geodesic.WGS84.Inverse(
                    report.first,
                    report.second,
                    lat,
                    lon,
                    GeodesicMask.DISTANCE
                ).s12
            }
        }

        val lastLocation = perturbedLocationCache.lastOrNull() ?: LatLonTs(0.0, 0.0, 0)

        if (ts - lastLocation.third < _deltaTime * 1000) {
            return null
        }
        //Voor jitter tegen te gaan als in in de buurt van vorige locatie die privacy blob doorsturen ongeacht andere dichter is
        val res: LatLonTs = if (Geodesic.WGS84.Inverse(
                lastLocation.first,
                lastLocation.second,
                lat,
                lon,
                GeodesicMask.DISTANCE
            ).s12 < _blobRadius
        ) {
            LatLonTs(lastLocation.first, lastLocation.second, ts)
        } else {
            LatLonTs(blob.first, blob.second, ts)
        }

        perturbedLocationCache.add(res)
        if (perturbedLocationCache.size > 5) perturbedLocationCache.removeAt(0)
        return res
    }

    override fun load(filesDir: File) {
        updateHistoryBlobs(File(filesDir, "LocationObfuscatorV1_historyBlobs.txt"))
    }

    override fun store(filesDir: File) {
        updateHistoryBlobs(File(filesDir, "LocationObfuscatorV1_historyBlobs.txt"))
    }

    override fun clearStorage(filesDir: File) {
        File(filesDir, "LocationObfuscatorV1_historyBlobs.txt").delete()
        File(filesDir, "LocationObfuscatorV1_settings.txt").delete()
        historyBlobs.clear()
    }

    private fun updateHistoryBlobs(file: File) {
        val historyBlobs: Set<PrivacyBlob> = if (file.exists()) {
            file.readLines().map { line ->
                val (x, y, radius) = line.split(",").map { it.toDouble() }
                PrivacyBlob(x, y, radius)
            }.toSet()

        } else {
            emptySet()
        }
        println("Loaded $historyBlobs privacy blobs from file")
        val updatedList = historyBlobs.union(this.historyBlobs)
        println("Loaded $updatedList privacy blobs from file")

        this.historyBlobs.clear()
        this.historyBlobs.addAll(updatedList)
        println("Loaded ${this.historyBlobs} privacy blobs from file")
        try {
            file.writeText(updatedList.joinToString("\n") { "${it.first},${it.second},${it.third}" })
        } catch (e: IOException) {
            println("Error: ${e.message}")
        }
    }

    fun getHistoryBlobs(): List<PrivacyBlob> {
        return historyBlobs.map { it.copy() }
    }
}