package org.bharatscan.app.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.bharatscan.app.R
import org.bharatscan.app.ui.theme.BharatSaffron

@Composable
fun BrandTitle(
    modifier: Modifier = Modifier,
    height: Dp = 26.dp,
) {
    val appLocales = AppCompatDelegate.getApplicationLocales()
    val currentLocale = LocalContext.current.resources.configuration.locales[0]
    val isEnglish = (appLocales.isEmpty && currentLocale.language == "en") ||
        (!appLocales.isEmpty && appLocales[0]?.language == "en")

    if (isEnglish) {
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.text),
            contentDescription = stringResource(R.string.app_name),
            modifier = modifier.height(height),
            contentScale = ContentScale.Fit
        )
    } else {
        val style = MaterialTheme.typography.titleLarge
        val brand = stringResource(R.string.app_name_brand).trim()
        val scan = stringResource(R.string.app_name_scan).trim()
        val fallbackName = stringResource(R.string.app_name).trim()
        val first = if (brand.isNotBlank()) brand else fallbackName
        val rest = if (scan.isNotBlank() && first != fallbackName) scan else null
        Row(
            modifier = modifier.height(height),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = first,
                style = style,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = Color.Black
            )
            if (!rest.isNullOrBlank()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = rest,
                    style = style,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    color = BharatSaffron
                )
            }
        }
    }
}
