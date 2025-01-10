package be.kuleuven.milanschollier.safegps

import android.location.Location
import java.io.File


interface LocationObfuscator {
    fun obfuscateLocation(location: Location): LatLonTs?
    fun load(filesDir: File)
    fun store(filesDir: File)
    fun clearStorage(filesDir: File)
}

