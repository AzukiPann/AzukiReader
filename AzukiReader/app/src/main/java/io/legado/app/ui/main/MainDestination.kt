package io.legado.app.ui.main

import androidx.annotation.StringRes
import io.legado.app.R
import kotlinx.collections.immutable.persistentListOf

/** Membership changes retain the selected route; hiding it falls back to the bookshelf. */
internal fun preservedDestinationIndex(selected: MainDestination?, destinations: List<MainDestination>): Int =
    destinations.indexOf(selected).takeIf { it >= 0 }
        ?: destinations.indexOf(MainDestination.Bookshelf).coerceAtLeast(0)

sealed class MainDestination(
    val route: String,
    @StringRes val labelId: Int
) {
    object Home : MainDestination(
        route = "home",
        labelId = R.string.home
    )

    object Bookshelf : MainDestination(
        route = "bookshelf",
        labelId = R.string.bookshelf
    )

    object Bookmarks : MainDestination(
        route = "bookmarks",
        labelId = R.string.bookmark
    )

    object Explore : MainDestination(
        route = "explore",
        labelId = R.string.discovery
    )

    object Rss : MainDestination(
        route = "rss",
        labelId = R.string.rss
    )

    object My : MainDestination(
        route = "my",
        labelId = R.string.my
    )

    companion object {
        val mainDestinations by lazy { persistentListOf<MainDestination>(Home, Bookshelf, Bookmarks, Explore, Rss, My) }

        fun ordered(order: String): List<MainDestination> {
            val byRoute = mainDestinations.associateBy { it.route }
            val ordered = order
                .split(',')
                .map(String::trim)
                .distinct()
                .mapNotNull(byRoute::get)
            val withBookmarks = ordered.toMutableList()
            if (Bookmarks !in withBookmarks) {
                val shelf = withBookmarks.indexOf(Bookshelf)
                val insertion = if (shelf >= 0) shelf + 1 else withBookmarks.indexOf(My).takeIf { it >= 0 } ?: withBookmarks.size
                withBookmarks.add(insertion, Bookmarks)
            }
            return withBookmarks + mainDestinations.filterNot { it in withBookmarks }
        }
    }
}
