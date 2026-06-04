package com.client.xvideos.l.model.enum

enum class AudiencesType(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val url: String
) {
    GAY(
        id = 2,
        title = "Gay / Yaoi",
        description = "For people who like men with men.",
        posterUrl = null,
        url = "/audiences/gay_2/"
    ),
    LESBIAN(
        id = 3,
        title = "Lesbian / Yuri",
        description = "For people who like women with women.",
        posterUrl = null,
        url = "/audiences/lesbian_3/"
    ),
    SOLO_GIRL(
        id = 6,
        title = "Solo Girl",
        description = "Features individual women without a partner.",
        posterUrl = null,
        url = "/audiences/solo-girl_6/"
    ),
    SOLO_GUY(
        id = 12,
        title = "Solo Guy",
        description = "",
        posterUrl = null,
        url = "/audiences/solo-male_12/"
    ),
    STRAIGHT(
        id = 1,
        title = "Straight Sex",
        description = "For people who like sex between men and women.",
        posterUrl = null,
        url = "/audiences/straight_1/"
    ),
    TRANS(
        id = 5,
        title = "Trans",
        description = "Features transsexual women in solo action.",
        posterUrl = null,
        url = "/audiences/trans_5/"
    ),
    TRANS_X_GIRL(
        id = 10,
        title = "Trans x Girl",
        description = "Features transsexual women engaged in sex with females.",
        posterUrl = null,
        url = "/audiences/trans-x-girl_10/"
    ),
    TRANS_X_GUY(
        id = 9,
        title = "Trans x Guy",
        description = "Features transsexual women engaged in sex with men.",
        posterUrl = null,
        url = "/audiences/trans-x-guy_9/"
    ),
    TRANS_X_TRANS(
        id = 8,
        title = "Trans x Trans",
        description = "Features transsexual women engaged in sex with fellow t-girls.",
        posterUrl = null,
        url = "/audiences/trans-x-trans_8/"
    );

    companion object {
        fun fromId(id: Int): AudiencesType? = entries.find { it.id == id }
        fun fromUrl(url: String): AudiencesType? = entries.find { it.url == url }
    }
}