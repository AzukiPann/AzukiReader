package io.legado.app.feature.bookmarks.export

import androidx.lifecycle.ViewModelStore
import io.legado.app.domain.gateway.ExcerptExportGateway
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportAllExcerptsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var count = 0
    private var destination = ""
    private val response = CompletableDeferred<Int>()
    private val gateway = object : ExcerptExportGateway {
        override suspend fun exportAll(destination: String): Int {
            count++
            this@ExportAllExcerptsViewModelTest.destination = destination
            return response.await()
        }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun model() = ExportAllExcerptsViewModel(gateway).also { store.put("export", it) }

    @Test fun `export serializes repeated taps and completion survives until acknowledged`() = runTest(dispatcher) {
        val vm = model()
        vm.onIntent(ExcerptExportIntent.Export("content://documents/all.md"))
        vm.onIntent(ExcerptExportIntent.Export("content://documents/duplicate.md"))
        runCurrent()
        assertTrue(vm.uiState.value.exporting)
        assertEquals(1, count)
        assertEquals("content://documents/all.md", destination)
        response.complete(12); runCurrent()
        assertFalse(vm.uiState.value.exporting)
        assertEquals(12, vm.uiState.value.exportedCount)
        vm.onIntent(ExcerptExportIntent.MessageShown)
        assertNull(vm.uiState.value.exportedCount)
    }
    @Test fun `provider failures never report success`() = runTest(dispatcher) {
        val vm = model()
        vm.onIntent(ExcerptExportIntent.Export("content://invalid")); runCurrent()
        response.completeExceptionally(java.io.IOException("write rejected")); runCurrent()
        assertTrue(vm.uiState.value.failed)
        assertNull(vm.uiState.value.exportedCount)
        vm.onIntent(ExcerptExportIntent.MessageShown)
        assertFalse(vm.uiState.value.failed)
    }
    @Test fun `cancelling an export does not report a provider failure`() = runTest(dispatcher) {
        val vm = model()
        vm.onIntent(ExcerptExportIntent.Export("content://documents/cancel.md")); runCurrent()
        assertTrue(vm.uiState.value.exporting)
        store.clear(); runCurrent()
        assertFalse(vm.uiState.value.exporting)
        assertFalse(vm.uiState.value.failed)
        assertNull(vm.uiState.value.exportedCount)
    }

}
