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
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.sifer.data.AppDatabase
import com.rohit.sifer.data.Zone
import com.rohit.sifer.logic.ActionEngine
import com.rohit.sifer.logic.GeofenceManager
import com.rohit.sifer.logic.UpdateInfo
import com.rohit.sifer.logic.UpdateManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SiferViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val zoneDao = AppDatabase.getDatabase(application).zoneDao()
    private val geofenceManager = GeofenceManager(application)
    private val actionEngine = ActionEngine(application)
    private val updateManager = UpdateManager(application)
    private val prefs = application.getSharedPreferences("sifer_prefs", Context.MODE_PRIVATE)

    // Concurrency control to prevent crashes during rapid toggling
    private val toggleMutex = Mutex()

    val allZones: Flow<List<Zone>> = zoneDao.getAllZones()
    val isServiceEnabled = mutableStateOf(prefs.getBoolean("service_enabled", true))
    
    val isDndEnabled = mutableStateOf(prefs.getBoolean("rule_dnd", true))
    val isVibrateEnabled = mutableStateOf(prefs.getBoolean("rule_vibrate", false))
    val isMediaMuteEnabled = mutableStateOf(prefs.getBoolean("rule_media_mute", false))

    // Real Permission States
    val hasLocationPermission = mutableStateOf(checkLocationPermission())
    val hasBackgroundLocationPermission = mutableStateOf(checkBackgroundLocationPermission())
    val hasDndPermission = mutableStateOf(checkDndPermission())
    val hasNotificationAccess = mutableStateOf(checkNotificationAccess())
    val isBatteryOptimized = mutableStateOf(checkBatteryOptimization())
    val isAutoStartEnabled = mutableStateOf(prefs.getBoolean("auto_start_on_boot", true))

    // Update State
    val updateInfo = mutableStateOf<UpdateInfo?>(null)
    val isCheckingForUpdates = mutableStateOf(false)

    // Excluded Apps State
    val excludedApps = mutableStateOf<Set<String>>(
        prefs.getStringSet("excluded_apps", emptySet()) ?: emptySet()
    )

    fun toggleAppExclusion(packageName: String) {
        val current = excludedApps.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        excludedApps.value = current
        prefs.edit().putStringSet("excluded_apps", current).apply()
    }

    init {
        refreshPermissionStates()
        triggerUpdateCheck(silent = true)
        
        // Initial sync on app start to catch if we're already in a haven
        viewModelScope.launch {
            actionEngine.refreshCurrentState()
        }
    }

    fun triggerUpdateCheck(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) isCheckingForUpdates.value = true
            val info = updateManager.checkForUpdates()
            updateInfo.value = info
            if (!silent) {
                isCheckingForUpdates.value = false
                if (!info.hasUpdate) {
                    Toast.makeText(context, "Sifer is up to date!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // In-app Update Download States
    val downloadProgress = mutableStateOf<Float?>(null)
    val downloadError = mutableStateOf<String?>(null)
    var downloadedApkFile: java.io.File? = null

    fun downloadUpdate() {
        val url = updateInfo.value?.downloadUrl ?: return
        downloadProgress.value = 0f
        downloadError.value = null
        downloadedApkFile = null

        viewModelScope.launch {
            updateManager.downloadApkWithProgress(
                urlString = url,
                fileName = "Sifer_Update.apk",
                onProgress = { progress ->
                    downloadProgress.value = progress
                },
                onComplete = { file ->
                    downloadProgress.value = null
                    downloadedApkFile = file
                    checkAndInstallApk(file)
                },
                onError = { errorMsg ->
                    downloadProgress.value = null
                    downloadError.value = errorMsg
                }
            )
        }
    }

    fun canInstallPackages(): Boolean {
        return updateManager.canInstallPackages()
    }

    fun checkAndInstallApk(file: java.io.File? = null) {
        val finalFile = file ?: downloadedApkFile ?: return
        if (updateManager.canInstallPackages()) {
            downloadedApkFile = null
            updateManager.installApk(finalFile)
        } else {
            // Keep the file reference and request permission
            downloadedApkFile = finalFile
            updateManager.requestInstallPermission()
        }
    }

    fun refreshPermissionStates() {
        hasLocationPermission.value = checkLocationPermission()
        hasBackgroundLocationPermission.value = checkBackgroundLocationPermission()
        hasDndPermission.value = checkDndPermission()
        isBatteryOptimized.value = checkBatteryOptimization()
        hasNotificationAccess.value = checkNotificationAccess()
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkDndPermission(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nm.isNotificationPolicyAccessGranted
        } else true
    }

    private fun checkNotificationAccess(): Boolean {
        val packageNames = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
        return packageNames.contains(context.packageName)
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

    fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
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
                // Force check if we're already in a haven when service starts
                actionEngine.refreshCurrentState()
            } else {
                geofenceManager.removeAllGeofences()
                actionEngine.stopServiceManually()
            }
        }
    }
    
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
                // Check if the newly added zone should be active immediately
                actionEngine.refreshCurrentState()
            }
        }
    }

    fun deleteZone(zone: Zone) {
        viewModelScope.launch {
            geofenceManager.removeGeofence(zone)
            // Ensure ActionEngine removes it from active list if it was active
            actionEngine.exitZone(zone.id.toString())
            zoneDao.deleteZone(zone)
        }
    }

    fun toggleZone(zone: Zone) {
        viewModelScope.launch {
            // Guard with Mutex to handle rapid clicks sequentially
            toggleMutex.withLock {
                // Fetch fresh state from DB instead of using stale UI object
                val currentZone = zoneDao.getZoneById(zone.id) ?: return@withLock
                val newEnabledState = !currentZone.isEnabled
                
                val updatedZone = currentZone.copy(isEnabled = newEnabledState)
                zoneDao.updateZone(updatedZone)
                
                if (isServiceEnabled.value) {
                    if (newEnabledState) {
                        geofenceManager.addGeofence(updatedZone)
                        // PROXIMITY SYNC: Force check if we are already inside the newly enabled zone
                        actionEngine.refreshCurrentState()
                    } else {
                        geofenceManager.removeGeofence(updatedZone)
                        // Also notify ActionEngine to restore state if this was the active zone
                        actionEngine.exitZone(updatedZone.id.toString())
                    }
                }
            }
        }
    }
}
