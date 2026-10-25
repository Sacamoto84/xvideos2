package com.client.xvideos.xlive.ui.following

import com.client.xvideos.xlive.auth.TestAccount
import com.client.xvideos.xlive.model.XLiveFollowed
import com.client.xvideos.xlive.model.XLiveRoomItem
import com.client.xvideos.xlive.network.XLiveSignInRequiredException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

// Без runTest: источники отвечают сразу, а неограниченный диспетчер гонит
// корутины списка до конца прямо внутри вызова.
@OptIn(ExperimentalCoroutinesApi::class)
class XLiveFollowingListTest {

    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private var loads = 0
    private var followed = XLiveFollowed(
        online = listOf(XLiveRoomItem("room_a", age = 24), XLiveRoomItem("room_b")),
        offline = listOf(XLiveRoomItem("room_c"), XLiveRoomItem("room_a")),
    )
    private var load: () -> XLiveFollowed = { followed }
    private val setCalls = mutableListOf<Pair<String, Boolean>>()
    private var set: (String, Boolean) -> Boolean = { _, follow -> follow }

    private fun list(account: TestAccount) = XLiveFollowingList(
        scope = scope,
        account = account,
        loadFollowed = {
            loads++
            load()
        },
        setFollowing = { name, follow ->
            setCalls += name to follow
            set(name, follow)
        },
        nowSeconds = { 1_000L },
    )

    private fun XLiveFollowingList.names() = state.value.list.rooms.map { it.username }

    @Test
    fun `без входа — ничего не грузится, только кнопка входа`() {
        val list = list(TestAccount(signedIn = false))

        list.show()

        assertFalse(list.state.value.isSignedIn)
        assertEquals(0, loads)
    }

    @Test
    fun `со входом — грузится при открытии, сначала в эфире, потом остальные без повторов`() {
        val list = list(TestAccount())
        assertEquals(0, loads)

        list.show()

        assertEquals(1, loads)
        assertEquals(listOf("room_a", "room_b", "room_c"), list.names())
        assertEquals(mapOf("room_a" to true, "room_b" to true, "room_c" to false), list.state.value.list.online)
        assertEquals(24, list.state.value.list.rooms.first().age)
        assertEquals(2, list.state.value.online)
        assertEquals(1_000L, list.state.value.list.snapshotStamp)
    }

    @Test
    fun `повторное открытие не перезапрашивает, свайп вниз — перезапрашивает`() {
        val list = list(TestAccount())
        list.show()

        list.show()
        assertEquals(1, loads)

        list.refresh()
        assertEquals(2, loads)
        assertFalse(list.state.value.list.isChecking)
    }

    @Test
    fun `вход после открытия подвкладки — список грузится сам`() {
        val account = TestAccount(signedIn = false)
        val list = list(account)
        list.show()

        account.signedIn.value = true

        assertTrue(list.state.value.isSignedIn)
        assertEquals(listOf("room_a", "room_b", "room_c"), list.names())
    }

    @Test
    fun `выход — список стирается, повторный вход грузит заново`() {
        val account = TestAccount()
        val list = list(account)
        list.show()

        account.signOut()
        assertFalse(list.state.value.isSignedIn)
        assertEquals(emptyList<String>(), list.names())

        account.signedIn.value = true
        assertEquals(2, loads)
        assertEquals(3, list.names().size)
    }

    @Test
    fun `сайт не узнал сессию — вход снимается`() {
        val account = TestAccount()
        load = { throw XLiveSignInRequiredException() }

        list(account).show()

        assertEquals(1, account.sessionLost)
        assertFalse(account.signedIn.value)
    }

    @Test
    fun `список не пришёл — ошибка, повтор грузит`() {
        load = { throw IOException("нет сети") }
        val list = list(TestAccount())
        list.show()
        assertEquals("Не удалось загрузить подписки", list.state.value.error)
        assertFalse(list.state.value.isLoading)

        load = { followed }
        list.retry()

        assertNull(list.state.value.error)
        assertEquals(3, list.names().size)
    }

    @Test
    fun `отписка — карточка уходит, счётчик подписок растёт, список перечитывается`() {
        val account = TestAccount()
        val list = list(account)
        list.show()
        followed = followed.copy(online = followed.online.take(1))

        list.unfollow("room_b")

        assertEquals(listOf("room_b" to false), setCalls)
        assertEquals(1, account.followRevision.value)
        assertEquals(2, loads)
        assertEquals(listOf("room_a", "room_c"), list.names())
    }

    @Test
    fun `отписка не прошла — карточка на месте, список не перечитывается`() {
        val account = TestAccount()
        set = { _, _ -> throw IOException("нет сети") }
        val list = list(account)
        list.show()

        list.unfollow("room_b")

        assertEquals(listOf("room_a", "room_b", "room_c"), list.names())
        assertEquals(0, account.followRevision.value)
        assertEquals(1, loads)
    }

    @Test
    fun `подписка в комнате — загруженный список перечитывается, незагруженный нет`() {
        val account = TestAccount()
        val list = list(account)

        account.onFollowChanged()
        assertEquals(0, loads)

        list.show()
        account.onFollowChanged()
        assertEquals(2, loads)
    }
}
