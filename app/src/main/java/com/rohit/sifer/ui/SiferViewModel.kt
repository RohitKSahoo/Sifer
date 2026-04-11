package com.rohit.sifer.ui

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
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

    private val context = application.applicationContext
    private val zoneDao = AppDatabase.getDatabase(application).zoneDao()
    private val geofenceManager = GeofenceManager(application)
    private val actionEngine = ActionEngine(application)
    private val prefs = application.getSharedPreferences("sifer_prefs", Context.MODE_PRIVATE)

    val allZones: Flow<List<Zone>> = zoneDao.getAllZones()
    val isServiceEnabled = mutableStateOf(prefs.getBoolean("service_enabled", true))
    
    // NEW Automation Rule States (Simplified)
    val isDndEnabled = mutableStateOf(prefs.getBoolean("rule_dnd", true))
    val isVibrateEnabled = mutableStateOf(prefs.getBoolean("rule_vibrate", false))
    val isMediaMuteEnabled = mutableStateOf(prefs.getBoolean("rule_media_mute", false))

    // Real Permission States
    val hasLocationPermission = mutableStateOf(checkLocationPermission())
    val hasDndPermission = mutableStateOf(checkDndPermission())
    val isBatteryOptimized = mutableStateOf(checkBatteryOptimization())
    val isAutoStartEnabled = mutableStateOf(prefs.getBoolean("auto_start_on_boot", true))

    // Activity Logging
    val activityHistory = mutableStateListOf<ActivityLog>()

    init {
        refreshPermissionStates()
    }

    fun refreshPermissionStates() {
        hasLocationPermission.value = checkLocationPermission()
        hasDndPermission.value = checkDndPermission()
        isBatteryOptimized.value = checkBatteryOptimization()
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkDndPermission(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nm.isNotificationPolicyAccessGranted
        } else true
    }

    private fun checkBatteryOptimization(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !pm.isIgnoringBatteryOptimizations(context.packageName)
        } else false
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openDndSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun toggleAutoStart(enabled: Boolean) {
        isAutoStartEnabled.value = enabled
        prefs.edit().putBoolean("auto_start_on_boot", enabled).apply()
    }

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
    
    // UPDATED Toggle Handlers with Mutual Exclusion and Real-time refresh
    fun toggleDndRule(enabled: Boolean) {
        if (enabled) {
            isVibrateEnabled.value = false
            prefs.edit().putBoolean("rule_vibrate", false).apply()
        }
        isDndEnabled.value = enabled
        prefs.edit().putBoolean("rule_dnd", enabled).apply()
        actionEngine.refreshCurrentState()
    }
    
    fun toggleVibrateRule(enabled: Boolean) {
        if (enabled) {
            isDndEnabled.value = false
            prefs.edit().putBoolean("rule_dnd", false).apply()
        }
        isVibrateEnabled.value = enabled
        prefs.edit().putBoolean("rule_vibrate", enabled).apply()
        actionEngine.refreshCurrentState()
    }
    
    fun toggleMediaMuteRule(enabled: Boolean) {
        isMediaMuteEnabled.value = enabled
        prefs.edit().putBoolean("rule_media_mute", enabled).apply()
        actionEngine.refreshCurrentState()
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
