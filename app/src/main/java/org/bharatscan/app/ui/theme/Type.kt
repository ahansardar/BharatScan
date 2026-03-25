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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import org.bharatscan.app.R

private val MontserratFont = GoogleFont("Montserrat")
private val NotoSansDevanagari = GoogleFont("Noto Sans Devanagari")
private val NotoSansBengali = GoogleFont("Noto Sans Bengali")
private val NotoSansTelugu = GoogleFont("Noto Sans Telugu")
private val NotoSansMarathi = GoogleFont("Noto Sans Devanagari")
private val NotoSansTamil = GoogleFont("Noto Sans Tamil")
private val NotoSansUrdu = GoogleFont("Noto Naskh Arabic")
private val NotoSansGujarati = GoogleFont("Noto Sans Gujarati")
private val NotoSansKannada = GoogleFont("Noto Sans Kannada")
private val NotoSansMalayalam = GoogleFont("Noto Sans Malayalam")
private val NotoSansOdia = GoogleFont("Noto Sans Oriya")
private val NotoSansPunjabi = GoogleFont("Noto Sans Gurmukhi")
private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun appFont(googleFont: GoogleFont, weight: FontWeight) =
    Font(googleFont = googleFont, fontProvider = GoogleFontProvider, weight = weight)

private val AppFontFamily = FontFamily(
    appFont(MontserratFont, FontWeight.Light),
    appFont(MontserratFont, FontWeight.Normal),
    appFont(MontserratFont, FontWeight.Medium),
    appFont(MontserratFont, FontWeight.SemiBold),
    appFont(MontserratFont, FontWeight.Bold),
    appFont(NotoSansDevanagari, FontWeight.Normal),
    appFont(NotoSansDevanagari, FontWeight.Bold),
    appFont(NotoSansBengali, FontWeight.Normal),
    appFont(NotoSansBengali, FontWeight.Bold),
    appFont(NotoSansTelugu, FontWeight.Normal),
    appFont(NotoSansTelugu, FontWeight.Bold),
    appFont(NotoSansMarathi, FontWeight.Normal),
    appFont(NotoSansMarathi, FontWeight.Bold),
    appFont(NotoSansTamil, FontWeight.Normal),
    appFont(NotoSansTamil, FontWeight.Bold),
    appFont(NotoSansUrdu, FontWeight.Normal),
    appFont(NotoSansUrdu, FontWeight.Bold),
    appFont(NotoSansGujarati, FontWeight.Normal),
    appFont(NotoSansGujarati, FontWeight.Bold),
    appFont(NotoSansKannada, FontWeight.Normal),
    appFont(NotoSansKannada, FontWeight.Bold),
    appFont(NotoSansMalayalam, FontWeight.Normal),
    appFont(NotoSansMalayalam, FontWeight.Bold),
    appFont(NotoSansOdia, FontWeight.Normal),
    appFont(NotoSansOdia, FontWeight.Bold),
    appFont(NotoSansPunjabi, FontWeight.Normal),
    appFont(NotoSansPunjabi, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.3.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.2.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.2.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.2.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp
    )
)


