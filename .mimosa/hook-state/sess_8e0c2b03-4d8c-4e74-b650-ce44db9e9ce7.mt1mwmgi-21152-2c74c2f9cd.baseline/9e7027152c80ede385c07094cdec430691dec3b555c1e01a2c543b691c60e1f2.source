package com.client.xvideos.common.p2p

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Подготовка файлов на экране отправки для источника, который знает про раздел.
 *
 * Базовый слой умеет отдать готовый бандл ([P2pSendSource.Ready]) и запаковать
 * коллекцию — там он работает с файлами и путями. А вот скачать несохранённый
 * item он не может: нужны сеть и модель раздела. Поэтому реализация живёт в
 * разделе, ставится в [P2pSendPreparers] при старте процесса, а экран отправки
 * вызывает её вслепую.
 */
interface P2pSendPreparer {

    /** Доля подготовки 0f..1f; [PROGRESS_HIDDEN] — индикатор показывать нечему. */
    val progress: StateFlow<Float>

    /**
     * Скачивает item, описанный в [itemJson] источника [P2pSendSource.DownloadL],
     * в [outboxRoot].
     *
     * @return папка со скачанными файлами, готовая к экспорту.
     */
    suspend fun downloadItem(itemJson: String, outboxRoot: File): File

    companion object {
        const val PROGRESS_HIDDEN = -2f
    }
}

/**
 * Реализация создаётся на каждый экран отправки: [scope] — это область жизни
 * экрана, к ней привязан прогресс.
 */
fun interface P2pSendPreparerFactory {
    fun create(context: Context, scope: CoroutineScope): P2pSendPreparer
}

/** Реестр реализаций по разделам. Заполняется в точке сборки, см. `App`. */
object P2pSendPreparers {

    @Volatile
    var l: P2pSendPreparerFactory? = null
}
