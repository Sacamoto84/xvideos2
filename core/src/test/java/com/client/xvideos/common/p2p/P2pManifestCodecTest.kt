package com.client.xvideos.common.p2p

import org.junit.Assert.assertEquals
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertThrows
import org.junit.Test

class P2pManifestCodecTest {

    private val sample = P2pManifest(
        type = P2pType.L,
        metadataFileName = "metadata.json",
        files = listOf(
            P2pManifestFile(name = "media.jpg", relativePath = "album_x/media.jpg", payloadId = 10L, size = 123L),
            P2pManifestFile(name = "metadata.json", relativePath = "album_x/metadata.json", payloadId = 11L, size = 456L),
        ),
    )

    @Test
    fun `json round trip preserves all fields`() {
        val json = P2pManifestCodec.toJson(sample)
        val back = P2pManifestCodec.fromJson(json)
        assertEquals(sample, back)
    }

    @Test
    fun `bytes round trip preserves all fields`() {
        val bytes = P2pManifestCodec.toBytes(sample)
        val back = P2pManifestCodec.fromBytes(bytes)
        assertEquals(sample, back)
    }

    @Test
    fun `album manifest round trip`() {
        val m = P2pManifest(
            type = P2pType.L_ALBUM,
            metadataFileName = "42.album",
            files = listOf(P2pManifestFile("42.album", "42.album", 1L, 10L)),
        )
        assertEquals(m, P2pManifestCodec.fromBytes(P2pManifestCodec.toBytes(m)))
    }

    @Test
    fun `collection manifest round trip`() {
        val m = P2pManifest(
            type = P2pType.L_COLLECTION,
            metadataFileName = null,
            files = listOf(P2pManifestFile("MyCol.zip", "MyCol.zip", 1L, 100L)),
        )
        assertEquals(m, P2pManifestCodec.fromBytes(P2pManifestCodec.toBytes(m)))
    }

    /*
     * Ниже — разбор манифеста, пришедшего с чужого устройства. Структуру
     * проверяет kotlinx: неизвестный type или отсутствующий files дают
     * SerializationException прямо на разборе, а не null в non-null поле,
     * который выстреливал позже и вдали от места разбора (так вёл себя Gson).
     * Битый манифест обязан отвергаться здесь, чтобы runCatching в
     * P2pReceiveController его поймал.
     */

    @Test
    fun `неизвестный тип отвергается`() {
        // Так выглядит бандл из более новой версии приложения.
        val json = """{"type":"L_SOMETHING_NEW","metadataFileName":null,"files":[]}"""

        assertThrows(SerializationException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }

    @Test
    fun `манифест без списка файлов отвергается`() {
        val json = """{"type":"L","metadataFileName":"metadata.json"}"""

        assertThrows(SerializationException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }

    @Test
    fun `файл без пути отвергается`() {
        val json = """{"type":"L","metadataFileName":null,"files":[{"name":"a.jpg","payloadId":1,"size":2}]}"""

        assertThrows(SerializationException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }

    @Test
    fun `пустой json отвергается`() {
        assertThrows(SerializationException::class.java) {
            P2pManifestCodec.fromJson("null")
        }
    }

    @Test
    fun `файл с выходом за корень отвергается`() {
        val json = """{"type":"L","metadataFileName":null,""" +
            """"files":[{"name":"a.jpg","relativePath":"../../shared_prefs/x.xml","payloadId":1,"size":2}]}"""

        assertThrows(IllegalArgumentException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }

    @Test
    fun `файл с абсолютным путём принимается, но кладётся внутрь store`() {
        // Ведущий слеш срезается нормализацией, поэтому манифест валиден, а
        // P2pBundleInstaller положит файл внутрь storeRoot. Проверка того, что
        // наружу он не уйдёт — в P2pBundleInstallerTest.
        val json = """{"type":"L","metadataFileName":null,""" +
            """"files":[{"name":"a.jpg","relativePath":"/data/data/com.client.xvideos/a.jpg","payloadId":1,"size":2}]}"""

        val parsed = P2pManifestCodec.fromJson(json)
        assertEquals(1, parsed.files.size)
    }

    @Test
    fun `манифест от Gson-версии приложения читается`() {
        // Байт-в-байт то, что писала прежняя реализация на Gson: те же имена
        // полей, тот же порядок. Пир со старой сборкой должен доехать до новой,
        // иначе миграция ломает передачу между версиями.
        val legacy = """{"type":"L_ALBUM","metadataFileName":"42.album","files":""" +
            """[{"name":"42.album","relativePath":"42.album","payloadId":7,"size":10}]}"""

        val parsed = P2pManifestCodec.fromJson(legacy)

        assertEquals(P2pType.L_ALBUM, parsed.type)
        assertEquals("42.album", parsed.metadataFileName)
        assertEquals(listOf(P2pManifestFile("42.album", "42.album", 7L, 10L)), parsed.files)
    }

    @Test
    fun `наш манифест читается разбором без строгих полей`() {
        // Обратная сторона: то, что пишем мы, должна принять и старая сборка.
        // Проверяем, что в выдаче нет ничего сверх известных полей и что
        // metadataFileName = null сериализуется явно, а не пропадает.
        val json = P2pManifestCodec.toJson(
            P2pManifest(
                type = P2pType.R_COLLECTION,
                metadataFileName = null,
                files = listOf(P2pManifestFile("c.zip", "c.zip", 1L, 2L)),
            )
        )

        assertEquals(
            """{"type":"R_COLLECTION","metadataFileName":null,"files":""" +
                """[{"name":"c.zip","relativePath":"c.zip","payloadId":1,"size":2}]}""",
            json,
        )
    }

    @Test
    fun `файл с пустым путём отвергается`() {
        val json = """{"type":"L","metadataFileName":null,""" +
            """"files":[{"name":"a.jpg","relativePath":"","payloadId":1,"size":2}]}"""

        assertThrows(IllegalArgumentException::class.java) {
            P2pManifestCodec.fromJson(json)
        }
    }
}
