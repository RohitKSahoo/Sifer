package com.rohit.sifer.logic

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.rohit.sifer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.os.Build
import android.provider.Settings
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val downloadUrl: String?,
    val releaseNotes: String?
)

data class RemoteConfig(
    val welcomeMessage: String = "Let's take a quick look at how to protect your focus.",
    val minRadius: Float = 50f,
    val maxRadius: Float = 500f,
    val maintenanceMode: Boolean = false
)

class UpdateManager(private val context: Context) {

    private val GITHUB_API_URL = "https://api.github.com/repos/RohitKSahoo/Sifer/releases/latest"
    private val REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/RohitKSahoo/Sifer/main/remote_config.json"

    suspend fun fetchRemoteConfig(): RemoteConfig = withContext(Dispatchers.IO) {
        try {
            val url = URL(REMOTE_CONFIG_URL)
            val connection = url.openConnection() as HttpURLConnection
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            RemoteConfig(
                welcomeMessage = json.optString("welcome_message", "Let's take a quick look at how to protect your focus."),
                minRadius = json.optDouble("min_radius", 50.0).toFloat(),
                maxRadius = json.optDouble("max_radius", 500.0).toFloat(),
                maintenanceMode = json.optBoolean("maintenance_mode", false)
            )
        } catch (e: Exception) {
            RemoteConfig() // Return defaults on error
        }
    }

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            
            val latestTag = json.getString("tag_name").replace("v", "")
            val currentVersion = BuildConfig.VERSION_NAME
            
            val hasUpdate = isNewerVersion(currentVersion, latestTag)
            
            var downloadUrl: String? = null
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersion = latestTag,
                downloadUrl = downloadUrl,
                releaseNotes = json.optString("body", "Bug fixes and improvements.")
            )
        } catch (e: Exception) {
            UpdateInfo(false, "", null, null)
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val currentParts = current.split(".").map { it.toInt() }
            val latestParts = latest.split(".").map { it.toInt() }
            
            for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
                val c = currentParts.getOrNull(i) ?: 0
                val l = latestParts.getOrNull(i) ?: 0
                if (l > c) return true
                if (c > l) return false
            }
            false
        } catch (e: Exception) {
            latest != current
        }
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    suspend fun downloadApkWithProgress(
        urlString: String,
        fileName: String = "Sifer_Update.apk",
        onProgress: (Float) -> Unit,
        onComplete: (File) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            var currentUrl = urlString
            var connection: HttpURLConnection
            var status: Int
            var redirectCount = 0
            val maxRedirects = 5

            var url = URL(currentUrl)
            while (true) {
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.requestMethod = "GET"
                connection.connect()

                status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                    status == HttpURLConnection.HTTP_MOVED_PERM || 
                    status == 307 || status == 308) {
                    if (redirectCount >= maxRedirects) {
                        onError("Too many redirects")
                        return@withContext
                    }
                    val newUrl = connection.getHeaderField("Location")
                    url = URL(newUrl)
                    redirectCount++
                    continue
                }
                break
            }

            if (status != HttpURLConnection.HTTP_OK) {
                onError("Server returned code $status")
                return@withContext
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            
            // Delete old file if exists
            if (file.exists()) {
                file.delete()
            }

            val output = file.outputStream()
            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress(total.toFloat() / fileLength.toFloat())
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            withContext(Dispatchers.Main) {
                onComplete(file)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Unknown download error")
            }
        }
    }

    fun installApk(file: File) {
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
