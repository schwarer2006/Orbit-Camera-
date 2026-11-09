package com.example.ui.studio

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.model.OrbitVariant
import com.example.BuildConfig
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.engine.BitmapUtils
import com.example.model.OrbitArtStyle
import com.example.model.OrbitBlendMode
import com.example.model.OrbitCompositionStrategy
import com.example.model.OrbitExportRatio
import com.example.model.OrbitExportSize
import com.example.model.OrbitProject
import com.example.model.OrbitWorkspaceMode
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.SlateDark950
import com.example.ui.theme.SlateLight100
import com.example.ui.theme.SlateWhite
import kotlin.math.roundToInt

@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    onNavigateToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var showAdvancedParams by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is StudioEffect.ExportSuccess -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is StudioEffect.ShareImage -> {
                    BitmapUtils.shareImage(context, effect.uri, effect.title)
                }
                is StudioEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.text)
                }
            }
        }
    }

    // Performance & Battery optimization: Pause background studio rendering when app is backgrounded or screen disposed
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.pauseStudioRendering()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pauseStudioRendering()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SlateDark950,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        val active = uiState.activeProject

        if (active == null || active.actualCount == 0) {
            StudioEmptyState(
                onNavigateToCamera = onNavigateToCamera,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            // Header / Project Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = active.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SlateWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${active.activeCount}/${active.actualCount} Aufnahmen • ${uiState.selectedArtStyle.title} • ORBIT V${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanAccent
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Performance & Akku-Diagnostik Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleDiagnostics() },
                        modifier = Modifier
                            .background(if (uiState.isDiagnosticsVisible) CyanAccent.copy(alpha = 0.25f) else SlateDark800, CircleShape)
                            .testTag("btn_diagnostics_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Performance & Akku-Diagnose",
                            tint = if (uiState.isDiagnosticsVisible) CyanNeon else SlateLight100
                        )
                    }

                    // About Dialog Button
                    IconButton(
                        onClick = { viewModel.openAboutDialog() },
                        modifier = Modifier
                            .background(SlateDark800, CircleShape)
                            .testTag("btn_about_orbit")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Über ORBIT",
                            tint = CyanAccent
                        )
                    }

                    // Export Button (Qualitäts-Export)
                    IconButton(
                        onClick = { viewModel.openExportDialog() },
                        enabled = !uiState.isRendering && !uiState.isExporting,
                        modifier = Modifier
                            .background(CyanAccent.copy(alpha = 0.2f), CircleShape)
                            .testTag("btn_top_export")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Qualitäts-Export",
                            tint = CyanNeon
                        )
                    }

                    // Recompute Button
                    IconButton(
                        onClick = { viewModel.recompute() },
                        enabled = !uiState.isRendering && !uiState.isExporting,
                        modifier = Modifier
                            .background(SlateDark800, CircleShape)
                            .testTag("btn_recompute_fusion")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Neu berechnen",
                            tint = if (uiState.isRendering) SlateDark600 else CyanAccent
                        )
                    }
                }
            }

            // Optional Performance & Battery Diagnostics Display
            AnimatedVisibility(
                visible = uiState.isDiagnosticsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("card_performance_diagnostics"),
                    colors = CardDefaults.cardColors(containerColor = SlateDark900),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ORBIT Performance & Akku-Diagnose",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                            Text(
                                text = "Lokal / On-Device",
                                fontSize = 10.sp,
                                color = SlateLight100.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val diag = uiState.diagnostics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Vorschau-Dauer", fontSize = 10.sp, color = SlateLight100.copy(alpha = 0.7f))
                                Text("${diag.lastRenderMs} ms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateWhite)
                            }
                            Column {
                                Text("Preview LRU Cache", fontSize = 10.sp, color = SlateLight100.copy(alpha = 0.7f))
                                Text("${diag.cacheHitRatePercent}% (${diag.cacheHits} Hits)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateWhite)
                            }
                            Column {
                                Text("Heap RAM", fontSize = 10.sp, color = SlateLight100.copy(alpha = 0.7f))
                                Text("${diag.usedHeapMb} / ${diag.maxHeapMb} MB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateWhite)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Render-Jobs", fontSize = 10.sp, color = SlateLight100.copy(alpha = 0.7f))
                                Text("${diag.jobsStarted} gestartet · ${diag.jobsCanceled} abgebr.", fontSize = 11.sp, color = SlateLight100)
                            }
                            Column {
                                Text("Aufnahme / Registrierung", fontSize = 10.sp, color = SlateLight100.copy(alpha = 0.7f))
                                Text("${diag.lastCaptureMs} ms · ${diag.lastRegistrationMs} ms", fontSize = 11.sp, color = SlateLight100)
                            }
                        }
                    }
                }
            }

            // Big Artwork Preview Viewport
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateDark900)
                    .border(1.dp, SlateDark700, RoundedCornerShape(16.dp))
                    .pointerInput(uiState.compositionStrategy) {
                        detectTapGestures { offset ->
                            if (uiState.compositionStrategy == OrbitCompositionStrategy.SUBJECT_CENTERED) {
                                val ax = (offset.x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                                val ay = (offset.y / size.height.toFloat()).coerceIn(0.05f, 0.95f)
                                viewModel.setSubjectAnchor(ax, ay)
                            }
                        }
                    }
                    .testTag("studio_preview_box"),
                contentAlignment = Alignment.Center
            ) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight
                val bitmapToDisplay = if (uiState.showOriginalComparison && uiState.referenceBitmap != null) {
                    uiState.referenceBitmap
                } else {
                    uiState.renderedBitmap ?: uiState.referenceBitmap
                }

                if (bitmapToDisplay != null) {
                    Image(
                        bitmap = bitmapToDisplay.asImageBitmap(),
                        contentDescription = "ORBIT Kunstwerk Vorschau",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = CyanAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Initialisiere Fusion...",
                            color = SlateLight100,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Interactive Anchor Reticle in SUBJECT_CENTERED mode
                if (uiState.compositionStrategy == OrbitCompositionStrategy.SUBJECT_CENTERED) {
                    val reticleX = boxWidth * uiState.anchorX - 16.dp
                    val reticleY = boxHeight * uiState.anchorY - 16.dp

                    // Optional selective mask outline
                    if (uiState.maskEnabled) {
                        val maskPixelRadius = boxWidth * (uiState.maskRadius / 100f) * 0.5f
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(
                                    x = boxWidth * uiState.anchorX - maskPixelRadius,
                                    y = boxHeight * uiState.anchorY - maskPixelRadius
                                )
                                .size(maskPixelRadius * 2)
                                .border(1.dp, CyanAccent.copy(alpha = 0.45f), CircleShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = reticleX, y = reticleY)
                            .size(32.dp)
                            .border(1.5.dp, CyanNeon, CircleShape)
                            .background(CyanNeon.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(CyanNeon, CircleShape)
                        )
                    }

                    // Anchor coordinate badge at top start
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SlateDark950.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Anker: ${(uiState.anchorX * 100).roundToInt()}% , ${(uiState.anchorY * 100).roundToInt()}% • Tippen zum Versetzen",
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Rendering Progress Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isRendering,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            val progress = uiState.renderProgress
                            val current = progress?.current ?: 0
                            val total = progress?.total ?: active.actualCount
                            val fraction = if (total > 0) current.toFloat() / total else 0f

                            CircularProgressIndicator(
                                progress = { fraction },
                                color = CyanNeon,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = progress?.stage ?: "Berechne Stil-Fusion...",
                                color = SlateWhite,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$current / $total Bilder",
                                color = CyanAccent,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Comparison badge / Press-and-hold toggle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateDark950.copy(alpha = 0.8f))
                        .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.toggleComparison(true)
                                    tryAwaitRelease()
                                    viewModel.toggleComparison(false)
                                }
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_compare_original")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Vergleich",
                            tint = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (uiState.showOriginalComparison) "Original Referenz" else "Halten für Original",
                            color = SlateWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. WORKSPACE TIER SELECTOR (Einfach | Kreativ | Experimentell)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateDark900)
                    .border(1.dp, SlateDark800, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OrbitWorkspaceMode.values().forEach { mode ->
                    val isSelected = uiState.workspaceMode == mode
                    val icon = when (mode) {
                        OrbitWorkspaceMode.SIMPLE -> Icons.Default.Tune
                        OrbitWorkspaceMode.CREATIVE -> Icons.Default.Brush
                        OrbitWorkspaceMode.EXPERIMENTAL -> Icons.Default.Science
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SlateDark800 else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) CyanNeon else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setWorkspaceMode(mode) }
                            .padding(vertical = 8.dp)
                            .testTag("workspace_tab_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = mode.title,
                                tint = if (isSelected) CyanNeon else SlateLight100.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) SlateWhite else SlateLight100.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. SURPRISE MIX & UNDO BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.surpriseMix() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = SlateDark950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_surprise_mix")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Überrasch mich",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Überrasch mich",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.undoSurpriseMix() },
                    enabled = uiState.canUndo,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (uiState.canUndo) AmberAccent else SlateDark600
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (uiState.canUndo) AmberAccent.copy(alpha = 0.7f) else SlateDark800
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("btn_undo_mix")
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Rückgängig",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rückgängig",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. KOMPOSITIONSSTRATEGIE (Motivzentriert vs Flächenorientiert)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Kompositionsstrategie",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SlateWhite
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Motivzentriert
                    val isSubjectCentered = (uiState.compositionStrategy == OrbitCompositionStrategy.SUBJECT_CENTERED)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSubjectCentered) SlateDark800 else SlateDark900)
                            .border(
                                width = if (isSubjectCentered) 1.5.dp else 1.dp,
                                color = if (isSubjectCentered) CyanNeon else SlateDark800,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.setCompositionStrategy(OrbitCompositionStrategy.SUBJECT_CENTERED) }
                            .padding(10.dp)
                            .testTag("strategy_subject_centered")
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = "Motivzentriert",
                                    tint = if (isSubjectCentered) CyanNeon else SlateLight100.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = OrbitCompositionStrategy.SUBJECT_CENTERED.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSubjectCentered) SlateWhite else SlateLight100.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Zentraler Motivanker • Solitäre",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSubjectCentered) CyanAccent else SlateLight100.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Flächenorientiert
                    val isStructural = (uiState.compositionStrategy == OrbitCompositionStrategy.STRUCTURAL_FIELD)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isStructural) SlateDark800 else SlateDark900)
                            .border(
                                width = if (isStructural) 1.5.dp else 1.dp,
                                color = if (isStructural) CyanNeon else SlateDark800,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.setCompositionStrategy(OrbitCompositionStrategy.STRUCTURAL_FIELD) }
                            .padding(10.dp)
                            .testTag("strategy_structural_field")
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Flächenorientiert",
                                    tint = if (isStructural) CyanNeon else SlateLight100.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = OrbitCompositionStrategy.STRUCTURAL_FIELD.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isStructural) SlateWhite else SlateLight100.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Flächenfeld-Synthese • Wälder",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isStructural) CyanAccent else SlateLight100.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. HORIZONTAL ART STYLE PRESET SELECTOR
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Künstlerischer Bildstil",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SlateWhite
                    )
                    Text(
                        text = uiState.selectedArtStyle.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("styles_horizontal_picker")
                ) {
                    items(OrbitArtStyle.values()) { style ->
                        val isSelected = uiState.selectedArtStyle == style
                        ArtStyleCard(
                            style = style,
                            isSelected = isSelected,
                            onClick = { viewModel.setArtStyle(style) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. PARAMETER-BEDIENPANEL (Abhängig vom Workspace-Modus: Einfach / Kreativ / Experimentell)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (uiState.workspaceMode) {
                                OrbitWorkspaceMode.SIMPLE -> "Einfache Steuerung"
                                OrbitWorkspaceMode.CREATIVE -> "Kreative Bildparameter"
                                OrbitWorkspaceMode.EXPERIMENTAL -> "Experimentelles Laboratorium"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlateWhite
                        )
                        Text(
                            text = "${uiState.workspaceMode.title}-Modus",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // 1. Abstraktion (in all 3 modes)
                    PrimaryControlSlider(
                        label = "1. Abstraktion",
                        value = uiState.styleParameters.abstraction,
                        onValueChange = { viewModel.setAbstraction(it) },
                        description = when {
                            uiState.styleParameters.abstraction < 30f -> "Motiv dominant • Dezente Perspektivkonturen"
                            uiState.styleParameters.abstraction < 70f -> "Künstlerische Synthese • Harmonische Raumtiefe"
                            else -> "Volle Abstraktion • Kinetische Dispersion"
                        },
                        tag = "slider_abstraction"
                    )

                    // 2. Farbintensität & Leuchtkraft (in all 3 modes)
                    PrimaryControlSlider(
                        label = "2. Farbintensität & Leuchtkraft",
                        value = uiState.styleParameters.colorIntensity,
                        onValueChange = { viewModel.setColorIntensity(it) },
                        description = when {
                            uiState.styleParameters.colorIntensity < 40f -> "Natürlich gedämpfte Tonalität"
                            uiState.styleParameters.colorIntensity < 75f -> "Lebendige, satte Bildfarben"
                            else -> "Expressive Leuchtkraft • Maximal leuchtend"
                        },
                        tag = "slider_color_intensity"
                    )

                    // ADDITIONAL CONTROLS FOR KREATIV & EXPERIMENTELL
                    if (uiState.workspaceMode != OrbitWorkspaceMode.SIMPLE) {
                        // 3. Motivstabilität
                        PrimaryControlSlider(
                            label = "3. Motivstabilität",
                            value = uiState.styleParameters.subjectStability,
                            onValueChange = { viewModel.setSubjectStability(it) },
                            description = when {
                                uiState.styleParameters.subjectStability < 40f -> "Freie Perspektiven-Dispersion"
                                uiState.styleParameters.subjectStability < 75f -> "Balancierte Verankerung"
                                else -> "Dominanter Motivfokus • Feste Zentrierung"
                            },
                            tag = "slider_subject_stability"
                        )

                        // 4. Hintergrundauflösung
                        PrimaryControlSlider(
                            label = "4. Hintergrundauflösung",
                            value = uiState.styleParameters.backgroundDissolve,
                            onValueChange = { viewModel.setBackgroundDissolve(it) },
                            description = when {
                                uiState.styleParameters.backgroundDissolve < 35f -> "Hintergrunddetails erkennbar"
                                uiState.styleParameters.backgroundDissolve < 70f -> "Harmonisch aufgelöster Dunst"
                                else -> "Malerischer Schleier • Raumtiefe freigestellt"
                            },
                            tag = "slider_background_dissolve"
                        )

                        // 5. Echo-Konturenstärke
                        PrimaryControlSlider(
                            label = "5. Echo-Konturenstärke (Ventosa Look)",
                            value = uiState.styleParameters.echoStrength,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(echoStrength = it)) },
                            description = when {
                                uiState.styleParameters.echoStrength < 30f -> "Dezente Konturlinien"
                                uiState.styleParameters.echoStrength < 70f -> "Prägnante Mehrfachbelichtungskanten"
                                else -> "Starke gestaffelte Konturschichten"
                            },
                            tag = "slider_echo_strength"
                        )

                        // 6. Vibrance / Farbfrische
                        PrimaryControlSlider(
                            label = "6. Vibrance (Farbfrische ohne Clipping)",
                            value = uiState.styleParameters.vibrance,
                            onValueChange = { viewModel.setVibrance(it) },
                            description = "Hebt selektiv gedeckte Farbtöne an, ohne bereits bunte Bildbereiche zu übersättigen.",
                            tag = "slider_vibrance"
                        )

                        // 7. Kontrast
                        PrimaryControlSlider(
                            label = "7. Kontrast & Tonalität (S-Kurve)",
                            value = uiState.styleParameters.contrast,
                            onValueChange = { viewModel.setContrast(it) },
                            description = when {
                                uiState.styleParameters.contrast < 45f -> "Weichere Tonalität • Flache Schatten"
                                uiState.styleParameters.contrast < 65f -> "Ausgewogene fotografische Gradation"
                                else -> "Tiefe Schwärzen & leuchtende Lichter"
                            },
                            tag = "slider_contrast"
                        )

                        // 8. Farbtemperatur (Warmth)
                        BipolarWarmthSlider(
                            value = uiState.styleParameters.warmth,
                            onValueChange = { viewModel.setWarmth(it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Referenzbild Carousel & Image Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Aufnahmen & Referenzbild",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = SlateWhite
                                )
                                Text(
                                    text = "${active.activeCount} von ${active.actualCount} aktiv • Ref: Bild ${uiState.selectedRefIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(active.imagePaths) { index, path ->
                                val isSelected = index == uiState.selectedRefIndex
                                val isDisabled = path in uiState.disabledImagePaths
                                val quality = uiState.imageQualityReports[path]

                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) CyanNeon else if (isDisabled) SlateDark800 else SlateDark700,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(if (isDisabled) Color.Black else SlateDark900)
                                        .clickable { viewModel.selectReferenceIndex(index) }
                                        .testTag("ref_thumb_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = path,
                                        contentDescription = "Perspektive $index",
                                        modifier = Modifier.fillMaxSize(),
                                        alpha = if (isDisabled) 0.35f else 1f,
                                        contentScale = ContentScale.Crop
                                    )

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .size(14.dp)
                                                .background(CyanNeon, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Ausgewählt",
                                                tint = SlateDark950,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }

                                    // Non-destructive Enable/Disable Toggle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(2.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (isDisabled) Color.Red.copy(alpha = 0.85f) else SlateDark950.copy(alpha = 0.75f))
                                            .clickable { viewModel.toggleImageDisabled(path) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDisabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isDisabled) "Aktivieren" else "Deaktivieren",
                                            tint = if (isDisabled) Color.White else CyanAccent,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }

                                    // Quality advisory badge
                                    if (quality != null && (quality.isBlurry || quality.isExposureOutlier || quality.isDuplicate)) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(2.dp)
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(AmberAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (quality.isBlurry) "U" else if (quality.isExposureOutlier) "B" else "D",
                                                color = SlateDark950,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ADDITIONAL CONTROLS SPECIFIC TO EXPERIMENTELL
                    if (uiState.workspaceMode == OrbitWorkspaceMode.EXPERIMENTAL) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Geometrische & Spektrale Feineinstellung",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanNeon
                        )

                        ParameterSlider(
                            label = "Räumlicher Perspektiv-Versatz (Perspective Spread)",
                            value = uiState.styleParameters.perspectiveSpread,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(perspectiveSpread = it)) }
                        )

                        ParameterSlider(
                            label = "Konturprägung (Unsharp Line Masking)",
                            value = uiState.styleParameters.edgeEmphasis,
                            onValueChange = { viewModel.setEdgeEmphasis(it) }
                        )

                        ParameterSlider(
                            label = "Farbdichte & Sättigungsfülle",
                            value = uiState.styleParameters.colorDensity,
                            onValueChange = { viewModel.setColorDensity(it) }
                        )

                        ParameterSlider(
                            label = "Anzahl sichtbarer Perspektiv-Echos (${uiState.styleParameters.echoCount.roundToInt()})",
                            value = (uiState.styleParameters.echoCount - 3f) / 9f * 100f,
                            onValueChange = {
                                val count = 3f + (it / 100f) * 9f
                                viewModel.updateStyleParameters(uiState.styleParameters.copy(echoCount = count))
                            }
                        )

                        ParameterSlider(
                            label = "Glanz & Leuchtschleier (Glow)",
                            value = uiState.styleParameters.lightGlow,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(lightGlow = it)) }
                        )

                        ParameterSlider(
                            label = "Schattentiefe & Mikrokontrast",
                            value = uiState.styleParameters.shadowDepth,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(shadowDepth = it)) }
                        )

                        ParameterSlider(
                            label = "Malerische Weichheit",
                            value = uiState.styleParameters.softness,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(softness = it)) }
                        )

                        ParameterSlider(
                            label = "Bewegungsdynamik (Trails)",
                            value = uiState.styleParameters.motionStrength,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(motionStrength = it)) }
                        )

                        ParameterSlider(
                            label = "Detail- & Texturdichte",
                            value = uiState.styleParameters.detailDensity,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(detailDensity = it)) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2D Bildregistrierung & Ausrichtung
                        Text(
                            text = "Authentische 2D-Bildregistrierung (Sobel Edge NCC)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlateWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Robuste Kanten-Kreuzkorrelation gegen die ausgewählte Referenzaufnahme",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.runAutoAlignment() },
                                enabled = !uiState.isAutoAligning && !uiState.isRendering,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.autoAlignApplied) CyanNeon else CyanAccent,
                                    contentColor = SlateDark950
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_auto_align")
                            ) {
                                if (uiState.isAutoAligning) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SlateDark950)
                                } else {
                                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.autoAlignApplied) "Sobel-Ausrichtung aktiv" else "Sobel-Registrierung berechnen",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetAlignment() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateLight100),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_reset_align")
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", fontSize = 11.sp)
                            }
                        }

                        // Registration report summary if available
                        val regReport = uiState.registrationReport
                        if (regReport != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            RegistrationReportCard(report = regReport)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Perspektiv-Staffelung (Natürlich vs Algorithmisch)
                        PerspectiveSpreadControl(
                            artisticSpreadEnabled = uiState.artisticSpreadEnabled,
                            onToggleArtisticSpread = { viewModel.toggleArtisticSpread(it) },
                            spreadValue = uiState.styleParameters.perspectiveSpread,
                            onSpreadChange = { viewModel.setPerspectiveSpread(it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fusions-Blend-Modi
                        BlendModeSelector(
                            selectedMode = uiState.blendMode,
                            onSelectMode = { viewModel.setBlendMode(it) }
                        )

                        // Selektive Motivmaske (wenn Motivzentriert)
                        if (uiState.compositionStrategy == OrbitCompositionStrategy.SUBJECT_CENTERED) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SelectiveMaskControl(
                                maskEnabled = uiState.maskEnabled,
                                onToggleMask = { viewModel.setSelectiveMask(it) },
                                radius = uiState.maskRadius,
                                feather = uiState.maskFeather,
                                intensity = uiState.maskIntensity,
                                onRadiusChange = { viewModel.setSelectiveMask(uiState.maskEnabled, radius = it) },
                                onFeatherChange = { viewModel.setSelectiveMask(uiState.maskEnabled, feather = it) },
                                onIntensityChange = { viewModel.setSelectiveMask(uiState.maskEnabled, intensity = it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Manuelle Verschiebung X: ${(uiState.offsetX * 10).roundToInt() / 10f} px",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateLight100.copy(alpha = 0.8f)
                        )
                        Slider(
                            value = uiState.offsetX,
                            onValueChange = { viewModel.setOffset(it, uiState.offsetY) },
                            valueRange = -20f..20f,
                            colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Manuelle Verschiebung Y: ${(uiState.offsetY * 10).roundToInt() / 10f} px",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateLight100.copy(alpha = 0.8f)
                        )
                        Slider(
                            value = uiState.offsetY,
                            onValueChange = { viewModel.setOffset(uiState.offsetX, it) },
                            valueRange = -20f..20f,
                            colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. EINZELBILDER & EBENEN (V1.1 Creative Fusion Expansion)
            LayerAndVariantPanel(
                uiState = uiState,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Export Actions Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.openExportDialog() },
                    enabled = !uiState.isRendering && !uiState.isExporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = SlateDark950
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_export_gallery")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Exportieren",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Qualitäts-Export (1080p – 4096p)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // If exported, show share button
                if (uiState.exportedUri != null) {
                    OutlinedButton(
                        onClick = {
                            BitmapUtils.shareImage(context, uiState.exportedUri!!, "ORBIT Kunstwerk")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_share_artwork")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Teilen",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Zuletzt exportiertes Bild teilen", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // V0.5 Simple Quality Export Dialog
        if (uiState.isExportDialogOpen) {
            OrbitExportDialog(
                uiState = uiState,
                onDismiss = { viewModel.closeExportDialog() },
                onSelectSize = { viewModel.setExportSize(it) },
                onSelectRatio = { viewModel.setExportRatio(it) },
                onSelectQuality = { viewModel.setExportQuality(it) },
                onSave = { viewModel.startExport(triggerShare = false) },
                onShare = { viewModel.startExport(triggerShare = true) }
            )
        }

        // V0.9.0 Über ORBIT Dialog
        if (uiState.isAboutDialogOpen) {
            OrbitAboutDialog(
                onDismiss = { viewModel.closeAboutDialog() }
            )
        }

        // V1.1 Variante speichern Dialog
        if (uiState.isSaveVariantDialogOpen) {
            OrbitSaveVariantDialog(
                onDismiss = { viewModel.closeSaveVariantDialog() },
                onSave = { viewModel.saveCurrentVariant(it) }
            )
        }
    }
}

@Composable
fun PrimaryControlSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    description: String,
    tag: String
) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SlateWhite
            )
            Text(
                text = "${value.roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = CyanAccent
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = SlateLight100.copy(alpha = 0.65f)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = CyanNeon,
                activeTrackColor = CyanAccent,
                inactiveTrackColor = SlateDark700
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
    }
}

@Composable
fun ArtStyleCard(
    style: OrbitArtStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (style) {
        OrbitArtStyle.IMPRESSIONIST -> Icons.Default.Brush
        OrbitArtStyle.IN_THE_ROUND -> Icons.Default.Sync
        OrbitArtStyle.ARCHITECTURAL -> Icons.Default.CenterFocusStrong
        OrbitArtStyle.ETHEREAL -> Icons.Default.Nightlight
        OrbitArtStyle.MOTION_FLOW -> Icons.Default.MotionPhotosOn
        OrbitArtStyle.URBAN_LAYERS -> Icons.Default.LocationCity
    }

    Box(
        modifier = Modifier
            .width(148.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SlateDark800 else SlateDark900)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) CyanNeon else SlateDark700,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
            .testTag("style_card_${style.name.lowercase()}"),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = style.title,
                    tint = if (isSelected) CyanNeon else SlateLight100.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(CyanNeon, CircleShape)
                    )
                }
            }

            Column {
                Text(
                    text = style.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) SlateWhite else SlateLight100,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = style.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) CyanAccent else SlateLight100.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BipolarWarmthSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val rounded = value.roundToInt()
    val statusText = when {
        rounded < -10 -> "$rounded (Kühl/Blau)"
        rounded > 10 -> "+$rounded (Warm/Gold)"
        else -> "0 (Neutral)"
    }
    val valueColor = when {
        rounded < -10 -> CyanNeon
        rounded > 10 -> AmberAccent
        else -> SlateWhite
    }

    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "8. Farbtemperatur (Kühl .. Neutral .. Warm)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SlateWhite
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
        }
        Text(
            text = "Steuert fotografische Wärme von kühlem Dämmerungsblau (-100) bis sonnigem Bernstein-Gold (+100).",
            style = MaterialTheme.typography.labelSmall,
            color = SlateLight100.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -100f..100f,
            colors = SliderDefaults.colors(
                thumbColor = valueColor,
                activeTrackColor = if (rounded >= 0) AmberAccent else CyanNeon,
                inactiveTrackColor = SlateDark700
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_warmth")
        )
    }
}

@Composable
fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SlateLight100.copy(alpha = 0.85f)
            )
            Text(
                text = "${value.roundToInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CyanAccent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent,
                inactiveTrackColor = SlateDark700
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StudioEmptyState(
    onNavigateToCamera: () -> Unit,
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
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Studio",
                        tint = CyanAccent,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Keine Aufnahmeserie gewählt",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Nimm mit der ORBIT Kamera eine Bildserie auf (30–100 Fotos) oder importiere vorhandene Bilder aus deiner Galerie im Reiter 'Projekte'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateLight100.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNavigateToCamera,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = SlateDark950
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_empty_to_camera")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Kamera"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zur Kamera",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OrbitExportDialog(
    uiState: StudioUiState,
    onDismiss: () -> Unit,
    onSelectSize: (OrbitExportSize) -> Unit,
    onSelectRatio: (OrbitExportRatio) -> Unit,
    onSelectQuality: (Int) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val size = uiState.selectedExportSize
    val ratio = uiState.selectedExportRatio
    val quality = uiState.selectedExportQuality
    val (targetW, targetH) = ratio.calculateDimensions(size.longEdge)

    Dialog(
        onDismissRequest = {
            if (!uiState.isExporting) onDismiss()
        }
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDark900),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark700),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("dialog_orbit_export")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(CyanAccent.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ORBIT Qualitäts-Export",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlateWhite
                        )
                        Text(
                            text = "Neu gerendert aus den Originalaufnahmen",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Bildgröße
                Text(
                    text = "1. Bildgröße (Lange Kante)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlateLight100
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OrbitExportSize.values().forEach { expSize ->
                        val isSelected = size == expSize
                        val (calcW, calcH) = ratio.calculateDimensions(expSize.longEdge)

                        Surface(
                            onClick = { if (!uiState.isExporting) onSelectSize(expSize) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else SlateDark800,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) CyanNeon else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chip_size_${expSize.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = expSize.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) CyanNeon else SlateWhite,
                                            fontSize = 13.sp
                                        )
                                        if (expSize == OrbitExportSize.HIGH_2048) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(CyanNeon)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("Standard", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateDark950)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${calcW} × ${calcH} px • ${expSize.description}",
                                        fontSize = 11.sp,
                                        color = if (isSelected) SlateLight100 else SlateDark600
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Seitenverhältnis
                Text(
                    text = "2. Seitenverhältnis (Ohne Bildverzerrung)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlateLight100
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrbitExportRatio.values().forEach { expRatio ->
                        val isSelected = ratio == expRatio
                        val (calcW, calcH) = expRatio.calculateDimensions(size.longEdge)

                        Surface(
                            onClick = { if (!uiState.isExporting) onSelectRatio(expRatio) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else SlateDark800,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) CyanNeon else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_ratio_${expRatio.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = expRatio.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) CyanNeon else SlateWhite,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${calcW}×${calcH}",
                                    fontSize = 10.sp,
                                    color = if (isSelected) CyanAccent else SlateDark600,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. JPEG-Qualität
                Text(
                    text = "3. JPEG-Qualität",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlateLight100
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(85, 90, 95).forEach { q ->
                        val isSelected = quality == q
                        Surface(
                            onClick = { if (!uiState.isExporting) onSelectQuality(q) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else SlateDark800,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) CyanNeon else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_quality_$q")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$q %",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) CyanNeon else SlateWhite,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (q == 95) "Beste Qualität" else if (q == 90) "Ausgewogen" else "Kompakt",
                                    fontSize = 9.sp,
                                    color = if (isSelected) CyanAccent else SlateDark600
                                )
                            }
                        }
                    }
                }

                // Progress View during Export
                if (uiState.isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateDark950),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().testTag("export_progress_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val progress = uiState.exportProgress
                            val current = progress?.current ?: 0
                            val total = progress?.total ?: 1
                            val fraction = if (total > 0) current.toFloat() / total else 0f

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = progress?.stage ?: "Berechne $targetW × $targetH px...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = CyanNeon,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(fraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SlateWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { fraction },
                                color = CyanNeon,
                                trackColor = SlateDark800,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Originale werden in voller Detailtiefe überlagert ($current / $total)",
                                fontSize = 10.sp,
                                color = SlateDark600
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Buttons: Abbrechen, Teilen, Speichern
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !uiState.isExporting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateLight100),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                            .testTag("btn_export_dismiss")
                    ) {
                        Text("Abbrechen", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onShare,
                        enabled = !uiState.isExporting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(44.dp)
                            .testTag("btn_export_share")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Teilen", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onSave,
                        enabled = !uiState.isExporting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = SlateDark950
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("btn_export_save")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Speichern", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun OrbitAboutDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDark900),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark700),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("dialog_about_orbit")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CyanAccent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ORBIT Camera",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateWhite
                )

                Text(
                    text = "Computational Art Photography",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanAccent
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SlateDark800)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Status: Entwicklung",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateLight100
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ORBIT Camera erzeugt aus fotografischen Aufnahmeserien neue, eigenständige künstlerische Bildkompositionen durch algorithmische Mehrperspektiven-Fusion und selektive Motivverankerung.\n\n" +
                            "Künstlerische Referenz:\n" +
                            "Der gestalterische Ansatz orientiert sich an der Mehrperspektivenfotografie „In the Round“ von Pep Ventosa.\n" +
                            "(https://www.brianlorenzo.com/pep-ventosa-in-the-round-fotografie-technik/)\n\n" +
                            "ORBIT ist ein eigenständiges Softwareprojekt ohne generative KI und verarbeitet ausschließlich echte Aufnahmen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateLight100.copy(alpha = 0.85f),
                    textAlign = TextAlign.Start,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = SlateDark950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_close_about_dialog")
                ) {
                    Text("Schließen", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BlendModeSelector(
    selectedMode: OrbitBlendMode,
    onSelectMode: (OrbitBlendMode) -> Unit
) {
    Column {
        Text(
            text = "Fusions-Mischmodus (Blend Mode)",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = SlateWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = selectedMode.description,
            style = MaterialTheme.typography.labelSmall,
            color = CyanAccent
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            val modes = listOf(
                OrbitBlendMode.WEIGHTED_AVERAGE,
                OrbitBlendMode.SEQUENTIAL_LAYERS,
                OrbitBlendMode.LIGHTEN,
                OrbitBlendMode.MULTIPLY,
                OrbitBlendMode.SCREEN,
                OrbitBlendMode.DRIFT_FLOW
            )
            items(modes) { mode ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SlateDark800 else SlateDark900)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) CyanNeon else SlateDark700,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectMode(mode) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("blend_mode_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) SlateWhite else SlateLight100.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun PerspectiveSpreadControl(
    artisticSpreadEnabled: Boolean,
    onToggleArtisticSpread: (Boolean) -> Unit,
    spreadValue: Float,
    onSpreadChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "Perspektiv-Staffelung",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = SlateWhite
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (artisticSpreadEnabled) "Künstlerische Auffächerung (Algorithmisch)" else "Natürliche Perspektive (Echte Kameraverschiebung)",
            style = MaterialTheme.typography.labelSmall,
            color = if (artisticSpreadEnabled) AmberAccent else CyanAccent
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!artisticSpreadEnabled) SlateDark800 else SlateDark900)
                    .border(
                        width = if (!artisticSpreadEnabled) 1.5.dp else 1.dp,
                        color = if (!artisticSpreadEnabled) CyanNeon else SlateDark800,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleArtisticSpread(false) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Natürlich",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (!artisticSpreadEnabled) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (!artisticSpreadEnabled) SlateWhite else SlateLight100.copy(alpha = 0.6f)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (artisticSpreadEnabled) SlateDark800 else SlateDark900)
                    .border(
                        width = if (artisticSpreadEnabled) 1.5.dp else 1.dp,
                        color = if (artisticSpreadEnabled) AmberAccent else SlateDark800,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleArtisticSpread(true) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Künstlerisch gestaffelt",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (artisticSpreadEnabled) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (artisticSpreadEnabled) SlateWhite else SlateLight100.copy(alpha = 0.6f)
                )
            }
        }

        if (artisticSpreadEnabled) {
            Spacer(modifier = Modifier.height(6.dp))
            ParameterSlider(
                label = "Auffächerungsstärke (Perspective Spread)",
                value = spreadValue,
                onValueChange = onSpreadChange
            )
        }
    }
}

@Composable
fun SelectiveMaskControl(
    maskEnabled: Boolean,
    onToggleMask: (Boolean) -> Unit,
    radius: Float,
    feather: Float,
    intensity: Float,
    onRadiusChange: (Float) -> Unit,
    onFeatherChange: (Float) -> Unit,
    onIntensityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateDark900)
            .border(1.dp, if (maskEnabled) CyanNeon.copy(alpha = 0.6f) else SlateDark800, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Selektive Motivmaske (Mask)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlateWhite
                )
                Text(
                    text = "Weiche Zentrierung um den Motivanker",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent
                )
            }
            androidx.compose.material3.Switch(
                checked = maskEnabled,
                onCheckedChange = onToggleMask,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = CyanNeon,
                    checkedTrackColor = CyanAccent.copy(alpha = 0.4f)
                )
            )
        }

        if (maskEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            ParameterSlider(
                label = "Maskenradius: ${radius.roundToInt()}%",
                value = radius,
                onValueChange = onRadiusChange
            )
            ParameterSlider(
                label = "Weiche Kante (Feather): ${feather.roundToInt()}%",
                value = feather,
                onValueChange = onFeatherChange
            )
            ParameterSlider(
                label = "Motiv-Dominanz: ${intensity.roundToInt()}%",
                value = intensity,
                onValueChange = onIntensityChange
            )
        }
    }
}

