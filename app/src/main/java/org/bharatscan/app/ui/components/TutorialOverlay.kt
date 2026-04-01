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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.bharatscan.app.R
import org.bharatscan.app.ui.TutorialStep
import org.bharatscan.app.ui.theme.BharatChakra
import org.bharatscan.app.ui.theme.BharatGreen
import org.bharatscan.app.ui.theme.BharatNavy
import org.bharatscan.app.ui.theme.BharatSaffron
import org.bharatscan.app.ui.theme.BharatWhite
import org.bharatscan.app.ui.theme.TextSecondary

@Composable
fun TutorialOverlay(
    step: TutorialStep,
    totalSteps: Int,
    stepIndex: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val (titleRes, bodyRes, accent, icon) = remember(step) {
        when (step) {
            TutorialStep.HOME -> Quad(
                R.string.tutorial_step_home_title,
                R.string.tutorial_step_home_body,
                BharatNavy,
                Icons.Default.Home
            )
            TutorialStep.SCAN -> Quad(
                R.string.tutorial_step_scan_title,
                R.string.tutorial_step_scan_body,
                BharatSaffron,
                Icons.Default.PhotoCamera
            )
            TutorialStep.EDIT -> Quad(
                R.string.tutorial_step_adjust_title,
                R.string.tutorial_step_adjust_body,
                BharatChakra,
                Icons.Default.Settings
            )
            TutorialStep.EXPORT -> Quad(
                R.string.tutorial_step_export_title,
                R.string.tutorial_step_export_body,
                BharatGreen,
                Icons.Default.Description
            )
            TutorialStep.SEARCH -> Quad(
                R.string.tutorial_step_search_title,
                R.string.tutorial_step_search_body,
                BharatNavy,
                Icons.Default.Search
            )
        }
    }

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.tutorial_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { index ->
                        val dotColor = if (index == stepIndex) BharatSaffron else TextSecondary.copy(alpha = 0.35f)
                        Box(
                            modifier = Modifier
                                .size(if (index == stepIndex) 10.dp else 8.dp)
                                .background(dotColor, CircleShape)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text(text = stringResource(R.string.tutorial_skip))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (stepIndex > 0) {
                            TextButton(onClick = onBack) {
                                Text(text = stringResource(R.string.tutorial_back))
                            }
                        }
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron)
                        ) {
                            Text(
                                text = stringResource(
                                    if (stepIndex == totalSteps - 1) R.string.tutorial_done else R.string.tutorial_next
                                ),
                                color = BharatWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quad(
    val titleRes: Int,
    val bodyRes: Int,
    val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
