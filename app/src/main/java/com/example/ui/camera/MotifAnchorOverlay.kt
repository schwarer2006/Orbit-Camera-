package com.example.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.model.OrbitCaptureMode
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon

@Composable
fun CameraOverlay(
    mode: OrbitCaptureMode,
    isGridVisible: Boolean,
    isAnchorVisible: Boolean,
    anchorX: Float,
    anchorY: Float,
    onTap: (normX: Float, normY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("camera_overlay")
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normX = offset.x / size.width
                    val normY = offset.y / size.height
                    onTap(normX, normY)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Grid
            if (isGridVisible) {
                val gridColor = Color.White.copy(alpha = 0.22f)
                val strokeWidth = 1.2f

                // Vertical lines
                drawLine(
                    color = gridColor,
                    start = Offset(width / 3f, 0f),
                    end = Offset(width / 3f, height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = gridColor,
                    start = Offset(width * 2f / 3f, 0f),
                    end = Offset(width * 2f / 3f, height),
                    strokeWidth = strokeWidth
                )

                // Horizontal lines
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height / 3f),
                    end = Offset(width, height / 3f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height * 2f / 3f),
                    end = Offset(width, height * 2f / 3f),
                    strokeWidth = strokeWidth
                )

                // If in Orbit mode, draw subtle concentric orbital rings in the center
                if (mode == OrbitCaptureMode.ORBIT) {
                    val orbitColor = CyanAccent.copy(alpha = 0.18f)
                    val center = Offset(width / 2f, height / 2f)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)

                    drawCircle(
                        color = orbitColor,
                        radius = width * 0.25f,
                        center = center,
                        style = Stroke(width = 1.5f, pathEffect = dashEffect)
                    )
                    drawCircle(
                        color = orbitColor,
                        radius = width * 0.38f,
                        center = center,
                        style = Stroke(width = 1.5f, pathEffect = dashEffect)
                    )
                }
            }

            // 2. Motif Anchor (Reticle)
            if (isAnchorVisible) {
                val px = anchorX * width
                val py = anchorY * height
                val reticleRadius = 38f
                val crossLength = 18f

                // Outer glowing circle
                drawCircle(
                    color = CyanNeon.copy(alpha = 0.9f),
                    radius = reticleRadius,
                    center = Offset(px, py),
                    style = Stroke(width = 2.5f)
                )

                // Inner core dot
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(px, py)
                )

                // 4 Crosshair lines outside the circle
                val reticleColor = CyanAccent
                val lineStroke = 2f

                // Top
                drawLine(
                    color = reticleColor,
                    start = Offset(px, py - reticleRadius - 4f),
                    end = Offset(px, py - reticleRadius - 4f - crossLength),
                    strokeWidth = lineStroke
                )
                // Bottom
                drawLine(
                    color = reticleColor,
                    start = Offset(px, py + reticleRadius + 4f),
                    end = Offset(px, py + reticleRadius + 4f + crossLength),
                    strokeWidth = lineStroke
                )
                // Left
                drawLine(
                    color = reticleColor,
                    start = Offset(px - reticleRadius - 4f, py),
                    end = Offset(px - reticleRadius - 4f - crossLength, py),
                    strokeWidth = lineStroke
                )
                // Right
                drawLine(
                    color = reticleColor,
                    start = Offset(px + reticleRadius + 4f, py),
                    end = Offset(px + reticleRadius + 4f + crossLength, py),
                    strokeWidth = lineStroke
                )
            }
        }
    }
}
