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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.bharatscan.app.R
import org.bharatscan.app.ui.theme.*

enum class BackgroundPattern {
    DEFAULT,
    AJRAKH,
    JALI
}

enum class BackgroundTheme {
    DEFAULT,
    HERITAGE_HOME
}

@Composable
fun DigitalBharatBackground(
    pattern: BackgroundPattern = BackgroundPattern.DEFAULT,
    theme: BackgroundTheme = BackgroundTheme.DEFAULT,
    showChakra: Boolean = true,
    ajrakhDots: Boolean = true,
    backgroundImageRes: Int? = null,
    backgroundImageContentScale: ContentScale = ContentScale.FillBounds,
    backgroundImageAlignment: Alignment = Alignment.Center,
    backgroundImageTint: Color? = null,
    backgroundImageTintAlpha: Float = 0.12f,
    backgroundImageScale: Float = 1f,
    showTopStripe: Boolean = true,
    useDecorations: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val rotation by rememberInfiniteTransition(label = "chakraWatermark")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(38000, easing = LinearEasing)
            ),
            label = "chakraRotation"
        )
    val bandDrift by rememberInfiniteTransition(label = "bandDrift")
        .animateFloat(
            initialValue = -18f,
            targetValue = 18f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bandDriftValue"
        )
    val ribbonPulse by rememberInfiniteTransition(label = "ribbonPulse")
        .animateFloat(
            initialValue = 0.25f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ribbonPulseValue"
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (backgroundImageRes == null) {
                    Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = when (theme) {
                                BackgroundTheme.DEFAULT -> listOf(
                                    AppBackgroundTop,
                                    AppBackgroundMid,
                                    AppBackgroundBottom
                                )
                                BackgroundTheme.HERITAGE_HOME -> listOf(
                                    Color(0xFFF8E9D6),
                                    Color(0xFFF4E7D9),
                                    Color(0xFFF8F2EA)
                                )
                            }
                        )
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (backgroundImageRes != null) {
            Image(
                painter = painterResource(id = backgroundImageRes),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(
                        scaleX = backgroundImageScale,
                        scaleY = backgroundImageScale
                    ),
                contentScale = backgroundImageContentScale,
                alignment = backgroundImageAlignment
            )
            if (backgroundImageTint != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(backgroundImageTint.copy(alpha = backgroundImageTintAlpha))
                )
            }
        }
        if (useDecorations && theme == BackgroundTheme.HERITAGE_HOME) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val heroHeight = size.height * 0.45f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BharatSaffron.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = heroHeight
                    ),
                    size = Size(size.width, heroHeight)
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            BharatSaffron.copy(alpha = 0.12f),
                            BharatWhite.copy(alpha = 0.1f),
                            BharatGreen.copy(alpha = 0.12f)
                        )
                    ),
                    topLeft = Offset(0f, heroHeight * 0.6f),
                    size = Size(size.width, heroHeight * 0.18f)
                )
            }
        }
        if (showTopStripe) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                BharatSaffron.copy(alpha = 0.7f),
                                BharatWhite.copy(alpha = 0.45f),
                                BharatGreen.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .graphicsLayer { alpha = ribbonPulse }
            )
        }
        if (useDecorations) {
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
                    topLeft = androidx.compose.ui.geometry.Offset(
                        -size.width * 0.1f + bandDrift,
                        size.height * 0.18f
                    ),
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
                    topLeft = androidx.compose.ui.geometry.Offset(
                        -size.width * 0.1f - bandDrift,
                        size.height * 0.72f
                    ),
                    size = androidx.compose.ui.geometry.Size(size.width * 1.1f, bandHeight * 0.9f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(120f, 120f)
                )
            }
            when (pattern) {
                BackgroundPattern.DEFAULT -> SubtleWeaveLines()
                BackgroundPattern.AJRAKH -> AjrakhPattern(showDots = ajrakhDots)
                BackgroundPattern.JALI -> JaliPattern()
            }
        }
        if (showChakra) {
            Icon(
                imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(
                    id = R.drawable.ashoka_chakra_loader
                ),
                contentDescription = null,
                tint = BharatChakra.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(280.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
        overlay()
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
fun AjrakhPattern(showDots: Boolean = true) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val tile = 108f
        val gap = 10f
        val cols = (size.width / tile).toInt() + 3
        val rows = (size.height / tile).toInt() + 3
        val startX = -tile
        val startY = -tile
        val palette = listOf(
            BharatSaffron.copy(alpha = 0.12f),
            BharatGreen.copy(alpha = 0.12f),
            BharatNavy.copy(alpha = 0.10f),
            BharatWhite.copy(alpha = 0.14f)
        )

        for (row in 0..rows) {
            for (col in 0..cols) {
                val x = startX + col * tile
                val y = startY + row * tile
                val baseColor = palette[(row + col) % palette.size]
                drawRoundRect(
                    color = baseColor,
                    topLeft = Offset(x + gap, y + gap),
                    size = Size(tile - gap * 2, tile - gap * 2),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                val center = Offset(x + tile / 2, y + tile / 2)
                val petalRadius = tile * 0.22f
                drawCircle(
                    color = BharatWhite.copy(alpha = 0.18f),
                    radius = petalRadius,
                    center = center
                )
                val spoke = tile * 0.32f
                val spokeColor = BharatSaffron.copy(alpha = 0.18f)
                val directions = listOf(
                    Offset(1f, 0f),
                    Offset(-1f, 0f),
                    Offset(0f, 1f),
                    Offset(0f, -1f),
                    Offset(0.7f, 0.7f),
                    Offset(-0.7f, 0.7f),
                    Offset(0.7f, -0.7f),
                    Offset(-0.7f, -0.7f)
                )
                directions.forEach { dir ->
                    drawLine(
                        color = spokeColor,
                        start = center,
                        end = Offset(center.x + dir.x * spoke, center.y + dir.y * spoke),
                        strokeWidth = 2.4f
                    )
                }
                if (showDots) {
                    drawCircle(
                        color = BharatChakra.copy(alpha = 0.14f),
                        radius = tile * 0.08f,
                        center = center
                    )
                }
            }
        }

        val gridSpacing = tile
        val lineColor = BharatWhite.copy(alpha = 0.22f)
        val height = size.height
        val width = size.width
        var x = -height
        while (x <= width + height) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x + height, height),
                strokeWidth = 2f
            )
            x += gridSpacing
        }
        var y = -height
        while (y <= width + height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(width, y + width),
                strokeWidth = 2f
            )
            y += gridSpacing
        }
        drawRect(color = BharatWhite.copy(alpha = 0.3f))
    }
}

