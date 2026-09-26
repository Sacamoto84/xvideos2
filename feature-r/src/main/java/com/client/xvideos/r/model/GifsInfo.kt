package com.client.xvideos.r.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import java.io.Serializable

/**
 * Основная доменная модель элемента медиа (GIF или изображения) в разделе RedGifs.
 *
 * Инкапсулирует метаданные ролика: идентификатор, ссылки на стриминг и постеры,
 * дату создания, информацию об авторе, счетчики лайков и просмотров.
 *
 * `Serializable` обязателен: модель лежит в аргументах `ScreenRedFullScreen`, а экраны
 * Voyager (`Screen : Serializable`) целиком уходят в saved state активити через
 * `Parcel.writeSerializable`. Без этого приложение падает с
 * `NotSerializableException`, когда система сохраняет состояние.
 *
 * @property id Уникальный текстовый идентификатор медиафайла (например, "drablonelychameleon").
 * @property createDate Время создания в формате unix timestamp (секунды/миллисекунды).
 * @property contentType Категория контента (по умолчанию "Solo Female").
 * @property likes Количество лайков на сервере.
 * @property width Ширина видео/изображения в пикселях.
 * @property height Высота видео/изображения в пикселях.
 * @property tags Список тегов медиа.
 * @property description Текстовое описание от автора.
 * @property views Количество просмотров (может быть null).
 * @property type Тип контента: 0/1 — GIF/Video, 2 — статическое изображение.
 * @property userName Имя автора/создателя контента.
 * @property urls Набор ссылок на медиафайлы различных разрешений и постеры.
 * @property duration Длительность видео в секундах (с плавающей точкой).
 * @property hls Флаг доступности HLS-потока.
 * @property niches Список слагов ниш, к которым относится данный ролик.
 */
@Stable
@kotlinx.serialization.Serializable
data class GifsInfo(
    @SerialName("id") val id: String = "",
    @SerialName("createDate") val createDate: Long = 0,
    @SerialName("contentType") val contentType: String = "Solo Female",
    @SerialName("likes") val likes: Int = 0,
    @SerialName("width") val width: Int = 100,
    @SerialName("height") val height: Int = 100,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("description") val description: String = "Описание",
    @SerialName("views") val views: Long? = null,
    @SerialName("type") val type: Int = 0,  //1-Gif 2-Image
    @SerialName("userName") val userName: String = "userName",           // "lilijunex"
    @SerialName("urls") val urls: URL1 = URL1.EMPTY,
    @SerialName("duration") val duration: Double? = null, //15.033,
    @SerialName("hls") val hls: Boolean? = null,
    @SerialName("niches") val niches: List<String>? = null,
) : Serializable {

    /** Проверяет, валиден ли объект (id не пуст и не состоит из одних пробелов). */
    val isValid: Boolean get() = id.isNotBlank()

    /** Проверяет, является ли объект статическим изображением (type == 2). */
    val isImage: Boolean get() = type == 2

    /** Проверяет, является ли объект анимацией/видео (type == 0 или 1). */
    val isGif: Boolean get() = type == 1 || type == 0

    /** Проверяет наличие непустого списка тегов. */
    val hasTags: Boolean get() = tags.isNotEmpty()

    /** Проверяет наличие привязанных ниш. */
    val hasNiches: Boolean get() = !niches.isNullOrEmpty()

    /** Проверяет наличие осмысленного описания (не дефолтного "Описание"). */
    val hasDescription: Boolean get() = description.isNotBlank() && description != "Описание"

    /** Проверяет, задана ли валидная положительная длительность. */
    val hasDuration: Boolean get() = duration != null && duration > 0.0

    /** Проверяет наличие положительного числа просмотров. */
    val hasViews: Boolean get() = views != null && views > 0L

    /** Проверяет наличие хотя бы одного лайка. */
    val hasLikes: Boolean get() = likes > 0

    /** Проверяет, валиден ли вложенный объект ссылок [urls]. */
    val hasUrls: Boolean get() = urls.isValid

    /** Проверяет, указано ли непустое имя автора (отличное от дефолтного "userName"). */
    val hasUserName: Boolean get() = userName.isNotBlank() && userName != "userName"

    /** Проверяет наличие корректных положительных габаритов. */
    val hasDimensions: Boolean get() = width > 0 && height > 0

    /** Вычисляет соотношение сторон медиа (width / height). */
    val aspectRatio: Float get() = if (height > 0) width.toFloat() / height.toFloat() else 1f

    /** Проверяет совпадение по ID, автору, описанию, тегам или нишам. */
    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return false
        val q = query.trim()
        return id.contains(q, ignoreCase = true) ||
            userName.contains(q, ignoreCase = true) ||
            description.contains(q, ignoreCase = true) ||
            tags.any { it.contains(q, ignoreCase = true) } ||
            niches?.any { it.contains(q, ignoreCase = true) } == true
    }

    /** Форматирует длительность в формат m:ss или Xs. */
    fun formatDuration(): String {
        val dur = duration ?: return ""
        val totalSecs = dur.toLong()
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return if (mins > 0) String.format(java.util.Locale.US, "%d:%02d", mins, secs) else "${secs}s"
    }

    companion object {
        /** Пустой экземпляр [GifsInfo] со значениями по умолчанию. */
        val EMPTY = GifsInfo()
    }
}

