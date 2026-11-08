package com.example.ui.studio

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

