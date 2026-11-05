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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var debounceRenderJob: Job? = null
    private var renderGeneration: Long = 0L
    private val previewDimension: Int = 720

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
        scheduleDebouncedFusion(delayMs = 0L)
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
        scheduleDebouncedFusion(delayMs = 0L)
    }

    fun setAbstraction(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(abstraction = value)
        _uiState.value = _uiState.value.copy(styleParameters = updatedParams)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    fun setSubjectStability(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(subjectStability = value)
        _uiState.value = _uiState.value.copy(styleParameters = updatedParams)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    fun setColorIntensity(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(colorIntensity = value)
        _uiState.value = _uiState.value.copy(styleParameters = updatedParams)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    fun setEdgeEmphasis(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(edgeEmphasis = value)
        _uiState.value = _uiState.value.copy(styleParameters = updatedParams)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    fun setBackgroundDissolve(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(backgroundDissolve = value)
        _uiState.value = _uiState.value.copy(styleParameters = updatedParams)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    fun updateStyleParameters(params: OrbitStyleParameters) {
        _uiState.value = _uiState.value.copy(styleParameters = params)
        scheduleDebouncedFusion(delayMs = 280L)
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
            scheduleDebouncedFusion(delayMs = 0L)
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
        scheduleDebouncedFusion(delayMs = 0L)
    }

    fun setBlendMode(mode: OrbitBlendMode) {
        _uiState.value = _uiState.value.copy(blendMode = mode)
        scheduleDebouncedFusion(delayMs = 0L)
    }

    fun selectReferenceIndex(index: Int) {
        val project = _uiState.value.activeProject ?: return
        if (index !in project.imagePaths.indices) return
        _uiState.value = _uiState.value.copy(selectedRefIndex = index)
        loadReferenceBitmap(project, index)
        scheduleDebouncedFusion(delayMs = 0L)
    }

    fun setOffset(x: Float, y: Float) {
        _uiState.value = _uiState.value.copy(offsetX = x, offsetY = y)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    fun toggleComparison(showOriginal: Boolean) {
        _uiState.value = _uiState.value.copy(showOriginalComparison = showOriginal)
    }

    fun recompute() {
        scheduleDebouncedFusion(delayMs = 0L)
    }

    private fun scheduleDebouncedFusion(delayMs: Long = 280L) {
        debounceRenderJob?.cancel()
        debounceRenderJob = viewModelScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }

            val state = _uiState.value
            val project = state.activeProject ?: return@launch
            if (project.imagePaths.isEmpty()) return@launch

            val thisGen = ++renderGeneration

            _uiState.value = _uiState.value.copy(
                isRendering = true,
                renderProgress = FusionProgress(0, project.actualCount, "Vorbereitung...")
            )

            val updatedProject = project.copy(
                artStyle = state.selectedArtStyle,
                styleParams = state.styleParameters,
                abstractionLevel = state.styleParameters.abstraction,
                blendMode = state.blendMode,
                referenceImageIndex = state.selectedRefIndex,
                offsetX = state.offsetX,
                offsetY = state.offsetY
            )
            repository.updateProject(updatedProject)

            try {
                val result = OrbitFusionEngine.renderFusion(
                    imagePaths = project.imagePaths,
                    referenceIndex = state.selectedRefIndex,
                    artStyle = state.selectedArtStyle,
                    styleParams = state.styleParameters,
                    individualShifts = state.individualShifts,
                    globalOffsetX = state.offsetX,
                    globalOffsetY = state.offsetY,
                    blendMode = state.blendMode,
                    targetWidth = previewDimension,
                    targetHeight = previewDimension,
                    onProgress = { progress ->
                        if (thisGen == renderGeneration) {
                            _uiState.value = _uiState.value.copy(renderProgress = progress)
                        }
                    }
                )

                if (thisGen == renderGeneration) {
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
            } catch (ce: CancellationException) {
                // Cancelled cooperatively by a newer slider movement or action
                throw ce
            } catch (t: Throwable) {
                t.printStackTrace()
                if (thisGen == renderGeneration) {
                    _uiState.value = _uiState.value.copy(
                        isRendering = false,
                        renderProgress = null,
                        message = "Fusionsfehler: ${t.localizedMessage ?: "Unbekannter Fehler"}"
                    )
                }
            }
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
