package com.example.engine

/**
 * Limited memory cache for decoded and center-cropped preview pixel buffers.
 * Avoids repeated disk reads and JPEG decompressions of 30-100 source photos on slider adjustments.
 * Strictly bound to current project ID, automatically purged upon project change or low memory.
 * Never used for final high-resolution exports.
 */
object OrbitPreviewCache {
    // 32 MB max memory limit for preview cache (holds ~16-20 frames of 720x720 ARGB)
    private const val MAX_CACHE_SIZE_BYTES = 32 * 1024 * 1024

    private var currentProjectId: String? = null
    private var currentSizeBytes: Int = 0

    // Access-order LinkedHashMap for LRU cache policy without Android SDK stub dependencies
    private val cache = object : LinkedHashMap<String, IntArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, IntArray>?): Boolean {
            return currentSizeBytes > MAX_CACHE_SIZE_BYTES
        }
    }

    @Synchronized
    fun get(projectId: String, path: String, width: Int, height: Int): IntArray? {
        if (projectId != currentProjectId) {
            clear()
            currentProjectId = projectId
            return null
        }
        val key = "${path}_${width}x$height"
        return cache[key]
    }

    @Synchronized
    fun put(projectId: String, path: String, width: Int, height: Int, pixels: IntArray) {
        if (projectId != currentProjectId) {
            clear()
            currentProjectId = projectId
        }
        val key = "${path}_${width}x$height"
        val existing = cache.remove(key)
        if (existing != null) {
            currentSizeBytes -= existing.size * 4
        }
        cache[key] = pixels
        currentSizeBytes += pixels.size * 4

        while (currentSizeBytes > MAX_CACHE_SIZE_BYTES && cache.isNotEmpty()) {
            val iterator = cache.iterator()
            if (iterator.hasNext()) {
                val entry = iterator.next()
                currentSizeBytes -= entry.value.size * 4
                iterator.remove()
            } else {
                break
            }
        }
    }

    fun getPreviewPixels(projectId: String, path: String, width: Int, height: Int): IntArray? =
        get(projectId, path, width, height)

    fun putPreviewPixels(projectId: String, path: String, width: Int, height: Int, pixels: IntArray) =
        put(projectId, path, width, height, pixels)

    @Synchronized
    fun evictProject(projectId: String) {
        if (currentProjectId == projectId) {
            clear()
        }
    }

    @Synchronized
    fun clear() {
        cache.clear()
        currentSizeBytes = 0
    }

    @Synchronized
    fun trimMemory() {
        val targetSize = MAX_CACHE_SIZE_BYTES / 2
        while (currentSizeBytes > targetSize && cache.isNotEmpty()) {
            val iterator = cache.iterator()
            if (iterator.hasNext()) {
                val entry = iterator.next()
                currentSizeBytes -= entry.value.size * 4
                iterator.remove()
            } else {
                break
            }
        }
    }

    @Synchronized
    fun cachedCount(): Int = cache.size
}
