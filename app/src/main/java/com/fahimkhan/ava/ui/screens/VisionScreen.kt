package com.fahimkhan.ava.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahimkhan.ava.ai.memory.MemoryRepository
import com.fahimkhan.ava.core.utils.CameraRequestState
import com.fahimkhan.ava.ui.components.CyberCard
import com.fahimkhan.ava.ui.components.NeonButton
import com.fahimkhan.ava.ui.components.NeonText
import com.fahimkhan.ava.ui.theme.AvaColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class VisionViewModel @Inject constructor(
    private val memory: MemoryRepository,
    private val cameraState: CameraRequestState
) : ViewModel() {
    private val _scanResult = MutableStateFlow("")
    val scanResult = _scanResult.asStateFlow()

    val useFrontCamera = cameraState.useFrontCamera
    val pendingCapture = cameraState.pendingCapture

    fun toggleCamera() { cameraState.useFrontCamera.value = !cameraState.useFrontCamera.value }
    fun requestCapture() { cameraState.pendingCapture.value = true }
    fun cancelPendingCapture() { cameraState.pendingCapture.value = false }
    fun onCaptureFinished(message: String) {
        _scanResult.value = message
        cameraState.pendingCapture.value = false
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _registrationName = MutableStateFlow("")
    val registrationName = _registrationName.asStateFlow()

    fun setRegistrationName(name: String) { _registrationName.value = name }

    fun registerFace(name: String, relationship: String = "trusted") {
        viewModelScope.launch {
            memory.registerFace(
                name         = name,
                relationship = relationship,
                embedding    = "placeholder_embedding_${System.currentTimeMillis()}",
                imageUri     = "",
                trustLevel   = 1
            )
            _scanResult.value = "✓ Face profile saved for $name"
        }
    }

    fun setOwnerFace(name: String) {
        viewModelScope.launch {
            memory.registerFace(
                name         = name,
                relationship = "owner",
                embedding    = "owner_embedding_${System.currentTimeMillis()}",
                imageUri     = "",
                trustLevel   = 2
            )
            _scanResult.value = "✓ Owner profile registered for $name"
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VisionScreen(
    viewModel: VisionViewModel = hiltViewModel(),
    onBack   : () -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val scanResult       by viewModel.scanResult.collectAsState()
    var registerName     by remember { mutableStateOf("") }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var registerAsOwner  by remember { mutableStateOf(false) }

    val context          = LocalContext.current
    val useFront         by viewModel.useFrontCamera.collectAsState()
    val pendingCapture   by viewModel.pendingCapture.collectAsState()
    val imageCapture     = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    var cameraReady      by remember { mutableStateOf(false) }

    // Don't let a request that never fired (e.g. camera permission denied)
    // surprise the person with a photo the next time this screen opens.
    DisposableEffect(Unit) {
        onDispose { viewModel.cancelPendingCapture() }
    }

    // A voice command ("ছবি তোলো") or the TAKE PHOTO button sets
    // pendingCapture; once the camera is bound we wait a beat for
    // auto-exposure, then take the shot with AVA's own camera.
    LaunchedEffect(cameraReady, pendingCapture) {
        if (cameraReady && pendingCapture) {
            kotlinx.coroutines.delay(1200)
            takePhoto(context, imageCapture) { message -> viewModel.onCaptureFinished(message) }
        }
    }

    // Scan line animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "scanY"
    )

    Box(modifier = Modifier.fillMaxSize().background(AvaColors.BlackAbyss)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = AvaColors.NeonPink) }
                Spacer(Modifier.width(8.dp))
                Column {
                    NeonText("VISION CORE", fontSize = 20.sp, glowColor = AvaColors.CyberCyan, color = AvaColors.TextPrimary, letterSpacing = 3.sp)
                    Text("VISUAL INTELLIGENCE SYSTEM", color = AvaColors.TextMuted, fontSize = 9.sp, letterSpacing = 2.sp)
                }
            }

            if (!cameraPermission.status.isGranted) {
                // Permission request
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = AvaColors.CyberCyan, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Camera access required for visual intelligence", color = AvaColors.TextSecondary, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        NeonButton("GRANT CAMERA ACCESS", AvaColors.CyberCyan, { cameraPermission.launchPermissionRequest() })
                    }
                }
            } else {
                // Camera preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    CameraPreview(useFront = useFront, imageCapture = imageCapture, onBound = { cameraReady = it })

                    // Scan overlay
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Corner brackets
                        VisionOverlay()
                        // Moving scan line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = (320 * scanY).dp)
                                .background(AvaColors.CyberCyan.copy(alpha = 0.7f))
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (scanResult.isNotBlank()) {
                        CyberCard(glowColor = AvaColors.Active) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = AvaColors.Active, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(scanResult, color = AvaColors.Active, fontSize = 13.sp)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NeonButton(
                            text     = "TAKE PHOTO",
                            color    = AvaColors.Active,
                            onClick  = { viewModel.requestCapture() },
                            modifier = Modifier.weight(1f)
                        )
                        NeonButton(
                            text     = if (useFront) "USE BACK CAM" else "USE FRONT CAM",
                            color    = AvaColors.CyberPurple,
                            onClick  = { viewModel.toggleCamera() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NeonButton(
                            text     = "REGISTER FACE",
                            color    = AvaColors.CyberCyan,
                            onClick  = { registerAsOwner = false; showRegisterDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        NeonButton(
                            text     = "SET AS OWNER",
                            color    = AvaColors.NeonRed,
                            onClick  = { registerAsOwner = true; showRegisterDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    CyberCard(glowColor = AvaColors.NeonPurple.copy(0.4f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("VISION CAPABILITIES", color = AvaColors.CyberPurple, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            VisionCapabilityRow(Icons.Rounded.Face,        "Face Recognition",     "ML Kit Face Detection")
                            VisionCapabilityRow(Icons.Rounded.TextFields,  "Text Recognition",     "OCR — Screen & Images")
                            VisionCapabilityRow(Icons.Rounded.Label,       "Object Detection",     "Image Labeling AI")
                            VisionCapabilityRow(Icons.Rounded.Security,    "Intruder Detection",   "Unknown Face Alerts")
                        }
                    }
                }
            }
        }

        // Register dialog
        if (showRegisterDialog) {
            AlertDialog(
                onDismissRequest = { showRegisterDialog = false },
                containerColor   = AvaColors.CardSurface,
                title = { Text(if (registerAsOwner) "SET OWNER PROFILE" else "REGISTER FACE", color = AvaColors.NeonPink, letterSpacing = 2.sp) },
                text = {
                    Column {
                        Text("Enter the person's name:", color = AvaColors.TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value       = registerName,
                            onValueChange = { registerName = it },
                            placeholder = { Text("Full name", color = AvaColors.TextMuted) },
                            colors      = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AvaColors.NeonPink,
                                unfocusedBorderColor = AvaColors.NeonPink.copy(0.4f),
                                focusedTextColor     = AvaColors.TextPrimary,
                                unfocusedTextColor   = AvaColors.TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (registerName.isNotBlank()) {
                            if (registerAsOwner) viewModel.setOwnerFace(registerName)
                            else viewModel.registerFace(registerName)
                            showRegisterDialog = false
                            registerName = ""
                        }
                    }) { Text("REGISTER", color = AvaColors.NeonPink, letterSpacing = 2.sp) }
                },
                dismissButton = {
                    TextButton(onClick = { showRegisterDialog = false }) {
                        Text("CANCEL", color = AvaColors.TextMuted, letterSpacing = 2.sp)
                    }
                }
            )
        }
    }
}

/**
 * Camera preview that also binds an ImageCapture use case so AVA can take
 * photos itself, and rebinds when the front/back lens changes. The parent
 * recomposes constantly (scan-line animation) and AndroidView's `update`
 * runs on every recomposition, so the bound lens is remembered and the
 * (flickery) rebind only happens when [useFront] actually changes.
 */
@Composable
fun CameraPreview(
    useFront     : Boolean,
    imageCapture : ImageCapture,
    onBound      : (Boolean) -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val boundLens      = remember { arrayOfNulls<Boolean>(1) }

    AndroidView(
        factory  = { ctx -> PreviewView(ctx) },
        update   = { previewView ->
            if (boundLens[0] != useFront) {
                boundLens[0] = useFront
                onBound(false)
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener({
                    try {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA
                                       else CameraSelector.DEFAULT_BACK_CAMERA
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                        onBound(true)
                    } catch (e: Exception) {
                        onBound(false)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Takes a photo with AVA's own camera session. Android 10+: saved into the
 * shared Pictures/AVA folder (shows in the Gallery, no storage permission
 * needed for your own new files); older: the app's own picture folder.
 */
private fun takePhoto(context: Context, imageCapture: ImageCapture, onResult: (String) -> Unit) {
    val name = "AVA_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    val outputOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/AVA")
        }
        ImageCapture.OutputFileOptions.Builder(
            context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ).build()
    } else {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        ImageCapture.OutputFileOptions.Builder(File(dir, name)).build()
    }

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onResult("✓ Photo saved: $name")
            }
            override fun onError(exc: ImageCaptureException) {
                onResult("✗ Couldn't take the photo: ${exc.message}")
            }
        }
    )
}

@Composable
fun VisionOverlay() {
    val color  = AvaColors.CyberCyan.copy(alpha = 0.8f)
    val stroke = 3.dp
    val size   = 40.dp

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Top-left
        Box(Modifier.align(Alignment.TopStart)) {
            Box(Modifier.width(size).height(stroke).background(color))
            Box(Modifier.width(stroke).height(size).background(color))
        }
        // Top-right
        Box(Modifier.align(Alignment.TopEnd)) {
            Box(Modifier.width(size).height(stroke).background(color).align(Alignment.TopEnd))
            Box(Modifier.width(stroke).height(size).background(color).align(Alignment.TopEnd))
        }
        // Bottom-left
        Box(Modifier.align(Alignment.BottomStart)) {
            Box(Modifier.width(size).height(stroke).background(color).align(Alignment.BottomStart))
            Box(Modifier.width(stroke).height(size).background(color).align(Alignment.BottomStart))
        }
        // Bottom-right
        Box(Modifier.align(Alignment.BottomEnd)) {
            Box(Modifier.width(size).height(stroke).background(color).align(Alignment.BottomEnd))
            Box(Modifier.width(stroke).height(size).background(color).align(Alignment.BottomEnd))
        }
    }
}

@Composable
fun VisionCapabilityRow(
    icon        : androidx.compose.ui.graphics.vector.ImageVector,
    label       : String,
    description : String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = AvaColors.CyberPurple, modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label,       color = AvaColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(description, color = AvaColors.TextMuted,     fontSize = 10.sp)
        }
    }
}
