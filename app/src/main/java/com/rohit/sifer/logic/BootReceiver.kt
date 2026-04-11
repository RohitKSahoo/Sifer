package com.rohit.sifer.logic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rohit.sifer.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("sifer_prefs", Context.MODE_PRIVATE)
            val isServiceEnabled = prefs.getBoolean("service_enabled", true)
            val isAutoStartEnabled = prefs.getBoolean("auto_start_on_boot", true)

            if (isServiceEnabled && isAutoStartEnabled) {
                val geofenceManager = GeofenceManager(context)
                val zoneDao = AppDatabase.getDatabase(context).zoneDao()
                
                // Re-register geofences in a background scope
                CoroutineScope(Dispatchers.IO).launch {
                    val zones = zoneDao.getAllZones().first()
                    geofenceManager.addGeofences(zones)
                }
            }
        }
    }
}
