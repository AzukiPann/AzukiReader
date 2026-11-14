package io.legado.app.core.designsystem.wenku8

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** ArchiveBackdrop from Wenku8: a static, bounded drafting diagram behind the header. */
fun Modifier.wenku8ArchiveBackdrop(ink: Color, top: Dp = 0.dp): Modifier = drawWithCache {
    val inset = top.toPx()
    val bottom = min(size.height, inset + 240.dp.toPx())
    val dark = ink.luminance() > .5f
    val firstPlane = Path().apply {
        moveTo(size.width * .42f, inset); lineTo(size.width, inset)
        lineTo(size.width, bottom * .82f); lineTo(size.width * .78f, bottom * .82f); close()
    }
    val secondPlane = Path().apply {
        moveTo(size.width * .60f, inset + 18.dp.toPx()); lineTo(size.width, inset + 18.dp.toPx())
        lineTo(size.width, bottom); lineTo(size.width * .92f, bottom); close()
    }
    val grid = Path().apply {
        var x = 24.dp.toPx()
        while (x < size.width) { moveTo(x, 0f); lineTo(x, bottom); x += 48.dp.toPx() }
        var y = inset + 4.dp.toPx()
        while (y < bottom) { moveTo(24.dp.toPx(), y); lineTo(size.width - 24.dp.toPx(), y); y += 48.dp.toPx() }
    }
    val marks = Path().apply {
        val y = inset + 60.dp.toPx()
        for (x in listOf(24.dp.toPx(), size.width - 24.dp.toPx())) {
            moveTo(x - 3.dp.toPx(), y); lineTo(x + 3.dp.toPx(), y)
            moveTo(x, y - 3.dp.toPx()); lineTo(x, y + 3.dp.toPx())
        }
    }
    onDrawBehind {
        drawPath(firstPlane, Wenku8Design.Milk.copy(alpha = if (dark) .018f else .42f))
        drawPath(secondPlane, Wenku8Design.Milk.copy(alpha = if (dark) .012f else .28f))
        drawPath(grid, ink.copy(alpha = .045f), style = Stroke(.5.dp.toPx()))
        drawPath(marks, ink.copy(alpha = .15f), style = Stroke(.5.dp.toPx()))
    }
}