/**
 * Очищает и нормализует объект [GifsInfo]:
 * - Возвращает null, если [GifsInfo.id] пуст или null;
 * - Заменяет потенциальные null-поля на безопасные значения по умолчанию;
 * - Фильтрует пустые теги в списке;
 * - Нормализует вложенный [URL1] через [URL1.sanitize];
 * - Избегает лишнего копирования, если объект уже чист.
 */
fun GifsInfo.sanitizeOrNull(): GifsInfo? {
    val safeId: String? = id
    if (safeId.isNullOrBlank()) return null

    val safeContentType: String? = contentType
    val safeTags: List<String>? = tags
    val safeDescription: String? = description
    val safeUserName: String? = userName
    val safeUrls: URL1? = urls

    val sanitizedTags = sanitizeTagsList(safeTags)
    val sanitizedUrls = safeUrls?.sanitize() ?: URL1.EMPTY

    val stringsValid = safeContentType != null && safeDescription != null && safeUserName != null
    if (stringsValid && sanitizedTags === safeTags && sanitizedUrls === safeUrls) {
        return this
    }

    return copy(
        id = safeId,
        contentType = safeContentType ?: "Solo Female",
        tags = sanitizedTags,
        description = safeDescription ?: "Описание",
        userName = safeUserName ?: "userName",
        urls = sanitizedUrls
    )
}

/**
 * Очищает список тегов от null и пустых строк.
 * Если все теги валидны, возвращает исходный список без дополнительных аллокаций.
 */
private fun sanitizeTagsList(safeTags: List<String>?): List<String> {
    if (safeTags.isNullOrEmpty()) return emptyList()
    var hasInvalid = false
    for (tag in safeTags) {
        val s: String? = tag
        if (s.isNullOrBlank()) {
            hasInvalid = true
            break
        }
    }
    if (!hasInvalid) return safeTags
    val out = ArrayList<String>(safeTags.size)
    for (tag in safeTags) {
        val s: String? = tag
        if (!s.isNullOrBlank()) {
            out.add(s)
        }
    }
    return if (out.isEmpty()) emptyList() else out
}

/**
 * Очищает и дедуплицирует список элементов [GifsInfo]:
 * - Пропускает невалидные элементы ([sanitizeOrNull]);
 * - Исключает дубликаты по [GifsInfo.id] с сохранением порядка первого вхождения;
 * - Безопасен для null-коллекций (возвращает emptyList()).
 */
fun List<GifsInfo>?.sanitizeGifsInfoList(): List<GifsInfo> {
    if (this.isNullOrEmpty()) return emptyList()
    if (this.size == 1) {
        val single = this[0].sanitizeOrNull() ?: return emptyList()
        return listOf(single)
    }
    val seenIds = HashSet<String>(this.size)
    val result = ArrayList<GifsInfo>(this.size)
    for (item in this) {
        val safeItem: GifsInfo? = item
        val sanitized = safeItem?.sanitizeOrNull() ?: continue
        if (seenIds.add(sanitized.id)) {
            result.add(sanitized)
        }
    }
    return result
}
