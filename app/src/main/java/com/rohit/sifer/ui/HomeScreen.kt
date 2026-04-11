package com.rohit.sifer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohit.sifer.data.Zone

@Composable
fun HomeScreen(viewModel: SiferViewModel) {
    val isServiceEnabled by viewModel.isServiceEnabled
    val isDndEnabled by viewModel.isDndEnabled
    val isVibrateEnabled by viewModel.isVibrateEnabled
    val isMediaMuteEnabled by viewModel.isMediaMuteEnabled
    
    val zones by viewModel.allZones.collectAsState(initial = emptyList())
    val history = viewModel.activityHistory

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SiferColors.White)
    ) {
        // Feature 2: Paper Grid Background
        GridBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Master Status Card
            item(key = "status_card") {
                NeoBrutalCard(
                    backgroundColor = if (isServiceEnabled) SiferColors.Green else SiferColors.White
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SiferBadge(
                                text = if (isServiceEnabled) "PROTECTION LIVE" else "PROTECTION OFF",
                                backgroundColor = if (isServiceEnabled) SiferColors.Black else SiferColors.Red,
                                textColor = SiferColors.White,
                                dotColor = if (isServiceEnabled) SiferColors.Green else null
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isServiceEnabled) "Sifer Active" else "Sifer Paused",
                                color = SiferColors.Black,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 34.sp
                            )
                        }
                        Icon(
                            imageVector = if (isServiceEnabled) Icons.Default.Security else Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = SiferColors.Black
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (isServiceEnabled) 
                            "Your digital environment is shielded. Sifer will automatically handle your device state in Havens." 
                            else "Automatic protection is disabled. Your device will not auto-silence in Havens.",
                        fontSize = 12.sp,
                        color = SiferColors.Black,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    SiferButton(
                        text = if (isServiceEnabled) "PAUSE PROTECTION" else "RESUME PROTECTION",
                        backgroundColor = SiferColors.Black,
                        textColor = SiferColors.White,
                        onClick = { viewModel.toggleService(!isServiceEnabled) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Automation Rules Section
            item(key = "automation_header") {
                SiferSectionHeader(title = "Automation Rules")
            }

            // Feature 1: Rules in a single row, smaller size
            item(key = "automation_rules") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AutomationTile(
                        title = "DND",
                        icon = Icons.Default.DoNotDisturbOn,
                        checked = isDndEnabled,
                        onClick = { viewModel.toggleDndRule(!isDndEnabled) },
                        modifier = Modifier.weight(1f)
                    )
                    AutomationTile(
                        title = "Vibrate",
                        icon = Icons.Default.Vibration,
                        checked = isVibrateEnabled,
                        onClick = { viewModel.toggleVibrateRule(!isVibrateEnabled) },
                        modifier = Modifier.weight(1f)
                    )
                    AutomationTile(
                        title = "Media",
                        icon = Icons.Default.MusicOff,
                        checked = isMediaMuteEnabled,
                        onClick = { viewModel.toggleMediaMuteRule(!isMediaMuteEnabled) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Managed Havens Section
            item(key = "havens_header") {
                SiferSectionHeader(title = "Active Havens", rightText = "${zones.size} SAVED")
            }

            if (zones.isEmpty()) {
                item(key = "no_havens") {
                    DashedCard {
                        Text(
                            "NO HAVENS CONFIGURED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = SiferColors.TextSecondary
                        )
                    }
                }
            } else {
                items(zones, key = { it.id }) { zone ->
                    HomeZoneItem(
                        zone = zone,
                        isGlobalActive = isServiceEnabled,
                        onToggle = { viewModel.toggleZone(zone) },
                        onDelete = { viewModel.deleteZone(zone) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Last Activity Section
            item(key = "activity_header") {
                SiferSectionHeader(title = "Recent Activity")
            }

            if (history.isEmpty()) {
                item(key = "no_activity") {
                    NeoBrutalCard(backgroundColor = SiferColors.White.copy(alpha = 0.5f)) {
                        Text(
                            "No recent activity recorded.",
                            fontSize = 12.sp,
                            color = SiferColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                items(history, key = { it.timestamp + it.title }) { log ->
                    NeoBrutalCard(padding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(SiferColors.Yellow)
                                    .border(2.dp, SiferColors.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(24.dp), tint = SiferColors.Black)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(log.title, color = SiferColors.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text("${log.subtitle} • ${log.timestamp}", fontSize = 11.sp, color = SiferColors.TextSecondary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AutomationTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeoBrutalCard(
        padding = 8.dp, // Reduced padding for smaller size
        shadowOffset = 4.dp, 
        modifier = modifier.clickable { onClick() },
        backgroundColor = if (checked) SiferColors.Green else SiferColors.White
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = SiferColors.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, color = SiferColors.Black, fontWeight = FontWeight.Black, fontSize = 11.sp) // Smaller font
        }
    }
}

@Composable
fun HomeZoneItem(
    zone: Zone,
    isGlobalActive: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = zone.isEnabled && isGlobalActive
    
    NeoBrutalCard(
        backgroundColor = if (isActive) SiferColors.White else SiferColors.Grey,
        padding = 12.dp,
        shadowOffset = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.name.uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isActive) SiferColors.Black else SiferColors.MediumGrey
                )
                Text(
                    text = "${zone.radius.toInt()}M RADIUS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = SiferColors.TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SiferColors.Red, modifier = Modifier.size(18.dp))
                }
                SiferSwitch(
                    checked = zone.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}
