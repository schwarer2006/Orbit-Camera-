package com.example.ui.projects

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OrbitProjectRepository
import com.example.model.OrbitCaptureMode
import com.example.model.OrbitProject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val projects: List<OrbitProject> = emptyList(),
    val isImporting: Boolean = false,
    val importCurrent: Int = 0,
    val importTotal: Int = 0,
    val projectToRename: OrbitProject? = null,
    val projectToDelete: OrbitProject? = null,
    val message: String? = null
)

sealed class ProjectsEffect {
    data class NavigateToStudio(val projectId: String) : ProjectsEffect()
    data class NavigateToCamera(val projectId: String) : ProjectsEffect()
    data class ShowMessage(val text: String) : ProjectsEffect()
}

class ProjectsViewModel(
    application: Application,
    private val repository: OrbitProjectRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ProjectsEffect>()
    val effects: SharedFlow<ProjectsEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.projects.collect { list ->
                _uiState.value = _uiState.value.copy(projects = list)
            }
        }
    }

    fun startImportFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImporting = true,
                importCurrent = 0,
                importTotal = uris.size
            )

            // Create new project for this imported series
            val project = repository.createProject(
                name = "Importierte Serie (${uris.size} Bilder)",
                mode = OrbitCaptureMode.LINEAR,
                targetCount = uris.size
            )

            val updated = repository.importFromUris(
                projectId = project.id,
                uris = uris,
                onProgress = { current, total ->
                    _uiState.value = _uiState.value.copy(
                        importCurrent = current,
                        importTotal = total
                    )
                }
            )

            _uiState.value = _uiState.value.copy(
                isImporting = false,
                message = "${uris.size} Bilder erfolgreich importiert"
            )

            if (updated != null && updated.actualCount > 0) {
                _effects.emit(ProjectsEffect.NavigateToStudio(updated.id))
            }
        }
    }

    fun createDemoProject() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImporting = true,
                importCurrent = 18,
                importTotal = 36
            )
            val demo = repository.createSampleProject()
            _uiState.value = _uiState.value.copy(
                isImporting = false,
                message = "Demo-Serie '${demo.name}' mit 36 Perspektiven erstellt!"
            )
            _effects.emit(ProjectsEffect.NavigateToStudio(demo.id))
        }
    }

    fun promptRename(project: OrbitProject) {
        _uiState.value = _uiState.value.copy(projectToRename = project)
    }

    fun dismissRename() {
        _uiState.value = _uiState.value.copy(projectToRename = null)
    }

    fun confirmRename(newName: String) {
        val project = _uiState.value.projectToRename ?: return
        if (newName.isNotBlank()) {
            val updated = project.copy(name = newName.trim())
            repository.updateProject(updated)
        }
        _uiState.value = _uiState.value.copy(projectToRename = null)
    }

    fun promptDelete(project: OrbitProject) {
        _uiState.value = _uiState.value.copy(projectToDelete = project)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(projectToDelete = null)
    }

    fun confirmDelete() {
        val project = _uiState.value.projectToDelete ?: return
        repository.deleteProject(project.id)
        _uiState.value = _uiState.value.copy(
            projectToDelete = null,
            message = "Serie '${project.name}' gelöscht"
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
