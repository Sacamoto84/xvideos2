package com.client.xvideos.r.model

enum class Order(val value: String) {
    TRENDING("trending"),
    TOP("top"),
    LATEST("latest"),
    OLDEST("oldest"),
    RECENT("recent"),
    BEST("best"),
    TOP28("top28"),

    /** Релевантность запросу. Есть только у поиска, у лент смысла не имеет. */
    RELEVANT("score"),

    //NEW("new"),

    FORCE_TEMP(""),

    // Значения именно top7/top28: столько же зашито в путь у getTopThisWeek и
    // getTopThisMonth. Раньше здесь стояли "week"/"month" — они никуда не
    // уходили, потому что для лент метод выбирается по самой константе, а не
    // по её значению. Но у поиска order берётся отсюда, и с "week" сервер
    // отдавал не то.
    TOP_WEEK("top7"),
    TOP_MONTH("top28"),
    //TOP ALL TIME
    TOP_ALLTIME("alltime"),


    //NICHES
    NICHES_SUBSCRIBERS_D("subscribers"),

    NICHES_SUBSCRIBERS_A("subscribers"),

    NICHES_POST_D("posts"),

    NICHES_POST_A("posts"),
    NICHES_NAME_A_Z("name"),
    NICHES_NAME_Z_A("name"),

}

enum class MediaType(val value: String) {
    IMAGE("i"),
    GIF("g"),
    ALL("all")
}











