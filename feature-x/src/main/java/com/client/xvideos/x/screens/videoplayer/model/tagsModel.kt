package com.client.xvideos.x.screens.videoplayer.model

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
data class TagsMainUploaderPornstar(val href: String, val name: String, val count: String)


//<li class="model">
//<a class="btn btn-default label profile hover-name is-pornstar" data-id="306248827" href="/pornstars/london-river">
//<span class="model-star-sub icon-f icf-star-o" data-user-id="306248827" data-user-profile="london-river"/>
//<span class="name">London River</span>
//<span class="user-subscribe" data-user-id="306248827" data-user-profile="london-river">
//<span class="count">198k</span>
//</span>
//</a>
//</li>
data class TagsModel(
    val mainUploader: List<TagsMainUploaderPornstar>,
    val pornstars: List<TagsMainUploaderPornstar>,
    val tags: List<String> = emptyList()
)
