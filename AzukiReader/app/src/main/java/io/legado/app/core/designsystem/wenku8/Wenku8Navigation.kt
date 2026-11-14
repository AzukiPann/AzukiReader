package io.legado.app.core.designsystem.wenku8

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Wenku8NavigationBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun RowScope.Wenku8NavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(120), label = "archiveNavPress")
    val emphasis by animateFloatAsState(if (selected) 1f else 0.64f, tween(120), label = "archiveNavSelect")
    Column(
        modifier = modifier.weight(1f).heightIn(min = 64.dp)
            .selectable(
                selected = selected, onClick = onClick, role = Role.Tab,
                interactionSource = interaction, indication = null,
            )
            .semantics { contentDescription = label }
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = emphasis }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.clearAndSetSemantics {}) { icon() }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                label, modifier = Modifier.clearAndSetSemantics {},
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.width(20.dp).height(1.dp)
                .background(if (selected) MaterialTheme.colorScheme.onBackground else androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}
