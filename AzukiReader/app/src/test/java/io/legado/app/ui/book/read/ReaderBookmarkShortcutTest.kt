package io.legado.app.ui.book.read

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.legado.app.data.repository.migrateBookmarkToolButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ReaderBookmarkShortcutTest {
    @Test fun `settings stays first regardless of old menu order and other actions keep their order`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val state = ReadBookUiState(menuConfig = ReadMenuConfig(bottomBarButtons = kotlinx.collections.immutable.persistentListOf(
            ReadBookButtonConfigItem("catalog", true), ReadBookButtonConfigItem("addBookmark", true),
            ReadBookButtonConfigItem("search", true), ReadBookButtonConfigItem("setting", true),
        )))
        val intents = mutableListOf<ReadBookIntent>()
        val buttons = loadToolButtons(context, state, false) { intents += it }
        assertEquals(listOf("setting", "catalog", "addBookmark", "search"), buttons.map { it.id })
        buttons.first().onClick()
        assertEquals(listOf(ReadBookIntent.OpenReadMenuRoute(ReadBookMenuRoute.ReadStyle)), intents)
    }

    @Test fun `fresh and existing menus gain bookmark once and later user choices are preserved`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test-bookmark-shortcut", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        migrateBookmarkToolButton(prefs)
        var raw = prefs.getString("tool_buttons", "").orEmpty()
        assertTrue(raw.contains("catalog,true;addBookmark,true"))
        assertTrue(raw.contains("setting,true"))

        prefs.edit().clear().putString("tool_buttons", "setting,true;catalog,true;addBookmark,false;search,false").commit()
        migrateBookmarkToolButton(prefs)
        raw = prefs.getString("tool_buttons", "").orEmpty()
        assertEquals("setting,true;catalog,true;addBookmark,true;search,false", raw)
        prefs.edit().putString("tool_buttons", "addBookmark,false;setting,true;catalog,true").commit()
        migrateBookmarkToolButton(prefs)
        assertEquals("addBookmark,false;setting,true;catalog,true", prefs.getString("tool_buttons", null))
    }

    @Test fun `reader bookmark button adds at the current position and long press opens this books bookmarks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intents = mutableListOf<ReadBookIntent>()
        val definitions = buildReadMenuActionDefs(context, ReadBookUiState(), false) { intents += it }
        val bookmark = definitions.getValue("addBookmark")
        assertEquals(context.getString(io.legado.app.R.string.bookmark), bookmark.label)
        bookmark.onClick()
        assertEquals(ReadBookIntent.AddBookmark, intents.last())
        assertNotNull(bookmark.onLongClick)
        bookmark.onLongClick!!.invoke()
        assertEquals(ReadBookIntent.OpenBookmarks, intents.last())
    }
}
