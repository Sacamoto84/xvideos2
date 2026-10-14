package com.client.xvideos.arch

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Сторож адаптивной раскладки.
 *
 * На раскрытом складном телефоне окно 700–880dp в ширину, и сетка с жёсткими
 * двумя колонками растягивает карточки вдвое. Колонки, ширина карточек ряда и
 * ширина контента берутся из `common/ui/adaptive/`; тест не даёт новой сетке
 * или ряду снова завязаться на число или на размер экрана.
 *
 * Высота экрана (`screenHeightDp`) не запрещена: доля высоты в диалогах и в
 * панели фильтра от ширины окна не зависит.
 *
 * Тест падает в обе стороны: и на новом нарушении, и на записи [BASELINE],
 * которая больше не нарушается.
 */
class AdaptiveLayoutTest {

    @Test
    fun `сетки и ряды берут колонки и ширину из common-ui-adaptive`() {
        val found = ProjectSources.kotlinSourcesWithoutComments()
            .filterNot { (path, _) -> path.startsWith(ADAPTIVE_PACKAGE) }
            .flatMap { (path, code) ->
                RULES.asSequence()
                    .filter { (_, pattern) -> pattern.containsMatchIn(code) }
                    .map { (name, _) -> "$path: $name" }
            }
            .toSortedSet()

        assertEquals(
            "Сетка или ряд снова завязаны на жёсткое число колонок или на размер экрана. " +
                "Колонки — adaptiveGridColumns() / adaptiveUserGridColumns(), ширина — " +
                "currentWindowWidthDp() / rowItemWidth(), полная строка — GridItemSpan(maxLineSpan). " +
                "Запись, которая больше не нарушается, уберите из BASELINE.",
            BASELINE,
            found,
        )
    }

    private companion object {
        const val ADAPTIVE_PACKAGE = "common/ui/adaptive/"

        val RULES = listOf(
            "колонки числом" to Regex("""\b(?:Staggered)?GridCells\.Fixed\(\s*\d"""),
            "строка сетки числом" to Regex("""\bGridItemSpan\(\s*\d"""),
            "ширина экрана" to Regex("""\bscreenWidthDp\b"""),
            "ориентация экрана" to Regex("""\bConfiguration\.ORIENTATION_(?:LANDSCAPE|PORTRAIT)\b"""),
        )

        /** Ещё не исправленные места. Каждая задача плана убирает свои строки. */
        val BASELINE = sortedSetOf(
            "common/collectionDB/ui/CollectionsGrid.kt: колонки числом",
            "l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt: ширина экрана",
            "l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt: ширина экрана",
            "l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt: ширина экрана",
            "l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt: колонки числом",
            "l/ui/screens/explorer/tab/saved/collection/CollectionsGrid.kt: колонки числом",
            "l/ui/screens/explorer/tab/saved/subscribedAlbums/molecule/SubscribedAlbumsGrid.kt: колонки числом",
            "l/ui/screens/screenAlbumList/molecule/AlbumListPageGrid.kt: колонки числом",
            "x/screens/actresses/molecule/ActressesIndexGrid.kt: колонки числом",
            "x/screens/actresses/molecule/ActressesIndexGrid.kt: строка сетки числом",
            "x/screens/channel/molecule/ChannelVideosPager.kt: колонки числом",
            "x/screens/dashboards/molecule/DashboardsPaginatedListContent.kt: колонки числом",
            "x/screens/favorites/molecule/FavoritesContent.kt: колонки числом",
            "x/screens/history/molecule/HistoryGrid.kt: колонки числом",
            "x/screens/tags/molecule/TagsPaginatedListScreen.kt: ориентация экрана",
        )
    }
}
