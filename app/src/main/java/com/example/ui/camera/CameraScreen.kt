package com.example.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.model.OrbitCaptureMode
import com.example.model.OrbitProject
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.RedShutter
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.SlateDark950
import com.example.ui.theme.SlateLight100
import kotlinx.coroutines.delay

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToStudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var showFlash by remember { mutableStateOf(false) }
    var showGhostSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CameraEffect.ShutterFlash -> {
                    showFlash = true
                    delay(80)
                    showFlash = false
                }
                is CameraEffect.SeriesFinished -> {
                    snackbarHostState.showSnackbar(
                        "Serie '${effect.project.name}' vollständig (${effect.project.targetCount} Aufnahmen)!"
                    )
                }
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Battery & Lifecycle optimization: Pause interval capture in background and unbind camera on dispose
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppMovedToBackground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onAppMovedToBackground()
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionRequestView(
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            modifier = modifier
        )
        return
    }

    // CameraX objects
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark950)
    ) {
        // 1. CameraX PreviewView
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_preview_view")
        )

        // 1b. Ghost Overlay (Onion Skin) – purely decorative over preview, never burned into captured photo
        if (uiState.isGhostOverlayEnabled && uiState.ghostOverlayBitmap != null) {
            Image(
                bitmap = uiState.ghostOverlayBitmap!!.asImageBitmap(),
                contentDescription = "Ghost Overlay Referenzbild",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(uiState.ghostOverlayAlpha)
                    .testTag("ghost_overlay_view"),
                contentScale = ContentScale.Crop
            )
        }

        // 2. Interactive Overlay (Grid, Motivanker, Guides)
        CameraOverlay(
            mode = uiState.selectedMode,
            isGridVisible = uiState.isGridVisible,
            isAnchorVisible = uiState.isAnchorVisible,
            anchorX = uiState.anchorX,
            anchorY = uiState.anchorY,
            onTap = { normX, normY ->
                viewModel.setAnchor(normX, normY)
            }
        )

        // 3. Shutter Flash Feedback
        AnimatedVisibility(
            visible = showFlash,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.65f))
            )
        }

        // 4. Top Controls & Header Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .align(Alignment.TopCenter)
        ) {
            // Mode Selector & Tool Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Orbit capture mode chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OrbitCaptureMode.values().forEach { mode ->
                        val isSelected = uiState.selectedMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectMode(mode) },
                            label = { Text(mode.title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = SlateDark950,
                                containerColor = SlateDark900.copy(alpha = 0.8f),
                                labelColor = SlateLight100
                            ),
                            modifier = Modifier.testTag("mode_chip_${mode.name.lowercase()}")
                        )
                    }
                }

                // Grid, Anchor, and Ghost Overlay Toggles
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { viewModel.toggleGrid() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (uiState.isGridVisible) CyanAccent.copy(alpha = 0.25f) else SlateDark900.copy(alpha = 0.8f),
                                CircleShape
                            )
                            .testTag("btn_toggle_grid")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Raster umschalten",
                            tint = if (uiState.isGridVisible) CyanNeon else SlateLight100,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleAnchor() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (uiState.isAnchorVisible) CyanAccent.copy(alpha = 0.25f) else SlateDark900.copy(alpha = 0.8f),
                                CircleShape
                            )
                            .testTag("btn_toggle_anchor")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Motivanker umschalten",
                            tint = if (uiState.isAnchorVisible) CyanNeon else SlateLight100,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Ghost Overlay (Onion Skin) Toggle
                    IconButton(
                        onClick = {
                            viewModel.toggleGhostOverlay()
                            if (!uiState.isGhostOverlayEnabled) {
                                showGhostSettings = true
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (uiState.isGhostOverlayEnabled) CyanNeon.copy(alpha = 0.35f) else SlateDark900.copy(alpha = 0.8f),
                                CircleShape
                            )
                            .border(
                                width = if (uiState.isGhostOverlayEnabled) 1.5.dp else 0.dp,
                                color = if (uiState.isGhostOverlayEnabled) CyanNeon else Color.Transparent,
                                shape = CircleShape
                            )
                            .testTag("btn_toggle_ghost")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Ghost Overlay umschalten",
                            tint = if (uiState.isGhostOverlayEnabled) CyanNeon else SlateLight100,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Ghost Overlay Quick Settings Panel (Expandable)
            if (uiState.isGhostOverlayEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDark900.copy(alpha = 0.92f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ghost_overlay_panel")
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Ghost Overlay (Onion Skin) • ${(uiState.ghostOverlayAlpha * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CyanNeon
                                )
                            }

                            // Reference Mode Switch (Zuletzt vs Erstes)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val isLast = uiState.ghostReferenceMode == "LAST"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isLast) CyanAccent else SlateDark800)
                                        .clickable { viewModel.setGhostReferenceMode("LAST") }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("btn_ghost_ref_last")
                                ) {
                                    Text(
                                        text = "Zuletzt",
                                        fontSize = 10.sp,
                                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isLast) SlateDark950 else SlateLight100
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (!isLast) CyanAccent else SlateDark800)
                                        .clickable { viewModel.setGhostReferenceMode("FIRST") }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("btn_ghost_ref_first")
                                ) {
                                    Text(
                                        text = "Erstes",
                                        fontSize = 10.sp,
                                        fontWeight = if (!isLast) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!isLast) SlateDark950 else SlateLight100
                                    )
                                }
                            }
                        }

                        // Opacity Slider & Quick Presets (25%, 45%, 65%, 80%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Slider(
                                value = uiState.ghostOverlayAlpha,
                                onValueChange = { viewModel.setGhostOverlayAlpha(it) },
                                valueRange = 0.05f..0.95f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanNeon,
                                    activeTrackColor = CyanAccent,
                                    inactiveTrackColor = SlateDark800
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .testTag("slider_ghost_alpha")
                            )

                            // Quick preset chips
                            listOf(0.25f to "25%", 0.45f to "45%", 0.65f to "65%", 0.80f to "80%").forEach { (presetAlpha, label) ->
                                val isSelected = kotlin.math.abs(uiState.ghostOverlayAlpha - presetAlpha) < 0.05f
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) CyanNeon else SlateDark800)
                                        .clickable { viewModel.setGhostOverlayAlpha(presetAlpha) }
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                        .testTag("preset_ghost_${label.replace("%", "")}")
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SlateDark950 else SlateLight100
                                    )
                                }
                            }
                        }

                        if (uiState.ghostOverlayBitmap == null) {
                            Text(
                                text = "Aktiviert • Erstes Foto wird automatisch als transparente Hilfslinie überlagert",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateLight100.copy(alpha = 0.6f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Guidance & Series Status Card
            val active = uiState.activeProject
            val captured = active?.actualCount ?: 0
            val target = uiState.targetCount

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark900.copy(alpha = 0.88f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = active?.name ?: "Bereit für Serie",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = SlateLight100
                        )

                        // Mode Selector: Manuell vs Intervall
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SlateDark800)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (!uiState.isIntervalMode) CyanAccent else Color.Transparent)
                                    .clickable { if (uiState.isIntervalMode) viewModel.toggleIntervalMode() }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("tab_mode_manual")
                            ) {
                                Text(
                                    text = "Manuell",
                                    fontSize = 11.sp,
                                    fontWeight = if (!uiState.isIntervalMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!uiState.isIntervalMode) SlateDark950 else SlateLight100
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (uiState.isIntervalMode) CyanAccent else Color.Transparent)
                                    .clickable { if (!uiState.isIntervalMode) viewModel.toggleIntervalMode() }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("tab_mode_interval")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = if (uiState.isIntervalMode) SlateDark950 else SlateLight100,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Intervall",
                                        fontSize = 11.sp,
                                        fontWeight = if (uiState.isIntervalMode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (uiState.isIntervalMode) SlateDark950 else SlateLight100
                                    )
                                }
                            }
                        }
                    }

                    // Target Count & Interval Seconds Configuration
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Series Target chips (30, 50, 75, 100)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Ziel:", fontSize = 10.sp, color = SlateLight100.copy(alpha = 0.7f))
                            listOf(20, 30, 50, 75, 100).forEach { count ->
                                val selected = target == count
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (selected) CyanAccent else SlateDark800)
                                        .clickable { viewModel.selectTargetCount(count) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("target_chip_$count")
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) SlateDark950 else SlateLight100
                                    )
                                }
                            }
                        }

                        // If Interval Mode: Interval Duration Chips (2s, 3s, 5s, 10s)
                        if (uiState.isIntervalMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("Takt:", fontSize = 10.sp, color = CyanAccent)
                                listOf(2, 3, 5, 10).forEach { sec ->
                                    val selected = uiState.intervalSeconds == sec
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(if (selected) CyanNeon else SlateDark800)
                                            .clickable { viewModel.setIntervalSeconds(sec) }
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                            .testTag("interval_chip_${sec}s")
                                    ) {
                                        Text(
                                            text = "${sec}s",
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) SlateDark950 else SlateLight100
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { if (target > 0) (captured.toFloat() / target).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyanAccent,
                        trackColor = SlateDark800
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isIntervalMode && uiState.intervalStatus == IntervalStatus.RUNNING) {
                                "Automatische Serie läuft • Bewege dich gleichmäßig im Kreis"
                            } else {
                                uiState.selectedMode.guideHint
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$captured / $target",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlateLight100,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // 5. Bottom Controls (Shutter / Interval Controls, Thumbnail, New Series)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live countdown status banner during Interval Capture
            if (uiState.isIntervalMode && uiState.intervalStatus == IntervalStatus.RUNNING) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateDark900.copy(alpha = 0.9f))
                        .border(1.dp, CyanNeon, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("interval_countdown_badge")
                ) {
                    Text(
                        text = if (uiState.intervalCountdown > 0) "Nächste Aufnahme in: ${uiState.intervalCountdown}s" else "Auslösen...",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanNeon
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (uiState.isIntervalMode && uiState.intervalStatus == IntervalStatus.PAUSED) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateDark900.copy(alpha = 0.9f))
                        .border(1.dp, AmberAccent, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("interval_paused_badge")
                ) {
                    Text(
                        text = "Intervallaufnahme pausiert",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AmberAccent
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Thumbnail of last photo / Open in Studio
                val latestThumb = uiState.lastCapturedThumbnail
                val activeProj = uiState.activeProject
                val lastPath = activeProj?.imagePaths?.lastOrNull()

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateDark800)
                        .border(1.5.dp, if (activeProj != null && activeProj.actualCount > 0) CyanAccent else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable(enabled = activeProj != null && activeProj.actualCount > 0) {
                            activeProj?.let { onNavigateToStudio(it.id) }
                        }
                        .testTag("btn_camera_thumbnail"),
                    contentAlignment = Alignment.Center
                ) {
                    if (latestThumb != null) {
                        Image(
                            bitmap = latestThumb.asImageBitmap(),
                            contentDescription = "Zuletzt aufgenommen",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (lastPath != null) {
                        AsyncImage(
                            model = lastPath,
                            contentDescription = "Zuletzt aufgenommen",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoAlbum,
                            contentDescription = "Keine Aufnahmen",
                            tint = SlateLight100.copy(alpha = 0.4f)
                        )
                    }
                }

                // Center Action: Manual Shutter vs Interval Controls
                if (!uiState.isIntervalMode) {
                    // Manual Single Trigger Shutter Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SlateDark950.copy(alpha = 0.6f))
                            .border(3.5.dp, CyanAccent, CircleShape)
                            .clickable(enabled = !uiState.isCapturing) {
                                viewModel.capturePhoto(imageCapture)
                            }
                            .padding(6.dp)
                            .testTag("btn_shutter_trigger"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (uiState.isCapturing) CyanNeon.copy(alpha = 0.5f) else Color.White)
                        )
                    }
                } else {
                    // Interval Mode Controls (Start / Pause / Resume / Stop)
                    when (uiState.intervalStatus) {
                        IntervalStatus.STOPPED -> {
                            // Big Interval Start Button
                            Button(
                                onClick = { viewModel.startIntervalCapture(imageCapture) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = SlateDark950
                                ),
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("btn_interval_start")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Intervall starten",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Intervall Start (${uiState.intervalSeconds}s)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        IntervalStatus.RUNNING -> {
                            // Running state: Pause + Stop controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pause Button
                                IconButton(
                                    onClick = { viewModel.pauseIntervalCapture() },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(AmberAccent, CircleShape)
                                        .testTag("btn_interval_pause")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = "Pausieren",
                                        tint = SlateDark950,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Center Countdown Badge
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(SlateDark900, CircleShape)
                                        .border(2.5.dp, CyanNeon, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${uiState.intervalCountdown}s",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CyanNeon
                                    )
                                }

                                // Stop Button
                                IconButton(
                                    onClick = { viewModel.stopIntervalCapture() },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(RedShutter, CircleShape)
                                        .testTag("btn_interval_stop")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Beenden",
                                        tint = SlateLight100,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                        IntervalStatus.PAUSED -> {
                            // Paused state: Resume + Stop controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Resume Button
                                Button(
                                    onClick = { viewModel.resumeIntervalCapture(imageCapture) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanAccent,
                                        contentColor = SlateDark950
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("btn_interval_resume")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Fortsetzen",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fortsetzen", fontWeight = FontWeight.Bold)
                                }

                                // Stop Button
                                IconButton(
                                    onClick = { viewModel.stopIntervalCapture() },
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(RedShutter, CircleShape)
                                        .testTag("btn_interval_stop_paused")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Beenden",
                                        tint = SlateLight100,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right: Start New Series Button
                IconButton(
                    onClick = { viewModel.startNewSeries() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(SlateDark800, CircleShape)
                        .border(1.dp, SlateLight100.copy(alpha = 0.2f), CircleShape)
                        .testTag("btn_new_series")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Neue Serie starten",
                        tint = SlateLight100
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (uiState.isIntervalMode) {
                    "Intervallmodus aktiv • Kamera löst alle ${uiState.intervalSeconds}s automatisch aus"
                } else {
                    "Tippe für Motivanker • Auslöser für nächste Perspektive"
                },
                style = MaterialTheme.typography.labelSmall,
                color = SlateLight100.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 110.dp)
        )
    }
}

@Composable
fun CameraPermissionRequestView(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark950)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateDark900),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Kamera",
                        tint = CyanAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Kamerazugriff erforderlich",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateLight100,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ORBIT Camera benötigt Zugriff auf deine Kamera, um geführte Bildserien aus 30 bis 100 Perspektiven für die künstlerische Bildfusion aufzunehmen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateLight100.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = SlateDark950
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_request_camera_permission")
                ) {
                    Text(
                        text = "Kamera aktivieren",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
