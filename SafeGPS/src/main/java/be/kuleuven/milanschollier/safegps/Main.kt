//package be.kuleuven.milanschollier.safegps
//
//import net.sf.geographiclib.Geodesic
//import net.sf.geographiclib.GeodesicMask
//import kotlin.math.PI
//import kotlin.random.Random.Default.nextDouble


//typealias FeatureSequence = kotlin.Pair<List<List<Double>>, Double>
//typealias NodeIndexType = Int
//data class Candidate(
//    val nodeIndex: MyNodeIndex,
//    val ts: Timestamp
//) {
//    companion object {
//        fun default(): Candidate {
//            return Candidate(MyNodeIndex.default(), 0)
//        }
//    }
//}
//data class EdgeWeight(
//    val distance: Double = 0.0,
//    val bearing: Double = Double.NaN,
//    val roadType: RoadType = RoadType.UNDEFINED
//){
//    companion object {
//        fun default(): EdgeWeight {
//            return EdgeWeight(0.0, Double.NaN, RoadType.UNDEFINED)
//        }
//    }
//}
//data class State(
//    val bearing: Double = 0.0,
//    val cost: Double = 0.0,
//    val backp: Candidate = Candidate.default(),
//    val micropath: List<Candidate> = emptyList()
//){
//    companion object {
//        fun default(): State {
//            return State(0.0, 0.0, Candidate.default(), emptyList())
//        }
//    }
//}
//enum class RoadType(private val speedLimit: Double) {
//    MOTORWAY(130.0),
//    TRUNK(90.0),
//    PRIMARY(70.0),
//    SECONDARY(70.0),
//    TERTIARY(50.0),
//    UNCLASSIFIED(50.0),
//    RESIDENTIAL(30.0),
//    SERVICE(20.0),
//    MOTORWAY_LINK(130.0),
//    TRUNK_LINK(90.0),
//    PRIMARY_LINK(70.0),
//    SECONDARY_LINK(70.0),
//    MOTORWAY_JUNCTION(130.0 / 1.2),
//    UNDEFINED(50.0);
//    companion object {
//        private const val KMH_TO_MPS = 1000.0 / 3600.0
//
//        fun fromString(value: String): RoadType {
//            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNDEFINED
//        }
//    }
//
//    fun toSpeed(): Double {
//        return this.speedLimit * KMH_TO_MPS
//    }
//
//    override fun toString(): String {
//        return name.lowercase()
//    }
//}
//fun splitTracesOnTime(traces: List<PathLatLonTs>, maxTime:Long):List<List<PathLatLonTs>>{
//    return traces.map { splitSingleTraceOnTime(it,maxTime) }
//}
//
//fun splitSingleTraceOnTime(trace: PathLatLonTs, maxTime:Long):List<PathLatLonTs>{
//    val traces = mutableListOf<PathLatLonTs>()
//    val currentTrace = mutableListOf<LatLonTs>()
//    var currentTime = trace[0].third
//
//    for ((lon, lat, ts) in trace){
//        if (ts - currentTime > maxTime){
//            traces.add(currentTrace.toList())
//            currentTrace.clear()
//        }
//        currentTrace.add(LatLonTs(lon,lat,ts))
//        currentTime=ts
//    }
//    traces.add(currentTrace.toList())
//    return traces
//}
//fun perturbSingleTrace(trace: PathLatLonTs):PathLatLonTs{
//
//    val settingsRadius = settings.first
//    val settingsInterval = settings.second
//    val angleSampler = { nextDouble(0.0, 2 * PI) }
//    val radiusSampler = { nextDouble(0.0, settingsRadius) }
//
//    var choiceLocation:Pair<Double,Double> = Pair(0.0,0.0)
//    var choiceTS:Timestamp = 0
//
//    val tracemapped:PathLatLonTs = trace.map { (lat,lon,ts) ->
//
//        val currentLocation=Pair(lat,lon)
////        als nog niet lang genoeg geleden stuur vorige terug
//        if ((ts - choiceTS) < settingsInterval) {
//            return@map LatLonTs(choiceLocation.first, choiceLocation.second, choiceTS)
//        }
//
//        val matchedReports = historyBlobs.filter { report ->
//            Geodesic.WGS84.Inverse(
//                report.first,
//                report.second,
//                lat,
//                lon,
//                GeodesicMask.DISTANCE
//            ).s12 < report.third
//        }
//        if (matchedReports.isNotEmpty()) {
//            val closestReport = matchedReports.minByOrNull { report ->
//                Geodesic.WGS84.Inverse(report.first,report.second,lat,lon, GeodesicMask.DISTANCE).s12
//            }
//            choiceLocation=Pair(closestReport!!.first,closestReport.second)
//            choiceTS=ts
//            return@map LatLonTs(choiceLocation.first, choiceLocation.second, choiceTS)
//        }else{
//            val sampledRadius = radiusSampler()
//            val sampledAngle = Math.toDegrees(angleSampler())
//            val res= Geodesic.WGS84.Direct(lat,lon,sampledAngle,sampledRadius)
//            val newHistoryBlob=PrivacyBlob(res.lat2,res.lon2,settingsRadius)
//            historyBlobs.add(newHistoryBlob)
//            choiceLocation=Pair(res.lat2,res.lon2)
//            choiceTS=ts
//            return@map LatLonTs(choiceLocation.first, choiceLocation.second, choiceTS)
//        }
//
//    }
//    return tracemapped
//}



