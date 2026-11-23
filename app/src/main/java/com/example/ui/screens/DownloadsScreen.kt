package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.local.DownloadEntity
import com.example.data.local.DownloadStatus
import com.example.downloads.NovaDownloadManager
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure

@Composable
fun DownloadsScreen(
    downloads: List<DownloadEntity>,
    isIncognito: Boolean,
    onStartDownloadUrl: (String) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onOpen: (DownloadEntity) -> Unit,
    onShare: (DownloadEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showNewDownloadDialog by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf("ALL") }
    var canInstallUnknownApps by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
            } else true
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canInstallUnknownApps = runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filteredDownloads = remember(downloads, activeFilter) {
        when (activeFilter) {
            "APK" -> downloads.filter { it.fileName.endsWith(".apk", ignoreCase = true) }
            "MEDIA" -> downloads.filter {
                it.mimeType.startsWith("image/") ||
                    it.mimeType.startsWith("video/") ||
                    it.mimeType.startsWith("audio/")
            }
            "DOCS" -> downloads.filter {
                it.mimeType.contains("pdf") ||
                    it.mimeType.contains("zip") ||
                    it.mimeType.contains("document") ||
                    it.mimeType.startsWith("text/")
            }
            else -> downloads
        }
    }

    GlassOverlayScaffold(
        title = "Downloads",
        isIncognito = isIncognito,
        onBack = onBack,
        trailingActions = {
            GlassIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Download file from URL",
                onClick = { showNewDownloadDialog = true },
                testTagName = "new_download_url_button"
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Live Android Unknown Sources (APK Installer) Status Bar
            LiquidGlassSurface(
                shape = RoundedCornerShape(18.dp),
                accentBorder = !canInstallUnknownApps,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.InstallMobile,
                            contentDescription = null,
                            tint = if (canInstallUnknownApps) MintSecure else AmberWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (canInstallUnknownApps) "Unknown Sources Allowed" else "Enable Install Unknown Apps",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (canInstallUnknownApps) "Ready to install downloaded APKs directly" else "Required by Android to install APK files",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            runCatching {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                } else {
                                    Intent(Settings.ACTION_SECURITY_SETTINGS)
                                }
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.testTag("downloads_unknown_sources_button")
                    ) {
                        Text(if (canInstallUnknownApps) "Manage" else "Allow")
                    }
                }
            }

            // Category Filter Chips (All, APKs, Media, Docs & Archives)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item { GlassPillChip("All (${downloads.size})", active = activeFilter == "ALL", onClick = { activeFilter = "ALL" }) }
                item { GlassPillChip("APKs", active = activeFilter == "APK", onClick = { activeFilter = "APK" }) }
                item { GlassPillChip("Media", active = activeFilter == "MEDIA", onClick = { activeFilter = "MEDIA" }) }
                item { GlassPillChip("Docs & ZIP", active = activeFilter == "DOCS", onClick = { activeFilter = "DOCS" }) }
            }

            if (filteredDownloads.isEmpty()) {
                EmptyGlassState(
                    icon = Icons.Rounded.Download,
                    title = if (downloads.isEmpty()) "No Downloads Yet" else "No Matching Files",
                    subtitle = "Supports APKs, videos, music, PDFs, ZIPs, images, and all file formats with Pause/Resume and direct Install/Open."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredDownloads, key = { it.id }) { item ->
                        DownloadItemCard(
                            item = item,
                            onPause = { onPause(item.id) },
                            onResume = { onResume(item.id) },
                            onRetry = { onRetry(item.id) },
                            onOpen = { onOpen(item) },
                            onShare = { onShare(item) },
                            onDelete = { onDelete(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (showNewDownloadDialog) {
        var urlInput by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { showNewDownloadDialog = false },
            title = { Text("Download Any File by URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste any direct HTTP/HTTPS link (APK, MP4, MP3, PDF, ZIP, image, etc.).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("File URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = urlInput.trim()
                        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                            onStartDownloadUrl(trimmed)
                            showNewDownloadDialog = false
                        }
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDownloadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val status = item.downloadStatus
    val isApk = item.fileName.endsWith(".apk", ignoreCase = true)

    val fileIcon = when {
        isApk -> Icons.Rounded.Android
        item.mimeType.startsWith("video/") -> Icons.Rounded.VideoFile
        item.mimeType.startsWith("audio/") -> Icons.Rounded.AudioFile
        item.mimeType.startsWith("image/") -> Icons.Rounded.Image
        item.mimeType.contains("zip") || item.mimeType.contains("rar") || item.mimeType.contains("7z") -> Icons.Rounded.FolderZip
        item.mimeType.contains("pdf") || item.mimeType.startsWith("text/") -> Icons.Rounded.Description
        else -> Icons.Rounded.InsertDriveFile
    }

    val statusColor = when (status) {
        DownloadStatus.COMPLETED -> MintSecure
        DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> glass.accentStart
        DownloadStatus.PAUSED -> AmberWarning
        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> CoralDanger
    }

    LiquidGlassSurface(
        shape = RoundedCornerShape(22.dp),
        accentBorder = status == DownloadStatus.DOWNLOADING || (status == DownloadStatus.COMPLETED && isApk),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = fileIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${status.name.lowercase().replaceFirstChar { it.uppercase() }} · " +
                            NovaDownloadManager.formatBytes(item.downloadedBytes) +
                            if (item.totalBytes > 0) " / ${NovaDownloadManager.formatBytes(item.totalBytes)}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                when (status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                        GlassIconButton(
                            icon = Icons.Rounded.Pause,
                            contentDescription = "Pause download",
                            onClick = onPause,
                            iconSize = 20.dp
                        )
                    }
                    DownloadStatus.PAUSED -> {
                        GlassIconButton(
                            icon = Icons.Rounded.PlayArrow,
                            contentDescription = "Resume download",
                            onClick = onResume,
                            tint = MintSecure,
                            iconSize = 20.dp
                        )
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        GlassIconButton(
                            icon = Icons.Rounded.Refresh,
                            contentDescription = "Retry download",
                            onClick = onRetry,
                            tint = glass.accentStart,
                            iconSize = 20.dp
                        )
                    }
                    DownloadStatus.COMPLETED -> {
                        GlassIconButton(
                            icon = Icons.Rounded.Share,
                            contentDescription = "Share file",
                            onClick = onShare,
                            iconSize = 19.dp
                        )
                    }
                }

                GlassIconButton(
                    icon = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    onClick = onDelete,
                    iconSize = 19.dp
                )
            }

            if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(10.dp))
                if (item.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { item.progressFraction },
                        color = statusColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (status == DownloadStatus.DOWNLOADING) {
                    LinearProgressIndicator(
                        color = statusColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (status == DownloadStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApk) MintSecure else glass.accentStart,
                        contentColor = Color(0xFF041424)
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("download_open_${item.id}")
                ) {
                    Icon(
                        imageVector = if (isApk) Icons.Rounded.InstallMobile else Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isApk) "Install APK" else "Open File")
                }
            }

            if (!item.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoralDanger
                )
            }
        }
    }
}