@Composable
fun RegistrationReportCard(
    report: com.example.engine.RegistrationReport
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark800),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark700),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sobel Edge NCC Registrierungsbericht",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = CyanNeon
                )
                Text(
                    text = "Ø ${(report.averageConfidence * 100).roundToInt()}% Konfidenz",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (report.averageConfidence >= 0.4f) CyanAccent else AmberAccent
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "• ${report.successfulCount}/${report.totalImages} Bilder zuverlässig ausgerichtet",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateWhite
                )
                if (report.lowConfidenceCount > 0) {
                    Text(
                        text = "• ${report.lowConfidenceCount} mit geringer Korrelation",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberAccent
                    )
                }
            }
        }
    }
}

@Composable
fun LayerAndVariantPanel(
    uiState: StudioUiState,
    viewModel: StudioViewModel
) {
    val project = uiState.activeProject ?: return
    val orderedPaths = uiState.effectiveImageOrder
    val selectedPath = uiState.selectedImagePath ?: orderedPaths.firstOrNull()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark900),
        border = BorderStroke(1.dp, SlateDark700),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("panel_layers_variants")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Einzelbilder & Ebenen",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SlateWhite
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SlateDark800)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${project.activeCount}/${project.actualCount} aktiv",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Individuelle Gewichtung, Deckkraft, Reihenfolge und künstlerische Varianten.",
                style = MaterialTheme.typography.bodySmall,
                color = SlateLight100.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Exposure Matching Toggle
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateDark800),
                border = BorderStroke(1.dp, if (uiState.exposureMatchEnabled) CyanAccent.copy(alpha = 0.5f) else SlateDark700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Exposure,
                            contentDescription = null,
                            tint = if (uiState.exposureMatchEnabled) CyanNeon else SlateLight100.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Belichtung der Serie angleichen",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SlateWhite
                            )
                            Text(
                                text = "Gleicht Tonwertschwankungen sanft an das Referenzbild an",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateLight100.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Switch(
                        checked = uiState.exposureMatchEnabled,
                        onCheckedChange = { viewModel.toggleExposureMatch(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanAccent.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("switch_exposure_match")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Artistic Variants Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Künstlerische Varianten (${uiState.variants.size})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = SlateWhite
                    )
                }

                Button(
                    onClick = { viewModel.openSaveVariantDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateDark800,
                        contentColor = CyanAccent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_save_variant_open")
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Neu speichern", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.variants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(uiState.variants) { variant ->
                        val isActive = variant.id == uiState.activeVariantId
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isActive) SlateDark700 else SlateDark800),
                            border = BorderStroke(1.dp, if (isActive) CyanNeon else SlateDark700),
                            modifier = Modifier
                                .clickable { viewModel.applyVariant(variant.id) }
                                .testTag("variant_item_${variant.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Column {
                                    Text(
                                        text = variant.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isActive) SlateWhite else SlateLight100
                                    )
                                    Text(
                                        text = "${variant.artStyle.title} · ${variant.blendMode.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateLight100.copy(alpha = 0.6f),
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.duplicateVariant(variant.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Duplizieren",
                                        tint = SlateLight100.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteVariant(variant.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Löschen",
                                        tint = SlateLight100.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Filmstrip of Ordered Layers
            Text(
                text = "Ebenen-Reihenfolge (Tippen zur Bearbeitung)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SlateWhite
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("layers_filmstrip")
            ) {
                itemsIndexed(orderedPaths) { idx, path ->
                    val isSelected = (path == selectedPath)
                    val isRef = (idx == uiState.selectedRefIndex || path == project.imagePaths.getOrNull(uiState.selectedRefIndex))
                    val isDisabled = path in uiState.disabledImagePaths
                    val weight = uiState.imageWeights[path] ?: 1.0f
                    val opacity = uiState.layerOpacities[path] ?: (1.0f / (idx + 1f).coerceAtLeast(1f))

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateDark800),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> CyanNeon
                                isRef -> AmberAccent
                                else -> SlateDark700
                            }
                        ),
                        modifier = Modifier
                            .width(88.dp)
                            .clickable { viewModel.selectImage(path) }
                            .testTag("layer_thumb_$idx")
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SlateDark950),
                                contentAlignment = Alignment.Center
                            ) {
                                val thumbBmp = remember(path) {
                                    BitmapUtils.decodeSampledBitmap(path, 120, 120)
                                }
                                if (thumbBmp != null) {
                                    Image(
                                        bitmap = thumbBmp.asImageBitmap(),
                                        contentDescription = "Ebene ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().alpha(if (isDisabled) 0.35f else 1.0f)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = SlateLight100.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                if (isRef) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AmberAccent)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("REF", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SlateDark950)
                                    }
                                }

                                if (isDisabled) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .clip(CircleShape)
                                            .background(SlateDark950.copy(alpha = 0.8f))
                                            .padding(4.dp)
                                    ) {
                                        Icon(Icons.Default.VisibilityOff, contentDescription = "Deaktiviert", tint = AmberAccent, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(SlateDark950.copy(alpha = 0.75f))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text("${idx + 1}", fontSize = 9.sp, color = SlateWhite)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "G:${(weight * 100).roundToInt()}%",
                                    fontSize = 9.sp,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "D:${(opacity * 100).roundToInt()}%",
                                    fontSize = 9.sp,
                                    color = SlateLight100.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Selected Layer Inspector
            if (selectedPath != null) {
                val selectedIdx = orderedPaths.indexOf(selectedPath).coerceAtLeast(0)
                val isSelectedRef = (selectedIdx == uiState.selectedRefIndex || selectedPath == project.imagePaths.getOrNull(uiState.selectedRefIndex))
                val isSelectedDisabled = selectedPath in uiState.disabledImagePaths
                val curWeight = uiState.imageWeights[selectedPath] ?: 1.0f
                val curOpacity = uiState.layerOpacities[selectedPath] ?: (1.0f / (selectedIdx + 1f).coerceAtLeast(1f))

                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateDark800),
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("layer_inspector")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ebene #${selectedIdx + 1} anpassen",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyanNeon
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.moveImageUp(selectedPath) },
                                    enabled = selectedIdx > 0,
                                    modifier = Modifier.size(32.dp).testTag("btn_layer_move_up")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Vor",
                                        tint = if (selectedIdx > 0) CyanAccent else SlateLight100.copy(alpha = 0.2f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.moveImageDown(selectedPath) },
                                    enabled = selectedIdx < orderedPaths.size - 1,
                                    modifier = Modifier.size(32.dp).testTag("btn_layer_move_down")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Zurück",
                                        tint = if (selectedIdx < orderedPaths.size - 1) CyanAccent else SlateLight100.copy(alpha = 0.2f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Weight Slider (0 - 100%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bildgewichtung: ${(curWeight * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateLight100
                            )
                            Text(
                                text = if (curWeight == 0f) "Stumm" else if (curWeight >= 1.5f) "Maximal" else "Normal",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent
                            )
                        }
                        Slider(
                            value = curWeight,
                            onValueChange = { viewModel.setImageWeight(selectedPath, it) },
                            valueRange = 0f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                            modifier = Modifier.fillMaxWidth().testTag("slider_layer_weight")
                        )

                        // Opacity Slider (0 - 100%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Deckkraft (Alpha-Überlagerung): ${(curOpacity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateLight100
                            )
                            Text(
                                text = if (uiState.blendMode == OrbitBlendMode.SEQUENTIAL_LAYERS) "Aktiv im Sequenzmodus" else "Vorbereitet",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.blendMode == OrbitBlendMode.SEQUENTIAL_LAYERS) CyanNeon else SlateLight100.copy(alpha = 0.6f)
                            )
                        }
                        Slider(
                            value = curOpacity,
                            onValueChange = { viewModel.setLayerOpacity(selectedPath, it) },
                            valueRange = 0.05f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon),
                            modifier = Modifier.fillMaxWidth().testTag("slider_layer_opacity")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val origIdx = project.imagePaths.indexOf(selectedPath)
                                    if (origIdx >= 0) viewModel.selectReferenceIndex(origIdx)
                                },
                                enabled = !isSelectedRef,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isSelectedRef) AmberAccent else SlateLight100
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_layer_set_ref")
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSelectedRef) "Referenz ★" else "Als Referenz", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleImageDisabled(selectedPath) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isSelectedDisabled) AmberAccent else SlateLight100
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_layer_toggle_active")
                            ) {
                                Icon(
                                    if (isSelectedDisabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSelectedDisabled) "Aktivieren" else "Ausblenden", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Global Layer Order Reorder Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.reverseImageOrder() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateLight100),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_order_reverse")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Umkehren", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.shuffleImageOrder() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateLight100),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_order_shuffle")
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mischen", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.resetImageOrder() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateLight100),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_order_reset")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aufnahme", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Surprise Mix Layer Variation Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ebenen & Gewichte im Mix variieren",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SlateWhite
                    )
                    Text(
                        text = "Mischt beim Überraschungs-Mix zusätzlich Ebenen und Gewichte",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateLight100.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = uiState.includeLayersInSurpriseMix,
                    onCheckedChange = { viewModel.toggleIncludeLayersInSurpriseMix(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanNeon,
                        checkedTrackColor = CyanAccent.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("switch_surprise_layers")
                )
            }
        }
    }
}

@Composable
fun OrbitSaveVariantDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateDark900),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Bildvariante speichern",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlateWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Speichert die aktuellen Bildparameter, Fusionsstile, Ebenenreihenfolgen und individuellen Gewichte ab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateLight100.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = {
                        Text(
                            text = "Name (z. B. 'Expressiver Kontrast')",
                            color = SlateLight100.copy(alpha = 0.4f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SlateWhite,
                        unfocusedTextColor = SlateWhite,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SlateDark700
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_variant_name")
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateLight100),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Abbrechen")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onSave(name) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SlateDark950),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_confirm_save_variant")
                    ) {
                        Text("Speichern", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

