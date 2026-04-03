package com.rohit.sifer.ui

import android.annotation.SuppressLint
import android.location.Geocoder
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun AddHavenScreen(viewModel: SiferViewModel, onZoneAdded: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // Use remember for MapView but don't trigger recomposition on its internal state changes
    val mapView = remember { MapView(context) }
    var zoneName by remember { mutableStateOf("") }
    var radius by remember { mutableFloatStateOf(100f) }
    
    var isMapMaximized by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    // Lifecycle management for MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initial location fetch - only happens once
    LaunchedEffect(Unit) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val gp = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.setCenter(gp)
                    mapView.controller.setZoom(17.5)
                }
            }
    }

    BackHandler(enabled = isMapMaximized) {
        isMapMaximized = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SiferColors.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SiferBadge(text = "LOCATION PROTOCOL", backgroundColor = SiferColors.Green)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CREATE NEW\nHAVEN",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 38.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Search Bar
            NeoBrutalCard(padding = 8.dp, shadowOffset = 4.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SiferColors.MediumGrey)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search address or coordinates...", color = SiferColors.MediumGrey, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = {
                        scope.launch {
                            val addresses = withContext(Dispatchers.IO) {
                                try {
                                    geocoder.getFromLocationName(searchQuery, 1)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                val gp = GeoPoint(address.latitude, address.longitude)
                                mapView.controller.animateTo(gp)
                            }
                        }
                    }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Search", tint = SiferColors.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Map Container Card - Optimization: AndroidView is isolated
            NeoBrutalCard(padding = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .graphicsLayer { clip = true }
                        .clickable { isMapMaximized = true }
                ) {
                    AndroidView(
                        factory = { 
                            mapView.apply {
                                setMultiTouchControls(true)
                                // Remove constant state updates during scroll to prevent stutter
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { /* No-op to avoid recomposition hits */ }
                    )
                    
                    // Static Center Target
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .background(SiferColors.White.copy(alpha = 0.5f), CircleShape)
                            .border(2.dp, SiferColors.Black, CircleShape),
                        tint = SiferColors.Black
                    )

                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(24.dp),
                        tint = SiferColors.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Input Fields
            NeoBrutalCard(padding = 16.dp) {
                Text("HAVEN IDENTITY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SiferColors.TextSecondary)
                TextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    placeholder = { Text("e.g. Physics Lab / Home", color = SiferColors.MediumGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = SiferColors.Black,
                        unfocusedIndicatorColor = SiferColors.Grey
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TRIGGER RADIUS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SiferColors.TextSecondary)
                    Text("${radius.toInt()} METERS", fontSize = 14.sp, fontWeight = FontWeight.Black, color = SiferColors.Green)
                }
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..500f,
                    colors = SliderDefaults.colors(
                        thumbColor = SiferColors.Black,
                        activeTrackColor = SiferColors.Green,
                        inactiveTrackColor = SiferColors.Grey
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            SiferButton(
                text = "INITIALIZE HAVEN",
                icon = Icons.Default.AddLocationAlt,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                onClick = {
                    if (zoneName.isNotBlank()) {
                        // Retrieve the center point only when the button is clicked
                        val center = mapView.mapCenter as GeoPoint
                        viewModel.addZone(zoneName, center.latitude, center.longitude, radius)
                        onZoneAdded()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }

        // Maximized Map Overlay
        if (isMapMaximized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SiferColors.White)
                    .zIndex(10f)
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay Controls
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding()) {
                    NeoBrutalCard(padding = 8.dp, shadowOffset = 4.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isMapMaximized = false }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search location...", fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            IconButton(onClick = {
                                scope.launch {
                                    val addresses = withContext(Dispatchers.IO) {
                                        try {
                                            geocoder.getFromLocationName(searchQuery, 1)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (!addresses.isNullOrEmpty()) {
                                        val address = addresses[0]
                                        val gp = GeoPoint(address.latitude, address.longitude)
                                        mapView.controller.animateTo(gp)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .background(SiferColors.White.copy(alpha = 0.5f), CircleShape)
                        .border(3.dp, SiferColors.Black, CircleShape),
                    tint = SiferColors.Black
                )

                FloatingActionButton(
                    onClick = { isMapMaximized = false },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                    containerColor = SiferColors.Black,
                    contentColor = SiferColors.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Done")
                }
                
                SmallFloatingActionButton(
                    onClick = {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            location?.let {
                                val gp = GeoPoint(it.latitude, it.longitude)
                                mapView.controller.animateTo(gp)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
                    containerColor = SiferColors.White,
                    contentColor = SiferColors.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }
            }
        }
    }
}
