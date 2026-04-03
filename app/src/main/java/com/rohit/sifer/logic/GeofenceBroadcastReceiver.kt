package com.rohit.sifer.logic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "GeofencingEvent error: ${geofencingEvent.errorCode}")
            return
        }

        val actionEngine = ActionEngine(context)
        val transition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()

        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                triggeringGeofences.forEach { geofence ->
                    Log.d("GeofenceReceiver", "Geofence ENTER: ${geofence.requestId}")
                    actionEngine.enterZone(geofence.requestId)
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                triggeringGeofences.forEach { geofence ->
                    Log.d("GeofenceReceiver", "Geofence EXIT: ${geofence.requestId}")
                    actionEngine.exitZone(geofence.requestId)
                }
            }
            else -> {
                Log.e("GeofenceReceiver", "Unknown geofence transition: $transition")
            }
        }
    }
}
