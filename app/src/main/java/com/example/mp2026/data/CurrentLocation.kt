package com.example.mp2026.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal

/** One foreground fix, no continuous/background tracking. Cancel on screen exit. */
class CurrentLocation(private val context: Context) {
    private var cancellation: CancellationSignal? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeout: Runnable? = null
    fun cancel() {
        cancellation?.cancel()
        cancellation = null
        timeout?.let(handler::removeCallbacks)
        timeout = null
    }
    fun request(callback: (Coordinates?) -> Unit) {
        cancel()
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) { callback(null); return }
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> { callback(null); return }
        }
        val signal = CancellationSignal()
        cancellation = signal
        var completed = false
        fun finish(point: Coordinates?) {
            if (completed || cancellation !== signal) return
            completed = true
            cancel()
            callback(point)
        }
        timeout = Runnable { finish(null) }.also { handler.postDelayed(it, 15000) }
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, signal,
                ContextCompat.getMainExecutor(context)) { location ->
                finish(location?.let { Coordinates(it.latitude, it.longitude) })
            }
        } catch (_: SecurityException) { finish(null) }
          catch (_: IllegalArgumentException) { finish(null) }
    }
}
