package io.legado.app.core.designsystem.wenku8.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import io.legado.app.core.designsystem.wenku8.chrome.Wenku8Motion
import io.legado.app.ui.theme.LegadoTheme
import androidx.compose.material3.MaterialTheme
import java.util.Collections
import java.util.LinkedHashMap

private val presentedCovers = Collections.synchronizedMap(object : LinkedHashMap<String, Boolean>(256, .75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > 256
})

/** A finite diagonal cut after decode; recycled/previously presented covers remain static. */
@Composable
fun Modifier.wenku8CoverArrival(identity: String?, ready: Boolean, shared: Boolean): Modifier {
    if (!LegadoTheme.isWenku8 || identity == null) return this
    val effects = rememberWenku8Effects()
    val seen = remember(identity) { presentedCovers[identity] == true }
    val progress = remember(identity) { Animatable(if (seen || shared || !effects.motion) 1f else 0f) }
    val placeholder = MaterialTheme.colorScheme.surfaceContainerLow
    LaunchedEffect(identity, ready, effects.motion, shared) {
        if (ready) {
            if (shared || !effects.motion) progress.snapTo(1f)
            else if (progress.value < 1f) progress.animateTo(1f, tween(Wenku8Motion.Enter, easing = Wenku8Motion.Ease))
            presentedCovers[identity] = true
        }
    }
    return drawWithContent {
        if (!ready || progress.value >= 1f) drawContent()
        else {
            drawRect(placeholder)
            val edge = progress.value * (size.height + size.width * .18f)
            val path = Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f)
                lineTo(size.width, edge - size.width * .18f); lineTo(0f, edge); close()
            }
            clipPath(path) { this@drawWithContent.drawContent() }
        }
    }
}
