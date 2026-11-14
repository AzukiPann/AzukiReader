package io.legado.app.core.designsystem.wenku8.dock

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w398dp-h800dp")
class Wenku8GlassDockTest {
    @get:Rule val compose = createComposeRule()
    private val items = persistentListOf(
        Wenku8DockItem("home", "Discover", Icons.Outlined.Home),
        Wenku8DockItem("shelf", "Bookshelf", Icons.Outlined.Home),
        Wenku8DockItem("settings", "Settings", Icons.Outlined.Home),
    )

    @Test
    fun `accessible tab activation preserves identity and selection`() {
        var selected by mutableStateOf(0)
        compose.setContent {
            MaterialTheme {
                Wenku8GlassDock(items, selected, { selected = it }, Modifier.width(196.dp), reduceMotion = true)
            }
        }
        compose.onNodeWithContentDescription("Bookshelf").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(1, selected) }
    }

    @Test
    fun `drag previews without navigating until release`() {
        val commits = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                Wenku8GlassDock(items, 0, commits::add, Modifier.width(196.dp).testTag("dock"), reduceMotion = true)
            }
        }
        compose.onNodeWithTag("dock").performTouchInput {
            down(Offset(width * .17f, height * .5f))
            moveTo(Offset(width * .83f, height * .5f))
        }
        compose.runOnIdle { assertEquals(emptyList<Int>(), commits) }
        compose.onNodeWithTag("dock").performTouchInput { up() }
        compose.runOnIdle { assertEquals(listOf(2), commits) }
    }

    @Test
    fun `cancelled drag does not change destination`() {
        val commits = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                Wenku8GlassDock(items, 0, commits::add, Modifier.width(196.dp).testTag("dock"), reduceMotion = true)
            }
        }
        compose.onNodeWithTag("dock").performTouchInput {
            down(Offset(width * .17f, height * .5f))
            moveTo(Offset(width * .83f, height * .5f))
            cancel()
        }
        compose.runOnIdle { assertEquals(emptyList<Int>(), commits) }
        compose.onNodeWithContentDescription("Discover").assertIsSelected()
    }

    @Test
    fun `right to left drag maps physical location to logical destination`() {
        val commits = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Wenku8GlassDock(items, 0, commits::add, Modifier.width(196.dp).testTag("dock"), reduceMotion = true)
                }
            }
        }
        compose.onNodeWithTag("dock").performTouchInput {
            down(Offset(width * .83f, height * .5f))
            moveTo(Offset(width * .17f, height * .5f))
            up()
        }
        compose.runOnIdle { assertEquals(listOf(2), commits) }
    }
}
