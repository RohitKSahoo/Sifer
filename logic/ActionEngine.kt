package com.rohit.sifer.logic

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log

class ActionEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun enterZone() {
        Log.d("ActionEngine", "Entering zone: Setting SILENT and DND ON")
        setSilentMode(true)
        setDndMode(true)
    }

    fun exitZone() {
        Log.d("ActionEngine", "Exiting zone: Setting NORMAL and DND OFF")
        setSilentMode(false)
        setDndMode(false)
    }

    private fun setSilentMode(silent: Boolean) {
        try {
            if (silent) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
        } catch (e: SecurityException) {
            Log.e("ActionEngine", "Permission denied for ringer mode: ${e.message}")
        }
    }

    private fun setDndMode(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val filter = if (enabled) {
                    NotificationManager.INTERRUPTION_FILTER_NONE
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
                notificationManager.setInterruptionFilter(filter)
            } else {
                Log.e("ActionEngine", "DND access not granted")
            }
        }
    }
}
