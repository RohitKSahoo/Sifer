package com.rohit.sifer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rohit.sifer.data.Zone

@Composable
fun HomeScreen(viewModel: SiferViewModel) {
    val zones by viewModel.allZones.collectAsState(initial = emptyList())
    val isServiceEnabled by viewModel.isServiceEnabled
    
    val activeZones = if (isServiceEnabled) zones.filter { it.isEnabled } else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sifer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Universal Start/Stop Button
            Button(
                onClick = { viewModel.toggleService(!isServiceEnabled) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isServiceEnabled) "STOP SIFER" else "START SIFER")
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceEnabled) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isServiceEnabled) "Service Active" else "Service Paused",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isServiceEnabled) 
                            "${activeZones.size} zones being monitored" 
                        else 
                            "Automations are currently disabled",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    imageVector = if (isServiceEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }

        Text(
            text = "Your Havens",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (zones.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No Havens added. Go to 'Add' to start.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn {
                items(zones) { zone ->
                    ZoneStatusItem(
                        zone = zone, 
                        isEnabled = isServiceEnabled,
                        onToggle = { viewModel.toggleZone(zone) }
                    )
                }
            }
        }
    }
}

@Composable
fun ZoneStatusItem(zone: Zone, isEnabled: Boolean, onToggle: () -> Unit) {
    ListItem(
        headlineContent = { 
            Text(
                text = zone.name,
                color = if (isEnabled) Color.Unspecified else MaterialTheme.colorScheme.outline
            ) 
        },
        supportingContent = { 
            Text(
                text = "${zone.radius}m radius",
                color = if (isEnabled) Color.Unspecified else MaterialTheme.colorScheme.outline
            ) 
        },
        trailingContent = {
            Switch(
                checked = zone.isEnabled,
                onCheckedChange = { onToggle() },
                enabled = isEnabled
            )
        }
    )
}
