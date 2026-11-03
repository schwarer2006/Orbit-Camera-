package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.data.OrbitProjectRepository
import com.example.ui.camera.CameraScreen
import com.example.ui.camera.CameraViewModel
import com.example.ui.projects.ProjectsScreen
import com.example.ui.projects.ProjectsViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.studio.StudioScreen
import com.example.ui.studio.StudioViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.SlateDark950
import com.example.ui.theme.SlateLight100

enum class OrbitTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CAMERA("Kamera", Icons.Filled.CameraAlt, Icons.Outlined.CameraAlt),
    PROJECTS("Projekte", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    STUDIO("Studio", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    SETTINGS("Einstellungen", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun OrbitMainContainer(
    repository: OrbitProjectRepository,
    cameraViewModel: CameraViewModel,
    projectsViewModel: ProjectsViewModel,
    studioViewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(OrbitTab.CAMERA) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SlateDark900,
                modifier = Modifier.testTag("orbit_bottom_navigation")
            ) {
                OrbitTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SlateDark950,
                            selectedTextColor = CyanNeon,
                            indicatorColor = CyanAccent,
                            unselectedIconColor = SlateLight100.copy(alpha = 0.6f),
                            unselectedTextColor = SlateLight100.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        containerColor = SlateDark950,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (selectedTab) {
            OrbitTab.CAMERA -> {
                CameraScreen(
                    viewModel = cameraViewModel,
                    onNavigateToStudio = { projectId ->
                        studioViewModel.selectProject(projectId)
                        selectedTab = OrbitTab.STUDIO
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            OrbitTab.PROJECTS -> {
                ProjectsScreen(
                    viewModel = projectsViewModel,
                    onOpenStudio = { projectId ->
                        studioViewModel.selectProject(projectId)
                        selectedTab = OrbitTab.STUDIO
                    },
                    onContinueShooting = { projectId ->
                        repository.getProject(projectId)?.let {
                            cameraViewModel.setActiveProject(it)
                        }
                        selectedTab = OrbitTab.CAMERA
                    },
                    onStartNewCameraSeries = {
                        cameraViewModel.startNewSeries()
                        selectedTab = OrbitTab.CAMERA
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            OrbitTab.STUDIO -> {
                StudioScreen(
                    viewModel = studioViewModel,
                    onNavigateToCamera = {
                        selectedTab = OrbitTab.CAMERA
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            OrbitTab.SETTINGS -> {
                SettingsScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
