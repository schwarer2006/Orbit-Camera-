package com.example.ui.studio

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OrbitProjectRepository
import com.example.engine.BitmapUtils
import com.example.engine.FusionProgress
import com.example.engine.OrbitFusionEngine
import com.example.model.OrbitBlendMode
import com.example.model.OrbitProject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class StudioUiState(
    val projects: List<OrbitProject> = emptyList(),
    val activeProject: OrbitProject? = null,
    val renderedBitmap: Bitmap? = null,
    val referenceBitmap: Bitmap? = null,
    val isRendering: Boolean = false,
    val renderProgress: FusionProgress? = null,
    val abstractionLevel: Float = 50f,
    val blendMode: OrbitBlendMode = OrbitBlendMode.WEIGHTED_AVERAGE,
    val selectedRefIndex: Int = 0,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val showOriginalComparison: Boolean = false,
    val exportedUri: Uri? = null,
    val message: String? = null
)

sealed class StudioEffect {
    data class ExportSuccess(val uri: Uri, val message: String) : StudioEffect()
    data class ShowMessage(val text: String) : StudioEffect()
}

class StudioViewModel(
    application: Application,
    private val repository: OrbitProjectRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<StudioEffect>()
    val effects: SharedFlow<StudioEffect> = _effects.asSharedFlow()

    private var renderJob: Job? = null

    init {
        viewModelScope.launch {
            repository.projects.collect { list ->
                _uiState.value = _uiState.value.copy(projects = list)
                val current = _uiState.value.activeProject
                if (current == null && list.isNotEmpty()) {
                    selectProject(list.first().id)
                } else if (current != null) {
                    val updated = list.firstOrNull { it.id == current.id }
                    if (updated != null && updated.actualCount != current.actualCount) {
                        selectProject(updated.id)
                    }
                }
            }
        }
    }

    fun selectProject(projectId: String) {
        val project = repository.getProject(projectId) ?: return
        val refIndex = project.referenceImageIndex.coerceIn(0, (project.actualCount - 1).coerceAtLeast(0))

        _uiState.value = _uiState.value.copy(
            activeProject = project,
            abstractionLevel = project.abstractionLevel,
            blendMode = project.blendMode,
            selectedRefIndex = refIndex,
            offsetX = project.offsetX,
            offsetY = project.offsetY,
            renderedBitmap = null,
            referenceBitmap = null,
            exportedUri = null
        )

        loadReferenceBitmap(project, refIndex)
        triggerFusion(project, project.abstractionLevel, project.blendMode, refIndex, project.offsetX, project.offsetY)
    }

    private fun loadReferenceBitmap(project: OrbitProject, refIndex: Int) {
        if (project.imagePaths.isEmpty() || refIndex !in project.imagePaths.indices) return
        viewModelScope.launch {
            val path = project.imagePaths[refIndex]
            val bmp = BitmapUtils.decodeSampledBitmap(path, 800, 800)
            _uiState.value = _uiState.value.copy(referenceBitmap = bmp)
        }
    }

    fun setAbstractionLevel(level: Float) {
        _uiState.value = _uiState.value.copy(abstractionLevel = level)
        val project = _uiState.value.activeProject ?: return
        triggerFusion(project, level, _uiState.value.blendMode, _uiState.value.selectedRefIndex, _uiState.value.offsetX, _uiState.value.offsetY)
    }

    fun setBlendMode(mode: OrbitBlendMode) {
        _uiState.value = _uiState.value.copy(blendMode = mode)
        val project = _uiState.value.activeProject ?: return
        triggerFusion(project, _uiState.value.abstractionLevel, mode, _uiState.value.selectedRefIndex, _uiState.value.offsetX, _uiState.value.offsetY)
    }

    fun selectReferenceIndex(index: Int) {
        val project = _uiState.value.activeProject ?: return
        if (index !in project.imagePaths.indices) return
        _uiState.value = _uiState.value.copy(selectedRefIndex = index)
        loadReferenceBitmap(project, index)
        triggerFusion(project, _uiState.value.abstractionLevel, _uiState.value.blendMode, index, _uiState.value.offsetX, _uiState.value.offsetY)
    }

    fun setOffset(x: Float, y: Float) {
        _uiState.value = _uiState.value.copy(offsetX = x, offsetY = y)
        val project = _uiState.value.activeProject ?: return
        triggerFusion(project, _uiState.value.abstractionLevel, _uiState.value.blendMode, _uiState.value.selectedRefIndex, x, y)
    }

    fun toggleComparison(showOriginal: Boolean) {
        _uiState.value = _uiState.value.copy(showOriginalComparison = showOriginal)
    }

    fun recompute() {
        val state = _uiState.value
        val project = state.activeProject ?: return
        triggerFusion(project, state.abstractionLevel, state.blendMode, state.selectedRefIndex, state.offsetX, state.offsetY)
    }

    private fun triggerFusion(
        project: OrbitProject,
        abstraction: Float,
        blendMode: OrbitBlendMode,
        refIndex: Int,
        offsetX: Float,
        offsetY: Float
    ) {
        if (project.imagePaths.isEmpty()) return

        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRendering = true,
                renderProgress = FusionProgress(0, project.actualCount, "Vorbereitung...")
            )

            // Save parameter adjustments to project
            val updatedProject = project.copy(
                abstractionLevel = abstraction,
                blendMode = blendMode,
                referenceImageIndex = refIndex,
                offsetX = offsetX,
                offsetY = offsetY
            )
            repository.updateProject(updatedProject)

            val result = OrbitFusionEngine.renderFusion(
                imagePaths = project.imagePaths,
                referenceIndex = refIndex,
                abstractionLevel = abstraction,
                offsetX = offsetX,
                offsetY = offsetY,
                blendMode = blendMode,
                targetWidth = 1080,
                targetHeight = 1080,
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(renderProgress = progress)
                }
            )

            if (result != null) {
                // Save preview file locally for quick caching
                val previewFile = File(getApplication<Application>().cacheDir, "preview_${project.id}.jpg")
                BitmapUtils.saveBitmapToFile(result, previewFile, quality = 90)
                repository.updateProject(updatedProject.copy(lastRenderedPath = previewFile.absolutePath))
            }

            _uiState.value = _uiState.value.copy(
                renderedBitmap = result,
                isRendering = false,
                renderProgress = null,
                activeProject = updatedProject
            )
        }
    }

    fun exportArtwork(quality: Int = 95) {
        val bitmap = _uiState.value.renderedBitmap ?: return
        val project = _uiState.value.activeProject ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRendering = true)
            val uri = BitmapUtils.exportToMediaStore(
                context = getApplication(),
                bitmap = bitmap,
                title = "ORBIT_${project.name.replace(" ", "_")}",
                quality = quality
            )

            _uiState.value = _uiState.value.copy(isRendering = false, exportedUri = uri)
            if (uri != null) {
                _effects.emit(StudioEffect.ExportSuccess(uri, "Kunstwerk in Android-Galerie gespeichert (Pictures/ORBIT_Camera)!"))
            } else {
                _effects.emit(StudioEffect.ShowMessage("Export fehlgeschlagen"))
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
