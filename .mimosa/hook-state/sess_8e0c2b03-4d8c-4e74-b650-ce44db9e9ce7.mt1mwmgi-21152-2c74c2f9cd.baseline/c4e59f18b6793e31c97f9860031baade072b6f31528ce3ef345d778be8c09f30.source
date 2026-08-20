package com.client.xvideos.common.p2p

import java.io.Serializable

/**
 * Источник данных для экрана отправки P2P.
 *
 * Serializable: лежит внутри Voyager-экрана `ScreenP2pSend`, который Android
 * сохраняет в saved instance state при сворачивании (см. [P2pExportBundle]).
 * Модель item'а — Parcelable, не Serializable, поэтому [DownloadL] хранит её
 * JSON-строкой; разбирает эту строку сам раздел (см. [P2pSendPreparer]).
 */
sealed interface P2pSendSource : Serializable {

    /** Бандл уже в store — отправляем как есть. */
    data class Ready(val bundle: P2pExportBundle) : P2pSendSource

    /** Item не скачан: качаем в outbox на экране отправки и шлём оттуда. */
    data class DownloadL(val itemJson: String) : P2pSendSource

    /** Коллекция L: зипуется в outbox на экране отправки и шлётся одним архивом. */
    data class ShareCollection(val collectionName: String) : P2pSendSource

    /** Коллекция R: зипуется в outbox на экране отправки и шлётся одним архивом. */
    data class ShareCollectionR(val collectionName: String) : P2pSendSource
}
