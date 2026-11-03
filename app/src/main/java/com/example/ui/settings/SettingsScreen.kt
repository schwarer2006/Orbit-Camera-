package com.example.ui.settings

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.SlateDark950
import com.example.ui.theme.SlateLight100
import com.example.ui.theme.SlateWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var defaultTarget by remember { mutableStateOf(50) }
    var defaultGrid by remember { mutableStateOf(true) }
    var highQualityExport by remember { mutableStateOf(true) }
    var storageText by remember { mutableStateOf("Berechne Speicher...") }

    fun refreshStorageSize() {
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                val projectsDir = File(context.filesDir, "projects")
                calculateFolderSize(projectsDir) + calculateFolderSize(context.cacheDir)
            }
            val mb = bytes / (1024.0 * 1024.0)
            storageText = "%.1f MB belegt".format(mb)
        }
    }

    LaunchedEffect(Unit) {
        refreshStorageSize()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SlateDark950,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Einstellungen",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = SlateWhite
                    )
                    Text(
                        text = "Kamera, Bildfusion & Speicher",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanAccent
                    )
                }
            }

            // 1. Kamera Optionen
            Text(
                text = "Kamera-Optionen",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SlateWhite,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark900),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Standard Zielaufnahmen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Standard-Zielaufnahmen",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SlateWhite
                            )
                            Text(
                                text = "Empfohlene Serienlänge pro Projekt",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateLight100.copy(alpha = 0.6f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(30, 50, 75, 100).forEach { count ->
                                val selected = defaultTarget == count
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) CyanAccent else SlateDark800)
                                        .clickable { defaultTarget = count }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("setting_target_$count")
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) SlateDark950 else SlateLight100
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gitternetz
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kompositionsraster",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SlateWhite
                            )
                            Text(
                                text = "Drittel-Regel und Orbit-Hilfslinien im Kamerasucher",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateLight100.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = defaultGrid,
                            onCheckedChange = { defaultGrid = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SlateDark950,
                                checkedTrackColor = CyanAccent,
                                uncheckedTrackColor = SlateDark800
                            ),
                            modifier = Modifier.testTag("switch_grid")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Fusion & Export Optionen
            Text(
                text = "Bildfusion & Export",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SlateWhite,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark900),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Maximale Exportqualität (100% JPEG)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SlateWhite
                            )
                            Text(
                                text = if (highQualityExport) "100% verlustarme Kompression" else "92% Standardkompression",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateLight100.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = highQualityExport,
                            onCheckedChange = { highQualityExport = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SlateDark950,
                                checkedTrackColor = CyanAccent,
                                uncheckedTrackColor = SlateDark800
                            ),
                            modifier = Modifier.testTag("switch_high_quality")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Speicherverwaltung
            Text(
                text = "Speicherverwaltung",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SlateWhite,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark900),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Speicher",
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Lokaler Speicher",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SlateWhite
                            )
                        }
                        Text(
                            text = storageText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanNeon
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    context.cacheDir.deleteRecursively()
                                    context.cacheDir.mkdirs()
                                }
                                refreshStorageSize()
                                snackbarHostState.showSnackbar("Vorschau-Cache erfolgreich bereinigt")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlateDark800,
                            contentColor = SlateLight100
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_clear_cache")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Bereinigen",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vorschau-Cache leeren")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Über ORBIT Camera
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark900),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Über",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "ORBIT Camera V1.0",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlateWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Computational Art Photography\n\nORBIT erzeugt aus 30–100 Einzelaufnahmen eines Motivs neue künstlerische Bildkompositionen auf Basis experimenteller Mehrperspektivenfotografie und algorithmischer Bildfusion. Vollständig offline auf dem Smartphone ausgeführt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateLight100.copy(alpha = 0.75f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

private fun calculateFolderSize(file: File): Long {
    var length = 0L
    if (file.isDirectory) {
        val files = file.listFiles() ?: return 0L
        for (f in files) {
            length += if (f.isDirectory) calculateFolderSize(f) else f.length()
        }
    } else {
        length = file.length()
    }
    return length
}
