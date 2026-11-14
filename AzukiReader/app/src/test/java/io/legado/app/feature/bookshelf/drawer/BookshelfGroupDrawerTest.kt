package io.legado.app.feature.bookshelf.drawer

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.legado.app.ui.main.bookshelf.BookGroupUi
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w398dp-h800dp")
class BookshelfGroupDrawerTest {
    @get:Rule val compose = createComposeRule()

    private val groups = persistentListOf(
        BookGroupUi(11L, "Fiction", null, 0, true, true, 0, false),
        BookGroupUi(22L, "Local", null, 1, true, true, 0, false),
    )

    @Test
    fun `drawer selection reports existing group identity and closes`() {
        var selected = 11L
        compose.setContent {
            MaterialTheme {
                BookshelfGroupDrawer(true, groups, selected, { selected = it }, {}) { open, focus ->
                    TextButton(
                        modifier = Modifier.focusRequester(focus),
                        onClick = { open?.invoke() },
                    ) { Text("Groups") }
                }
            }
        }
        compose.onNodeWithText("Groups").performClick()
        compose.onNodeWithText("Local").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(22L, selected) }
        compose.onNodeWithText("Groups").assertIsDisplayed()
    }

    @Test
    fun `switching group layout preserves remembered shelf content`() {
        var drawerEnabled by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                BookshelfGroupDrawer(drawerEnabled, groups, 11L, {}, {}) { _, _ ->
                    var position by remember { mutableStateOf(0) }
                    Column {
                        Text("Position $position")
                        TextButton(onClick = { position++ }) { Text("Scroll") }
                    }
                }
            }
        }
        compose.onNodeWithText("Scroll").performClick()
        compose.runOnIdle { drawerEnabled = true }
        compose.onNodeWithText("Position 1").assertIsDisplayed()
        compose.runOnIdle { drawerEnabled = false }
        compose.onNodeWithText("Position 1").assertIsDisplayed()
    }
}
