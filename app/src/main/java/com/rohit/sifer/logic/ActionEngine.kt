package com.rohit.sifer.logic

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.rohit.sifer.MainActivity
import com.rohit.sifer.data.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.coroutines.resume

/**
 * Extension to convert GMS Task to Coroutine.
 */
suspend fun <T> Task<T>.awaitResult(): T? = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resume(null)
        }
    }
}

class ActionEngine(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val zoneDao = AppDatabase.getDatabase(appContext).zoneDao()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
    
    private val prefs: SharedPreferences = appContext.getSharedPreferences("sifer_prefs", Context.MODE_PRIVATE)

    private val CHANNEL_ID = "sifer_protection_channel"
    private val ALERT_CHANNEL_ID = "sifer_priority_alerts_v2" 

    companion object {
        private val lock = Any()
    }

    fun enterZone(zoneId: String) {
        if (!isServiceEnabled()) return
        
        synchronized(lock) {
            val activeZones = getActiveZones().toMutableSet()
            if (activeZones.add(zoneId)) {
                saveActiveZones(activeZones)
                
                if (activeZones.size == 1) {
                    saveCurrentState()
                }
                applyRules()
                showEntryNotification(zoneId)
            }
        }
        updateStatusNotification()
    }

    fun exitZone(zoneId: String) {
        synchronized(lock) {
            val activeZones = getActiveZones().toMutableSet()
            if (activeZones.remove(zoneId)) {
                saveActiveZones(activeZones)
                
                if (activeZones.isEmpty()) {
                    restoreToDefaultRing()
                    showExitNotification()
                } else {
                    applyRules()
                }
            }
        }
        updateStatusNotification()
    }

    fun stopServiceManually() {
        synchronized(lock) {
            saveActiveZones(emptySet())
            restoreToDefaultRing()
        }
        notificationManager.cancel(1001)
    }

    @SuppressLint("MissingPermission")
    fun refreshCurrentState() {
        if (!isServiceEnabled()) {
            notificationManager.cancel(1001)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            validateActiveZones()
            try {
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).awaitResult()
                    ?: fusedLocationClient.lastLocation.awaitResult()
                
                if (location != null) {
                    checkProximityToZones(location)
                }
            } catch (e: Exception) {
                Log.e("ActionEngine", "Proximity check failed: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                val activeZones = getActiveZones()
                if (activeZones.isNotEmpty()) {
                    applyRules()
                } else {
                    restoreToDefaultRing()
                }
                updateStatusNotification()
            }
        }
    }

    private fun showEntryNotification(zoneId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val zone = zoneId.toIntOrNull()?.let { zoneDao.getZoneById(it) }
            val havenName = zone?.name ?: "Haven"
            
            val dndRule = prefs.getBoolean("rule_dnd", false)
            val vibrateRule = prefs.getBoolean("rule_vibrate", false)
            
            val modeText = when {
                dndRule -> "silenced"
                vibrateRule -> "set to vibrate"
                else -> "active"
            }

            sendAlertNotification(
                title = "Entered $havenName",
                text = "Your phone is now $modeText."
            )
        }
    }

    private fun showExitNotification() {
        sendAlertNotification(
            title = "Left Haven",
            text = "Settings restored. Your phone is back to normal."
        )
    }

    private fun sendAlertNotification(title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Haven Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for Haven transitions"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            appContext, 
            99,
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(appContext, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(9999, notification)
    }

    private suspend fun checkProximityToZones(currentLocation: Location) {
        val allZones = zoneDao.getAllZones().first()
        val enabledZones = allZones.filter { it.isEnabled }
        
        synchronized(lock) {
            val activeZones = getActiveZones().toMutableSet()
            var changed = false

            enabledZones.forEach { zone ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    currentLocation.latitude, currentLocation.longitude,
                    zone.latitude, zone.longitude,
                    results
                )
                val distance = results[0]

                if (distance <= zone.radius) {
                    if (activeZones.add(zone.id.toString())) {
                        changed = true
                        showEntryNotification(zone.id.toString())
                    }
                } else {
                    if (activeZones.remove(zone.id.toString())) {
                        changed = true
                        if (activeZones.isEmpty()) showExitNotification()
                    }
                }
            }

            if (changed) {
                saveActiveZones(activeZones)
                if (activeZones.isNotEmpty() && !prefs.getBoolean("is_managing_audio", false)) {
                    saveCurrentState()
                }
            }
        }
    }

    private suspend fun validateActiveZones() {
        val cachedActiveIds = getActiveZones()
        if (cachedActiveIds.isEmpty()) return

        val validActiveIds = mutableSetOf<String>()
        
        cachedActiveIds.forEach { id ->
            val zoneIdInt = id.toIntOrNull()
            if (zoneIdInt != null) {
                val zone = zoneDao.getZoneById(zoneIdInt)
                if (zone != null && zone.isEnabled) {
                    validActiveIds.add(id)
                }
            }
        }

        if (validActiveIds.size != cachedActiveIds.size) {
            synchronized(lock) {
                saveActiveZones(validActiveIds)
            }
        }
    }

    private fun applyRules() {
        val dndRule = prefs.getBoolean("rule_dnd", false)
        val vibrateRule = prefs.getBoolean("rule_vibrate", false)
        val mediaMuteRule = prefs.getBoolean("rule_media_mute", false)

        val hasDndAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else true

        if (hasDndAccess) {
            try {
                when {
                    dndRule -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    vibrateRule -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    else -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            } catch (e: SecurityException) {
                Log.e("ActionEngine", "SecurityException setting ringer mode: ${e.message}")
            }
        }

        if (mediaMuteRule) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        }
        
        if (dndRule) setSystemDndFilter(true) else setSystemDndFilter(false)
    }

    private fun isServiceEnabled(): Boolean {
        return prefs.getBoolean("service_enabled", true)
    }

    private fun getActiveZones(): Set<String> {
        return prefs.getStringSet("active_zone_ids", emptySet()) ?: emptySet()
    }

    private fun saveActiveZones(zones: Set<String>) {
        prefs.edit().putStringSet("active_zone_ids", zones).apply()
    }

    private fun saveCurrentState() {
        if (prefs.getBoolean("is_managing_audio", false)) return
        
        val currentRinger = audioManager.ringerMode
        val currentRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val currentMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        
        prefs.edit()
            .putBoolean("is_managing_audio", true)
            .putInt("saved_ringer_mode", currentRinger)
            .putInt("saved_ring_volume", currentRingVolume)
            .putInt("saved_media_volume", currentMediaVolume)
            .apply()
    }

    private fun restoreToDefaultRing() {
        if (!prefs.getBoolean("is_managing_audio", false)) return

        val savedRinger = prefs.getInt("saved_ringer_mode", AudioManager.RINGER_MODE_NORMAL)
        val savedRingVolume = prefs.getInt("saved_ring_volume", audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2)
        val savedMediaVolume = prefs.getInt("saved_media_volume", audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2)

        try {
            setSystemDndFilter(false)
            val hasDndAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else true

            if (hasDndAccess) {
                audioManager.ringerMode = savedRinger
            }
            audioManager.setStreamVolume(AudioManager.STREAM_RING, savedRingVolume, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedMediaVolume, 0)
        } catch (e: Exception) {
            Log.e("ActionEngine", "Error during restoration: ${e.message}")
        }
        
        prefs.edit().putBoolean("is_managing_audio", false).apply()
    }

    private fun setSystemDndFilter(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
            val filter = if (enabled) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL
            notificationManager.setInterruptionFilter(filter)
        }
    }

    private fun updateStatusNotification() {
        if (!isServiceEnabled()) {
            notificationManager.cancel(1001)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Sifer Status", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val activeZones = getActiveZones()
        val contentText = if (activeZones.isEmpty()) "Scanning for Havens..." else "Protected: Active in ${activeZones.size} Haven(s)"

        val intent = Intent(appContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Sifer Protection Active")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(1001, notification)
    }
}
