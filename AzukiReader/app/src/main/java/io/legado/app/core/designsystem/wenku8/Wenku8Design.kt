package io.legado.app.core.designsystem.wenku8

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.ui.theme.BaseColorScheme

/** Native implementation of the archive/cream visual vocabulary; no Flutter runtime/assets. */
object Wenku8Design : BaseColorScheme() {
    const val ThemeValue = "wenku8"
    val Paper = Color(0xFFF4F3EF)
    val Surface = Color(0xFFFDFCFA)
    val Milk = Color(0xFFFEFDF8)
    val Ink = Color(0xFF191919)
    val Muted = Color(0xFF666B63)

    override val lightScheme = lightColorScheme(
        primary = Ink, onPrimary = Milk,
        primaryContainer = Color(0xFFE7E6DF), onPrimaryContainer = Ink,
        secondary = Muted, onSecondary = Milk,
        secondaryContainer = Color(0xFFEAE9E2), onSecondaryContainer = Ink,
        tertiary = Muted, onTertiary = Milk,
        tertiaryContainer = Color(0xFFE7E6DF), onTertiaryContainer = Ink,
        background = Paper, onBackground = Ink,
        surface = Surface, onSurface = Ink,
        surfaceVariant = Color(0xFFEAE9E2), onSurfaceVariant = Muted,
        surfaceTint = Color.Transparent,
        outline = Color(0xFF83867D), outlineVariant = Color(0xFFDADBD3),
        inverseSurface = Ink, inverseOnSurface = Paper, inversePrimary = Milk,
        surfaceDim = Color(0xFFE6E5DE), surfaceBright = Milk,
        surfaceContainerLowest = Milk, surfaceContainerLow = Surface,
        surfaceContainer = Paper, surfaceContainerHigh = Color(0xFFEDECE5),
        surfaceContainerHighest = Color(0xFFE7E6DF),
        primaryFixed = Color(0xFFE7E6DF), primaryFixedDim = Color(0xFFDADBD3),
        onPrimaryFixed = Ink, onPrimaryFixedVariant = Muted,
        secondaryFixed = Color(0xFFEAE9E2), secondaryFixedDim = Color(0xFFDADBD3),
        onSecondaryFixed = Ink, onSecondaryFixedVariant = Muted,
        tertiaryFixed = Color(0xFFE7E6DF), tertiaryFixedDim = Color(0xFFDADBD3),
        onTertiaryFixed = Ink, onTertiaryFixedVariant = Muted,
    )

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFF2F2F2), onPrimary = Ink,
        primaryContainer = Color(0xFF3A3A3A), onPrimaryContainer = Color(0xFFF2F2F2),
        secondary = Color(0xFFB7B7B7), onSecondary = Ink,
        secondaryContainer = Color(0xFF2D2D2D), onSecondaryContainer = Color(0xFFF2F2F2),
        tertiary = Color(0xFFB7B7B7), onTertiary = Ink,
        tertiaryContainer = Color(0xFF3A3A3A), onTertiaryContainer = Color(0xFFF2F2F2),
        background = Ink, onBackground = Color(0xFFF2F2F2),
        surface = Color(0xFF242424), onSurface = Color(0xFFF2F2F2),
        surfaceVariant = Color(0xFF2D2D2D), onSurfaceVariant = Color(0xFFB7B7B7),
        surfaceTint = Color.Transparent,
        outline = Color(0xFF8B8B8B), outlineVariant = Color(0xFF3C3C3C),
        inverseSurface = Paper, inverseOnSurface = Ink, inversePrimary = Ink,
        surfaceDim = Ink, surfaceBright = Color(0xFF3A3A3A),
        surfaceContainerLowest = Ink, surfaceContainerLow = Color(0xFF242424),
        surfaceContainer = Color(0xFF242424), surfaceContainerHigh = Color(0xFF2D2D2D),
        surfaceContainerHighest = Color(0xFF3A3A3A),
        primaryFixed = Color(0xFFE7E6DF), primaryFixedDim = Color(0xFFDADBD3),
        onPrimaryFixed = Ink, onPrimaryFixedVariant = Muted,
        secondaryFixed = Color(0xFFEAE9E2), secondaryFixedDim = Color(0xFFDADBD3),
        onSecondaryFixed = Ink, onSecondaryFixedVariant = Muted,
        tertiaryFixed = Color(0xFFE7E6DF), tertiaryFixedDim = Color(0xFFDADBD3),
        onTertiaryFixed = Ink, onTertiaryFixedVariant = Muted,
    )

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(3.dp), small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(4.dp), large = RoundedCornerShape(12.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

    fun typography(base: Typography, font: FontFamily?): Typography = base.copy(
        headlineLarge = base.headlineLarge.copy(fontFamily = font ?: FontFamily.SansSerif),
        headlineMedium = base.headlineMedium.copy(fontFamily = font ?: FontFamily.SansSerif),
        headlineSmall = base.headlineSmall.copy(fontFamily = font ?: FontFamily.SansSerif),
        titleLarge = base.titleLarge.copy(
            fontFamily = font ?: FontFamily.SansSerif, fontWeight = FontWeight.Bold,
            fontSize = 23.sp, letterSpacing = (-0.7).sp,
        ),
        labelSmall = base.labelSmall.copy(
            fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.sp,
        ),
    )
}
