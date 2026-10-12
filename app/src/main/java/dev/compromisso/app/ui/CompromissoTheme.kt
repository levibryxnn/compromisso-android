package dev.compromisso.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CompromissoColors = darkColorScheme(
    primary = Color(0xFFF1C998),
    onPrimary = Color(0xFF071320),
    primaryContainer = Color(0xFF3A3025),
    onPrimaryContainer = Color(0xFFFFF0D7),
    background = Color(0xFF071320),
    onBackground = Color(0xFFF0F5F6),
    surface = Color(0xFF071320),
    onSurface = Color(0xFFF0F5F6),
    surfaceVariant = Color(0xFF172638),
    onSurfaceVariant = Color(0xFF9FAEBF),
    outline = Color(0xFF46586A),
    error = Color(0xFFFFA69C),
)

@Composable
fun CompromissoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CompromissoColors, content = content)
}
