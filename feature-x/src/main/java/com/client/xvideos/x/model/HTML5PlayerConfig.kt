package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable

/**
 * Конфигурация HTML5-видеоплеера, извлекаемая парсером со страницы видео X.
 *
 * Содержит прямые ссылки на видеопотоки (MP4 низкого/высокого качества, HLS m3u8),
 * ссылки на эскизы, раскадровку и метаданные автора.
 *
 * @property videoTitle Название видео.
 * @property encodedIdVideo Закодированный строковый ID ролика.
 * @property sponsors Список спонсоров ролика.
 * @property videoUrlLow Прямая ссылка на MP4 видео низкого качества (360p/480p).
 * @property videoUrlHigh Прямая ссылка на MP4 видео высокого качества (720p/1080p).
 * @property videoHLS Ссылка на адаптивный манифест потока HLS (`.m3u8`).
 * @property thumbUrl Базовый эскиз ролика.
 * @property thumbUrl169 Широкоформатный эскиз 16:9.
 * @property relatedVideos Сырые метаданные связанных видео.
 * @property thumbSlide Базовая раскадровка (storyboard).
 * @property thumbSlideBig Увеличенная раскадровка.
 * @property thumbSlideMinute Поминутная раскадровка.
 * @property idCDN Идентификатор CDN-сервера.
 * @property idCdnHLS Идентификатор CDN-сервера HLS.
 * @property fakePlayer Флаг заглушки плеера.
 * @property desktopView Флаг десктопного режима отображения.
 * @property seekBarColor Цвет полосы перемотки.
 * @property uploaderName Имя автора/канала, загрузившего видео.
 * @property videoURL Канонический URL видео.
 * @property staticPath Путь к статическим ресурсам.
 * @property https Флаг защищенного соединения.
 * @property viewData Дополнительные данные аналитики просмотров.
 */
@Immutable
data class HTML5PlayerConfig(
    val videoTitle: String = "",
    val encodedIdVideo: String = "",
    val sponsors: List<Sponsor> = emptyList(),
    val videoUrlLow: String = "",
    val videoUrlHigh: String = "",
    val videoHLS: String = "",
    val thumbUrl: String = "",
    val thumbUrl169: String = "",
    val relatedVideos: Any? = null, // Assuming video_related is a complex object
    val thumbSlide: String = "",
    val thumbSlideBig: String = "",
    val thumbSlideMinute: String = "",
    val idCDN: String = "",
    val idCdnHLS: String = "",
    val fakePlayer: Boolean = true,
    val desktopView: Boolean = true,
    val seekBarColor: String = "",
    val uploaderName: String = "",
    val videoURL: String = "",
    val staticPath: String = "",
    val https: Boolean = true,
    val viewData: String = ""
) {
    /** `true`, если доступна хотя бы одна прямая ссылка на видео (MP4 High/Low или HLS). */
    val hasVideoUrl: Boolean
        get() = videoUrlHigh.isNotEmpty() || videoHLS.isNotEmpty() || videoUrlLow.isNotEmpty()

    /** Наилучшая ссылка на видео (приоритет: High MP4 -> HLS -> Low MP4). */
    val bestVideoUrl: String
        get() = videoUrlHigh.ifEmpty { videoHLS.ifEmpty { videoUrlLow } }

    /** Наилучший эскиз (приоритет: широкоформатный 16:9 -> базовый). */
    val bestThumbnailUrl: String
        get() = thumbUrl169.ifEmpty { thumbUrl }

    val hasHls: Boolean get() = videoHLS.isNotEmpty()
    val hasHighQuality: Boolean get() = videoUrlHigh.isNotEmpty()
    val hasLowQuality: Boolean get() = videoUrlLow.isNotEmpty()
    val hasThumbnails: Boolean get() = thumbUrl.isNotEmpty() || thumbUrl169.isNotEmpty()
    val hasSponsors: Boolean get() = sponsors.isNotEmpty()
    val hasUploader: Boolean get() = uploaderName.isNotBlank()
    val hasTitle: Boolean get() = videoTitle.isNotBlank()
    val hasValidTitle: Boolean get() = videoTitle.isNotBlank()
    val hasSlides: Boolean get() = thumbSlide.isNotBlank() || thumbSlideBig.isNotBlank() || thumbSlideMinute.isNotBlank()
    val hasAnyMedia: Boolean get() = hasVideoUrl || hasThumbnails

    val isValid: Boolean get() = hasVideoUrl

    fun withVideoUrls(high: String, low: String, hls: String = ""): HTML5PlayerConfig =
        copy(videoUrlHigh = high, videoUrlLow = low, videoHLS = hls)

    companion object {
        val EMPTY = HTML5PlayerConfig()
    }
}

/**
 * Информация о спонсоре или рекламной ссылке в плеере X.
 */
@Immutable
data class Sponsor(
    val link: String = "",
    val desc: String = "",
    val records2257: String = "",
    val name: String = ""
) {
    val isValid: Boolean get() = link.isNotBlank() || name.isNotBlank()
    val hasLink: Boolean get() = link.isNotBlank()
    val hasName: Boolean get() = name.isNotBlank()
    val displayName: String get() = name.ifBlank { desc }

    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        val q = query.trim()
        return name.contains(q, ignoreCase = true) || desc.contains(q, ignoreCase = true)
    }

    companion object {
        val EMPTY = Sponsor()
    }
}
