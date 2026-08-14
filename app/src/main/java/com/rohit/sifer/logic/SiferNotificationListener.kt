package com.rohit.sifer.logic

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SiferNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val prefs = getSharedPreferences("sifer_prefs", Context.MODE_PRIVATE)
        
        val isServiceEnabled = prefs.getBoolean("service_enabled", true)
        val activeZones = prefs.getStringSet("active_zone_ids", emptySet()) ?: emptySet()
        
        if (!isServiceEnabled || activeZones.isEmpty()) {
            return
        }

        val excludedApps = prefs.getStringSet("excluded_apps", emptySet()) ?: emptySet()
        if (excludedApps.contains(packageName)) {
            Log.d("SiferNotifListener", "Notification from excluded app: $packageName. Bypassing silence.")
            triggerBypassAlert()
        }
    }

    private fun triggerBypassAlert() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val aa = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    ringtone.audioAttributes = aa
                }
                ringtone.play()
            }

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        } catch (e: Exception) {
            Log.e("SiferNotifListener", "Failed to trigger bypass alert: ${e.message}")
        }
    }
}
