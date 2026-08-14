package com.rohit.sifer.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfoItem(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedAppsScreen(viewModel: SiferViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val excludedApps by viewModel.excludedApps

    var appList by remember { mutableStateOf(emptyList<AppInfoItem>()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val filtered = installed.filter { app ->
                // Filter out system apps that don't have launch intents, to keep list readable
                pm.getLaunchIntentForPackage(app.packageName) != null
            }.map { app ->
                AppInfoItem(
                    label = app.loadLabel(pm).toString(),
                    packageName = app.packageName,
                    icon = app.loadIcon(pm)
                )
            }.sortedBy { it.label.uppercase() }

            withContext(Dispatchers.Main) {
                appList = filtered
                isLoading = false
            }
        }
    }

    val filteredApps = appList.filter {
        it.label.contains(searchQuery, ignoreCase = true) || 
        it.packageName.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SiferColors.White)
    ) {
        GridBackground()

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SiferColors.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Search Bar Card
                    NeoBrutalCard(
                        padding = 0.dp,
                        shadowOffset = 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = SiferColors.Black, modifier = Modifier.size(18.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search apps...", color = SiferColors.MediumGrey, fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = TextStyle(color = SiferColors.Black, fontSize = 14.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = SiferColors.Black,
                                    unfocusedTextColor = SiferColors.Black
                                )
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = SiferColors.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SiferColors.Black)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isExcluded = excludedApps.contains(app.packageName)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleAppExclusion(app.packageName) }
                                        .padding(vertical = 12.dp, horizontal = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .border(1.dp, SiferColors.Black)
                                            .background(SiferColors.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        app.icon?.let { icon ->
                                            Image(
                                                bitmap = icon.toBitmap().asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.label,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = SiferColors.Black
                                        )
                                        Text(
                                            text = app.packageName,
                                            fontSize = 10.sp,
                                            color = SiferColors.TextSecondary
                                        )
                                    }
                                    SiferSwitch(
                                        checked = isExcluded,
                                        onCheckedChange = { viewModel.toggleAppExclusion(app.packageName) }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(SiferColors.Grey)
                                )
                            }
                        }
                    }
                }
            }
        }
}
