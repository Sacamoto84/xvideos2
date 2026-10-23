package com.client.xvideos.x.screens.channel.model

import com.client.xvideos.x.model.ChannelRankingCategory

/**
 * Заголовок сворачиваемого блока рейтингов. Одна категория — её название, и
 * внутри блока оно уже не повторяется; несколько — общее «Рейтинг».
 */
fun channelRankingsTitle(rankings: List<ChannelRankingCategory>): String =
    rankings.singleOrNull()?.label ?: "Рейтинг"
