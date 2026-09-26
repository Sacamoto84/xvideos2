package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable

//<li class="main-uploader">
//<a class="btn btn-default label main uploader-tag hover-name" href="/milfed">
//<span class="name">
//<span class="icon-f icf-device-tv-v2"/>
//Milfed
//</span>
//<span class="user-subscribe" data -user-id="568100199" data -user-profile="milfed">
//<span class="count">359k</span>
//</span>
//</a>
//</li>
@Immutable
data class TagsMainUploaderPornstar(
    val href: String = "",
    val name: String = "",
    val count: String = ""
) {
    val isValid: Boolean get() = href.isNotBlank() && name.isNotBlank()
    val hasCount: Boolean get() = count.isNotBlank()

    companion object {
        val EMPTY = TagsMainUploaderPornstar()
    }
}


//<li class="model">
//<a class="btn btn-default label profile hover-name is-pornstar" data-id="306248827" href="/pornstars/london-river">
//<span class="model-star-sub icon-f icf-star-o" data-user-id="306248827" data-user-profile="london-river"/>
//<span class="name">London River</span>
//<span class="user-subscribe" data-user-id="306248827" data-user-profile="london-river">
//<span class="count">198k</span>
//</span>
//</a>
//</li>
@Immutable
data class TagsModel(
    val mainUploader: List<TagsMainUploaderPornstar> = emptyList(),
    val pornstars: List<TagsMainUploaderPornstar> = emptyList(),
    val tags: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = mainUploader.isEmpty() && pornstars.isEmpty() && tags.isEmpty()
    val isNotEmpty: Boolean get() = !isEmpty
    val hasMainUploader: Boolean get() = mainUploader.isNotEmpty()
    val hasPornstars: Boolean get() = pornstars.isNotEmpty()
    val hasTags: Boolean get() = tags.isNotEmpty()
    val totalCount: Int get() = mainUploader.size + pornstars.size + tags.size

    companion object {
        val EMPTY = TagsModel()
    }
}
