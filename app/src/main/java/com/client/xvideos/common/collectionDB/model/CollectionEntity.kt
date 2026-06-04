package com.client.xvideos.common.collectionDB.model

import com.google.gson.annotations.SerializedName

data class CollectionEntity<T>(
    @SerializedName("collection") val collection: String,
    @SerializedName("list")       val items: List<T>
)