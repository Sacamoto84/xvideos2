package com.client.xvideos.r.model

import com.google.gson.annotations.SerializedName

data class CreatorsResponse(
    @SerializedName("page") val page: Int = 0,             //
    @SerializedName("pages")  val pages: Int = 0,            //
    @SerializedName("total") val total: Int = 0,            //
    @SerializedName("items")  val items: List<UserInfo> = emptyList(), //
)


