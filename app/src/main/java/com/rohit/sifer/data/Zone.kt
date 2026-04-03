package com.rohit.sifer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "zones")
data class Zone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val isEnabled: Boolean = true
)
