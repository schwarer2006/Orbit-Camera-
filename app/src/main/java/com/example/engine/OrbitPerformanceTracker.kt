package com.example.engine

import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

/**
 * Lightweight, strictly local in-memory performance and battery diagnostics tracker.
 * No telemetry, no network transmission, no persistent logging.
 */
object OrbitPerformanceTracker {

    data class DiagnosticsSnapshot(
        val lastRenderMs: Long = 0L,
        val lastCaptureMs: Long = 0L,
        val lastThumbnailMs: Long = 0L,
        val lastRegistrationMs: Long = 0L,
        val lastExportMs: Long = 0L,
        val jobsStarted: Int = 0,
        val jobsCanceled: Int = 0,
        val cacheHits: Int = 0,
        val cacheMisses: Int = 0,
        val cacheHitRatePercent: Int = 0,
        val usedHeapMb: Int = 0,
        val maxHeapMb: Int = 0
    )

    private val startedJobs = AtomicInteger(0)
    private val canceledJobs = AtomicInteger(0)
    private val activeJobs = AtomicInteger(0)
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)

    @Volatile var lastRenderMs: Long = 0L
    @Volatile var lastCaptureMs: Long = 0L
    @Volatile var lastThumbnailMs: Long = 0L
    @Volatile var lastRegistrationMs: Long = 0L
    @Volatile var lastExportMs: Long = 0L

    fun recordJobStarted() {
        startedJobs.incrementAndGet()
        activeJobs.incrementAndGet()
    }

    fun recordJobCanceled() {
        activeJobs.decrementAndGet()
        canceledJobs.incrementAndGet()
    }

    fun recordRender(durationMs: Long) {
        activeJobs.decrementAndGet()
        lastRenderMs = durationMs
    }

    fun recordCapture(durationMs: Long) {
        lastCaptureMs = durationMs
    }

    fun recordThumbnail(durationMs: Long) {
        lastThumbnailMs = durationMs
    }

    fun recordRegistration(durationMs: Long) {
        lastRegistrationMs = durationMs
    }

    fun recordExport(durationMs: Long) {
        lastExportMs = durationMs
    }

    fun recordCacheHit() {
        cacheHits.incrementAndGet()
    }

    fun recordCacheMiss() {
        cacheMisses.incrementAndGet()
    }

    fun getSnapshot(): DiagnosticsSnapshot {
        val runtime = Runtime.getRuntime()
        val usedMemMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)).toInt()
        val maxMemMb = (runtime.maxMemory() / (1024L * 1024L)).toInt()

        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        val total = hits + misses
        val ratePercent = if (total > 0) ((hits.toDouble() / total) * 100).roundToInt() else 0

        return DiagnosticsSnapshot(
            lastRenderMs = lastRenderMs,
            lastCaptureMs = lastCaptureMs,
            lastThumbnailMs = lastThumbnailMs,
            lastRegistrationMs = lastRegistrationMs,
            lastExportMs = lastExportMs,
            jobsStarted = startedJobs.get(),
            jobsCanceled = canceledJobs.get(),
            cacheHits = hits,
            cacheMisses = misses,
            cacheHitRatePercent = ratePercent,
            usedHeapMb = usedMemMb,
            maxHeapMb = maxMemMb
        )
    }

    fun reset() {
        startedJobs.set(0)
        canceledJobs.set(0)
        activeJobs.set(0)
        cacheHits.set(0)
        cacheMisses.set(0)
        lastRenderMs = 0L
        lastCaptureMs = 0L
        lastThumbnailMs = 0L
        lastRegistrationMs = 0L
        lastExportMs = 0L
    }
}

typealias DiagnosticsSnapshot = OrbitPerformanceTracker.DiagnosticsSnapshot
