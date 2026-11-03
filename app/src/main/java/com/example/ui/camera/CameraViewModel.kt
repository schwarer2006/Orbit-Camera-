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
import com.example.model.OrbitCaptureMode
import com.example.model.OrbitProject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
    val message: String? = null
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
                        targetCount = candidate.targetCount
                    )
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

    fun startNewSeries() {
        val newProj = repository.createProject(
            mode = _uiState.value.selectedMode,
            targetCount = _uiState.value.targetCount
        )
        _uiState.value = _uiState.value.copy(
            activeProject = newProj,
            lastCapturedThumbnail = null,
            message = "Neue Serie '${newProj.name}' gestartet"
        )
    }

    fun setActiveProject(project: OrbitProject) {
        _uiState.value = _uiState.value.copy(
            activeProject = project,
            selectedMode = project.mode,
            targetCount = project.targetCount
        )
    }

    fun capturePhoto(imageCapture: ImageCapture) {
        if (_uiState.value.isCapturing) return

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
                            val updatedProject = repository.addCapture(project.id, bitmap)
                            _uiState.value = _uiState.value.copy(
                                isCapturing = false,
                                activeProject = updatedProject ?: project,
                                lastCapturedThumbnail = bitmap
                            )

                            if (updatedProject != null && updatedProject.isCompleted) {
                                _effects.emit(CameraEffect.SeriesFinished(updatedProject))
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isCapturing = false,
                                message = "Aufnahmeverarbeitung fehlgeschlagen"
                            )
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(
                            isCapturing = false,
                            message = "Kamerafehler: ${exception.message}"
                        )
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
        cameraExecutor.shutdown()
    }
}
