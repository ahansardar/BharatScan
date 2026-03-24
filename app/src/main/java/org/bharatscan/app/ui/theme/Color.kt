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
package org.bharatscan.app.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Digital Bharat Palette
val BharatNavy = Color(0xFF0E1B3D)
val BharatSaffron = Color(0xFFFF8A34)
val BharatGreen = Color(0xFF14803A)
val BharatWhite = Color(0xFFFDFBF7)
val BharatChakra = Color(0xFF1E3A8A)

val BharatNavySoft = Color(0xFF2B3C78)
val BharatSaffronDeep = Color(0xFFE0721D)
val BharatGreenDeep = Color(0xFF0B6B33)

// UI Accent Colors
val SurfaceLight = Color(0xFFF6F4EF)
val SurfaceDark = Color(0xFF0F141F)
val SurfaceElevated = Color(0xFFFBFAF7)
val TextPrimary = Color(0xFF0B1B2B)
val TextSecondary = Color(0xFF4F5B6A)
val DividerColor = Color(0xFFE1E6EF)
val OutlineSoft = Color(0xFFE7ECF4)
val OutlineStrong = Color(0xFFCBD3E1)
val SurfaceVariant = Color(0xFFEFF1F7)
val OnSurfaceVariant = Color(0xFF4B5563)

// Background Tints
val AppBackgroundTop = Color(0xFFF8F5EE)
val AppBackgroundMid = Color(0xFFF1F2FA)
val AppBackgroundBottom = Color(0xFFFDF7EF)

// Particle & Glow Effects
val SaffronGlow = Color(0xFFFFD9B0)
val GreenGlow = Color(0xFFCBEFD9)
val NavyGlow = Color(0xFFD8E0F6)
val ChakraGlow = Color(0xFFB8C6FF)

// Legacy compatibility (to avoid breaking existing code immediately)
val DeepNavyBlue = BharatNavy
val VibrantSaffron = BharatSaffron
val CleanWhite = BharatWhite
val AshokaChakraBlue = BharatChakra
val FreshGreen = BharatGreen
val SoftGray = DividerColor
val ParticleSaffron = BharatSaffron
val ParticleWhite = Color(0xFFBDBDBD)
val ParticleGreen = BharatGreen

// Material 3 Mapping
val Primary = BharatNavy
val OnPrimary = BharatWhite
val PrimaryContainer = NavyGlow
val OnPrimaryContainer = BharatNavy

val Secondary = BharatSaffron
val OnSecondary = BharatWhite
val SecondaryContainer = SaffronGlow
val OnSecondaryContainer = BharatNavy

val Tertiary = BharatGreen
val OnTertiary = BharatWhite
val TertiaryContainer = GreenGlow
val OnTertiaryContainer = BharatGreen

val Background = SurfaceLight
val OnBackground = TextPrimary
val Surface = SurfaceElevated
val OnSurface = TextPrimary

val Error = Color(0xFFB00020)
val OnError = BharatWhite
val ErrorContainer = Color(0xFFF6D9DE)
val OnErrorContainer = Color(0xFF7C1D2E)

// Dark Mode Mapping
val PrimaryDark = Color(0xFFB2C3FF)
val OnPrimaryDark = Color(0xFF0B142A)
val PrimaryContainerDark = Color(0xFF1C2B5F)
val OnPrimaryContainerDark = Color(0xFFDCE3FF)

val BackgroundDark = SurfaceDark
val OnBackgroundDark = Color(0xFFE4E8F0)
val SurfaceDarkM3 = Color(0xFF151B28)
val OnSurfaceDark = Color(0xFFE4E8F0)
val SurfaceVariantDark = Color(0xFF242B3A)
val OnSurfaceVariantDark = Color(0xFFB8C2D6)
val ErrorContainerDark = Color(0xFF7C1D2E)
val OnErrorContainerDark = Color(0xFFFFD8E4)


