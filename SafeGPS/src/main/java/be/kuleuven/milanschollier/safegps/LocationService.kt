package be.kuleuven.milanschollier.safegps

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder

class LocationService : Service() {
    private var callback: ((Location)-> Unit)? = null
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

}