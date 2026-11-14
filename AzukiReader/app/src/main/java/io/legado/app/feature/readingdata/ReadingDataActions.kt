package io.legado.app.feature.readingdata

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.widget.components.text.AppText
import io.legado.app.ui.widget.components.button.series.SmallTonalButton

@Composable
fun ReadingDataActions(
    busy: Boolean,
    @StringRes message: Int?,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppText(text = stringResource(R.string.reading_data_description))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallTonalButton(onClick = onImport, enabled = !busy,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                text = stringResource(R.string.reading_data_import))
            SmallTonalButton(onClick = onExport, enabled = !busy,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                text = stringResource(R.string.reading_data_export))
        }
        val status = if (busy) R.string.reading_data_busy else message
        status?.let { AppText(text = stringResource(it)) }
    }
}
