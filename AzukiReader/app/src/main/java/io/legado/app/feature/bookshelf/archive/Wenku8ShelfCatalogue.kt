package io.legado.app.feature.bookshelf.archive

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.legado.app.core.designsystem.wenku8.effects.LocalArchiveReveal
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.R
import io.legado.app.core.designsystem.wenku8.effects.wenku8Press

@Composable
fun Wenku8ShelfDiscoveryHeader(
    showShelf: Boolean,
    sortLabel: String,
    onSort: () -> Unit,
    shelf: @Composable () -> Unit,
) {
    Column {
        if (showShelf) shelf()
        Wenku8ShelfCatalogueHeading(sortLabel, onSort)
    }
}

@Composable
fun Wenku8ShelfCatalogueHeading(sortLabel: String, onSort: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("SHELF / CATALOGUE", fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.wenku8_catalogue), color = MaterialTheme.colorScheme.onSurface, fontSize = 21.sp, fontWeight = FontWeight.W600)
        }
        TextButton(onClick = onSort) {
            Text(sortLabel, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Default.Sort, stringResource(R.string.wenku8_catalogue_sort), Modifier.size(18.dp))
        }
    }
}

/** NovelRow proportions: 56x76 cover, 15 gap, 13 vertical inset, file code and divider. */
@Composable
fun Wenku8ShelfCatalogueRow(
    title: String,
    author: String,
    code: String,
    progress: String?,
    order: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    cover: @Composable (Modifier) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    Column(modifier.fillMaxWidth().archiveShelfReveal(LocalArchiveReveal.current, ((order + 2).coerceIn(0, 8) * .055f), ((order + 2).coerceIn(0, 8) * .055f + .52f)).wenku8Press(interactions)
        .combinedClickable(interactionSource = interactions, indication = null,
            role = Role.Button, onClick = onClick, onLongClick = onLongClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            cover(Modifier.size(56.dp, 76.dp))
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(code, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.W600, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp))
                Text(author, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!progress.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(progress, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.AutoMirrored.Default.KeyboardArrowRight, null, Modifier.padding(start = 8.dp).size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(thickness = .5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

internal val ArchiveShelfEase = CubicBezierEasing(.215f, .61f, .355f, 1f)

/** RevealFromTop: the same finite clock clips text vertically with an (-8,-6) displacement. */
internal fun Modifier.archiveShelfReveal(clock: State<Float>?, start: Float, end: Float = 1f): Modifier {
    if (clock == null) return this
    return drawWithContent {
        val progress = ArchiveShelfEase.transform(((clock.value - start) / (end - start)).coerceIn(0f, 1f))
        if (progress >= 1f) drawContent()
        else if (progress > 0f) translate(-8.dp.toPx() * (1 - progress), -6.dp.toPx() * (1 - progress)) {
            clipRect(bottom = size.height * progress) { this@drawWithContent.drawContent() }
        }
    }
}
