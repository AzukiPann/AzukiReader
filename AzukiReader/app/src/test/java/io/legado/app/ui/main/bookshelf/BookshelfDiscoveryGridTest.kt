package io.legado.app.ui.main.bookshelf

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.material3.MaterialTheme
import coil3.ImageLoader
import coil3.ColorImage
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.decode.DataSource
import kotlinx.coroutines.CompletableDeferred
import org.koin.core.context.loadKoinModules
import io.legado.app.ui.widget.components.image.cover.CoilBookCover
import java.util.concurrent.atomic.AtomicBoolean
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.domain.model.settings.BookshelfSettings
import io.legado.app.help.config.AppConfigStore
import io.legado.app.ui.theme.AppTheme
import kotlinx.collections.immutable.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import splitties.init.injectAsAppCtx
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import io.legado.app.domain.usecase.CoverAlbumUseCase
import io.legado.app.data.repository.CoverAlbumRepository
import io.legado.app.data.repository.SettingsRepository

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "zh-rCN-w420dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BookshelfDiscoveryGridTest {
    @get:Rule val compose = createComposeRule()
    @Before fun initialize() {
        val app = RuntimeEnvironment.getApplication()
        app.injectAsAppCtx()
        AppConfigStore.init(app)
        stopKoin()
        startKoin { modules(module {
            single { CoverAlbumUseCase(CoverAlbumRepository(app, SettingsRepository())) }
        }) }
    }
    @After fun cleanup() { stopKoin() }

    @Test fun `archive loading stays blank paper until the cover arrives without changing bounds`() {
        val release = CompletableDeferred<Unit>()
        val requested = AtomicBoolean(false)
        val finished = AtomicBoolean(false)
        val loader = ImageLoader.Builder(RuntimeEnvironment.getApplication()).components {
            add(Fetcher.Factory<Any> { _, _, _ -> Fetcher {
                requested.set(true)
                release.await()
                ImageFetchResult(ColorImage(android.graphics.Color.CYAN, 100, 140), false, DataSource.MEMORY)
            } })
        }.build()
        loadKoinModules(module { single<ImageLoader> { loader } })
        var paper = 0
        try {
            compose.setContent {
                AppTheme(AppUiConfiguration(), darkTheme = false) {
                    paper = MaterialTheme.colorScheme.surfaceContainer.toArgb()
                    CoilBookCover("Pending title", "Pending author", "https://example.invalid/cover.jpg",
                        modifier = Modifier.size(100.dp, 140.dp).testTag("pending-cover"),
                        blankWhileLoading = true, showLoadingPlaceholder = false,
                        onLoadFinish = { finished.set(true) })
                }
            }
            compose.waitUntil(5000) { requested.get() }
            val before = compose.onNodeWithTag("pending-cover").captureToImage().asAndroidBitmap()
            for (y in 5 until before.height - 5) for (x in 5 until before.width - 5) {
                assertEquals("Loading must not paint fallback text or icons", paper, before.getPixel(x, y))
            }
            release.complete(Unit)
            compose.waitUntil(5000) { finished.get() }
            val after = compose.onNodeWithTag("pending-cover").captureToImage().asAndroidBitmap()
            assertEquals(before.width, after.width)
            assertEquals(before.height, after.height)
            assertEquals(android.graphics.Color.CYAN, after.getPixel(after.width / 2, after.height / 2))
        } finally { loader.shutdown() }
    }

    @Test fun `archive header spans above grid and grid column settings remain effective`() {
        val books = (0 until 18).map { index ->
            BookShelfItem("book-$index", "书籍 $index", "作者", "", "", null, null, "第一章", 0, 0,
                null, 0, 0, 1, 0, 0, 0, index).toUiItem()
        }.toImmutableList()
        var settings by mutableStateOf(BookshelfSettings(bookshelfLayoutModePortrait = 1,
            bookshelfLayoutGridPortrait = 3, bookshelfGridFixedColumns = true, wenku8ShowArchiveShelf = true))
        lateinit var grid: androidx.compose.foundation.lazy.grid.LazyGridState
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AppTheme(AppUiConfiguration(), darkTheme = false) {
                    grid = rememberLazyGridState()
                    Box(Modifier.requiredSize(420.dp, 1000.dp)) {
                        BookshelfPage(gridState = grid, paddingValues = PaddingValues(), books = books,
                            uiState = BookshelfUiState(items = books, settings = settings, isInitialLoading = false),
                            selectedBookUrls = persistentSetOf(), canReorderBooks = false,
                            onToggleBookSelection = {}, draggingBooks = null, pendingSavedBooks = null,
                            onDragStarted = {}, onMoveBook = { _, _, _ -> }, onDragFinished = {},
                            onGlobalSearch = {}, onBookClick = { _, _ -> }, onBookLongClick = { _, _ -> },
                            sharedCoverGroupId = 0L)
                    }
                }
            }
        }
        compose.onNodeWithText("01 / 12").assertExists()
        compose.runOnIdle { settings = settings.copy(wenku8ArchiveShelfBookCount = 3) }
        compose.onNodeWithText("01 / 03").assertExists()
        compose.runOnIdle {
            val items = grid.layoutInfo.visibleItemsInfo
            val header = items.first { it.key == "wenku8-discovery-header" }
            assertTrue("Books beyond the carousel limit remain in the lower grid", items.any { it.key == "book-3" })
            val first = items.first { it.key == "book-0" }
            val second = items.first { it.key == "book-1" }
            val third = items.first { it.key == "book-2" }
            assertTrue(first.offset.y >= header.offset.y + header.size.height)
            assertEquals(first.offset.y, second.offset.y)
            assertEquals(first.offset.y, third.offset.y)
            assertTrue(second.offset.x > first.offset.x)
            settings = settings.copy(bookshelfLayoutGridPortrait = 2)
        }
        compose.runOnIdle {
            val items = grid.layoutInfo.visibleItemsInfo
            assertEquals(items.first { it.key == "book-0" }.offset.y, items.first { it.key == "book-1" }.offset.y)
            assertTrue(items.first { it.key == "book-2" }.offset.y > items.first { it.key == "book-0" }.offset.y)
            settings = settings.copy(wenku8ShowArchiveShelf = false)
        }
        compose.runOnIdle {
            val items = grid.layoutInfo.visibleItemsInfo
            val header = items.first { it.key == "wenku8-discovery-header" }
            assertTrue(header.size.height < 200)
            assertEquals(items.first { it.key == "book-0" }.offset.y, items.first { it.key == "book-1" }.offset.y)
        }
    }
}
