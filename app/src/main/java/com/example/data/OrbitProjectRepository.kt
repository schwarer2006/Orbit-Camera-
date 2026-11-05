package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import com.example.engine.BitmapUtils
import com.example.model.OrbitArtStyle
import com.example.model.OrbitBlendMode
import com.example.model.OrbitCaptureMode
import com.example.model.OrbitProject
import com.example.model.OrbitStyleParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class OrbitProjectRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val metadataFile = File(context.filesDir, "orbit_projects_v1.json")
    private val projectsDir = File(context.filesDir, "projects").apply { mkdirs() }

    private val _projects = MutableStateFlow<List<OrbitProject>>(emptyList())
    val projects: StateFlow<List<OrbitProject>> = _projects.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        if (!metadataFile.exists()) {
            _projects.value = emptyList()
            return
        }

        try {
            val jsonStr = metadataFile.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<OrbitProject>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val imagePathsJson = obj.getJSONArray("imagePaths")
                val paths = mutableListOf<String>()
                for (j in 0 until imagePathsJson.length()) {
                    paths.add(imagePathsJson.getString(j))
                }

                val artStyle = try {
                    OrbitArtStyle.valueOf(obj.optString("artStyle", OrbitArtStyle.IMPRESSIONIST.name))
                } catch (e: Exception) {
                    OrbitArtStyle.IMPRESSIONIST
                }

                val paramsObj = obj.optJSONObject("styleParams")
                val def = artStyle.defaultParameters
                val styleParams = if (paramsObj != null) {
                    OrbitStyleParameters(
                        abstraction = paramsObj.optDouble("abstraction", obj.optDouble("abstractionLevel", def.abstraction.toDouble())).toFloat(),
                        subjectStability = paramsObj.optDouble("subjectStability", def.subjectStability.toDouble()).toFloat(),
                        colorIntensity = paramsObj.optDouble("colorIntensity", def.colorIntensity.toDouble()).toFloat(),
                        edgeEmphasis = paramsObj.optDouble("edgeEmphasis", def.edgeEmphasis.toDouble()).toFloat(),
                        backgroundDissolve = paramsObj.optDouble("backgroundDissolve", def.backgroundDissolve.toDouble()).toFloat(),
                        echoStrength = paramsObj.optDouble("echoStrength", def.echoStrength.toDouble()).toFloat(),
                        echoCount = paramsObj.optDouble("echoCount", def.echoCount.toDouble()).toFloat(),
                        perspectiveSpread = paramsObj.optDouble("perspectiveSpread", def.perspectiveSpread.toDouble()).toFloat(),
                        colorDensity = paramsObj.optDouble("colorDensity", def.colorDensity.toDouble()).toFloat(),
                        warmth = paramsObj.optDouble("warmth", def.warmth.toDouble()).toFloat(),
                        lightGlow = paramsObj.optDouble("lightGlow", def.lightGlow.toDouble()).toFloat(),
                        shadowDepth = paramsObj.optDouble("shadowDepth", def.shadowDepth.toDouble()).toFloat(),
                        softness = paramsObj.optDouble("softness", def.softness.toDouble()).toFloat(),
                        motionStrength = paramsObj.optDouble("motionStrength", def.motionStrength.toDouble()).toFloat(),
                        detailDensity = paramsObj.optDouble("detailDensity", def.detailDensity.toDouble()).toFloat(),
                        atmosphere = paramsObj.optDouble("atmosphere", def.atmosphere.toDouble()).toFloat(),
                        blendStrength = paramsObj.optDouble("blendStrength", def.blendStrength.toDouble()).toFloat()
                    )
                } else {
                    artStyle.defaultParameters.copy(
                        abstraction = obj.optDouble("abstractionLevel", def.abstraction.toDouble()).toFloat()
                    )
                }

                val project = OrbitProject(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    mode = try {
                        OrbitCaptureMode.valueOf(obj.optString("mode", OrbitCaptureMode.ORBIT.name))
                    } catch (e: Exception) {
                        OrbitCaptureMode.ORBIT
                    },
                    targetCount = obj.optInt("targetCount", 50),
                    imagePaths = paths,
                    referenceImageIndex = obj.optInt("referenceImageIndex", 0),
                    abstractionLevel = styleParams.abstraction,
                    offsetX = obj.optDouble("offsetX", 0.0).toFloat(),
                    offsetY = obj.optDouble("offsetY", 0.0).toFloat(),
                    blendMode = try {
                        OrbitBlendMode.valueOf(obj.optString("blendMode", OrbitBlendMode.WEIGHTED_AVERAGE.name))
                    } catch (e: Exception) {
                        OrbitBlendMode.WEIGHTED_AVERAGE
                    },
                    artStyle = artStyle,
                    styleParams = styleParams,
                    lastRenderedPath = obj.optString("lastRenderedPath", null).takeIf { it != "null" && it?.isNotBlank() == true },
                    ghostOverlayEnabled = obj.optBoolean("ghostOverlayEnabled", false),
                    ghostOverlayAlpha = obj.optDouble("ghostOverlayAlpha", 0.45).toFloat(),
                    ghostReferenceMode = obj.optString("ghostReferenceMode", "PREVIOUS"),
                    isIntervalMode = obj.optBoolean("isIntervalMode", false),
                    intervalSeconds = obj.optInt("intervalSeconds", 3)
                )
                list.add(project)
            }
            _projects.value = list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveProjectsSync() {
        try {
            val array = JSONArray()
            for (p in _projects.value) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("createdAt", p.createdAt)
                    put("mode", p.mode.name)
                    put("targetCount", p.targetCount)
                    val pathsArr = JSONArray()
                    p.imagePaths.forEach { pathsArr.put(it) }
                    put("imagePaths", pathsArr)
                    put("referenceImageIndex", p.referenceImageIndex)
                    put("abstractionLevel", p.styleParams.abstraction.toDouble())
                    put("offsetX", p.offsetX.toDouble())
                    put("offsetY", p.offsetY.toDouble())
                    put("blendMode", p.blendMode.name)
                    put("artStyle", p.artStyle.name)

                    val paramsObj = JSONObject().apply {
                        put("abstraction", p.styleParams.abstraction.toDouble())
                        put("subjectStability", p.styleParams.subjectStability.toDouble())
                        put("colorIntensity", p.styleParams.colorIntensity.toDouble())
                        put("edgeEmphasis", p.styleParams.edgeEmphasis.toDouble())
                        put("backgroundDissolve", p.styleParams.backgroundDissolve.toDouble())
                        put("echoStrength", p.styleParams.echoStrength.toDouble())
                        put("echoCount", p.styleParams.echoCount.toDouble())
                        put("perspectiveSpread", p.styleParams.perspectiveSpread.toDouble())
                        put("colorDensity", p.styleParams.colorDensity.toDouble())
                        put("warmth", p.styleParams.warmth.toDouble())
                        put("lightGlow", p.styleParams.lightGlow.toDouble())
                        put("shadowDepth", p.styleParams.shadowDepth.toDouble())
                        put("softness", p.styleParams.softness.toDouble())
                        put("motionStrength", p.styleParams.motionStrength.toDouble())
                        put("detailDensity", p.styleParams.detailDensity.toDouble())
                        put("atmosphere", p.styleParams.atmosphere.toDouble())
                        put("blendStrength", p.styleParams.blendStrength.toDouble())
                    }
                    put("styleParams", paramsObj)

                    put("lastRenderedPath", p.lastRenderedPath ?: JSONObject.NULL)
                    put("ghostOverlayEnabled", p.ghostOverlayEnabled)
                    put("ghostOverlayAlpha", p.ghostOverlayAlpha.toDouble())
                    put("ghostReferenceMode", p.ghostReferenceMode)
                    put("isIntervalMode", p.isIntervalMode)
                    put("intervalSeconds", p.intervalSeconds)
                }
                array.put(obj)
            }
            metadataFile.writeText(array.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getProject(id: String): OrbitProject? {
        return _projects.value.firstOrNull { it.id == id }
    }

    fun createProject(
        name: String? = null,
        mode: OrbitCaptureMode = OrbitCaptureMode.ORBIT,
        targetCount: Int = 50
    ): OrbitProject {
        val dateFormat = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY)
        val defaultName = name?.ifBlank { null } ?: "Serie ${dateFormat.format(Date())}"
        val project = OrbitProject(
            id = UUID.randomUUID().toString(),
            name = defaultName,
            mode = mode,
            targetCount = targetCount
        )

        val updated = listOf(project) + _projects.value
        _projects.value = updated
        scope.launch { saveProjectsSync() }
        return project
    }

    suspend fun addCapture(projectId: String, bitmap: Bitmap): OrbitProject? = withContext(Dispatchers.IO) {
        val project = getProject(projectId) ?: return@withContext null
        val projectFolder = File(projectsDir, project.id).apply { mkdirs() }
        val filename = "shot_${project.actualCount + 1}_${System.currentTimeMillis()}.jpg"
        val targetFile = File(projectFolder, filename)

        val saved = BitmapUtils.saveBitmapToFile(bitmap, targetFile, quality = 95)
        if (!saved) return@withContext null

        val updatedPaths = project.imagePaths + targetFile.absolutePath
        val refIndex = if (project.imagePaths.isEmpty()) 0 else updatedPaths.size / 2

        val updatedProject = project.copy(
            imagePaths = updatedPaths,
            referenceImageIndex = refIndex
        )
        updateProject(updatedProject)
        updatedProject
    }

    suspend fun importFromUris(
        projectId: String,
        uris: List<Uri>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): OrbitProject? = withContext(Dispatchers.IO) {
        val project = getProject(projectId) ?: return@withContext null
        val projectFolder = File(projectsDir, project.id).apply { mkdirs() }
        val newPaths = mutableListOf<String>()

        for (i in uris.indices) {
            onProgress(i + 1, uris.size)
            val uri = uris[i]
            val filename = "import_${project.actualCount + i + 1}_${System.currentTimeMillis()}.jpg"
            val targetFile = File(projectFolder, filename)

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    newPaths.add(targetFile.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val updatedPaths = project.imagePaths + newPaths
        val refIndex = if (updatedPaths.isNotEmpty()) updatedPaths.size / 2 else 0

        val updatedProject = project.copy(
            imagePaths = updatedPaths,
            referenceImageIndex = refIndex
        )
        updateProject(updatedProject)
        updatedProject
    }

    fun updateProject(project: OrbitProject) {
        val current = _projects.value.toMutableList()
        val index = current.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            current[index] = project
        } else {
            current.add(0, project)
        }
        _projects.value = current.sortedByDescending { it.createdAt }
        scope.launch { saveProjectsSync() }
    }

    fun deleteProject(projectId: String): Boolean {
        val current = _projects.value.toMutableList()
        val removed = current.removeAll { it.id == projectId }
        if (removed) {
            _projects.value = current
            scope.launch {
                saveProjectsSync()
                val folder = File(projectsDir, projectId)
                if (folder.exists()) {
                    folder.deleteRecursively()
                }
            }
        }
        return removed
    }

    /**
     * Generates a sample series of 36 computational orbit perspectives
     * depicting a geometric sculpture/architectural light structure.
     * This guarantees immediate testability of multi-exposure fusion right away!
     */
    suspend fun createSampleProject(): OrbitProject = withContext(Dispatchers.IO) {
        val projectId = UUID.randomUUID().toString()
        val projectFolder = File(projectsDir, projectId).apply { mkdirs() }
        val samplePaths = mutableListOf<String>()
        val count = 36

        val width = 720
        val height = 720
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in 0 until count) {
            val angleDeg = (i * 360f / count)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Deep studio background with subtle vignette
            canvas.drawColor(Color.rgb(18, 22, 26))

            // Draw architectural subject with dynamic orbital rotation perspective
            val centerX = width / 2f
            val centerY = height / 2f

            val camDist = 220f
            val shiftX = (cos(angleRad) * 45f).toFloat()
            val shiftY = (sin(angleRad) * 25f).toFloat()

            // Concentric structure and sculpture facets
            for (ring in 1..4) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f + ring * 1.5f
                paint.color = Color.argb(
                    180,
                    (40 + ring * 25).coerceAtMost(255),
                    (160 + ring * 20).coerceAtMost(255),
                    (220 + ring * 8).coerceAtMost(255)
                )

                val rx = 100f + ring * 45f + shiftX * (ring * 0.3f)
                val ry = 70f + ring * 35f + shiftY * (ring * 0.3f)
                canvas.drawOval(
                    centerX - rx, centerY - ry,
                    centerX + rx, centerY + ry,
                    paint
                )
            }

            // Central motif core
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(78, 235, 251)
            canvas.drawCircle(centerX + shiftX, centerY + shiftY, 28f, paint)

            paint.color = Color.rgb(255, 255, 255)
            canvas.drawCircle(centerX + shiftX, centerY + shiftY, 14f, paint)

            // Dynamic light spire lines
            paint.strokeWidth = 2.5f
            paint.color = Color.argb(140, 245, 158, 11)
            for (k in 0 until 6) {
                val pAngle = Math.toRadians((angleDeg + k * 60).toDouble())
                val endX = centerX + shiftX + (cos(pAngle) * 260f).toFloat()
                val endY = centerY + shiftY + (sin(pAngle) * 260f).toFloat()
                canvas.drawLine(centerX + shiftX, centerY + shiftY, endX, endY, paint)
            }

            val file = File(projectFolder, "sample_orbit_${i + 1}.jpg")
            BitmapUtils.saveBitmapToFile(bitmap, file, quality = 90)
            bitmap.recycle()
            samplePaths.add(file.absolutePath)
        }

        val project = OrbitProject(
            id = projectId,
            name = "Demo: Orbit Lichtskulptur",
            mode = OrbitCaptureMode.ORBIT,
            targetCount = 36,
            imagePaths = samplePaths,
            referenceImageIndex = 18,
            abstractionLevel = 50f,
            blendMode = OrbitBlendMode.WEIGHTED_AVERAGE
        )

        updateProject(project)
        project
    }
}
