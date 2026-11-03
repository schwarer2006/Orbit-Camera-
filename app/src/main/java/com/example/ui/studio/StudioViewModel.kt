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
import com.example.model.OrbitArtStyle
import com.example.model.OrbitBlendMode
import com.example.model.OrbitProject
import com.example.model.OrbitStyleParameters
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
    val selectedArtStyle: OrbitArtStyle = OrbitArtStyle.IMPRESSIONIST,
    val styleParameters: OrbitStyleParameters = OrbitArtStyle.IMPRESSIONIST.defaultParameters,
    val individualShifts: Map<Int, Pair<Float, Float>> = emptyMap(),
    val isAutoAligning: Boolean = false,
    val autoAlignApplied: Boolean = false,
    val blendMode: OrbitBlendMode = OrbitBlendMode.WEIGHTED_AVERAGE,
    val selectedRefIndex: Int = 0,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val showOriginalComparison: Boolean = false,
    val exportedUri: Uri? = null,
    val message: String? = null
) {
    val abstractionLevel: Float
        get() = styleParameters.abstraction
}

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
            selectedArtStyle = project.artStyle,
            styleParameters = project.styleParams,
            blendMode = project.blendMode,
            selectedRefIndex = refIndex,
            offsetX = project.offsetX,
            offsetY = project.offsetY,
            individualShifts = emptyMap(),
            autoAlignApplied = false,
            renderedBitmap = null,
            referenceBitmap = null,
            exportedUri = null
        )

        loadReferenceBitmap(project, refIndex)
        triggerFusion(
            project = project,
            artStyle = project.artStyle,
            params = project.styleParams,
            refIndex = refIndex,
            offsetX = project.offsetX,
            offsetY = project.offsetY,
            shifts = emptyMap(),
            blendMode = project.blendMode
        )
    }

    private fun loadReferenceBitmap(project: OrbitProject, refIndex: Int) {
        if (project.imagePaths.isEmpty() || refIndex !in project.imagePaths.indices) return
        viewModelScope.launch {
            val path = project.imagePaths[refIndex]
            val bmp = BitmapUtils.decodeSampledBitmap(path, 800, 800)
            _uiState.value = _uiState.value.copy(referenceBitmap = bmp)
        }
    }

    fun setArtStyle(style: OrbitArtStyle) {
        val currentParams = _uiState.value.styleParameters
        // Keep user's current abstraction & stability or apply preset defaults
        val newParams = style.defaultParameters.copy(
            abstraction = currentParams.abstraction,
            subjectStability = currentParams.subjectStability
        )
        _uiState.value = _uiState.value.copy(
            selectedArtStyle = style,
            styleParameters = newParams
        )
        val project = _uiState.value.activeProject ?: return
        triggerFusion(
            project = project,
            artStyle = style,
            params = newParams,
            refIndex = _uiState.value.selectedRefIndex,
            offsetX = _uiState.value.offsetX,
            offsetY = _uiState.value.offsetY,
            shifts = _uiState.value.individualShifts,
            blendMode = _uiState.value.blendMode
        )
    }

    fun setAbstraction(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(abstraction = value)
        _uiState.value = _uiState.value.copy(styleParameters = updatedParams)
        val project = _uiState.value.activeProject ?: return
        triggerFusion(
            project = project,
            artStyle = _uiState.value.selectedArtStyle,
            params = updatedParams,
            refIndex = _uiState.value.selectedRefIndex,
            offsetX = _uiState.value.offsetX,
            offsetY = _uiState.value.offsetY,
            shifts = _uiState.value.individualShifts,
            blendMode = _uiState.value.blendMode
        )
    }

    fun setSubjectStability(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(subjectStability = value)
        _uiState.value = _uiState.value.copy(styleParameters = updatedParams)
        val project = _uiState.value.activeProject ?: return
        triggerFusion(
            project = project,
            artStyle = _uiState.value.selectedArtStyle,
            params = updatedParams,
            refIndex = _uiState.value.selectedRefIndex,
            offsetX = _uiState.value.offsetX,
            offsetY = _uiState.value.offsetY,
            shifts = _uiState.value.individualShifts,
            blendMode = _uiState.value.blendMode
        )
    }

    fun updateStyleParameters(params: OrbitStyleParameters) {
        _uiState.value = _uiState.value.copy(styleParameters = params)
        val project = _uiState.value.activeProject ?: return
        triggerFusion(
            project = project,
            artStyle = _uiState.value.selectedArtStyle,
            params = params,
            refIndex = _uiState.value.selectedRefIndex,
            offsetX = _uiState.value.offsetX,
            offsetY = _uiState.value.offsetY,
            shifts = _uiState.value.individualShifts,
            blendMode = _uiState.value.blendMode
        )
    }

    fun runAutoAlignment() {
        val project = _uiState.value.activeProject ?: return
        if (project.imagePaths.size <= 1) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAutoAligning = true)
            val shifts = OrbitFusionEngine.autoAlignImages(project.imagePaths, _uiState.value.selectedRefIndex)
            _uiState.value = _uiState.value.copy(
                isAutoAligning = false,
                individualShifts = shifts,
                autoAlignApplied = true,
                message = "Automatische 2D-Ausrichtung angewendet"
            )
            triggerFusion(
                project = project,
                artStyle = _uiState.value.selectedArtStyle,
                params = _uiState.value.styleParameters,
                refIndex = _uiState.value.selectedRefIndex,
                offsetX = _uiState.value.offsetX,
                offsetY = _uiState.value.offsetY,
                shifts = shifts,
                blendMode = _uiState.value.blendMode
            )
        }
    }

    fun resetAlignment() {
        _uiState.value = _uiState.value.copy(
            individualShifts = emptyMap(),
            autoAlignApplied = false,
            offsetX = 0f,
            offsetY = 0f,
            message = "Ausrichtung zurückgesetzt"
        )
        val project = _uiState.value.activeProject ?: return
        triggerFusion(
            project = project,
            artStyle = _uiState.value.selectedArtStyle,
            params = _uiState.value.styleParameters,
            refIndex = _uiState.value.selectedRefIndex,
            offsetX = 0f,
            offsetY = 0f,
            shifts = emptyMap(),
            blendMode = _uiState.value.blendMode
        )
    }

    fun setBlendMode(mode: OrbitBlendMode) {
        _uiState.value = _uiState.value.copy(blendMode = mode)
        val project = _uiState.value.activeProject ?: return
        triggerFusion(
            project = project,
            artStyle = _uiState.value.selectedArtStyle,
            params = _uiState.value.styleParameters,
            refIndex = _uiState.value.selectedRefIndex,
            offsetX = _uiState.value.offsetX,
            offsetY = _uiState.value.offsetY,
            shifts = _uiState.value.individualShifts,
            blendMode = mode
        )
    }

    fun selectReferenceIndex(index: Int) {
        val project = _uiState.value.activeProject ?: return
        if (index !in project.imagePaths.indices) return
        _uiState.value = _uiState.value.copy(selectedRefIndex = index)
        loadReferenceBitmap(project, index)
        triggerFusion(
            project = project,
            artStyle = _uiState.value.selectedArtStyle,
            params = _uiState.value.styleParameters,
            refIndex = index,
            offsetX = _uiState.value.offsetX,
            offsetY = _uiState.value.offsetY,
            shifts = _uiState.value.individualShifts,
            blendMode = _uiState.value.blendMode
        )
    }

    fun setOffset(x: Float, y: Float) {
        _uiState.value = _uiState.value.copy(offsetX = x, offsetY = y)
        val project = _uiState.value.activeProject ?: return
        triggerFusion(
            project = project,
            artStyle = _uiState.value.selectedArtStyle,
            params = _uiState.value.styleParameters,
            refIndex = _uiState.value.selectedRefIndex,
            offsetX = x,
            offsetY = y,
            shifts = _uiState.value.individualShifts,
            blendMode = _uiState.value.blendMode
        )
    }

    fun toggleComparison(showOriginal: Boolean) {
        _uiState.value = _uiState.value.copy(showOriginalComparison = showOriginal)
    }

    fun recompute() {
        val state = _uiState.value
        val project = state.activeProject ?: return
        triggerFusion(
            project = project,
            artStyle = state.selectedArtStyle,
            params = state.styleParameters,
            refIndex = state.selectedRefIndex,
            offsetX = state.offsetX,
            offsetY = state.offsetY,
            shifts = state.individualShifts,
            blendMode = state.blendMode
        )
    }

    private fun triggerFusion(
        project: OrbitProject,
        artStyle: OrbitArtStyle,
        params: OrbitStyleParameters,
        refIndex: Int,
        offsetX: Float,
        offsetY: Float,
        shifts: Map<Int, Pair<Float, Float>>,
        blendMode: OrbitBlendMode
    ) {
        if (project.imagePaths.isEmpty()) return

        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRendering = true,
                renderProgress = FusionProgress(0, project.actualCount, "Vorbereitung...")
            )

            val updatedProject = project.copy(
                artStyle = artStyle,
                styleParams = params,
                abstractionLevel = params.abstraction,
                blendMode = blendMode,
                referenceImageIndex = refIndex,
                offsetX = offsetX,
                offsetY = offsetY
            )
            repository.updateProject(updatedProject)

            val result = OrbitFusionEngine.renderFusion(
                imagePaths = project.imagePaths,
                referenceIndex = refIndex,
                artStyle = artStyle,
                styleParams = params,
                individualShifts = shifts,
                globalOffsetX = offsetX,
                globalOffsetY = offsetY,
                blendMode = blendMode,
                targetWidth = 1080,
                targetHeight = 1080,
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(renderProgress = progress)
                }
            )

            if (result != null) {
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
                title = "ORBIT_${project.name.replace(" ", "_")}_${_uiState.value.selectedArtStyle.name}",
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
