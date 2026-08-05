package com.client.xvideos.l.featured.saved

import android.content.Context
import com.client.xvideos.common.p2p.P2pSendPreparer
import com.client.xvideos.common.p2p.P2pSendPreparerFactory
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.Luscious
import com.google.gson.Gson
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Источник отправки для несохранённого L-item: базовый слой хранит его
 * JSON-строкой и про [PicsDetails] не знает.
 */
fun lP2pSendSource(item: PicsDetails): P2pSendSource.DownloadL =
    P2pSendSource.DownloadL(Gson().toJson(item))

/** Обратный разбор [lP2pSendSource]; null, если строка битая. */
fun lP2pItem(itemJson: String): PicsDetails? =
    runCatching { Gson().fromJson(itemJson, PicsDetails::class.java) }.getOrNull()

/** Доступ к Hilt-синглтонам из объекта вне DI-графа. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LSendEntryPoint {
    fun luscious(): Luscious
}

/**
 * L-реализация [P2pSendPreparer]: качает item в outbox-зеркало и отдаёт папку.
 *
 * Зеркало повторяет структуру `/xvideos`, поэтому relativePath манифеста
 * совпадает с боевым — на приёме файлы лягут туда же, куда легли бы из store.
 */
class LSendPreparer(
    context: Context,
    scope: CoroutineScope,
) : P2pSendPreparer {

    private val downloadProgress = LDownloadProgress(scope)

    override val progress: StateFlow<Float> = downloadProgress.percentDownload

    private val luscious: Luscious = EntryPointAccessors
        .fromApplication(context.applicationContext, LSendEntryPoint::class.java)
        .luscious()

    override suspend fun downloadItem(itemJson: String, outboxRoot: File): File {
        val item = lP2pItem(itemJson) ?: error("Битые данные item")
        return lPersistPicsDetailsToFolder(item, outboxRoot, luscious, downloadProgress).getOrThrow()
    }

    companion object : P2pSendPreparerFactory {
        override fun create(context: Context, scope: CoroutineScope): P2pSendPreparer =
            LSendPreparer(context, scope)
    }
}
