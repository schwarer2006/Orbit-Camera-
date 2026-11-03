package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.OrbitProjectRepository
import com.example.ui.camera.CameraViewModel
import com.example.ui.navigation.OrbitMainContainer
import com.example.ui.projects.ProjectsViewModel
import com.example.ui.studio.StudioViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: OrbitProjectRepository

    private val cameraViewModel: CameraViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CameraViewModel(application, repository) as T
            }
        }
    }

    private val projectsViewModel: ProjectsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProjectsViewModel(application, repository) as T
            }
        }
    }

    private val studioViewModel: StudioViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StudioViewModel(application, repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = OrbitProjectRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                OrbitMainContainer(
                    repository = repository,
                    cameraViewModel = cameraViewModel,
                    projectsViewModel = projectsViewModel,
                    studioViewModel = studioViewModel
                )
            }
        }
    }
}
