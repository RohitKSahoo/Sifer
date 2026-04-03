package com.rohit.sifer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rohit.sifer.data.Zone

@Composable
fun ZonesScreen(viewModel: SiferViewModel) {
    val zones by viewModel.allZones.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Manage Zones",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (zones.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(text = "No zones added yet.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn {
                items(zones) { zone ->
                    ZoneItem(
                        zone = zone,
                        onToggle = { viewModel.toggleZone(zone) },
                        onDelete = { viewModel.deleteZone(zone) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ZoneItem(zone: Zone, onToggle: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(zone.name) },
        supportingContent = { 
            Text("Lat: ${zone.latitude}, Lon: ${zone.longitude}\nRadius: ${zone.radius}m") 
        },
        trailingContent = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(
                    checked = zone.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    )
}
