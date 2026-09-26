package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable

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
    val hasVideoUrl: Boolean
        get() = videoUrlHigh.isNotEmpty() || videoHLS.isNotEmpty() || videoUrlLow.isNotEmpty()

    val bestVideoUrl: String
        get() = videoUrlHigh.ifEmpty { videoHLS.ifEmpty { videoUrlLow } }

    val bestThumbnailUrl: String
        get() = thumbUrl169.ifEmpty { thumbUrl }

    val hasHls: Boolean get() = videoHLS.isNotEmpty()
    val hasHighQuality: Boolean get() = videoUrlHigh.isNotEmpty()
    val hasLowQuality: Boolean get() = videoUrlLow.isNotEmpty()
    val hasThumbnails: Boolean get() = thumbUrl.isNotEmpty() || thumbUrl169.isNotEmpty()
    val hasSponsors: Boolean get() = sponsors.isNotEmpty()
    val hasUploader: Boolean get() = uploaderName.isNotBlank()
    val hasTitle: Boolean get() = videoTitle.isNotBlank()
    val hasSlides: Boolean get() = thumbSlide.isNotBlank() || thumbSlideBig.isNotBlank() || thumbSlideMinute.isNotBlank()

    val isValid: Boolean get() = hasVideoUrl

    companion object {
        val EMPTY = HTML5PlayerConfig()
    }
}

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

    companion object {
        val EMPTY = Sponsor()
    }
}