@Composable
fun JaliPattern() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 42f
        val radius = 7f
        val cols = (size.width / spacing).toInt() + 2
        val rows = (size.height / spacing).toInt() + 2
        for (row in 0..rows) {
            val y = row * spacing
            val offset = if (row % 2 == 0) 0f else spacing / 2
            for (col in 0..cols) {
                val x = col * spacing + offset
                drawCircle(
                    color = BharatNavy.copy(alpha = 0.045f),
                    radius = radius,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = BharatChakra.copy(alpha = 0.04f),
                    radius = radius * 0.6f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
fun RotatingChakraDotsOverlay() {
    val rotation by rememberInfiniteTransition(label = "settingsChakra")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(26000, easing = LinearEasing)
            ),
            label = "settingsChakraRotation"
        )
    val dotPulse by rememberInfiniteTransition(label = "settingsDots")
        .animateFloat(
            initialValue = 0.12f,
            targetValue = 0.26f,
            animationSpec = infiniteRepeatable(
                animation = tween(4200, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "settingsDotsPulse"
        )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val spacing = 54f
            val dotRadius = 4.5f
            val cols = (size.width / spacing).toInt() + 2
            val rows = (size.height / spacing).toInt() + 2
            for (row in 0..rows) {
                val y = row * spacing
                val offset = if (row % 2 == 0) 0f else spacing / 2
                for (col in 0..cols) {
                    val x = col * spacing + offset
                    drawCircle(
                        color = BharatChakra.copy(alpha = dotPulse),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }
        Icon(
            imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(
                id = R.drawable.ashoka_chakra_loader
            ),
            contentDescription = null,
            tint = BharatChakra.copy(alpha = 0.14f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 18.dp)
                .size(140.dp)
                .graphicsLayer { rotationZ = rotation }
        )
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


