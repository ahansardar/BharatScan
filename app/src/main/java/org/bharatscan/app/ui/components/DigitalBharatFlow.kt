/*
 * Copyright 2025-2026 Ahan Sardar
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.bharatscan.app.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.bharatscan.app.ui.theme.*

@Composable
fun DigitalBharatBackground(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppBackgroundTop,
                        AppBackgroundMid,
                        AppBackgroundBottom
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = size.minDimension * 0.7f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SaffronGlow.copy(alpha = 0.25f), Color.Transparent)
                ),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GreenGlow.copy(alpha = 0.22f), Color.Transparent)
                ),
                radius = radius * 0.85f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.9f)
            )
            val bandHeight = size.height * 0.12f
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BharatSaffron.copy(alpha = 0.08f),
                        BharatWhite.copy(alpha = 0.04f),
                        BharatGreen.copy(alpha = 0.08f)
                    )
                ),
                topLeft = androidx.compose.ui.geometry.Offset(-size.width * 0.1f, size.height * 0.18f),
                size = androidx.compose.ui.geometry.Size(size.width * 1.2f, bandHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(120f, 120f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BharatGreen.copy(alpha = 0.06f),
                        BharatWhite.copy(alpha = 0.03f),
                        BharatSaffron.copy(alpha = 0.06f)
                    )
                ),
                topLeft = androidx.compose.ui.geometry.Offset(-size.width * 0.1f, size.height * 0.72f),
                size = androidx.compose.ui.geometry.Size(size.width * 1.1f, bandHeight * 0.9f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(120f, 120f)
            )
        }
        SubtleWeaveLines()
        content()
    }
}

@Composable
fun SubtleWeaveLines() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 140f
        val stroke = 1.2f
        val height = size.height
        val width = size.width
        val start = -height
        val end = width + height

        var x = start
        while (x <= end) {
            drawLine(
                color = BharatNavy.copy(alpha = 0.045f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x - height, height),
                strokeWidth = stroke
            )
            x += spacing
        }

        var y = start
        while (y <= end) {
            drawLine(
                color = BharatSaffron.copy(alpha = 0.035f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(width, y - width),
                strokeWidth = stroke
            )
            y += spacing
        }
    }
}

@Composable
fun ScannerLogo(
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    LaunchedEffect(Unit) {
        onAnimationFinished()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(BharatWhite, NavyGlow.copy(alpha = 0.45f))
                )
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Document Icon
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.7f),
            tint = BharatNavy
        )

        // Scanning Line with Pulse
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = scanLineY * 70.dp.toPx()
                }
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BharatSaffron.copy(alpha = 0.85f),
                            BharatChakra.copy(alpha = 0.85f),
                            BharatGreen.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}


