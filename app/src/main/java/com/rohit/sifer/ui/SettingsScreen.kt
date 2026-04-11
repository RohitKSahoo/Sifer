package com.rohit.sifer.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun SettingsScreen(viewModel: SiferViewModel) {
    val hasLocation by viewModel.hasLocationPermission
    val hasDnd by viewModel.hasDndPermission
    val isBatteryOptimized by viewModel.isBatteryOptimized
    val isAutoStartEnabled by viewModel.isAutoStartEnabled
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(SiferColors.White)) {
        GridBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SiferBadge(text = "SYSTEM_CONFIG_V4.0", backgroundColor = SiferColors.LightBlue)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SETTINGS", // Feature 5: Renamed
                    color = SiferColors.Black,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Optimizing sensor fidelity for your campus experience.",
                    fontSize = 14.sp,
                    color = SiferColors.TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                PermissionSectionHeader(number = "01", title = "CORE PERMISSIONS")
            }

            item {
                PermissionItem(
                    icon = Icons.Default.LocationOn,
                    title = "PRECISE LOCATION",
                    status = if (hasLocation) "ACTIVE" else "REQUIRED",
                    statusColor = if (hasLocation) SiferColors.Green else SiferColors.Red,
                    description = "Always-on background campus mapping.",
                    checked = hasLocation,
                    onClick = { if (!hasLocation) viewModel.openLocationSettings() }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                PermissionItem(
                    icon = Icons.Default.DoNotDisturbOn,
                    title = "DND ACCESS",
                    status = if (hasDnd) "ACTIVE" else "OFF",
                    statusColor = if (hasDnd) SiferColors.Green else SiferColors.Red,
                    description = "Override silence during critical sifer alerts.",
                    checked = hasDnd,
                    onClick = { viewModel.openDndSettings() }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                PermissionSectionHeader(number = "02", title = "GENERAL")
            }

            item {
                GradientPermissionItem(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "BATTERY OPTIMIZATION",
                    description = if (isBatteryOptimized) 
                        "System is restricting background sync. Performance may be degraded." 
                        else "Unrestricted background performance active.",
                    gradient = if (isBatteryOptimized) 
                        Brush.verticalGradient(listOf(Color(0xFFFFEBEE), Color(0xFFFFCDD2)))
                        else Brush.verticalGradient(listOf(Color(0xFF80CBC4), Color(0xFFDCEDC8))),
                    checked = !isBatteryOptimized,
                    onClick = { if (isBatteryOptimized) viewModel.openBatterySettings() }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                GradientPermissionItem(
                    icon = Icons.Default.PowerSettingsNew,
                    title = "AUTO-START ON BOOT",
                    description = "Initialize Sifer kernel immediately upon device wake.",
                    gradient = Brush.verticalGradient(listOf(Color(0xFF80CBC4), Color(0xFFDCEDC8))),
                    checked = isAutoStartEnabled,
                    onClick = { viewModel.toggleAutoStart(!isAutoStartEnabled) }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // Feature 5: GitHub Link Footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RohitKSahoo/Sifer"))
                            context.startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SiferColors.MediumGrey))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = SiferColors.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RohitKSahoo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = SiferColors.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionSectionHeader(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
        Box(
            modifier = Modifier
                .background(SiferColors.Black)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = number, color = SiferColors.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, color = SiferColors.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(SiferColors.Black))
        }
    }
}

@Composable
fun PermissionItem(
    icon: ImageVector,
    title: String,
    status: String,
    description: String,
    checked: Boolean,
    statusColor: Color = SiferColors.Green,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiferColors.Grey)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, SiferColors.Black)
                    .background(SiferColors.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = SiferColors.Black, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, color = SiferColors.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    SiferBadge(text = status, backgroundColor = statusColor.copy(alpha = 0.2f), textColor = statusColor)
                }
                Text(text = description, color = SiferColors.TextSecondary, fontSize = 11.sp)
            }
            SiferCheckbox(checked = checked)
        }
    }
}

@Composable
fun GradientPermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    gradient: Brush,
    checked: Boolean,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .border(1.dp, SiferColors.Black)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = SiferColors.Black, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = SiferColors.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, color = Color(0xFF2D3748), fontSize = 11.sp)
            }
            SiferCheckbox(checked = checked)
        }
    }
}

@Composable
fun SiferCheckbox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp, 20.dp)
            .background(if (checked) Color(0xFF1A365D) else SiferColors.White)
            .border(1.dp, SiferColors.Black)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .background(if (checked) Color(0xFF4299E1) else SiferColors.White)
                .border(1.dp, SiferColors.Black)
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SiferColors.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
