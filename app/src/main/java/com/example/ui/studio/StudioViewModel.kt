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
import com.example.model.OrbitCompositionStrategy
import com.example.model.OrbitExportRatio
import com.example.model.OrbitExportSize
import com.example.model.OrbitProject
import com.example.model.OrbitStyleParameters
import com.example.model.OrbitWorkspaceMode
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
import kotlin.random.Random

data class StudioUiState(
    val projects: List<OrbitProject> = emptyList(),
    val activeProject: OrbitProject? = null,
    val renderedBitmap: Bitmap? = null,
    val referenceBitmap: Bitmap? = null,
    val isRendering: Boolean = false,
    val renderProgress: FusionProgress? = null,
    val selectedArtStyle: OrbitArtStyle = OrbitArtStyle.IMPRESSIONIST,
    val compositionStrategy: OrbitCompositionStrategy = OrbitCompositionStrategy.SUBJECT_CENTERED,
    val workspaceMode: OrbitWorkspaceMode = OrbitWorkspaceMode.SIMPLE,
    val canUndo: Boolean = false,
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
    val message: String? = null,

    // V0.5 Simple Quality Export
    val isExportDialogOpen: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: FusionProgress? = null,
    val selectedExportSize: OrbitExportSize = OrbitExportSize.HIGH_2048,
    val selectedExportRatio: OrbitExportRatio = OrbitExportRatio.SQUARE_1_1,
    val selectedExportQuality: Int = 95
) {
    val abstractionLevel: Float
        get() = styleParameters.abstraction
}

sealed class StudioEffect {
    data class ExportSuccess(val uri: Uri, val message: String) : StudioEffect()
    data class ShareImage(val uri: Uri, val title: String) : StudioEffect()
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

    private var undoStyleParameters: OrbitStyleParameters? = null
    private var undoCompositionStrategy: OrbitCompositionStrategy? = null

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
            compositionStrategy = project.compositionStrategy,
            workspaceMode = project.workspaceMode,
            canUndo = false,
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

    fun setWorkspaceMode(mode: OrbitWorkspaceMode) {
        _uiState.value = _uiState.value.copy(workspaceMode = mode)
        val project = _uiState.value.activeProject ?: return
        repository.updateProject(project.copy(workspaceMode = mode))
    }

