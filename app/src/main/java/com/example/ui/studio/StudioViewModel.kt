package com.example.ui.studio

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OrbitProjectRepository
import com.example.engine.BitmapUtils
import com.example.engine.FusionProgress
import com.example.engine.ImageQualityReport
import com.example.engine.OrbitFusionEngine
import com.example.engine.OrbitPerformanceTracker
import com.example.engine.OrbitPreviewCache
import com.example.engine.RegistrationReport
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
    val registrationReport: RegistrationReport? = null,
    val blendMode: OrbitBlendMode = OrbitBlendMode.WEIGHTED_AVERAGE,
    val selectedRefIndex: Int = 0,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f,
    val artisticSpreadEnabled: Boolean = false,
    val disabledImagePaths: Set<String> = emptySet(),
    val maskEnabled: Boolean = false,
    val maskRadius: Float = 45f,
    val maskFeather: Float = 30f,
    val maskIntensity: Float = 60f,
    val imageQualityReports: Map<String, ImageQualityReport> = emptyMap(),
    val isAboutDialogOpen: Boolean = false,
    val showOriginalComparison: Boolean = false,
    val exportedUri: Uri? = null,
    val message: String? = null,

    // Simple Quality Export
    val isExportDialogOpen: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: FusionProgress? = null,
    val selectedExportSize: OrbitExportSize = OrbitExportSize.HIGH_2048,
    val selectedExportRatio: OrbitExportRatio = OrbitExportRatio.SQUARE_1_1,
    val selectedExportQuality: Int = 95,

    // Performance & Diagnostics
    val diagnostics: OrbitPerformanceTracker.DiagnosticsSnapshot = OrbitPerformanceTracker.getSnapshot(),
    val isDiagnosticsVisible: Boolean = false
) {
    val abstractionLevel: Float
        get() = styleParameters.abstraction
}

