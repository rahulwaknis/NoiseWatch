package com.example.noisewatch.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val readableAddress: String? = null,
    val locality: String? = null
)

class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermission()) return null

        return try {
            val lastLocation = getLastLocation()
            val validLastLocation = if (lastLocation != null && isFresh(lastLocation)) lastLocation else null
            val locationToUse = validLastLocation ?: requestCurrentLocationFix()

            if (locationToUse != null) {
                val (address, locality) = reverseGeocode(locationToUse.latitude, locationToUse.longitude)
                LocationData(
                    latitude = locationToUse.latitude,
                    longitude = locationToUse.longitude,
                    accuracyMeters = if (locationToUse.hasAccuracy()) locationToUse.accuracy else null,
                    readableAddress = address,
                    locality = locality
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (continuation.isActive) continuation.resume(loc)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
    }

    private fun isFresh(location: Location): Boolean {
        val age = System.currentTimeMillis() - location.time
        return age < 60_000 // Fresh if taken within last 60 seconds
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocationFix(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                5000L
            )
            .setMaxUpdates(1)
            .setDurationMillis(10000L)
            .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    stopLocationUpdates()
                    if (continuation.isActive) {
                        continuation.resume(result.lastLocation)
                    }
                }
            }
            locationCallback = callback

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener {
                stopLocationUpdates()
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) return@withContext Pair(null, null)
                val geocoder = Geocoder(context, Locale.getDefault())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            if (continuation.isActive) {
                                if (addresses.isNotEmpty()) {
                                    continuation.resume(extractAddressDetails(addresses[0]))
                                } else {
                                    continuation.resume(Pair(null, null))
                                }
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        extractAddressDetails(addresses[0])
                    } else {
                        Pair(null, null)
                    }
                }
            } catch (e: Exception) {
                Pair(null, null)
            }
        }
    }

    private fun extractAddressDetails(address: Address): Pair<String?, String?> {
        val subLocality = address.subLocality
        val locality = address.locality ?: address.adminArea

        val derivedLocality = when {
            !subLocality.isNullBlank() && !locality.isNullBlank() && subLocality != locality -> "$subLocality, $locality"
            !subLocality.isNullBlank() -> subLocality
            !locality.isNullBlank() -> locality
            else -> null
        }

        val fullAddress = (0..address.maxAddressLineIndex)
            .mapNotNull { address.getAddressLine(it) }
            .joinToString(", ")
            .ifBlank { null }

        return Pair(fullAddress, derivedLocality)
    }

    private fun String?.isNullBlank(): Boolean = this == null || this.isBlank()

    fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }
}
