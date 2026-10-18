package net.iegy.hisn.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Forest = Color(0xFF103C2F)
private val Emerald = Color(0xFF1BA875)
private val Mint = Color(0xFFDFF4E9)
private val OffWhite = Color(0xFFF7F8F4)
private val Charcoal = Color(0xFF121B18)
private val SoftGrey = Color(0xFF7C8782)

@Composable
fun HisnTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF62D5A4),
            onPrimary = Color(0xFF06281D),
            secondary = Color(0xFF9ADCC2),
            background = Charcoal,
            surface = Color(0xFF1A2420),
            onBackground = Color(0xFFE9F0EC),
            onSurface = Color(0xFFE9F0EC),
            outline = Color(0xFF56645D)
        )
    } else {
        lightColorScheme(
            primary = Forest,
            onPrimary = Color.White,
            secondary = Emerald,
            tertiary = Mint,
            background = OffWhite,
            surface = Color.White,
            onBackground = Charcoal,
            onSurface = Charcoal,
            outline = SoftGrey
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
