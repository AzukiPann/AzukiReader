package io.legado.app.feature.performance

import androidx.lifecycle.ViewModelStore
import io.legado.app.domain.gateway.AppShellSettingsGateway
import io.legado.app.domain.gateway.ThemeSettingsGateway
import io.legado.app.domain.model.settings.*
import io.legado.app.core.designsystem.wenku8.effects.resolveWenku8Effects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var fail = false
    private val theme = object : ThemeSettingsGateway {
        override val settings = MutableStateFlow(ThemeSettings())
        override val currentSettings get() = settings.value
        override suspend fun update(transform: (ThemeSettings) -> ThemeSettings) {
            if (fail) error("disk unavailable")
            settings.value = transform(settings.value)
        }
    }
    private val shell = object : AppShellSettingsGateway {
        override val settings = MutableStateFlow(AppShellSettings())
        override val currentSettings get() = settings.value
        override suspend fun update(transform: (AppShellSettings) -> AppShellSettings) {
            if (fail) error("disk unavailable")
            settings.value = transform(settings.value)
        }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun model() = PerformanceViewModel(theme, shell).also { store.put("performance", it) }

    @Test fun `reading rate defaults to 30 and persists independently of application rate`() = runTest(dispatcher) {
        val vm = model()
        assertEquals(30, vm.uiState.value.theme.readerRefreshRate)
        vm.onIntent(PerformanceIntent.RefreshRate(120))
        vm.onIntent(PerformanceIntent.ReaderRefreshRate(60))
        runCurrent()
        assertEquals(120, theme.currentSettings.wenku8RefreshRate)
        assertEquals(60, theme.currentSettings.readerRefreshRate)
        val restored = model(); runCurrent()
        assertEquals(60, restored.uiState.value.theme.readerRefreshRate)
        restored.onIntent(PerformanceIntent.RefreshRate(-1)); runCurrent()
        assertEquals(60, theme.currentSettings.readerRefreshRate)
        fail = true
        restored.onIntent(PerformanceIntent.ReaderRefreshRate(30)); runCurrent()
        assertTrue(restored.uiState.value.saveFailed)
        assertEquals(60, theme.currentSettings.readerRefreshRate)
    }

    @Test fun `navigation switches persist separately without changing order or reading logic`() = runTest(dispatcher) {
        val vm = model()
        val original = shell.currentSettings
        vm.onIntent(PerformanceIntent.Discovery(false))
        vm.onIntent(PerformanceIntent.Rss(false))
        runCurrent()
        assertEquals(original.copy(showDiscovery = false, showRss = false), shell.currentSettings)
        assertFalse(vm.uiState.value.shell.showRss)
        val restored = model(); runCurrent()
        assertFalse(restored.uiState.value.shell.showDiscovery)
        restored.onIntent(PerformanceIntent.Discovery(true)); runCurrent()
        assertTrue(shell.currentSettings.showDiscovery)
        assertFalse(shell.currentSettings.showRss)
    }

    @Test fun `preset suppresses effects independently and restores original appearance`() = runTest(dispatcher) {
        val vm = model()
        vm.onIntent(PerformanceIntent.Blur(15f)); vm.onIntent(PerformanceIntent.Opacity(.42f))
        vm.onIntent(PerformanceIntent.RefreshRate(90)); runCurrent()
        val original = theme.currentSettings
        vm.onIntent(PerformanceIntent.LowPerformance(true)); runCurrent()
        var policy = resolveWenku8Effects(theme.currentSettings, false, false, true)
        assertFalse(policy.motion || policy.optics || policy.blur || policy.edgeBlur || policy.spatial || policy.shelf3d)
        vm.onIntent(PerformanceIntent.Retain(Wenku8PerformanceEffect.Blur, true)); runCurrent()
        policy = resolveWenku8Effects(theme.currentSettings, false, false, true)
        assertTrue(policy.blur)
        assertFalse(policy.optics || policy.motion || policy.edgeBlur || policy.spatial || policy.shelf3d)
        vm.onIntent(PerformanceIntent.LowPerformance(false)); runCurrent()
        assertEquals(original.copy(wenku8RetainedEffects = 2), theme.currentSettings)
        policy = resolveWenku8Effects(theme.currentSettings, false, false, true)
        assertTrue(policy.motion && policy.optics && policy.blur && policy.edgeBlur && policy.spatial && policy.shelf3d)
        vm.onIntent(PerformanceIntent.ResetLightweight); runCurrent()
        assertTrue(theme.currentSettings.wenku8LowPerformance)
        assertEquals(0, theme.currentSettings.wenku8RetainedEffects)
        assertEquals(15f, theme.currentSettings.wenku8GlassBlur)
    }

    @Test fun `reduce motion preserves optics and platform restrictions override retained effects`() {
        val reduced = ThemeSettings(wenku8ReducedEffects = true)
        val policy = resolveWenku8Effects(reduced, false, false, true)
        assertFalse(policy.motion)
        assertTrue(policy.optics && policy.blur && policy.edgeBlur)
        assertFalse(policy.spatial)
        assertTrue(policy.shelf3d)
        val onlyShelf = resolveWenku8Effects(ThemeSettings(wenku8LowPerformance = true, wenku8RetainedEffects = 16), false, false, true)
        assertTrue(onlyShelf.shelf3d)
        assertFalse(onlyShelf.motion || onlyShelf.spatial)
        val onlyMotion = resolveWenku8Effects(ThemeSettings(wenku8LowPerformance = true, wenku8RetainedEffects = 8), false, false, true)
        assertTrue(onlyMotion.motion && onlyMotion.spatial)
        assertFalse(onlyMotion.shelf3d)
        val retained = reduced.copy(wenku8LowPerformance = true, wenku8RetainedEffects = 31)
        val saver = resolveWenku8Effects(retained, true, false, true)
        assertFalse(saver.motion || saver.spatial || saver.optics || saver.blur || saver.edgeBlur || saver.shelf3d)
        assertFalse(resolveWenku8Effects(ThemeSettings(), false, false, false).motion)
    }

    @Test fun `clamping resets and failed saves retain real persisted state`() = runTest(dispatcher) {
        val vm = model()
        vm.onIntent(PerformanceIntent.Blur(50f)); vm.onIntent(PerformanceIntent.Opacity(-3f)); runCurrent()
        assertEquals(24f, theme.currentSettings.wenku8GlassBlur)
        assertEquals(0f, theme.currentSettings.wenku8GlassOpacity)
        fail = true
        vm.onIntent(PerformanceIntent.ResetGlass); runCurrent()
        assertTrue(vm.uiState.value.saveFailed)
        assertEquals(24f, theme.currentSettings.wenku8GlassBlur)
        fail = false
        vm.onIntent(PerformanceIntent.ResetGlass); runCurrent()
        assertFalse(vm.uiState.value.saveFailed)
        assertEquals(8f, theme.currentSettings.wenku8GlassBlur)
        assertEquals(.87f, theme.currentSettings.wenku8GlassOpacity)
    }
}
