package com.example.downloads

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import com.example.data.local.DownloadEntity
import com.example.data.local.DownloadStatus
import com.example.data.repository.BrowserRepository
import com.example.notifications.NovaNotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class OpenFileOutcome {
    data object Opened : OpenFileOutcome()
    data class NeedsUnknownSourcesPermission(val item: DownloadEntity) : OpenFileOutcome()
    data class Error(val message: String) : OpenFileOutcome()
}

class NovaDownloadManager(
    private val context: Context,
    private val repository: BrowserRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val pausedIds = ConcurrentHashMap.newKeySet<Long>()

    private val _completedDownloads = MutableSharedFlow<DownloadEntity>(extraBufferCapacity = 8)
    val completedDownloads: SharedFlow<DownloadEntity> = _completedDownloads.asSharedFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    fun getDownloadsDirectory(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun startDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        refererUrl: String? = null,
        onStarted: (String) -> Unit = {}
    ) {
        scope.launch {
            val cleanUrl = url.trim()
            if (cleanUrl.startsWith("data:", ignoreCase = true)) {
                handleDataUriDownload(cleanUrl, onStarted = onStarted)
                return@launch
            }
            if (cleanUrl.startsWith("blob:", ignoreCase = true)) {
                onStarted("Resolving blob download...")
                return@launch
            }
            if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
                !cleanUrl.startsWith("https://", ignoreCase = true)
            ) {
                onStarted("Unsupported download URL")
                return@launch
            }

            val resolvedMime = resolveBestMimeType(cleanUrl, mimeType, contentDisposition)
            val guessedName = buildCleanFileName(cleanUrl, contentDisposition, resolvedMime)
            val targetFile = uniqueTargetFile(getDownloadsDirectory(), guessedName)

            val entity = DownloadEntity(
                fileName = targetFile.name,
                url = cleanUrl,
                filePath = targetFile.absolutePath,
                mimeType = resolvedMime,
                totalBytes = -1L,
                downloadedBytes = 0L,
                status = DownloadStatus.QUEUED.name
            )
            val id = repository.insertDownload(entity)
            onStarted(targetFile.name)
            launchDownloadJob(id, resume = false, userAgent = userAgent, refererUrl = refererUrl)
        }
    }

    fun saveBase64OrBlobDownload(
        payload: String,
        suggestedFileName: String? = null,
        suggestedMime: String? = null,
        onStarted: (String) -> Unit = {}
    ) {
        scope.launch {
            handleDataUriDownload(
                dataUrl = payload,
                explicitFileName = suggestedFileName,
                explicitMime = suggestedMime,
                onStarted = onStarted
            )
        }
    }

    fun pauseDownload(downloadId: Long) {
        pausedIds.add(downloadId)
        activeJobs.remove(downloadId)?.cancel()
        scope.launch {
            val existing = repository.getDownload(downloadId) ?: return@launch
            val fileBytes = File(existing.filePath).takeIf { it.exists() }?.length() ?: existing.downloadedBytes
            repository.updateDownloadProgress(
                id = downloadId,
                downloadedBytes = fileBytes,
                totalBytes = existing.totalBytes,
                status = DownloadStatus.PAUSED.name,
                errorMessage = null
            )
        }
    }

    fun resumeDownload(downloadId: Long) {
        pausedIds.remove(downloadId)
        launchDownloadJob(downloadId, resume = true, userAgent = null, refererUrl = null)
    }

    fun retryDownload(downloadId: Long) {
        pausedIds.remove(downloadId)
        scope.launch {
            val existing = repository.getDownload(downloadId) ?: return@launch
            File(existing.filePath).takeIf { it.exists() }?.delete()
            repository.updateDownloadProgress(
                id = downloadId,
                downloadedBytes = 0L,
                totalBytes = -1L,
                status = DownloadStatus.QUEUED.name,
                errorMessage = null
            )
            launchDownloadJob(downloadId, resume = false, userAgent = null, refererUrl = null)
        }
    }

    fun deleteDownload(downloadId: Long) {
        pausedIds.remove(downloadId)
        activeJobs.remove(downloadId)?.cancel()
        scope.launch {
            val existing = repository.getDownload(downloadId)
            if (existing != null) {
                runCatching { File(existing.filePath).delete() }
            }
            repository.deleteDownload(downloadId)
        }
    }

    /**
     * Checks whether Android allows this browser to invoke the official Package Installer
     * for downloaded APK files ("Install unknown apps" permission).
     */
    fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else {
            true
        }
    }

    /**
     * Opens Android's official "Install unknown apps -> Allow from this source" screen for Nova Browser.
     */
    fun createUnknownSourcesSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    /**
     * Opens or installs any downloaded file (APK, PDF, MP4, MP3, ZIP, Image, Document, etc.)
     * directly via FileProvider content URI and Android's official Package Installer / ACTION_VIEW flow.
     */
    fun openDownloadedFile(item: DownloadEntity): OpenFileOutcome {
        val file = File(item.filePath)
        if (!file.exists()) {
            return OpenFileOutcome.Error("File no longer exists on device.")
        }

        val isApk = item.fileName.endsWith(".apk", ignoreCase = true)
        if (isApk && !canInstallUnknownApps()) {
            return OpenFileOutcome.NeedsUnknownSourcesPermission(item)
        }

        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val resolvedMime = if (isApk) {
                APK_MIME_TYPE
            } else {
                resolveMimeFromFileName(item.fileName)
                    ?: item.mimeType.takeIf { it.isNotBlank() && it != "application/octet-stream" }
                    ?: "*/*"
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (isApk) {
                val directLaunched = runCatching {
                    context.startActivity(viewIntent)
                    true
                }.getOrDefault(false)

                if (!directLaunched) {
                    val downloadsViewer = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(downloadsViewer)
                }
            } else {
                val chooser = Intent.createChooser(viewIntent, "Open ${item.fileName}").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
            OpenFileOutcome.Opened
        }.getOrElse { err ->
            OpenFileOutcome.Error(err.localizedMessage ?: "Unable to open ${item.fileName}")
        }
    }

    fun shareDownloadedFile(item: DownloadEntity): Result<Unit> {
        return runCatching {
            val file = File(item.filePath)
            if (!file.exists()) error("File not found on device.")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = resolveMimeFromFileName(item.fileName) ?: item.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share ${item.fileName}")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun showDownloadCompletedNotification(fileName: String) {
        NovaNotificationHelper.showDownloadNotification(context, fileName)
    }

    private suspend fun handleDataUriDownload(
        dataUrl: String,
        explicitFileName: String? = null,
        explicitMime: String? = null,
        onStarted: (String) -> Unit
    ) {
        try {
            val hasDataPrefix = dataUrl.startsWith("data:", ignoreCase = true)
            val header = if (hasDataPrefix) dataUrl.substringAfter("data:", "").substringBefore(",", "") else ""
            val payload = if (hasDataPrefix) dataUrl.substringAfter(",", "") else dataUrl
            val mime = explicitMime?.ifBlank { null }
                ?: header.substringBefore(";").ifBlank { null }
                ?: "application/octet-stream"
            val ext = explicitFileName?.substringAfterLast('.', "")?.ifBlank { null }
                ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                ?: resolveExtensionFromMime(mime)
                ?: "bin"
            val rawBase = explicitFileName?.substringBeforeLast('.')?.replace(Regex("[^a-zA-Z0-9._-]"), "_")?.ifBlank { null }
                ?: "nova_file_${System.currentTimeMillis()}"
            val targetName = if (explicitFileName != null && explicitFileName.contains(".")) {
                explicitFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            } else {
                "$rawBase.$ext"
            }
            val targetFile = uniqueTargetFile(getDownloadsDirectory(), targetName)
            val bytes = if (!hasDataPrefix || header.contains(";base64", ignoreCase = true)) {
                Base64.decode(payload.trim(), Base64.DEFAULT)
            } else {
                Uri.decode(payload).toByteArray(Charsets.UTF_8)
            }
            targetFile.writeBytes(bytes)
            mirrorToPublicDownloads(targetFile, mime)
            val entity = DownloadEntity(
                fileName = targetFile.name,
                url = if (hasDataPrefix) "data:$mime" else "blob:completed",
                filePath = targetFile.absolutePath,
                mimeType = mime,
                totalBytes = bytes.size.toLong(),
                downloadedBytes = bytes.size.toLong(),
                status = DownloadStatus.COMPLETED.name
            )
            val id = repository.insertDownload(entity)
            showDownloadCompletedNotification(targetFile.name)
            onStarted(targetFile.name)
            _completedDownloads.tryEmit(entity.copy(id = id))
        } catch (_: Exception) {
            onStarted("Download failed")
        }
    }

    private fun launchDownloadJob(
        downloadId: Long,
        resume: Boolean,
        userAgent: String?,
        refererUrl: String?
    ) {
        activeJobs.remove(downloadId)?.cancel()
        val job = scope.launch {
            val currentJob = currentCoroutineContext()[Job]
            val item = repository.getDownload(downloadId) ?: return@launch
            val file = File(item.filePath)
            file.parentFile?.mkdirs()

            val existingBytes = if (resume && file.exists()) file.length() else 0L
            try {
                repository.updateDownloadProgress(
                    id = downloadId,
                    downloadedBytes = existingBytes,
                    totalBytes = item.totalBytes,
                    status = DownloadStatus.DOWNLOADING.name,
                    errorMessage = null
                )

                val requestBuilder = Request.Builder()
                    .url(item.url)
                    .header("Accept", "*/*")

                // Forward active WebView session cookies so authenticated & mirror downloads succeed
                val cookies = runCatching { CookieManager.getInstance().getCookie(item.url) }.getOrNull()
                if (!cookies.isNullOrBlank()) {
                    requestBuilder.header("Cookie", cookies)
                }
                if (!userAgent.isNullOrBlank()) {
                    requestBuilder.header("User-Agent", userAgent)
                } else {
                    requestBuilder.header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    )
                }
                if (!refererUrl.isNullOrBlank() && refererUrl.startsWith("http")) {
                    requestBuilder.header("Referer", refererUrl)
                }
                if (existingBytes > 0L) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        repository.updateDownloadProgress(
                            id = downloadId,
                            downloadedBytes = existingBytes,
                            totalBytes = item.totalBytes,
                            status = DownloadStatus.FAILED.name,
                            errorMessage = "HTTP ${response.code}: ${response.message}"
                        )
                        return@launch
                    }

                    val body = response.body ?: run {
                        repository.updateDownloadProgress(
                            id = downloadId,
                            downloadedBytes = existingBytes,
                            totalBytes = item.totalBytes,
                            status = DownloadStatus.FAILED.name,
                            errorMessage = "Empty response body"
                        )
                        return@launch
                    }

                    val isPartialAppend = response.code == 206 && existingBytes > 0L
                    val startOffset = if (isPartialAppend) existingBytes else 0L
                    val contentLength = body.contentLength()
                    val totalBytes = if (contentLength > 0L) startOffset + contentLength else -1L

                    var downloaded = startOffset
                    var lastEmitTime = 0L

                    FileOutputStream(file, isPartialAppend).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                if (pausedIds.contains(downloadId)) {
                                    repository.updateDownloadProgress(
                                        id = downloadId,
                                        downloadedBytes = downloaded,
                                        totalBytes = totalBytes,
                                        status = DownloadStatus.PAUSED.name
                                    )
                                    return@launch
                                }
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read

                                val now = System.currentTimeMillis()
                                if (now - lastEmitTime >= 200L) {
                                    lastEmitTime = now
                                    repository.updateDownloadProgress(
                                        id = downloadId,
                                        downloadedBytes = downloaded,
                                        totalBytes = totalBytes,
                                        status = DownloadStatus.DOWNLOADING.name
                                    )
                                }
                            }
                            output.flush()
                        }
                    }

                    val finalTotal = if (totalBytes > 0L) totalBytes else downloaded
                    val finalMime = resolveMimeFromFileName(item.fileName)
                        ?: response.header("Content-Type")?.substringBefore(";")?.trim()
                        ?: item.mimeType

                    // Mirror completed file to public MediaStore Downloads on Android 10+ (zero permissions)
                    mirrorToPublicDownloads(file, finalMime)

                    repository.updateDownloadProgress(
                        id = downloadId,
                        downloadedBytes = downloaded,
                        totalBytes = finalTotal,
                        status = DownloadStatus.COMPLETED.name,
                        errorMessage = null
                    )

                    _completedDownloads.tryEmit(
                        item.copy(
                            mimeType = finalMime,
                            downloadedBytes = downloaded,
                            totalBytes = finalTotal,
                            status = DownloadStatus.COMPLETED.name
                        )
                    )
                }
            } catch (ce: CancellationException) {
                if (pausedIds.contains(downloadId) && activeJobs[downloadId] == null) {
                    val bytes = file.takeIf { it.exists() }?.length() ?: existingBytes
                    repository.updateDownloadProgress(
                        id = downloadId,
                        downloadedBytes = bytes,
                        totalBytes = item.totalBytes,
                        status = DownloadStatus.PAUSED.name
                    )
                }
                throw ce
            } catch (e: Exception) {
                val bytes = file.takeIf { it.exists() }?.length() ?: existingBytes
                repository.updateDownloadProgress(
                    id = downloadId,
                    downloadedBytes = bytes,
                    totalBytes = item.totalBytes,
                    status = DownloadStatus.FAILED.name,
                    errorMessage = e.localizedMessage ?: "Download failed"
                )
            } finally {
                if (currentJob != null) {
                    activeJobs.remove(downloadId, currentJob)
                }
            }
        }
        activeJobs[downloadId] = job
    }

    /**
     * Copies completed downloads into Android's public Downloads folder via MediaStore on API 29+
     * without requesting broad storage permissions, so files are visible in system Files too.
     */
    private fun mirrorToPublicDownloads(sourceFile: File, mimeType: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.Downloads.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NovaBrowser")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    private fun extractFileNameFromContentDisposition(cd: String?): String? {
        if (cd.isNullOrBlank()) return null
        val starMatch = Regex("""filename\*\s*=\s*([^\'\s]+)\'\'([^;\s]+)""", RegexOption.IGNORE_CASE).find(cd)
        if (starMatch != null) {
            val encoded = starMatch.groupValues[2]
            val decoded = runCatching { Uri.decode(encoded) }.getOrNull()
            if (!decoded.isNullOrBlank()) return decoded.substringAfterLast('/').substringAfterLast('\\')
        }
        val plainMatch = Regex("""filename\s*=\s*"?([^";]+)"?""", RegexOption.IGNORE_CASE).find(cd)
        if (plainMatch != null) {
            val name = plainMatch.groupValues[1].trim()
            if (name.isNotBlank()) return name.substringAfterLast('/').substringAfterLast('\\')
        }
        return null
    }

    private fun buildCleanFileName(url: String, contentDisposition: String?, mimeType: String): String {
        val fromDisposition = extractFileNameFromContentDisposition(contentDisposition)
        val guessed = fromDisposition ?: URLUtil.guessFileName(url, contentDisposition, mimeType)
        val sanitized = guessed.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifBlank { "nova_download_${System.currentTimeMillis()}" }

        // Ensure APK URLs keep .apk extension even when server returns generic mime
        if (url.substringBefore("?").endsWith(".apk", ignoreCase = true) &&
            !sanitized.endsWith(".apk", ignoreCase = true)
        ) {
            return sanitized.substringBeforeLast(".") + ".apk"
        }
        return sanitized
    }

    private fun resolveBestMimeType(url: String, providedMime: String?, contentDisposition: String?): String {
        val cleanUrl = url.substringBefore("?").lowercase()
        val dispLower = contentDisposition.orEmpty().lowercase()
        if (cleanUrl.endsWith(".apk") || dispLower.contains(".apk")) {
            return APK_MIME_TYPE
        }
        val fromUrl = resolveMimeFromFileName(cleanUrl)
        if (fromUrl != null) return fromUrl
        if (!providedMime.isNullOrBlank() && providedMime != "application/octet-stream") {
            return providedMime.substringBefore(";").trim()
        }
        return "application/octet-stream"
    }

    private fun uniqueTargetFile(dir: File, desiredName: String): File {
        var candidate = File(dir, desiredName)
        if (!candidate.exists()) return candidate
        val dot = desiredName.lastIndexOf('.')
        val base = if (dot > 0) desiredName.substring(0, dot) else desiredName
        val ext = if (dot > 0) desiredName.substring(dot) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_($counter)$ext")
            counter++
        }
        return candidate
    }

    companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"

        private val explicitMimeMap = mapOf(
            "apk" to APK_MIME_TYPE,
            "apks" to "application/zip",
            "xapk" to "application/zip",
            "pdf" to "application/pdf",
            "zip" to "application/zip",
            "rar" to "application/vnd.rar",
            "7z" to "application/x-7z-compressed",
            "mp4" to "video/mp4",
            "mkv" to "video/x-matroska",
            "webm" to "video/webm",
            "mp3" to "audio/mpeg",
            "wav" to "audio/wav",
            "m4a" to "audio/mp4",
            "ogg" to "audio/ogg",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "gif" to "image/gif",
            "webp" to "image/webp",
            "svg" to "image/svg+xml",
            "txt" to "text/plain",
            "csv" to "text/csv",
            "json" to "application/json",
            "doc" to "application/msword",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls" to "application/vnd.ms-excel",
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "ppt" to "application/vnd.ms-powerpoint",
            "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "tar" to "application/x-tar",
            "gz" to "application/gzip",
            "bz2" to "application/x-bzip2",
            "xz" to "application/x-xz",
            "iso" to "application/x-iso9660-image",
            "dmg" to "application/x-apple-diskimage",
            "exe" to "application/x-msdownload",
            "msi" to "application/x-msi",
            "bin" to "application/octet-stream",
            "epub" to "application/epub+zip",
            "torrent" to "application/x-bittorrent",
            "flac" to "audio/flac",
            "aac" to "audio/aac",
            "avi" to "video/x-msvideo",
            "mov" to "video/quicktime",
            "flv" to "video/x-flv",
            "3gp" to "video/3gpp",
            "bmp" to "image/bmp",
            "ico" to "image/x-icon",
            "rtf" to "application/rtf"
        )

        fun resolveExtensionFromMime(mime: String): String? {
            val cleanMime = mime.substringBefore(";").trim().lowercase()
            for ((ext, m) in explicitMimeMap) {
                if (m.equals(cleanMime, ignoreCase = true)) return ext
            }
            return null
        }

        fun resolveMimeFromFileName(fileNameOrUrl: String): String? {
            val clean = fileNameOrUrl.substringBefore("?").substringBefore("#")
            val ext = clean.substringAfterLast('.', "").lowercase()
            if (ext.isBlank()) return null
            explicitMimeMap[ext]?.let { return it }
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }

        fun formatBytes(bytes: Long): String {
            if (bytes < 0L) return "Unknown size"
            if (bytes < 1024L) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024.0) return String.format(Locale.US, "%.2f MB", mb)
            return String.format(Locale.US, "%.2f GB", mb / 1024.0)
        }
    }
}
