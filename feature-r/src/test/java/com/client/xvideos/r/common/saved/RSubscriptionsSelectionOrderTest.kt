package com.client.xvideos.r.common.saved

import com.client.xvideos.r.model.UserInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Порядок авторов в выборе ленты подписок.
 *
 * Новый автор дописывался в конец выбора, а после перезапуска оказывался в
 * начале: список с диска читается от новых к старым. Выбор теперь идёт в
 * порядке списка авторов.
 */
class RSubscriptionsSelectionOrderTest {

    private fun creator(name: String) = UserInfo(username = name, profileImageUrl = "avatar_$name")

    @Test
    fun `выбор идёт в порядке списка авторов`() {
        val current = listOf(SelectedCreator("a", true, "avatar_a"), SelectedCreator("b", true, "avatar_b"))
        // «c» подписан последним: в списке авторов он первый.
        val creators = listOf(creator("c"), creator("b"), creator("a"))

        val synced = syncSelectedCreators(current, creators)

        assertEquals(listOf("c", "b", "a"), synced.map { it.name })
    }

    @Test
    fun `снятая отметка автора сохраняется, новый автор отмечен`() {
        val current = listOf(SelectedCreator("a", false, "avatar_a"))

        val synced = syncSelectedCreators(current, listOf(creator("b"), creator("a")))

        assertEquals(
            listOf(SelectedCreator("b", true, "avatar_b"), SelectedCreator("a", false, "avatar_a")),
            synced,
        )
    }

    @Test
    fun `автор, от которого отписались, из выбора уходит`() {
        val current = listOf(SelectedCreator("a", true, null), SelectedCreator("b", true, null))

        val synced = syncSelectedCreators(current, listOf(creator("b")))

        assertEquals(listOf("b"), synced.map { it.name })
    }
}
