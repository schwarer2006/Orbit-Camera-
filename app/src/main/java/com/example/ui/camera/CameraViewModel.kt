package com.example.ui.camera

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OrbitProjectRepository
import com.example.engine.BitmapUtils
import com.example.engine.OrbitPerformanceTracker
import com.example.model.OrbitCaptureMode
import com.example.model.OrbitProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

enum class IntervalStatus {
    STOPPED,
    RUNNING,
    PAUSED
}

data class CameraUiState(
    val activeProject: OrbitProject? = null,
    val selectedMode: OrbitCaptureMode = OrbitCaptureMode.ORBIT,
    val targetCount: Int = 50,
    val isGridVisible: Boolean = true,
    val isAnchorVisible: Boolean = false,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f,
    val isCapturing: Boolean = false,
    val lastCapturedThumbnail: Bitmap? = null,
    val message: String? = null,

    // V0.4 Guided Capture: Ghost Overlay (Onion Skin)
    val isGhostOverlayEnabled: Boolean = false,
    val ghostOverlayAlpha: Float = 0.45f,
    val ghostReferenceMode: String = "LAST", // "LAST" or "FIRST"
    val ghostOverlayBitmap: Bitmap? = null,

    // V0.4 Guided Capture: Interval Capture
    val isIntervalMode: Boolean = false,
    val intervalSeconds: Int = 3,
    val intervalStatus: IntervalStatus = IntervalStatus.STOPPED,
    val intervalCountdown: Int = 0
)

sealed class CameraEffect {
    object ShutterFlash : CameraEffect()
    data class SeriesFinished(val project: OrbitProject) : CameraEffect()
}

