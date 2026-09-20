package com.client.xvideos.r.ui.fullscreen

import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.URL1
import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Экраны Voyager обязаны переживать Java-сериализацию.
 *
 * `cafe.adriel.voyager.core.screen.Screen` на Android объявлен как
 * `interface Screen : Serializable`, а `SnapshotStateStack` сохраняет стек через
 * `listSaver(save = { stack -> stack.items })`, то есть кладёт в saved state
 * **сами объекты экранов**. Когда система парселит saved state активити, экран
 * уходит через `Parcel.writeSerializable`, и любое несериализуемое поле роняет
 * приложение:
 *
 * ```
 * android.os.BadParcelableException: Parcelable encountered IOException writing
 *   serializable object (name = com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreen)
 * Caused by: java.io.NotSerializableException: com.client.xvideos.r.model.GifsInfo
 * ```
 *
 * Краш ловится не на каждый сворачивание: парселинг случается тогда, когда
 * система реально сохраняет запись активити, а не при каждом уходе в фон.
 * Поэтому проверка тестом, а не руками на устройстве.
 */
class ScreenRedFullScreenSerializationTest {

    private fun serialize(value: Any) {
        ObjectOutputStream(ByteArrayOutputStream()).use { it.writeObject(value) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> roundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos -> oos.writeObject(value) }
            baos.toByteArray()
        }
        return ByteArrayInputStream(bytes).use { bais ->
            ObjectInputStream(bais).use { ois -> ois.readObject() as T }
        }
    }

    @Test
    fun `экран полноэкранной ленты переживает запись в saved state`() {
        val screen = ScreenRedFullScreen(
            item = GifsInfo(
                id = "abc123",
                userName = "creator",
                tags = listOf("tag1", "tag2"),
                urls = URL1(sd = "https://example/sd.mp4", hd = "https://example/hd.mp4"),
                niches = listOf("niche"),
            ),
            feedKey = "RFeed:Home::1",
            startIndex = 7,
        )
        val restored = roundTrip(screen)
        assertEquals("RedFullScreen:abc123:7", restored.key)
        assertEquals("abc123", restored.item.id)
    }

    @Test
    fun `модель ролика сериализуется отдельно от экрана`() {
        // Отдельно от экрана: GifsInfo лежит ещё и в списках, которые уходят в
        // saved state других экранов — контракт нужен самой модели, а не одному
        // месту её использования.
        serialize(GifsInfo(id = "abc123", userName = "creator"))
    }

    @Test
    fun `значения по умолчанию тоже сериализуются`() {
        // Пустые списки и null-поля — самый частый случай на первом кадре ленты.
        serialize(GifsInfo())
    }
}
