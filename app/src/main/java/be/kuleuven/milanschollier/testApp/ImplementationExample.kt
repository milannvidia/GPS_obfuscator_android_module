package be.kuleuven.milanschollier.testApp

import android.location.Location
import be.kuleuven.milanschollier.safegps.LatLonTs
import be.kuleuven.milanschollier.safegps.LocationObfuscator
import java.io.File

class ImplementationExample private constructor() :LocationObfuscator {
    companion object {
        @Volatile
        private var instance: ImplementationExample? = null
        fun getInstance(): ImplementationExample {
            return instance ?: synchronized(this) {
                instance ?: ImplementationExample().also {
                    instance = it
                }
            }
        }
    }
    override fun obfuscateLocation(location: Location): LatLonTs {
        return LatLonTs(location.latitude, location.longitude, location.time)
    }
    override fun load(filesDir: File) {return}
    override fun store(filesDir: File) {return}
    override fun clearStorage(filesDir: File) {return}
}