package com.client.xvideos.r.ui.ui

internal sealed interface MetadataLoadState<out T> {
    data object Loading : MetadataLoadState<Nothing>
    data class Ready<T>(val value: T) : MetadataLoadState<T>
    data class Failed(val message: String) : MetadataLoadState<Nothing>
}
