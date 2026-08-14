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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
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
    val coroutineScope = rememberCoroutineScope()
    
    val pagerState = rememberPagerState(pageCount = { 3 })

    val selectedNavItem by remember {
        derivedStateOf { pagerState.currentPage }
    }

    // Permission and State
    val hasLocation by viewModel.hasLocationPermission
    val hasBgLocation by viewModel.hasBackgroundLocationPermission
    val hasDnd by viewModel.hasDndPermission
    val updateInfoState = viewModel.updateInfo.value

    var isOnboardingComplete by remember {
        mutableStateOf(hasLocation && hasBgLocation && hasDnd)
    }

    // Dynamic state sync when screen is loaded/resumed via lifecycle observer
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
                isOnboardingComplete = hasLocation && hasBgLocation && hasDnd
                
                // Auto-trigger installation if permission was granted after setting redirect
                viewModel.downloadedApkFile?.let { file ->
                    if (viewModel.canInstallPackages()) {
                        viewModel.checkAndInstallApk(file)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!isOnboardingComplete) {
        OnboardingPermissionsScreen(
            viewModel = viewModel,
            onOnboardingComplete = {
                viewModel.refreshPermissionStates()
                isOnboardingComplete = true
            }
        )
    } else {
        // NEO-BRUTAL UPDATE DIALOG
        updateInfoState?.let { info ->
            if (info.hasUpdate) {
                Dialog(
                    onDismissRequest = { viewModel.updateInfo.value = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        NeoBrutalCard(
                            backgroundColor = SiferColors.White,
                            shadowOffset = 10.dp,
                            padding = 0.dp
                        ) {
                            Column {
                                // Tactical Header Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SiferColors.Black)
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Update,
                                            contentDescription = null,
                                            tint = SiferColors.Yellow,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "PATCH AVAILABLE",
                                            color = SiferColors.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                Column(modifier = Modifier.padding(20.dp)) {
                                    val downloadProgress by viewModel.downloadProgress
                                    val downloadError by viewModel.downloadError

                                    if (downloadProgress != null) {
                                        Text(
                                            "DOWNLOADING PATCH...",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SiferColors.Black
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // NEO-BRUTAL PROGRESS BAR
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(24.dp)
                                                .border(2.dp, SiferColors.Black)
                                                .background(SiferColors.White)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(fraction = downloadProgress ?: 0f)
                                                    .background(SiferColors.Green)
                                            )
                                            Text(
                                                text = "${((downloadProgress ?: 0f) * 100).toInt()}%",
                                                modifier = Modifier.align(Alignment.Center),
                                                color = SiferColors.Black,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Text(
                                            "Please keep Sifer open while download is in progress.",
                                            fontSize = 10.sp,
                                            color = SiferColors.TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else if (downloadError != null) {
                                        Text(
                                            "DOWNLOAD FAILED",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SiferColors.Red
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = downloadError ?: "An unexpected connection error occurred.",
                                            fontSize = 12.sp,
                                            color = SiferColors.Black,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            SiferButton(
                                                text = "CANCEL",
                                                backgroundColor = SiferColors.White,
                                                textColor = SiferColors.Black,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    viewModel.downloadError.value = null
                                                    viewModel.updateInfo.value = null
                                                }
                                            )
                                            SiferButton(
                                                text = "RETRY",
                                                backgroundColor = SiferColors.Green,
                                                textColor = SiferColors.Black,
                                                modifier = Modifier.weight(1.2f),
                                                onClick = {
                                                    viewModel.downloadUpdate()
                                                }
                                            )
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SiferBadge(
                                                text = "v${info.latestVersion}",
                                                backgroundColor = SiferColors.Yellow.copy(alpha = 0.2f),
                                                textColor = SiferColors.Black
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "STABLE RELEASE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = SiferColors.Black
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = info.releaseNotes ?: "Kernel stability and UI performance improvements.",
                                            fontSize = 13.sp,
                                            color = SiferColors.Black,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            SiferButton(
                                                text = "LATER",
                                                backgroundColor = SiferColors.White,
                                                textColor = SiferColors.Black,
                                                modifier = Modifier.weight(1f),
                                                onClick = { viewModel.updateInfo.value = null }
                                            )
                                            SiferButton(
                                                text = "UPDATE",
                                                backgroundColor = SiferColors.Green,
                                                textColor = SiferColors.Black,
                                                modifier = Modifier.weight(1.2f),
                                                onClick = {
                                                    viewModel.downloadUpdate()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                key = { index -> index },
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
                        2 -> {
                            var activeSubScreen by remember { mutableStateOf<String?>(null) }
                            if (activeSubScreen == "excluded_apps") {
                                ExcludedAppsScreen(viewModel) { activeSubScreen = null }
                            } else {
                                SettingsScreen(viewModel) { activeSubScreen = "excluded_apps" }
                            }
                        }
                    }
                }
            }
        }
    }
}
