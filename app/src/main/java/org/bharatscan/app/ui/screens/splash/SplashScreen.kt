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
package org.bharatscan.app.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.bharatscan.app.R
import org.bharatscan.app.ui.components.BrandTitle
import org.bharatscan.app.ui.theme.BharatNavy
import org.bharatscan.app.ui.theme.BharatWhite

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
    ) {
        DottedBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BrandTitle(height = 22.dp)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChakraLoader(modifier = Modifier.size(120.dp))
                Text(
                    text = stringResource(R.string.loading_bharatscan),
                    color = BharatNavy,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.securing_documents),
                    color = BharatNavy.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                )
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(4.dp)
                        .background(BharatNavy, shape = MaterialTheme.shapes.small)
                )
            }

            Text(
                text = "MADE WITH ❤ IN INDIA.",
                color = BharatNavy.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
            )
        }
    }
}

@Composable
private fun DottedBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 28.dp.toPx()
        val radius = 1.1.dp.toPx()
        val dotColor = BharatNavy.copy(alpha = 0.1f)
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(dotColor, radius, androidx.compose.ui.geometry.Offset(x, y))
                y += spacing
            }
            x += spacing
        }
    }
}

@Composable
private fun ChakraLoader(modifier: Modifier = Modifier) {
    val rotation by rememberInfiniteTransition(label = "chakra")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing)
            ),
            label = "rotation"
        )
    Canvas(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
    ) {
        val stroke = 3.dp.toPx()
        val outer = size.minDimension / 2f
        val hub = outer * 0.09f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = BharatNavy,
            radius = outer - stroke,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawCircle(
            color = BharatNavy,
            radius = hub,
            center = center
        )
        val spokeCount = 24
        for (i in 0 until spokeCount) {
            val angle = (2 * Math.PI / spokeCount) * i
            val start = androidx.compose.ui.geometry.Offset(
                x = center.x + hub * kotlin.math.cos(angle).toFloat(),
                y = center.y + hub * kotlin.math.sin(angle).toFloat()
            )
            val end = androidx.compose.ui.geometry.Offset(
                x = center.x + (outer - stroke) * kotlin.math.cos(angle).toFloat(),
                y = center.y + (outer - stroke) * kotlin.math.sin(angle).toFloat()
            )
            drawLine(
                color = BharatNavy,
                start = start,
                end = end,
                strokeWidth = stroke
            )
        }
    }
}
