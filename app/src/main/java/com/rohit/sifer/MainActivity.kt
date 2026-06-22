package com.rohit.sifer

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohit.sifer.ui.*
import com.rohit.sifer.ui.theme.SiferTheme
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // OSMdroid Configuration
        val osmConfig = Configuration.getInstance()
        osmConfig.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        val osmDir = File(filesDir, "osmdroid")
        if (!osmDir.exists()) osmDir.mkdirs()
        
        val tileCache = File(osmDir, "tiles")
        if (!tileCache.exists()) tileCache.mkdirs()
        
        osmConfig.osmdroidBasePath = osmDir
        osmConfig.osmdroidTileCache = tileCache
        osmConfig.userAgentValue = packageName
        osmConfig.tileDownloadThreads = 12
        osmConfig.tileFileSystemCacheMaxBytes = 600L * 1024 * 1024
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            SiferTheme {
                MainContainer()
            }
        }
    }
}

@Composable
fun MainContainer() {
    val context = LocalContext.current
    val viewModel: SiferViewModel = viewModel()
    var showDndDialog by remember { mutableStateOf(false) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }
    
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val selectedNavItem by remember {
        derivedStateOf { pagerState.currentPage }
    }

    // Permission launchers
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.refreshPermissionStates()
        } else {
            Toast.makeText(context, "Background location is required for automatic protection", Toast.LENGTH_LONG).show()
        }
    }

    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                showBackgroundLocationDialog = true
            }
            viewModel.refreshPermissionStates()
        } else {
            Toast.makeText(context, "Location permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasForeground = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (!hasForeground) {
            foregroundLocationLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasBackground) {
                showBackgroundLocationDialog = true
            }
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            showDndDialog = true
        }
    }

    if (showBackgroundLocationDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationDialog = false },
            title = { Text("Background Location Required") },
            text = { 
                Text("Sifer needs 'Allow all the time' location access to detect when you enter a Haven even when the app is closed. Please select 'Allow all the time' in the next screen.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackgroundLocationDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundLocationDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    if (showDndDialog) {
        AlertDialog(
            onDismissRequest = { showDndDialog = false },
            title = { Text("DND Access Required") },
            text = { 
                Text("Sifer needs Do Not Disturb access to automatically silence your phone.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDndDialog = false
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text("Continue")
                }
            }
        )
    }

    Scaffold(
        topBar = { SiferTopBar() },
        bottomBar = {
            SiferBottomNav(
                selectedItem = selectedNavItem,
                onItemSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            beyondViewportPageCount = 2,
            key = { it },
            userScrollEnabled = false
        ) { page ->
            Box {
                when (page) {
                    0 -> HomeScreen(viewModel)
                    1 -> AddHavenScreen(
                        viewModel = viewModel,
                        isActive = selectedNavItem == 1
                    ) { 
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    }
                    2 -> SettingsScreen(viewModel)
                }
            }
        }
    }
}
