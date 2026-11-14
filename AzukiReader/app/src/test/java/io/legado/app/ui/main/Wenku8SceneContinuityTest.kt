package io.legado.app.ui.main

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.*
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import io.legado.app.core.designsystem.wenku8.effects.Wenku8ScreenMotion
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.domain.model.settings.NavigationAnimationStyle
import io.legado.app.ui.theme.AppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Wenku8SceneContinuityTest {
    @get:Rule val compose = createComposeRule()
    private fun advance() { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }

    @Test fun `settings fade stays full screen through push pop and predictive cancellation`() = verifyScenes(false)

    @Test fun `reader fade stays opaque through push pop and predictive cancellation`() = verifyScenes(true)

    private fun verifyScenes(reader: Boolean) {
        val first = MainRouteSettings
        val second = MainRouteSettingsTheme
        val stack = mutableStateListOf<NavKey>(first)
        val input = DirectNavigationEventInput()
        val owner = object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = NavigationEventDispatcher()
        }
        compose.runOnUiThread { owner.navigationEventDispatcher.addInput(input) }
        val tracker = MainNavRouteTracker()
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true,
                LocalNavigationEventDispatcherOwner provides owner) {
                AppTheme(AppUiConfiguration(), darkTheme = false, applyBackground = false) {
                    Box(Modifier.requiredSize(320.dp, 240.dp).background(Color.Blue).testTag("scene")) {
                        SharedTransitionLayout {
                            val sharedScope = this
                            val direction = LocalLayoutDirection.current
                            val decorator = remember { NavigationPageSceneDecorator(mutableFloatStateOf(0f)) }
                            NavDisplay(backStack = stack, sharedTransitionScope = this,
                                sceneStrategies = listOf(SinglePaneSceneStrategy()),
                                sceneDecoratorStrategies = listOf(decorator),
                                transitionSpec = { navigationPreset(NavigationAnimationStyle.Default, true, direction, wenku8 = true)!! },
                                popTransitionSpec = { navigationPreset(NavigationAnimationStyle.Default, false, direction, wenku8 = true)!! },
                                predictivePopTransitionSpec = { edge -> navigationPreset(NavigationAnimationStyle.Default, false, direction, edge, wenku8 = true)!! },
                                entryProvider = mainPaneEntryProvider(stack, false, tracker, {}) {
                                    entry<MainRouteSettings> { Wenku8ScreenMotion { Box(Modifier.fillMaxSize().background(Color.Green)) } }
                                    entry<MainRouteSettingsTheme>(metadata = if (reader) readerEntryMetadata(true) else emptyMap()) {
                                        Wenku8ScreenMotion {
                                            val base = Modifier.fillMaxSize()
                                            val modifier = if (reader) base.readerSharedBounds(
                                                sharedScope, LocalNavAnimatedContentScope.current,
                                                "reader-cover", 0f, 1f,
                                            ).also { assertSame("reader must not add a cover morph", base, it) } else base
                                            Box(modifier.background(Color.Red))
                                        }
                                    }
                                })
                        }
                    }
                }
            }
        }
        fun frame(name: String? = null): Bitmap {
            val bitmap = compose.onNodeWithTag("scene").captureToImage().asAndroidBitmap()
            if (name != null) System.getProperty("wenku8.continuityRenderDir")?.let {
                File(it).mkdirs()
                File(it, "$name.png").outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            }
            return bitmap
        }
        fun opaque(name: String) {
            val bitmap = frame(name)
            val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)

            for (x in 4 until bitmap.width - 4 step 8) for (y in 4 until bitmap.height - 4 step 8) {
                val pixel = bitmap.getPixel(x, y)
                assertTrue("$name exposed the canvas or blended page opacity at ($x,$y): $pixel",
                    android.graphics.Color.blue(pixel) == 0 && android.graphics.Color.alpha(pixel) == 255)
                assertEquals("$name split two pages horizontally at ($x,$y)", center, pixel)
            }

        }
        fun event(p: Float) = NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = p)
        compose.mainClock.autoAdvance = false
        assertEquals(android.graphics.Color.GREEN, frame().getPixel(160, 120))
        compose.runOnIdle { stack.add(second) }
        repeat(48) { advance(); if (it % 4 == 0) opaque("push-$it") }
        assertEquals(android.graphics.Color.RED, frame().getPixel(160, 120))
        compose.runOnIdle { stack.removeLastOrNull() }
        repeat(48) { advance(); if (it % 4 == 0) opaque("simple-pop-$it") }
        assertEquals("ordinary pop did not settle", android.graphics.Color.GREEN, frame().getPixel(160, 120))
        compose.runOnIdle { stack.add(second) }; repeat(48) { advance() }
        compose.runOnIdle { input.backStarted(event(0f)) }; repeat(2) { advance() }
        compose.runOnIdle { input.backProgressed(event(.4f)) }; repeat(3) { advance() }
        opaque("predictive-preview")
        compose.runOnIdle { input.backCancelled() }
        repeat(48) { advance(); if (it % 6 == 0) opaque("cancel-$it") }
        assertEquals(listOf(first, second), stack.toList())
        assertEquals(android.graphics.Color.RED, frame().getPixel(160, 120))
        compose.runOnIdle { stack.removeLastOrNull() }
        repeat(48) { advance(); if (it % 4 == 0) opaque("pop-$it") }
        assertEquals("stack=$stack", android.graphics.Color.GREEN, frame().getPixel(160, 120))
        compose.runOnIdle { stack.add(second) }; repeat(48) { advance() }
        compose.runOnIdle { input.backStarted(event(0f)) }; repeat(2) { advance() }
        compose.runOnIdle { input.backProgressed(event(.6f)) }; repeat(4) { advance() }
        opaque("predictive-completing")
        compose.runOnIdle { input.backCompleted() }; repeat(48) { advance() }
        assertEquals(listOf(first), stack.toList())
        assertEquals(android.graphics.Color.GREEN, frame().getPixel(160, 120))
    }

}
