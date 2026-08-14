package com.rohit.sifer.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.graphics.Color
import com.rohit.sifer.ui.theme.*

@Composable
fun OnboardingPermissionsScreen(
    viewModel: SiferViewModel,
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    
    // Real Permission States from ViewModel
    val hasLocation by viewModel.hasLocationPermission
    val hasBgLocation by viewModel.hasBackgroundLocationPermission
    val hasDnd by viewModel.hasDndPermission
    val hasNotificationAccess by viewModel.hasNotificationAccess

    // Launchers for Requesting Permissions
    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshPermissionStates()
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(context, "Location permissions are required for Sifer to monitor Havens.", Toast.LENGTH_SHORT).show()
        }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissionStates()
        if (!granted) {
            Toast.makeText(context, "Background location is required to automate Havens in the background.", Toast.LENGTH_LONG).show()
        }
    }

    // Refresh state when resuming screen via lifecycle observer
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SiferColors.White)
    ) {
        GridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Brand Header Area
            Column {
                Text(
                    text = "Permissions Required",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    lineHeight = 42.sp,
                    color = SiferColors.Black
                )
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .width(80.dp)
                        .height(3.dp)
                        .background(SiferColors.Green)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Before we silent your device state in Havens, we need the following permissions configured.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SiferColors.TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // LazyColumn containing Checklist Items
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Precise Location
                item {
                    OnboardingPermissionItem(
                        icon = Icons.Default.LocationOn,
                        title = "PRECISE LOCATION",
                        description = "Required to detect your boundaries and configure Havens on the map.",
                        isGranted = hasLocation,
                        statusText = if (hasLocation) "ACTIVE" else "REQUIRED",
                        statusColor = if (hasLocation) SiferColors.Green else SiferColors.Red,
                        onClick = {
                            if (!hasLocation) {
                                foregroundLocationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    )
                }

                // Background Location
                item {
                    OnboardingPermissionItem(
                        icon = Icons.Default.Explore,
                        title = "BACKGROUND LOCATION",
                        description = "Allows Sifer to monitor Havens and auto-silence when the screen is off.",
                        isGranted = hasBgLocation,
                        statusText = if (hasBgLocation) "ACTIVE" else "REQUIRED",
                        statusColor = if (hasBgLocation) SiferColors.Green else SiferColors.Red,
                        onClick = {
                            if (!hasLocation) {
                                Toast.makeText(context, "Please grant Precise Location permission first.", Toast.LENGTH_SHORT).show()
                            } else if (!hasBgLocation) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                            }
                        }
                    )
                }

                // DND Access
                item {
                    OnboardingPermissionItem(
                        icon = Icons.Default.DoNotDisturbOn,
                        title = "DO NOT DISTURB ACCESS",
                        description = "Enables automatic DND silencing features when you enter active Havens.",
                        isGranted = hasDnd,
                        statusText = if (hasDnd) "ACTIVE" else "REQUIRED",
                        statusColor = if (hasDnd) SiferColors.Green else SiferColors.Red,
                        onClick = {
                            if (!hasDnd) {
                                viewModel.openDndSettings()
                            }
                        }
                    )
                }

                // Notification Access
                item {
                    OnboardingPermissionItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "NOTIFICATION ACCESS",
                        description = "Optional. Enables bypassing DND filters for your configured Excluded Apps.",
                        isGranted = hasNotificationAccess,
                        statusText = if (hasNotificationAccess) "ACTIVE" else "RECOMMENDED",
                        statusColor = if (hasNotificationAccess) SiferColors.Green else Color(0xFF0066CC),
                        onClick = {
                            if (!hasNotificationAccess) {
                                viewModel.openNotificationAccessSettings()
                            }
                        }
                    )
                }
            }

            // Enter Button Area
            val allCoreGranted = hasLocation && hasBgLocation && hasDnd
            
            SiferButton(
                text = "CONTINUE TO SIFER",
                backgroundColor = if (allCoreGranted) SiferColors.Black else SiferColors.Grey,
                textColor = if (allCoreGranted) SiferColors.White else SiferColors.MediumGrey,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                onClick = {
                    if (allCoreGranted) {
                        onOnboardingComplete()
                    } else {
                        Toast.makeText(context, "Please grant all core required permissions.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun OnboardingPermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    statusText: String,
    statusColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiferColors.Grey)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SiferColors.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = SiferColors.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SiferBadge(
                        text = statusText,
                        backgroundColor = statusColor.copy(alpha = 0.2f),
                        textColor = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = SiferColors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isGranted) SiferColors.Green else SiferColors.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
