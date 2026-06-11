package com.client.xvideos.common.p2p

import java.io.File

/**
 * Готовый к отправке бандл: набор файлов store + контекст для построения манифеста.
 *
 * @param type источник.
 * @param storeRoot корень, относительно которого считаются relativePath файлов.
 * @param files файлы для отправки (медиа, превью, метаданные).
 * @param metadataFile файл-метаданные среди [files], или null.
 */
data class P2pExportBundle(
    val type: P2pType,
    val storeRoot: File,
    val files: List<File>,
    val metadataFile: File?,
)
