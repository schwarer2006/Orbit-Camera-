package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import com.example.engine.BitmapUtils
import com.example.engine.OrbitPreviewCache
import com.example.model.OrbitArtStyle
import com.example.model.OrbitBlendMode
import com.example.model.OrbitCaptureMode
import com.example.model.OrbitCompositionStrategy
import com.example.model.OrbitProject
import com.example.model.OrbitStyleParameters
import com.example.model.OrbitWorkspaceMode
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
                        vibrance = paramsObj.optDouble("vibrance", def.vibrance.toDouble()).toFloat(),
                        contrast = paramsObj.optDouble("contrast", def.contrast.toDouble()).toFloat(),
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
                        blendStrength = paramsObj.optDouble("blendStrength", def.blendStrength.toDouble()).toFloat(),
                        artisticSpreadEnabled = paramsObj.optBoolean("artisticSpreadEnabled", def.artisticSpreadEnabled),
                        maskEnabled = paramsObj.optBoolean("maskEnabled", def.maskEnabled),
                        maskRadius = paramsObj.optDouble("maskRadius", def.maskRadius.toDouble()).toFloat(),
                        maskFeather = paramsObj.optDouble("maskFeather", def.maskFeather.toDouble()).toFloat(),
                        maskIntensity = paramsObj.optDouble("maskIntensity", def.maskIntensity.toDouble()).toFloat()
                    )
                } else {
                    artStyle.defaultParameters.copy(
                        abstraction = obj.optDouble("abstractionLevel", def.abstraction.toDouble()).toFloat()
                    )
                }

                val disabledPathsSet = mutableSetOf<String>()
                val disabledArr = obj.optJSONArray("disabledImagePaths")
                if (disabledArr != null) {
                    for (k in 0 until disabledArr.length()) {
                        disabledPathsSet.add(disabledArr.getString(k))
                    }
                }

                // V1.1 Creative Fusion Expansion fields
                val customOrderList = mutableListOf<String>()
                val customOrderArr = obj.optJSONArray("customImageOrder")
                if (customOrderArr != null) {
                    for (k in 0 until customOrderArr.length()) {
                        customOrderList.add(customOrderArr.getString(k))
                    }
                }

                val imageWeightsMap = mutableMapOf<String, Float>()
                val weightsObj = obj.optJSONObject("imageWeights")
                if (weightsObj != null) {
                    val keys = weightsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        imageWeightsMap[key] = weightsObj.optDouble(key, 1.0).toFloat().coerceIn(0f, 1f)
                    }
                }

                val layerOpacitiesMap = mutableMapOf<String, Float>()
                val opacitiesObj = obj.optJSONObject("layerOpacities")
                if (opacitiesObj != null) {
                    val keys = opacitiesObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        layerOpacitiesMap[key] = opacitiesObj.optDouble(key, 1.0).toFloat().coerceIn(0f, 1f)
                    }
                }

                val exposureMatch = obj.optBoolean("exposureMatchEnabled", false)
                val activeVariantId = obj.optString("activeVariantId", null).takeIf { it != "null" && it?.isNotBlank() == true }

                val variantsList = mutableListOf<com.example.model.OrbitVariant>()
                val variantsArr = obj.optJSONArray("variants")
                if (variantsArr != null) {
                    for (vIdx in 0 until variantsArr.length()) {
                        val vObj = variantsArr.getJSONObject(vIdx)
                        val vStyle = try {
                            OrbitArtStyle.valueOf(vObj.optString("artStyle", OrbitArtStyle.IN_THE_ROUND.name))
                        } catch (e: Exception) {
                            OrbitArtStyle.IN_THE_ROUND
                        }
                        val vStrategy = try {
                            OrbitCompositionStrategy.valueOf(vObj.optString("compositionStrategy", OrbitCompositionStrategy.SUBJECT_CENTERED.name))
                        } catch (e: Exception) {
                            OrbitCompositionStrategy.SUBJECT_CENTERED
                        }
                        val vBlend = OrbitBlendMode.fromString(vObj.optString("blendMode", OrbitBlendMode.WEIGHTED_AVERAGE.name))

                        val vDisabledSet = mutableSetOf<String>()
                        val vDisabledArr = vObj.optJSONArray("disabledImagePaths")
                        if (vDisabledArr != null) {
                            for (dk in 0 until vDisabledArr.length()) {
                                vDisabledSet.add(vDisabledArr.getString(dk))
                            }
                        }

                        val vOrderList = mutableListOf<String>()
                        val vOrderArr = vObj.optJSONArray("customImageOrder")
                        if (vOrderArr != null) {
                            for (ok in 0 until vOrderArr.length()) {
                                vOrderList.add(vOrderArr.getString(ok))
                            }
                        }

                        val vWeightsMap = mutableMapOf<String, Float>()
                        val vWeightsObj = vObj.optJSONObject("imageWeights")
                        if (vWeightsObj != null) {
                            val vKeys = vWeightsObj.keys()
                            while (vKeys.hasNext()) {
                                val vk = vKeys.next()
                                vWeightsMap[vk] = vWeightsObj.optDouble(vk, 1.0).toFloat().coerceIn(0f, 1f)
                            }
                        }

                        val vOpacitiesMap = mutableMapOf<String, Float>()
                        val vOpacitiesObj = vObj.optJSONObject("layerOpacities")
                        if (vOpacitiesObj != null) {
                            val vKeys = vOpacitiesObj.keys()
                            while (vKeys.hasNext()) {
                                val vk = vKeys.next()
                                vOpacitiesMap[vk] = vOpacitiesObj.optDouble(vk, 1.0).toFloat().coerceIn(0f, 1f)
                            }
                        }

                        val vParamsObj = vObj.optJSONObject("styleParams")
                        val vDef = vStyle.defaultParameters
                        val vParams = if (vParamsObj != null) {
                            OrbitStyleParameters(
                                abstraction = vParamsObj.optDouble("abstraction", vDef.abstraction.toDouble()).toFloat(),
                                subjectStability = vParamsObj.optDouble("subjectStability", vDef.subjectStability.toDouble()).toFloat(),
                                colorIntensity = vParamsObj.optDouble("colorIntensity", vDef.colorIntensity.toDouble()).toFloat(),
                                vibrance = vParamsObj.optDouble("vibrance", vDef.vibrance.toDouble()).toFloat(),
                                contrast = vParamsObj.optDouble("contrast", vDef.contrast.toDouble()).toFloat(),
                                edgeEmphasis = vParamsObj.optDouble("edgeEmphasis", vDef.edgeEmphasis.toDouble()).toFloat(),
                                backgroundDissolve = vParamsObj.optDouble("backgroundDissolve", vDef.backgroundDissolve.toDouble()).toFloat(),
                                echoStrength = vParamsObj.optDouble("echoStrength", vDef.echoStrength.toDouble()).toFloat(),
                                echoCount = vParamsObj.optDouble("echoCount", vDef.echoCount.toDouble()).toFloat(),
                                perspectiveSpread = vParamsObj.optDouble("perspectiveSpread", vDef.perspectiveSpread.toDouble()).toFloat(),
                                colorDensity = vParamsObj.optDouble("colorDensity", vDef.colorDensity.toDouble()).toFloat(),
                                warmth = vParamsObj.optDouble("warmth", vDef.warmth.toDouble()).toFloat(),
                                lightGlow = vParamsObj.optDouble("lightGlow", vDef.lightGlow.toDouble()).toFloat(),
                                shadowDepth = vParamsObj.optDouble("shadowDepth", vDef.shadowDepth.toDouble()).toFloat(),
                                softness = vParamsObj.optDouble("softness", vDef.softness.toDouble()).toFloat(),
                                motionStrength = vParamsObj.optDouble("motionStrength", vDef.motionStrength.toDouble()).toFloat(),
                                detailDensity = vParamsObj.optDouble("detailDensity", vDef.detailDensity.toDouble()).toFloat(),
                                atmosphere = vParamsObj.optDouble("atmosphere", vDef.atmosphere.toDouble()).toFloat(),
                                blendStrength = vParamsObj.optDouble("blendStrength", vDef.blendStrength.toDouble()).toFloat(),
                                artisticSpreadEnabled = vParamsObj.optBoolean("artisticSpreadEnabled", vDef.artisticSpreadEnabled),
                                maskEnabled = vParamsObj.optBoolean("maskEnabled", vDef.maskEnabled),
                                maskRadius = vParamsObj.optDouble("maskRadius", vDef.maskRadius.toDouble()).toFloat(),
                                maskFeather = vParamsObj.optDouble("maskFeather", vDef.maskFeather.toDouble()).toFloat(),
                                maskIntensity = vParamsObj.optDouble("maskIntensity", vDef.maskIntensity.toDouble()).toFloat()
                            )
                        } else vDef

                        variantsList.add(
                            com.example.model.OrbitVariant(
                                id = vObj.getString("id"),
                                name = vObj.getString("name"),
                                createdAt = vObj.optLong("createdAt", System.currentTimeMillis()),
                                artStyle = vStyle,
                                compositionStrategy = vStrategy,
                                blendMode = vBlend,
                                styleParams = vParams,
                                referenceImageIndex = vObj.optInt("referenceImageIndex", 0),
                                disabledImagePaths = vDisabledSet,
                                customImageOrder = vOrderList,
                                imageWeights = vWeightsMap,
                                layerOpacities = vOpacitiesMap,
                                exposureMatchEnabled = vObj.optBoolean("exposureMatchEnabled", false),
                                anchorX = vObj.optDouble("anchorX", 0.5).toFloat(),
                                anchorY = vObj.optDouble("anchorY", 0.5).toFloat(),
                                artisticSpreadEnabled = vObj.optBoolean("artisticSpreadEnabled", false),
                                maskEnabled = vObj.optBoolean("maskEnabled", false),
                                maskRadius = vObj.optDouble("maskRadius", 0.35).toFloat(),
                                maskFeather = vObj.optDouble("maskFeather", 0.25).toFloat(),
                                maskIntensity = vObj.optDouble("maskIntensity", 0.70).toFloat(),
                                offsetX = vObj.optDouble("offsetX", 0.0).toFloat(),
                                offsetY = vObj.optDouble("offsetY", 0.0).toFloat()
                            )
                        )
                    }
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
                    blendMode = OrbitBlendMode.fromString(obj.optString("blendMode", OrbitBlendMode.WEIGHTED_AVERAGE.name)),
                    artStyle = artStyle,
                    styleParams = styleParams,
                    lastRenderedPath = obj.optString("lastRenderedPath", null).takeIf { it != "null" && it?.isNotBlank() == true },
                    ghostOverlayEnabled = obj.optBoolean("ghostOverlayEnabled", false),
                    ghostOverlayAlpha = obj.optDouble("ghostOverlayAlpha", 0.45).toFloat(),
                    ghostReferenceMode = obj.optString("ghostReferenceMode", "PREVIOUS"),
                    isIntervalMode = obj.optBoolean("isIntervalMode", false),
                    intervalSeconds = obj.optInt("intervalSeconds", 3),
                    compositionStrategy = try {
                        OrbitCompositionStrategy.valueOf(obj.optString("compositionStrategy", OrbitCompositionStrategy.SUBJECT_CENTERED.name))
                    } catch (e: Exception) {
                        OrbitCompositionStrategy.SUBJECT_CENTERED
                    },
                    workspaceMode = try {
                        OrbitWorkspaceMode.valueOf(obj.optString("workspaceMode", OrbitWorkspaceMode.SIMPLE.name))
                    } catch (e: Exception) {
                        OrbitWorkspaceMode.SIMPLE
                    },
                    anchorX = obj.optDouble("anchorX", 0.5).toFloat(),
                    anchorY = obj.optDouble("anchorY", 0.5).toFloat(),
                    artisticSpreadEnabled = obj.optBoolean("artisticSpreadEnabled", false),
                    disabledImagePaths = disabledPathsSet,
                    maskEnabled = obj.optBoolean("maskEnabled", false),
                    maskRadius = obj.optDouble("maskRadius", 0.35).toFloat(),
                    maskFeather = obj.optDouble("maskFeather", 0.25).toFloat(),
                    maskIntensity = obj.optDouble("maskIntensity", 0.70).toFloat(),
                    customImageOrder = customOrderList,
                    imageWeights = imageWeightsMap,
                    layerOpacities = layerOpacitiesMap,
                    exposureMatchEnabled = exposureMatch,
                    variants = variantsList,
                    activeVariantId = activeVariantId
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
                        put("vibrance", p.styleParams.vibrance.toDouble())
                        put("contrast", p.styleParams.contrast.toDouble())
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
                        put("artisticSpreadEnabled", p.styleParams.artisticSpreadEnabled)
                        put("maskEnabled", p.styleParams.maskEnabled)
                        put("maskRadius", p.styleParams.maskRadius.toDouble())
                        put("maskFeather", p.styleParams.maskFeather.toDouble())
                        put("maskIntensity", p.styleParams.maskIntensity.toDouble())
                    }
                    put("styleParams", paramsObj)

                    put("lastRenderedPath", p.lastRenderedPath ?: JSONObject.NULL)
                    put("ghostOverlayEnabled", p.ghostOverlayEnabled)
                    put("ghostOverlayAlpha", p.ghostOverlayAlpha.toDouble())
                    put("ghostReferenceMode", p.ghostReferenceMode)
                    put("isIntervalMode", p.isIntervalMode)
                    put("intervalSeconds", p.intervalSeconds)
                    put("compositionStrategy", p.compositionStrategy.name)
                    put("workspaceMode", p.workspaceMode.name)
                    put("anchorX", p.anchorX.toDouble())
                    put("anchorY", p.anchorY.toDouble())
                    put("artisticSpreadEnabled", p.artisticSpreadEnabled)
                    val disabledArr = JSONArray()
                    p.disabledImagePaths.forEach { disabledArr.put(it) }
                    put("disabledImagePaths", disabledArr)
                    put("maskEnabled", p.maskEnabled)
                    put("maskRadius", p.maskRadius.toDouble())
                    put("maskFeather", p.maskFeather.toDouble())
                    put("maskIntensity", p.maskIntensity.toDouble())

                    // V1.1 Creative Fusion Expansion fields
                    val orderArr = JSONArray()
                    p.customImageOrder.forEach { orderArr.put(it) }
                    put("customImageOrder", orderArr)

                    val weightsObj = JSONObject()
                    p.imageWeights.forEach { (k, v) -> weightsObj.put(k, v.toDouble()) }
                    put("imageWeights", weightsObj)

                    val opacitiesObj = JSONObject()
                    p.layerOpacities.forEach { (k, v) -> opacitiesObj.put(k, v.toDouble()) }
                    put("layerOpacities", opacitiesObj)

                    put("exposureMatchEnabled", p.exposureMatchEnabled)
                    put("activeVariantId", p.activeVariantId ?: JSONObject.NULL)

                    val variantsArr = JSONArray()
                    p.variants.forEach { v ->
                        val vObj = JSONObject().apply {
                            put("id", v.id)
                            put("name", v.name)
                            put("createdAt", v.createdAt)
                            put("artStyle", v.artStyle.name)
                            put("compositionStrategy", v.compositionStrategy.name)
                            put("blendMode", v.blendMode.name)
                            put("referenceImageIndex", v.referenceImageIndex)

                            val vDisabledArr = JSONArray()
                            v.disabledImagePaths.forEach { vDisabledArr.put(it) }
                            put("disabledImagePaths", vDisabledArr)

                            val vOrderArr = JSONArray()
                            v.customImageOrder.forEach { vOrderArr.put(it) }
                            put("customImageOrder", vOrderArr)

                            val vWeightsObj = JSONObject()
                            v.imageWeights.forEach { (vk, vv) -> vWeightsObj.put(vk, vv.toDouble()) }
                            put("imageWeights", vWeightsObj)

                            val vOpacitiesObj = JSONObject()
                            v.layerOpacities.forEach { (vk, vv) -> vOpacitiesObj.put(vk, vv.toDouble()) }
                            put("layerOpacities", vOpacitiesObj)

                            put("exposureMatchEnabled", v.exposureMatchEnabled)
                            put("anchorX", v.anchorX.toDouble())
                            put("anchorY", v.anchorY.toDouble())
                            put("artisticSpreadEnabled", v.artisticSpreadEnabled)
                            put("maskEnabled", v.maskEnabled)
                            put("maskRadius", v.maskRadius.toDouble())
                            put("maskFeather", v.maskFeather.toDouble())
                            put("maskIntensity", v.maskIntensity.toDouble())
                            put("offsetX", v.offsetX.toDouble())
                            put("offsetY", v.offsetY.toDouble())

                            val vParamsObj = JSONObject().apply {
                                put("abstraction", v.styleParams.abstraction.toDouble())
                                put("subjectStability", v.styleParams.subjectStability.toDouble())
                                put("colorIntensity", v.styleParams.colorIntensity.toDouble())
                                put("vibrance", v.styleParams.vibrance.toDouble())
                                put("contrast", v.styleParams.contrast.toDouble())
                                put("edgeEmphasis", v.styleParams.edgeEmphasis.toDouble())
                                put("backgroundDissolve", v.styleParams.backgroundDissolve.toDouble())
                                put("echoStrength", v.styleParams.echoStrength.toDouble())
                                put("echoCount", v.styleParams.echoCount.toDouble())
                                put("perspectiveSpread", v.styleParams.perspectiveSpread.toDouble())
                                put("colorDensity", v.styleParams.colorDensity.toDouble())
                                put("warmth", v.styleParams.warmth.toDouble())
                                put("lightGlow", v.styleParams.lightGlow.toDouble())
                                put("shadowDepth", v.styleParams.shadowDepth.toDouble())
                                put("softness", v.styleParams.softness.toDouble())
                                put("motionStrength", v.styleParams.motionStrength.toDouble())
                                put("detailDensity", v.styleParams.detailDensity.toDouble())
                                put("atmosphere", v.styleParams.atmosphere.toDouble())
                                put("blendStrength", v.styleParams.blendStrength.toDouble())
                                put("artisticSpreadEnabled", v.styleParams.artisticSpreadEnabled)
                                put("maskEnabled", v.styleParams.maskEnabled)
                                put("maskRadius", v.styleParams.maskRadius.toDouble())
                                put("maskFeather", v.styleParams.maskFeather.toDouble())
                                put("maskIntensity", v.styleParams.maskIntensity.toDouble())
                            }
                            put("styleParams", vParamsObj)
                        }
                        variantsArr.put(vObj)
                    }
                    put("variants", variantsArr)
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
        OrbitPreviewCache.evictProject(projectId)
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

    fun saveVariant(projectId: String, variant: com.example.model.OrbitVariant): com.example.model.OrbitVariant? {
        val project = getProject(projectId) ?: return null
        val updatedVariants = project.variants + variant
        val updatedProject = project.copy(
            variants = updatedVariants,
            activeVariantId = variant.id
        )
        updateProject(updatedProject)
        return variant
    }

    fun saveVariant(projectId: String, variantName: String): com.example.model.OrbitVariant? {
        val project = getProject(projectId) ?: return null
        val newVariant = com.example.model.OrbitVariant(
            name = variantName.ifBlank { "Variante ${project.variants.size + 1}" },
            artStyle = project.artStyle,
            compositionStrategy = project.compositionStrategy,
            blendMode = project.blendMode,
            styleParams = project.styleParams,
            referenceImageIndex = project.referenceImageIndex,
            disabledImagePaths = project.disabledImagePaths,
            customImageOrder = project.customImageOrder,
            imageWeights = project.imageWeights,
            layerOpacities = project.layerOpacities,
            exposureMatchEnabled = project.exposureMatchEnabled,
            anchorX = project.anchorX,
            anchorY = project.anchorY,
            artisticSpreadEnabled = project.artisticSpreadEnabled,
            maskEnabled = project.maskEnabled,
            maskRadius = project.maskRadius,
            maskFeather = project.maskFeather,
            maskIntensity = project.maskIntensity,
            offsetX = project.offsetX,
            offsetY = project.offsetY
        )
        val updatedVariants = project.variants + newVariant
        val updatedProject = project.copy(
            variants = updatedVariants,
            activeVariantId = newVariant.id
        )
        updateProject(updatedProject)
        return newVariant
    }

    fun applyVariant(projectId: String, variantId: String): OrbitProject? {
        val project = getProject(projectId) ?: return null
        val variant = project.variants.firstOrNull { it.id == variantId } ?: return null
        val updatedProject = project.copy(
            activeVariantId = variant.id,
            artStyle = variant.artStyle,
            compositionStrategy = variant.compositionStrategy,
            blendMode = variant.blendMode,
            styleParams = variant.styleParams,
            referenceImageIndex = variant.referenceImageIndex,
            disabledImagePaths = variant.disabledImagePaths,
            customImageOrder = variant.customImageOrder,
            imageWeights = variant.imageWeights,
            layerOpacities = variant.layerOpacities,
            exposureMatchEnabled = variant.exposureMatchEnabled,
            anchorX = variant.anchorX,
            anchorY = variant.anchorY,
            artisticSpreadEnabled = variant.artisticSpreadEnabled,
            maskEnabled = variant.maskEnabled,
            maskRadius = variant.maskRadius,
            maskFeather = variant.maskFeather,
            maskIntensity = variant.maskIntensity,
            offsetX = variant.offsetX,
            offsetY = variant.offsetY
        )
        updateProject(updatedProject)
        return updatedProject
    }

    fun duplicateVariant(projectId: String, variantId: String): com.example.model.OrbitVariant? {
        val project = getProject(projectId) ?: return null
        val existing = project.variants.firstOrNull { it.id == variantId } ?: return null
        val duplicate = existing.copy(
            id = UUID.randomUUID().toString(),
            name = "${existing.name} (Kopie)",
            createdAt = System.currentTimeMillis()
        )
        val updatedProject = project.copy(
            variants = project.variants + duplicate,
            activeVariantId = duplicate.id
        )
        updateProject(updatedProject)
        return duplicate
    }

    fun renameVariant(projectId: String, variantId: String, newName: String): OrbitProject? {
        val project = getProject(projectId) ?: return null
        val updated = project.variants.map {
            if (it.id == variantId) it.copy(name = newName.ifBlank { it.name }) else it
        }
        val updatedProject = project.copy(variants = updated)
        updateProject(updatedProject)
        return updatedProject
    }

    fun deleteVariant(projectId: String, variantId: String): OrbitProject? {
        val project = getProject(projectId) ?: return null
        val updatedVariants = project.variants.filter { it.id != variantId }
        val newActiveId = if (project.activeVariantId == variantId) updatedVariants.firstOrNull()?.id else project.activeVariantId
        val updatedProject = project.copy(
            variants = updatedVariants,
            activeVariantId = newActiveId
        )
        updateProject(updatedProject)
        return updatedProject
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
