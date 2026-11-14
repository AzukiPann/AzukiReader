package io.legado.app.core.designsystem.wenku8.effects

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import io.legado.app.ui.theme.LegadoTheme
import kotlin.math.PI
import kotlin.math.sin

val LocalWenku8Returning = staticCompositionLocalOf { false }

/** Native retained-layer paper geometry; zero depth at both endpoints. */
internal object ArchivePaperGeometry {
    fun depth(progress: Float, row: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        val envelope = sin(p * PI).toFloat()
        return envelope * sin((row * .8f - p) * PI).toFloat() * 18f
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun wenku8CoverBounds(returning: Boolean): BoundsTransform = BoundsTransform { from, to ->
    val duration = if (returning) 840 else 1000
    val middle = Rect(
        (from.left + to.left) / 2, (from.top + to.top) / 2 - 24,
        (from.right + to.right) / 2, (from.bottom + to.bottom) / 2 - 24)
    keyframes {
        durationMillis = duration
        from at 0
        if (returning) {
            from at 180
            middle at 460
            to at 740
        } else {
            from at 120
            middle at 350
            to at 580
        }
        to at duration
    }
}

/** Bend is drawn inside the shared overlay; it never moves or screenshots a business View. */
@Composable
fun Modifier.wenku8PaperFlight(scope: AnimatedVisibilityScope?): Modifier {
    if (!LegadoTheme.isWenku8 || scope == null || !rememberWenku8Effects().spatial) return this
    val returning = LocalWenku8Returning.current
    val progress = scope.transition.animateFloat(
        transitionSpec = { tween(if (returning) 840 else 1000) },
        label = "archive-paper-flight") { if (it == EnterExitState.Visible) 1f else 0f }
    return drawWithCache {
        val retained = obtainGraphicsLayer()
        val bands = 18
        onDrawWithContent {
            val p = progress.value
            if (p <= .001f || p >= .999f) drawContent()
            else {
                retained.record { this@onDrawWithContent.drawContent() }
                for (band in 0 until bands) {
                    val top = size.height * band / bands
                    val bottom = size.height * (band + 1) / bands
                    val depth = ArchivePaperGeometry.depth(p, (band + .5f) / bands) * density
                    translate(left = depth, top = -sin(p * PI).toFloat() * 8f * density) {
                        clipRect(top = top, bottom = bottom + .5f) { drawLayer(retained) }
                    }
                }
                drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .06f * sin(p * PI).toFloat()),
                    Color.Transparent, Color.White.copy(alpha = .09f * sin(p * PI).toFloat()))))
            }
        }
    }
}
