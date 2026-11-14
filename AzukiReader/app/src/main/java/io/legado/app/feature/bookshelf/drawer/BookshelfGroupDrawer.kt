package io.legado.app.feature.bookshelf.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.main.bookshelf.BookGroupUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

/** Presentation-only: the host retains group selection, privacy and settings ownership. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BookshelfGroupDrawer(
    enabled: Boolean,
    groups: ImmutableList<BookGroupUi>,
    selectedGroupId: Long,
    onSelectGroup: (Long) -> Unit,
    onManageGroups: () -> Unit,
    content: @Composable (openDrawer: (() -> Unit)?, triggerFocus: FocusRequester) -> Unit,
) {
    val triggerFocus = remember { FocusRequester() }
    val currentContent by rememberUpdatedState(content)
    // Layout changes must not discard the shelf's remembered per-group scroll positions.
    val shelfContent = remember {
        movableContentOf<(() -> Unit)?, FocusRequester> { open, focus -> currentContent(open, focus) }
    }
    if (!enabled) {
        shelfContent(null, triggerFocus)
        return
    }
    // DrawerState intentionally is not saveable: recreation restores closed, not an overlay.
    val drawerState = remember { DrawerState(DrawerValue.Closed) }
    val scope = rememberCoroutineScope()
    val firstItemFocus = remember { FocusRequester() }
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.isOpen && groups.isNotEmpty()) firstItemFocus.requestFocus()
    }

    BoxWithConstraints {
        val permanent = maxWidth >= 840.dp
        LaunchedEffect(permanent) {
            // A resize must not leave a hidden modal drawer open behind the permanent rail.
            if (drawerState.isOpen) drawerState.close()
        }
        val drawerContent: @Composable () -> Unit = {
            Column(Modifier.fillMaxHeight().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.wenku8_shelf_archive),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.bookshelf), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(groups, key = { it.groupId }) { group ->
                        NavigationDrawerItem(
                            modifier = if (group == groups.firstOrNull()) {
                                Modifier.focusRequester(firstItemFocus)
                            } else Modifier,
                            label = {
                                Text(group.groupName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            },
                            selected = group.groupId == selectedGroupId,
                            onClick = {
                                onSelectGroup(group.groupId)
                                if (!permanent) scope.launch {
                                    drawerState.close()
                                    triggerFocus.requestFocus()
                                }
                            },
                            icon = if (group.isPrivate) {
                                { Icon(Icons.Default.Lock, stringResource(R.string.private_mark_book)) }
                            } else null,
                            badge = if (group.groupId == selectedGroupId) {
                                { Icon(Icons.Default.Check, null) }
                            } else null,
                            shape = RoundedCornerShape(4.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(onClick = {
                    scope.launch {
                        if (!permanent) drawerState.close()
                        onManageGroups()
                    }
                }) { Text(stringResource(R.string.group_manage)) }
                Spacer(Modifier.height(16.dp))
            }
        }
        if (permanent) {
            PermanentNavigationDrawer(drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier.width(280.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerShape = RoundedCornerShape(4.dp),
                    content = { drawerContent() },
                )
            }) { shelfContent(null, triggerFocus) }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                // Only the explicit button opens it; avoid stealing the system back/shelf gesture.
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerState = drawerState,
                        modifier = Modifier.width(maxWidth.coerceAtMost(320.dp)),
                        drawerShape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                    ) { drawerContent() }
                },
            ) {
                shelfContent({ scope.launch { drawerState.open() } }, triggerFocus)
            }
        }
    }
}
