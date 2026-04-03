package com.rohit.sifer.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
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

data class ActivityLog(val title: String, val subtitle: String, val timestamp: String = "Just now")

class SiferViewModel(application: Application) : AndroidViewModel(application) {

    private val zoneDao = AppDatabase.getDatabase(application).zoneDao()
    private val geofenceManager = GeofenceManager(application)
    private val actionEngine = ActionEngine(application)
    private val prefs = application.getSharedPreferences("sifer_prefs", Context.MODE_PRIVATE)

    val allZones: Flow<List<Zone>> = zoneDao.getAllZones()
    val isServiceEnabled = mutableStateOf(prefs.getBoolean("service_enabled", true))
    
    // Automation Rule States
    val isAutoSilenceEnabled = mutableStateOf(prefs.getBoolean("auto_silence_enabled", true))
    val isWifiShieldEnabled = mutableStateOf(prefs.getBoolean("wifi_shield_enabled", false))

    // Activity Logging
    val activityHistory = mutableStateListOf<ActivityLog>()

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
    
    fun toggleAutoSilence(enabled: Boolean) {
        isAutoSilenceEnabled.value = enabled
        prefs.edit().putBoolean("auto_silence_enabled", enabled).apply()
    }
    
    fun toggleWifiShield(enabled: Boolean) {
        isWifiShieldEnabled.value = enabled
        prefs.edit().putBoolean("wifi_shield_enabled", enabled).apply()
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
            
            logActivity("Created Haven '$name'", "Protocol initialized at current coordinates.")
        }
    }

    fun deleteZone(zone: Zone) {
        viewModelScope.launch {
            geofenceManager.removeGeofence(zone)
            zoneDao.deleteZone(zone)
            logActivity("Deleted Haven '${zone.name}'", "Monitoring protocol terminated.")
        }
    }

    fun toggleZone(zone: Zone) {
        viewModelScope.launch {
            val updatedZone = zone.copy(isEnabled = !zone.isEnabled)
            zoneDao.updateZone(updatedZone)
            if (isServiceEnabled.value) {
                if (updatedZone.isEnabled) {
                    geofenceManager.addGeofence(updatedZone)
                    logActivity("Enabled '${zone.name}'", "Resumed monitoring for this location.")
                } else {
                    geofenceManager.removeGeofence(updatedZone)
                    actionEngine.exitZone(updatedZone.id.toString())
                    logActivity("Disabled '${zone.name}'", "Paused monitoring for this location.")
                }
            }
        }
    }

    private fun logActivity(title: String, subtitle: String) {
        activityHistory.add(0, ActivityLog(title, subtitle))
        if (activityHistory.size > 5) activityHistory.removeAt(activityHistory.size - 1)
    }
}
