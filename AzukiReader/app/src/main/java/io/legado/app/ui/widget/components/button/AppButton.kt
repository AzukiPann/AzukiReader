package io.legado.app.ui.widget.components.button

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.wenku8.effects.wenku8Press
import io.legado.app.ui.theme.ComposeEngine
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LegadoTheme.composeEngine
import io.legado.app.ui.widget.components.mochi.AppMochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String
) {
    val interactions = remember { MutableInteractionSource() }
    when (composeEngine) {
        ComposeEngine.Miuix -> {
            MiuixButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = MiuixButtonDefaults.buttonColorsPrimary()
            ) {
                MiuixText(text = text, style = LegadoTheme.typography.labelLarge)
            }
        }
        ComposeEngine.Mochi -> {
            AppMochiButton(onClick, modifier, enabled, text, MochiButtonStyle.Filled)
        }
        ComposeEngine.Material, ComposeEngine.Coui -> {
            Button(
                onClick = onClick,
                modifier = modifier.wenku8Press(interactions),
                interactionSource = interactions,
                shape = if (LegadoTheme.isWenku8) RoundedCornerShape(4.dp) else androidx.compose.material3.ButtonDefaults.shape,
                enabled = enabled,
            ) {
                Text(text = text, style = LegadoTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String
) {
    val interactions = remember { MutableInteractionSource() }
    when (composeEngine) {
        ComposeEngine.Miuix -> {
            MiuixButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = MiuixButtonDefaults.buttonColors()
            ) {
                MiuixText(text = text,style = LegadoTheme.typography.labelLarge)
            }
        }
        ComposeEngine.Mochi -> {
            AppMochiButton(onClick, modifier, enabled, text, MochiButtonStyle.Outlined)
        }
        ComposeEngine.Material, ComposeEngine.Coui -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.wenku8Press(interactions),
                interactionSource = interactions,
                shape = if (LegadoTheme.isWenku8) RoundedCornerShape(4.dp) else androidx.compose.material3.ButtonDefaults.shape,
                enabled = enabled,
            ) {
                Text(text = text, style = LegadoTheme.typography.labelLarge)
            }
        }
    }
}