class CameraViewModel(
    application: Application,
    private val repository: OrbitProjectRepository
) : AndroidViewModel(application) {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CameraEffect>()
    val effects: SharedFlow<CameraEffect> = _effects.asSharedFlow()

    private var intervalJob: Job? = null

    init {
        // Automatically link to latest active project or create one on first capture
        viewModelScope.launch {
            repository.projects.collect { projects ->
                val currentActive = _uiState.value.activeProject
                if (currentActive == null && projects.isNotEmpty()) {
                    val candidate = projects.first()
                    _uiState.value = _uiState.value.copy(
                        activeProject = candidate,
                        selectedMode = candidate.mode,
                        targetCount = candidate.targetCount,
                        isGhostOverlayEnabled = candidate.ghostOverlayEnabled,
                        ghostOverlayAlpha = candidate.ghostOverlayAlpha,
                        ghostReferenceMode = candidate.ghostReferenceMode,
                        isIntervalMode = candidate.isIntervalMode,
                        intervalSeconds = candidate.intervalSeconds
                    )
                    loadGhostOverlayFromProject(candidate)
                } else if (currentActive != null) {
                    val updated = projects.firstOrNull { it.id == currentActive.id }
                    if (updated != null) {
                        _uiState.value = _uiState.value.copy(activeProject = updated)
                    }
                }
            }
        }
    }

    fun selectMode(mode: OrbitCaptureMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
        val active = _uiState.value.activeProject
        if (active != null && active.actualCount == 0) {
            val updated = active.copy(mode = mode)
            repository.updateProject(updated)
            _uiState.value = _uiState.value.copy(activeProject = updated)
        }
    }

    fun selectTargetCount(count: Int) {
        _uiState.value = _uiState.value.copy(targetCount = count)
        val active = _uiState.value.activeProject
        if (active != null) {
            val updated = active.copy(targetCount = count)
            repository.updateProject(updated)
            _uiState.value = _uiState.value.copy(activeProject = updated)
        }
    }

    fun applyInTheRoundTemplate() {
        selectMode(OrbitCaptureMode.ORBIT)
        selectTargetCount(20)
        _uiState.value = _uiState.value.copy(
            isGhostOverlayEnabled = true,
            ghostOverlayAlpha = 0.45f,
            ghostReferenceMode = "LAST"
        )
        val proj = _uiState.value.activeProject
        if (proj != null) {
            val updated = proj.copy(
                mode = OrbitCaptureMode.ORBIT,
                targetCount = 20,
                ghostOverlayEnabled = true,
                ghostOverlayAlpha = 0.45f,
                ghostReferenceMode = "PREVIOUS"
            )
            repository.updateProject(updated)
            _uiState.value = _uiState.value.copy(activeProject = updated)
            loadGhostOverlayFromProject(updated)
        }
    }

    fun toggleGrid() {
        _uiState.value = _uiState.value.copy(isGridVisible = !_uiState.value.isGridVisible)
    }

    fun setAnchor(normX: Float, normY: Float) {
        _uiState.value = _uiState.value.copy(
            isAnchorVisible = true,
            anchorX = normX.coerceIn(0.1f, 0.9f),
            anchorY = normY.coerceIn(0.1f, 0.9f)
        )
    }

    fun toggleAnchor() {
        _uiState.value = _uiState.value.copy(isAnchorVisible = !_uiState.value.isAnchorVisible)
    }

    // Ghost Overlay (Onion Skin) Controls
    fun toggleGhostOverlay() {
        val nextState = !_uiState.value.isGhostOverlayEnabled
        _uiState.value = _uiState.value.copy(isGhostOverlayEnabled = nextState)
        val proj = _uiState.value.activeProject
        if (proj != null) {
            val updated = proj.copy(ghostOverlayEnabled = nextState)
            repository.updateProject(updated)
            if (nextState && _uiState.value.ghostOverlayBitmap == null) {
                loadGhostOverlayFromProject(updated)
            }
        }
    }

    fun setGhostOverlayAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(0.05f, 0.95f)
        _uiState.value = _uiState.value.copy(ghostOverlayAlpha = clamped)
        val proj = _uiState.value.activeProject
        if (proj != null) {
            repository.updateProject(proj.copy(ghostOverlayAlpha = clamped))
        }
    }

    fun setGhostReferenceMode(mode: String) {
        _uiState.value = _uiState.value.copy(ghostReferenceMode = mode)
        val proj = _uiState.value.activeProject
        if (proj != null) {
            repository.updateProject(proj.copy(ghostReferenceMode = mode))
            loadGhostOverlayFromProject(proj)
        }
    }

    private fun loadGhostOverlayFromProject(project: OrbitProject) {
        if (project.imagePaths.isEmpty()) {
            _uiState.value = _uiState.value.copy(ghostOverlayBitmap = null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val path = if (_uiState.value.ghostReferenceMode == "FIRST") {
                project.imagePaths.firstOrNull()
            } else {
                project.imagePaths.lastOrNull()
            }
            if (path != null) {
                val bmp = BitmapUtils.decodeSampledBitmap(path, 720, 720)
                _uiState.value = _uiState.value.copy(ghostOverlayBitmap = bmp)
            }
        }
    }

    // Interval Mode Controls
    fun toggleIntervalMode() {
        val nextMode = !_uiState.value.isIntervalMode
        if (!nextMode && _uiState.value.intervalStatus == IntervalStatus.RUNNING) {
            stopIntervalCapture()
        }
        _uiState.value = _uiState.value.copy(isIntervalMode = nextMode)
        val proj = _uiState.value.activeProject
        if (proj != null) {
            repository.updateProject(proj.copy(isIntervalMode = nextMode))
        }
    }

    fun setIntervalSeconds(seconds: Int) {
        val safeSeconds = seconds.coerceIn(1, 60)
        _uiState.value = _uiState.value.copy(intervalSeconds = safeSeconds)
        val proj = _uiState.value.activeProject
        if (proj != null) {
            repository.updateProject(proj.copy(intervalSeconds = safeSeconds))
        }
    }

    fun startIntervalCapture(imageCapture: ImageCapture) {
        if (_uiState.value.intervalStatus == IntervalStatus.RUNNING) return
        _uiState.value = _uiState.value.copy(intervalStatus = IntervalStatus.RUNNING)
        runIntervalLoop(imageCapture)
    }

    fun pauseIntervalCapture() {
        intervalJob?.cancel()
        _uiState.value = _uiState.value.copy(intervalStatus = IntervalStatus.PAUSED)
    }

    fun resumeIntervalCapture(imageCapture: ImageCapture) {
        if (_uiState.value.intervalStatus == IntervalStatus.RUNNING) return
        _uiState.value = _uiState.value.copy(intervalStatus = IntervalStatus.RUNNING)
        runIntervalLoop(imageCapture)
    }

    fun stopIntervalCapture() {
        intervalJob?.cancel()
        _uiState.value = _uiState.value.copy(
            intervalStatus = IntervalStatus.STOPPED,
            intervalCountdown = 0
        )
    }

    fun onAppMovedToBackground() {
        if (_uiState.value.intervalStatus == IntervalStatus.RUNNING) {
            pauseIntervalCapture()
        }
    }

    private fun runIntervalLoop(imageCapture: ImageCapture) {
        intervalJob?.cancel()
        intervalJob = viewModelScope.launch {
            while (_uiState.value.intervalStatus == IntervalStatus.RUNNING) {
                val proj = _uiState.value.activeProject
                val target = _uiState.value.targetCount
                val current = proj?.actualCount ?: 0
                if (current >= target) {
                    stopIntervalCapture()
                    break
                }

                val totalSec = _uiState.value.intervalSeconds
                for (s in totalSec downTo 1) {
                    if (_uiState.value.intervalStatus != IntervalStatus.RUNNING) break
                    _uiState.value = _uiState.value.copy(intervalCountdown = s)
                    delay(1000L)
                }

                if (_uiState.value.intervalStatus != IntervalStatus.RUNNING) break
                _uiState.value = _uiState.value.copy(intervalCountdown = 0)

                // Sequential capture - wait for capture callback before proceeding to next interval
                val success = suspendCapturePhoto(imageCapture)
                if (!success) {
                    delay(500L)
                }
                delay(200L)
            }
        }
    }

    fun startNewSeries() {
        stopIntervalCapture()
        val newProj = repository.createProject(
            mode = _uiState.value.selectedMode,
            targetCount = _uiState.value.targetCount
        )
        _uiState.value = _uiState.value.copy(
            activeProject = newProj,
            lastCapturedThumbnail = null,
            ghostOverlayBitmap = null,
            message = "Neue Serie '${newProj.name}' gestartet"
        )
    }

    fun setActiveProject(project: OrbitProject) {
        stopIntervalCapture()
        _uiState.value = _uiState.value.copy(
            activeProject = project,
            selectedMode = project.mode,
            targetCount = project.targetCount,
            isGhostOverlayEnabled = project.ghostOverlayEnabled,
            ghostOverlayAlpha = project.ghostOverlayAlpha,
            ghostReferenceMode = project.ghostReferenceMode,
            isIntervalMode = project.isIntervalMode,
            intervalSeconds = project.intervalSeconds
        )
        loadGhostOverlayFromProject(project)
    }

    fun capturePhoto(imageCapture: ImageCapture) {
        viewModelScope.launch {
            suspendCapturePhoto(imageCapture)
        }
    }

    private suspend fun suspendCapturePhoto(imageCapture: ImageCapture): Boolean =
        suspendCancellableCoroutine { continuation ->
            if (_uiState.value.isCapturing) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            var project = _uiState.value.activeProject
            if (project == null) {
                project = repository.createProject(
                    mode = _uiState.value.selectedMode,
                    targetCount = _uiState.value.targetCount
                )
                _uiState.value = _uiState.value.copy(activeProject = project)
            }

            _uiState.value = _uiState.value.copy(isCapturing = true)

            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        val bitmap = imageProxyToBitmap(imageProxy)
                        imageProxy.close()

                        viewModelScope.launch {
                            _effects.emit(CameraEffect.ShutterFlash)
                            if (bitmap != null) {
                                val captureStartTime = System.currentTimeMillis()
                                val (updatedProject, thumbBmp, ghostBmp) = withContext(Dispatchers.IO) {
                                    val updated = repository.addCapture(project.id, bitmap)
                                    val thumbStart = System.currentTimeMillis()
                                    val thumb = Bitmap.createScaledBitmap(
                                        bitmap,
                                        160,
                                        (160f * bitmap.height / bitmap.width).toInt().coerceAtLeast(1),
                                        true
                                    )
                                    val ghost = Bitmap.createScaledBitmap(
                                        bitmap,
                                        600,
                                        (600f * bitmap.height / bitmap.width).toInt().coerceAtLeast(1),
                                        true
                                    )
                                    OrbitPerformanceTracker.recordThumbnail(System.currentTimeMillis() - thumbStart)
                                    Triple(updated, thumb, ghost)
                                }
                                OrbitPerformanceTracker.recordCapture(System.currentTimeMillis() - captureStartTime)

                                val currentGhost = _uiState.value.ghostOverlayBitmap
                                val newGhost = if (_uiState.value.ghostReferenceMode == "FIRST") {
                                    if (currentGhost != null) {
                                        ghostBmp.recycle()
                                        currentGhost
                                    } else {
                                        ghostBmp
                                    }
                                } else {
                                    currentGhost?.recycle()
                                    ghostBmp
                                }

                                // Recycle previous thumbnail bitmap to avoid heap accumulation
                                _uiState.value.lastCapturedThumbnail?.recycle()
                                // Original uncompressed camera bitmap is safely persisted to disk and can now be freed
                                bitmap.recycle()

                                _uiState.value = _uiState.value.copy(
                                    isCapturing = false,
                                    activeProject = updatedProject ?: project,
                                    lastCapturedThumbnail = thumbBmp,
                                    ghostOverlayBitmap = newGhost
                                )

                                if (updatedProject != null && updatedProject.isCompleted) {
                                    _effects.emit(CameraEffect.SeriesFinished(updatedProject))
                                    stopIntervalCapture()
                                }
                                continuation.resume(true)
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isCapturing = false,
                                    message = "Aufnahmeverarbeitung fehlgeschlagen"
                                )
                                continuation.resume(false)
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        viewModelScope.launch {
                            _uiState.value = _uiState.value.copy(
                                isCapturing = false,
                                message = "Kamerafehler: ${exception.message}"
                            )
                            continuation.resume(false)
                        }
                    }
                }
            )
        }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            val rotationDegrees = image.imageInfo.rotationDegrees
            if (rotationDegrees != 0 && bitmap != null) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) bitmap.recycle()
                rotated
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopIntervalCapture()
        cameraExecutor.shutdown()
    }
}
