package com.client.xvideos.x.search.model

import kotlinx.serialization.Serializable


//{
//    "N": "asian",
//    "R": "82.887"
//}
@Serializable
data class Keyword(val N: String, val R: String) //N группа R-рейтинг

@Serializable
data class Pornstar(
    val N: String,
    val F: String,
    val T: String, //"pornstar"
    val MV: Int,   //Количество видео
    val M: Int,
    val L: Int,
    val P: String, //Путь до картинки
    val RF: String, //Количество подписчиков
    val A: Map<String, String>? = null // Обрабатываем возможное отсутствие поля A
)

@Serializable
data class Channel(
    val N: String, //Отображаемое название канала в поисковике
    val F: String, //путь к /profiles/xxx
    val T: String, //Тип "channel"
    val CPV: Boolean, //true
    val M: Int, //0
    val L: Int, //0
    val P: String, // Путь к картинке
    val RF: String, //Количество подписчиков
    val A: Map<String, String>? = null //Хуета
)

@Serializable
data class SearchResult(
    val result: Boolean,
    val code: Int,
    val keywords: List<Keyword>,
    val pornstar: List<Pornstar>? = null, // Может отсутствовать
    val channel: List<Channel>? = null,   // Может отсутствовать
    val BLACKLISTED: Boolean? = null      // Может отсутствовать
)
