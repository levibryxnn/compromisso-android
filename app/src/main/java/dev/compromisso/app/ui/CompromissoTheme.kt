package dev.compromisso.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CompromissoColors = darkColorScheme(
    primary = Color(0xFF31E6A0),
    onPrimary = Color(0xFF071710),
    primaryContainer = Color(0xFF164C3B),
    onPrimaryContainer = Color(0xFFE3F9ED),
    background = Color(0xFF0A111B),
    onBackground = Color(0xFFF0F5F6),
    surface = Color(0xFF0A111B),
    onSurface = Color(0xFFF0F5F6),
    surfaceVariant = Color(0xFF192531),
    onSurfaceVariant = Color(0xFF9BAAB8),
    outline = Color(0xFF506270),
    error = Color(0xFFFFA69C),
)

@Composable
fun CompromissoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CompromissoColors, content = content)
}
