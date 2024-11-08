package be.kuleuven.milanschollier.safegps

import android.location.Location
import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.GeodesicMask
import kotlin.math.PI
import kotlin.random.Random.Default.nextDouble
typealias PrivacyBlob = Triple<Double, Double, Double> // (x, y, radius)
typealias Settings = Pair<Double, Timestamp>         // radius, time interval
typealias Timestamp = Long
typealias LatLonTs = Triple<Double, Double, Timestamp> // lon, lat, timestamp
typealias PathLatLonTs = List<LatLonTs>

class LocationObfuscator {
    private val historyBlobs = mutableListOf<PrivacyBlob>()
    private val settings = Settings(100.0, 3600)
    private val perturbedLocationCache = mutableListOf<LatLonTs>()
    val angleSampler = { nextDouble(0.0, 2 * PI) }
    val radiusSampler = { nextDouble(0.0, settings.first) }

    fun perturbLocation(location: Location): LatLonTs {
        val lat = location.latitude
        val lon = location.longitude
        val ts:Timestamp = location.time

        val lastLocation = perturbedLocationCache.lastOrNull()?:LatLonTs(0.0,0.0,0)
        if (ts - lastLocation.third < settings.second) {
            return lastLocation
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
            val choiceLocation=Pair(closestReport.first,closestReport.second)
            perturbedLocationCache.add(LatLonTs(choiceLocation.first, choiceLocation.second, ts))
            return LatLonTs(closestReport.first, choiceLocation.second, ts)
        }

        val sampledRadius = radiusSampler()
        val sampledAngle = Math.toDegrees(angleSampler())
        val res= Geodesic.WGS84.Direct(lat,lon,sampledAngle,sampledRadius)
        val newHistoryBlob=PrivacyBlob(res.lat2,res.lon2,settings.first)
        historyBlobs.add(newHistoryBlob)
        perturbedLocationCache.add(LatLonTs(res.lat2, res.lon2, ts))
        return LatLonTs(res.lat2, res.lon2, ts)

    }


}