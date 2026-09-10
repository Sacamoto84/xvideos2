package com.client.xvideos.p2p

import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.r.common.saved.SavedRed
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Доступ к хранилищам разделов из [sectionBundleImporter] — обычной функции
 * вне DI-графа.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface P2pRefreshEntryPoint {
    fun savedL(): SavedL
    fun savedRed(): SavedRed
}
