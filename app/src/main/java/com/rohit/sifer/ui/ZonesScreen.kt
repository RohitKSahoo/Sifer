package com.rohit.sifer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohit.sifer.data.Zone

@Composable
fun ZonesScreen(viewModel: SiferViewModel) {
    val zones by viewModel.allZones.collectAsState(initial = emptyList())
    val isServiceEnabled by viewModel.isServiceEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiferColors.White)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Column {
            SiferBadge(text = "GEOGRAPHIC PROTOCOLS", backgroundColor = SiferColors.LightBlue)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ACTIVE\nHAVENS",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                lineHeight = 42.sp,
                color = SiferColors.Black
            )
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(6.dp)
                    .background(SiferColors.Yellow)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (zones.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NeoBrutalCard(backgroundColor = SiferColors.Grey) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "NO HAVENS DETECTED",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        "Add a new Haven to enable automatic state protection in specific locations.",
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = SiferColors.TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(zones) { zone ->
                    ZoneListItem(
                        zone = zone,
                        isGlobalActive = isServiceEnabled,
                        onToggle = { viewModel.toggleZone(zone) },
                        onDelete = { viewModel.deleteZone(zone) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ZoneListItem(
    zone: Zone,
    isGlobalActive: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = zone.isEnabled && isGlobalActive
    
    NeoBrutalCard(
        backgroundColor = if (isActive) SiferColors.White else SiferColors.Grey,
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.name.uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isActive) SiferColors.Black else SiferColors.MediumGrey
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(14.dp), tint = SiferColors.TextSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${zone.radius.toInt()}M RADIUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SiferColors.TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SiferColors.Red, modifier = Modifier.size(20.dp))
                }
                SiferSwitch(
                    checked = zone.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
        
        if (isActive) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SiferColors.Green.copy(alpha = 0.1f))
                    .border(1.dp, SiferColors.Green)
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SiferColors.Green, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MONITORING ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SiferColors.Green)
                }
            }
        }
    }
}
