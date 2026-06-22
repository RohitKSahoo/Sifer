package com.rohit.sifer.ui

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.TextStyle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

data class PhotonSuggestion(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

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
            
            val marker = Marker(mapView).apply {
                position = center
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = zone.name
                subDescription = "Radius: ${zone.radius.toInt()}m"
                icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                icon?.setTint(android.graphics.Color.BLACK)
            }
            
            val circle = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(center, zone.radius.toDouble())
                fillPaint.color = android.graphics.Color.argb(50, 0, 255, 127)
                outlinePaint.color = android.graphics.Color.BLACK
                outlinePaint.strokeWidth = 2f
            }
            
            mapView.overlays.add(circle)
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }
    
    var zoneName by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(100f) }
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<PhotonSuggestion>()) }
    var isSearching by remember { mutableStateOf(false) }

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

    // Photon API Search Implementation with Location Biasing
    LaunchedEffect(searchQuery) {
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.length < 2) {
            suggestions = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        delay(250) // Faster response
        isSearching = true
        
        // Use current map center to bias results (important for "nearby" relevance)
        val center = mapView.mapCenter as GeoPoint
        
        withContext(Dispatchers.IO) {
            try {
                // lat/lon parameters bias the search to current location/view
                val urlString = "https://photon.komoot.io/api/?q=${trimmedQuery.replace(" ", "+")}&limit=5&lat=${center.latitude}&lon=${center.longitude}"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val features = json.getJSONArray("features")
                
                val results = mutableListOf<PhotonSuggestion>()
                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val props = feature.getJSONObject("properties")
                    val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
                    
                    val name = props.optString("name", "")
                    val street = props.optString("street", "")
                    val houseNumber = props.optString("housenumber", "")
                    
                    val displayName = when {
                        name.isNotBlank() -> name
                        street.isNotBlank() -> "$houseNumber $street".trim()
                        else -> "Unknown Location"
                    }
                    
                    val city = props.optString("city", "")
                    val country = props.optString("country", "")
                    val description = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
                    
                    results.add(PhotonSuggestion(
                        name = displayName,
                        description = description,
                        latitude = coords.getDouble(1),
                        longitude = coords.getDouble(0)
                    ))
                }
                
                withContext(Dispatchers.Main) {
                    suggestions = results
                    isSearching = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSearching = false
                }
            }
        }
    }

    val performSearch: (String) -> Unit = { query ->
        if (query.isNotBlank() && suggestions.isNotEmpty()) {
            val suggestion = suggestions[0]
            val gp = GeoPoint(suggestion.latitude, suggestion.longitude)
            mapView.controller.animateTo(gp)
            mapView.controller.setZoom(17.5)
            searchQuery = suggestion.name
            if (zoneName.isBlank()) zoneName = suggestion.name
            suggestions = emptyList()
            focusManager.clearFocus()
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            NeoBrutalCard(padding = 0.dp, shadowOffset = 4.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SiferColors.Black, modifier = Modifier.size(20.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search location...", color = SiferColors.MediumGrey, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(color = SiferColors.Black),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = SiferColors.Black,
                            unfocusedTextColor = SiferColors.Black
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SiferColors.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                suggestions = emptyList()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = SiferColors.Black, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Suggestions List - Appears instantly as user types
            AnimatedVisibility(
                visible = suggestions.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                NeoBrutalCard(padding = 0.dp, shadowOffset = 4.dp) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(suggestions) { suggestion ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Update map to selected suggestion instantly
                                        val gp = GeoPoint(suggestion.latitude, suggestion.longitude)
                                        mapView.controller.animateTo(gp)
                                        mapView.controller.setZoom(17.5)
                                        
                                        // Update UI fields
                                        searchQuery = suggestion.name
                                        if (zoneName.isBlank()) zoneName = suggestion.name
                                        
                                        // Hide list and keyboard
                                        suggestions = emptyList()
                                        focusManager.clearFocus()
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = suggestion.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = SiferColors.Black
                                )
                                if (suggestion.description.isNotBlank()) {
                                    Text(
                                        text = suggestion.description,
                                        fontSize = 11.sp,
                                        color = SiferColors.TextSecondary
                                    )
                                }
                            }
                            HorizontalDivider(color = SiferColors.Black, thickness = 1.dp)
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
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = SiferColors.Black, modifier = Modifier.size(20.dp))
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
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, modifier = Modifier.size(16.dp), tint = SiferColors.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = zoneName,
                            onValueChange = { zoneName = it },
                            placeholder = { Text("Haven Name (e.g. Home)", color = SiferColors.MediumGrey, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(color = SiferColors.Black, fontSize = 14.sp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = SiferColors.Black,
                                unfocusedIndicatorColor = SiferColors.Grey,
                                focusedTextColor = SiferColors.Black,
                                unfocusedTextColor = SiferColors.Black
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("RADIUS", color = SiferColors.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
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
