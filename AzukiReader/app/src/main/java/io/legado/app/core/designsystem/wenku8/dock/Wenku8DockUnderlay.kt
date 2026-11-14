package io.legado.app.core.designsystem.wenku8.dock

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur

/** Draws only a sampled scroll edge; it never intercepts input or blurs dock labels. */
@Composable
fun Wenku8DockUnderlay(backdrop: Backdrop?, modifier: Modifier = Modifier) {
    val policy = io.legado.app.core.designsystem.wenku8.effects.rememberWenku8Effects()
    if (!policy.edgeBlur) return
    if (backdrop == null || Build.VERSION.SDK_INT < 33) return
    val density = LocalDensity.current
    val systemBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    Box(modifier.fillMaxWidth().height(52.dp + systemBottom)
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)), blendMode = BlendMode.DstIn)
        }
        .drawBackdrop(
            backdrop = backdrop, shape = { RoundedCornerShape(0.dp) },
            effects = { blur(1.28.dp.toPx()) }, highlight = null, shadow = null,
        ),
    )
}
