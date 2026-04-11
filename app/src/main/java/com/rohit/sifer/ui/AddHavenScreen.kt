package com.rohit.sifer.ui

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun AddHavenScreen(viewModel: SiferViewModel, isActive: Boolean, onZoneAdded: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val focusManager = LocalFocusManager.current
    
    // Observe all existing havens
    val zones by viewModel.allZones.collectAsState(initial = emptyList())
    
    // Performance: Apply threading config early
    remember {
        Configuration.getInstance().tileDownloadThreads = 12
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 600L * 1024 * 1024
    }

    val mapView = remember { 
        MapView(context).apply {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
            isVerticalMapRepetitionEnabled = false
            isHorizontalMapRepetitionEnabled = true
        }
    }
    
    // Sync zones with map overlays
    LaunchedEffect(zones) {
        mapView.overlays.clear()
        zones.forEach { zone ->
            val center = GeoPoint(zone.latitude, zone.longitude)
            
            // Add Marker (The "Blip")
            val marker = Marker(mapView).apply {
                position = center
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = zone.name
                subDescription = "Radius: ${zone.radius.toInt()}m"
                // Using standard icon for now, could be customized
                icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                icon?.setTint(android.graphics.Color.BLACK)
            }
            
            // Add Circle (The Radius)
            val circle = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(center, zone.radius.toDouble())
                fillPaint.color = android.graphics.Color.argb(50, 0, 255, 127) // Sifer Green with alpha
                outlinePaint.color = android.graphics.Color.BLACK
                outlinePaint.strokeWidth = 2f
            }
            
            mapView.overlays.add(circle)
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }
    
    var zoneName by remember { mutableStateOf("") }
    var radius by remember { mutableFloatStateOf(100f) }
    var searchQuery by remember { mutableStateOf("") }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    // Logic to snap to location
    val snapToLocation: () -> Unit = {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val gp = GeoPoint(it.latitude, it.longitude)
                mapView.controller.setCenter(gp)
                mapView.controller.setZoom(17.5)
                mapView.invalidate()
            }
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val gp = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.setCenter(gp)
                    mapView.controller.setZoom(17.5)
                    mapView.invalidate()
                }
            }
    }

    // Trigger snap whenever the page becomes active
    LaunchedEffect(isActive) {
        if (isActive) {
            snapToLocation()
        }
    }

    val performSearch: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            scope.launch {
                @Suppress("DEPRECATION")
                val addresses = withContext(Dispatchers.IO) {
                    try {
                        geocoder.getFromLocationName(query, 1)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val gp = GeoPoint(address.latitude, address.longitude)
                    withContext(Dispatchers.Main) {
                        mapView.controller.animateTo(gp)
                        mapView.controller.setZoom(17.5)
                        focusManager.clearFocus()
                    }
                }
            }
        }
    }

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
            (mapView.parent as? ViewGroup)?.removeView(mapView)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // FULLSCREEN MAP
        AndroidView(
            factory = { 
                (mapView.parent as? ViewGroup)?.removeView(mapView)
                mapView 
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.invalidate()
            }
        )

        // CENTER TARGET
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(40.dp)
                .background(SiferColors.White.copy(alpha = 0.5f), CircleShape)
                .border(2.dp, SiferColors.Black, CircleShape),
            tint = SiferColors.Black
        )

        // TOP: FLOATING SEARCH
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            NeoBrutalCard(padding = 0.dp, shadowOffset = 4.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SiferColors.MediumGrey, modifier = Modifier.size(20.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search location...", color = SiferColors.MediumGrey, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    } else {
                        IconButton(onClick = { performSearch(searchQuery) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Search", tint = SiferColors.Black, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // RIGHT: MAP TOOLS
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(SiferColors.White, RoundedCornerShape(4.dp))
                    .border(2.dp, SiferColors.Black, RoundedCornerShape(4.dp))
            ) {
                IconButton(onClick = { mapView.controller.zoomIn() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = SiferColors.Black)
                }
                Box(modifier = Modifier.width(24.dp).height(2.dp).background(SiferColors.Black).align(Alignment.CenterHorizontally))
                IconButton(onClick = { mapView.controller.zoomOut() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = "Zoom Out", tint = SiferColors.Black)
                }
            }

            FloatingActionButton(
                onClick = { snapToLocation() },
                containerColor = SiferColors.White,
                contentColor = SiferColors.Black,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(40.dp).border(2.dp, SiferColors.Black, RoundedCornerShape(4.dp)),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(20.dp))
            }
        }

        // BOTTOM: CONSOLIDATED INPUTS
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeoBrutalCard(padding = 12.dp, shadowOffset = 6.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, modifier = Modifier.size(16.dp), tint = SiferColors.MediumGrey)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = zoneName,
                            onValueChange = { zoneName = it },
                            placeholder = { Text("Haven Name (e.g. Home)", color = SiferColors.MediumGrey, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = SiferColors.Black,
                                unfocusedIndicatorColor = SiferColors.Grey
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("RADIUS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SiferColors.TextSecondary)
                        Text("${radius.toInt()}m", fontSize = 14.sp, fontWeight = FontWeight.Black, color = SiferColors.Green)
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
            }

            SiferButton(
                text = "ESTABLISH HAVEN",
                icon = Icons.Default.AddLocation,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = {
                    if (zoneName.isNotBlank()) {
                        val center = mapView.mapCenter as GeoPoint
                        viewModel.addZone(zoneName, center.latitude, center.longitude, radius)
                        onZoneAdded()
                    }
                }
            )
        }
    }
}
