package com.client.xvideos.common.p2p.export

import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pType
import java.io.File

/**
 * Exporters берут уже сохранённый/скачанный item и собирают [P2pExportBundle].
 * Корни store передаются параметром (тестируемость); вызывающий подставляет `AppPath.*`.
 * Возвращают null, если файлов на диске нет (v1: «Сначала сохрани»).
 */
object XExporter {
    fun export(storeRoot: File, id: Long): P2pExportBundle? {
        val l = XBundleLocator.locate(storeRoot, id) ?: return null
        return P2pExportBundle(P2pType.X, l.storeRoot, l.files, l.metadataFile)
    }
}

object RExporter {
    fun export(storeRoot: File, userName: String, id: String): P2pExportBundle? {
        val l = RBundleLocator.locate(storeRoot, userName, id) ?: return null
        return P2pExportBundle(P2pType.R, l.storeRoot, l.files, l.metadataFile)
    }
}

object LExporter {
    /** @param itemFolder папка сохранённого L-item (вызывающий находит её через существующий `lFindLikeFolder`). */
    fun export(itemFolder: File): P2pExportBundle? {
        val l = LBundleLocator.locate(itemFolder) ?: return null
        return P2pExportBundle(P2pType.L, l.storeRoot, l.files, l.metadataFile)
    }
}
