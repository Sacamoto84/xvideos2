package com.client.xvideos.x.screens.channel.model

import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.urlStart

/** Адрес JSON с рейтингами канала или модели в выбранной [category]. */
fun buildProfileRanksUrl(cleanSlug: String, category: XCategory): String =
    "$urlStart/profiles/$cleanSlug/ranks/${category.siteKey}"
