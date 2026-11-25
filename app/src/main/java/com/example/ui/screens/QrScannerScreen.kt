package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.components.GlassIconButton
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.NovaWallpaperBackground
import com.example.ui.components.consumeAllTouches
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LuminousViolet
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrScannerScreen(
    onCodeScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val scannedOnce = remember { AtomicBoolean(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Consume any stray touches so taps on scanner preview or empty areas never hit Home underneath
        Box(modifier = Modifier.fillMaxSize().consumeAllTouches())
        if (hasCameraPermission) {
            val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
            val reader = remember { MultiFormatReader() }

            var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
            var imageAnalysisRef by remember { mutableStateOf<ImageAnalysis?>(null) }

            DisposableEffect(Unit) {
                onDispose {
                    runCatching { imageAnalysisRef?.clearAnalyzer() }
                    runCatching { cameraProviderRef?.unbindAll() }
                    analyzerExecutor.shutdown()
                }
            }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        isClickable = true
                        isFocusable = true
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        runCatching {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderRef = cameraProvider
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            imageAnalysisRef = imageAnalysis

                            imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                                decodeQrFromImageProxy(imageProxy, reader)?.let { text ->
                                    if (scannedOnce.compareAndSet(false, true)) {
                                        previewView.post { onCodeScanned(text) }
                                    }
                                }
                            }

                            cameraProvider.unbindAll()
                            boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Futuristic Scanner Reticle (also shields touches after AndroidView PreviewView)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeAllTouches(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(listOf(ElectricCyan, LuminousViolet)),
                            shape = RoundedCornerShape(28.dp)
                        )
                )
            }
        } else {
            NovaWallpaperBackground(isIncognito = false)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassSurface(shape = RoundedCornerShape(24.dp), strong = true) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Grant camera access to scan QR links directly into Nova Browser.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Allow Camera")
                        }
                    }
                }
            }
        }

        // Top Floating Controls
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            LiquidGlassSurface(shape = RoundedCornerShape(50), strong = true) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack
                )
            }

            if (hasCameraPermission) {
                LiquidGlassSurface(shape = RoundedCornerShape(50), strong = true) {
                    GlassIconButton(
                        icon = if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = "Toggle flashlight",
                        active = torchEnabled,
                        onClick = {
                            torchEnabled = !torchEnabled
                            boundCamera?.cameraControl?.enableTorch(torchEnabled)
                        }
                    )
                }
            }
        }

        // Bottom Hint Pill
        LiquidGlassSurface(
            shape = RoundedCornerShape(22.dp),
            strong = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Text(
                text = "Point camera at a QR code to open URL or search",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )
        }
    }
}

private fun decodeQrFromImageProxy(
    imageProxy: ImageProxy,
    reader: MultiFormatReader
): String? {
    return try {
        val plane = imageProxy.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val data = if (pixelStride == 1 && rowStride == width) {
            ByteArray(buffer.remaining()).also { buffer.get(it) }
        } else {
            // Compact Y-plane rows cleanly regardless of device rowStride padding
            val packed = ByteArray(width * height)
            val rowBuf = ByteArray(rowStride)
            for (row in 0 until height) {
                val rowStart = row * rowStride
                if (rowStart >= buffer.limit()) break
                buffer.position(rowStart)
                val toRead = minOf(rowStride, buffer.remaining())
                buffer.get(rowBuf, 0, toRead)
                if (pixelStride == 1) {
                    System.arraycopy(rowBuf, 0, packed, row * width, minOf(width, toRead))
                } else {
                    for (col in 0 until width) {
                        val idx = col * pixelStride
                        if (idx < toRead) {
                            packed[row * width + col] = rowBuf[idx]
                        }
                    }
                }
            }
            packed
        }

        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        reader.decode(bitmap).text
    } catch (_: Exception) {
        null
    } finally {
        reader.reset()
        imageProxy.close()
    }
}