    fun setCompositionStrategy(strategy: OrbitCompositionStrategy) {
        _uiState.value = _uiState.value.copy(compositionStrategy = strategy)
        val project = _uiState.value.activeProject ?: return
        repository.updateProject(project.copy(compositionStrategy = strategy))
        scheduleDebouncedFusion(delayMs = 0L)
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

    fun setVibrance(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(vibrance = value)
        _uiState.value = _uiState.value.copy(styleParameters = updatedParams)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    fun setContrast(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(contrast = value)
        _uiState.value = _uiState.value.copy(styleParameters = updatedParams)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    fun setWarmth(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(warmth = value)
        _uiState.value = _uiState.value.copy(styleParameters = updatedParams)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    fun setColorDensity(value: Float) {
        val updatedParams = _uiState.value.styleParameters.copy(colorDensity = value)
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

    fun surpriseMix() {
        val currentParams = _uiState.value.styleParameters
        val currentStrategy = _uiState.value.compositionStrategy
        val style = _uiState.value.selectedArtStyle

        // Save state for undo
        undoStyleParameters = currentParams
        undoCompositionStrategy = currentStrategy

        fun r(min: Float, max: Float) = min + Random.nextFloat() * (max - min)

        val newParams = when (style) {
            OrbitArtStyle.IMPRESSIONIST -> currentParams.copy(
                abstraction = r(40f, 68f),
                subjectStability = r(45f, 75f),
                vibrance = r(50f, 85f),
                colorIntensity = r(55f, 80f),
                contrast = r(45f, 65f),
                warmth = r(15f, 60f),
                echoStrength = r(25f, 55f),
                backgroundDissolve = r(35f, 70f),
                softness = r(30f, 60f)
            )
            OrbitArtStyle.IN_THE_ROUND -> currentParams.copy(
                abstraction = r(45f, 75f),
                subjectStability = r(45f, 80f),
                vibrance = r(55f, 90f),
                colorIntensity = r(60f, 85f),
                contrast = r(55f, 75f),
                warmth = r(-10f, 40f),
                echoStrength = r(45f, 85f),
                perspectiveSpread = r(35f, 75f),
                backgroundDissolve = r(40f, 80f)
            )
            OrbitArtStyle.ARCHITECTURAL -> currentParams.copy(
                abstraction = r(20f, 50f),
                subjectStability = r(65f, 92f),
                vibrance = r(35f, 70f),
                colorIntensity = r(45f, 70f),
                contrast = r(65f, 88f),
                warmth = r(-30f, 15f),
                edgeEmphasis = r(60f, 90f),
                echoStrength = r(45f, 80f)
            )
            OrbitArtStyle.ETHEREAL -> currentParams.copy(
                abstraction = r(55f, 85f),
                subjectStability = r(30f, 60f),
                vibrance = r(40f, 75f),
                colorIntensity = r(40f, 70f),
                contrast = r(35f, 55f),
                warmth = r(-25f, 25f),
                lightGlow = r(55f, 88f),
                backgroundDissolve = r(50f, 85f)
            )
            OrbitArtStyle.MOTION_FLOW -> currentParams.copy(
                abstraction = r(40f, 75f),
                subjectStability = r(30f, 65f),
                vibrance = r(45f, 80f),
                colorIntensity = r(50f, 80f),
                motionStrength = r(50f, 88f),
                echoStrength = r(35f, 70f)
            )
            OrbitArtStyle.URBAN_LAYERS -> currentParams.copy(
                abstraction = r(35f, 70f),
                subjectStability = r(40f, 70f),
                vibrance = r(55f, 92f),
                colorIntensity = r(60f, 88f),
                contrast = r(60f, 85f),
                detailDensity = r(50f, 85f),
                echoStrength = r(45f, 80f)
            )
        }

        _uiState.value = _uiState.value.copy(
            styleParameters = newParams,
            canUndo = true,
            message = "Überraschungs-Mix angewendet"
        )
        scheduleDebouncedFusion(delayMs = 0L)
    }

    fun undoSurpriseMix() {
        val prevParams = undoStyleParameters ?: return
        val prevStrategy = undoCompositionStrategy ?: _uiState.value.compositionStrategy

        _uiState.value = _uiState.value.copy(
            styleParameters = prevParams,
            compositionStrategy = prevStrategy,
            canUndo = false,
            message = "Vorherige Parameter wiederhergestellt"
        )
        undoStyleParameters = null
        undoCompositionStrategy = null
        scheduleDebouncedFusion(delayMs = 0L)
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
                compositionStrategy = state.compositionStrategy,
                workspaceMode = state.workspaceMode,
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
                    compositionStrategy = state.compositionStrategy,
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

    fun openExportDialog() {
        _uiState.value = _uiState.value.copy(isExportDialogOpen = true)
    }

    fun closeExportDialog() {
        if (!_uiState.value.isExporting) {
            _uiState.value = _uiState.value.copy(isExportDialogOpen = false)
        }
    }

    fun setExportSize(size: OrbitExportSize) {
        _uiState.value = _uiState.value.copy(selectedExportSize = size)
    }

    fun setExportRatio(ratio: OrbitExportRatio) {
        _uiState.value = _uiState.value.copy(selectedExportRatio = ratio)
    }

    fun setExportQuality(quality: Int) {
        _uiState.value = _uiState.value.copy(selectedExportQuality = quality)
    }

    fun exportArtwork(quality: Int = 95) {
        openExportDialog()
    }

    fun shareExportedImage(uri: Uri) {
        viewModelScope.launch {
            _effects.emit(StudioEffect.ShareImage(uri, "ORBIT Kunstwerk"))
        }
    }

    fun startExport(triggerShare: Boolean = false) {
        val state = _uiState.value
        val project = state.activeProject ?: return
        if (project.imagePaths.isEmpty()) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("Keine Bilder zum Exportieren vorhanden"))
            }
            return
        }

        val size = state.selectedExportSize
        val ratio = state.selectedExportRatio
        val quality = state.selectedExportQuality
        val (targetW, targetH) = ratio.calculateDimensions(size.longEdge)

        val artStyle = state.selectedArtStyle
        val params = state.styleParameters
        val shifts = state.individualShifts
        val refIndex = state.selectedRefIndex
        val offsetX = state.offsetX
        val offsetY = state.offsetY
        val blendMode = state.blendMode

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExporting = true,
                exportProgress = FusionProgress(0, project.actualCount, "Vorbereitung für ${size.title} ($targetW×$targetH px)...")
            )

            try {
                // Re-compute freshly from original raw images and current style parameters!
                val exportBitmap = OrbitFusionEngine.renderFusion(
                    imagePaths = project.imagePaths,
                    referenceIndex = refIndex,
                    artStyle = artStyle,
                    compositionStrategy = state.compositionStrategy,
                    styleParams = params,
                    individualShifts = shifts,
                    globalOffsetX = offsetX,
                    globalOffsetY = offsetY,
                    blendMode = blendMode,
                    targetWidth = targetW,
                    targetHeight = targetH,
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(exportProgress = progress)
                    }
                )

                if (exportBitmap != null) {
                    val title = "ORBIT_${project.name.replace(" ", "_")}_${artStyle.name}_${targetW}x${targetH}"
                    val uri = BitmapUtils.exportToMediaStore(
                        context = getApplication(),
                        bitmap = exportBitmap,
                        title = title,
                        quality = quality
                    )
                    exportBitmap.recycle()

                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        isExportDialogOpen = false,
                        exportProgress = null,
                        exportedUri = uri
                    )

                    if (uri != null) {
                        if (triggerShare) {
                            _effects.emit(StudioEffect.ShareImage(uri, title))
                        } else {
                            _effects.emit(StudioEffect.ExportSuccess(uri, "Kunstwerk ($targetW×$targetH px) in Galerie gespeichert!"))
                        }
                    } else {
                        _effects.emit(StudioEffect.ShowMessage("Fehler beim Speichern in MediaStore"))
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportProgress = null,
                        message = "Speicher reicht für $targetW×$targetH px nicht aus. Bitte wähle eine kleinere Exportgröße (z. B. 2048 px)."
                    )
                    _effects.emit(StudioEffect.ShowMessage("Export-Speicherlimit erreicht. Bitte 2048 px wählen."))
                }
            } catch (oom: OutOfMemoryError) {
                System.gc()
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportProgress = null,
                    message = "Speicherüberlauf bei ${size.title}. Bitte wähle 2048 px."
                )
                _effects.emit(StudioEffect.ShowMessage("Speicherlimit überschritten. Bitte 2048 px wählen."))
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportProgress = null,
                    message = "Exportfehler: ${e.localizedMessage ?: "Unbekannter Fehler"}"
                )
                _effects.emit(StudioEffect.ShowMessage("Export fehlgeschlagen"))
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
