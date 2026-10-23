package com.gpstrigger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.gpstrigger.data.ThemeMode

// Fallback palette (Android < 12) taken from the app icon.
private val PinRed = Color(0xFFD32F2F)
private val PinCoral = Color(0xFFFF8A70)
private val Navy = Color(0xFF243447)
private val SkyBlue = Color(0xFF52A9E0)
private val Amber = Color(0xFFF29C11)
private val LightAmber = Color(0xFFFFD866)

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun GpsTriggerTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme(primary = PinCoral, secondary = SkyBlue, tertiary = LightAmber)
        else -> lightColorScheme(primary = PinRed, secondary = Navy, tertiary = Amber)
    }
    MaterialTheme(colorScheme = colors, content = content)
}
