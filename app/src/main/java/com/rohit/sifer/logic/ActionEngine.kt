package com.rohit.sifer.logic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rohit.sifer.MainActivity

class ActionEngine(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private val prefs: SharedPreferences = appContext.getSharedPreferences("sifer_prefs", Context.MODE_PRIVATE)

    private val CHANNEL_ID = "sifer_protection_channel"

    companion object {
        private val lock = Any()
    }

    fun enterZone(zoneId: String) {
        if (!isServiceEnabled()) return
        
        synchronized(lock) {
            val activeZones = getActiveZones().toMutableSet()
            if (activeZones.add(zoneId)) {
                saveActiveZones(activeZones)
                Log.d("ActionEngine", "Entered zone: $zoneId. Total active: ${activeZones.size}")
                
                if (activeZones.size == 1) {
                    saveCurrentState()
                    applyRules()
                }
            }
        }
        updateStatusNotification()
    }

    fun exitZone(zoneId: String) {
        synchronized(lock) {
            val activeZones = getActiveZones().toMutableSet()
            if (activeZones.remove(zoneId)) {
                saveActiveZones(activeZones)
                Log.d("ActionEngine", "Exited zone: $zoneId. Total active: ${activeZones.size}")
                
                if (activeZones.isEmpty()) {
                    restoreToDefaultRing()
                }
            }
        }
        updateStatusNotification()
    }

    fun stopServiceManually() {
        Log.d("ActionEngine", "Service stopped manually: Clearing zones and restoring state")
        synchronized(lock) {
            saveActiveZones(emptySet())
            restoreToDefaultRing()
        }
        notificationManager.cancel(1001)
    }

    fun refreshCurrentState() {
        if (!isServiceEnabled()) {
            notificationManager.cancel(1001)
            return
        }
        val activeZones = getActiveZones()
        if (activeZones.isNotEmpty()) {
            applyRules()
        } else {
            restoreToDefaultRing()
        }
        updateStatusNotification()
    }

    private fun applyRules() {
        val dndRule = prefs.getBoolean("rule_dnd", false)
        val vibrateRule = prefs.getBoolean("rule_vibrate", false)
        val mediaMuteRule = prefs.getBoolean("rule_media_mute", false)

        val hasDndAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else true

        // 1. Ringer & Silence Logic
        if (hasDndAccess) {
            try {
                when {
                    dndRule -> {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    }
                    vibrateRule -> {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    }
                    else -> {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }
                }
            } catch (e: SecurityException) {
                Log.e("ActionEngine", "SecurityException setting ringer mode: ${e.message}")
            }
        }

        // 2. Media Mute Logic
        if (mediaMuteRule) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        }
        
        // If DND rule is on, also trigger system DND for visual consistency
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
            
        Log.d("ActionEngine", "Saved state: Ringer=$currentRinger, RingVol=$currentRingVolume, MediaVol=$currentMediaVolume")
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
            
            Log.d("ActionEngine", "Restored to saved state: Ringer=$savedRinger, RingVol=$savedRingVolume")
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
        val contentText = if (activeZones.isEmpty()) {
            "Scanning for Havens..."
        } else {
            "Protected: Active in ${activeZones.size} Haven(s)"
        }

        val intent = Intent(appContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            appContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

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
