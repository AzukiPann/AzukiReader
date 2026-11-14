package io.legado.app.feature.bookmarks.export

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.legado.app.R
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportAllExcerptsAction(viewModel: ExportAllExcerptsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) viewModel.onIntent(ExcerptExportIntent.Export(uri.toString()))
    }
    LaunchedEffect(viewModel, owner, context) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collectLatest { result ->
                val message = when {
                    result.failed -> context.getString(R.string.excerpts_export_failed)
                    result.exportedCount != null -> context.getString(R.string.excerpts_export_success, result.exportedCount)
                    else -> null
                }
                if (message != null) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    viewModel.onIntent(ExcerptExportIntent.MessageShown)
                }
            }
        }
    }
    TopBarActionButton(
        imageVector = Icons.Outlined.FileDownload,
        contentDescription = stringResource(if (state.exporting) R.string.excerpts_exporting else R.string.excerpts_export_all),
        onClick = {
            if (!state.exporting) {
                val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
                launcher.launch("AzukiReader-摘录-$stamp.md")
            }
        },
    )
}
