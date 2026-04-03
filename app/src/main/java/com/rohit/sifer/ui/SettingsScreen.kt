package com.rohit.sifer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiferColors.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SiferBadge(text = "SYSTEM_CONFIG_V4.0", backgroundColor = SiferColors.LightBlue)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SETTINGS &\nPERMISSIONS",
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

            // Recalibrate Card
            item {
                NeoBrutalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        SiferBadge(text = "ACTION REQUIRED", backgroundColor = SiferColors.Black, textColor = SiferColors.White)
                    }
                    Text(text = "RECALIBRATE SENSORS", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reset biometric and environmental data arrays for 99.9% accuracy.",
                        fontSize = 12.sp,
                        color = SiferColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = SiferColors.Green, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        SiferButton(text = "START SCAN")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(SiferColors.Grey)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight()
                                .background(SiferColors.Green)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Core Permissions Section
            item {
                PermissionSectionHeader(number = "01", title = "CORE PERMISSIONS")
            }

            item {
                PermissionItem(
                    icon = Icons.Default.LocationOn,
                    title = "PRECISE LOCATION",
                    status = "ACTIVE",
                    description = "Always-on background campus mapping.",
                    checked = true
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                PermissionItem(
                    icon = Icons.Default.DoNotDisturbOn,
                    title = "DND ACCESS",
                    status = "OFF",
                    statusColor = SiferColors.Red,
                    description = "Override silence during critical sifer alerts.",
                    checked = false
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // General Section
            item {
                PermissionSectionHeader(number = "02", title = "GENERAL")
            }

            item {
                GradientPermissionItem(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "BATTERY OPTIMIZATION",
                    description = "Ignore system restrictions for uninterrupted syncing.",
                    gradient = Brush.verticalGradient(listOf(Color(0xFF80CBC4), Color(0xFFDCEDC8), Color(0xFFFFF9C4))),
                    checked = true
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                GradientPermissionItem(
                    icon = Icons.Default.PowerSettingsNew,
                    title = "AUTO-START ON BOOT",
                    description = "Initialize Sifer kernel immediately upon device wake.",
                    gradient = Brush.verticalGradient(listOf(Color(0xFF80CBC4), Color(0xFFDCEDC8), Color(0xFFFFF9C4))),
                    checked = true
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SiferColors.MediumGrey))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "VULNERABILITY PATCH: 0XFF92A • SIFER CORP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SiferColors.BlueBadge,
                            letterSpacing = 1.sp
                        )
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
            Text(text = title, fontWeight = FontWeight.Black, fontSize = 16.sp)
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
    statusColor: Color = SiferColors.Green
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiferColors.Grey)
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
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    SiferBadge(text = status, backgroundColor = statusColor.copy(alpha = 0.2f), textColor = statusColor)
                }
                Text(text = description, fontSize = 11.sp, color = SiferColors.TextSecondary)
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
    checked: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .border(1.dp, SiferColors.Black)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, fontSize = 11.sp, color = Color(0xFF4A5568))
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
