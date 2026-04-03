package com.rohit.sifer.logic

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.util.Log

class ActionEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    } else {
        null
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences("sifer_prefs", Context.MODE_PRIVATE)

    fun enterZone(zoneId: String) {
        if (!isServiceEnabled()) return
        
        val activeZones = getActiveZones().toMutableSet()
        activeZones.add(zoneId)
        saveActiveZones(activeZones)
        
        Log.d("ActionEngine", "Entered zone: $zoneId. Total active: ${activeZones.size}")
        
        if (activeZones.size == 1) {
            saveCurrentState()
            setSilentMode(true)
            setDndMode(true)
        }
    }

    fun exitZone(zoneId: String) {
        val activeZones = getActiveZones().toMutableSet()
        if (activeZones.remove(zoneId)) {
            saveActiveZones(activeZones)
            Log.d("ActionEngine", "Exited zone: $zoneId. Total active: ${activeZones.size}")
            
            if (activeZones.isEmpty()) {
                restorePreviousState()
            }
        }
    }

    fun stopServiceManually() {
        Log.d("ActionEngine", "Service stopped manually: Clearing zones and restoring state")
        saveActiveZones(emptySet())
        restorePreviousState()
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

        val currentRingerMode = audioManager.ringerMode
        val currentDndFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
            notificationManager.currentInterruptionFilter
        } else {
            -1
        }
        
        prefs.edit()
            .putInt("prev_ringer_mode", currentRingerMode)
            .putInt("prev_dnd_filter", currentDndFilter)
            .putBoolean("is_managing_audio", true)
            .commit()
    }

    private fun restorePreviousState() {
        if (!prefs.getBoolean("is_managing_audio", false)) return

        val prevRingerMode = prefs.getInt("prev_ringer_mode", AudioManager.RINGER_MODE_NORMAL)
        val prevDndFilter = prefs.getInt("prev_dnd_filter", -1)

        try {
            audioManager.ringerMode = prevRingerMode
        } catch (e: Exception) {
            Log.e("ActionEngine", "Error restoring ringer mode: ${e.message}")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null && prevDndFilter != -1) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(prevDndFilter)
            }
        }
        
        prefs.edit().putBoolean("is_managing_audio", false).commit()
    }

    private fun setSilentMode(silent: Boolean) {
        try {
            audioManager.ringerMode = if (silent) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
        } catch (e: Exception) {
            Log.e("ActionEngine", "Error setting ringer mode")
        }
    }

    private fun setDndMode(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager?.isNotificationPolicyAccessGranted == true) {
            val filter = if (enabled) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
            notificationManager.setInterruptionFilter(filter)
        }
    }
}