private data class RenderKey(
    val projectId: String,
    val refIndex: Int,
    val artStyle: OrbitArtStyle,
    val strategy: OrbitCompositionStrategy,
    val params: OrbitStyleParameters,
    val shifts: Map<Int, Pair<Float, Float>>,
    val offsetX: Float,
    val offsetY: Float,
    val blendMode: OrbitBlendMode,
    val anchorX: Float,
    val anchorY: Float,
    val spreadEnabled: Boolean,
    val disabledPaths: Set<String>,
    val maskEnabled: Boolean,
    val maskRadius: Float,
    val maskFeather: Float,
    val maskIntensity: Float,
    val imageCount: Int
)

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
    private var lastRenderKey: RenderKey? = null

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
            anchorX = project.anchorX,
            anchorY = project.anchorY,
            artisticSpreadEnabled = project.artisticSpreadEnabled,
            disabledImagePaths = project.disabledImagePaths,
            maskEnabled = project.maskEnabled,
            maskRadius = project.maskRadius,
            maskFeather = project.maskFeather,
            maskIntensity = project.maskIntensity,
            individualShifts = emptyMap(),
            autoAlignApplied = false,
            registrationReport = null,
            renderedBitmap = null,
            referenceBitmap = null,
            exportedUri = null
        )

        lastRenderKey = null
        loadReferenceBitmap(project, refIndex)
        analyzeProjectQuality(project)
        scheduleDebouncedFusion(delayMs = 0L)
    }

    private fun analyzeProjectQuality(project: OrbitProject) {
        if (project.imagePaths.isEmpty()) return
        viewModelScope.launch {
            val qualityMap = BitmapUtils.analyzeImageQuality(project.imagePaths)
            _uiState.value = _uiState.value.copy(imageQualityReports = qualityMap)
        }
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
        val newParams = style.defaultParameters.copy(
            abstraction = currentParams.abstraction,
            subjectStability = currentParams.subjectStability,
            colorIntensity = currentParams.colorIntensity,
            contrast = currentParams.contrast,
            vibrance = currentParams.vibrance,
            warmth = currentParams.warmth
        )

        _uiState.value = _uiState.value.copy(
            selectedArtStyle = style,
            styleParameters = newParams,
            compositionStrategy = style.defaultStrategy
        )
        val project = _uiState.value.activeProject ?: return
        repository.updateProject(
            project.copy(
                artStyle = style,
                styleParams = newParams,
                compositionStrategy = style.defaultStrategy
            )
        )
        scheduleDebouncedFusion(delayMs = 0L)
    }

    fun setSubjectAnchor(x: Float, y: Float) {
        val clampedX = x.coerceIn(0.05f, 0.95f)
        val clampedY = y.coerceIn(0.05f, 0.95f)
        _uiState.value = _uiState.value.copy(anchorX = clampedX, anchorY = clampedY)
        val project = _uiState.value.activeProject ?: return
        repository.updateProject(project.copy(anchorX = clampedX, anchorY = clampedY))
        scheduleDebouncedFusion(delayMs = 150L)
    }

    fun toggleArtisticSpread(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(artisticSpreadEnabled = enabled)
        val project = _uiState.value.activeProject ?: return
        repository.updateProject(project.copy(artisticSpreadEnabled = enabled))
        scheduleDebouncedFusion(delayMs = 0L)
    }

    fun toggleImageDisabled(path: String) {
        val currentDisabled = _uiState.value.disabledImagePaths.toMutableSet()
        val wasDisabled = path in currentDisabled
        val totalImages = _uiState.value.activeProject?.imagePaths?.size ?: 0
        val activeCountBefore = totalImages - currentDisabled.size

        if (!wasDisabled && activeCountBefore <= 2) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("Mindestens 2 Aufnahmen müssen aktiv bleiben"))
            }
            return
        }

        if (wasDisabled) {
            currentDisabled.remove(path)
        } else {
            currentDisabled.add(path)
        }

        _uiState.value = _uiState.value.copy(disabledImagePaths = currentDisabled)
        val project = _uiState.value.activeProject ?: return
        repository.updateProject(project.copy(disabledImagePaths = currentDisabled))
        scheduleDebouncedFusion(delayMs = 0L)
    }

    fun setSelectiveMask(
        enabled: Boolean,
        radius: Float? = null,
        feather: Float? = null,
        intensity: Float? = null
    ) {
        val newR = radius ?: _uiState.value.maskRadius
        val newF = feather ?: _uiState.value.maskFeather
        val newI = intensity ?: _uiState.value.maskIntensity

        _uiState.value = _uiState.value.copy(
            maskEnabled = enabled,
            maskRadius = newR,
            maskFeather = newF,
            maskIntensity = newI
        )
        val project = _uiState.value.activeProject ?: return
        repository.updateProject(
            project.copy(
                maskEnabled = enabled,
                maskRadius = newR,
                maskFeather = newF,
                maskIntensity = newI
            )
        )
        scheduleDebouncedFusion(delayMs = 200L)
    }

    fun openAboutDialog() {
        _uiState.value = _uiState.value.copy(isAboutDialogOpen = true)
    }

    fun closeAboutDialog() {
        _uiState.value = _uiState.value.copy(isAboutDialogOpen = false)
    }

    fun setAbstraction(value: Float) {
        updateParams { it.copy(abstraction = value.coerceIn(0f, 100f)) }
    }

    fun setSubjectStability(value: Float) {
        updateParams { it.copy(subjectStability = value.coerceIn(0f, 100f)) }
    }

    fun setColorIntensity(value: Float) {
        updateParams { it.copy(colorIntensity = value.coerceIn(0f, 100f)) }
    }

    fun setEdgeEmphasis(value: Float) {
        updateParams { it.copy(edgeEmphasis = value.coerceIn(0f, 100f)) }
    }

    fun setBackgroundDissolve(value: Float) {
        updateParams { it.copy(backgroundDissolve = value.coerceIn(0f, 100f)) }
    }

    fun setVibrance(value: Float) {
        updateParams { it.copy(vibrance = value.coerceIn(0f, 100f)) }
    }

    fun setContrast(value: Float) {
        updateParams { it.copy(contrast = value.coerceIn(0f, 100f)) }
    }

    fun setWarmth(value: Float) {
        updateParams { it.copy(warmth = value.coerceIn(-100f, 100f)) }
    }

    fun setColorDensity(value: Float) {
        updateParams { it.copy(colorDensity = value.coerceIn(0f, 100f)) }
    }

    fun setEchoStrength(value: Float) {
        updateParams { it.copy(echoStrength = value.coerceIn(0f, 100f)) }
    }

    fun setEchoCount(value: Float) {
        updateParams { it.copy(echoCount = value.coerceIn(3f, 12f)) }
    }

    fun setPerspectiveSpread(value: Float) {
        updateParams { it.copy(perspectiveSpread = value.coerceIn(0f, 100f)) }
    }

    fun setSoftness(value: Float) {
        updateParams { it.copy(softness = value.coerceIn(0f, 100f)) }
    }

    fun setLightGlow(value: Float) {
        updateParams { it.copy(lightGlow = value.coerceIn(0f, 100f)) }
    }

    fun setMotionStrength(value: Float) {
        updateParams { it.copy(motionStrength = value.coerceIn(0f, 100f)) }
    }

    fun setDetailDensity(value: Float) {
        updateParams { it.copy(detailDensity = value.coerceIn(0f, 100f)) }
    }

    fun updateStyleParameters(params: OrbitStyleParameters) {
        _uiState.value = _uiState.value.copy(styleParameters = params)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    private fun updateParams(transform: (OrbitStyleParameters) -> OrbitStyleParameters) {
        val newParams = transform(_uiState.value.styleParameters)
        _uiState.value = _uiState.value.copy(styleParameters = newParams)
        scheduleDebouncedFusion(delayMs = 280L)
    }

    fun surpriseMix() = applySurpriseMix()

    fun applySurpriseMix() {
        val currentParams = _uiState.value.styleParameters
        val currentStrategy = _uiState.value.compositionStrategy
        undoStyleParameters = currentParams
        undoCompositionStrategy = currentStrategy

        fun r(min: Float, max: Float) = (min + Random.nextFloat() * (max - min))

        val newParams = when (_uiState.value.selectedArtStyle) {
            OrbitArtStyle.IMPRESSIONIST -> currentParams.copy(
                abstraction = r(30f, 65f),
                subjectStability = r(40f, 75f),
                vibrance = r(50f, 85f),
                colorIntensity = r(55f, 80f),
                contrast = r(45f, 70f),
                warmth = r(10f, 50f),
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
            val alignResult = OrbitFusionEngine.autoAlignImages(project.imagePaths, _uiState.value.selectedRefIndex)
            val rep = alignResult.report
            _uiState.value = _uiState.value.copy(
                isAutoAligning = false,
                individualShifts = alignResult.shifts,
                registrationReport = rep,
                autoAlignApplied = true,
                message = "Sobel-Registrierung: ${rep.successfulCount}/${rep.totalImages} Fotos zuverlässig ausgerichtet"
            )
            scheduleDebouncedFusion(delayMs = 0L)
        }
    }

    fun resetAlignment() {
        _uiState.value = _uiState.value.copy(
            individualShifts = emptyMap(),
            autoAlignApplied = false,
            registrationReport = null,
            offsetX = 0f,
            offsetY = 0f,
            message = "Ausrichtung zurückgesetzt"
        )
        scheduleDebouncedFusion(delayMs = 0L)
    }

    fun setBlendMode(mode: OrbitBlendMode) {
        _uiState.value = _uiState.value.copy(blendMode = mode)
        val project = _uiState.value.activeProject ?: return
        repository.updateProject(project.copy(blendMode = mode))
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
        if (debounceRenderJob?.isActive == true) {
            debounceRenderJob?.cancel()
            OrbitPerformanceTracker.recordJobCanceled()
        }
        debounceRenderJob = viewModelScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }

            val state = _uiState.value
            val project = state.activeProject ?: return@launch
            if (project.imagePaths.isEmpty()) return@launch

            val currentKey = RenderKey(
                projectId = project.id,
                refIndex = state.selectedRefIndex,
                artStyle = state.selectedArtStyle,
                strategy = state.compositionStrategy,
                params = state.styleParameters,
                shifts = state.individualShifts,
                offsetX = state.offsetX,
                offsetY = state.offsetY,
                blendMode = state.blendMode,
                anchorX = state.anchorX,
                anchorY = state.anchorY,
                spreadEnabled = state.artisticSpreadEnabled,
                disabledPaths = state.disabledImagePaths,
                maskEnabled = state.maskEnabled,
                maskRadius = state.maskRadius,
                maskFeather = state.maskFeather,
                maskIntensity = state.maskIntensity,
                imageCount = project.activeCount
            )

            // Redundant calculation avoidance: Skip if parameters haven't changed and a render already exists
            if (currentKey == lastRenderKey && _uiState.value.renderedBitmap != null) {
                return@launch
            }

            val thisGen = ++renderGeneration
            OrbitPerformanceTracker.recordJobStarted()

            _uiState.value = _uiState.value.copy(
                isRendering = true,
                renderProgress = FusionProgress(0, project.activeCount, "Vorbereitung...")
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
                offsetY = state.offsetY,
                anchorX = state.anchorX,
                anchorY = state.anchorY,
                artisticSpreadEnabled = state.artisticSpreadEnabled,
                disabledImagePaths = state.disabledImagePaths,
                maskEnabled = state.maskEnabled,
                maskRadius = state.maskRadius,
                maskFeather = state.maskFeather,
                maskIntensity = state.maskIntensity
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
                    anchorX = state.anchorX,
                    anchorY = state.anchorY,
                    artisticSpreadEnabled = state.artisticSpreadEnabled,
                    disabledImagePaths = state.disabledImagePaths,
                    maskEnabled = state.maskEnabled,
                    maskRadius = state.maskRadius,
                    maskFeather = state.maskFeather,
                    maskIntensity = state.maskIntensity,
                    projectId = project.id,
                    onProgress = { progress ->
                        if (thisGen == renderGeneration) {
                            _uiState.value = _uiState.value.copy(renderProgress = progress)
                        }
                    }
                )

                if (thisGen == renderGeneration) {
                    if (result != null) {
                        lastRenderKey = currentKey
                        val previewFile = File(getApplication<Application>().cacheDir, "preview_${project.id}.jpg")
                        BitmapUtils.saveBitmapToFile(result, previewFile, quality = 90)
                        repository.updateProject(updatedProject.copy(lastRenderedPath = previewFile.absolutePath))
                    }

                    _uiState.value = _uiState.value.copy(
                        renderedBitmap = result,
                        isRendering = false,
                        renderProgress = null,
                        activeProject = updatedProject,
                        diagnostics = OrbitPerformanceTracker.getSnapshot()
                    )
                }
            } catch (ce: CancellationException) {
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

    fun toggleDiagnostics() {
        _uiState.value = _uiState.value.copy(
            isDiagnosticsVisible = !_uiState.value.isDiagnosticsVisible,
            diagnostics = OrbitPerformanceTracker.getSnapshot()
        )
    }

    fun pauseStudioRendering() {
        if (debounceRenderJob?.isActive == true) {
            debounceRenderJob?.cancel()
            OrbitPerformanceTracker.recordJobCanceled()
        }
        _uiState.value = _uiState.value.copy(isRendering = false, renderProgress = null)
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
                exportProgress = FusionProgress(0, project.activeCount, "Vorbereitung für ${size.title} ($targetW×$targetH px)...")
            )

            try {
                val exportStartTime = System.currentTimeMillis()
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
                    anchorX = state.anchorX,
                    anchorY = state.anchorY,
                    artisticSpreadEnabled = state.artisticSpreadEnabled,
                    disabledImagePaths = state.disabledImagePaths,
                    maskEnabled = state.maskEnabled,
                    maskRadius = state.maskRadius,
                    maskFeather = state.maskFeather,
                    maskIntensity = state.maskIntensity,
                    projectId = null, // Ensure export never uses downsampled preview cache
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(exportProgress = progress)
                    }
                )

                if (exportBitmap != null) {
                    OrbitPerformanceTracker.recordExport(System.currentTimeMillis() - exportStartTime)
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
