package com.client.xvideos.common.fileDB

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Два FileDB могут жить в одном каталоге и различаться только расширением
 * (подписки X: каналы и модели). Уборка `.tmp` у одного не должна попадать
 * в середину записи другого — для этого лок у них общий, по каталогу.
 */
class FileDBSharedDirectoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Сериализатор, который застревает посреди записи — запись в этот момент держит лок. */
    private class BlockingSerializer(
        private val entered: CountDownLatch,
        private val release: CountDownLatch,
    ) : KSerializer<Row> {
        private val delegate = Row.serializer()
        override val descriptor: SerialDescriptor = delegate.descriptor
        override fun deserialize(decoder: Decoder): Row = delegate.deserialize(decoder)
        override fun serialize(encoder: Encoder, value: Row) {
            entered.countDown()
            release.await(5, TimeUnit.SECONDS)
            delegate.serialize(encoder, value)
        }
    }

    @Test
    fun `refresh соседнего FileDB ждёт окончания записи в том же каталоге`() {
        val root = tmp.newFolder("shared")
        val writing = CountDownLatch(1)
        val release = CountDownLatch(1)
        val writerDb = FileDB(root.absolutePath, "models", BlockingSerializer(writing, release))
        val neighbourDb = FileDB(root.absolutePath, "channels", Row.serializer())

        val writer = thread { writerDb.insert("m1", Row("m1", "v")) }
        assertTrue(writing.await(5, TimeUnit.SECONDS))

        val refreshed = CountDownLatch(1)
        val refresher = thread {
            neighbourDb.refresh()
            refreshed.countDown()
        }
        val refreshedDuringWrite = refreshed.await(300, TimeUnit.MILLISECONDS)

        release.countDown()
        writer.join()
        refresher.join()

        assertFalse("refresh прошёл, пока сосед писал в тот же каталог", refreshedDuringWrite)
        assertTrue(writerDb.contains("m1"))
    }
}
