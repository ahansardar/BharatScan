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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import org.bharatscan.app.R
import org.bharatscan.app.ui.theme.*

@Composable
fun MainActionButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    iconDescription: String? = null,
    enabled: Boolean = true,
    ) {
    val shape = RoundedCornerShape(18.dp)
    val gradient = Brush.horizontalGradient(
        colors = listOf(BharatSaffron, BharatSaffronDeep)
    )
    val contentAlpha = if (enabled) 1f else 0.5f
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        shadowElevation = 10.dp,
        shape = shape,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .background(gradient, shape)
                .alpha(contentAlpha)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    icon,
                    contentDescription = iconDescription,
                    modifier = Modifier.size(20.dp),
                    tint = BharatWhite
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = BharatWhite
            )
        }
    }
}

@Composable
fun SecondaryActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        shadowElevation = 6.dp,
        modifier = modifier.size(48.dp)
    ) {
        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(6.dp)
        )
    }
}


