package com.rohit.sifer.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.sifer.data.AppDatabase
import com.rohit.sifer.data.Zone
import com.rohit.sifer.logic.ActionEngine
import com.rohit.sifer.logic.GeofenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SiferViewModel(application: Application) : AndroidViewModel(application) {

    private val zoneDao = AppDatabase.getDatabase(application).zoneDao()
    private val geofenceManager = GeofenceManager(application)
    private val actionEngine = ActionEngine(application)
    private val prefs = application.getSharedPreferences("sifer_prefs", Context.MODE_PRIVATE)

    val allZones: Flow<List<Zone>> = zoneDao.getAllZones()
    val isServiceEnabled = mutableStateOf(prefs.getBoolean("service_enabled", true))

    fun toggleService(enabled: Boolean) {
        isServiceEnabled.value = enabled
        prefs.edit().putBoolean("service_enabled", enabled).apply()
        
        viewModelScope.launch {
            if (enabled) {
                val zones = allZones.first()
                geofenceManager.addGeofences(zones)
            } else {
                geofenceManager.removeAllGeofences()
                actionEngine.stopServiceManually()
            }
        }
    }

    fun addZone(name: String, latitude: Double, longitude: Double, radius: Float) {
        viewModelScope.launch {
            val zone = Zone(
                name = name,
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                isEnabled = true
            )
            val id = zoneDao.insertZone(zone)
            val insertedZone = zone.copy(id = id.toInt())
            if (isServiceEnabled.value) {
                geofenceManager.addGeofence(insertedZone)
            }
        }
    }

    fun deleteZone(zone: Zone) {
        viewModelScope.launch {
            geofenceManager.removeGeofence(zone)
            zoneDao.deleteZone(zone)
        }
    }

    fun toggleZone(zone: Zone) {
        viewModelScope.launch {
            val updatedZone = zone.copy(isEnabled = !zone.isEnabled)
            zoneDao.updateZone(updatedZone)
            if (isServiceEnabled.value) {
                if (updatedZone.isEnabled) {
                    geofenceManager.addGeofence(updatedZone)
                } else {
                    geofenceManager.removeGeofence(updatedZone)
                    // If disabling an individual zone, inform the engine to handle possible state restoration
                    actionEngine.exitZone(updatedZone.id.toString())
                }
            }
        }
    }
}
