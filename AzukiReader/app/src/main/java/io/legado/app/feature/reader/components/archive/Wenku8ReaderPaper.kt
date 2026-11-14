package io.legado.app.feature.reader.components.archive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import io.legado.app.feature.reader.PageSnapshotLayer
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Complementary paper clips, a curved moving edge and a crease; the reader still owns the turn. */
@Composable
fun Wenku8ReaderPaper(
    next: Boolean,
    progress: () -> Float,
    currentLayer: GraphicsLayer,
    targetLayer: GraphicsLayer,
    redrawSignal: State<Long>,
    paper: Color,
    current: @Composable () -> Unit,
    target: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        PageSnapshotLayer(currentLayer, redrawSignal, current)
        PageSnapshotLayer(targetLayer, redrawSignal, target)
        Canvas(Modifier.fillMaxSize()) {
            val p = progress().coerceIn(0f, 1f)
            if (p <= 0f) { drawLayer(currentLayer); return@Canvas }
            if (p >= 1f) { drawLayer(targetLayer); return@Canvas }
            val envelope = sin(p * PI).toFloat()
            val edge = size.width * if (next) 1f - p else p
            val bend = min(24f * density, size.width * .045f) * envelope * if (next) 1f else -1f
            val face = Path().apply {
                moveTo(0f, 0f); lineTo(edge, 0f)
                cubicTo(edge + bend, size.height * .25f, edge + bend, size.height * .75f, edge, size.height)
                lineTo(0f, size.height); close()
            }
            drawRect(paper)
            drawLayer(if (next) targetLayer else currentLayer)
            clipPath(face) { drawLayer(if (next) currentLayer else targetLayer) }
            val crease = min(26f * density, size.width * .07f) * envelope
            if (crease > 0f) {
                drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .13f * envelope), Color.Transparent),
                    edge, edge + crease), Offset(edge, 0f), Size(crease, size.height))
                clipPath(face) {
                    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, paper.copy(alpha = .34f * envelope)),
                        edge - crease, edge), Offset(edge - crease, 0f), Size(crease, size.height))
                }
            }
        }
    }
}
