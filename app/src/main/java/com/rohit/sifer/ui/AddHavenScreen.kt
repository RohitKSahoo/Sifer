package com.rohit.sifer.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@SuppressLint("MissingPermission")
@Composable
fun AddHavenScreen(viewModel: SiferViewModel, onZoneAdded: () -> Unit) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var confirmedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    
    var zoneName by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }

    // Validation logic
    val radiusValue = radius.toFloatOrNull()
    val isRadiusValid = radiusValue != null && radiusValue >= 50f
    val isNameValid = zoneName.isNotBlank()

    LaunchedEffect(Unit) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    mapViewRef?.controller?.animateTo(GeoPoint(it.latitude, it.longitude))
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                    controller.setCenter(GeoPoint(0.0, 0.0))
                    mapViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Pin",
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = (-24).dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        SmallFloatingActionButton(
            onClick = {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        location?.let {
                            mapViewRef?.controller?.animateTo(GeoPoint(it.latitude, it.longitude))
                        }
                    }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My Location")
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Move the map to set Haven center",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Button(
                    onClick = {
                        mapViewRef?.let {
                            confirmedLocation = it.mapCenter as GeoPoint
                            showDetailsDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Location", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (showDetailsDialog && confirmedLocation != null) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Configure Haven") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = zoneName,
                        onValueChange = { zoneName = it },
                        label = { Text("Zone Name") },
                        placeholder = { Text("e.g. Campus Library") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = !isNameValid
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = radius,
                        onValueChange = { radius = it },
                        label = { Text("Radius (meters)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !isRadiusValid,
                        supportingText = {
                            if (!isRadiusValid) {
                                Text("Minimum radius is 50m for reliability", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Best results with 100m+")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Location: ${String.format("%.5f", confirmedLocation!!.latitude)}, ${String.format("%.5f", confirmedLocation!!.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isNameValid && isRadiusValid) {
                            viewModel.addZone(
                                zoneName,
                                confirmedLocation!!.latitude,
                                confirmedLocation!!.longitude,
                                radiusValue!!
                            )
                            showDetailsDialog = false
                            onZoneAdded()
                        }
                    },
                    enabled = isNameValid && isRadiusValid
                ) {
                    Text("Save Haven")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
