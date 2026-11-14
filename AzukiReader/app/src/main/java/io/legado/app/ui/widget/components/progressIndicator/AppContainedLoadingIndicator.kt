package io.legado.app.ui.widget.components.progressIndicator

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.legado.app.ui.theme.ComposeEngine
import io.legado.app.ui.theme.LegadoTheme.composeEngine
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator as MiuixInfiniteProgressIndicator

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppContainedLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    if (io.legado.app.ui.theme.LegadoTheme.isWenku8) {
        io.legado.app.core.designsystem.wenku8.chrome.Wenku8Loading(modifier)
        return
    }
    when (composeEngine) {
        ComposeEngine.Miuix -> {
            MiuixInfiniteProgressIndicator(
                modifier = modifier,
            )
        }
        ComposeEngine.Material, ComposeEngine.Mochi, ComposeEngine.Coui -> {
            ContainedLoadingIndicator(
                modifier = modifier
            )
        }
    }
}
