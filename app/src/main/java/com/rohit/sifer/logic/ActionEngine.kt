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

class ActionEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private val prefs: SharedPreferences = context.getSharedPreferences("sifer_prefs", Context.MODE_PRIVATE)

    private val CHANNEL_ID = "sifer_protection_channel"

    fun enterZone(zoneId: String) {
        if (!isServiceEnabled()) return
        
        val activeZones = getActiveZones().toMutableSet()
        activeZones.add(zoneId)
        saveActiveZones(activeZones)
        
        Log.d("ActionEngine", "Entered zone: $zoneId. Total active: ${activeZones.size}")
        
        if (activeZones.size == 1) {
            saveCurrentState()
            applyRules()
        }
        updateStatusNotification()
    }

    fun exitZone(zoneId: String) {
        val activeZones = getActiveZones().toMutableSet()
        if (activeZones.remove(zoneId)) {
            saveActiveZones(activeZones)
            Log.d("ActionEngine", "Exited zone: $zoneId. Total active: ${activeZones.size}")
            
            if (activeZones.isEmpty()) {
                restoreToDefaultRing()
            }
        }
        updateStatusNotification()
    }

    fun stopServiceManually() {
        Log.d("ActionEngine", "Service stopped manually: Clearing zones and restoring state")
        saveActiveZones(emptySet())
        restoreToDefaultRing()
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

        // 1. Ringer & Silence Logic (Using RINGER_MODE_SILENT for DND as requested)
        when {
            dndRule -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
            vibrateRule -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
            else -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                audioManager.setStreamVolume(AudioManager.STREAM_RING, max / 2, 0)
            }
        }

        // 2. Media Mute Logic
        if (mediaMuteRule) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } else {
            val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMedia / 2, 0)
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
        prefs.edit().putBoolean("is_managing_audio", true).apply()
    }

    private fun restoreToDefaultRing() {
        try {
            setSystemDndFilter(false)
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            
            val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing / 2, 0)
            
            val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMedia / 2, 0)
            
            Log.d("ActionEngine", "Restored to Protocol Defaults: Ring + 50% Volume")
        } catch (e: Exception) {
            Log.e("ActionEngine", "Error forcing ring restoration")
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
            val channel = NotificationChannel(CHANNEL_ID, "Sifer Status", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val activeZones = getActiveZones()
        val contentText = if (activeZones.isEmpty()) {
            "Scanning for Havens..."
        } else {
            "Protected: Active in ${activeZones.size} Haven(s)"
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Sifer Protection Active")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(1001, notification)
    }
}
