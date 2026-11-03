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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import coil.compose.AsyncImage
import com.example.model.OrbitArtStyle
import com.example.model.OrbitBlendMode
import com.example.model.OrbitProject
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
                        text = "${active.actualCount} Aufnahmen • Stil: ${uiState.selectedArtStyle.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanAccent
                    )
                }

                // Recompute Button
                IconButton(
                    onClick = { viewModel.recompute() },
                    enabled = !uiState.isRendering,
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

            // Big Artwork Preview Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateDark900)
                    .border(1.dp, SlateDark700, RoundedCornerShape(16.dp))
                    .testTag("studio_preview_box"),
                contentAlignment = Alignment.Center
            ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // 1. HORIZONTAL ART STYLE PRESET SELECTOR (Directly under image preview)
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

            Spacer(modifier = Modifier.height(16.dp))

            // 2. GLOBAL CONTROLS CARD (Abstraktion & Motivstabilität)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Global Slider 1: Abstraktion
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Abstraktion",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlateWhite
                        )
                        Text(
                            text = "${uiState.styleParameters.abstraction.roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanAccent
                        )
                    }

                    val abstractionDesc = when {
                        uiState.styleParameters.abstraction < 30f -> "Motiv dominant • Dezente Perspektivkonturen"
                        uiState.styleParameters.abstraction < 70f -> "Künstlerische Synthese • Harmonische Raumtiefe"
                        else -> "Volle Abstraktion • Kinetische Dispersion"
                    }

                    Text(
                        text = abstractionDesc,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateLight100.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = uiState.styleParameters.abstraction,
                        onValueChange = { viewModel.setAbstraction(it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeon,
                            activeTrackColor = CyanAccent,
                            inactiveTrackColor = SlateDark700
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_abstraction")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Global Slider 2: Motivstabilität
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Motivstabilität",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlateWhite
                        )
                        Text(
                            text = "${uiState.styleParameters.subjectStability.roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanNeon
                        )
                    }

                    val stabilityDesc = when {
                        uiState.styleParameters.subjectStability < 40f -> "Freie Perspektiven-Dispersion"
                        uiState.styleParameters.subjectStability < 75f -> "Balancierte Hauptmotiv-Verankerung"
                        else -> "Dominanter Motivfokus • Feste Zentrierung"
                    }

                    Text(
                        text = stabilityDesc,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateLight100.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = uiState.styleParameters.subjectStability,
                        onValueChange = { viewModel.setSubjectStability(it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeon,
                            activeTrackColor = CyanAccent,
                            inactiveTrackColor = SlateDark700
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_subject_stability")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reference Image Carousel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Referenzbild auswählen",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = SlateWhite
                        )
                        Text(
                            text = "Bild ${uiState.selectedRefIndex + 1} / ${active.actualCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(active.imagePaths) { index, path ->
                            val isSelected = index == uiState.selectedRefIndex
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) CyanNeon else SlateDark700,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectReferenceIndex(index) }
                                    .testTag("ref_thumb_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = path,
                                    contentDescription = "Perspektive $index",
                                    modifier = Modifier.fillMaxSize(),
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
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Expandable Advanced Parameters Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateDark800)
                            .clickable { showAdvancedParams = !showAdvancedParams }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Erweiterte Einstellungen",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Erweiterte Stil- & Ausrichtungsparameter",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SlateLight100
                            )
                        }
                        Icon(
                            imageVector = if (showAdvancedParams) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Aufklappen",
                            tint = CyanAccent
                        )
                    }

                    // Advanced Parameters Body
                    if (showAdvancedParams) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Überlagerungsstärke
                        ParameterSlider(
                            label = "Überlagerungsstärke (Blend)",
                            value = uiState.styleParameters.blendStrength,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(blendStrength = it)) }
                        )

                        // Kantenbetonung
                        ParameterSlider(
                            label = "Kantenbetonung (Kontur/Linien)",
                            value = uiState.styleParameters.edgeEmphasis,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(edgeEmphasis = it)) }
                        )

                        // Farbverdichtung
                        ParameterSlider(
                            label = "Farbverdichtung (Sättigung/Dichte)",
                            value = uiState.styleParameters.colorDensity,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(colorDensity = it)) }
                        )

                        // Weichheit & Schleier
                        ParameterSlider(
                            label = "Malerische Weichheit",
                            value = uiState.styleParameters.softness,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(softness = it)) }
                        )

                        // Bewegungsdynamik
                        ParameterSlider(
                            label = "Bewegungsdynamik (Trails)",
                            value = uiState.styleParameters.motionStrength,
                            onValueChange = { viewModel.updateStyleParameters(uiState.styleParameters.copy(motionStrength = it)) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2D Bildregistrierung & Ausrichtung
                        Text(
                            text = "2D Bildregistrierung & Translation",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlateWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))

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
                                    text = if (uiState.autoAlignApplied) "Ausrichtung aktiv" else "Grobe 2D-Ausrichtung",
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Manual Fine Shift
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
                    onClick = { viewModel.exportArtwork(quality = 95) },
                    enabled = uiState.renderedBitmap != null && !uiState.isRendering,
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
                        text = "In Android-Galerie exportieren (JPEG)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // If exported, show share button
                if (uiState.exportedUri != null) {
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, uiState.exportedUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "ORBIT Kunstwerk teilen"))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
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
                        Text(text = "Kunstwerk teilen")
                    }
                }
            }
        }
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
            .width(145.dp)
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
