package io.legado.app.core.designsystem.wenku8.chrome

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.core.designsystem.wenku8.effects.archiveReveal

/** Real section titles remain accessible; registration codes are decoration only. */
@Composable
fun Wenku8ArchiveSection(title: String, code: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(code, fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.05.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {}.archiveReveal(start = .1f, end = .68f))
            HorizontalDivider(Modifier.weight(1f).padding(top = 6.dp).archiveReveal(start = 0f, end = .46f), thickness = .6.dp)
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }.archiveReveal(start = .16f, end = .82f))
    }
}
