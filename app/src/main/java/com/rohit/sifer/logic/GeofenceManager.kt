package com.rohit.sifer.logic

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.rohit.sifer.data.Zone

class GeofenceManager(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(zone: Zone, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        if (!zone.isEnabled) return

        val geofence = Geofence.Builder()
            .setRequestId(zone.id.toString())
            .setCircularRegion(zone.latitude, zone.longitude, zone.radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence added: ${zone.name}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to add geofence: ${e.message}")
                onFailure(e)
            }
    }

    @SuppressLint("MissingPermission")
    fun addGeofences(zones: List<Zone>, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val geofenceList = zones.filter { it.isEnabled }.map { zone ->
            Geofence.Builder()
                .setRequestId(zone.id.toString())
                .setCircularRegion(zone.latitude, zone.longitude, zone.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        if (geofenceList.isEmpty()) return

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceManager", "Multiple geofences added")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to add geofences: ${e.message}")
                onFailure(e)
            }
    }

    fun removeGeofence(zone: Zone, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        geofencingClient.removeGeofences(listOf(zone.id.toString()))
            .addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence removed: ${zone.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to remove geofence: ${e.message}")
                onFailure(e)
            }
    }

    fun removeAllGeofences(onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceManager", "All geofences removed from system")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to remove all geofences: ${e.message}")
                onFailure(e)
            }
    }
}
